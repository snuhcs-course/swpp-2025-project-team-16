package com.fitquest.app.model

import org.threeten.bp.LocalDate

data class DailyHistoryItem (
    val date: LocalDate,
    val summaryText: String,
    val schedules: List<Schedule>,
    val sessions: List<Session>
)