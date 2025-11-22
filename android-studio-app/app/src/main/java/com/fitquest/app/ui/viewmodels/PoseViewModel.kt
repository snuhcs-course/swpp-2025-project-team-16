package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.remote.PoseUploadRequest
import com.fitquest.app.model.PoseAnalysis
import com.fitquest.app.repository.PoseAnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PoseViewModel(
    private val repository: PoseAnalysisRepository
) : ViewModel() {

    private val _analyses = MutableStateFlow<List<PoseAnalysis>>(emptyList())
    val analyses: StateFlow<List<PoseAnalysis>> = _analyses

    private val _selectedAnalysis = MutableStateFlow<PoseAnalysis?>(null)
    val selectedAnalysis: StateFlow<PoseAnalysis?> = _selectedAnalysis

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun getPoseAnalyses() {
        viewModelScope.launch {
            _loading.value = true
            _analyses.value = repository.getPoseAnalyses()
            _loading.value = false
        }
    }

    fun getPoseAnalysesBySession(sessionId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _analyses.value = repository.getPoseAnalysesBySession(sessionId)
            _loading.value = false
        }
    }

    fun getPoseAnalysesBySchedule(scheduleId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _analyses.value = repository.getPoseAnalysesBySchedule(scheduleId)
            _loading.value = false
        }
    }

    fun uploadPose(request: PoseUploadRequest) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.uploadPose(request)
            _selectedAnalysis.value = result
            _loading.value = false
        }
    }
}