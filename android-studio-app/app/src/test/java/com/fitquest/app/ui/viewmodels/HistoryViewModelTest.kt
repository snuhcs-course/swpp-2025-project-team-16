// HistoryViewModelTest.kt
package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.model.DailySummary
import com.fitquest.app.model.Schedule
import com.fitquest.app.model.Session
import com.fitquest.app.repository.DailySummaryRepository
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalCoroutinesApi
class HistoryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule() // LiveData 동기화

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var sessionRepository: SessionRepository

    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        dailySummaryRepository = mock(DailySummaryRepository::class.java)
        scheduleRepository = mock(ScheduleRepository::class.java)
        sessionRepository = mock(SessionRepository::class.java)

        viewModel = HistoryViewModel(dailySummaryRepository, scheduleRepository, sessionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHistory should populate dailyHistories correctly`() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Mock schedules
        val schedules = listOf(
            Schedule(id = 1, scheduledDate = yesterday, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), activity = "squat", repsTarget = 10, repsDone = 10, status = "completed"),
            Schedule(id = 2, scheduledDate = today, startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0), activity = "lunge", repsTarget = 10, repsDone = 0, status = "planned")
        )
        `when`(scheduleRepository.getSchedules()).thenReturn(schedules)

        // Mock sessions
        val sessions = listOf(
            Session(id = 1, activity = "squat", repsCount = 10, schedule = null, createdAt = LocalDateTime.now().minusDays(1)),
            Session(id = 2, activity = "squat", repsCount = 10, schedule = null, createdAt = LocalDateTime.now())
        )
        `when`(sessionRepository.getSessions()).thenReturn(sessions)

        // Mock daily summaries
        val summaries = listOf(
            DailySummary(date = yesterday, summaryText = "Summary for yesterday")
        )
        `when`(dailySummaryRepository.getDailySummaries()).thenReturn(summaries)

        // Call the method
        viewModel.loadHistory()
        testDispatcher.scheduler.advanceUntilIdle() // 코루틴 실행 대기

        val dailyHistories = viewModel.dailyHistories.value
        assertNotNull(dailyHistories)
        assertEquals(2, dailyHistories!!.size)

        val yesterdayItem = dailyHistories.first { it.date == yesterday }
        assertEquals("Summary for yesterday", yesterdayItem.summaryText)
        assertEquals(1, yesterdayItem.schedules.size)
        assertEquals(1, yesterdayItem.sessions.size)

        val todayItem = dailyHistories.first { it.date == today }
        assertEquals("", todayItem.summaryText)
        assertEquals(0, todayItem.schedules.size) // today schedule is planned, should be filtered out
        assertEquals(1, todayItem.sessions.size)
    }
}
