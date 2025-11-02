package com.fitquest.app.model

import java.util.Date

data class WorkoutPlan(
    val date: String,
    val exercises: List<Exercise>,
    val isCompleted: Boolean = false,
    val points: Int? = null,
    val feedback: String? = null
)

data class WorkoutHistory(
    val date: String,
    val exercises: List<Exercise>,
    val isCompleted: Boolean,
    val points: Int? = null,
    val feedback: String? = null,
    val completedAt: Date? = null,
    val aiScore: Int? = null
)
