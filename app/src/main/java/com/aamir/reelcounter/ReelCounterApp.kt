package com.aamir.reelcounter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aamir.reelcounter.data.AppDatabase
import com.aamir.reelcounter.data.StatsRepository
import com.aamir.reelcounter.data.UserPreferences

class ReelCounterApp : Application() {

    // Lazy so AccessibilityService bind isn't blocked by Room open (was ANR-killing the service).
    val repository: StatsRepository by lazy {
        StatsRepository(AppDatabase.get(this).dailyStatsDao())
    }

    val preferences: UserPreferences by lazy {
        UserPreferences(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Channels only — keep startup cheap for a11y service cold starts.
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LIMIT,
                getString(R.string.notification_channel_limit),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    companion object {
        const val CHANNEL_LIMIT = "daily_limit"
        const val CHANNEL_SERVICE = "service_status"

        @Volatile
        private var instance: ReelCounterApp? = null

        fun get(): ReelCounterApp =
            instance ?: error("Application not initialized")
    }
}
