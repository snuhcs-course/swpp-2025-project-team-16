package com.fitquest.app.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class User (
    val id: Int,
    val name: String,
    val email: String,
    val xp: Int = 0,
    val level: Int = 1,

    @SerializedName("initial_reps")
    val initialReps: Int = 0,

    @SerializedName("total_reps")
    val totalReps: Int = 0,

    @SerializedName("total_time")
    val totalTime: Float = 0f,

    @SerializedName("last_session_at")
    val lastSessionAt: LocalDateTime? = null,

    val rank: Int = 0,
)