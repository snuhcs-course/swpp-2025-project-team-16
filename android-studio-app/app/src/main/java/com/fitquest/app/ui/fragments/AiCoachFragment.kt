package com.fitquest.app.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fitquest.app.R
import com.fitquest.app.ui.coachutils.OverlayView
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import com.fitquest.app.ui.coachutils.counter.BaseCounter
import com.fitquest.app.ui.coachutils.counter.PlankTimer
import com.fitquest.app.ui.coachutils.counter.SquatCounter
import com.fitquest.app.ui.coachutils.counter.LungeCounter
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Locale
import kotlin.math.exp

class AiCoachFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    private enum class Exercise { SQUAT, PLANK, LUNGE }

    // UI
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var spinnerExercise: Spinner
    private lateinit var tvCurrentExerciseEmoji: TextView
    private lateinit var labelReps: TextView
    private lateinit var startPauseButton: MaterialButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var repCountText: TextView
    private lateinit var pointsText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var hudTopContainer: View
    private lateinit var recordingIndicator: View
    private lateinit var formScoreContainer: View
    private lateinit var tvCountdown: TextView

    // Camera/Pose
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    // VM
    private val coachViewModel: AiCoachViewModel by activityViewModels()

    // State
    private var isTraining = false
    private var isCountingDown = false
    private var countdownTimer: CountDownTimer? = null
    private var repCount = 0
    private var points = 0
    private var selectedExercise: Exercise = Exercise.SQUAT

    private var counter: BaseCounter? = null

    // Tracking lock FSM
    private var trackingLocked = false
    private var badVisFrames = 0
    private var goodVisFrames = 0
    private var disarmUntilMs = 0L

    private val VIS_THRESH = 0.95f
    private val LOWER_NEEDED = intArrayOf(23, 24, 25, 26, 27, 28)
    private val LOWER_REQUIRED = 5
    private val BAD_VIS_LIMIT = 12
    private val GOOD_VIS_LIMIT = 12
    private val DISARM_MS_AFTER_UNLOCK = 300L

    private val COACH_MSG_IDLE = "Position yourself in frame"
    private val COACH_MSG_ANALYZING = "Analyzing form... 🔍"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_ai_coach, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind
        previewView = view.findViewById(R.id.cameraPreview)
        overlayView = view.findViewById(R.id.overlay)
        spinnerExercise = view.findViewById(R.id.spinnerExercise)
        tvCurrentExerciseEmoji = view.findViewById(R.id.tvCurrentExerciseEmoji)
        labelReps = view.findViewById(R.id.labelReps)
        startPauseButton = view.findViewById(R.id.btnStartWorkout)
        switchCameraButton = view.findViewById(R.id.btnSwitchCamera)
        repCountText = view.findViewById(R.id.tvRepCount)
        pointsText = view.findViewById(R.id.tvXpPoints)
        feedbackText = view.findViewById(R.id.tvFeedback)
        progressBar = view.findViewById(R.id.progressFormQuality)
        hudTopContainer = view.findViewById(R.id.hudTopContainer)
        recordingIndicator = view.findViewById(R.id.recordingIndicator)
        formScoreContainer = view.findViewById(R.id.formScoreContainer)
        tvCountdown = view.findViewById(R.id.tvCountdown)

        // Pose helper
        cameraExecutor = Executors.newSingleThreadExecutor()
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

        // Permission
        if (allPermissionsGranted()) setupCameraProvider()
        else ActivityCompat.requestPermissions(
            requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )

        // Spinner (entries는 XML에 정의되어 있음)
        spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val sel = parent?.getItemAtPosition(pos)?.toString() ?: ""
                selectedExercise = mapSelectionToExercise(sel)
                applyExerciseUi(selectedExercise)
                if (isTraining) feedbackText.text = "Exercise changed. Applies on next start."
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedExercise = Exercise.SQUAT
                applyExerciseUi(selectedExercise)
            }
        }

        // Buttons
        startPauseButton.setOnClickListener {
            if (isTraining) pauseWorkout() else startCountdownThenBegin(10)
        }
        switchCameraButton.setOnClickListener { toggleCameraLens() }
        switchCameraButton.setImageResource(R.drawable.ic_switch_camera)
        ImageViewCompat.setImageTintList(
            switchCameraButton,
            ContextCompat.getColorStateList(requireContext(), android.R.color.white)
        )

        // UI init
        feedbackText.text = COACH_MSG_IDLE
        applyTrainingButtonStyle()
        updateTrainingUiState()
        applyExerciseUi(selectedExercise)
    }

    // ---------------- CameraX ----------------

    private fun setupCameraProvider() {
        val f = ProcessCameraProvider.getInstance(requireContext())
        f.addListener({
            cameraProvider = f.get()
            bindCameraUseCases(includeAnalyzer = false)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(includeAnalyzer: Boolean) {
        val provider = cameraProvider ?: return
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        imageAnalyzer = if (includeAnalyzer) {
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { detectPose(it) }
                }
        } else null
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            provider.unbindAll()
            val useCases = mutableListOf<UseCase>(preview)
            imageAnalyzer?.let { useCases.add(it) }
            provider.bindToLifecycle(this, selector, *useCases.toTypedArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleCameraLens() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        bindCameraUseCases(includeAnalyzer = isTraining)
    }

    // --------------- Pose callbacks ---------------

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

            overlayView.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            overlayView.invalidate()

            val lm = result.landmarks().firstOrNull() ?: return@runOnUiThread
            if (lm.size < 33) return@runOnUiThread

            val now = System.currentTimeMillis()

            // 가시성 기반 트래킹 락
            val lowerCnt = lowerBodyVisibleCount(lm, VIS_THRESH)
            val visGood = (lowerCnt >= LOWER_REQUIRED)
            if (trackingLocked) {
                if (visGood) {
                    goodVisFrames++
                    if (goodVisFrames >= GOOD_VIS_LIMIT) {
                        trackingLocked = false
                        goodVisFrames = 0
                        badVisFrames = 0
                        disarmUntilMs = now + DISARM_MS_AFTER_UNLOCK
                        tvCountdown.visibility = View.GONE
                    }
                } else goodVisFrames = 0
                return@runOnUiThread
            } else {
                if (!visGood) {
                    badVisFrames++
                    if (badVisFrames >= BAD_VIS_LIMIT) {
                        trackingLocked = true
                        badVisFrames = 0
                        goodVisFrames = 0
                        tvCountdown.text = "STEP BACK"
                        tvCountdown.visibility = View.VISIBLE
                        return@runOnUiThread
                    }
                } else badVisFrames = 0
            }
            if (now < disarmUntilMs) return@runOnUiThread

            // float[33*3]
            val pts = FloatArray(lm.size * 3)
            for (i in lm.indices) {
                pts[3 * i] = lm[i].x()
                pts[3 * i + 1] = lm[i].y()
                pts[3 * i + 2] = lm[i].z()
            }
            counter?.update(pts, now)

            // ---- UI 반영 ----
            if (selectedExercise == Exercise.PLANK && counter is PlankTimer) {
                val pt = counter as PlankTimer
                // 0.1초 단위 표시
                val seconds = pt.holdSeconds()
                repCountText.text = String.format(Locale.getDefault(), "%.1f", seconds)
                // 내부 count(Int)는 floor(seconds)이므로 기존 VM 업데이트는 그대로 유지
                updateRepCount(counter?.count ?: 0)
            } else {
                // 스쿼트는 정수 reps
                updateRepCount(counter?.count ?: 0)
            }
            feedbackText.text = "Phase: ${counter?.phase ?: "-"}"
        }
    }

    override fun onError(error: String, errorCode: Int) {
        // 필요시 로그/토스트
    }

    // ---------------- Training control ----------------

    fun beginWorkout() {
        isTraining = true
        repCount = 0
        points = 0

        val now = System.currentTimeMillis()
        counter = when (selectedExercise) {
            Exercise.SQUAT -> SquatCounter().also { it.reset(now) }
            Exercise.PLANK -> PlankTimer().also { it.reset(now) }
            Exercise.LUNGE -> LungeCounter().also { it.reset(now) }

        }

        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
        tvCountdown.visibility = View.GONE

        coachViewModel.beginTraining()
        coachViewModel.setSessionActive(true)
        feedbackText.text = COACH_MSG_ANALYZING
        applyTrainingButtonStyle()
        updateTrainingUiState()
        bindCameraUseCases(includeAnalyzer = true)
    }

    private fun pauseWorkout() {
        if (isCountingDown) cancelCountdown()
        isTraining = false
        coachViewModel.pauseTraining()
        coachViewModel.setSessionActive(false)
        feedbackText.text = COACH_MSG_IDLE
        applyTrainingButtonStyle()
        updateTrainingUiState()
        overlayView.clear()
        counter = null
        tvCountdown.visibility = View.GONE

        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
        bindCameraUseCases(includeAnalyzer = false)
    }

    private fun updateTrainingUiState() {
        val v = if (isTraining) View.VISIBLE else View.GONE
        hudTopContainer.visibility = v
        recordingIndicator.visibility = v
        formScoreContainer.visibility = v
        overlayView.visibility = v
    }

    private fun applyTrainingButtonStyle() {
        if (isTraining) {
            startPauseButton.text = "Pause Training"
            startPauseButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_square)
            startPauseButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.error_red)
            startPauseButton.setTextColor(Color.WHITE)
            startPauseButton.iconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
            startPauseButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        } else {
            startPauseButton.text = "Begin Training"
            startPauseButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_begin_triangle)
            startPauseButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.success_green)
            startPauseButton.setTextColor(Color.WHITE)
            startPauseButton.iconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
            startPauseButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        }
    }

    private fun applyExerciseUi(ex: Exercise) {
        when (ex) {
            Exercise.SQUAT -> {
                tvCurrentExerciseEmoji.text = "🦵"
                labelReps.text = "REPS"
            }
            Exercise.PLANK -> {
                tvCurrentExerciseEmoji.text = "⚡"
                labelReps.text = "SECONDS"
            }
            Exercise.LUNGE -> {
                tvCurrentExerciseEmoji.text = "🦵"
                labelReps.text = "REPS"
            }
        }
        // 디스플레이 초기화
        repCountText.text = if (ex == Exercise.PLANK) "0.0" else "0"
        pointsText.text = "+0"
    }

    private fun mapSelectionToExercise(s: String): Exercise {
        val t = s.lowercase()
        return when {
            "plank" in t -> Exercise.PLANK
            "squat" in t -> Exercise.SQUAT
            "lunge" in t -> Exercise.LUNGE
            else -> Exercise.SQUAT
        }
    }

    private fun updateRepCount(count: Int) {
        repCount = count
        points = count * 10
        coachViewModel.updateRepCount(count)
        if (selectedExercise == Exercise.PLANK) {
            // 플랭크는 repCountText를 위의 onResults에서 소수 1자리로 따로 세팅하므로 여기선 포인트만
            pointsText.text = "+$points"
        } else {
            repCountText.text = count.toString()
            pointsText.text = "+$points"
        }
    }

    // ---------------- Countdown ----------------

    private fun startCountdownThenBegin(seconds: Int = 10) {
        if (isCountingDown) return
        isCountingDown = true
        updateTrainingUiStateForCountdown(true)
        startPauseButton.isEnabled = false
        tvCountdown.visibility = View.VISIBLE

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                val remain = ((ms + 999) / 1000L).toInt()
                tvCountdown.text = remain.toString()
            }
            override fun onFinish() {
                tvCountdown.visibility = View.GONE
                startPauseButton.isEnabled = true
                isCountingDown = false
                beginWorkout()
            }
        }.start()
    }

    private fun updateTrainingUiStateForCountdown(show: Boolean) {
        if (show) {
            hudTopContainer.visibility = View.GONE
            recordingIndicator.visibility = View.GONE
            formScoreContainer.visibility = View.GONE
            overlayView.visibility = View.GONE
            feedbackText.text = "Get ready... ⏳"
        } else {
            updateTrainingUiState()
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

    // ---------------- Utils ----------------

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun toProbMaybeLogit(x: Float?): Float? {
        if (x == null || x.isNaN()) return null
        return if (x in 0f..1f) x else 1f / (1f + exp(-x))
    }

    private fun inFrame(p: com.google.mediapipe.tasks.components.containers.NormalizedLandmark): Boolean {
        val x = p.x(); val y = p.y()
        return x in 0f..1f && y in 0f..1f
    }

    private fun safeVis(p: com.google.mediapipe.tasks.components.containers.NormalizedLandmark): Float {
        val visProb = toProbMaybeLogit((p.visibility() as? Number)?.toFloat())
        val presProb = toProbMaybeLogit((p.presence() as? Number)?.toFloat())
        val best = listOfNotNull(visProb, presProb).maxOrNull()
        if (best != null) return best.coerceIn(0f, 1f)
        return if (inFrame(p)) 1f else 0f
    }

    private fun lowerBodyVisibleCount(
        lm: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        thresh: Float = VIS_THRESH
    ): Int {
        var ok = 0
        for (i in LOWER_NEEDED) {
            val s = safeVis(lm[i])
            if (s >= thresh) ok++
        }
        return ok
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            setupCameraProvider()
        }
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
