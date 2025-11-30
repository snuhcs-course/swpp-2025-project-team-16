package com.fitquest.app

import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.model.Schedule
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

class FakeScheduleApiService: ScheduleApiService {
    override suspend fun getSchedules(status: String?): List<Schedule> {
        return listOf(Schedule(scheduledDate = LocalDate.now(), startTime = LocalTime.now(), endTime = LocalTime.now(), activity = "squat"))
    }

    override suspend fun createSchedule(schedule: Schedule): Schedule {
        return Schedule(scheduledDate = LocalDate.now(), startTime = LocalTime.now(), endTime = LocalTime.now(), activity = "squat")
    }

    override suspend fun autoGenerateSchedules(): List<Schedule> {
        return listOf(Schedule(scheduledDate = LocalDate.now(), startTime = LocalTime.now(), endTime = LocalTime.now(), activity = "squat"))
    }

    override suspend fun markMissedSchedules() {

    }
}