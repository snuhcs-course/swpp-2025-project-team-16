package com.fitquest.app.data.remote

import com.fitquest.app.model.Session
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class StartSessionRequest(
    val activity: String,
    val schedule_id: Int? = null
)

data class EndSessionRequest(
    val reps_count: Int? = null,
    val duration: Int? = null // 초 단위
)

interface SessionApiService {

    @GET("/sessions/")
    suspend fun getSessions(): List<Session>

    @POST("/sessions/start/")
    suspend fun startSession(
        @Body request: StartSessionRequest
    ): Response<Session>

    @PATCH("/sessions/{session_id}/end/")
    suspend fun endSession(
        @Path("session_id") sessionId: Int,
        @Body request: EndSessionRequest
    ): Response<Session>
}