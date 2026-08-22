package dev.hasali.archery.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hasali.archery.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object ExportSucceeded : SettingsEvent

    data object ExportFailed : SettingsEvent
}

class SettingsViewModel(
    private val repo: SettingsRepository,
) : ViewModel() {
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun exportDatabase(destination: Uri) {
        viewModelScope.launch {
            val event = try {
                repo.exportDatabase(destination)
                SettingsEvent.ExportSucceeded
            } catch (_: Exception) {
                SettingsEvent.ExportFailed
            }
            _events.emit(event)
        }
    }
}

class SettingsViewModelFactory(
    private val repo: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repo) as T
}
