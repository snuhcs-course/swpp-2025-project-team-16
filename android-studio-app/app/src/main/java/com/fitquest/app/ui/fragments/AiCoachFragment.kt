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
import com.fitquest.app.model.WorkoutResult
import com.fitquest.app.repository.SessionRepository
import com.fitquest.app.ui.coachutils.OverlayView
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import com.fitquest.app.ui.coachutils.counter.BaseCounter
import com.fitquest.app.ui.coachutils.counter.PlankTimer
import com.fitquest.app.ui.coachutils.counter.SquatCounter
import com.fitquest.app.ui.coachutils.counter.LungeCounter
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.fitquest.app.ui.viewmodels.AiCoachViewModelFactory
import com.fitquest.app.util.ActivityUtils
import com.fitquest.app.util.TargetType
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Locale
import kotlin.math.exp

class AiCoachFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

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
    private val coachViewModel: AiCoachViewModel by activityViewModels {
        AiCoachViewModelFactory(
            SessionRepository()
        )
    }

    // State
    private var isTraining = false
    private var isCountingDown = false
    private var countdownTimer: CountDownTimer? = null

    private var selectedExercise: String = "squat" // String으로 변경, 기본값은 "squat"

    private var counter: BaseCounter? = null

    private var scheduleId: Int? = null
    private var scheduleRepsTarget: Int? = null
    private var scheduleDurationTarget: Int? = null

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

        // ✅ 수정: Bundle에서 스케줄 정보 (ID, 목표, 운동 키)를 가져와 초기화합니다.
        arguments?.let {
            val id = it.getInt(ARG_SCHEDULE_ID, -1).takeIf { i -> i != -1 }
            scheduleId = id

            // 전달된 타겟 값이 -1이 아닐 경우에만 저장
            val repsTarget = it.getInt(ARG_REPS_TARGET, -1).takeIf { t -> t != -1 }
            scheduleRepsTarget = repsTarget

            val durationTarget = it.getInt(ARG_DURATION_TARGET, -1).takeIf { t -> t != -1 }
            scheduleDurationTarget = durationTarget

            // 운동 키를 받아 현재 운동으로 설정합니다.
            val scheduledActivity = it.getString(ARG_ACTIVITY_KEY)?.lowercase()
            if (scheduledActivity != null) {
                selectedExercise = scheduledActivity
            }
        }

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

        // --- Spinner 초기화 및 바인딩 ---
        val exerciseListWithEmoji = ActivityUtils.activityMetadataMap.values.map { metadata ->
            "${metadata.emoji} ${metadata.label}"
        }.toTypedArray()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            exerciseListWithEmoji
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExercise.adapter = adapter

        spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (!spinnerExercise.isEnabled) {
                    // 잠금 상태일 때 선택이 바뀌어도 selectedExercise를 변경하지 않고 리턴
                    return
                }
                // 선택된 아이템의 순서(pos)를 사용하여 원래의 운동 키(소문자)를 찾습니다.
                val selectedKey = ActivityUtils.activityMetadataMap.keys.toList().getOrNull(pos) ?: "squat"
                selectedExercise = selectedKey
                applyExerciseUi(selectedExercise)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedExercise = "squat" // 기본값 설정
                applyExerciseUi(selectedExercise)
            }
        }

        // Buttons
        startPauseButton.setOnClickListener {
            if (isCountingDown) {
                cancelCountdown()
            } else if (isTraining) {
                pauseWorkout()
            } else {
                startCountdownThenBegin(10)
            }
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

        // ViewModel LiveData 관찰 로직
        coachViewModel.repCount.observe(viewLifecycleOwner) { count ->
            val targetType = ActivityUtils.getTargetType(selectedExercise)
            if (targetType == TargetType.REPS) {
                repCountText.text = count.toString()
            }
        }

        coachViewModel.points.observe(viewLifecycleOwner) { points ->
            pointsText.text = "+$points"
        }

        coachViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                // (선택 사항) 토스트를 띄운 후 메시지를 초기화하는 추가적인 이벤트 처리가 필요할 수 있다.
            }
        }

        // 1. 스케줄 연동 시 초기 운동 선택/UI 잠금 처리
        handleScheduleLocking()
        // 2. 목표 UI 업데이트 (tvSystemSubtitle 재활용)
        updateTargetUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ 추가: Fragment의 뷰가 해제될 때 카메라 관련 스레드 풀을 종료하여 자원 누수를 방지합니다.
        cameraExecutor.shutdown()
        // _binding = null (이 Fragment는 바인딩을 사용하지 않으므로 제거)
        // super.onDestroyView() 호출은 이미 되어있으므로, 여기에 정리 로직을 추가합니다.
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
            val targetType = ActivityUtils.getTargetType(selectedExercise)

            if (targetType == TargetType.DURATION && counter is PlankTimer) {
                val pt = counter as PlankTimer
                // 0.1초 단위 표시
                val seconds = pt.holdSeconds()
                repCountText.text = String.format(Locale.getDefault(), "%.1f", seconds)
                // 내부 count(Int)는 floor(seconds)이므로 기존 VM 업데이트는 그대로 유지
                coachViewModel.updateRepCount(counter?.count ?: 0) // VM 호출로 변경
            } else {
                // 스쿼트, 런지 등 횟수 기반
                coachViewModel.updateRepCount(counter?.count ?: 0) // VM 호출로 변경
            }
            feedbackText.text = "Phase: ${counter?.phase ?: "-"}"
        }
    }

    override fun onError(error: String, errorCode: Int) {
        // 필요시 로그/토스트
    }

    // ---------------- Training control ----------------

    private fun lowerBodyVisibleCount(lm: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>, thresh: Float): Int {
        var count = 0
        for (i in LOWER_NEEDED) {
            val p = lm[i]
            val visProb = toProbMaybeLogit((p.visibility() as? Number)?.toFloat()) ?: 0f
            if (visProb >= thresh && inFrame(p)) {
                count++
            }
        }
        return count
    }

    private fun handleScheduleLocking() {
        val isScheduled = scheduleId != null

        if (isScheduled) {
            // 스피너 비활성화 (운동 변경 불가)
            spinnerExercise.isEnabled = false
            spinnerExercise.alpha = 0.5f

            // 스피너의 현재 선택된 아이템을 스케줄 운동으로 설정 (UI에 표시)
            val activityKey = selectedExercise // 이미 selectedExercise는 Bundle에서 받은 값으로 초기화됨
            val keys = ActivityUtils.activityMetadataMap.keys.toList()
            val pos = keys.indexOf(activityKey)
            if (pos != -1) {
                spinnerExercise.setSelection(pos)
            }
        } else {
            spinnerExercise.isEnabled = !isTraining // 트레이닝 중이 아니라면 활성화
            spinnerExercise.alpha = 1.0f
        }
    }

    private fun updateTargetUi() {
        val isScheduled = scheduleId != null
        val targetType = ActivityUtils.getTargetType(selectedExercise)

        // findViewById를 한 번 더 호출하는 대신, onViewCreated에서 바인딩된 뷰를 사용합니다.
        // 현재 tvSystemSubtitle은 바인딩되어 있지 않으므로 임시로 findViewById를 사용하거나,
        // 이 뷰가 레이아웃에 포함되어 있다고 가정합니다. (여기서는 기존 코드를 유지합니다.)
        val tvSystemSubtitle: TextView = requireView().findViewById(R.id.tvSystemSubtitle)

        if (isScheduled) {
            val exerciseLabel = ActivityUtils.getLabel(selectedExercise)

            val targetText = when (targetType) {
                TargetType.REPS -> if (scheduleRepsTarget != null) "$exerciseLabel Target: ${scheduleRepsTarget} Reps" else "$exerciseLabel Scheduled"
                TargetType.DURATION -> if (scheduleDurationTarget != null) "$exerciseLabel Target: ${scheduleDurationTarget} Secs" else "$exerciseLabel Scheduled"
                else -> "$exerciseLabel Scheduled"
            }
            tvSystemSubtitle.text = targetText
        } else {
            tvSystemSubtitle.text = "Start your session"
        }
    }

    fun beginWorkout() {
        isTraining = true

        val now = System.currentTimeMillis()
        val activity = selectedExercise.lowercase(Locale.getDefault())

        counter = when (activity) {
            "squat" -> SquatCounter().also { it.reset(now) }
            "plank" -> PlankTimer().also { it.reset(now) }
            "lunge" -> LungeCounter().also { it.reset(now) }
            // TODO: 리팩토링 시 BaseCounter를 상속받는 클래스 맵을 만들 수 있으나, 현재는 이렇게 유지
            else -> SquatCounter().also { it.reset(now) }
        }

        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
        tvCountdown.visibility = View.GONE

        coachViewModel.beginTraining(activity, scheduleId)

        handleScheduleLocking()

        feedbackText.text = COACH_MSG_ANALYZING
        applyTrainingButtonStyle()
        updateTrainingUiState()
        bindCameraUseCases(includeAnalyzer = true)
    }

    private fun pauseWorkout() {
        if (isCountingDown) cancelCountdown()
        if (!isTraining) return

        isTraining = false

        val targetType = ActivityUtils.getTargetType(selectedExercise)
        val currentCounter = counter

        // 1. 결과 추출 및 WorkoutResult 객체 생성
        val workoutResult: WorkoutResult = when (targetType) {
            TargetType.DURATION -> {
                val duration = (currentCounter as? PlankTimer)?.holdSeconds()?.toInt()
                WorkoutResult(durationSeconds = duration)
            }
            TargetType.REPS, null -> {
                val reps = currentCounter?.count
                WorkoutResult(repsCount = reps)
            }
        }

        // 2. ViewModel에 WorkoutResult 객체 전달
        coachViewModel.pauseTraining(workoutResult)

        handleScheduleLocking()

        // 3. Fragment Local 상태 정리
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

    private fun applyExerciseUi(exerciseName: String) {
        val lowerCaseName = exerciseName.lowercase(Locale.getDefault())
        val targetType = ActivityUtils.getTargetType(lowerCaseName)

        // 1. ActivityUtils에서 이모지 가져오기
        tvCurrentExerciseEmoji.text = ActivityUtils.getEmoji(lowerCaseName)

        // 2. 운동 종류에 따라 라벨 변경
        labelReps.text = when (targetType) {
            TargetType.DURATION -> "SECONDS"
            TargetType.REPS, null -> "REPS"
        }

        // 디스플레이 초기화
        repCountText.text = if (targetType == TargetType.DURATION) "0.0" else "0"
        pointsText.text = "+0"
    }

    // ---------------- Countdown ----------------

    private fun startCountdownThenBegin(seconds: Int = 10) {
        if (isCountingDown) return
        isCountingDown = true

        // ✅ (추가) ViewModel에 '세션 준비 중' 상태를 알립니다. (Bottom Nav Lock 시작)
        coachViewModel.setSessionPreparing(true)

        updateTrainingUiStateForCountdown(true)
        // startPauseButton.isEnabled = false
        tvCountdown.visibility = View.VISIBLE

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                val remain = ((ms + 999) / 1000L).toInt()
                tvCountdown.text = remain.toString()
            }
            override fun onFinish() {
                // ✅ 로컬 카운트다운이 끝났으니 ViewModel의 '준비 중' 상태를 해제
                // 이제 VM의 beginTraining()이 호출될 준비가 되었다.
                coachViewModel.setSessionPreparing(false)

                tvCountdown.visibility = View.GONE
                // startPauseButton.isEnabled = true
                isCountingDown = false
                beginWorkout()
            }
        }.start()
        // UI 업데이트 (버튼 텍스트를 "Cancel" 등으로 변경하는 로직을 추가할 수도 있다.)
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

        // ✅ (추가) ViewModel의 상태를 해제 (Bottom Nav Lock 해제)
        coachViewModel.cancelCountdown()
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

    companion object {
        // ✅ 수정: Navigation Component에서 사용하는 Argument Key 상수 정의
        private const val ARG_SCHEDULE_ID = "scheduleId"
        private const val ARG_ACTIVITY_KEY = "activityKey"
        private const val ARG_REPS_TARGET = "repsTarget"
        private const val ARG_DURATION_TARGET = "durationTarget"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
