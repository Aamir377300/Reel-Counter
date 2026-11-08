package com.aamir.reelcounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aamir.reelcounter.ReelCounterApp
import com.aamir.reelcounter.data.DailyStats
import com.aamir.reelcounter.data.StatsRepository
import com.aamir.reelcounter.data.TrackedApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val instagram: DailyStats? = null,
    val youtube: DailyStats? = null,
    val totalReels: Int = 0,
    val totalPixels: Long = 0L,
    val totalTimeMs: Long = 0L
)

class HomeViewModel(repo: StatsRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repo.observeToday()
        .map { rows ->
            val ig = rows.find { it.app == TrackedApp.INSTAGRAM }
            val yt = rows.find { it.app == TrackedApp.YOUTUBE }
            HomeUiState(
                instagram = ig,
                youtube = yt,
                totalReels = rows.sumOf { it.reelCount },
                totalPixels = rows.sumOf { it.scrollPixels },
                totalTimeMs = rows.sumOf { it.timeSpentMs }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(ReelCounterApp.get().repository) as T
            }
        }
    }
}
