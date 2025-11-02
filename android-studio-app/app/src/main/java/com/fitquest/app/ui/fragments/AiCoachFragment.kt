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
import com.fitquest.app.ui.coachutils.counter.BaseCounter
import com.fitquest.app.ui.coachutils.counter.SquatCounter
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.CountDownTimer




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

    // HUD 컨테이너
    private lateinit var hudTopContainer: View
    private lateinit var recordingIndicator: View
    private lateinit var formScoreContainer: View

    // --- Camera / Pose ---
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT // 기본 전면

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    private lateinit var tvCountdown: TextView
    private var countdownTimer: CountDownTimer? = null
    private var isCountingDown = false


    // --- ViewModel ---
    private val coachViewModel: AiCoachViewModel by activityViewModels()

    // --- Local state ---
    private var isTraining = false
    private var repCount = 0
    private var points = 0

    // --- Counter (추가) ---
    private var counter: BaseCounter? = null

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

        tvCountdown = view.findViewById(R.id.tvCountdown)


        // 2. Camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 3. PoseLandmarkerHelper 초기화
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
            if (isTraining) {
                // 진행 중이면 일시정지
                pauseWorkout()
            } else {
                // 아직 훈련 전이면 10초 카운트다운 후 시작
                startCountdownThenBegin(10)
            }
        }

        switchCameraButton.setOnClickListener { toggleCameraLens()
        }

        // switch camera 아이콘
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
            // 처음엔 훈련 전이므로 Preview만
            bindCameraUseCases(includeAnalyzer = false)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(includeAnalyzer: Boolean) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

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
        } else null

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
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        bindCameraUseCases(includeAnalyzer = isTraining)
    }

    // =======================
    // Pose inference callbacks
    // =======================

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

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (!isTraining) return@runOnUiThread

            val result = resultBundle.results.firstOrNull() ?: return@runOnUiThread

            // 1) 오버레이 갱신 (스켈레톤/각도 텍스트)
            overlayView.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            overlayView.invalidate()

            // 2) 카운터 갱신 (정규화 랜드마크 → float[33*3])
            val lm = result.landmarks().firstOrNull() ?: return@runOnUiThread
            val pts = FloatArray(lm.size * 3)
            for (i in lm.indices) {
                pts[3 * i]     = lm[i].x()
                pts[3 * i + 1] = lm[i].y()
                pts[3 * i + 2] = lm[i].z()
            }
            val now = System.currentTimeMillis()
            counter?.update(pts, now)

            // 3) UI 반영 (reps/points/phase)
            updateRepCount(counter?.count ?: 0)
            feedbackText.text = "Phase: ${counter?.phase ?: "-"}"
        }
    }

    override fun onError(error: String, errorCode: Int) {
        // 필요 시 로깅/토스트
    }

    // =======================
    // Training control logic
    // =======================

    private fun toggleTraining() {
        if (isTraining) pauseWorkout() else beginWorkout()
    }

    private fun beginWorkout() {
        isTraining = true
        repCount = 0
        points = 0

        // 기본: 스쿼트 카운터 사용 (운동선택 UI 붙이면 분기)
        counter = SquatCounter().also { it.reset(System.currentTimeMillis()) }

        coachViewModel.beginTraining()
        feedbackText.text = COACH_MSG_ANALYZING
        applyTrainingButtonStyle()
        updateTrainingUiState()

        // 분석 켜기
        bindCameraUseCases(includeAnalyzer = true)
    }

    private fun pauseWorkout() {
        // 카운트다운 중이었다면 취소
        if (isCountingDown) cancelCountdown()

        isTraining = false
        coachViewModel.pauseTraining()
        feedbackText.text = COACH_MSG_IDLE
        applyTrainingButtonStyle()
        updateTrainingUiState()

        overlayView.clear()
        counter = null
        bindCameraUseCases(includeAnalyzer = false)
    }


    private fun updateTrainingUiState() {
        val hudVisibility = if (isTraining) View.VISIBLE else View.GONE
        hudTopContainer.visibility = hudVisibility
        recordingIndicator.visibility = hudVisibility
        formScoreContainer.visibility = hudVisibility
        overlayView.visibility = hudVisibility
    }

    private fun applyTrainingButtonStyle() {
        if (isTraining) {
            startPauseButton.text = "Pause Training"
            startPauseButton.icon = ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_pause_square
            )
            startPauseButton.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(), R.color.error_red
            )
            startPauseButton.setTextColor(Color.WHITE)
            startPauseButton.iconTint = ContextCompat.getColorStateList(
                requireContext(), android.R.color.white
            )
            startPauseButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        } else {
            startPauseButton.text = "Begin Training"
            startPauseButton.icon = ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_begin_triangle
            )
            startPauseButton.backgroundTintList = ContextCompat.getColorStateList(
                requireContext(), R.color.success_green
            )
            startPauseButton.setTextColor(Color.WHITE)
            startPauseButton.iconTint = ContextCompat.getColorStateList(
                requireContext(), android.R.color.white
            )
            startPauseButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        }
    }

    // HUD 숫자 업데이트 (ViewModel 연동 그대로 유지)
    fun updateRepCount(count: Int) {
        repCount = count
        points = count * 10
        coachViewModel.updateRepCount(count)
        repCountText.text = count.toString()
        pointsText.text = "+$points"
    }

    // =======================
    // Permission & lifecycle
    // =======================

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
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

    private fun startCountdownThenBegin(seconds: Int = 10) {
        if (isCountingDown) return
        isCountingDown = true

        // 카운트다운 동안 HUD/오버레이는 숨기고 버튼 비활성화
        updateTrainingUiStateForCountdown(show = true)
        startPauseButton.isEnabled = false

        tvCountdown.visibility = View.VISIBLE

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                // 10, 9, 8 ... 1 표시 (ceil 효과 위해 +999)
                val remain = ((ms + 999) / 1000L).toInt()
                tvCountdown.text = remain.toString()
            }
            override fun onFinish() {
                tvCountdown.visibility = View.GONE
                startPauseButton.isEnabled = true
                isCountingDown = false
                // 실제 훈련 시작
                beginWorkout()
            }
        }.start()
    }

    /** 카운트다운 중에는 숫자만 보이게 하고 HUD/Overlay는 잠시 숨김 */
    private fun updateTrainingUiStateForCountdown(show: Boolean) {
        if (show) {
            hudTopContainer.visibility = View.GONE
            recordingIndicator.visibility = View.GONE
            formScoreContainer.visibility = View.GONE
            overlayView.visibility = View.GONE
            feedbackText.text = "Get ready... ⏳"
        } else {
            updateTrainingUiState() // 원래 로직으로 복귀
        }
    }


    private fun cancelCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        isCountingDown = false
        tvCountdown.visibility = View.GONE
        feedbackText.text = COACH_MSG_IDLE
        applyTrainingButtonStyle()
        updateTrainingUiState()
    }

    private fun applyCountdownButtonStyle() {
        startPauseButton.text = "Cancel"
        startPauseButton.icon = ContextCompat.getDrawable(
            requireContext(), R.drawable.ic_pause_square
        )
        startPauseButton.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(), R.color.error_red
        )
        startPauseButton.setTextColor(Color.WHITE)
        startPauseButton.iconTint = ContextCompat.getColorStateList(
            requireContext(), android.R.color.white
        )
        startPauseButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
    }




    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        countdownTimer = null
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
