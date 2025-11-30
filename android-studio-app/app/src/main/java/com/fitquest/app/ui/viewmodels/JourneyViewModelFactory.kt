package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.repository.ScheduleRepository

class JourneyViewModelFactory(
    private val scheduleApiService: ScheduleApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JourneyViewModel::class.java)) {
            val repository = ScheduleRepository(scheduleApiService)
            @Suppress("UNCHECKED_CAST")
            return JourneyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}