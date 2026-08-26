package com.hermes.agent.data.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hermes.agent.domain.settings.SettingsRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    private companion object {
        /** DataStore key names from `SettingsRepositoryImpl.Keys` holding credentials. */
        val SECRET_PREFERENCE_KEYS = listOf(
            "cloud_api_key",
            "aux_api_key",
            "github_pat",
            "api_server_key",
            "ssh_password",
            "backup_passphrase",
        )
        const val PROVIDER_PROFILES_KEY = "cloud_provider_profiles"
    }

    suspend fun exportToZip(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "jeeves_backup_$dateStr.zip"
            
            // Primary: /Jeeves/Backup at the external-storage root
            // (requires All Files Access — MANAGE_EXTERNAL_STORAGE — declared in
            // the manifest). MediaStore can't target a non-standard root folder,
            // so we write directly via the File API.
            // Credentials are re-encrypted for travel rather than copied. What
            // sits in the DataStore file is sealed with this install's keystore
            // key, which cannot leave the device, so a straight copy restores
            // bytes no other install can open. Those values are stripped from
            // the copy and rewritten into `secrets.json` under a passphrase.
            val secretsJson = buildEncryptedSecrets()
            val prefs = sanitizedPrefsCopies()
            try {
                val externalResult = exportViaExternalStorage(fileName, prefs, secretsJson)
                if (externalResult.isSuccess) {
                    return@withContext externalResult
                }

                // Fallback to app-specific storage if the root write fails (e.g. All
                // Files Access not yet granted). Never silently routes to Downloads.
                exportViaAppSpecificStorage(fileName, prefs, secretsJson)
            } finally {
                prefs.forEach { (_, file) -> runCatching { file.delete() } }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to export local backup")
            Result.failure(e)
        }
    }

    /**
     * Writes the backup ZIP directly to `<external storage>/Jeeves/Backup`
     * via the File API. Works because the app holds MANAGE_EXTERNAL_STORAGE
     * (All Files Access). If that permission isn't granted the write throws and
     * the caller falls back to app-specific storage.
     */
    private fun exportViaExternalStorage(
        fileName: String,
        prefs: List<Pair<String, File>>,
        secretsJson: String?,
    ): Result<Uri> {
        return try {
            @Suppress("DEPRECATION")
            val backupDir = File(Environment.getExternalStorageDirectory(), "Jeeves/Backup")
                .apply { mkdirs() }
            val backupFile = File(backupDir, fileName)
            FileOutputStream(backupFile).use { outputStream ->
                writeZipToStream(outputStream, prefs, secretsJson)
            }
            Result.success(Uri.fromFile(backupFile))
        } catch (e: Exception) {
            Timber.w(e, "External-storage backup export failed, falling back to app storage")
            Result.failure(e)
        }
    }

    private fun exportViaAppSpecificStorage(
        fileName: String,
        prefs: List<Pair<String, File>>,
        secretsJson: String?,
    ): Result<Uri> {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), "Backup").apply { mkdirs() }
            val backupFile = File(backupDir, fileName)
            FileOutputStream(backupFile).use { outputStream ->
                writeZipToStream(outputStream, prefs, secretsJson)
            }
            Result.success(Uri.fromFile(backupFile))
        } catch (e: Exception) {
            Timber.e(e, "App-specific backup storage failed")
            Result.failure(e)
        }
    }

    private fun writeZipToStream(
        outputStream: java.io.OutputStream,
        prefs: List<Pair<String, File>>,
        secretsJson: String?,
    ) {
        ZipOutputStream(outputStream).use { zos ->
            val dbFile = context.getDatabasePath("hermes.db")
            val walFile = context.getDatabasePath("hermes.db-wal")
            val shmFile = context.getDatabasePath("hermes.db-shm")

            val entries = mutableListOf<Pair<String, File>>()
            listOf(dbFile, walFile, shmFile).forEach { if (it.exists()) entries += it.name to it }
            // Preference files arrive already stripped of credentials.
            entries += prefs

            for ((entryName, file) in entries) {
                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            }

            secretsJson?.let { json ->
                zos.putNextEntry(ZipEntry(BackupSecrets.ENTRY_NAME))
                zos.write(json.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
    }

    /**
     * Re-encrypts every stored credential under the device's backup passphrase,
     * generating that passphrase on first use so routine backups stay
     * unattended. Returns null when there is nothing to carry.
     */
    private suspend fun buildEncryptedSecrets(): String? {
        val settings = settingsRepository.current()
        val passphrase = settings.backupPassphrase.ifBlank {
            BackupCrypto.generatePassphrase().also { settingsRepository.setBackupPassphrase(it) }
        }

        fun seal(value: String) = BackupCrypto.encrypt(passphrase, value).orEmpty()

        val secrets = BackupSecrets(
            cloudApiKey = seal(settings.cloudApiKey),
            auxApiKey = seal(settings.auxApiKey),
            apiServerKey = seal(settings.apiServerKey),
            sshPassword = seal(settings.sshPassword),
            providerKeys = settings.cloudProviderProfiles
                .filter { it.apiKey.isNotBlank() }
                .associate { it.id to seal(it.apiKey) }
                .filterValues { it.isNotEmpty() },
        )

        val carriesSomething = listOf(
            secrets.cloudApiKey, secrets.auxApiKey,
            secrets.apiServerKey, secrets.sshPassword,
        ).any { it.isNotEmpty() } || secrets.providerKeys.isNotEmpty()

        // encodeDefaults, so version/kdf/iterations are always written even when
        // they match the current defaults. A reader that has to guess which
        // scheme produced an archive cannot safely change the scheme later.
        val json = Json { encodeDefaults = true }
        return if (carriesSomething) json.encodeToString(BackupSecrets.serializer(), secrets) else null
    }

    /**
     * Copies each DataStore preference file to the cache and removes the
     * keystore-sealed credentials from the copy, returning `entry name to file`.
     *
     * Edited through DataStore rather than by patching protobuf bytes, so the
     * file stays valid however the format evolves. Provider profiles are kept —
     * their base URLs and model ids are worth restoring — with only the key
     * blanked, over the raw JSON tree so a new field on `CloudProviderProfile`
     * cannot be silently dropped here.
     */
    private suspend fun sanitizedPrefsCopies(): List<Pair<String, File>> {
        val dataStoreDir = File(context.filesDir, "datastore")
        val sources = dataStoreDir.takeIf { it.isDirectory }
            ?.listFiles { f: File -> f.isFile && f.name.endsWith(".preferences_pb") }
            ?.toList()
            .orEmpty()

        return sources.mapNotNull { source ->
            runCatching {
                val copy = File(context.cacheDir, "backup-${source.name}")
                copy.delete()
                source.copyTo(copy, overwrite = true)

                val store = PreferenceDataStoreFactory.create(produceFile = { copy })
                store.edit { p ->
                    SECRET_PREFERENCE_KEYS.forEach { p.remove(stringPreferencesKey(it)) }
                    val profilesKey = stringPreferencesKey(PROVIDER_PROFILES_KEY)
                    p[profilesKey]?.let { json -> p[profilesKey] = blankProfileKeys(json) }
                }
                source.name to copy
            }.onFailure {
                Timber.w(it, "Could not sanitize ${source.name}; excluding it from the backup")
            }.getOrNull()
        }
    }

    /** Replaces every `apiKey` in the serialized provider list with an empty string. */
    private fun blankProfileKeys(json: String): String = runCatching {
        val cleaned = Json.parseToJsonElement(json).jsonArray.map { element ->
            JsonObject(element.jsonObject.toMutableMap().apply { put("apiKey", JsonPrimitive("")) })
        }
        JsonArray(cleaned).toString()
    }.getOrElse {
        Timber.w(it, "Provider profiles unparseable; dropping them from the backup")
        "[]"
    }

    suspend fun restoreFromZip(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val fileName = entry.name
                        val targetFile = when (fileName) {
                            "hermes.db", "hermes.db-wal", "hermes.db-shm" -> context.getDatabasePath(fileName)
                            "hermes_settings.preferences_pb" -> File(context.filesDir, "datastore/$fileName")
                            // Staged, not applied. Writing settings now would race
                            // the preference file being replaced in this same loop.
                            BackupSecrets.ENTRY_NAME ->
                                File(context.filesDir, BackupSecrets.PENDING_FILE)
                            else -> null
                        }

                        if (targetFile != null) {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream for zip"))

            // Restart app to load new data
            restartApp()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore local backup")
            Result.failure(e)
        }
    }

    private fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent)
        Process.killProcess(Process.myPid())
    }
}
