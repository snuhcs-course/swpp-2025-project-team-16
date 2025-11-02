package com.fitquest.app.ui.viewmodels

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fitquest.app.model.Exercise
import com.fitquest.app.model.WorkoutPlan
import java.util.Locale

/**
 * ViewModel for ScheduleFragment
 */
class ScheduleViewModel : ViewModel() {

    private val _selectedDate = MutableLiveData<String>("")
    val selectedDate: LiveData<String> = _selectedDate

    private val _exercises = MutableLiveData<List<Exercise>>(emptyList())
    val exercises: LiveData<List<Exercise>> = _exercises

    private val _isEditing = MutableLiveData<Boolean>(false)
    val isEditing: LiveData<Boolean> = _isEditing

    private val _scheduledDate= MutableLiveData<List<String>>(emptyList())
    val scheduledDate: LiveData<List<String>> =_scheduledDate

    val workoutPlans:MutableList<WorkoutPlan> =mutableListOf()
    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        loadScheduleForDate(date)
    }

    fun loadScheduleForDate(date: String) {
        // TODO: Backend - Load existing schedule for date
        var tempPlans=emptyList<Exercise>()
        for(workoutPlan in workoutPlans){
            if(workoutPlan.date==date){
                tempPlans=workoutPlan.exercises
                break
            }
        }
        _exercises.value=tempPlans

        // _exercises.value = fetchedExercises
    }

    fun generateSchedule() {
        // TODO: Backend - Generate AI-based workout schedule
        // _exercises.value = generatedExercises
        val currentDate= Calendar.getInstance()
        for(i in 0..30){
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = sdf.format(currentDate.time)
            val tempPlans: MutableList<Exercise> =mutableListOf()
            tempPlans.add(Exercise("${3*i}","스쿼트","스쿼트","100","0","5",""))
            tempPlans.add(Exercise("${3*i+1}","팔굽혀펴기","팔굽혀펴기","100","30","5",""))
            tempPlans.add(Exercise("${3*i+2}","플랭크","플랭크","100","30","5",""))

            workoutPlans.add(WorkoutPlan(selectedDate,tempPlans,false,30,null))
            if(i==0){
                _exercises.value=tempPlans
            }
            currentDate.add(Calendar.DAY_OF_MONTH,1)
        }
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
