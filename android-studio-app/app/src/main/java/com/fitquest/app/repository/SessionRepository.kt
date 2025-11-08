package com.fitquest.app.repository

import com.fitquest.app.model.Session
import com.fitquest.app.data.remote.RetrofitClient

class SessionRepository {
    private val service = RetrofitClient.sessionApiService

    suspend fun startSession(activity: String, scheduleId: Int? = null): Session {
        val body = mutableMapOf<String, Any>("activity" to activity)
        scheduleId?.let { body["schedule_id"] = it }
        return service.startSession(body)
    }

    suspend fun endSession(sessionId: Int, repsCount: Int? = null, duration: Int? = null): Session {
        val body = mutableMapOf<String, Any>()
        repsCount?.let { body["reps_count"] = it }
        duration?.let { body["duration"] = it }
        return service.endSession(sessionId, body)
    }
}
