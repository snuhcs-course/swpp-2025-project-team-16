package com.fitquest.app.model

data class DailyWorkoutItem(
    val dateLabel: String,            // "Nov 18"
    val exercises: List<WorkoutItem>
)
