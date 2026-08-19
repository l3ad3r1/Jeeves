package com.jeeves.core.settings.ai

import kotlinx.coroutines.flow.Flow

interface JotterAiProvider {
    fun generateSummary(noteContent: String): Flow<String>
    fun generateFlashcards(noteContent: String): Flow<String>
    fun generateAudioOverview(noteContent: String): Flow<String>
    fun chatWithNote(noteContent: String, userMessage: String): Flow<String>
}
