package com.fitquest.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.Session
import com.fitquest.app.repository.SessionRepository
import kotlinx.coroutines.launch

class SessionViewModel(private val repo: SessionRepository) : ViewModel() {

    private val _currentSession = MutableLiveData<Session?>()
    val currentSession: LiveData<Session?> get() = _currentSession

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun startSession(activity: String, scheduleId: Int? = null) {
        viewModelScope.launch {
            val result = repo.startSession(activity, scheduleId)

            result.onSuccess { session ->
                _currentSession.value = session
            }.onFailure { e ->
                _currentSession.value = null

                val message = "Session creation failed: ${e.message ?: "Unknown error"}"
                _errorMessage.value = message

                // Log.e("SessionVM", "startSession error: $e")
            }
        }
    }

    fun endSession(reps: Int? = null, duration: Int? = null) {
        // 활성화된 세션 ID가 없다면 로그를 남기고 종료
        val sessionId = _currentSession.value?.id ?: run {
            // Log.w("SessionVM", "endSession called without active session ID.")
            return
        }

        viewModelScope.launch {
            val result = repo.endSession(sessionId, reps, duration)

            result.onSuccess { session ->
                _currentSession.value = session
            }.onFailure { e ->
                val message = "Session completion failed: ${e.message ?: "Unknown error"}"
                _errorMessage.value = message

                // Log.e("SessionVM", "endSession error: $e")

                // Note: 오류가 났더라도 _currentSession.value를 null로 만들지 않고,
                // 오류 메시지를 통해 사용자에게 알린 후 상태는 유지하여
                // 재시도 등의 UI 흐름을 고려할 수 있다.
            }
        }
    }
}