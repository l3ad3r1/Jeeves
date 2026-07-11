package com.hermes.agent.domain.tool

import com.hermes.agent.data.llm.ToolCall
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolConfirmationService @Inject constructor() {
    private var pendingDeferred: CompletableDeferred<Boolean>? = null
    
    private val _pendingRequest = MutableStateFlow<ToolCall?>(null)
    val pendingRequest: StateFlow<ToolCall?> = _pendingRequest

    suspend fun awaitConfirmation(call: ToolCall): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingDeferred = deferred
        _pendingRequest.value = call
        try {
            // Deny after a timeout rather than waiting forever: turns can run
            // with NO confirmation UI attached (Telegram connector, local API
            // server, cron worker), and an unanswered await would hang that
            // turn permanently. Denial is the safe default for a tool that
            // asked for confirmation.
            return kotlinx.coroutines.withTimeoutOrNull(CONFIRMATION_TIMEOUT_MS) {
                deferred.await()
            } ?: false
        } finally {
            _pendingRequest.value = null
            pendingDeferred = null
        }
    }

    fun submitConfirmation(approved: Boolean) {
        pendingDeferred?.complete(approved)
    }

    companion object {
        const val CONFIRMATION_TIMEOUT_MS = 60_000L
    }
}
