package com.aamir.reelcounter.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.aamir.reelcounter.R
import com.aamir.reelcounter.ReelCounterApp
import com.aamir.reelcounter.data.SettingsState
import com.aamir.reelcounter.data.TrackedApp
import com.aamir.reelcounter.util.DateHelpers
import com.aamir.reelcounter.util.TrackingKeywords
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Counts Instagram Reels / YouTube Shorts scrolls and shows a live overlay.
 *
 * Keyword lists live in [TrackingKeywords] so you can tweak them after app updates.
 */
class ReelTrackerService : AccessibilityService() {

    // ── Tunables ──────────────────────────────────────────────────────────────
    companion object {
        private const val TAG = "ReelTracker"
        private const val DEBUG_TAG = "REEL_DEBUG"

        /** Fallback: count at most one reel per this window if index/scrollY is unavailable. */
        private const val REEL_DEBOUNCE_MS = 600L

        /** How often we flush in-memory counters to Room. */
        private const val DB_FLUSH_MS = 5_000L

        /** Re-walk the node tree at most this often when already on a tracked window. */
        private const val NODE_RECHECK_MS = 1_000L

        /**
         * Instagram nests clips_* IDs ~14+ levels deep — 8 was too shallow and
         * never found Reels. Prefer findByViewId first; walk is the fallback.
         */
        private const val MAX_NODE_DEPTH = 24

        private const val MIN_SCROLL_JUMP_FOR_REEL = 200

        /** First red milestone, then every +50 (100, 150, 200, …). */
        private const val MILESTONE_START = 100
        private const val MILESTONE_STEP = 50

        /** Don't hide the pill on brief null/system window blips after overlay add. */
        private const val LEAVE_DEBOUNCE_MS = 800L

        const val ACTION_DEMO_MILESTONE = "com.aamir.reelcounter.DEMO_MILESTONE"
        private const val NOTIF_TRACKING = 1003
        private const val NOTIF_DISABLED = 1002

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        private var demoOverlay: OverlayController? = null

        /** Used by [MilestoneDemoReceiver] for adb demos. */
        fun overlayForDemo(): OverlayController? = demoOverlay
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var overlay: OverlayController

    @Volatile private var settings = SettingsState()
    private var settingsJob: Job? = null

    // Tracking state
    @Volatile private var activeApp: TrackedApp? = null
    @Volatile private var inReelsOrShorts: Boolean = false
    @Volatile private var currentPackage: String? = null

    private var lastNodeCheckAt = 0L
    private var lastReelAt = 0L
    private var lastFromIndex = -1
    private var lastToIndex = -1
    private var lastScrollY = Int.MIN_VALUE

    private var sessionStartedAt = 0L
    private var overlayEnabledCached = true

    // Live totals for the overlay (today, both apps combined + per-app for limit)
    private var todayReels = 0
    private var todayPixels = 0L
    private var todayTimeMs = 0L

    private val flushRunnable = object : Runnable {
        override fun run() {
            serviceScope.launch {
                persistSessionTime()
                ReelCounterApp.get().repository.flush()
                refreshOverlayTotals()
            }
            mainHandler.postDelayed(this, DB_FLUSH_MS)
        }
    }

    private val leaveRunnable = Runnable { commitLeaveReels() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Guard against duplicate connect callbacks on some OEMs.
        if (isRunning && ::overlay.isInitialized) {
            Log.i(TAG, "Accessibility service already connected — skip re-init")
            showTrackingNotification()
            return
        }
        isRunning = true
        // Return fast — heavy init caused Oppo/Realme to ANR-kill this service.
        overlay = OverlayController(this)
        demoOverlay = overlay
        showTrackingNotification()
        Log.i(TAG, "Accessibility service connected")

        mainHandler.post {
            settingsJob?.cancel()
            settingsJob = serviceScope.launch {
                ReelCounterApp.get().preferences.settings.collect { s ->
                    settings = s
                    overlayEnabledCached = s.overlayEnabled
                    mainHandler.post {
                        if (::overlay.isInitialized) {
                            overlay.setPosition(s.overlayPosition)
                            if (!s.overlayEnabled) overlay.hide()
                        }
                    }
                }
            }
            serviceScope.launch { refreshOverlayTotals() }
            mainHandler.removeCallbacks(flushRunnable)
            mainHandler.postDelayed(flushRunnable, DB_FLUSH_MS)
        }
    }

    /** Ongoing notification — raises process importance so Oppo Athena is less likely to kill us. */
    private fun showTrackingNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val notification = NotificationCompat.Builder(this, ReelCounterApp.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("ReelCounter is on")
                .setContentText("Counting Reels & Shorts in the background")
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
            nm.notify(NOTIF_TRACKING, notification)
        } catch (t: Throwable) {
            Log.w(TAG, "tracking notification failed", t)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            handleEvent(event)
        } catch (t: Throwable) {
            // Never crash the a11y process.
            Log.e(TAG, "Event handling failed", t)
        }
    }

