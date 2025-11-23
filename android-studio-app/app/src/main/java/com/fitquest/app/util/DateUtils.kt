package com.fitquest.app.util

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.util.Locale

object DateUtils {
    fun formatDate(date: LocalDate): String {
        val month = date.month.name.lowercase(Locale.getDefault())
            .replaceFirstChar { it.uppercaseChar() }
            .take(3)
        return "$month ${date.dayOfMonth}"
    }

    fun formatTime(time: LocalTime): String {
        val hour = time.hour.toString().padStart(2, '0')
        val minute = time.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    fun formatTotalTime(totalSeconds: Float): String {
        return when {
            totalSeconds < 60 -> String.format("%.0fs", totalSeconds)
            totalSeconds < 3600 -> String.format("%.1fm", totalSeconds / 60f)
            else -> String.format("%.1fh", totalSeconds / 3600f)
        }
    }
}