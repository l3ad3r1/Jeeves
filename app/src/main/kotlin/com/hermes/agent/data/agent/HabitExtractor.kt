package com.hermes.agent.data.agent

import android.content.Context
import com.hermes.agent.domain.agent.AgentFeature
import com.hermes.agent.domain.repository.MemoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts habits and routines from modular [AgentFeature] contributors (e.g. Daybook alarms, Jotter notes)
 * and feeds them into the RAG memory pipeline.
 */
@Singleton
class HabitExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryRepository: MemoryRepository,
    private val features: Set<@JvmSuppressWildcards AgentFeature> = emptySet(),
) {
    suspend fun extractAndStoreHabits() {
        try {
            // This runs on every nightly consolidation. Without this sweep,
            // a near-identical "Habit insight" memory accumulates per night
            // and pollutes vector search within weeks. Habit insights are a
            // rolling snapshot, not history: replace, don't append.
            memoryRepository.observeMemories().first()
                .filter { it.content.startsWith(HABIT_PREFIX) }
                .forEach { memoryRepository.deleteMemory(it.id) }

            for (feature in features) {
                val insight = feature.habitInsight(context) ?: continue
                val habit = "$HABIT_PREFIX $insight"
                memoryRepository.addMemory(habit)
                Timber.tag("HabitExtractor").i("Extracted feature habit from %s: %s", feature.id, habit)
            }
        } catch (e: Exception) {
            Timber.tag("HabitExtractor").w(e, "Failed to extract habits")
        }
    }

    companion object {
        private const val HABIT_PREFIX = "Habit insight:"
    }
}
