package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.DailyHistoryItem
import com.fitquest.app.repository.DailySummaryRepository
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.repository.SessionRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class HistoryViewModel(
    private val dailySummaryRepository: DailySummaryRepository,
    private val scheduleRepository: ScheduleRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _dailyHistories = MutableLiveData<List<DailyHistoryItem>>()
    val dailyHistories: LiveData<List<DailyHistoryItem>> = _dailyHistories

    fun loadHistory() {
        viewModelScope.launch {
            val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

            val schedules = scheduleRepository.getSchedules()

            val pastSchedules = schedules.filter {
                it.status != "planned"
            }

            val sessions = sessionRepository.getSessions()
            val pastSessions = sessions.filter {
                (it.createdAt?.isBefore(now) ?: false) && it.schedule == null
            }

            val dailySummaries = dailySummaryRepository.getDailySummaries()
            val pastDailySummaries = dailySummaries.filter {
                (it.date.isBefore(now.toLocalDate()))
            }

            val allDates = (pastSchedules.map { it.scheduledDate } + pastSessions.map { it.createdAt!!.toLocalDate() }).distinct()

            val dailyItems = allDates.map { date ->
                DailyHistoryItem(
                    date = date,
                    summaryText = pastDailySummaries.firstOrNull { it.date == date }?.summaryText ?: "",
                    schedules = pastSchedules.filter { it.scheduledDate == date },
                    sessions = pastSessions.filter { it.createdAt?.toLocalDate() == date }
                )
            }.sortedByDescending { it.date }

            _dailyHistories.value = dailyItems
        }
    }
}
