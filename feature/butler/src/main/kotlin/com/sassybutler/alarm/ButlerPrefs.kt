package com.sassybutler.alarm

import android.content.Context
import com.jeeves.core.settings.JeevesSettings

/**
 * ButlerPrefs — user preferences for the butler's service.
 * (The voice choice itself lives in [VoiceCatalog].)
 *
 * Butler is an integrated part of Jeeves, so these no longer live in a private
 * `butler_prefs` file: every accessor delegates to [JeevesSettings], the one settings store,
 * which migrates the old file on first touch. Defaults and the 0–100 clamp on sass level are
 * unchanged, and the public API is identical — so the alarm wake path
 * (`AlarmForegroundService`, `AudioEngine`, `ButlerScript`, `AlarmActivity`) still reads these
 * synchronously and needed no edits.
 */
object ButlerPrefs {

    /** "Sir" | "Madam" | "Boss" */
    fun honorific(context: Context): String = JeevesSettings.honorific(context)

    fun setHonorific(context: Context, value: String) = JeevesSettings.setHonorific(context, value)

    /** 0–100; picks the wake-line tier. */
    fun sassLevel(context: Context): Int = JeevesSettings.sassLevel(context)

    fun setSassLevel(context: Context, value: Int) = JeevesSettings.setSassLevel(context, value)

    fun snoozeMinutes(context: Context): Int = JeevesSettings.snoozeMinutes(context)

    fun setSnoozeMinutes(context: Context, value: Int) =
        JeevesSettings.setSnoozeMinutes(context, value)

    /** Play the birds-chirping intro before the spoken greeting. */
    fun birdsIntro(context: Context): Boolean = JeevesSettings.birdsIntro(context)

    fun setBirdsIntro(context: Context, value: Boolean) =
        JeevesSettings.setBirdsIntro(context, value)

    /**
     * Master switch for the butler's TTS voice. When off, the alarm loops
     * the birdsong as its wake sound and stays silent through dismiss/snooze.
     */
    fun voiceEnabled(context: Context): Boolean = JeevesSettings.voiceEnabled(context)

    fun setVoiceEnabled(context: Context, value: Boolean) =
        JeevesSettings.setVoiceEnabled(context, value)

    fun haptics(context: Context): Boolean = JeevesSettings.haptics(context)

    fun setHaptics(context: Context, value: Boolean) = JeevesSettings.setHaptics(context, value)

    /** The butler comments when you snooze. */
    fun snoozeCommentary(context: Context): Boolean = JeevesSettings.snoozeCommentary(context)

    fun setSnoozeCommentary(context: Context, value: Boolean) =
        JeevesSettings.setSnoozeCommentary(context, value)

    fun preGeneratedBriefing(context: Context): String? = JeevesSettings.preGeneratedBriefing(context)
    fun setPreGeneratedBriefing(context: Context, value: String?) = JeevesSettings.setPreGeneratedBriefing(context, value)

    fun preGeneratedBriefingTimestamp(context: Context): Long = JeevesSettings.preGeneratedBriefingTimestamp(context)
    fun setPreGeneratedBriefingTimestamp(context: Context, value: Long) = JeevesSettings.setPreGeneratedBriefingTimestamp(context, value)

    fun briefingCalendar(context: Context): Boolean = JeevesSettings.briefingCalendar(context)
    fun briefingWeather(context: Context): Boolean = JeevesSettings.briefingWeather(context)
    fun briefingTodos(context: Context): Boolean = JeevesSettings.briefingTodos(context)
    fun briefingNotes(context: Context): Boolean = JeevesSettings.briefingNotes(context)
    fun briefingHeadlines(context: Context): Boolean = JeevesSettings.briefingHeadlines(context)
}
