package com.l3ad3r1.octojotter.domain

import com.jeeves.core.settings.ai.JotterAiProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

typealias JotterAiProvider = com.jeeves.core.settings.ai.JotterAiProvider

@EntryPoint
@InstallIn(SingletonComponent::class)
interface JotterAiProviderEntryPoint {
    fun jotterAiProvider(): JotterAiProvider
}
