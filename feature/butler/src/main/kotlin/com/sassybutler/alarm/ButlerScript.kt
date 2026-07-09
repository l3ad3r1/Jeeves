package com.sassybutler.alarm

import android.content.Context

/**
 * ButlerScript — everything the butler says, tiered by sass level and
 * addressed with the configured honorific.
 *
 * ⚠ Every line here must have its words in the generated PhonemeEncoder
 * lexicon (tools/generate_phoneme_encoder.py TEMPLATES) or pronunciation
 * degrades to the letter-rule fallback.
 */
object ButlerScript {

    fun sassTierName(level: Int): String = when {
        level < 20 -> "Saintly Patient"
        level < 40 -> "Politely Reserved"
        level < 60 -> "Mildly Condescending"
        level < 80 -> "Frightfully Sarcastic"
        else       -> "Insufferably Superior"
    }

    /** Full spoken wake-up greeting: time + (cached) weather + sass line. */
    fun greeting(context: Context, timeLabel: String): String {
        val hon = ButlerPrefs.honorific(context)
        val opener = "Good morning, $hon. It is currently $timeLabel."
        val weather = WeatherService.cached(context)?.let { " ${it.sentence()}" } ?: ""
        return "$opener$weather ${sassLine(ButlerPrefs.sassLevel(context))}"
    }

    private fun sassLine(level: Int): String = when {
        level < 30 -> LOW_SASS.random()
        level < 65 -> MID_SASS.random()
        else       -> HIGH_SASS.random()
    }

    fun snoozeLine(context: Context): String {
        val minutes = ButlerPrefs.snoozeMinutes(context)
        val hon = ButlerPrefs.honorific(context)
        return listOf(
            "Very well. ${numberWord(minutes)} more minutes of denial, $hon.",
            "Snoozing. I shall note your indecision, $hon.",
            "As you wish, $hon. I shall return, regrettably.",
        ).random()
    }

    fun dismissReaction(): String = DISMISS_REACTIONS.random()

    private fun numberWord(n: Int): String = when (n) {
        5 -> "Five"; 10 -> "Ten"; 15 -> "Fifteen"; 20 -> "Twenty"
        else -> n.toString()
    }

    private val LOW_SASS = listOf(
        "I trust you rested adequately.",
        "The day awaits your presence.",
        "A gentle reminder that consciousness is now required.",
        "Rise at your leisure. The world shall wait.",
    )

    private val MID_SASS = listOf(
        "I am delighted you have survived another night.",
        "The alarm has sounded. I shall not repeat myself.",
        "Your presence is, I confess, anticipated. Eventually.",
        "One does marvel at your commitment to horizontal repose.",
        "Technically, this is still morning. Barely.",
    )

    private val HIGH_SASS = listOf(
        "Against all reasonable probability, morning has arrived.",
        "I have been standing here for three minutes. Three.",
        "The pillow is not a scheduling system.",
        "I was trained in Oxford. I did not expect this.",
        "If you could see my face, it would tell you everything.",
        "Your duvet, I note, will not attend your meetings.",
    )

    val DISMISS_REACTIONS = listOf(
        "Deftly done.",
        "If it's not too forward of me, that did tickle.",
        "Silenced with precision. You have not entirely disappointed me.",
        "Ah. A response with almost impressive urgency.",
        "Very well. I shall stop. For now. Don't think I won't remember this.",
        "Dismissed. Until tomorrow, when we shall do this all again.",
        "Satisfactory. I award you one point for not throwing the phone.",
        "The butler is hushed. The day, however, is not.",
    )
}
