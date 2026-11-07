package com.aamir.reelcounter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStats)

    @Query(
        """
        INSERT INTO daily_stats(date, app, reelCount, scrollPixels, timeSpentMs)
        VALUES(:date, :app, :reelCount, :scrollPixels, :timeSpentMs)
        ON CONFLICT(date, app) DO UPDATE SET
            reelCount = reelCount + :reelCount,
            scrollPixels = scrollPixels + :scrollPixels,
            timeSpentMs = timeSpentMs + :timeSpentMs
        """
    )
    suspend fun increment(
        date: String,
        app: TrackedApp,
        reelCount: Int,
        scrollPixels: Long,
        timeSpentMs: Long
    )

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun observeForDate(date: String): Flow<List<DailyStats>>

    @Query("SELECT * FROM daily_stats WHERE date = :date AND app = :app LIMIT 1")
    suspend fun getOne(date: String, app: TrackedApp): DailyStats?

    @Query(
        """
        SELECT * FROM daily_stats
        WHERE date >= :fromDate
        ORDER BY date DESC, app ASC
        """
    )
    fun observeSince(fromDate: String): Flow<List<DailyStats>>

    @Query(
        """
        SELECT * FROM daily_stats
        WHERE date >= :fromDate
        ORDER BY date DESC, app ASC
        """
    )
    suspend fun getSince(fromDate: String): List<DailyStats>

    @Query(
        """
        SELECT app,
               SUM(reelCount) AS reelCount,
               SUM(scrollPixels) AS scrollPixels,
               SUM(timeSpentMs) AS timeSpentMs
        FROM daily_stats
        GROUP BY app
        """
    )
    fun observeTotalsPerApp(): Flow<List<AppTotals>>

    @Query("DELETE FROM daily_stats WHERE date = :date")
    suspend fun deleteDate(date: String)

    @Query("DELETE FROM daily_stats")
    suspend fun clearAll()
}

/** Aggregate row returned by [DailyStatsDao.observeTotalsPerApp]. */
data class AppTotals(
    val app: TrackedApp,
    val reelCount: Int,
    val scrollPixels: Long,
    val timeSpentMs: Long
)
