package com.fitquest.app.data.remote

import com.fitquest.app.model.PoseAnalysis
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PoseAnalysisApiService {

    @POST("pose-analyses/evaluate_posture/")
    suspend fun uploadPose(
        @Body request: PoseUploadRequest
    ): PoseAnalysis

    @POST("pose-analyses/save/")
    suspend fun savePoseResult(
        @Body request: PoseResultSaveRequest
    ): retrofit2.Response<Void>

    @GET("pose-analyses/")
    suspend fun getPoseAnalyses(): List<PoseAnalysis>

    @GET("pose-analyses/{id}/")
    suspend fun getPoseAnalysis(
        @Path("id") id: Int
    ): PoseAnalysis

    @GET("pose-analyses/session/{session_id}/")
    suspend fun getPoseAnalysesBySession(
        @Path("session_id") sessionId: Int
    ): List<PoseAnalysis>

    @GET("pose-analyses/schedule/{schedule_id}")
    suspend fun getPoseAnalysesBySchedule(
        @Path("schedule_id") scheduleId: Int
    ): List<PoseAnalysis>
}