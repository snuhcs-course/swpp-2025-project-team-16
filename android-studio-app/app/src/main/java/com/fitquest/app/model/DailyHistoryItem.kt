package com.fitquest.app.model

import org.threeten.bp.LocalDate

data class DailyHistoryItem (
    val date: LocalDate,              // 정렬용
    val dateLabel: String,            // UI용 ex. "Nov 18"
    val exercises: List<Schedule>
)