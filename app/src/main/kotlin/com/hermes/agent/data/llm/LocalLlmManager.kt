package com.hermes.agent.data.llm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.isModelLoaded
import com.hermes.agent.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Singleton
class LocalLlmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val downloadCoordinator: LocalModelDownloadCoordinator,
) {
    private val engine: InferenceEngine = AiChat.getInferenceEngine(context)

    private suspend fun activeModel(): DownloadableModel =
        ModelCatalog.byId(settingsRepository.current().selectedModelId)

    private suspend fun destinationDir(): File {
        val custom = settingsRepository.current().modelDownloadDir
        return if (custom.isNotBlank()) File(custom)
        else File(Environment.getExternalStorageDirectory(), ModelCatalog.DEFAULT_DIR_NAME)
    }

    private suspend fun currentModelFile(): File = File(destinationDir(), activeModel().fileName)

    suspend fun isModelDownloaded(): Boolean {
        val settings = settingsRepository.current()
        if (settings.localModelUri.isNotBlank()) {
            return runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(settings.localModelUri), "r")
                    ?.use { it.statSize != 0L } == true
            }.getOrDefault(false)
        }
        val model = activeModel()
        val file = currentModelFile()
        return file.isFile && file.length() == model.sizeBytes
    }

    val isDownloading: StateFlow<Boolean> = downloadCoordinator.isDownloading
    val downloadProgress: StateFlow<Float> = downloadCoordinator.progress
    val downloadError: StateFlow<String> = downloadCoordinator.error

    suspend fun startDownload() {
        if (isDownloading.value || isModelDownloaded()) return
        if (!hasStorageAccess(context)) {
            downloadCoordinator.reportError(
                "Storage access is required to save the model. Grant it above and try again.",
            )
            return
        }
        downloadCoordinator.enqueue(activeModel(), destinationDir())
    }

    fun clearDownloadError() = downloadCoordinator.clearError()

    suspend fun initialize() {
        if (!isModelDownloaded()) {
            throw IllegalStateException("Model not downloaded yet. Please download it in settings.")
        }
        if (!engine.state.value.isModelLoaded) {
            val customUri = settingsRepository.current().localModelUri
            if (customUri.isNotBlank()) {
                val uri = Uri.parse(customUri)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    engine.loadModel("/proc/self/fd/${descriptor.fd}")
                } ?: throw IllegalStateException("Cannot open the custom model file. Choose it again.")
            } else {
                engine.loadModel(currentModelFile().absolutePath)
            }
        }
    }

    fun generateResponse(systemPrompt: String, userPrompt: String): Flow<String> = flow {
        if (!engine.state.value.isModelLoaded) initialize()
        // Always reset native chat state: the provider supplies a bounded transcript
        // on every call, including internal calls that have no explicit system message.
        engine.setSystemPrompt(systemPrompt.ifBlank { DEFAULT_SYSTEM_PROMPT })
        engine.sendUserPrompt(userPrompt).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun close() {
        engine.cleanUp()
    }

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = "You are Jeeves, a helpful on-device assistant."

        fun hasStorageAccess(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            }
    }
}
