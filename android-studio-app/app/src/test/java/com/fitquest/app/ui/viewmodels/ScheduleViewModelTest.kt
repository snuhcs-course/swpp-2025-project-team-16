package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import com.fitquest.app.model.Schedule
import com.fitquest.app.repository.ScheduleRepository
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

@RunWith(MockitoJUnitRunner::class)
class ScheduleViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var viewModel: ScheduleViewModel

    @Mock
    private lateinit var repository: ScheduleRepository

    private lateinit var viewModelFactory: ScheduleViewModelFactory

    @Before
    fun setUp() {
        viewModelFactory= ScheduleViewModelFactory(repository)
        viewModel= viewModelFactory.create(ScheduleViewModel::class.java)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getSchedules()=runTest {
        val scheduleList= listOf(Schedule(scheduledDate = "2025-11-06", activity = "squat", startTime = "07:00", endTime = "09:00"))
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
        val scheduleList= listOf(Schedule(scheduledDate = "2025-11-06", activity = "squat", startTime = "07:00", endTime = "09:00"))
        Mockito.`when`(repository.getSchedules("planned")).thenReturn(scheduleList)
        val scheduleList2= listOf(Schedule(id=0,scheduledDate = "2025-11-07", activity = "squat", startTime = "07:00", endTime = "09:00"))
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