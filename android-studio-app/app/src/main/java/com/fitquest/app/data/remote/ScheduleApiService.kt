package com.fitquest.app.data.remote

import com.fitquest.app.model.Exercise
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Header

data class ScheduleResponse(
    val id:String,
    val date:String,
    val exercises:List<Exercise>,
    val startTime:String,
    val finishTime:String,
    val point:Int,
    val isCompleted: Boolean,
    val feedback:String
)
data class PatchResponse(
    val id:String,
    val startTime:String,
    val finishTime: String,
    val repTarget:Int
)
interface ScheduleApiService {
    @GET("schedule/schedules")
    suspend fun getUserSchedules(
        @Header("Authorization") token: String
    ): Response<List<ScheduleResponse>>

    @POST("schedule/generate_schedule")
    suspend fun generateUserSchedules(
        @Header("Authorization") token: String,
        @Body schedule: ScheduleResponse
    ): Response<String>

    @PATCH("schedule/")
    suspend fun modifySchedules(
        @Header("Authorization") token: String,
        scheduleId:String,
        startTime:String,
        finishTime:String
    ): Response<String>

    @PATCH("schedule/exercise")
    suspend fun completeExercise(
        @Header("Authorization") token: String,
        scheduleId:String,
        exerciseId:String
    ):Response<String>

    @POST("schedule/auto-generate")
    suspend fun generateSchedulesWithAI(
        @Header("Authorization") token: String,
        goal:String,
        days:Int
    ): Response<List<ScheduleResponse>>

}