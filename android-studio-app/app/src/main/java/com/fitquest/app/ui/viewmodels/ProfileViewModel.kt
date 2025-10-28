package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fitquest.app.model.User
import com.fitquest.app.model.WorkoutHistory

/**
 * ViewModel for ProfileFragment
 */
class ProfileViewModel : ViewModel() {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _workoutHistory = MutableLiveData<List<WorkoutHistory>>()
    val workoutHistory: LiveData<List<WorkoutHistory>> = _workoutHistory

    private val _selectedWorkout = MutableLiveData<WorkoutHistory?>()
    val selectedWorkout: LiveData<WorkoutHistory?> = _selectedWorkout

    private val _level = MutableLiveData<Int>()
    val level: LiveData<Int> = _level

    private val _levelProgress = MutableLiveData<Int>()
    val levelProgress: LiveData<Int> = _levelProgress

    fun loadUserProfile() {
        // TODO: Backend - Fetch user profile data
        // _user.value = fetchedUser
        
        // Calculate level and progress
        _user.value?.let { user ->
            val calculatedLevel = calculateLevel(user.totalPoints)
            _level.value = calculatedLevel
            _levelProgress.value = calculateLevelProgress(user.totalPoints)
        }
    }

    fun loadWorkoutHistory() {
        // TODO: Backend - Fetch workout history
        // _workoutHistory.value = fetchedHistory
    }

    fun selectWorkout(workout: WorkoutHistory) {
        _selectedWorkout.value = workout
    }

    fun clearWorkoutSelection() {
        _selectedWorkout.value = null
    }

    private fun calculateLevel(points: Int): Int {
        return (points / 500) + 1
    }

    private fun calculateLevelProgress(points: Int): Int {
        return ((points % 500) * 100) / 500
    }

    fun getPointsToNextLevel(currentPoints: Int): Int {
        return 500 - (currentPoints % 500)
    }
}
