package com.fitquest.app.model

data class StartSessionRequest(
    val activity: String,
    val schedule_id: Int? = null
)