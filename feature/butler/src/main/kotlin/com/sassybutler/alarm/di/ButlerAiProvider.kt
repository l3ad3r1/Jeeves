package com.sassybutler.alarm.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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

    suspend fun preGenerateBriefing(context: android.content.Context)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ButlerAiProviderEntryPoint {
    fun getButlerAiProvider(): ButlerAiProvider
}
