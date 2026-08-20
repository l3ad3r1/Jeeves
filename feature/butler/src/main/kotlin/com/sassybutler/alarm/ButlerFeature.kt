package com.sassybutler.alarm

import android.content.Context
import com.hermes.agent.data.backup.AlarmBackup
import com.hermes.agent.domain.agent.AgentFeature
import com.hermes.agent.domain.agent.BackupContribution
import com.hermes.agent.domain.agent.NavEntry
import com.hermes.agent.domain.tool.Tool
import com.sassybutler.alarm.tools.SetAlarmTool
import com.sassybutler.alarm.tools.TtsTool
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ButlerFeature @Inject constructor(
    private val setAlarmTool: SetAlarmTool,
    private val ttsTool: TtsTool,
    private val briefingComposer: BriefingComposer,
) : AgentFeature {
    override val id: String = "butler"
    override val name: String = "Sassy Butler"

    override fun tools(): List<Tool> = listOf(setAlarmTool, ttsTool)

    override fun promptFragment(): String =
        "You can manage alarms with set_alarm and speak responses with speak."

    override fun backupContributions(): List<BackupContribution> = listOf(
        BackupContribution("butler_alarms", "Alarms and schedules")
    )

    override fun entries(): List<NavEntry> = listOf(
        NavEntry(
            route = "butler_alarms",
            label = "Daybook",
            subtitle = "Alarms, weather & calendar",
            targetActivityClassName = "com.sassybutler.alarm.MainAlarmSetupActivity",
            intentAction = "com.hermes.agent.action.SET_ALARM",
        )
    )

    override fun habitInsight(context: Context): String? {
        return try {
            val alarms = AlarmStore.all(context)
            if (alarms.isEmpty()) return null
            val activeAlarms = alarms.filter { it.enabled }
            if (activeAlarms.isEmpty()) return null
            val formattedAlarms = activeAlarms.joinToString(", ") {
                "${String.format("%02d:%02d", it.hour, it.minute)} (days: ${it.days.joinToString("")})"
            }
            "User usually has alarms set for $formattedAlarms."
        } catch (_: Exception) {
            null
        }
    }

    override fun composeBriefingContext(context: Context): String? {
        return try {
            briefingComposer.composeContext(context)
        } catch (_: Exception) {
            null
        }
    }

    override fun exportAlarms(context: Context): List<AlarmBackup> {
        return try {
            AlarmStore.all(context).map {
                AlarmBackup(
                    id = it.id,
                    hour = it.hour,
                    minute = it.minute,
                    label = it.label,
                    enabled = it.enabled,
                    days = it.days,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun importAlarms(context: Context, alarms: List<AlarmBackup>): Int {
        var imported = 0
        try {
            val scheduler = AlarmScheduler(context)
            for (a in alarms) {
                runCatching {
                    val alarm = Alarm(
                        id = a.id,
                        hour = a.hour,
                        minute = a.minute,
                        label = a.label,
                        enabled = a.enabled,
                        days = a.days,
                    )
                    AlarmStore.upsert(context, alarm)
                    if (alarm.enabled) {
                        scheduler.schedule(alarm)
                    }
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.WRITE_CALENDAR
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        CalendarSyncManager.syncAlarmToCalendar(context, alarm)
                    }
                }.onSuccess {
                    imported++
                }
            }
        } catch (_: Exception) {
        }
        return imported
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ButlerFeatureModule {
    @Binds
    @IntoSet
    abstract fun bindButlerFeature(feature: ButlerFeature): AgentFeature
}
