package com.sassybutler.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Alarm — one scheduled appointment.
 *
 * @param days Calendar.DAY_OF_WEEK values (SUNDAY=1..SATURDAY=7) the alarm
 *             repeats on. Empty = one-shot (next occurrence, then disabled).
 */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    val days: Set<Int>,
) {
    /** Next trigger time strictly after [after]. */
    fun nextTrigger(after: Calendar = Calendar.getInstance()): Long {
        val t = (after.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(8) {
            if (t.timeInMillis > after.timeInMillis &&
                (days.isEmpty() || t.get(Calendar.DAY_OF_WEEK) in days)
            ) return t.timeInMillis
            t.add(Calendar.DAY_OF_YEAR, 1)
        }
        return t.timeInMillis // unreachable for valid data
    }

    fun daysLabel(): String {
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        if (days == WEEKDAYS) return "Weekdays"
        if (days == WEEKEND) return "Weekend"
        return DAY_ORDER.filter { it in days }.joinToString(" · ") { DAY_NAMES[it]!! }
    }

    companion object {
        val WEEKDAYS = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                             Calendar.THURSDAY, Calendar.FRIDAY)
        val WEEKEND  = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        val DAY_ORDER = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                               Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
        val DAY_NAMES = mapOf(
            Calendar.MONDAY to "Mon", Calendar.TUESDAY to "Tue", Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu", Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat", Calendar.SUNDAY to "Sun",
        )
    }
}

/**
 * AlarmStore — JSON-in-SharedPreferences persistence for the alarm list.
 * Small data (a handful of alarms), so no database needed; everything is
 * synchronous and safe to call from the main thread.
 */
object AlarmStore {

    private const val PREFS = "alarms_store"
    private const val KEY = "alarms"

    fun all(context: Context): List<Alarm> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun get(context: Context, id: Int): Alarm? = all(context).firstOrNull { it.id == id }

    fun upsert(context: Context, alarm: Alarm) {
        val list = all(context).filter { it.id != alarm.id } + alarm
        save(context, list.sortedBy { it.hour * 60 + it.minute })
    }

    fun delete(context: Context, id: Int) =
        save(context, all(context).filter { it.id != id })

    fun nextId(context: Context): Int = (all(context).maxOfOrNull { it.id } ?: 0) + 1

    private fun save(context: Context, list: List<Alarm>) {
        val arr = JSONArray().apply { list.forEach { put(toJson(it)) } }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(a: Alarm) = JSONObject().apply {
        put("id", a.id); put("hour", a.hour); put("minute", a.minute)
        put("label", a.label); put("enabled", a.enabled)
        put("days", JSONArray(a.days.toList()))
    }

    private fun fromJson(o: JSONObject): Alarm {
        val days = o.optJSONArray("days") ?: JSONArray()
        return Alarm(
            id = o.getInt("id"), hour = o.getInt("hour"), minute = o.getInt("minute"),
            label = o.optString("label", "Appointment"),
            enabled = o.optBoolean("enabled", true),
            days = (0 until days.length()).map { days.getInt(it) }.toSet(),
        )
    }
}
