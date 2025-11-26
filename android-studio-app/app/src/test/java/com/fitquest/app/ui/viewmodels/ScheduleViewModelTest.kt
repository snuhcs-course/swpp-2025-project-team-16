package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.model.Schedule
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.util.DateUtils.formatTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

@RunWith(MockitoJUnitRunner::class)
class ScheduleViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var viewModel: ScheduleViewModel

    @Mock
    private lateinit var repository: ScheduleApiService

    private lateinit var viewModelFactory: ScheduleViewModelFactory

    @Before
    fun setUp() {
        viewModelFactory= ScheduleViewModelFactory(repository)
        viewModel= viewModelFactory.create(ScheduleViewModel::class.java)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getSchedules()=runTest {
        val scheduleList= listOf(Schedule(scheduledDate = LocalDate.now(), activity = "squat", startTime = LocalTime.now(), endTime = LocalTime.now()))
        Mockito.`when`(repository.getSchedules("planned")).thenReturn(scheduleList)
        viewModel.getSchedules()
        advanceUntilIdle()
        val schedules=viewModel.schedules.value
        assertEquals(scheduleList, schedules)
    }

    @Test
    fun getLoading() {
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getError() =runTest{
        Mockito.`when`(repository.getSchedules("planned")).thenThrow(RuntimeException("error"))
        viewModel.getSchedules()
        advanceUntilIdle()
        val error=viewModel.error.value
        assertEquals("error", error)
    }

    @Test
    fun getNewlyGeneratedIds() {
    }

    @Test
    fun testGetSchedules() {
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autoGenerateSchedules()=runTest {
        val scheduleList= listOf(Schedule(scheduledDate = LocalDate.ofYearDay(2025,1), activity = "squat", startTime = LocalTime.now(), endTime = LocalTime.now()))
        Mockito.`when`(repository.getSchedules("planned")).thenReturn(scheduleList)
        val scheduleList2= listOf(Schedule(id=0,scheduledDate = LocalDate.ofYearDay(2025,2), activity = "squat", startTime = LocalTime.now(), endTime = LocalTime.now()))
        Mockito.`when`(repository.autoGenerateSchedules()).thenReturn(scheduleList2)
        viewModel.getSchedules()
        advanceUntilIdle()
        viewModel.autoGenerateSchedules()
        advanceUntilIdle()
        val schedules=viewModel.schedules.value
        assertEquals(scheduleList+scheduleList2, schedules)

    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autoGenerateSchedulesWithError()=runTest {
        Mockito.`when`(repository.autoGenerateSchedules()).thenThrow(RuntimeException("error"))
        viewModel.autoGenerateSchedules()
        advanceUntilIdle()
        val error=viewModel.error.value
        assertEquals("error", error)
    }

}