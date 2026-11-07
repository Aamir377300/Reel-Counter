package com.aamir.reelcounter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class OverlayPosition { TOP_LEFT, TOP_RIGHT }

data class SettingsState(
    val dailyLimit: Int = 100,
    val overlayEnabled: Boolean = true,
    val overlayPosition: OverlayPosition = OverlayPosition.TOP_RIGHT,
    val debugMode: Boolean = false,
    val setupCompleted: Boolean = false,
    val limitAlertShownDate: String = "",
    /** Last milestone (100/150/200…) already shown today; 0 = none. */
    val lastMilestoneShown: Int = 0,
    val milestoneDate: String = ""
)

class UserPreferences(private val context: Context) {

    private object Keys {
        val DAILY_LIMIT = intPreferencesKey("daily_limit")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val OVERLAY_POSITION = stringPreferencesKey("overlay_position")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val LIMIT_ALERT_DATE = stringPreferencesKey("limit_alert_date")
        val LAST_MILESTONE = intPreferencesKey("last_milestone_shown")
        val MILESTONE_DATE = stringPreferencesKey("milestone_date")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            dailyLimit = prefs[Keys.DAILY_LIMIT] ?: 100,
            overlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: true,
            overlayPosition = runCatching {
                OverlayPosition.valueOf(prefs[Keys.OVERLAY_POSITION] ?: OverlayPosition.TOP_RIGHT.name)
            }.getOrDefault(OverlayPosition.TOP_RIGHT),
            debugMode = prefs[Keys.DEBUG_MODE] ?: false,
            setupCompleted = prefs[Keys.SETUP_COMPLETED] ?: false,
            limitAlertShownDate = prefs[Keys.LIMIT_ALERT_DATE] ?: "",
            lastMilestoneShown = prefs[Keys.LAST_MILESTONE] ?: 0,
            milestoneDate = prefs[Keys.MILESTONE_DATE] ?: ""
        )
    }

    suspend fun setDailyLimit(value: Int) {
        context.dataStore.edit { it[Keys.DAILY_LIMIT] = value.coerceIn(1, 10_000) }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
    }

    suspend fun setOverlayPosition(position: OverlayPosition) {
        context.dataStore.edit { it[Keys.OVERLAY_POSITION] = position.name }
    }

    suspend fun setDebugMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEBUG_MODE] = enabled }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_COMPLETED] = completed }
    }

    suspend fun setLimitAlertShownDate(date: String) {
        context.dataStore.edit { it[Keys.LIMIT_ALERT_DATE] = date }
    }

    suspend fun setLastMilestoneShown(date: String, milestone: Int) {
        context.dataStore.edit {
            it[Keys.MILESTONE_DATE] = date
            it[Keys.LAST_MILESTONE] = milestone
        }
    }
}
