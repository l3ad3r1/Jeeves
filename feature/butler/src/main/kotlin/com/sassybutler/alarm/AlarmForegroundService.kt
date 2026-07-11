package com.sassybutler.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.*

/**
 * AlarmForegroundService — Owns the alarm lifecycle.
 *
 * Responsibilities:
 *  1. Promote itself to foreground immediately (required on API 26+).
 *  2. Hold a PARTIAL_WAKE_LOCK so the CPU stays awake during audio.
 *  3. Launch AlarmActivity on the lock screen.
 *  4. Own an [AudioEngine] instance and start the birds → TTS sequence.
 *  5. Receive a dismiss command from [AlarmActivity] and stop everything.
 */
class AlarmForegroundService : LifecycleService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var audioEngine: AudioEngine
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var alarmRunning = false
    private var currentAlarmId = -1
    private var currentHour = 7
    private var currentMinute = 0

    // ─── Service lifecycle ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioEngine = AudioEngine(applicationContext)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_ALARM   -> handleStartAlarm(intent)
            ACTION_DISMISS_ALARM -> handleDismiss()
            ACTION_SNOOZE_ALARM  -> handleSnooze()
        }

        return START_NOT_STICKY // don't restart automatically
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        scope.cancel()
        stopHaptics()
        audioEngine.release()
        wakeLock?.release()
        wakeLock = null
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    // ─── Action handlers ────────────────────────────────────────────────

    private fun handleStartAlarm(intent: Intent) {
        // The system can re-deliver a start intent (e.g. service restart after
        // a crash) while an alarm is already ringing; a second concurrent
        // audio sequence doubles the TTS inference load and the audio.
        if (alarmRunning) {
            Log.w(TAG, "Alarm already running — ignoring duplicate start")
            return
        }
        alarmRunning = true

        val alarmId  = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
        val hour     = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 7)
        val minute   = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0)
        currentAlarmId = alarmId
        currentHour    = hour
        currentMinute  = minute

        Log.i(TAG, "Starting alarm id=$alarmId at $hour:${minute.toString().padStart(2,'0')}")

        // 1. Become a foreground service immediately to avoid ANR/Crash
        promoteToForeground(alarmId, hour, minute)

        // 2. Acquire wake lock
        acquireWakeLock()

        // 3. Keep the schedule alive immediately
        AlarmStore.get(this, alarmId)?.let { alarm ->
            if (alarm.days.isNotEmpty()) {
                AlarmScheduler(this).schedule(alarm)
            } else {
                AlarmStore.upsert(this, alarm.copy(enabled = false))
            }
        }

        // 4. Generate AI Greeting and play sequence
        scope.launch {
            var greeting = ButlerScript.greeting(this@AlarmForegroundService, formatTime(hour, minute))
            try {
                val aiProvider = dagger.hilt.android.EntryPointAccessors.fromApplication(
                    applicationContext, com.sassybutler.alarm.di.ButlerAiProviderEntryPoint::class.java
                ).getButlerAiProvider()
                
                val hon = ButlerPrefs.honorific(this@AlarmForegroundService)
                val sassLevel = ButlerPrefs.sassLevel(this@AlarmForegroundService)
                val weather = WeatherService.cached(this@AlarmForegroundService)?.sentence() ?: "Unknown weather"
                
                val generated = aiProvider.generateMorningGreeting(weather, formatTime(hour, minute), hon, sassLevel)
                if (generated != null && generated.isNotBlank()) greeting = generated
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate AI greeting, using fallback.")
            }

            // Launch the full-screen lock screen activity (shows the greeting)
            launchAlarmActivity(alarmId, hour, minute, greeting)

            // Optional refined vibration
            startHaptics()

            // Begin the audio sequence: (birds →) TTS greeting
            audioEngine.playAlarmSequence(
                greeting     = greeting,
                skipBirds    = !ButlerPrefs.birdsIntro(this@AlarmForegroundService),
                voiceEnabled = ButlerPrefs.voiceEnabled(this@AlarmForegroundService),
                onBirdsComplete = { Log.d(TAG, "Birds finished, TTS playing") }
            )

            // Refresh the weather cache in the background
            WeatherService.refresh(this@AlarmForegroundService)
        }
    }

    private fun handleDismiss() {
        Log.i(TAG, "Dismiss received")
        alarmRunning = false
        stopHaptics()
        if (!ButlerPrefs.voiceEnabled(this)) {
            audioEngine.stopAll()
            stopSelf()
            return
        }
        scope.launch {
            // Stop when the parting shot has actually finished playing —
            // a fixed delay truncates it when synthesis runs slow. The
            // timeout is a safety net against TTS hanging.
            withTimeoutOrNull(20_000) {
                audioEngine.dismissWithReaction()
            }
            stopSelf()
        }
    }

    private fun handleSnooze() {
        val minutes = ButlerPrefs.snoozeMinutes(this)
        Log.i(TAG, "Snooze received — $minutes min")
        alarmRunning = false
        stopHaptics()
        audioEngine.stopAll()

        AlarmScheduler(this).snoozeIn(currentAlarmId, currentHour, currentMinute, minutes)

        if (ButlerPrefs.voiceEnabled(this) && ButlerPrefs.snoozeCommentary(this)) {
            scope.launch {
                withTimeoutOrNull(20_000) {
                    audioEngine.speak(ButlerScript.snoozeLine(this@AlarmForegroundService))
                }
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    // ─── Haptics ─────────────────────────────────────────────────────────

    private fun startHaptics() {
        if (!ButlerPrefs.haptics(this)) return
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // A refined pattern: brief tap, long pause — not a jackhammer.
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 1650), 0))
    }

    private fun stopHaptics() {
        vibrator?.cancel()
        vibrator = null
    }

    // ─── Foreground notification ────────────────────────────────────────

    private fun promoteToForeground(alarmId: Int, hour: Int, minute: Int) {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmReceiver.EXTRA_ALARM_ID,     alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR,   hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
        }

        val fullScreenPending = PendingIntent.getActivity(
            this, alarmId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val dismissIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPending = PendingIntent.getService(
            this, alarmId + 1000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Rise and shine, if you must.")
            .setContentText("Your ${formatTime(hour, minute)} alarm is ringing.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPending, true)
            .addAction(R.drawable.ic_dismiss, "Hush", dismissPending)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun launchAlarmActivity(alarmId: Int, hour: Int, minute: Int, greeting: String) {
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmReceiver.EXTRA_ALARM_ID,     alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR,   hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
            putExtra(EXTRA_GREETING, greeting)
        }
        startActivity(activityIntent)
    }

    // ─── Wake lock ───────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "JeevesAlarms::WakeLock"
        ).also { it.acquire(10 * 60 * 1000L) } // 10 min max
    }

    // ─── Notification channel ────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description  = "Used for alarm firing notifications"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    // ─── Utilities ───────────────────────────────────────────────────────

    private fun formatTime(hour: Int, minute: Int): String {
        val h  = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val m  = minute.toString().padStart(2, '0')
        val am = if (hour < 12) "AM" else "PM"
        return "$h:$m $am"
    }

    companion object {
        private const val TAG             = "AlarmForegroundService"
        private const val CHANNEL_ID      = "alarm_channel"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_GREETING = "extra_greeting"

        const val ACTION_START_ALARM   = "com.sassybutler.alarm.ACTION_START_ALARM"
        const val ACTION_DISMISS_ALARM = "com.sassybutler.alarm.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM  = "com.sassybutler.alarm.ACTION_SNOOZE_ALARM"
    }
}
