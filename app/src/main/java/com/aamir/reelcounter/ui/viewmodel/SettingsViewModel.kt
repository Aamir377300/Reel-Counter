package com.aamir.reelcounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aamir.reelcounter.ReelCounterApp
import com.aamir.reelcounter.data.OverlayPosition
import com.aamir.reelcounter.data.SettingsState
import com.aamir.reelcounter.data.StatsRepository
import com.aamir.reelcounter.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: UserPreferences,
    private val repo: StatsRepository
) : ViewModel() {

    val settings: StateFlow<SettingsState> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setDailyLimit(value: Int) = viewModelScope.launch { prefs.setDailyLimit(value) }
    fun setOverlayEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setOverlayEnabled(enabled) }
    fun setOverlayPosition(position: OverlayPosition) =
        viewModelScope.launch { prefs.setOverlayPosition(position) }
    fun setDebugMode(enabled: Boolean) = viewModelScope.launch { prefs.setDebugMode(enabled) }

    fun resetToday() = viewModelScope.launch { repo.resetToday() }
    fun clearAll() = viewModelScope.launch { repo.clearAll() }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = ReelCounterApp.get()
                return SettingsViewModel(app.preferences, app.repository) as T
            }
        }
    }
}
