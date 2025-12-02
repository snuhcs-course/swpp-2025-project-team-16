package com.fitquest.app.repository

import com.fitquest.app.data.remote.PoseAnalysisApiService
import com.fitquest.app.model.pose.PoseUploadRequest
import com.fitquest.app.model.pose.PoseAnalysis
import retrofit2.Response

class PoseAnalysisRepository(private val service: PoseAnalysisApiService) {
    suspend fun uploadPose(request: PoseUploadRequest): Response<PoseAnalysis> =
        service.uploadPose(request)
}