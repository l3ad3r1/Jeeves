package com.sassybutler.alarm

import android.content.Context
import android.content.SharedPreferences

/**
 * ButlerPrefs — user preferences for the butler's service.
 * (The voice choice itself lives in [VoiceCatalog]'s prefs.)
 */
object ButlerPrefs {

    private const val PREFS = "butler_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** "Sir" | "Madam" | "Boss" */
    fun honorific(context: Context): String =
        prefs(context).getString("honorific", "Sir") ?: "Sir"

    fun setHonorific(context: Context, value: String) =
        prefs(context).edit().putString("honorific", value).apply()

    /** 0–100; picks the wake-line tier. */
    fun sassLevel(context: Context): Int =
        prefs(context).getInt("sass_level", 45)

    fun setSassLevel(context: Context, value: Int) =
        prefs(context).edit().putInt("sass_level", value.coerceIn(0, 100)).apply()

    fun snoozeMinutes(context: Context): Int =
        prefs(context).getInt("snooze_minutes", 10)

    fun setSnoozeMinutes(context: Context, value: Int) =
        prefs(context).edit().putInt("snooze_minutes", value).apply()

    /** Play the birds-chirping intro before the spoken greeting. */
    fun birdsIntro(context: Context): Boolean =
        prefs(context).getBoolean("birds_intro", true)

    fun setBirdsIntro(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("birds_intro", value).apply()

    /**
     * Master switch for the butler's TTS voice. When off, the alarm loops
     * the birdsong as its wake sound and stays silent through dismiss/snooze.
     */
    fun voiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean("voice_enabled", true)

    fun setVoiceEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("voice_enabled", value).apply()

    fun haptics(context: Context): Boolean =
        prefs(context).getBoolean("haptics", false)

    fun setHaptics(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("haptics", value).apply()

    /** The butler comments when you snooze. */
    fun snoozeCommentary(context: Context): Boolean =
        prefs(context).getBoolean("snooze_commentary", true)

    fun setSnoozeCommentary(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("snooze_commentary", value).apply()
}
