package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.repository.ScheduleRepository

class LoginViewModelFactory(
    private val scheduleApiService: ScheduleApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val scheduleRepository = ScheduleRepository(scheduleApiService)
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(scheduleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}