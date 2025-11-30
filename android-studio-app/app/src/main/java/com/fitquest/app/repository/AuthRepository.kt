package com.fitquest.app.repository

import android.content.Context
import com.fitquest.app.data.remote.AuthApiService
import com.fitquest.app.data.remote.TokenManager
import com.fitquest.app.model.login.EmailCheckResponse
import com.fitquest.app.model.login.InitialCountRequest
import com.fitquest.app.model.login.InitialCountResponse
import com.fitquest.app.model.login.LoginRequest
import com.fitquest.app.model.login.LoginResponse
import com.fitquest.app.model.login.SignupRequest
import com.fitquest.app.model.login.SignupResponse
import retrofit2.Response

class AuthRepository(private val service: AuthApiService, private val context: Context) {
    suspend fun signup(request: SignupRequest): Response<SignupResponse> =
        service.signup(request)

    suspend fun login(request: LoginRequest): Response<LoginResponse> =
        service.login(request)

    suspend fun checkEmail(request: Map<String, String>): Response<EmailCheckResponse> =
        service.checkEmail(request)

    suspend fun updateInitialReps(body: InitialCountRequest): Response<InitialCountResponse> {
        return service.updateInitialReps(body)
    }
}