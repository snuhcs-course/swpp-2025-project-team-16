package com.fitquest.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class ExerciseResponse(
    val name: String,
    val detail: String,
    val status: String
)

data class WorkoutDayResponse(
    val date: String,
    val xp: Int,
    val exercises: List<ExerciseResponse>
)

interface JourneyApiService{
    @GET("schedules/")
    suspend fun getUserSchedules(
        @Header("Authorization") token: String
    ): Response<List<WorkoutDayResponse>>
}
