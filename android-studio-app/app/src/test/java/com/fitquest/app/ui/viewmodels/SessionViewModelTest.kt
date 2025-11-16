package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import com.fitquest.app.model.Session
import com.fitquest.app.repository.SessionRepository
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
class SessionViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()
    private lateinit var viewModel: SessionViewModel
    @Mock
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        viewModel= SessionViewModel(repository)
    }

    @Test
    fun getCurrentSession() {
    }

    @Test
    fun getErrorMessage() {
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startSession()=runTest {
        Mockito.`when`(repository.startSession("squat",0)).thenReturn(Result.success(Session(activity = "squat")))
        viewModel.startSession("squat",0)
        advanceUntilIdle()
        assertEquals(Session(activity = "squat"),viewModel.currentSession.value)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startSessionWithError()=runTest {
        Mockito.`when`(repository.startSession("squat",0)).thenReturn(Result.failure(RuntimeException("Error")))
        viewModel.startSession("squat",0)
        advanceUntilIdle()
        assertEquals("Session creation failed: Error",viewModel.errorMessage.value)
        assertNull(viewModel.currentSession.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun endSession()=runTest {
        Mockito.`when`(repository.startSession("squat",0)).thenReturn(Result.success(Session(activity = "squat",id=0)))
        viewModel.startSession("squat",0)
        advanceUntilIdle()
        Mockito.`when`(repository.endSession(0,5,50)).thenReturn(Result.success(Session(activity = "squat",id=0)))
        viewModel.endSession(5,50)
        advanceUntilIdle()
        assertEquals(Session(activity = "squat",id=0),viewModel.currentSession.value)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun endSessionWithError()=runTest {
        Mockito.`when`(repository.startSession("squat",0)).thenReturn(Result.success(Session(activity = "squat",id=0)))
        viewModel.startSession("squat",0)
        advanceUntilIdle()
        Mockito.`when`(repository.endSession(0,5,50)).thenReturn(Result.failure(RuntimeException("Error")))
        viewModel.endSession(5,50)
        advanceUntilIdle()
        assertEquals("Session completion failed: Error",viewModel.errorMessage.value)
    }

}