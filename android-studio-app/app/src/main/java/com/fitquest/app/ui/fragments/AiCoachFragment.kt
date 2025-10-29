package com.fitquest.app.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fitquest.app.R
import com.fitquest.app.ui.coachutils.OverlayView
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * AiCoachFragment
 *
 * - Begin Training ↔ Pause Training 버튼
 *   · Begin Training: 초록/▶  -> 훈련 시작, pose 분석 시작, HUD/Overlay 표시
 *   · Pause Training: 빨강/■ -> 훈련 일시정지, pose 분석 중단, HUD/Overlay 숨김
 *
 * - Switch Camera: 전/후면 전환
 *
 * - HUD: Reps/XP 카드, ANALYZING 배지, Form Quality Progress bar
 *   -> 훈련 중일 때만 보여줌
 *
 * - AI Coach 메시지:
 *   · idle: "Position yourself in frame"
 *   · training: "Analyzing form... 🔍"
 *
 * - PoseLandmarkerHelper:
 *   CameraX ImageAnalysis에서 프레임을 받고,
 *   관절 landmarks 결과를 OverlayView에 그려준다.
 */
class AiCoachFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    // --- UI refs ---
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    private lateinit var currentExercise: TextView

    private lateinit var startPauseButton: MaterialButton // Begin ↔ Pause
    private lateinit var switchCameraButton: MaterialButton // Switch Camera

    // HUD 내부 텍스트들
    private lateinit var repCountText: TextView      // tvRepCount
    private lateinit var pointsText: TextView        // tvXpPoints
    private lateinit var feedbackText: TextView      // tvFeedback
    private lateinit var progressBar: LinearProgressIndicator // progressFormQuality

    // HUD 컨테이너들 (훈련 중일 때만 보여줄 큰 블럭)
    private lateinit var hudTopContainer: View       // Reps / XP 카드
    private lateinit var recordingIndicator: View    // ANALYZING 빨간 배지
    private lateinit var formScoreContainer: View    // Form Quality 영역 전체

    // --- Camera / Pose ---
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT // 기본 전면

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    // --- ViewModel ---
    private val coachViewModel: AiCoachViewModel by activityViewModels()

    // --- Local state mirrored to UI ---
    private var isTraining = false
    private var repCount = 0
    private var points = 0

    private val COACH_MSG_IDLE = "Position yourself in frame"
    private val COACH_MSG_ANALYZING = "Analyzing form... 🔍"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ai_coach, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. View binding
        previewView = view.findViewById(R.id.cameraPreview)
        overlayView = view.findViewById(R.id.overlay)

        currentExercise = view.findViewById(R.id.tvCurrentExerciseEmoji)

        startPauseButton = view.findViewById(R.id.btnStartWorkout)
        switchCameraButton = view.findViewById(R.id.btnToggleCamera)

        repCountText = view.findViewById(R.id.tvRepCount)
        pointsText = view.findViewById(R.id.tvXpPoints)
        feedbackText = view.findViewById(R.id.tvFeedback)
        progressBar = view.findViewById(R.id.progressFormQuality)

        hudTopContainer = view.findViewById(R.id.hudTopContainer)
        recordingIndicator = view.findViewById(R.id.recordingIndicator)
        formScoreContainer = view.findViewById(R.id.formScoreContainer)

        // 2. Camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 3. PoseLandmarkerHelper 초기화 (GPU delegate 등은 ViewModel 값 사용)
        cameraExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                minPoseDetectionConfidence = coachViewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = coachViewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = coachViewModel.currentMinPosePresenceConfidence,
                currentModel = coachViewModel.currentModel,
                currentDelegate = coachViewModel.currentDelegate,
                runningMode = RunningMode.LIVE_STREAM,
                context = requireContext(),
                poseLandmarkerHelperListener = this
            )
        }

        // 4. Camera permission & setup
        if (allPermissionsGranted()) {
            setupCameraProvider()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // 5. Button listeners
        startPauseButton.setOnClickListener {
            toggleTraining()
        }

        switchCameraButton.setOnClickListener {
            toggleCameraLens()
        }

        // switch camera 아이콘 (왼쪽에 아이콘 -> 텍스트)
        switchCameraButton.icon = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_switch_camera
        )
        switchCameraButton.iconTint = ContextCompat.getColorStateList(
            requireContext(),
            R.color.cyan
        )
        switchCameraButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START

        // 6. 초기 UI 세팅
        feedbackText.text = COACH_MSG_IDLE
        applyTrainingButtonStyle()
        updateTrainingUiState()
    }

    // =======================
    // CameraX setup / binding
    // =======================

    private fun setupCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            // 처음엔 훈련 전이니까 분석 없이 Preview만
            bindCameraUseCases(includeAnalyzer = false)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * includeAnalyzer=true 면 Pose 분석기(ImageAnalysis)까지 바인딩,
     * false면 Preview만 바인딩.
     */
    private fun bindCameraUseCases(includeAnalyzer: Boolean) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageAnalyzer = if (includeAnalyzer) {
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        detectPose(imageProxy)
                    }
                }
        } else {
            null
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()

            val useCases = mutableListOf<UseCase>(preview)
            imageAnalyzer?.let { useCases.add(it) }

            provider.bindToLifecycle(
                this,
                cameraSelector,
                *useCases.toTypedArray()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleCameraLens() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        // 현재 isTraining 상태를 유지한 채로 다시 바인딩
        bindCameraUseCases(includeAnalyzer = isTraining)
    }

    // =======================
    // Pose inference callbacks
    // =======================

    /**
     * CameraX analyzer에서 프레임마다 불림.
     * 프레임을 PoseLandmarkerHelper에 던져주면,
     * onResults()에서 landmark 결과를 받을 수 있다.
     */
    private fun detectPose(imageProxy: ImageProxy) {
        if (isTraining && ::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = (lensFacing == CameraSelector.LENS_FACING_FRONT)
            )
        } else {
            imageProxy.close()
        }
    }

    /**
     * PoseLandmarkerHelper.LandmarkerListener 구현부:
     * MediaPipe가 landmark 결과를 내릴 때마다 호출된다.
     * 여기서 OverlayView에 스켈레톤과 각도 텍스트를 그린다.
     */
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (!isTraining) return@runOnUiThread

            val result = resultBundle.results.firstOrNull() ?: return@runOnUiThread
            overlayView.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            overlayView.invalidate()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        // TODO: Log / Toast if you want
    }

    // =======================
    // Training control logic
    // =======================

    private fun toggleTraining() {
        if (isTraining) {
            pauseWorkout()
        } else {
            beginWorkout()
        }
    }

    private fun beginWorkout() {
        isTraining = true
        repCount = 0
        points = 0

        // ViewModel에도 반영 (세션 상태 유지용)
        coachViewModel.beginTraining()

        // 코치 텍스트 -> Analyzing...
        feedbackText.text = COACH_MSG_ANALYZING

        // 버튼 스타일: Pause Training (빨강/■)
        applyTrainingButtonStyle()

        // HUD & Overlay 보여주기
        updateTrainingUiState()

        // 카메라 다시 바인딩 (이번엔 analyzer 포함해서 pose 모델 활성화)
        bindCameraUseCases(includeAnalyzer = true)
    }

    private fun pauseWorkout() {
        isTraining = false

        coachViewModel.pauseTraining()

        // 코치 텍스트 -> idle 멘트로 복귀
        feedbackText.text = COACH_MSG_IDLE

        // 버튼 스타일: Begin Training (초록/▶)
        applyTrainingButtonStyle()

        // HUD & Overlay 숨기기
        updateTrainingUiState()

        // 오버레이 지우기 (스켈레톤 안 남게)
        overlayView.clear()

        // 카메라를 다시 바인딩하되 analyzer 제거해서 pose 중단
        bindCameraUseCases(includeAnalyzer = false)
    }

    /**
     * isTraining에 따라 HUD 카드들 + overlayView 자체를 show/hide.
     * - hudTopContainer : Reps/XP 카드
     * - recordingIndicator : 빨간 ANALYZING 배지
     * - formScoreContainer : Form Quality 박스
     * - overlayView : 스켈레톤 오버레이
     */
    private fun updateTrainingUiState() {
        val hudVisibility = if (isTraining) View.VISIBLE else View.GONE

        hudTopContainer.visibility = hudVisibility
        recordingIndicator.visibility = hudVisibility
        formScoreContainer.visibility = hudVisibility
        overlayView.visibility = hudVisibility
    }

    /**
     * Begin / Pause 버튼 비주얼(색상, 아이콘, 텍스트) 업데이트
     */
    private fun applyTrainingButtonStyle() {
        if (isTraining) {
            // Pause Training 스타일 (빨강, 정지 사각형)
            startPauseButton.text = "Pause Training"
            startPauseButton.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_pause_square
            )
            startPauseButton.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(),
                R.color.error_red
            )
            startPauseButton.setTextColor(Color.WHITE)
            startPauseButton.iconTint = ContextCompat.getColorStateList(
                requireContext(),
                android.R.color.white
            )
            startPauseButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        } else {
            // Begin Training 스타일 (초록, ▶)
            startPauseButton.text = "Begin Training"
            startPauseButton.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_begin_triangle
            )
            startPauseButton.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(),
                R.color.success_green
            )
            startPauseButton.setTextColor(Color.WHITE)
            startPauseButton.iconTint = ContextCompat.getColorStateList(
                requireContext(),
                android.R.color.white
            )
            startPauseButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        }
    }

    // HUD 숫자/폼 점수 업데이트 도우미 (나중에 Pose logic이 reps/폼 계산하면 여기 호출)
    fun updateRepCount(count: Int) {
        repCount = count
        points = count * 10

        coachViewModel.updateRepCount(count)

        repCountText.text = count.toString()
        pointsText.text = "+$points"
    }

    fun updateFormFeedback(feedback: String, score: Int) {
        coachViewModel.updateFormFeedback(feedback, score)

        feedbackText.text = feedback
        progressBar.progress = score
    }

    // =======================
    // Permission & lifecycle
    // =======================

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            setupCameraProvider()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.clearPoseLandmarker()
        }
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
