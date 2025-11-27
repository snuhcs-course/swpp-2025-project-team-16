package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitquest.app.data.remote.DailySummaryApiService
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.repository.DailySummaryRepository
import com.fitquest.app.repository.ScheduleRepository

class JourneyViewModelFactory(
    private val scheduleApiService: ScheduleApiService,
    private val dailySummaryApiService: DailySummaryApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JourneyViewModel::class.java)) {
            val scheduleRepository = ScheduleRepository(scheduleApiService)
            val dailySummaryRepository = DailySummaryRepository(dailySummaryApiService)
            @Suppress("UNCHECKED_CAST")
            return JourneyViewModel(scheduleRepository, dailySummaryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}