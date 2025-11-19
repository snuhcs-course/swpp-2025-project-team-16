package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.model.WorkoutItem
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.util.DateUtils.formatDate
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

class JourneyViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _dailyWorkouts = MutableLiveData<List<DailyWorkoutItem>>()
    val dailyWorkouts: LiveData<List<DailyWorkoutItem>> = _dailyWorkouts

    fun loadUpcomingSchedules() {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val schedules = repository.getSchedules()

            val upcoming = schedules.filter {
                val scheduleEnd = LocalDateTime.of(LocalDate.parse(it.scheduledDate),
                    LocalTime.parse(it.endTime))
                scheduleEnd.isAfter(now) || scheduleEnd.isEqual(now)
            }

            val grouped = upcoming.groupBy { it.scheduledDate }

            val dailyItems = grouped.map { (date, scheduleList) ->
                val exercises = scheduleList.map { schedule ->
                    WorkoutItem(
                        name = schedule.activity,
                        targetCount = schedule.repsTarget,
                        targetDuration = schedule.durationTarget,
                        status = schedule.status
                    )
                }
                DailyWorkoutItem(
                    date = LocalDate.parse(date),
                    dateLabel = formatDate(date),
                    exercises = exercises
                )
            }.sortedBy { it.date }

            _dailyWorkouts.value = dailyItems
        }
    }
}
