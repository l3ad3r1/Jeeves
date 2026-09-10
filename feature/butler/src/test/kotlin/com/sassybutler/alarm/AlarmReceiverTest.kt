package com.sassybutler.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmReceiverTest {

    private val enabledAlarm = Alarm(
        id = 42,
        hour = 7,
        minute = 30,
        label = "Wake up",
        enabled = true,
        days = emptySet(),
    )

    @Test
    fun `accepts only a stored enabled occurrence that is due`() {
        assertTrue(
            AlarmReceiver.isExpectedAlarmFire(
                alarm = enabledAlarm,
                alarmId = 42,
                hour = 7,
                minute = 30,
                triggerAtMillis = 100_000L,
                nowMillis = 101_000L,
            )
        )
    }

    @Test
    fun `rejects forged, disabled, edited, early, and stale occurrences`() {
        assertFalse(AlarmReceiver.isExpectedAlarmFire(null, 42, 7, 30, 100_000L, 101_000L))
        assertFalse(AlarmReceiver.isExpectedAlarmFire(enabledAlarm.copy(enabled = false), 42, 7, 30, 100_000L, 101_000L))
        assertFalse(AlarmReceiver.isExpectedAlarmFire(enabledAlarm, 41, 7, 30, 100_000L, 101_000L))
        assertFalse(AlarmReceiver.isExpectedAlarmFire(enabledAlarm, 42, 8, 30, 100_000L, 101_000L))
        assertFalse(AlarmReceiver.isExpectedAlarmFire(enabledAlarm, 42, 7, 30, 102_000L, 101_000L))
        assertFalse(AlarmReceiver.isExpectedAlarmFire(enabledAlarm, 42, 7, 30, 100_000L, 1_001_000L))
    }
}
