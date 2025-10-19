package com.aisportspt.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

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

interface ApiService {
    @POST("api/accounts/signup/")
    suspend fun signup(@Body request: Map<String, String>): Response<SignupResponse>

    @POST("api/accounts/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/accounts/check_email/")
    suspend fun checkEmail(@Body request: Map<String, String>): Response<EmailCheckResponse>

    @GET("/api/recommendation/")
    suspend fun getRecommendations(): RecommendationResponse

//    @GET("/api/recommendation/")
//    suspend fun getRecommendations(@Query("userId") userId: String): RecommendationResponse

//    @GET("/api/recommendation/")
//    suspend fun getRecommendations(@Header("Authorization") token: String): RecommendationResponse

}
