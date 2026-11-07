package com.aamir.reelcounter.util

import android.content.Context
import android.util.DisplayMetrics
import kotlin.math.abs

object ScrollMetrics {

    /** Convert absolute pixel scroll distance to metres using screen dpi. */
    fun pixelsToMetres(pixels: Long, context: Context): Double {
        val dpi = context.resources.displayMetrics.densityDpi
            .takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
        // inches = px / dpi ; metres = inches * 0.0254
        return abs(pixels).toDouble() / dpi * 0.0254
    }

    fun formatMetres(metres: Double): String {
        return if (metres < 10) {
            String.format("%.1f m", metres)
        } else {
            String.format("%.0f m", metres)
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "$minutes min"
            else -> "${totalSec}s"
        }
    }
}
