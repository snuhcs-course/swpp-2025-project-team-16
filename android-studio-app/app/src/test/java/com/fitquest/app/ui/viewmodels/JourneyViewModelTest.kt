package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.model.Schedule
import com.fitquest.app.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.mockito.Mockito
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
class JourneyViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: ScheduleRepository
    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = Mockito.mock(ScheduleRepository::class.java)
        viewModel = JourneyViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUpcomingSchedules returns only planned schedules`() = runTest {
        // given
        val today = LocalDate.now()

        val plannedSchedule = Schedule(
            id = 1,
            scheduledDate = today,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            activity = "squat",
            repsTarget = 10,
            status = "planned"
        )

        val completedSchedule = Schedule(
            id = 2,
            scheduledDate = today,
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 0),
            activity = "lunge",
            repsTarget = 10,
            repsDone = 10,
            status = "completed"
        )

        Mockito.`when`(repository.getSchedules())
            .thenReturn(listOf(plannedSchedule, completedSchedule))

        // when
        viewModel.loadUpcomingSchedules()

        testDispatcher.scheduler.advanceUntilIdle()

        // then
        val result = viewModel.dailyWorkouts.value

        Assert.assertNotNull(result)
        Assert.assertEquals(1, result!!.size)
        Assert.assertEquals(today, result[0].date)
        Assert.assertEquals(1, result[0].schedules.size)
        Assert.assertEquals(1, result[0].schedules[0].id)
        Assert.assertEquals("planned", result[0].schedules[0].status)
    }

    @Test
    fun `loadUpcomingSchedules groups schedules by date`() = runTest {
        // given
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val todaySchedule = Schedule(
            id = 1,
            scheduledDate = today,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            activity = "squat",
            repsTarget = 10,
            status = "planned"
        )

        val tomorrowSchedule = Schedule(
            id = 2,
            scheduledDate = tomorrow,
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(16, 0),
            activity = "lunge",
            repsTarget = 20,
            status = "planned"
        )

        Mockito.`when`(repository.getSchedules())
            .thenReturn(listOf(todaySchedule, tomorrowSchedule))

        // when
        viewModel.loadUpcomingSchedules()

        testDispatcher.scheduler.advanceUntilIdle()

        // then
        val result = viewModel.dailyWorkouts.value

        Assert.assertNotNull(result)
        Assert.assertEquals(2, result!!.size)

        Assert.assertEquals(today, result[0].date)
        Assert.assertEquals(tomorrow, result[1].date)
    }

    @Test
    fun `loadUpcomingSchedules sorts schedules by start time`() = runTest {
        // given
        val today = LocalDate.now()

        val late = Schedule(
            id = 1,
            scheduledDate = today,
            startTime = LocalTime.of(18, 0),
            endTime = LocalTime.of(19, 0),
            activity = "squat",
            repsTarget = 10,
            status = "planned"
        )

        val early = Schedule(
            id = 2,
            scheduledDate = today,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            activity = "lunge",
            repsTarget = 10,
            status = "planned"
        )

        Mockito.`when`(repository.getSchedules())
            .thenReturn(listOf(late, early))

        // when
        viewModel.loadUpcomingSchedules()

        testDispatcher.scheduler.advanceUntilIdle()

        // then
        val schedules = viewModel.dailyWorkouts.value!![0].schedules

        Assert.assertEquals(2, schedules[0].id)
        Assert.assertEquals(1, schedules[1].id)
    }
}