package dev.hasali.archery.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hasali.archery.data.Session
import dev.hasali.archery.repository.NewSessionParams
import dev.hasali.archery.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionsUiState(
    val sessions: List<Session> = emptyList(),
    val totals: Map<Int, Int> = emptyMap(),
    val pbSessionIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
)

class SessionsViewModel(
    private val repo: SessionRepository,
) : ViewModel() {
    val uiState = repo
        .watchSessions()
        .map { buildUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionsUiState(isLoading = true),
        )

    fun deleteSession(id: Int) {
        viewModelScope.launch { repo.deleteSession(id) }
    }

    suspend fun createSession(params: NewSessionParams): Int = repo.createSession(params)

    private fun buildUiState(sessions: List<Session>): SessionsUiState {
        val totals = sessions.associate { s -> s.id to s.scores.sumOf { it.value } }
        val pbByRound = mutableMapOf<String, Int>()
        val pbIds = mutableSetOf<Int>()

        for (session in sessions) {
            val roundId = session.roundDetails.id ?: continue
            val total = totals[session.id] ?: 0
            if (total > (pbByRound[roundId] ?: -1)) {
                // Remove any previous PB session for this round
                pbIds.removeIf { id ->
                    sessions.find { it.id == id }?.roundDetails?.id == roundId
                }
                pbByRound[roundId] = total
                pbIds.add(session.id)
            }
        }

        return SessionsUiState(
            sessions = sessions,
            totals = totals,
            pbSessionIds = pbIds,
            isLoading = false,
        )
    }
}

class SessionsViewModelFactory(
    private val repo: SessionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionsViewModel(repo) as T
}
