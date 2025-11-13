package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.data.remote.ScheduleResponse
import com.fitquest.app.model.Exercise
import com.fitquest.app.model.WorkoutPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import retrofit2.Response

@ExperimentalCoroutinesApi
class ScheduleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApiService: ScheduleApiService

    private lateinit var viewModel: ScheduleViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ScheduleViewModel()
        RetrofitClient.scheduleApiService = mockApiService
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        assertTrue(viewModel.exercises.value.orEmpty().isEmpty())
        assertFalse(viewModel.isEditing.value ?: true)
    }

    @Test
    fun `loadScheduleForDate updates exercises LiveData`() {
        val exercises1 = listOf(Exercise(id = "1", name = "Push-up", detail = "3 sets", status = "todo"))
        val exercises2 = listOf(Exercise(id = "2", name = "Squat", detail = "10 reps", status = "todo"))
        viewModel.workoutPlans.addAll(listOf(
            WorkoutPlan(id = "plan1", date = "2024-01-01", exercises = exercises1, startTime = "10:00", finishTime = "11:00"),
            WorkoutPlan(id = "plan2", date = "2024-01-02", exercises = exercises2, startTime = "10:00", finishTime = "11:00")
        ))

        viewModel.loadScheduleForDate("2024-01-02")
        assertEquals(exercises2, viewModel.exercises.value)

        viewModel.loadScheduleForDate("2024-01-03")
        assertTrue(viewModel.exercises.value.orEmpty().isEmpty())
    }

    @Test
    fun `add and remove exercises modify list`() {
        val exercise = Exercise(id = "1", name = "Plank", detail = "60 seconds", status = "todo")
        viewModel.addExercise(exercise)
        assertEquals(listOf(exercise), viewModel.exercises.value)

        viewModel.removeExercise("1")
        assertTrue(viewModel.exercises.value.orEmpty().isEmpty())
    }

    @Test
    fun `editing state changes correctly`() {
        viewModel.startEditing()
        assertTrue(viewModel.isEditing.value ?: false)

        viewModel.cancelEditing()
        assertFalse(viewModel.isEditing.value ?: true)
    }

    @Test
    fun `loadAllSchedules handles success`() = runTest {
        val serverResponse = listOf(
            ScheduleResponse(
                id = "1",
                date = "2024-01-01",
                startTime = "10:00",
                finishTime = "11:00",
                point = 100,
                isCompleted = false,
                feedback = "",
                exercises = listOf(Exercise(name = "Squat", detail = "10 reps", status = "todo"))
            )
        )
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.success(serverResponse))

        viewModel.loadAllSchedules("fake-token")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("SUCCESS", viewModel.message.value)
        assertEquals(1, viewModel.workoutPlans.size)
        assertEquals("2024-01-01", viewModel.workoutPlans[0].date)
    }

    @Test
    fun `loadAllSchedules handles failure`() = runTest {
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.error(404, okhttp3.ResponseBody.create(null, "")))

        viewModel.loadAllSchedules("fake-token")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed: 404", viewModel.message.value)
    }
}
