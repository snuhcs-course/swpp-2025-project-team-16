package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.Schedule
import com.fitquest.app.repository.ScheduleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {

    val schedules = MutableLiveData<List<Schedule>>()
    val loading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String>()

    val newlyGeneratedIds = MutableLiveData<Set<Int>>(emptySet())

    private fun sortSchedules(list: List<Schedule>): List<Schedule> {
        return list.sortedWith(
            compareBy(
                { it.scheduledDate },
                { it.startTime }
            )
        )
    }

    fun getSchedules(status: String = "planned") {
        loading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getSchedules(status.lowercase())
                schedules.postValue(sortSchedules(result))
            } catch (e: Exception) {
                error.postValue(e.message)
            } finally {
                loading.postValue(false)
            }
        }
    }

//    fun autoGenerateSchedules(onComplete: (() -> Unit)? = null) {
//        viewModelScope.launch {
//            try {
//                val generated = repository.autoGenerateSchedules()
//
//                val newIds = generated.mapNotNull { it.id }.toSet()
//                newlyGeneratedIds.postValue(newIds) // 어댑터에 하이라이트 요청
//
//                val currentList = schedules.value.orEmpty()
//                val updatedList = sortSchedules(currentList + generated)
//                schedules.postValue(updatedList)
//
//                onComplete?.invoke()
//
//                delay(5000)
//                if (newlyGeneratedIds.value == newIds) {
//                    newlyGeneratedIds.postValue(emptySet())
//                }
//
//            } catch (e: Exception) {
//                error.postValue(e.message)
//            }
//        }
//    }
}