package com.fitquest.app.model

data class DailyHistoryItem (
    val dateLabel: String,            // "Nov 18"
    val exercises: List<Schedule>
)