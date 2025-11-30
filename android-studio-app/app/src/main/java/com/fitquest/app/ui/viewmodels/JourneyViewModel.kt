package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.repository.ScheduleRepository
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

class JourneyViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _dailyWorkouts = MutableLiveData<List<DailyWorkoutItem>>()
    val dailyWorkouts: LiveData<List<DailyWorkoutItem>> = _dailyWorkouts

    fun loadUpcomingSchedules() {
        viewModelScope.launch {
            val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            val schedules = repository.getSchedules()

            val upcoming = schedules.filter {
                val scheduleEnd = LocalDateTime.of(it.scheduledDate, it.endTime)
                val isUpcoming = scheduleEnd.isAfter(now)
                println("Schedule: ${it.scheduledDate} ${it.endTime}, Now: $now, IsUpcoming: $isUpcoming")
                isUpcoming
                scheduleEnd.isAfter(now) || scheduleEnd.isEqual(now)
            }

            val grouped = upcoming.groupBy { it.scheduledDate }

            val dailyItems = grouped.map { (date, scheduleList) ->
                DailyWorkoutItem(
                    date = date,
                    schedules = scheduleList.sortedBy { it.startTime }
                )
            }.sortedBy { it.date }

            _dailyWorkouts.value = dailyItems
        }
    }
}
