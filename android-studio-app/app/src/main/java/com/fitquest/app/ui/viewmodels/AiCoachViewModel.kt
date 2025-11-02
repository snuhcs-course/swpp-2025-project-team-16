package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper

/**
 * AiCoachViewModel
 *
 * - 운동/코칭 HUD 상태 관리 (reps, xp, form score 등)
 * - 포즈 랜드마커 설정값 (delegate, confidence 등)
 *
 * 이 ViewModel 하나로 Fragment에서 다 참조하게 된다.
 */
class AiCoachViewModel : ViewModel() {

    // ==========================
    // Workout / HUD state
    // ==========================

    private val _selectedExercise = MutableLiveData<String>("")
    val selectedExercise: LiveData<String> = _selectedExercise

    private val _isTraining = MutableLiveData<Boolean>(false)
    val isTraining: LiveData<Boolean> = _isTraining

    private val _repCount = MutableLiveData<Int>(0)
    val repCount: LiveData<Int> = _repCount

    private val _points = MutableLiveData<Int>(0)
    val points: LiveData<Int> = _points

    private val _feedback = MutableLiveData<String>("")
    val feedback: LiveData<String> = _feedback

    private val _formScore = MutableLiveData<Int>(0)
    val formScore: LiveData<Int> = _formScore

    fun selectExercise(exercise: String) {
        _selectedExercise.value = exercise
    }

    fun beginTraining() {
        _isTraining.value = true
        _repCount.value = 0
        _points.value = 0
        _feedback.value = "Get ready! 🚀"
    }

    fun pauseTraining() {
        _isTraining.value = false
        _feedback.value = "Workout paused 💪"
        saveWorkoutSession()
    }

    fun updateRepCount(count: Int) {
        _repCount.value = count
        _points.value = count * 10
    }

    fun updateFormFeedback(feedback: String, score: Int) {
        _feedback.value = feedback
        _formScore.value = score
    }

    private fun saveWorkoutSession() {
        val exercise = _selectedExercise.value ?: ""
        val reps = _repCount.value ?: 0
        val xp = _points.value ?: 0
        val avgScore = _formScore.value ?: 0

        // TODO: backend/local DB 저장
        // exercise, reps, xp, avgScore, timestamp 등
    }

    // ==========================
    // Pose Landmarker settings
    // (원래 MainViewModel가 하던 일)
    // ==========================

    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL
    private var _delegate = PoseLandmarkerHelper.DELEGATE_GPU
    private var _minPoseDetectionConfidence =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence =
        PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence =
        PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE

    val currentModel: Int get() = _model
    val currentDelegate: Int get() = _delegate
    val currentMinPoseDetectionConfidence: Float get() = _minPoseDetectionConfidence
    val currentMinPoseTrackingConfidence: Float get() = _minPoseTrackingConfidence
    val currentMinPosePresenceConfidence: Float get() = _minPosePresenceConfidence

    fun setDelegate(delegate: Int) { _delegate = delegate }
    fun setModel(model: Int) { _model = model }

    fun setMinPoseDetectionConfidence(v: Float) {
        _minPoseDetectionConfidence = v
    }

    fun setMinPoseTrackingConfidence(v: Float) {
        _minPoseTrackingConfidence = v
    }

    fun setMinPosePresenceConfidence(v: Float) {
        _minPosePresenceConfidence = v
    }
}
