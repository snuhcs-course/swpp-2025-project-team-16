package com.fitquest.app.model

import org.threeten.bp.LocalDate

data class DailyWorkoutItem(
    val date: LocalDate,
    val schedules: List<Schedule>
)
