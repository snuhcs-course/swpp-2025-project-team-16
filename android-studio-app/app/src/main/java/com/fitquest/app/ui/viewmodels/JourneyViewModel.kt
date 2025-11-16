package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fitquest.app.model.WorkoutPlan

/**
 * ViewModel for JourneyFragment
 */
class JourneyViewModel : ViewModel() {

    private val _workoutPlans = MutableLiveData<List<WorkoutPlan>>()
    val workoutPlans: LiveData<List<WorkoutPlan>> = _workoutPlans

    private val _selectedWorkout = MutableLiveData<WorkoutPlan?>()
    val selectedWorkout: LiveData<WorkoutPlan?> = _selectedWorkout

    fun loadWorkoutPlans() {
        // TODO: Backend - Fetch upcoming workout plans
        // _workoutPlans.value = fetchedPlans
    }

    fun selectWorkout(workout: WorkoutPlan) {
        _selectedWorkout.value = workout
    }

    fun clearSelection() {
        _selectedWorkout.value = null
    }
}
