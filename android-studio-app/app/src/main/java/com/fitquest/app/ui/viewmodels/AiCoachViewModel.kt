package com.fitquest.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.model.WorkoutResult
import com.fitquest.app.repository.SessionRepository
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import com.fitquest.app.util.ActivityUtils
import com.fitquest.app.util.TargetType
import kotlinx.coroutines.launch

/**
 * AiCoachViewModel
 *
 * - 운동/코칭 HUD 상태 관리 (reps, xp, form score 등)
 * - 포즈 랜드마커 설정값 (delegate, confidence 등)
 *
 * 이 ViewModel 하나로 Fragment에서 다 참조하게 된다.
 */
class AiCoachViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    // ==========================
    // Workout / HUD state
    // ==========================

    // ✅ 세션 준비 중 상태 추가 (카운트다운 포함)
    private val _sessionPreparing = MutableLiveData<Boolean>(false)
    val sessionPreparing: LiveData<Boolean> = _sessionPreparing

    private val _currentSessionId = MutableLiveData<Int?>(null)
    val currentSessionId: LiveData<Int?> = _currentSessionId

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

    // ✅ API 오류 메시지 전용 LiveData 추가 (Fragment에서 Toast 띄우기 위함)
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _formScore = MutableLiveData<Int>(0)
    val formScore: LiveData<Int> = _formScore

    private val _sessionActive = MutableLiveData(false)
    val sessionActive: LiveData<Boolean> = _sessionActive

    // ✅ 세션 준비 상태 설정 (카운트다운 시작)
    fun setSessionPreparing(preparing: Boolean) {
        _sessionPreparing.value = preparing
        // 준비 중이거나 트레이닝 중이면 세션 활성화
        updateSessionActiveState()
    }

    // ✅ 세션 활성화 상태를 통합 관리
    private fun updateSessionActiveState() {
        _sessionActive.value = _sessionPreparing.value == true || _isTraining.value == true
    }

    fun beginTraining(activity: String, scheduleId: Int? = null) {
        // 이미 트레이닝 중이거나 준비 중이라면 무시
        if (_isTraining.value == true || _sessionPreparing.value == true) return

        _selectedExercise.value = activity

        // ✅ 준비 상태 시작 (카운트다운 시작 시점)
        _sessionPreparing.value = true
        updateSessionActiveState()

        viewModelScope.launch {
            val result = sessionRepository.startSession(activity, scheduleId)

            result.onSuccess { session ->
                _currentSessionId.value = session.id

                // ✅ 준비 완료, 트레이닝 시작
                _sessionPreparing.value = false
                _isTraining.value = true
                updateSessionActiveState()

                _repCount.value = 0
                _points.value = 0
                _feedback.value = "Get ready! 🚀"

            }.onFailure { e ->
                // ✅ 실패 시 모든 상태 해제
                _sessionPreparing.value = false
                _isTraining.value = false
                updateSessionActiveState()

                // ✅ 오류 메시지를 전용 LiveData에 할당하여 Fragment에 전달
                _errorMessage.value = "Session start failed: ${e.message ?: "Unknown error"}"

                // 피드백 텍스트에는 일반적인 실패 메시지를 표시
                _feedback.value = "Session start failed. Check connection."
            }
        }
    }

    // ✅ pauseTraining 함수 시그니처 및 로직 변경 (WorkoutResult 사용)
    fun pauseTraining(result: WorkoutResult) {
        val sessionId = _currentSessionId.value ?: run {
            _isTraining.value = false
            _sessionPreparing.value = false
            _currentSessionId.value = null
            updateSessionActiveState()
            _feedback.value = "Workout paused (No active session ID) 💪"
            return
        }

        val reps = result.repsCount
        val duration = result.durationSeconds
        val sessionDurationSeconds = result.sessionDurationSeconds

        viewModelScope.launch {
            val endResult = sessionRepository.endSession(sessionId, reps, duration, sessionDurationSeconds)

            // ✅ 세션 종료 후 모든 상태 초기화
            _isTraining.value = false
            _sessionPreparing.value = false
            updateSessionActiveState()
            _currentSessionId.value = null

            endResult.onSuccess { session ->
                // TODO: 서버에서 받은 실제 XP로 업데이트하는 로직이 필요하지만, 현재는 임시로 표시
                _feedback.value = "Session saved! Earned ${_points.value} XP! 💪"
            }.onFailure { e ->
                // ✅ 오류 메시지를 전용 LiveData에 할당하여 Fragment에 전달
                _errorMessage.value = "Failed to sync session data: ${e.message ?: "Unknown error"}"

                // 피드백 텍스트에는 저장 실패 메시지를 표시
                _feedback.value = "Workout saved locally, but sync failed."
            }
        }
    }

    // ✅ 카운트다운 취소 기능 추가
    fun cancelCountdown() {
        _sessionPreparing.value = false
        updateSessionActiveState()
        _feedback.value = ""
    }

    // ✅ updateRepCount 로직 수정 (TargetType을 사용하여 XP 계산 분리)
    fun updateRepCount(count: Int) {
        _repCount.value = count

        // 현재 선택된 운동의 타입을 확인하여 Reps 기반일 때만 포인트 계산
        val targetType = ActivityUtils.getTargetType(_selectedExercise.value ?: "")
        if (targetType == TargetType.REPS) {
            _points.value = count * 10
        }
        // Duration 기반 운동(Plank)은 count가 초 단위로 올라가지만, XP는 서버가 계산하도록 둡니다.
    }

    fun updateFormFeedback(feedback: String, score: Int) {
        _feedback.value = feedback
        _formScore.value = score
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
