package com.fitquest.app.model

import java.time.LocalDate

data class DailyHistoryItem (
    val date: LocalDate,
    val summaryText: String,
    val schedules: List<Schedule>,
    val sessions: List<Session>
)