package com.jeeves.core.settings.ai

import android.content.Context

interface ButlerAiProvider {
    suspend fun generateMorningGreeting(
        weatherContext: String,
        timeContext: String,
        honorific: String,
        sassLevel: Int
    ): String?

    suspend fun generateBriefing(
        contextData: String,
        honorific: String,
        sassLevel: Int
    ): String?

    suspend fun preGenerateBriefing(context: Context)
}
