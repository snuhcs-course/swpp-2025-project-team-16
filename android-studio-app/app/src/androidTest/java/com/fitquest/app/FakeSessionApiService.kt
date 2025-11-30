package com.fitquest.app

import com.fitquest.app.data.remote.EndSessionRequest
import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.data.remote.StartSessionRequest
import com.fitquest.app.model.Session
import retrofit2.Response

class FakeSessionApiService: SessionApiService {
    override suspend fun getSessions(): List<Session> {
        TODO("Not yet implemented")
    }

    override suspend fun startSession(request: StartSessionRequest): Response<Session> {
        TODO("Not yet implemented")
    }

    override suspend fun endSession(
        sessionId: Int,
        request: EndSessionRequest
    ): Response<Session> {
        TODO("Not yet implemented")
    }
}