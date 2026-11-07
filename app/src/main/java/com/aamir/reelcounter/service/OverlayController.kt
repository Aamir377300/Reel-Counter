package com.aamir.reelcounter.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.aamir.reelcounter.data.OverlayPosition

/**
 * Single non-interactive TYPE_ACCESSIBILITY_OVERLAY pill:
 * - normal: just the reel number
 * - milestone: red pill "You have reached X reels" for a few seconds
 */
class OverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var textView: TextView? = null
    private var visible = false
    private var position: OverlayPosition = OverlayPosition.TOP_RIGHT
    private var lastCount: Int = 0
    private var showingMilestone = false

    private val restoreCountRunnable = Runnable {
        showingMilestone = false
        if (visible && lastCount >= 0) {
            applyCountStyle(lastCount)
        }
    }

    fun setPosition(pos: OverlayPosition) {
        if (position == pos) return
        position = pos
        if (visible) {
            hideCountOnly()
        }
    }

    fun showCount(reelCount: Int) {
        lastCount = reelCount
        if (showingMilestone) {
            // Don't overwrite the red banner mid-display.
            return
        }
        ensureVisible()
        applyCountStyle(reelCount)
    }

    fun hideCountOnly() {
        if (showingMilestone) return // keep red banner until timer ends
        hide()
    }

    fun hide() {
        mainHandler.removeCallbacks(restoreCountRunnable)
        showingMilestone = false
        val tv = textView ?: return
        if (visible) {
            try {
                windowManager.removeView(tv)
            } catch (_: Exception) {
                // Already removed.
            }
            visible = false
        }
    }

    fun destroy() {
        hide()
        textView = null
    }

    /** Red pill for ~3.5s, then restores the number. */
    fun showMilestone(reelCount: Int, durationMs: Long = 3_500L) {
        mainHandler.removeCallbacks(restoreCountRunnable)
        showingMilestone = true
        ensureVisible()
        val tv = textView ?: return
        tv.text = "You have reached $reelCount reels"
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        tv.background = roundedBg(0xE0DC2626.toInt(), 12f)
        tv.maxWidth = dp(280f)
        try {
            windowManager.updateViewLayout(tv, buildParams())
        } catch (_: Exception) {
            // Ignore.
        }
        mainHandler.postDelayed(restoreCountRunnable, durationMs)
    }

    private fun applyCountStyle(reelCount: Int) {
        val tv = textView ?: return
        tv.text = reelCount.toString()
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        tv.background = roundedBg(0xCC1F2937.toInt(), 20f)
        tv.maxWidth = Int.MAX_VALUE
        try {
            windowManager.updateViewLayout(tv, buildParams())
        } catch (_: Exception) {
            // Ignore.
        }
    }

    private fun ensureVisible() {
        var tv = textView
        if (tv == null) {
            tv = createView()
            textView = tv
        }
        if (!visible) {
            try {
                windowManager.addView(tv, buildParams())
                visible = true
            } catch (_: Exception) {
                // Token may be invalid briefly — recreate view and retry once.
                try {
                    tv = createView()
                    textView = tv
                    windowManager.addView(tv, buildParams())
                    visible = true
                } catch (_: Exception) {
                    visible = false
                }
            }
        }
    }

    private fun createView(): TextView {
        val padH = dp(14f)
        val padV = dp(8f)
        return TextView(context).apply {
            setPadding(padH, padV, padH, padV)
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = roundedBg(0xCC1F2937.toInt(), 20f)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            gravity = Gravity.CENTER
        }
    }

    private fun roundedBg(color: Int, radiusDp: Float): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }
    }

    private fun buildParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        val gravity = when (position) {
            OverlayPosition.TOP_LEFT -> Gravity.TOP or Gravity.START
            OverlayPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = dp(12f)
            y = dp(48f)
        }
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        ).toInt()
    }
}
