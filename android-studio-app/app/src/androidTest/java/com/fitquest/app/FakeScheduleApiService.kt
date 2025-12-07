package com.fitquest.app

import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.model.AutoGenerateSchedulesResponse
import com.fitquest.app.model.Schedule
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

class FakeScheduleApiService: ScheduleApiService {
    override suspend fun getSchedules(status: String?): List<Schedule> {
        return listOf(Schedule(scheduledDate = LocalDate.now(), startTime = LocalTime.now(), endTime = LocalTime.now().plusHours(1), activity = "squat",status="planned", repsTarget = 100, repsDone = 0,id=0),
            Schedule(scheduledDate = LocalDate.now().minusDays(1), startTime = LocalTime.now(),endTime=LocalTime.now(), activity = "squat", status = "missed",id=1),
            Schedule(scheduledDate = LocalDate.now().minusDays(2), startTime = LocalTime.now(), endTime = LocalTime.now().plusHours(1), activity = "squat",status="planned", repsTarget = 100, repsDone = 0,id=2),
            Schedule(scheduledDate = LocalDate.now().plusDays(1), startTime = LocalTime.now(), endTime = LocalTime.now().plusHours(1), activity = "squat",status="planned", repsTarget = 100, repsDone = 0,id=3))
    }



    override suspend fun autoGenerateSchedules(): AutoGenerateSchedulesResponse {
        return AutoGenerateSchedulesResponse(1,listOf("2025-12-07","2025-12-08"))
    }

    override suspend fun markMissedSchedules() {

    }
}