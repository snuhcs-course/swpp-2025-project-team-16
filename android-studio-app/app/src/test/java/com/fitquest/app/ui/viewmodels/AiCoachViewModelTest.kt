package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.fitquest.app.MainDispatcherRule
import com.fitquest.app.model.Session
import com.fitquest.app.model.WorkoutResult
import com.fitquest.app.repository.SessionRepository
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
class AiCoachViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: AiCoachViewModel

    @Mock
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        viewModel = AiCoachViewModel(repository)
    }

    @Test
    fun `setSessionPreparing sets preparing state and updates active session`() {
        viewModel.setSessionPreparing(true)
        assertTrue(viewModel.sessionPreparing.value!!)
        assertTrue(viewModel.sessionActive.value!!)

        viewModel.setSessionPreparing(false)
        assertFalse(viewModel.sessionPreparing.value!!)
        assertFalse(viewModel.sessionActive.value!!)
    }

    @Test
    fun `beginTraining when already training`() = runTest {
        viewModel.setSessionPreparing(false)

        // 이미 training 중 상태로 세팅
        val isTrainingField =
            AiCoachViewModel::class.java.getDeclaredField("_isTraining")
        isTrainingField.isAccessible = true
        (isTrainingField.get(viewModel) as MutableLiveData<Boolean>).value = true

        viewModel.beginTraining("squat", 0)

        Mockito.verify(repository, Mockito.never()).startSession(Mockito.anyString(), Mockito.isNull())
    }

    @Test
    fun `beginTraining when already in preparing state`() = runTest {
        viewModel.setSessionPreparing(true)

        viewModel.beginTraining("squat", 0)

        Mockito.verify(repository, Mockito.never()).startSession(Mockito.anyString(), Mockito.isNull())
    }

    @Test
    fun `beginTraining API failure handling`() = runTest {
        Mockito.`when`(repository.startSession("squat", 0))
            .thenReturn(Result.failure(Exception("Server error")))

        viewModel.beginTraining("squat", 0)
        advanceUntilIdle()

        assertFalse(viewModel.isTraining.value!!)
        assertFalse(viewModel.sessionPreparing.value!!)
        assertTrue(viewModel.errorMessage.value!!.contains("Server error"))
        assertEquals("Session start failed. Check connection.", viewModel.feedback.value)
    }

    @Test
    fun `beginTraining with null scheduleId`() = runTest {
        Mockito.`when`(repository.startSession("squat", null))
            .thenReturn(Result.success(Session(id = 1, activity = "squat")))

        viewModel.beginTraining("squat", null)
        advanceUntilIdle()

        Mockito.verify(repository).startSession("squat", null)
    }

    @Test
    fun `beginTraining LiveData flow for success`() = runTest {
        Mockito.`when`(repository.startSession("squat", 0))
            .thenReturn(Result.success(Session(id = 1, activity = "squat")))

        viewModel.beginTraining("squat", 0)

        // 바로 preparing은 true
        assertTrue(viewModel.sessionPreparing.value!!)

        advanceUntilIdle()

        assertFalse(viewModel.sessionPreparing.value!!)
        assertTrue(viewModel.isTraining.value!!)
    }

    @Test
    fun `pauseTraining with a null session ID`() = runTest {
        viewModel.pauseTraining(WorkoutResult(0, 0, 0))

        assertFalse(viewModel.isTraining.value!!)
        assertEquals(
            "Workout paused (No active session ID) 💪",
            viewModel.feedback.value
        )

        Mockito.verify(repository, Mockito.never()).endSession(
            Mockito.anyInt(),
            Mockito.anyInt(),
            Mockito.anyInt(),
            Mockito.anyInt()
        )
    }

    @Test
    fun `pauseTraining API failure handling`() = runTest {
        Mockito.`when`(repository.startSession("squat", 0))
            .thenReturn(Result.success(Session(1, 1, "squat")))

        Mockito.`when`(
            repository.endSession(
                Mockito.eq(1), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()
            )
        ).thenReturn(Result.failure(Exception("Sync failed")))

        viewModel.beginTraining("squat", 0)
        advanceUntilIdle()

        viewModel.pauseTraining(WorkoutResult(10, 30, 30))
        advanceUntilIdle()

        assertFalse(viewModel.isTraining.value!!)
        assertTrue(viewModel.errorMessage.value!!.contains("Sync failed"))
        assertEquals("Workout saved locally, but sync failed.", viewModel.feedback.value)
    }

    @Test
    fun `cancelCountdown functionality`() {
        viewModel.setSessionPreparing(true)

        viewModel.cancelCountdown()

        assertFalse(viewModel.sessionPreparing.value!!)
        assertFalse(viewModel.sessionActive.value!!)
        assertEquals("", viewModel.feedback.value)
    }

    @Test
    fun `updateRepCount for non REPS based exercises`() {
        viewModel.beginTraining("plank", null)

        viewModel.updateRepCount(5)

        assertEquals(5, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value) // plank는 시간 기반
    }

    @Test
    fun `updateRepCount with zero and negative values`() {
        viewModel.updateRepCount(0)
        assertEquals(0, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value)

        viewModel.updateRepCount(-5)
        assertEquals(-5, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value)
    }

    @Test
    fun `sessionActive LiveData logic when training starts`() = runTest {
        Mockito.`when`(repository.startSession("squat", 0))
            .thenReturn(Result.success(Session(1, 1, "squat")))

        viewModel.beginTraining("squat", 0)
        advanceUntilIdle()

        assertTrue(viewModel.sessionActive.value!!)
    }

    @Test
    fun `sessionActive LiveData logic when training pauses`() = runTest {
        Mockito.`when`(repository.startSession("squat", 0))
            .thenReturn(Result.success(Session(1, 1, "squat")))

        Mockito.`when`(
            repository.endSession(Mockito.eq(1), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())
        ).thenReturn(Result.success(Session(1, 1, "squat")))

        viewModel.beginTraining("squat", 0)
        advanceUntilIdle()

        viewModel.pauseTraining(WorkoutResult(0, 0, 0))
        advanceUntilIdle()

        assertFalse(viewModel.sessionActive.value!!)
    }

    @Test
    fun `Verify getErrorMessage LiveData emission on startSession failure`() = runTest {
        Mockito.`when`(repository.startSession("squat", 0))
            .thenReturn(Result.failure(Exception("No internet")))

        viewModel.beginTraining("squat", 0)
        advanceUntilIdle()

        assertTrue(viewModel.errorMessage.value!!.contains("No internet"))
    }

    @Test
    fun `Verify getErrorMessage LiveData emission on endSession failure`() = runTest {
        Mockito.`when`(repository.startSession("squat", 0))
            .thenReturn(Result.success(Session(1, 1, "squat")))

        Mockito.`when`(
            repository.endSession(Mockito.eq(1), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())
        ).thenReturn(Result.failure(Exception("Server crash")))

        viewModel.beginTraining("squat", 0)
        advanceUntilIdle()

        viewModel.pauseTraining(WorkoutResult(3, 10, 10))
        advanceUntilIdle()

        assertTrue(viewModel.errorMessage.value!!.contains("Server crash"))
    }
}
