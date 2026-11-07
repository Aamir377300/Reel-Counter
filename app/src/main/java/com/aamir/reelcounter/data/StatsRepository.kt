package com.aamir.reelcounter.data

import com.aamir.reelcounter.util.DateHelpers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory batching: the accessibility service bumps counters here;
 * we flush to Room every ~5 s (and on destroy) so we don't write on every scroll.
 */
class StatsRepository(private val dao: DailyStatsDao) {

    data class Pending(
        var reelCount: Int = 0,
        var scrollPixels: Long = 0L,
        var timeSpentMs: Long = 0L
    )

    private val mutex = Mutex()
    private val pending = mutableMapOf<Pair<String, TrackedApp>, Pending>()
    private var currentDate: String = DateHelpers.today()

    fun observeToday(): Flow<List<DailyStats>> = dao.observeForDate(DateHelpers.today())

    fun observeLastDays(days: Int): Flow<List<DailyStats>> =
        dao.observeSince(DateHelpers.daysAgo(days - 1))

    fun observeTotals(): Flow<List<AppTotals>> = dao.observeTotalsPerApp()

    suspend fun getLastDays(days: Int): List<DailyStats> =
        dao.getSince(DateHelpers.daysAgo(days - 1))

    suspend fun addReel(app: TrackedApp, count: Int = 1) = mutex.withLock {
        ensureDayLocked()
        bucket(app).reelCount += count
    }

    suspend fun addScrollPixels(app: TrackedApp, pixels: Long) = mutex.withLock {
        ensureDayLocked()
        bucket(app).scrollPixels += pixels
    }

    suspend fun addTimeSpent(app: TrackedApp, ms: Long) = mutex.withLock {
        ensureDayLocked()
        bucket(app).timeSpentMs += ms
    }

    /** Flush in-memory deltas into Room. Safe to call often. */
    suspend fun flush() = mutex.withLock {
        ensureDayLocked()
        val snapshot = pending.toMap()
        pending.clear()
        snapshot.forEach { (key, delta) ->
            if (delta.reelCount == 0 && delta.scrollPixels == 0L && delta.timeSpentMs == 0L) return@forEach
            dao.increment(
                date = key.first,
                app = key.second,
                reelCount = delta.reelCount,
                scrollPixels = delta.scrollPixels,
                timeSpentMs = delta.timeSpentMs
            )
        }
    }

    suspend fun resetToday() {
        flush()
        dao.deleteDate(DateHelpers.today())
        mutex.withLock { pending.clear() }
    }

    suspend fun clearAll() {
        flush()
        dao.clearAll()
        mutex.withLock { pending.clear() }
    }

    /** Today's DB values plus any not-yet-flushed in-memory deltas (for overlay). */
    suspend fun todaySnapshot(app: TrackedApp): DailyStats {
        flush()
        val date = DateHelpers.today()
        return dao.getOne(date, app) ?: DailyStats(date = date, app = app)
    }

    suspend fun todayCombined(): Triple<Int, Long, Long> {
        flush()
        val rows = dao.getSince(DateHelpers.today()).filter { it.date == DateHelpers.today() }
        return Triple(
            rows.sumOf { it.reelCount },
            rows.sumOf { it.scrollPixels },
            rows.sumOf { it.timeSpentMs }
        )
    }

    private fun bucket(app: TrackedApp): Pending {
        val key = currentDate to app
        return pending.getOrPut(key) { Pending() }
    }

    /** If the calendar day rolled over, flush old-day pending under the old date key. */
    private suspend fun ensureDayLocked() {
        val today = DateHelpers.today()
        if (today != currentDate) {
            // Pending keys already store their date, so just update the pointer.
            currentDate = today
        }
    }
}
