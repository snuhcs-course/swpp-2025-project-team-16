package com.fitquest.app.data.remote

import com.fitquest.app.model.Session
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface SessionApiService {

    @POST("sessions/")
    suspend fun startSession(
        @Body body: Map<String, Any>
    ): Session

    @PATCH("sessions/{id}/")
    suspend fun endSession(
        @Path("id") id: Int,
        @Body body: Map<String, Any>
    ): Session
}
