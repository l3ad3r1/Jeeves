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
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ButlerAiProviderEntryPoint {
    fun getButlerAiProvider(): ButlerAiProvider
}
