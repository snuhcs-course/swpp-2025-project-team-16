package com.fitquest.app.repository

import com.fitquest.app.model.Schedule
import com.fitquest.app.data.remote.RetrofitClient

class ScheduleRepository {
    private val service = RetrofitClient.scheduleApiService

    suspend fun getSchedules(status: String? = null): List<Schedule> =
        service.getSchedules(status)

    suspend fun createSchedule(schedule: Schedule): Schedule =
        service.createSchedule(schedule)

    suspend fun autoGenerateSchedules(): List<Schedule> =
        service.autoGenerateSchedules()
}