package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.model.EndSessionRequest
import com.fitquest.app.model.Session
import com.fitquest.app.model.StartSessionRequest
import com.fitquest.app.model.WorkoutResult
import com.fitquest.app.repository.SessionRepository
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response
/*
@RunWith(MockitoJUnitRunner::class)
class AiCoachViewModelTest {

    // Rule to execute LiveData updates instantly
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var viewModel: AiCoachViewModel

    @Mock
    private lateinit var repository: SessionApiService

    private lateinit var viewModelFactory: AiCoachViewModelFactory

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        viewModelFactory=AiCoachViewModelFactory(repository)
        viewModel = viewModelFactory.create(AiCoachViewModel::class.java)
    }
    @Test
    fun `initial state is correct`() {
        assertFalse(viewModel.isTraining.value ?: true)
        assertEquals(0, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value)
        assertEquals("", viewModel.feedback.value)
        assertEquals(0, viewModel.formScore.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `beginTraining updates LiveData correctly`() =runTest{
        Mockito.`when`(repository.startSession(StartSessionRequest("squat"))).thenReturn(Response.success(Session(activity="squat")))
        viewModel.beginTraining("squat",0)
        advanceUntilIdle()
        assertTrue(viewModel.isTraining.value ?: false)
        assertEquals(0, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value)
        assertEquals("Get ready! 🚀", viewModel.feedback.value)
    }
    @Test
    fun `beginTraining updates LiveData correct`() =runTest{
        Mockito.`when`(repository.startSession(StartSessionRequest("squat"))).thenReturn(Response.error(0,"error".toResponseBody()))
        viewModel.beginTraining("squat",0)
        assertFalse(viewModel.isTraining.value ?: false)
        assertEquals(0, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value)
        assertEquals("", viewModel.feedback.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pauseTraining updates LiveData correctly`() =runTest{
        // Start training first
        Mockito.`when`(repository.startSession(StartSessionRequest("squat"))).thenReturn(Response.success(Session(activity="squat")))
        Mockito.`when`(repository.endSession(0, EndSessionRequest(0, 0, 0))).thenReturn(Response.success(Session(activity="squat")))
        viewModel.beginTraining("squat",0)
        // Then pause
        advanceUntilIdle()
        viewModel.pauseTraining(WorkoutResult(0,0,0))
        advanceUntilIdle()
        assertFalse(viewModel.isTraining.value ?: true)
        assertEquals("squat",viewModel.selectedExercise.value)
        assertEquals("Session saved! Earned 0 XP! 💪", viewModel.feedback.value)
    }

    @Test
    fun `updateRepCount updates reps and points`() =runTest{
        Mockito.`when`(repository.startSession(StartSessionRequest("squat"))).thenReturn(Response.success(Session(activity="squat")))
        viewModel.beginTraining("squat",0)
        viewModel.updateRepCount(5)
        assertEquals(5, viewModel.repCount.value)
        assertEquals(50, viewModel.points.value) // 5 * 10

        viewModel.updateRepCount(12)
        assertEquals(12, viewModel.repCount.value)
        assertEquals(120, viewModel.points.value) // 12 * 10
    }

    @Test
    fun `updateFormFeedback updates feedback and score`() {
        viewModel.updateFormFeedback("Good form!", 95)
        assertEquals("Good form!", viewModel.feedback.value)
        assertEquals(95, viewModel.formScore.value)
    }

    @Test
    fun `setDelegate updates currentDelegate`() {
        viewModel.setDelegate(PoseLandmarkerHelper.DELEGATE_CPU)
        assertEquals(PoseLandmarkerHelper.DELEGATE_CPU, viewModel.currentDelegate)
    }

    @Test
    fun `setModel updates currentModel`() {
        viewModel.setModel(PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_LITE)
        assertEquals(PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_LITE, viewModel.currentModel)
    }

    @Test
    fun `confidence setters update values`() {
        viewModel.setMinPoseDetectionConfidence(0.6f)
        assertEquals(0.6f, viewModel.currentMinPoseDetectionConfidence)

        viewModel.setMinPoseTrackingConfidence(0.7f)
        assertEquals(0.7f, viewModel.currentMinPoseTrackingConfidence)

        viewModel.setMinPosePresenceConfidence(0.8f)
        assertEquals(0.8f, viewModel.currentMinPosePresenceConfidence)
    }
}
*/