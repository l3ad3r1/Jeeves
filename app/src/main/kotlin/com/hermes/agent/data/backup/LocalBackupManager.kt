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
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun exportToZip(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "hermes_backup_$dateStr.zip"
            
            // Primary: /Hermes Agent/Backup at the external-storage root
            // (requires All Files Access — MANAGE_EXTERNAL_STORAGE — declared in
            // the manifest). MediaStore can't target a non-standard root folder,
            // so we write directly via the File API.
            val externalResult = exportViaExternalStorage(fileName)
            if (externalResult.isSuccess) {
                return@withContext externalResult
            }

            // Fallback to app-specific storage if the root write fails (e.g. All
            // Files Access not yet granted). Never silently routes to Downloads.
            exportViaAppSpecificStorage(fileName)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export local backup")
            Result.failure(e)
        }
    }

    /**
     * Writes the backup ZIP directly to `<external storage>/Hermes Agent/Backup`
     * via the File API. Works because the app holds MANAGE_EXTERNAL_STORAGE
     * (All Files Access). If that permission isn't granted the write throws and
     * the caller falls back to app-specific storage.
     */
    private fun exportViaExternalStorage(fileName: String): Result<Uri> {
        return try {
            @Suppress("DEPRECATION")
            val backupDir = File(Environment.getExternalStorageDirectory(), "Hermes Agent/Backup")
                .apply { mkdirs() }
            val backupFile = File(backupDir, fileName)
            FileOutputStream(backupFile).use { outputStream ->
                writeZipToStream(outputStream)
            }
            Result.success(Uri.fromFile(backupFile))
        } catch (e: Exception) {
            Timber.w(e, "External-storage backup export failed, falling back to app storage")
            Result.failure(e)
        }
    }

    private fun exportViaAppSpecificStorage(fileName: String): Result<Uri> {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), "Backup").apply { mkdirs() }
            val backupFile = File(backupDir, fileName)
            FileOutputStream(backupFile).use { outputStream ->
                writeZipToStream(outputStream)
            }
            Result.success(Uri.fromFile(backupFile))
        } catch (e: Exception) {
            Timber.e(e, "App-specific backup storage failed")
            Result.failure(e)
        }
    }

    private fun writeZipToStream(outputStream: java.io.OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            // Database files
            val dbFile = context.getDatabasePath("hermes.db")
            val walFile = context.getDatabasePath("hermes.db-wal")
            val shmFile = context.getDatabasePath("hermes.db-shm")

            val filesToBackup = mutableListOf<File>()
            listOf(dbFile, walFile, shmFile).forEach { if (it.exists()) filesToBackup.add(it) }

            // DataStore directory files
            val dataStoreDir = File(context.filesDir, "datastore")
            if (dataStoreDir.exists() && dataStoreDir.isDirectory) {
                dataStoreDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".preferences_pb")) {
                        filesToBackup.add(file)
                    }
                }
            }

            for (file in filesToBackup) {
                zos.putNextEntry(ZipEntry(file.name))
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
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
