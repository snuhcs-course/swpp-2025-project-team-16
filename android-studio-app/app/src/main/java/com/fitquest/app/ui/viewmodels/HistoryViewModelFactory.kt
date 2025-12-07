package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitquest.app.data.remote.DailySummaryApiService
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.repository.DailySummaryRepository
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.repository.SessionRepository

class HistoryViewModelFactory (
    private val dailySummaryApiService: DailySummaryApiService,
    private val scheduleApiService: ScheduleApiService,
    private val sessionApiService: SessionApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            val dailySummaryRepository = DailySummaryRepository(dailySummaryApiService)
            val scheduleRepository = ScheduleRepository(scheduleApiService)
            val sessionRepository = SessionRepository(sessionApiService)
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(dailySummaryRepository, scheduleRepository, sessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}