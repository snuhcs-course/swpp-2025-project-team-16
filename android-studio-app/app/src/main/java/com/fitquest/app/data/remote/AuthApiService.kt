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
import retrofit2.http.Url

// ====== 기존 로그인/회원가입 ======
import retrofit2.http.Header

// -------------------- Data Classes --------------------

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

// -------------------- Retrofit Interface --------------------

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
        // @Header("Authorization") token: String, // "Bearer <JWT>"
        @Body body: InitialCountRequest
    ): Response<InitialCountResponse>
  
    // ====== 자세 평가(동적 URL 사용) ======
    // 예) fullUrl = "http://147.46.78.29:8004/evaluate_posture"
    @POST
    suspend fun evaluatePosture(
        @Url fullUrl: String,
        @Body request: EvaluatePostureRequest
    ): Response<EvaluatePostureResponse>
}
