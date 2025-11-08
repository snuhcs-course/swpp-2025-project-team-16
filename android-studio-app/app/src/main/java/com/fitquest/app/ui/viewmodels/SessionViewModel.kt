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

    private val _currentSession = MutableLiveData<Session>()
    val currentSession: LiveData<Session> get() = _currentSession

    fun startSession(activity: String, scheduleId: Int? = null) {
        viewModelScope.launch {
            try {
                _currentSession.value = repo.startSession(activity, scheduleId)
            } catch(e: Exception) {
                Log.e("SessionVM", "startSession error: $e")
            }
        }
    }

    fun endSession(reps: Int? = null, duration: Int? = null) {
        val sessionId = _currentSession.value?.id ?: return
        viewModelScope.launch {
            try {
                _currentSession.value = repo.endSession(sessionId, reps, duration)
            } catch(e: Exception) {
                Log.e("SessionVM", "endSession error: $e")
            }
        }
    }
}
