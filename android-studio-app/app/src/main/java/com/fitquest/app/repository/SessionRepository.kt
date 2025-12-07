package com.fitquest.app.repository

import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.model.EndSessionRequest
import com.fitquest.app.model.Session
import com.fitquest.app.model.StartSessionRequest
import retrofit2.HttpException
import java.io.IOException

class SessionRepository(private val service: SessionApiService) {

    suspend fun getSessions(): List<Session> =
        service.getSessions()

    suspend fun startSession(activity: String, scheduleId: Int?): Result<Session> {
        return try {
            val req = StartSessionRequest(activity = activity, schedule_id = scheduleId)
            val response = service.startSession(req)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                // HTTP 오류 (4xx, 5xx) 처리
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            // 네트워크 오류 처리
            Result.failure(e)
        }
    }

    suspend fun endSession(sessionId: Int, repsCount: Int?, duration: Int?, sessionDurationSeconds: Int): Result<Session> {
        return try {
            val req = EndSessionRequest(
                reps_count = repsCount,
                duration = duration,
                session_duration_seconds = sessionDurationSeconds
            )
            val response = service.endSession(sessionId, req)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
