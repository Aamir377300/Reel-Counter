package com.aamir.reelcounter.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object DateHelpers {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(ISO)

    fun daysAgo(days: Int): String = LocalDate.now().minusDays(days.toLong()).format(ISO)

    fun formatDisplay(isoDate: String): String {
        return try {
            LocalDate.parse(isoDate, ISO)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatShort(isoDate: String): String {
        return try {
            LocalDate.parse(isoDate, ISO)
                .format(DateTimeFormatter.ofPattern("MMM d"))
        } catch (_: Exception) {
            isoDate
        }
    }
}
