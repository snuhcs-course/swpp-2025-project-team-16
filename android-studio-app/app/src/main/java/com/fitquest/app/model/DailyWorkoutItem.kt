package com.fitquest.app.model

import java.time.LocalDate

data class DailyWorkoutItem(
    val date: LocalDate,
    val schedules: List<Schedule>
)
