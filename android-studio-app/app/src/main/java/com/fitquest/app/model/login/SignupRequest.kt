package com.fitquest.app.model.login

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)