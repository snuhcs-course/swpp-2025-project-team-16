package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.repository.SessionRepository

class AiCoachViewModelFactory(
    private val service: SessionApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiCoachViewModel::class.java)) {
            val repository = SessionRepository(service)
            @Suppress("UNCHECKED_CAST")
            return AiCoachViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}