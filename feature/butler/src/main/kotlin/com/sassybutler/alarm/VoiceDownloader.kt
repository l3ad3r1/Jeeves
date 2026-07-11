package com.sassybutler.alarm

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File

object VoiceDownloader {
    private const val TAG = "VoiceDownloader"
    private const val VOICES_URL = "https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/voices.bin"
    const val DOWNLOADED_VOICES_FILE = "voices-full.bin"

    private var downloadId: Long = -1L
    private var isDownloading = false

    fun isDownloaded(context: Context): Boolean {
        val file = File(context.filesDir, DOWNLOADED_VOICES_FILE)
        return file.exists() && file.length() > 0
    }

    fun downloadVoices(context: Context, onComplete: () -> Unit) {
        if (isDownloaded(context)) {
            onComplete()
            return
        }
        
        if (isDownloading) {
            Toast.makeText(context, "Voice bundle is already downloading...", Toast.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(Uri.parse(VOICES_URL)).apply {
            setTitle("Downloading Jeeves Voices")
            setDescription("The butler requires his vocal cords.")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Save to external files dir temporarily so DownloadManager can write to it
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, DOWNLOADED_VOICES_FILE)
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = manager.enqueue(request)
        isDownloading = true
        Toast.makeText(context, "Downloading additional voices...", Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    isDownloading = false
                    Log.d(TAG, "Download complete!")
                    
                    // Move from external files dir to internal files dir
                    try {
                        val externalFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOWNLOADED_VOICES_FILE)
                        val internalFile = File(context.filesDir, DOWNLOADED_VOICES_FILE)
                        if (externalFile.exists()) {
                            externalFile.copyTo(internalFile, overwrite = true)
                            externalFile.delete()
                            Log.d(TAG, "Moved voices to internal storage")
                            onComplete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to move downloaded file", e)
                        Toast.makeText(context, "Failed to install voices.", Toast.LENGTH_SHORT).show()
                    }
                    
                    context.unregisterReceiver(this)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }
}
