package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fitquest.app.data.remote.PoseAnalysisApiService
import com.fitquest.app.repository.PoseAnalysisRepository

class PoseViewModelFactory(
    private val service: PoseAnalysisApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoseViewModel::class.java)) {
            val repository = PoseAnalysisRepository(service)
            @Suppress("UNCHECKED_CAST")
            return PoseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
