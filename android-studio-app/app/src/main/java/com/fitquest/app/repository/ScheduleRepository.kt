package com.fitquest.app.repository

import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.model.AutoGenerateSchedulesResponse
import com.fitquest.app.model.Schedule

class ScheduleRepository(private val service: ScheduleApiService) {

    suspend fun getSchedules(status: String? = null): List<Schedule> =
        service.getSchedules(status)

    suspend fun createSchedule(schedule: Schedule): Schedule =
        service.createSchedule(schedule)

    suspend fun autoGenerateSchedules(): AutoGenerateSchedulesResponse =
        service.autoGenerateSchedules()

    suspend fun markMissedSchedules() =
        service.markMissedSchedules()
}