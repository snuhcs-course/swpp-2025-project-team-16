package com.fitquest.app.data.remote

import com.fitquest.app.model.pose.PoseUploadResponse
import com.fitquest.app.model.pose.PoseUploadRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PoseAnalysisApiService {
    @POST("pose-analyses/evaluate_posture/")
    suspend fun uploadPose(
        @Body request: PoseUploadRequest
    ): Response<PoseUploadResponse>
}