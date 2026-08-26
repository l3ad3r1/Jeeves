package com.sassybutler.alarm

import android.content.Context
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefingComposer @Inject constructor() {

    fun composeContext(context: Context): String {
        val sb = StringBuilder()

        // Weather
        if (ButlerPrefs.briefingWeather(context)) {
            val weather = WeatherService.cached(context)?.sentence()
            if (weather != null) {
                sb.append("Weather: ").append(weather).append("\n\n")
            }
        }

        // Calendar
        if (ButlerPrefs.briefingCalendar(context)) {
            val events = CalendarSyncManager.todayEvents(context)
            if (events.isNotEmpty()) {
                sb.append("Calendar Events Today:\n")
                events.forEach { event ->
                    val timeLabel = if (event.allDay) "All Day" else {
                        val cal = Calendar.getInstance().apply { timeInMillis = event.startMillis }
                        val h = cal.get(Calendar.HOUR_OF_DAY)
                        val m = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
                        "$h:$m"
                    }
                    sb.append("- ").append(event.title).append(" at ").append(timeLabel).append("\n")
                }
                sb.append("\n")
            } else {
                sb.append("Calendar Events Today: None.\n\n")
            }
        }

        return sb.toString()
    }
}
