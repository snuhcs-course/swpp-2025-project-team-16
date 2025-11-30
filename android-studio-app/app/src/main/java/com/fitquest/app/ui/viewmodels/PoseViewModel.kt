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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearSelectedAnalysis() {
        _selectedAnalysis.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun getPoseAnalyses() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _analyses.value = repository.getPoseAnalyses()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun getPoseAnalysesBySession(sessionId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _analyses.value = repository.getPoseAnalysesBySession(sessionId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun getPoseAnalysesBySchedule(scheduleId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _analyses.value = repository.getPoseAnalysesBySchedule(scheduleId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun uploadPose(request: PoseUploadRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.uploadPose(request)
                _selectedAnalysis.value = result
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _loading.value = false
            }
        }
    }
}
