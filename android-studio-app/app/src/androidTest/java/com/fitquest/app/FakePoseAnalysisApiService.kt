package com.fitquest.app

import com.fitquest.app.data.remote.PoseAnalysisApiService
import com.fitquest.app.model.pose.PoseUploadRequest
import com.fitquest.app.model.pose.PoseUploadResponse
import kotlinx.coroutines.delay
import retrofit2.Response

class FakePoseAnalysisApiService: PoseAnalysisApiService {
    override suspend fun uploadPose(request: PoseUploadRequest): Response<PoseUploadResponse> {
        delay(10000)
        return Response.success(PoseUploadResponse(0,"well","poor","do right"))
    }
}