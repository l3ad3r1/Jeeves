package com.hermes.agent.ui.evolution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.agent.data.evolution.ReflectivePromptRefiner
import com.hermes.agent.domain.model.AgentRole
import com.hermes.agent.domain.repository.SupplementalPromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One agent role plus whatever learned guidance it currently carries. */
data class RoleRow(
    val role: AgentRole,
    val content: String,
    val version: String,
)

data class PromptRevisionRow(
    val id: String,
    val version: String,
    val note: String,
    val content: String,
    val replacedAt: Long,
)

data class PromptHistoryState(
    val role: AgentRole,
    val revisions: List<PromptRevisionRow>,
    val loading: Boolean,
)

sealed class PromptUiState {
    object Idle : PromptUiState()
    data class Running(val role: AgentRole) : PromptUiState()
    data class Proposal(val proposal: ReflectivePromptRefiner.Proposal) : PromptUiState()
    object Applied : PromptUiState()
    data class Restored(val version: String) : PromptUiState()
    data class NoChange(val message: String) : PromptUiState()
    data class Error(val message: String) : PromptUiState()
}

@HiltViewModel
class RefinePromptViewModel @Inject constructor(
    private val refiner: ReflectivePromptRefiner,
    private val promptRepository: SupplementalPromptRepository,
) : ViewModel() {

    private val _roles = MutableStateFlow<List<RoleRow>>(emptyList())
    val roles: StateFlow<List<RoleRow>> = _roles.asStateFlow()

    private val _state = MutableStateFlow<PromptUiState>(PromptUiState.Idle)
    val state: StateFlow<PromptUiState> = _state.asStateFlow()

    private val _history = MutableStateFlow<PromptHistoryState?>(null)
    val history: StateFlow<PromptHistoryState?> = _history.asStateFlow()

    init {
        loadRoles()
    }

    private fun loadRoles() {
        viewModelScope.launch {
            val stored = runCatching { promptRepository.getAll() }.getOrDefault(emptyMap())
            // Every role is listed, not just the ones with a row — a role with
            // no guidance yet is exactly the one worth refining first.
            _roles.value = AgentRole.entries.map { role ->
                val prompt = stored[role]
                RoleRow(
                    role = role,
                    content = prompt?.content.orEmpty(),
                    version = prompt?.version ?: "—",
                )
            }
        }
    }

    fun refine(role: AgentRole) {
        if (_state.value is PromptUiState.Running) return
        _state.value = PromptUiState.Running(role)
        viewModelScope.launch {
            _state.value = when (val outcome = refiner.refine(role)) {
                is ReflectivePromptRefiner.Outcome.Ready -> PromptUiState.Proposal(outcome.proposal)
                is ReflectivePromptRefiner.Outcome.NoChange -> PromptUiState.NoChange(outcome.reason)
                is ReflectivePromptRefiner.Outcome.Failed -> PromptUiState.Error(outcome.message)
            }
        }
    }

    fun apply(proposal: ReflectivePromptRefiner.Proposal) {
        viewModelScope.launch {
            runCatching { refiner.apply(proposal) }
                .onSuccess {
                    _state.value = PromptUiState.Applied
                    loadRoles()
                }
                .onFailure { _state.value = PromptUiState.Error(it.message ?: "Failed to apply") }
        }
    }

    /** Clear a role's learned guidance. The cleared text is still archived. */
    fun clear(role: AgentRole) {
        viewModelScope.launch {
            runCatching {
                promptRepository.put(role, content = "", version = "1.0.0", revisionNote = "Cleared")
            }
                .onSuccess {
                    _state.value = PromptUiState.Applied
                    loadRoles()
                }
                .onFailure { _state.value = PromptUiState.Error(it.message ?: "Failed to clear") }
        }
    }

    fun showHistory(role: AgentRole) {
        _history.value = PromptHistoryState(role, emptyList(), loading = true)
        viewModelScope.launch {
            val rows = runCatching { promptRepository.revisions(role) }
                .getOrDefault(emptyList())
                .map {
                    PromptRevisionRow(
                        id = it.id,
                        version = it.version,
                        note = it.note,
                        content = it.content,
                        replacedAt = it.replacedAt,
                    )
                }
            if (_history.value?.role == role) {
                _history.value = PromptHistoryState(role, rows, loading = false)
            }
        }
    }

    fun closeHistory() {
        _history.value = null
    }

    fun restore(revision: PromptRevisionRow) {
        val open = _history.value ?: return
        viewModelScope.launch {
            runCatching { promptRepository.restore(revision.id) }
                .onSuccess { restored ->
                    if (restored == null) {
                        _state.value = PromptUiState.Error("That revision is no longer available.")
                        _history.value = null
                    } else {
                        _state.value = PromptUiState.Restored(revision.version)
                        loadRoles()
                        showHistory(open.role)
                    }
                }
                .onFailure { _state.value = PromptUiState.Error(it.message ?: "Restore failed") }
        }
    }

    fun reset() {
        _state.value = PromptUiState.Idle
    }
}
