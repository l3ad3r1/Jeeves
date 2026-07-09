package com.sassybutler.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * AlarmReceiver — Entry point when AlarmManager fires.
 *
 * Responsibilities:
 *  1. Receive the exact alarm PendingIntent from AlarmManager.
 *  2. Acquire a brief WakeLock so we have time to start the service.
 *  3. Launch AlarmForegroundService which owns the real wake-lock and audio.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive — action=${intent.action}")

        when (intent.action) {
            ACTION_ALARM_FIRE -> handleAlarmFired(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAlarmsAfterBoot(context)
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────

    private fun handleAlarmFired(context: Context, intent: Intent) {
        val alarmId   = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val alarmHour = intent.getIntExtra(EXTRA_ALARM_HOUR, 7)
        val alarmMin  = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)

        Log.i(TAG, "Alarm $alarmId fired at $alarmHour:$alarmMin — launching foreground service")

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_START_ALARM
            putExtra(EXTRA_ALARM_ID,     alarmId)
            putExtra(EXTRA_ALARM_HOUR,   alarmHour)
            putExtra(EXTRA_ALARM_MINUTE, alarmMin)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    /** AlarmManager clears all alarms on reboot — re-arm from AlarmStore. */
    private fun rescheduleAlarmsAfterBoot(context: Context) {
        Log.i(TAG, "Boot completed — rescheduling stored alarms")
        AlarmScheduler(context).rescheduleAll()
    }

    companion object {
        private const val TAG = "AlarmReceiver"

        const val ACTION_ALARM_FIRE   = "com.sassybutler.alarm.ACTION_ALARM_FIRE"
        const val EXTRA_ALARM_ID      = "extra_alarm_id"
        const val EXTRA_ALARM_HOUR    = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE  = "extra_alarm_minute"

        /**
         * Build the PendingIntent that AlarmManager will deliver when the alarm fires.
         */
        fun buildPendingIntent(
            context: Context,
            alarmId: Int,
            hour: Int,
            minute: Int
        ): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_FIRE
                putExtra(EXTRA_ALARM_ID,     alarmId)
                putExtra(EXTRA_ALARM_HOUR,   hour)
                putExtra(EXTRA_ALARM_MINUTE, minute)
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

            return PendingIntent.getBroadcast(context, alarmId, intent, flags)
        }
    }
}
