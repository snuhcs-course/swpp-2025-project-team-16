package com.fitquest.app.ui.viewmodels
/*
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.model.Exercise
import com.fitquest.app.model.Schedule
import com.fitquest.app.repository.ScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import kotlin.collections.component1
import kotlin.collections.component2

class JourneyViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var viewModel: JourneyViewModel
    private lateinit var viewModelFactory: JourneyViewModelFactory

    @Mock
    private lateinit var mockJourneyRepository: ScheduleRepository

    @Mock
    private lateinit var mockApiService: ScheduleApiService

    @Before
    fun setUp() {
        viewModelFactory= JourneyViewModelFactory(mockApiService)
        viewModel = viewModelFactory.create(JourneyViewModel::class.java)
    }

    @Test
    fun `initial selectedWorkout is null`() {
        assertNull(viewModel.dailyWorkouts.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `selectWorkout updates selectedWorkout LiveData`()=runTest {
        val exercise = Exercise("e1", "Push-up", "10 reps", "todo")
        val workoutPlan = Schedule(
            id = 1,
            scheduledDate = LocalDate.now(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            activity = "Push-up"
        )
        val list=listOf(workoutPlan)
        Mockito.`when`(mockJourneyRepository.getSchedules()).thenReturn(list)

        viewModel.loadUpcomingSchedules()
        advanceUntilIdle()
        val grouped = list.groupBy { it.scheduledDate }
        val dailyItems = grouped.map { (date, scheduleList) ->
            DailyWorkoutItem(
                date = date,
                schedules = scheduleList.sortedBy { it.startTime }
            )
        }.sortedBy { it.date }
        assertEquals(dailyItems, viewModel.dailyWorkouts.value)
    }
    @Test
    fun `workoutPlans LiveData is observable`() {
        val plans = viewModel.dailyWorkouts
        assertNotNull(plans)
        assertNull(plans.value)
    }
}


 */