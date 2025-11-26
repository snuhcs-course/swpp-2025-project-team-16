package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.login.EmailCheckResponse
import com.fitquest.app.model.login.InitialCountRequest
import com.fitquest.app.model.login.InitialCountResponse
import com.fitquest.app.model.login.LoginRequest
import com.fitquest.app.model.login.LoginResponse
import com.fitquest.app.model.login.SignupRequest
import com.fitquest.app.model.login.SignupResponse
import com.fitquest.app.repository.AuthRepository
import kotlinx.coroutines.launch


class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    // -------------------- Login & Signup 상태 --------------------
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val name = MutableLiveData<String>()

    // -------------------- API 결과 LiveData --------------------
    private val _signupResult = MutableLiveData<NetworkResult<SignupResponse>>()
    val signupResult: LiveData<NetworkResult<SignupResponse>> = _signupResult

    private val _loginResult = MutableLiveData<NetworkResult<LoginResponse>>()
    val loginResult: LiveData<NetworkResult<LoginResponse>> = _loginResult

    private val _checkEmailResult = MutableLiveData<NetworkResult<EmailCheckResponse>>()
    val checkEmailResult: LiveData<NetworkResult<EmailCheckResponse>> = _checkEmailResult

    private val _updateInitialRepsResult = MutableLiveData<NetworkResult<InitialCountResponse>>()
    val updateInitialRepsResult: LiveData<NetworkResult<InitialCountResponse>> = _updateInitialRepsResult

    fun signup(request: SignupRequest) {
        viewModelScope.launch {
            try {
                val response = repository.signup(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && !body.token.isNullOrEmpty()) {
                        _signupResult.value = NetworkResult.Success(body)
                    } else {
                        _signupResult.value = NetworkResult.ServerError(response.code(), "Empty response body")
                    }
                } else {
                    _signupResult.value = NetworkResult.ServerError(response.code(), response.message())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _signupResult.value = NetworkResult.NetworkError(e)
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            try {
                val response = repository.login(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && !body.token.isNullOrEmpty()) {
                        _loginResult.value = NetworkResult.Success(body)
                    } else {
                        _loginResult.value = NetworkResult.ServerError(
                            code = response.code(),
                            message = body?.error ?: "Invalid credentials"
                        )
                    }
                } else {
                    _loginResult.value = NetworkResult.ServerError(
                        code = response.code(),
                        message = "Server error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loginResult.value = NetworkResult.NetworkError(e)
            }
        }
    }

    fun checkEmail(request: Map<String, String>) {
        viewModelScope.launch {
            try {
                val response = repository.checkEmail(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _checkEmailResult.value = NetworkResult.Success(body)
                    } else {
                        _checkEmailResult.value = NetworkResult.ServerError(response.code(), "Empty response body")
                    }
                } else {
                    _checkEmailResult.value = NetworkResult.ServerError(response.code(), response.message())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _checkEmailResult.value = NetworkResult.NetworkError(e)
            }
        }
    }

    fun updateInitialReps(request: InitialCountRequest) {
        viewModelScope.launch {
            try {
                val response = repository.updateInitialReps(request)
                if (response.isSuccessful && response.body() != null) {
                    _updateInitialRepsResult.value = NetworkResult.Success(response.body()!!)
                } else {
                    _updateInitialRepsResult.value = NetworkResult.ServerError(response.code(), response.message())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _updateInitialRepsResult.value = NetworkResult.NetworkError(e)
            }
        }
    }

    fun resetCheckEmailResult() {
        _checkEmailResult.value = NetworkResult.Idle
    }

    fun resetSignupResult() {
        _signupResult.value = NetworkResult.Idle
    }

    fun resetLoginResult() {
        _loginResult.value = NetworkResult.Idle
    }

    fun resetUpdateInitialRepsResult() {
        _updateInitialRepsResult.value = NetworkResult.Idle
    }
}
