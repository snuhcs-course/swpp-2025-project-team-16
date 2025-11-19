package com.fitquest.app.model

import org.threeten.bp.LocalDate

data class DailyWorkoutItem(
    val date: LocalDate,              // 정렬용
    val dateLabel: String,            // "Nov 18"
    val exercises: List<WorkoutItem>
)
