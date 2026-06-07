package dev.hasali.archery.ui.scoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hasali.archery.data.Score
import dev.hasali.archery.data.Session
import dev.hasali.archery.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

data class ScoringUiState(
    val session: Session? = null,
    val scores: List<Score> = emptyList(),
    val isLoading: Boolean = true,
)

class SessionScoringViewModel(
    private val sessionId: Int,
    private val repo: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScoringUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo
                .watchSession(sessionId)
                .collect { session ->
                    _uiState.update {
                        it.copy(session = session, scores = session.scores, isLoading = false)
                    }
                }
        }
    }

    fun addScore(score: Score) {
        val newIndex = _uiState.value.scores.size
        viewModelScope.launch {
            repo.insertScore(sessionId, newIndex, score.id)
        }
    }

    fun removeLastScore() {
        val current = _uiState.value.scores
        if (current.isEmpty()) return
        viewModelScope.launch {
            repo.deleteScore(sessionId, current.size - 1)
        }
    }

    fun updateStartTime(startTime: Instant) {
        viewModelScope.launch {
            repo.updateStartTime(sessionId, startTime)
        }
    }

    fun deleteSession(onComplete: () -> Unit) {
        viewModelScope.launch {
            repo.deleteSession(sessionId)
            onComplete()
        }
    }
}

class SessionScoringViewModelFactory(
    private val sessionId: Int,
    private val repo: SessionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionScoringViewModel(sessionId, repo) as T
}
