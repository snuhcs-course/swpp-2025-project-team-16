package com.fitquest.app.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class Session(
    val id: Int? = null,
    val user: Int? = null,
    val activity: String,

    @SerializedName("reps_count")
    val repsCount: Int? = null,

    val duration: Int? = null, // 초 단위
    val schedule: Int? = null,

    @SerializedName("created_at")
    val createdAt: LocalDateTime? = null
)
