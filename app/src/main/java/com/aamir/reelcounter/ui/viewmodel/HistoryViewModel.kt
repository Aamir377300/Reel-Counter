package com.aamir.reelcounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aamir.reelcounter.ReelCounterApp
import com.aamir.reelcounter.data.DailyStats
import com.aamir.reelcounter.data.StatsRepository
import com.aamir.reelcounter.util.DateHelpers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DayAggregate(
    val date: String,
    val reelCount: Int,
    val scrollPixels: Long,
    val timeSpentMs: Long
)

data class HistoryUiState(
    val daysRange: Int = 7,
    val days: List<DayAggregate> = emptyList()
)

class HistoryViewModel(repo: StatsRepository) : ViewModel() {

    private val range = MutableStateFlow(7)

    val uiState: StateFlow<HistoryUiState> = combine(
        range,
        repo.observeLastDays(30)
    ) { daysRange, rows ->
        val from = DateHelpers.daysAgo(daysRange - 1)
        val filtered = rows.filter { it.date >= from }
        val byDate = filtered.groupBy { it.date }.map { (date, list) ->
            DayAggregate(
                date = date,
                reelCount = list.sumOf { it.reelCount },
                scrollPixels = list.sumOf { it.scrollPixels },
                timeSpentMs = list.sumOf { it.timeSpentMs }
            )
        }.sortedByDescending { it.date }

        // Fill missing days so the chart has a continuous axis.
        val filled = (0 until daysRange).map { offset ->
            val d = DateHelpers.daysAgo(offset)
            byDate.find { it.date == d } ?: DayAggregate(d, 0, 0, 0)
        }.sortedBy { it.date }

        HistoryUiState(daysRange = daysRange, days = filled.reversed())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(days: Int) {
        range.value = days
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(ReelCounterApp.get().repository) as T
            }
        }
    }
}
