package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.pose.PoseUploadRequest
import com.fitquest.app.model.pose.PoseUploadResponse
import com.fitquest.app.repository.PoseAnalysisRepository
import kotlinx.coroutines.launch

class PoseViewModel(
    private val repository: PoseAnalysisRepository
) : ViewModel() {

    private val _poseAnalysisResult = MutableLiveData<NetworkResult<PoseUploadResponse>>()
    val poseAnalysisResult: LiveData<NetworkResult<PoseUploadResponse>> = _poseAnalysisResult

    fun uploadPose(request: PoseUploadRequest) {
        viewModelScope.launch {
            _poseAnalysisResult.value = NetworkResult.Idle
            try {
                val response = repository.uploadPose(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _poseAnalysisResult.value = NetworkResult.Success(body)
                    } else {
                        _poseAnalysisResult.value = NetworkResult.ServerError(response.code(), "Empty response body")
                    }
                } else {
                    _poseAnalysisResult.value = NetworkResult.ServerError(response.code(), response.message())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _poseAnalysisResult.value = NetworkResult.NetworkError(e)
            }
        }
    }

    fun resetPoseAnalysisResult() {
        _poseAnalysisResult.value = NetworkResult.Idle
    }
}
