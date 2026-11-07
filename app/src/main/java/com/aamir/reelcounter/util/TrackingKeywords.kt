package com.aamir.reelcounter.util

/**
 * Edit these when Instagram / YouTube update their view IDs.
 *
 * Prefer exact [INSTAGRAM_REELS_VIEW_IDS] / [YOUTUBE_SHORTS_VIEW_IDS] — those are
 * looked up with findAccessibilityNodeInfosByViewId (fast + reliable).
 * Hint strings are a fallback substring match during a deeper tree walk.
 *
 * Tip: Settings → Debug mode ON, then: adb logcat -s REEL_DEBUG
 */
object TrackingKeywords {

    val TRACKED_PACKAGES = setOf(
        "com.instagram.android",
        "com.google.android.youtube"
    )

    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    /**
     * Exact resource IDs that only appear on the Reels viewer
     * (NOT the bottom-nav clips_tab, which exists on every Instagram screen).
     */
    val INSTAGRAM_REELS_VIEW_IDS = listOf(
        "com.instagram.android:id/clips_viewer_container",
        "com.instagram.android:id/root_clips_layout",
        "com.instagram.android:id/clips_viewer_view_pager",
        "com.instagram.android:id/clips_video_container",
        "com.instagram.android:id/clips_viewer_video_layout"
    )

    val YOUTUBE_SHORTS_VIEW_IDS = listOf(
        "com.google.android.youtube:id/shorts_player",
        "com.google.android.youtube:id/reel_recycler",
        "com.google.android.youtube:id/shorts_container",
        "com.google.android.youtube:id/reel_player_page_container",
        "com.google.android.youtube:id/reel_watch_player"
    )

    /** Substring fallbacks for content-desc / odd ID variants. Avoid bare "clips" — matches nav tab. */
    val INSTAGRAM_REELS_HINTS = listOf(
        "clips_viewer",
        "root_clips",
        "clips_video",
        "clips_media",
        "reel by"
    )

    val YOUTUBE_SHORTS_HINTS = listOf(
        "shorts_player",
        "shorts_container",
        "reel_watch",
        "reel_recycler",
        "shorts"
    )

    fun viewIdsForPackage(packageName: String): List<String> = when (packageName) {
        INSTAGRAM_PACKAGE -> INSTAGRAM_REELS_VIEW_IDS
        YOUTUBE_PACKAGE -> YOUTUBE_SHORTS_VIEW_IDS
        else -> emptyList()
    }

    fun hintsForPackage(packageName: String): List<String> = when (packageName) {
        INSTAGRAM_PACKAGE -> INSTAGRAM_REELS_HINTS
        YOUTUBE_PACKAGE -> YOUTUBE_SHORTS_HINTS
        else -> emptyList()
    }
}
