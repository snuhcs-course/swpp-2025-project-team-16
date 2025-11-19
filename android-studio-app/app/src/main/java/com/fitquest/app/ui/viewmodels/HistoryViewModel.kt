package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.DailyHistoryItem
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.util.DateUtils.formatDate
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

class HistoryViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _dailyHistories = MutableLiveData<List<DailyHistoryItem>>()
    val dailyHistories: LiveData<List<DailyHistoryItem>> = _dailyHistories

    fun loadPastSchedules() {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val schedules = repository.getSchedules()

            val past = schedules.filter {
                val scheduleEnd = LocalDateTime.of(
                    LocalDate.parse(it.scheduledDate),
                    LocalTime.parse(it.endTime)
                )
                scheduleEnd.isBefore(now)
            }

            val grouped = past.groupBy { it.scheduledDate }

            val dailyItems = grouped.map { (date, scheduleList) ->
                DailyHistoryItem(
                    dateLabel = formatDate(date),
                    exercises = scheduleList
                )
            }.sortedByDescending { it.dateLabel }

            _dailyHistories.value = dailyItems
        }
    }
}
