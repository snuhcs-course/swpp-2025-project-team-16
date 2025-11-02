package com.fitquest.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

// ====== 기존 로그인/회원가입 ======
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    val name: String?,
    val error: String?
)

data class EmailCheckResponse(
    val exists: Boolean
)

data class SignupRequest(val name: String, val email: String, val password: String)
data class SignupResponse(val message: String)

// ====== 자세 평가용 추가 ======
data class EvaluatePostureRequest(
    val category: String,
    val image_base64: String
)

data class EvaluatePostureResponse(
    val status: String,
    val good_points: String,
    val improvement_points: String,
    val improvement_methods: String
)

interface ApiService {
    @POST("api/accounts/signup/")
    suspend fun signup(@Body request: Map<String, String>): Response<SignupResponse>

    @POST("api/accounts/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/accounts/check_email/")
    suspend fun checkEmail(@Body request: Map<String, String>): Response<EmailCheckResponse>

    // ====== 자세 평가(동적 URL 사용) ======
    // 예) fullUrl = "http://147.46.78.29:8004/evaluate_posture"
    @POST
    suspend fun evaluatePosture(
        @Url fullUrl: String,
        @Body request: EvaluatePostureRequest
    ): Response<EvaluatePostureResponse>
}
