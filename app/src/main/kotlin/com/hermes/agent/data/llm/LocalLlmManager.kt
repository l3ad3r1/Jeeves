package com.hermes.agent.data.llm

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.hermes.agent.data.settings.SettingsRepository
import android.os.ParcelFileDescriptor

@Singleton
class LocalLlmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val engine: InferenceEngine = AiChat.getInferenceEngine(context)

    // ─── Model + destination resolution ─────────────────────────────────────

    /** The catalog model the user has selected (or the default). */
    private suspend fun activeModel(): DownloadableModel =
        ModelCatalog.byId(settingsRepository.current().selectedModelId)

    /**
     * Where models are stored: the user's custom directory if set, else the
     * default top-level "AI Models" folder on shared storage. Writing here on
     * Android 11+ needs All-Files access (see [hasStorageAccess]).
     */
    private suspend fun destinationDir(): File {
        val custom = settingsRepository.current().modelDownloadDir
        return if (custom.isNotBlank()) File(custom)
        else File(Environment.getExternalStorageDirectory(), ModelCatalog.DEFAULT_DIR_NAME)
    }

    /** Absolute file the active model is (or will be) stored at. */
    private suspend fun currentModelFile(): File = File(destinationDir(), activeModel().fileName)

    suspend fun isModelDownloaded(): Boolean {
        val settings = settingsRepository.current()
        if (settings.localModelUri.isNotBlank()) return true
        val f = currentModelFile()
        return f.exists() && f.length() > 0
    }

    // ─── State ──────────────────────────────────────────────────────────────

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    /** Last download error, surfaced in Settings (L-007). Blank = no error. */
    private val _downloadError = MutableStateFlow("")
    val downloadError: StateFlow<String> = _downloadError

    fun clearDownloadError() { _downloadError.value = "" }

    // ─── Download ────────────────────────────────────────────────────────────

    fun startDownload() {
        CoroutineScope(Dispatchers.IO).launch {
            if (isDownloading.value || isModelDownloaded()) return@launch

            if (!hasStorageAccess(context)) {
                _downloadError.value =
                    "Storage access is required to save the model. Grant it above and try again."
                return@launch
            }

            val model = activeModel()
            val destDir = destinationDir()
            _isDownloading.value = true
            _downloadProgress.value = 0f
            _downloadError.value = ""

            // DownloadManager runs in a separate system process that does NOT
            // inherit this app's All-Files access, so it cannot write to a
            // top-level shared folder. It downloads to our app-external dir
            // (always writable, no permission), then we move the finished file
            // into destDir. The move is a same-volume rename — instant, no copy
            // (a ~800 MB copy only happens as a cross-volume fallback, e.g. a
            // custom folder on an SD card).
            val staging = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), model.fileName)
            val request = DownloadManager.Request(Uri.parse(model.url)).apply {
                setTitle("Downloading ${model.displayName}")
                setDescription("The local LLM is being downloaded.")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, model.fileName)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)

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
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false
                            val moved = moveIntoPlace(staging, destDir, model.fileName)
                            if (!moved) {
                                _downloadError.value =
                                    "Downloaded, but couldn't save into ${destDir.absolutePath}. " +
                                        "Check the folder path and storage access."
                            }
                            _isDownloading.value = false
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                            val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else -1
                            staging.delete()
                            _downloadError.value = "Download failed (code $reason). Check your connection and try again."
                            _isDownloading.value = false
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

    /**
     * Move [staging] into [destDir]/[fileName]. Tries an instant rename first
     * (same volume), falls back to copy+delete across volumes. Returns true on
     * success.
     */
    private fun moveIntoPlace(staging: File, destDir: File, fileName: String): Boolean {
        return try {
            if (!staging.exists()) return false
            if (!destDir.exists()) destDir.mkdirs()
            val dest = File(destDir, fileName)
            if (dest.exists()) dest.delete()
            if (staging.renameTo(dest)) return true
            // Cross-volume: rename fails, fall back to a copy.
            staging.copyTo(dest, overwrite = true)
            staging.delete()
            dest.exists() && dest.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ─── Load ────────────────────────────────────────────────────────────────

    suspend fun initialize() {
        if (!isModelDownloaded()) {
            throw IllegalStateException("Model not downloaded yet. Please download it in settings.")
        }
        if (!engine.state.value.isModelLoaded) {
            val customUri = settingsRepository.current().localModelUri
            if (customUri.isNotBlank()) {
                val uri = Uri.parse(customUri)
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalStateException("Cannot open custom model file")
                engine.loadModel("/proc/self/fd/${pfd.fd}")
                pfd.close()
            } else {
                engine.loadModel(currentModelFile().absolutePath)
            }
        }
    }

    fun generateResponse(systemPrompt: String, userPrompt: String): Flow<String> = flow {
        if (!engine.state.value.isModelLoaded) {
            initialize()
        }
        if (systemPrompt.isNotEmpty()) {
            engine.setSystemPrompt(systemPrompt)
        }
        engine.sendUserPrompt(userPrompt).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun close() {
        engine.cleanUp()
    }

    companion object {
        /**
         * Whether the app can write to a user-visible shared-storage folder.
         * Android 11+ (R) gates this behind All-Files access (a Settings grant);
         * Android 10 (API 29) uses legacy WRITE_EXTERNAL_STORAGE +
         * requestLegacyExternalStorage (manifest) for raw-path writes.
         */
        fun hasStorageAccess(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            }
    }
}
