package com.fitquest.app.data.remote

import com.fitquest.app.model.login.EmailCheckResponse
import com.fitquest.app.model.login.InitialCountRequest
import com.fitquest.app.model.login.InitialCountResponse
import com.fitquest.app.model.login.LoginRequest
import com.fitquest.app.model.login.LoginResponse
import com.fitquest.app.model.login.SignupRequest
import com.fitquest.app.model.login.SignupResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("accounts/signup/")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SignupResponse>

    @POST("accounts/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("accounts/check_email/")
    suspend fun checkEmail(
        @Body request: Map<String, String>
    ): Response<EmailCheckResponse>

    @POST("accounts/update_initial_reps/")
    suspend fun updateInitialReps(
        @Body body: InitialCountRequest
    ): Response<InitialCountResponse>
}
