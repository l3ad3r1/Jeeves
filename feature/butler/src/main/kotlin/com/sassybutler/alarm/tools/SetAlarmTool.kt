package com.sassybutler.alarm.tools

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.hermes.agent.domain.tool.ParameterType
import com.hermes.agent.domain.tool.Tool
import com.hermes.agent.domain.tool.ToolDescriptor
import com.hermes.agent.domain.tool.ToolParameter
import com.hermes.agent.domain.tool.ToolResult
import com.sassybutler.alarm.Alarm
import com.sassybutler.alarm.AlarmScheduler
import com.sassybutler.alarm.AlarmStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetAlarmTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmScheduler: AlarmScheduler,
) : Tool {

    override val descriptor = ToolDescriptor(
        name = "set_alarm",
        description = "Set an alarm clock that wakes the user at a given time, using the " +
            "Sassy Butler alarm app. Use this for 'wake me at 7am' or 'set an alarm for 6:30'. " +
            "The alarm fires once, at the next occurrence of that time. This is NOT for " +
            "recurring background tasks — use the 'scheduler' tool for those.",
        parameters = listOf(
            ToolParameter(
                name = "hour",
                type = ParameterType.INTEGER,
                description = "Hour of day on a 24-hour clock, 0-23.",
            ),
            ToolParameter(
                name = "minute",
                type = ParameterType.INTEGER,
                description = "Minute of the hour, 0-59.",
            ),
            ToolParameter(
                name = "label",
                type = ParameterType.STRING,
                description = "Optional short label shown when the alarm fires, e.g. 'Standup'.",
                required = false,
            ),
        ),
        category = "productivity",
        capabilities = setOf("notes_and_reminders"),
        requiresConfirmation = false,
    )

    override suspend fun execute(arguments: Map<String, JsonElement>): ToolResult {
        val hour = arguments["hour"]?.extractInt()
            ?: return ToolResult.error("missing or non-numeric parameter: hour")
        val minute = arguments["minute"]?.extractInt()
            ?: return ToolResult.error("missing or non-numeric parameter: minute")

        if (hour !in 0..23) return ToolResult.error("hour must be 0-23, got $hour")
        if (minute !in 0..59) return ToolResult.error("minute must be 0-59, got $minute")

        val label = arguments["label"]?.extractString()?.takeIf { it.isNotBlank() } ?: "Alarm"

        return try {
            val alarm = Alarm(
                id = AGENT_ALARM_ID_BASE + hour * 100 + minute,
                hour = hour,
                minute = minute,
                label = label,
                enabled = true,
                days = emptySet(),
            )
            AlarmStore.upsert(context, alarm)
            alarmScheduler.schedule(alarm)

            val time = "%02d:%02d".format(hour, minute)
            val exact = canScheduleExact()
            val caveat = if (exact) "" else
                " (exact-alarm permission is not granted, so it may fire a few minutes late)"
            ToolResult.ok("alarm \"$label\" set for $time$caveat")
        } catch (e: Exception) {
            ToolResult.error("could not set alarm: ${e.message}")
        }
    }

    private fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun JsonElement.extractString(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement.extractInt(): Int? =
        (this as? JsonPrimitive)?.contentOrNull?.trim()?.toIntOrNull()

    private companion object {
        const val AGENT_ALARM_ID_BASE = 10_000
    }
}