    private fun handleEvent(event: AccessibilityEvent) {
        // Pick up adb demo milestone requests (SharedPreferences handoff).
        val pendingMilestone = MilestoneDemoReceiver.consumePending(this)
        if (pendingMilestone > 0) {
            mainHandler.post { overlay.showMilestone(pendingMilestone) }
        }

        val pkg = event.packageName?.toString()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Adding/removing our overlay fires WINDOWS_CHANGED with our own
                // packageName — ignore those or the pill flickers then disappears.
                if (pkg == this.packageName) return
                onWindowChanged(pkg)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (pkg != null && TrackingKeywords.TRACKED_PACKAGES.contains(pkg)) {
                    maybeRecheckReels(pkg, force = false)
                    if (inReelsOrShorts && activeApp != null) {
                        onScrolled(event, activeApp!!)
                    }
                }
            }
        }
    }

    private fun onWindowChanged(eventPkg: String?) {
        // Prefer the active window package — WINDOWS_CHANGED often has a null/wrong pkg.
        // Do NOT recycle rootInActiveWindow here; OEMs are flaky about that.
        val activePkg = rootInActiveWindow?.packageName?.toString() ?: eventPkg

        currentPackage = activePkg
        when {
            activePkg == null || activePkg == this.packageName -> {
                // Overlay / system blip — ignore; don't hide the counter.
                return
            }
            !TrackingKeywords.TRACKED_PACKAGES.contains(activePkg) -> {
                scheduleLeaveReels()
            }
            else -> maybeRecheckReels(activePkg, force = true)
        }
    }

    private fun maybeRecheckReels(pkg: String, force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNodeCheckAt < NODE_RECHECK_MS) return
        lastNodeCheckAt = now

        val root = rootInActiveWindow
        val isReels = if (root != null) {
            try {
                val found = detectReelsOrShorts(root, pkg)
                if (settings.debugMode) dumpViewIds(root, pkg)
                found
            } catch (t: Throwable) {
                Log.e(TAG, "detect failed", t)
                // Keep previous state on errors instead of hiding.
                return
            }
        } else {
            // No root yet — don't treat as leave.
            return
        }

        if (isReels) {
            enterReels(pkg)
        } else {
            scheduleLeaveReels()
        }
    }

    private fun enterReels(pkg: String) {
        val app = TrackedApp.fromPackage(pkg) ?: return
        mainHandler.removeCallbacks(leaveRunnable)
        if (!inReelsOrShorts || activeApp != app) {
            // Switching apps or entering for the first time — close prior session timer.
            persistSessionTimeSync()
            activeApp = app
            inReelsOrShorts = true
            sessionStartedAt = System.currentTimeMillis()
            lastFromIndex = -1
            lastToIndex = -1
            lastScrollY = Int.MIN_VALUE
            Log.d(TAG, "Entered ${app.name} reels/shorts")
        }
        mainHandler.post { updateOverlayUi() }
    }

    private fun scheduleLeaveReels() {
        mainHandler.removeCallbacks(leaveRunnable)
        mainHandler.postDelayed(leaveRunnable, LEAVE_DEBOUNCE_MS)
    }

    private fun commitLeaveReels() {
        if (!inReelsOrShorts) {
            if (::overlay.isInitialized) overlay.hideCountOnly()
            return
        }
        persistSessionTimeSync()
        inReelsOrShorts = false
        activeApp = null
        sessionStartedAt = 0L
        if (::overlay.isInitialized) overlay.hideCountOnly()
        Log.d(TAG, "Left reels/shorts")
    }

    private fun onScrolled(event: AccessibilityEvent, app: TrackedApp) {
        // Scroll distance (absolute).
        val dy = abs(event.scrollDeltaY)
        if (dy > 0) {
            todayPixels += dy.toLong()
            serviceScope.launch {
                ReelCounterApp.get().repository.addScrollPixels(app, dy.toLong())
            }
        }

        val counted = detectPageChange(event)
        if (counted) {
            todayReels += 1
            serviceScope.launch {
                ReelCounterApp.get().repository.addReel(app, 1)
                maybeNotifyMilestone()
                maybeNotifyLimit()
            }
            updateOverlayUi()
        } else if (dy > 0) {
            updateOverlayUi()
        }
    }

    /**
     * Prefer index / scrollY jumps as a "new reel" signal; fall back to debounce.
     */
    private fun detectPageChange(event: AccessibilityEvent): Boolean {
        val now = System.currentTimeMillis()
        val from = event.fromIndex
        val to = event.toIndex
        val scrollY = event.scrollY

        var pageChanged = false

        if (from >= 0 && to >= 0) {
            if (lastFromIndex >= 0 && (from != lastFromIndex || to != lastToIndex)) {
                pageChanged = true
            }
            lastFromIndex = from
            lastToIndex = to
        } else if (scrollY != Int.MIN_VALUE && lastScrollY != Int.MIN_VALUE) {
            if (abs(scrollY - lastScrollY) >= MIN_SCROLL_JUMP_FOR_REEL) {
                pageChanged = true
            }
            lastScrollY = scrollY
        } else if (scrollY != Int.MIN_VALUE) {
            lastScrollY = scrollY
        }

        if (!pageChanged) {
            // Fallback: one count per debounce window while scrolling in reels.
            if (abs(event.scrollDeltaY) > 40 && now - lastReelAt >= REEL_DEBOUNCE_MS) {
                pageChanged = true
            }
        }

        if (pageChanged) {
            if (now - lastReelAt < REEL_DEBOUNCE_MS / 2) {
                // Guard against double-firing from both index jump and debounce.
                return false
            }
            lastReelAt = now
            return true
        }
        return false
    }

    // ── Node-tree detection ───────────────────────────────────────────────────

    private fun detectReelsOrShorts(root: AccessibilityNodeInfo, pkg: String): Boolean {
        // 1) Exact view-ID lookup (cheap, works regardless of tree depth).
        for (id in TrackingKeywords.viewIdsForPackage(pkg)) {
            val matches = try {
                root.findAccessibilityNodeInfosByViewId(id)
            } catch (_: Exception) {
                null
            }
            if (!matches.isNullOrEmpty()) {
                matches.forEach { recycleQuietly(it) }
                Log.d(TAG, "Detected via viewId=$id")
                return true
            }
        }

        // 2) Fallback: walk for hint substrings (deeper trees / renamed IDs).
        val hints = TrackingKeywords.hintsForPackage(pkg)
        if (hints.isEmpty()) return false
        return walkForHints(root, hints, depth = 0)
    }

    private fun walkForHints(
        node: AccessibilityNodeInfo?,
        hints: List<String>,
        depth: Int
    ): Boolean {
        if (node == null || depth > MAX_NODE_DEPTH) return false

        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val text = node.text?.toString()?.lowercase().orEmpty()
        val haystack = "$viewId $desc $text"

        if (hints.any { haystack.contains(it) }) return true

        for (i in 0 until node.childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                try {
                    if (walkForHints(child, hints, depth + 1)) return true
                } finally {
                    recycleQuietly(child)
                }
            }
        }
        return false
    }

    private fun dumpViewIds(root: AccessibilityNodeInfo, pkg: String) {
        val ids = mutableListOf<String>()
        collectIds(root, ids, 0)
        Log.d(DEBUG_TAG, "pkg=$pkg ids=${ids.take(60).joinToString()}")
    }

    private fun collectIds(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > MAX_NODE_DEPTH || out.size > 80) return
        node.viewIdResourceName?.let { if (it.isNotBlank()) out.add(it) }
        for (i in 0 until node.childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                try {
                    collectIds(child, out, depth + 1)
                } finally {
                    recycleQuietly(child)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun recycleQuietly(node: AccessibilityNodeInfo?) {
        // recycle() is a no-op on newer Android versions but still required
        // on older ones to avoid leaking a11y node handles.
        try {
            node?.recycle()
        } catch (_: Exception) {
            // Recycle can throw on already-recycled nodes on some OEMs.
        }
    }

    // ── Time / overlay / limit ────────────────────────────────────────────────

    private fun persistSessionTimeSync() {
        val app = activeApp ?: return
        if (sessionStartedAt <= 0L) return
        val elapsed = System.currentTimeMillis() - sessionStartedAt
        sessionStartedAt = System.currentTimeMillis()
        if (elapsed <= 0L) return
        todayTimeMs += elapsed
        serviceScope.launch {
            ReelCounterApp.get().repository.addTimeSpent(app, elapsed)
        }
    }

    private suspend fun persistSessionTime() {
        val app = activeApp ?: return
        if (!inReelsOrShorts || sessionStartedAt <= 0L) return
        val now = System.currentTimeMillis()
        val elapsed = now - sessionStartedAt
        sessionStartedAt = now
        if (elapsed > 0) {
            todayTimeMs += elapsed
            ReelCounterApp.get().repository.addTimeSpent(app, elapsed)
        }
    }

    private suspend fun refreshOverlayTotals() {
        val (reels, pixels, time) = ReelCounterApp.get().repository.todayCombined()
        todayReels = reels
        todayPixels = pixels
        todayTimeMs = time
        mainHandler.post { updateOverlayUi() }
    }

    private fun updateOverlayUi() {
        if (!inReelsOrShorts || !overlayEnabledCached) {
            overlay.hideCountOnly()
            return
        }
        // Overlay shows count only — metres / time stay on the Home screen.
        overlay.showCount(todayReels)
    }

    /**
     * Red banner at 100, 150, 200, … (once each per day).
     */
    private suspend fun maybeNotifyMilestone() {
        if (todayReels < MILESTONE_START) return
        val milestone = (todayReels / MILESTONE_STEP) * MILESTONE_STEP
        if (milestone < MILESTONE_START) return
        // Only fire exactly when we land on a milestone (not every scroll past it).
        if (todayReels != milestone) return

        val prefs = ReelCounterApp.get().preferences
        val s = prefs.settings.first()
        val today = DateHelpers.today()
        val last = if (s.milestoneDate == today) s.lastMilestoneShown else 0
        if (milestone <= last) return

        prefs.setLastMilestoneShown(today, milestone)
        mainHandler.post {
            if (overlayEnabledCached) overlay.showMilestone(milestone)
            vibrateOnce()
        }
    }

    private suspend fun maybeNotifyLimit() {
        val prefs = ReelCounterApp.get().preferences
        val s = prefs.settings.first()
        val today = DateHelpers.today()
        if (todayReels < s.dailyLimit) return
        if (s.limitAlertShownDate == today) return

        prefs.setLimitAlertShownDate(today)
        mainHandler.post {
            vibrateOnce()
            showLimitNotification(s.dailyLimit)
        }
    }

    private fun vibrateOnce() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(250)
                }
            }
        } catch (_: Exception) {
            // Vibration is best-effort.
        }
    }

    private fun showLimitNotification(limit: Int) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(this, ReelCounterApp.CHANNEL_LIMIT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.limit_notification_title))
            .setContentText(getString(R.string.limit_notification_text, limit))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(1001, notification)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(flushRunnable)
        mainHandler.removeCallbacks(leaveRunnable)
        settingsJob?.cancel()
        persistSessionTimeSync()
        serviceScope.launch {
            ReelCounterApp.get().repository.flush()
        }
        if (::overlay.isInitialized) overlay.destroy()
        demoOverlay = null
        serviceScope.cancel()
        isRunning = false

        // Let the user know tracking stopped so they can re-enable it.
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIF_TRACKING)
            val n = NotificationCompat.Builder(this, ReelCounterApp.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.service_disabled_title))
                .setContentText(getString(R.string.service_disabled_text))
                .setAutoCancel(true)
                .build()
            nm?.notify(NOTIF_DISABLED, n)
        } catch (_: Exception) {
            // Ignore.
        }
        super.onDestroy()
    }
}
