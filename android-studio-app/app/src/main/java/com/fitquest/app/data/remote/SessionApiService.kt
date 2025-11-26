package com.fitquest.app.data.remote

import com.fitquest.app.model.EndSessionRequest
import com.fitquest.app.model.Session
import com.fitquest.app.model.StartSessionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

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