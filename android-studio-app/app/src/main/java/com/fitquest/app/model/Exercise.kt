package com.fitquest.app.model

data class Exercise(
    val id: String,
    val name: String,
    val type: String,
    val reps: Int? = null,
    val duration: Int? = null,
    val sets: Int? = null,
    val emoji: String
)
