package com.fitquest.app.model

data class EndSessionRequest(
    val reps_count: Int? = null,
    val duration: Int? = null, // 초 단위
    val session_duration_seconds: Int
)