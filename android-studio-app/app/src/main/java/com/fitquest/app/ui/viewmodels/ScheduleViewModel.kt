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
    private val _message= MutableLiveData<String>("")
    val message: LiveData<String> = _message
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

    fun generateSchedule(token:String) {
        // TODO: Backend - Generate AI-based workout schedule
        // _exercises.value = generatedExercises
        val currentDate= Calendar.getInstance()
        for(i in 0..30){
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = sdf.format(currentDate.time)
            val tempPlans: MutableList<Exercise> =mutableListOf()
            tempPlans.add(Exercise(id="${3*i}",name="스쿼트", repTarget=5+i))
            tempPlans.add(Exercise(id="${3*i+1}",name="팔굽혀펴기", repTarget = 5+i))
            tempPlans.add(Exercise(id="${3*i+2}",name="플랭크", duration = "${5*i+30}초"))
            val workoutPlan=WorkoutPlan("$i",selectedDate,tempPlans,false,30,"", startTime = "08:30:00", finishTime = "09:30:00")
            workoutPlans.add(workoutPlan)
            viewModelScope.launch {
                try {
                    val response = RetrofitClient.scheduleApiService.generateUserSchedules(
                        token = "Bearer $token",
                        ScheduleResponse(workoutPlan.id,workoutPlan.date,workoutPlan.exercises,workoutPlan.startTime,workoutPlan.finishTime,workoutPlan.points,workoutPlan.isCompleted,workoutPlan.feedback)
                    )
                    if (response.isSuccessful) {
                        _message.value="SUCCESS"
                    } else {
                        _message.value="Failed: ${response.code()}"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _message.value="Network error: ${e.localizedMessage}"
                }
            }
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
    fun loadAllSchedules(token:String){
        viewModelScope.launch {
            try {
                val response = RetrofitClient.scheduleApiService.getUserSchedules(
                    token = "Bearer $token",
                )
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        for (work in result){
                            val listExercise: MutableList<Exercise> =mutableListOf()
                            for(exercise in work.exercises){
                                listExercise.add(Exercise(name=exercise.name, detail = exercise.detail, status = exercise.status))
                            }
                            val plan= WorkoutPlan(work.id,work.date,listExercise,work.isCompleted,work.point,work.feedback,work.startTime,work.finishTime)
                            workoutPlans.add(plan)
                        }
                    }
                    _message.value="SUCCESS"
                } else {
                    _message.value="Failed: ${response.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _message.value="Network error: ${e.localizedMessage}"
            }
        }

    }
}
