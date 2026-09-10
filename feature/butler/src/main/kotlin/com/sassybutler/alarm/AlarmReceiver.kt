package com.sassybutler.alarm

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
        val alarmHour = intent.getIntExtra(EXTRA_ALARM_HOUR, -1)
        val alarmMin  = intent.getIntExtra(EXTRA_ALARM_MINUTE, -1)
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT_MILLIS, INVALID_TRIGGER_AT)
        val alarm = AlarmStore.get(context, alarmId)

        if (!isExpectedAlarmFire(alarm, alarmId, alarmHour, alarmMin, triggerAt)) {
            Log.w(TAG, "Ignoring invalid, disabled, or stale alarm occurrence id=$alarmId")
            return
        }
        val storedAlarm = alarm ?: return

        // Advance the stored alarm before asking the foreground service to play it.
        // A second alarm can therefore arrive while another is ringing without losing
        // its next recurring occurrence.
        val scheduler = AlarmScheduler(context)
        if (storedAlarm.days.isNotEmpty()) {
            scheduler.schedule(storedAlarm)
        } else {
            AlarmStore.upsert(context, storedAlarm.copy(enabled = false))
            scheduler.cancel(storedAlarm.id, storedAlarm.hour, storedAlarm.minute)
        }

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
        const val EXTRA_TRIGGER_AT_MILLIS = "extra_alarm_trigger_at_millis"

        private const val INVALID_TRIGGER_AT = Long.MIN_VALUE
        private const val MAX_DELIVERY_DELAY_MILLIS = 15 * 60 * 1000L

        /**
         * Pending-intent extras are not an authority boundary, but validating them
         * against persisted state rejects stale occurrences after edits, disables, and
         * restores. The receiver itself is app-internal; the bounded delivery window
         * accounts for Android's inexact-alarm fallback.
         */
        internal fun isExpectedAlarmFire(
            alarm: Alarm?,
            alarmId: Int,
            hour: Int,
            minute: Int,
            triggerAtMillis: Long,
            nowMillis: Long = System.currentTimeMillis(),
        ): Boolean = alarm != null &&
            alarm.id == alarmId &&
            alarm.enabled &&
            alarm.hour == hour &&
            alarm.minute == minute &&
            triggerAtMillis != INVALID_TRIGGER_AT &&
            nowMillis >= triggerAtMillis &&
            nowMillis - triggerAtMillis <= MAX_DELIVERY_DELAY_MILLIS

        /**
         * Build the PendingIntent that AlarmManager will deliver when the alarm fires.
         */
        fun buildPendingIntent(
            context: Context,
            alarmId: Int,
            hour: Int,
            minute: Int,
            triggerAtMillis: Long = INVALID_TRIGGER_AT,
        ): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_FIRE
                putExtra(EXTRA_ALARM_ID,     alarmId)
                putExtra(EXTRA_ALARM_HOUR,   hour)
                putExtra(EXTRA_ALARM_MINUTE, minute)
                putExtra(EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

            return PendingIntent.getBroadcast(context, alarmId, intent, flags)
        }
    }
}
