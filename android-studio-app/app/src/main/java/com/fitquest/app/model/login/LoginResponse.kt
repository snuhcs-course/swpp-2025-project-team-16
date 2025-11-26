package com.fitquest.app.model.login

data class LoginResponse(
    val token: String?,
    val name: String?,
    val level: Int?,
    val xp: Int?,
    val error: String?
)