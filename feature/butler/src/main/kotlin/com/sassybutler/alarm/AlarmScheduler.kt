package com.sassybutler.alarm

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * AlarmScheduler — Thin wrapper around [AlarmManager] for scheduling
 * and cancelling exact alarms that fire even in Doze mode.
 *
 * Usage:
 *   val scheduler = AlarmScheduler(context)
 *   val alarmId   = scheduler.schedule(hour = 7, minute = 0)
 *   scheduler.cancel(alarmId)
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule an alarm for the next occurrence of [hour]:[minute].
     * If that time has already passed today, schedules for tomorrow.
     *
     * @return The alarm ID that can be used to cancel later.
     */
    fun schedule(hour: Int, minute: Int, alarmId: Int = hour * 100 + minute): Int {
        val triggerAt = nextTriggerTime(hour, minute)

        val pendingIntent = AlarmReceiver.buildPendingIntent(
            context   = context,
            alarmId   = alarmId,
            hour      = hour,
            minute    = minute,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted — using inexact alarm")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                return alarmId
            }
        }

        // setExactAndAllowWhileIdle fires in Doze mode — essential for alarms
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )

        val readableTime = formatTime(hour, minute)
        Log.i(TAG, "Alarm $alarmId scheduled for $readableTime (triggerAt=$triggerAt)")

        return alarmId
    }

    /** Schedule a stored [Alarm] for its next occurrence (respects repeat days). */
    fun schedule(alarm: Alarm): Int {
        val triggerAt = alarm.nextTrigger()
        val pendingIntent = AlarmReceiver.buildPendingIntent(
            context, alarm.id, alarm.hour, alarm.minute
        )
        setExact(triggerAt, pendingIntent)
        Log.i(TAG, "Alarm ${alarm.id} '${alarm.label}' scheduled (triggerAt=$triggerAt)")
        
        // Enqueue pre-generation worker 15 minutes before the alarm fires
        val preGenTime = triggerAt - 15 * 60 * 1000L
        if (preGenTime > System.currentTimeMillis()) {
            val delay = preGenTime - System.currentTimeMillis()
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<PreGenerateBriefingWorker>()
                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "pregen_briefing_${alarm.id}",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            Log.i(TAG, "Briefing pre-generation scheduled for $preGenTime (delay=$delay ms)")
        }

        return alarm.id
    }

    /** Re-arm every enabled stored alarm (after boot, or after edits). */
    fun rescheduleAll() {
        AlarmStore.all(context).filter { it.enabled }.forEach { schedule(it) }
    }

    /** Fire alarm [alarmId] again [minutes] from now (snooze). */
    fun snoozeIn(alarmId: Int, hour: Int, minute: Int, minutes: Int) {
        val pendingIntent = AlarmReceiver.buildPendingIntent(context, alarmId, hour, minute)
        setExact(System.currentTimeMillis() + minutes * 60_000L, pendingIntent)
        Log.i(TAG, "Alarm $alarmId snoozed for $minutes min")
    }

    private fun setExact(triggerAt: Long, pendingIntent: android.app.PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    /** Cancel a previously scheduled alarm by its ID. */
    fun cancel(alarmId: Int, hour: Int = 0, minute: Int = 0) {
        val pendingIntent = AlarmReceiver.buildPendingIntent(
            context  = context,
            alarmId  = alarmId,
            hour     = hour,
            minute   = minute,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("pregen_briefing_$alarmId")
        Log.i(TAG, "Alarm $alarmId cancelled")
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private fun nextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }
        // If time already passed today, fire tomorrow
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h  = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val m  = minute.toString().padStart(2, '0')
        val am = if (hour < 12) "AM" else "PM"
        return "$h:$m $am"
    }

    companion object {
        private const val TAG = "AlarmScheduler"
    }
}
