package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.repository.ScheduleRepository
import kotlinx.coroutines.launch

class JourneyViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _dailyWorkouts = MutableLiveData<List<DailyWorkoutItem>>()
    val dailyWorkouts: LiveData<List<DailyWorkoutItem>> = _dailyWorkouts

    fun loadUpcomingSchedules() {
        viewModelScope.launch {
            val schedules = repository.getSchedules()

            val upcoming = schedules.filter {
                it.status == "planned"
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
