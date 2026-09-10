package com.sassybutler.alarm

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hermes.agent.data.backup.AlarmBackup
import com.sassybutler.alarm.tools.SetAlarmTool
import com.sassybutler.alarm.tools.TtsTool
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ButlerAlarmRestoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val feature = ButlerFeature(
        setAlarmTool = mockk<SetAlarmTool>(relaxed = true),
        ttsTool = mockk<TtsTool>(relaxed = true),
        briefingComposer = mockk<BriefingComposer>(relaxed = true),
    )

    @Before
    fun clearAlarms() {
        AlarmStore.all(context).forEach { AlarmStore.delete(context, it.id) }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowOf(alarmManager).scheduledAlarms.forEach { alarmManager.cancel(it.operation) }
    }

    @Test
    fun `restoring a disabled alarm removes its previous alarm-manager schedule`() {
        val existing = Alarm(9, 8, 15, "Existing", enabled = true, days = emptySet())
        AlarmStore.upsert(context, existing)
        // This overload has the same AlarmManager occurrence but does not set up
        // briefing work, keeping the test focused on cancellation of the alarm itself.
        AlarmScheduler(context).schedule(hour = 8, minute = 15, alarmId = 9)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val expectedOperation = AlarmReceiver.buildPendingIntent(context, 9, 8, 15)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.any { it.operation == expectedOperation })

        feature.importAlarms(
            context,
            listOf(AlarmBackup(9, 8, 15, "Restored", enabled = false, days = emptySet())),
        )

        assertFalse(AlarmStore.get(context, 9)!!.enabled)
        assertFalse(shadowOf(alarmManager).scheduledAlarms.any { it.operation == expectedOperation })
    }
}
