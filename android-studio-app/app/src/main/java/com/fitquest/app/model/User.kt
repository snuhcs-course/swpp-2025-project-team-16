package com.fitquest.app.model

import java.util.Date

data class User(
    val id: String,
    val email: String,
    val username: String,
    val fitnessLevel: FitnessLevel,
    val createdAt: Date,
    val totalWorkouts: Int = 0,
    val streak: Int = 0,
    val missedDays: Int = 0,
    val totalPoints: Int = 0
)
