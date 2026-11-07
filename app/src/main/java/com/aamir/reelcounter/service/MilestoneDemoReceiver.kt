package com.aamir.reelcounter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * QA helper — leaves a pending milestone for the accessibility service to show:
 *   adb shell am broadcast -a com.aamir.reelcounter.DEMO_MILESTONE -p com.aamir.reelcounter --ei count 100
 */
class MilestoneDemoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReelTrackerService.ACTION_DEMO_MILESTONE) return
        val count = intent.getIntExtra("count", 100)
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PENDING, count)
            .apply()
        Log.i(TAG, "Queued demo milestone=$count")
        // Also try the live service overlay immediately if it is already up.
        ReelTrackerService.overlayForDemo()?.showMilestone(count)
    }

    companion object {
        private const val TAG = "MilestoneDemo"
        const val PREFS = "milestone_demo"
        const val KEY_PENDING = "pending_milestone"

        fun consumePending(context: Context): Int {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val value = prefs.getInt(KEY_PENDING, -1)
            if (value > 0) {
                prefs.edit().remove(KEY_PENDING).apply()
            }
            return value
        }
    }
}
