package com.sassybutler.alarm.di

import com.jeeves.core.settings.ai.ButlerAiProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

typealias ButlerAiProvider = com.jeeves.core.settings.ai.ButlerAiProvider

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ButlerAiProviderEntryPoint {
    fun getButlerAiProvider(): ButlerAiProvider
}
