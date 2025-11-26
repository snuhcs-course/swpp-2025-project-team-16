package com.fitquest.app.model.login

data class SignupResponse(
    val message: String,
    val token: String? = null
)