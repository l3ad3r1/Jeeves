package com.hermes.agent.data.rag

import com.hermes.agent.domain.agent.AgentFeature
import com.hermes.agent.domain.agent.RagDocument
import com.hermes.agent.domain.rag.Document
import com.hermes.agent.domain.rag.RagPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges modular feature documents (e.g. Octo Jotter notes) into the agent's RAG memory.
 */
@Singleton
class NoteIndexer @Inject constructor(
    private val ragPipeline: RagPipeline,
    private val features: Set<@JvmSuppressWildcards AgentFeature> = emptySet(),
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            val flows = features.map { it.observeRagDocuments() }
            val combinedFlow = if (flows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(flows) { lists -> lists.flatMap { it } }
            }

            combine(
                combinedFlow,
                ragPipeline.observeDocuments()
            ) { activeDocs, indexedDocs ->
                Pair(activeDocs, indexedDocs)
            }.collect { (activeDocs, indexedDocs) ->
                sync(activeDocs, indexedDocs)
            }
        }
    }

    private suspend fun sync(
        activeDocs: List<RagDocument>,
        indexedDocs: List<Document>
    ) {
        val indexedMap = indexedDocs.associateBy { it.id }
        val eligibleIds = activeDocs.map { it.id }.toSet()

        for (doc in indexedDocs) {
            if (doc.id.startsWith("note_") && doc.id !in eligibleIds) {
                Timber.tag("NoteIndexer").i("Evicting note from RAG: %s", doc.id)
                ragPipeline.deleteDocument(doc.id)
            }
        }

        for (item in activeDocs) {
            val existing = indexedMap[item.id]
            if (existing == null || existing.createdAt < item.createdAt) {
                Timber.tag("NoteIndexer").i("Indexing note %s ('%s')", item.id, item.title)
                if (existing != null) {
                    ragPipeline.deleteDocument(item.id)
                }
                val doc = Document(
                    id = item.id,
                    title = item.title,
                    sourceUri = item.sourceUri,
                    mimeType = item.mimeType,
                    content = item.content,
                    createdAt = item.createdAt,
                )
                try {
                    ragPipeline.ingest(doc)
                } catch (e: Exception) {
                    Timber.tag("NoteIndexer").e(e, "Failed to ingest note %s", item.id)
                }
            }
        }
    }
}
