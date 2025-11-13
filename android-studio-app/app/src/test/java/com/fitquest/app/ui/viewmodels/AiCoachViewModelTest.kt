package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AiCoachViewModelTest {

    // Rule to execute LiveData updates instantly
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AiCoachViewModel

    @Before
    fun setUp() {
        viewModel = AiCoachViewModel()
    }

    @Test
    fun `initial state is correct`() {
        assertFalse(viewModel.isTraining.value ?: true)
        assertEquals(0, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value)
        assertEquals("", viewModel.feedback.value)
        assertEquals(0, viewModel.formScore.value)
    }

    @Test
    fun `beginTraining updates LiveData correctly`() {
        viewModel.beginTraining()
        assertTrue(viewModel.isTraining.value ?: false)
        assertEquals(0, viewModel.repCount.value)
        assertEquals(0, viewModel.points.value)
        assertEquals("Get ready! 🚀", viewModel.feedback.value)
    }

    @Test
    fun `pauseTraining updates LiveData correctly`() {
        // Start training first
        viewModel.beginTraining()
        // Then pause
        viewModel.pauseTraining()
        assertFalse(viewModel.isTraining.value ?: true)
        assertEquals("Workout paused 💪", viewModel.feedback.value)
    }

    @Test
    fun `updateRepCount updates reps and points`() {
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
