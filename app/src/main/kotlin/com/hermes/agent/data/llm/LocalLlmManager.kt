package com.hermes.agent.data.llm

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.isModelLoaded
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val engine: InferenceEngine = AiChat.getInferenceEngine(context)
    
    private val modelFileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"

    val modelFile: File
        get() = File(context.filesDir, modelFileName)

    fun isModelDownloaded(): Boolean {
        return modelFile.exists() && modelFile.length() > 0
    }

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    fun startDownload() {
        if (isDownloading.value || isModelDownloaded()) return
        _isDownloading.value = true
        _downloadProgress.value = 0f
        
        val url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Downloading Llama 3.2 1B")
            setDescription("The local LLM is being downloaded.")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, modelFileName)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    try {
                        val externalFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), modelFileName)
                        val internalFile = File(context.filesDir, modelFileName)
                        if (externalFile.exists()) {
                            externalFile.copyTo(internalFile, overwrite = true)
                            externalFile.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    _isDownloading.value = false
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading && _isDownloading.value) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = manager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    
                    if (statusIndex >= 0 && bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0) {
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                            if (status == DownloadManager.STATUS_FAILED) {
                                _isDownloading.value = false
                            }
                        } else {
                            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                            val bytesTotal = cursor.getLong(bytesTotalIndex)
                            if (bytesTotal > 0) {
                                _downloadProgress.value = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                            }
                        }
                    }
                    cursor.close()
                }
                delay(1000)
            }
        }
    }

    suspend fun initialize() {
        if (!isModelDownloaded()) {
            throw IllegalStateException("Model not downloaded yet. Please download it in settings.")
        }
        if (!engine.state.value.isModelLoaded) {
            engine.loadModel(modelFile.absolutePath)
        }
    }

    fun generateResponse(prompt: String): Flow<String> = flow {
        if (!engine.state.value.isModelLoaded) {
            initialize()
        }
        engine.sendUserPrompt(prompt).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun close() {
        engine.cleanUp()
    }
}
