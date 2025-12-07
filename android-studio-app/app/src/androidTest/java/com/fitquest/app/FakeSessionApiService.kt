package com.fitquest.app


import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.model.EndSessionRequest
import com.fitquest.app.model.Session
import com.fitquest.app.model.StartSessionRequest
import org.threeten.bp.LocalDateTime
import retrofit2.Response

class FakeSessionApiService: SessionApiService {
    override suspend fun getSessions(): List<Session> {
        return listOf(Session(activity = "squat",id=0, repsCount = 5, createdAt = LocalDateTime.now().minusDays(1)))
    }

    override suspend fun startSession(request: StartSessionRequest): Response<Session> {
        return Response.success(Session(activity = "squat",id=0))
    }

    override suspend fun endSession(
        sessionId: Int,
        request: EndSessionRequest
    ): Response<Session> {
        return Response.success(Session(activity = "squat", repsCount = 5))
    }

}