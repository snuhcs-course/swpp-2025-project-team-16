package com.fitquest.app.data.remote

import com.fitquest.app.model.AutoGenerateSchedulesResponse
import com.fitquest.app.model.Schedule
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ScheduleApiService {

    @GET("schedules/")
    suspend fun getSchedules(
        @Query("status") status: String? = null
    ): List<Schedule>

    @POST("schedules/")
    suspend fun createSchedule(
        @Body schedule: Schedule
    ): Schedule

    @POST("schedules/auto-generate/")
    suspend fun autoGenerateSchedules(): AutoGenerateSchedulesResponse

    @POST("schedules/mark-missed/")
    suspend fun markMissedSchedules()
}