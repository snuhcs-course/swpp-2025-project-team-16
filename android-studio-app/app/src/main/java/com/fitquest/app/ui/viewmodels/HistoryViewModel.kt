package com.fitquest.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.DailyHistoryItem
import com.fitquest.app.repository.DailySummaryRepository
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.repository.SessionRepository
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

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
            Log.d("HistoryVM", "Loaded schedules = ${schedules.size}")

            val pastSchedules = schedules.filter {
                it.status != "planned"
            }
            Log.d("HistoryVM", "Past schedules = ${pastSchedules.size}")
            pastSchedules.forEach {
                Log.d("HistoryVM", "Past schedule: ${it.scheduledDate} ${it.startTime} ~ ${it.endTime}")
            }

            val sessions = sessionRepository.getSessions()
            Log.d("HistoryVM", "Loaded sessions = ${sessions.size}")
            val pastSessions = sessions.filter {
                (it.createdAt?.isBefore(now) ?: false) && it.schedule == null
            }
            Log.d("HistoryVM", "Past sessions (schedule == null) = ${pastSessions.size}")
            pastSessions.forEach {
                Log.d("HistoryVM", "Past session: createdAt=${it.createdAt}, schedule=${it.schedule}")
            }

            val dailySummaries = dailySummaryRepository.getDailySummaries()
            val pastDailySummaries = dailySummaries.filter {
                (it.date.isBefore(now.toLocalDate()))
            }

            val allDates = (pastSchedules.map { it.scheduledDate } + pastSessions.map { it.createdAt!!.toLocalDate() }).distinct()

            Log.d("HistoryVM", "All unique dates = $allDates")

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
