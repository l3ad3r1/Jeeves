package com.l3ad3r1.octojotter.domain

import kotlinx.coroutines.flow.Flow
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

interface JotterAiProvider {
    fun generateSummary(noteContent: String): Flow<String>
    fun generateFlashcards(noteContent: String): Flow<String>
    fun generateAudioOverview(noteContent: String): Flow<String>
    fun chatWithNote(noteContent: String, userMessage: String): Flow<String>
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface JotterAiProviderEntryPoint {
    fun jotterAiProvider(): JotterAiProvider
}
