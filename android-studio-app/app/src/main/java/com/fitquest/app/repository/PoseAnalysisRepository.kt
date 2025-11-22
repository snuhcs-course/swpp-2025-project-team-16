package com.fitquest.app.repository

import com.fitquest.app.data.remote.PoseAnalysisApiService
import com.fitquest.app.data.remote.PoseUploadRequest
import com.fitquest.app.model.PoseAnalysis

class PoseAnalysisRepository(private val service: PoseAnalysisApiService) {
    suspend fun uploadPose(request: PoseUploadRequest): PoseAnalysis =
        service.uploadPose(request)

    suspend fun getPoseAnalyses(): List<PoseAnalysis> =
        service.getPoseAnalyses()

    suspend fun getPoseAnalysis(id: Int): PoseAnalysis =
        service.getPoseAnalysis(id)

    suspend fun getPoseAnalysesBySession(sessionId: Int): List<PoseAnalysis> =
        service.getPoseAnalysesBySession(sessionId)

    suspend fun getPoseAnalysesBySchedule(scheduleId: Int): List<PoseAnalysis> =
        service.getPoseAnalysesBySchedule(scheduleId)
}