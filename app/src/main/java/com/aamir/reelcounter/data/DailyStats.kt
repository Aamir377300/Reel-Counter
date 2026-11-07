package com.aamir.reelcounter.data

import androidx.room.Entity

@Entity(
    tableName = "daily_stats",
    primaryKeys = ["date", "app"]
)
data class DailyStats(
    val date: String,          // yyyy-MM-dd
    val app: TrackedApp,
    val reelCount: Int = 0,
    val scrollPixels: Long = 0L,
    val timeSpentMs: Long = 0L
)
