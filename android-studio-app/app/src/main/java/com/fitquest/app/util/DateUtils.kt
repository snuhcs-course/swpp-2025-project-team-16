package com.fitquest.app.util

import org.threeten.bp.LocalDate
import java.util.Locale

object DateUtils {

    /**
     * Formats an ISO 8601 date string ("YYYY-MM-DD") into a user-friendly format ("Oct 23").
     * Used for displaying date labels in the Journey and History screens.
     */
    fun formatDate(date: String): String {
        return try {
            val parsed = LocalDate.parse(date)
            val month = parsed.month.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercaseChar() }.take(3)
            "$month ${parsed.dayOfMonth}"
        } catch (e: Exception) {
            date
        }
    }
}