package com.fitquest.app.ui.viewmodels

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.remote.InitialCountRequest
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.ScheduleResponse
import com.fitquest.app.data.remote.WorkoutDayResponse
import com.fitquest.app.model.Exercise
import com.fitquest.app.model.WorkoutPlan
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel for ScheduleFragment
 */
class ScheduleViewModel : ViewModel() {

    // 🔹 전체 스케줄 (모든 날짜의 WorkoutPlan)
    private val _workoutPlans = MutableLiveData<List<WorkoutPlan>>(emptyList())
    val workoutPlans: LiveData<List<WorkoutPlan>> get() = _workoutPlans

    // 🔹 현재 선택된 날짜
    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> get() = _selectedDate

    // 🔹 선택된 날짜의 운동 리스트
    private val _exercises = MutableLiveData<List<Exercise>>(emptyList())
    val exercises: LiveData<List<Exercise>> get() = _exercises

    // 🔹 메시지 (성공/오류/상태 표시)
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message


    /** 전체 WorkoutPlan 리스트 업데이트 */
    fun updateWorkoutPlans(plans: List<WorkoutPlan>) {
        _workoutPlans.value = plans
    }

    /** 특정 날짜의 운동 리스트 필터링 */
    fun loadScheduleForDate(date: String) {
        _selectedDate.value = date
        val allPlans = _workoutPlans.value ?: return

        val todayPlans = allPlans.filter { it.date == date }

        if (todayPlans.isNotEmpty()) {
            // 여러 계획이 있어도 첫 번째 걸 표시한다고 가정
//            _exercises.value = todayPlans.first().exercises
            val allExercises = todayPlans.flatMap { it.exercises }
            _exercises.value = allExercises
        } else {
            _exercises.value = emptyList()
        }
    }

    /** 메시지 업데이트 (UI 토스트용 등) */
    fun setMessage(msg: String) {
        _message.value = msg
    }

    /** 모든 LiveData 초기화 (로그아웃 시 등) */
    fun clearAll() {
        _workoutPlans.value = emptyList()
        _exercises.value = emptyList()
        _selectedDate.value = ""
        _message.value = ""
    }
}
