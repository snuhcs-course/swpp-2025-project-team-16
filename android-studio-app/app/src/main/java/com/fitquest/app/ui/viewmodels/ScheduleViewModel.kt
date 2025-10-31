package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fitquest.app.model.Exercise
import com.fitquest.app.model.WorkoutPlan

/**
 * ViewModel for ScheduleFragment
 */
class ScheduleViewModel : ViewModel() {

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val _exercises = MutableLiveData<List<Exercise>>()
    val exercises: LiveData<List<Exercise>> = _exercises

    private val _isEditing = MutableLiveData<Boolean>(false)
    val isEditing: LiveData<Boolean> = _isEditing

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        loadScheduleForDate(date)
    }

    fun loadScheduleForDate(date: String) {
        // TODO: Backend - Load existing schedule for date
        // _exercises.value = fetchedExercises
    }

    fun generateSchedule() {
        // TODO: Backend - Generate AI-based workout schedule
        // _exercises.value = generatedExercises
        _isEditing.value = true
    }

    fun addExercise(exercise: Exercise) {
        val currentList = _exercises.value.orEmpty().toMutableList()
        currentList.add(exercise)
        _exercises.value = currentList
    }

    fun removeExercise(exerciseId: String) {
        val currentList = _exercises.value.orEmpty().toMutableList()
        currentList.removeAll { it.id == exerciseId }
        _exercises.value = currentList
    }

    fun saveSchedule() {
        val date = _selectedDate.value ?: return
        val exerciseList = _exercises.value ?: return
        
        // TODO: Backend - Save schedule to database
        _isEditing.value = false
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
        // Reload original schedule
        _selectedDate.value?.let { loadScheduleForDate(it) }
    }
}
