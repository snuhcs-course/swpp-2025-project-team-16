package com.fitquest.app.model

import java.util.Date

data class WorkoutPlan(
    val id: Int? = null,
    val date: String? = null,
    val exercises: List<Exercise> = emptyList(),
    val isCompleted: Boolean = false,
    val point: Int = 0,
    val feedback: String? = null,
    val startTime: String? = null,
    val finishTime: String? = null
)

data class WorkoutHistory(
    val id: Int? = null,
    val date: String,
    val exercises: List<Exercise>,
    val isCompleted: Boolean,
    val points: Int? = null,
    val feedback: String? = null,
    val completedAt: Date? = null,
    val aiScore: Int? = null
)