package com.fitquest.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// -------------------- Data Classes --------------------

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    val name: String?,
    val level: Int?,
    val xp: Int?,
    val error: String?
)

data class EmailCheckResponse(
    val exists: Boolean
)

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class SignupResponse(
    val message: String,
    val token: String? = null
)


data class InitialCountRequest(
    val initial_reps: Int
)

data class InitialCountResponse(
    val message: String,
    val initial_reps: Int
)


// -------------------- Retrofit Interface --------------------

interface ApiService {

    // ✅ 회원가입
    @POST("accounts/signup/")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SignupResponse>

    // ✅ 로그인
    @POST("accounts/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // ✅ 이메일 중복 확인
    @POST("accounts/check_email/")
    suspend fun checkEmail(
        @Body request: Map<String, String>
    ): Response<EmailCheckResponse>

    // ✅ 초기 운동 개수 설정 (JWT 필요)
    @POST("accounts/update_initial_reps/")
    suspend fun updateInitialReps(
        @Header("Authorization") token: String, // "Bearer <JWT>"
        @Body body: InitialCountRequest
    ): Response<InitialCountResponse>
}
