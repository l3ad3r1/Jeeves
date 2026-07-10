package com.sassybutler.alarm

import android.content.Context

/**
 * VoiceCatalog — the Kokoro v1.0 voices shipped in voices-v1.0.bin,
 * plus the user's persisted selection.
 *
 * All voices read the butler's English lines; non-English voices simply
 * do so with an accent (the voice embedding sets timbre/accent, the
 * PhonemeEncoder sets pronunciation). British voices are phonemized
 * with the en-gb lexicon, everything else with en-us.
 *
 * Grades are from the upstream Kokoro VOICES.md quality table.
 */
object VoiceCatalog {

    data class Voice(val name: String, val label: String)

    val VOICES: List<Voice> = listOf(
        // English (British) — the butler's homeland
        Voice("bm_george",   "🇬🇧 George — British male"),
        Voice("bm_fable",    "🇬🇧 Fable — British male"),
        Voice("bm_lewis",    "🇬🇧 Lewis — British male"),
        Voice("bm_daniel",   "🇬🇧 Daniel — British male"),
        Voice("bf_emma",     "🇬🇧 Emma — British female ★"),
        Voice("bf_isabella", "🇬🇧 Isabella — British female"),
        Voice("bf_alice",    "🇬🇧 Alice — British female"),
        Voice("bf_lily",     "🇬🇧 Lily — British female"),
        // English (American)
        Voice("af_heart",    "🇺🇸 Heart — American female ★★"),
        Voice("af_bella",    "🇺🇸 Bella — American female ★★"),
        Voice("af_nicole",   "🇺🇸 Nicole — American female ★ (whispery)"),
        Voice("af_sarah",    "🇺🇸 Sarah — American female"),
        Voice("af_aoede",    "🇺🇸 Aoede — American female"),
        Voice("af_kore",     "🇺🇸 Kore — American female"),
        Voice("af_alloy",    "🇺🇸 Alloy — American female"),
        Voice("af_nova",     "🇺🇸 Nova — American female"),
        Voice("af_jessica",  "🇺🇸 Jessica — American female"),
        Voice("af_river",    "🇺🇸 River — American female"),
        Voice("af_sky",      "🇺🇸 Sky — American female"),
        Voice("am_fenrir",   "🇺🇸 Fenrir — American male"),
        Voice("am_michael",  "🇺🇸 Michael — American male"),
        Voice("am_puck",     "🇺🇸 Puck — American male"),
        Voice("am_echo",     "🇺🇸 Echo — American male"),
        Voice("am_eric",     "🇺🇸 Eric — American male"),
        Voice("am_liam",     "🇺🇸 Liam — American male"),
        Voice("am_onyx",     "🇺🇸 Onyx — American male"),
        Voice("am_adam",     "🇺🇸 Adam — American male"),
        Voice("am_santa",    "🇺🇸 Santa — American male 🎅"),
        // Accented English (voices trained on other languages)
        Voice("jf_alpha",      "🇯🇵 Alpha — Japanese female"),
        Voice("jf_gongitsune", "🇯🇵 Gongitsune — Japanese female"),
        Voice("jf_nezumi",     "🇯🇵 Nezumi — Japanese female"),
        Voice("jf_tebukuro",   "🇯🇵 Tebukuro — Japanese female"),
        Voice("jm_kumo",       "🇯🇵 Kumo — Japanese male"),
        Voice("zf_xiaoxiao",   "🇨🇳 Xiaoxiao — Mandarin female"),
        Voice("zf_xiaobei",    "🇨🇳 Xiaobei — Mandarin female"),
        Voice("zf_xiaoni",     "🇨🇳 Xiaoni — Mandarin female"),
        Voice("zf_xiaoyi",     "🇨🇳 Xiaoyi — Mandarin female"),
        Voice("zm_yunjian",    "🇨🇳 Yunjian — Mandarin male"),
        Voice("zm_yunxi",      "🇨🇳 Yunxi — Mandarin male"),
        Voice("zm_yunxia",     "🇨🇳 Yunxia — Mandarin male"),
        Voice("zm_yunyang",    "🇨🇳 Yunyang — Mandarin male"),
        Voice("ef_dora",       "🇪🇸 Dora — Spanish female"),
        Voice("em_alex",       "🇪🇸 Alex — Spanish male"),
        Voice("em_santa",      "🇪🇸 Santa — Spanish male"),
        Voice("ff_siwis",      "🇫🇷 Siwis — French female ★"),
        Voice("hf_alpha",      "🇮🇳 Alpha — Hindi female"),
        Voice("hf_beta",       "🇮🇳 Beta — Hindi female"),
        Voice("hm_omega",      "🇮🇳 Omega — Hindi male"),
        Voice("hm_psi",        "🇮🇳 Psi — Hindi male"),
        Voice("if_sara",       "🇮🇹 Sara — Italian female"),
        Voice("im_nicola",     "🇮🇹 Nicola — Italian male"),
        Voice("pf_dora",       "🇧🇷 Dora — Portuguese female"),
        Voice("pm_alex",       "🇧🇷 Alex — Portuguese male"),
        Voice("pm_santa",      "🇧🇷 Santa — Portuguese male"),
    )

    // The selection now lives in the one settings store (which migrates the old
    // `voice_prefs` file on first touch); TtsEngine.DEFAULT_VOICE remains the default.
    fun selected(context: Context): String =
        com.jeeves.core.settings.JeevesSettings.voiceName(context, TtsEngine.DEFAULT_VOICE)

    fun select(context: Context, voiceName: String) {
        com.jeeves.core.settings.JeevesSettings.setVoiceName(context, voiceName)
    }

    fun indexOf(voiceName: String): Int =
        VOICES.indexOfFirst { it.name == voiceName }.coerceAtLeast(0)

    /** British voices are trained on en-gb phonemes; everything else en-us. */
    fun dialectFor(voiceName: String): PhonemeEncoder.Dialect =
        if (voiceName.startsWith("b")) PhonemeEncoder.Dialect.EN_GB
        else PhonemeEncoder.Dialect.EN_US
}
