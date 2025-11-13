package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.model.Exercise
import com.fitquest.app.model.WorkoutPlan
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class JourneyViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        viewModel = JourneyViewModel()
    }

    @Test
    fun `initial selectedWorkout is null`() {
        assertNull(viewModel.selectedWorkout.value)
    }

    @Test
    fun `selectWorkout updates selectedWorkout LiveData`() {
        val exercise = Exercise("e1", "Push-up", "10 reps", "todo")
        val workoutPlan = WorkoutPlan(
            id = "plan1",
            date = "2024-05-21",
            exercises = listOf(exercise),
            isCompleted = false,
            points = 100,
            feedback = "Good job",
            startTime = "10:00",
            finishTime = "11:00"
        )

        viewModel.selectWorkout(workoutPlan)

        assertEquals(workoutPlan, viewModel.selectedWorkout.value)
    }

    @Test
    fun `clearSelection sets selectedWorkout LiveData to null`() {
        // First, select a workout
        val exercise = Exercise("e1", "Push-up", "10 reps", "todo")
        val workoutPlan = WorkoutPlan(
            id = "plan1",
            date = "2024-05-21",
            exercises = listOf(exercise),
            isCompleted = false,
            points = 100,
            feedback = "Good job",
            startTime = "10:00",
            finishTime = "11:00"
        )
        viewModel.selectWorkout(workoutPlan)

        // Then, clear the selection
        viewModel.clearSelection()

        assertNull(viewModel.selectedWorkout.value)
    }

    @Test
    fun `workoutPlans LiveData is observable`() {
        val plans = viewModel.workoutPlans
        assertNotNull(plans)
        assertNull(plans.value)
    }
}
