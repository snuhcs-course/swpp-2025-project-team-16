package com.fitquest.app.ui.viewmodels

import com.fitquest.app.model.Exercise
import com.fitquest.app.model.WorkoutPlan
import org.junit.Assert.*
import org.junit.Test

class ScheduleViewModelTest {
    val viewModel:ScheduleViewModel= ScheduleViewModel()
    @Test
    fun generateSchedule() {
        val workoutPlans:MutableList<WorkoutPlan> =mutableListOf()
        val exercises:List<Exercise> =emptyList()
        viewModel.generateSchedule()
        assertEquals(viewModel.workoutPlans,workoutPlans)
        assertEquals(viewModel.exercises.value,exercises)
    }

    @Test
    fun loadScheduleForDate() {
        val exercise:List<Exercise> =emptyList()
        viewModel.generateSchedule()
        viewModel.loadScheduleForDate("2025-11-02")
        assertEquals(viewModel.exercises,exercise)

    }

}