package com.fitquest.app.model

data class WorkoutItem(
    val name: String,
    val targetCount: Int?,
    val targetDuration: Int?,
    val status: String?
)