package com.fitquest.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.RankResponse
import com.fitquest.app.model.User
import com.fitquest.app.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _rankings = MutableStateFlow<List<RankResponse>>(emptyList())
    val rankings: StateFlow<List<RankResponse>> = _rankings

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun getProfile() {
        viewModelScope.launch {
            try {
                val profile = repository.getProfile()
                _userProfile.value = profile
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun getRankings() {
        viewModelScope.launch {
            try {
                val response = repository.getRankings()
                if (response.isSuccessful) {
                    _rankings.value = response.body() ?: emptyList()
                } else {
                    _error.value = "HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to fetch rankings: ${e.localizedMessage}")
                _error.value = e.localizedMessage
            }
        }
    }
}
