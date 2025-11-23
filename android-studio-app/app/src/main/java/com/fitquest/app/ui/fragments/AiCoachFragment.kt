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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentAiCoachBinding
import com.fitquest.app.model.WorkoutResult
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
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Locale
import kotlin.math.exp

class AiCoachFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    private var _binding: FragmentAiCoachBinding? = null
    private val binding get() = _binding!!

    private val coachViewModel: AiCoachViewModel by activityViewModels {
        AiCoachViewModelFactory(RetrofitClient.sessionApiService)
    }

    // ✅ 중앙 REP 팝업용 TextView
    private lateinit var repPopupText: TextView

    // Camera/Pose
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    // State
    private var isTraining = false
    private var isCountingDown = false
    private var countdownTimer: CountDownTimer? = null
    private var repCount = 0
    private var points = 0
    private var selectedExercise: String = "squat" // 기본값

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiCoachBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 중앙 REP 팝업 바인딩
        repPopupText = view.findViewById(R.id.tvRepPopup)

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
        binding.spinnerExercise.adapter = adapter

        binding.spinnerExercise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (!binding.spinnerExercise.isEnabled) {
                    // 잠금 상태일 때 선택이 바뀌어도 selectedExercise를 변경하지 않고 리턴
                    return
                }
                // 선택된 아이템의 순서(pos)를 사용하여 원래의 운동 키(소문자)를 찾습니다.
                val selectedKey = ActivityUtils.activityMetadataMap.keys.toList().getOrNull(pos) ?: "squat"
                selectedExercise = selectedKey

                applyExerciseUi(selectedExercise)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedExercise = "squat"
                applyExerciseUi(selectedExercise)
            }
        }

        // Buttons
        binding.btnStartWorkout.setOnClickListener {
            if (isCountingDown) {
                cancelCountdown()
            } else if (isTraining) {
                pauseWorkout()
            } else {
                startCountdownThenBegin(10)
            }
        }
        binding.btnSwitchCamera.setOnClickListener { toggleCameraLens() }
        binding.btnSwitchCamera.setImageResource(R.drawable.ic_switch_camera)
        ImageViewCompat.setImageTintList(
            binding.btnSwitchCamera,
            ContextCompat.getColorStateList(requireContext(), android.R.color.white)
        )

        // UI init
        binding.tvFeedback.text = COACH_MSG_IDLE
        applyTrainingButtonStyle()
        updateTrainingUiState()

        // ViewModel LiveData 관찰 로직
        coachViewModel.repCount.observe(viewLifecycleOwner) { count ->
            val targetType = ActivityUtils.getTargetType(selectedExercise)
            if (targetType == TargetType.REPS) {
                binding.tvRepCount.text = count.toString()
            }
        }

        coachViewModel.points.observe(viewLifecycleOwner) { points ->
            binding.tvXpPoints.text = "+$points"
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
        _binding = null
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
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }
        imageAnalyzer = if (includeAnalyzer) {
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.cameraPreview.display.rotation)
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

            binding.overlay.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            binding.overlay.invalidate()

            val lm = result.landmarks().firstOrNull() ?: return@runOnUiThread
            if (lm.size < 33) return@runOnUiThread

            val now = System.currentTimeMillis()

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
                        binding.tvCountdown.visibility = View.GONE
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
                        binding.tvCountdown.text = "STEP BACK"
                        binding.tvCountdown.visibility = View.VISIBLE
                        return@runOnUiThread
                    }
                } else badVisFrames = 0
            }
            if (now < disarmUntilMs) return@runOnUiThread

            val pts = FloatArray(lm.size * 3)
            for (i in lm.indices) {
                pts[3 * i] = lm[i].x()
                pts[3 * i + 1] = lm[i].y()
                pts[3 * i + 2] = lm[i].z()
            }
            counter?.update(pts, now)

            // ---- UI 반영 ----
            val lowerName = selectedExercise.lowercase(Locale.getDefault())
            if (lowerName == "plank" && counter is PlankTimer) {
                val pt = counter as PlankTimer
                val seconds = pt.holdSeconds()
                binding.tvRepCount.text = String.format(Locale.getDefault(), "%.1f", seconds)
                updateRepCount(counter?.count ?: 0)
            } else {
                updateRepCount(counter?.count ?: 0)
            }
            binding.tvFeedback.text = "Phase: ${counter?.phase ?: "-"}"
        }
    }

    override fun onError(error: String, errorCode: Int) {
        // 필요시 처리
    }

    // ---------------- Training control ----------------

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

    private fun safeVis(p: com.google.mediapipe.tasks.components.containers.NormalizedLandmark): Float {
        val visProb = toProbMaybeLogit((p.visibility() as? Number)?.toFloat())
        val presProb = toProbMaybeLogit((p.presence() as? Number)?.toFloat())
        val best = listOfNotNull(visProb, presProb).maxOrNull()
        if (best != null) return best.coerceIn(0f, 1f)
        return if (inFrame(p)) 1f else 0f
    }


    private fun handleScheduleLocking() {
        val isScheduled = scheduleId != null

        if (isScheduled) {
            // 스피너 비활성화 (운동 변경 불가)
            binding.spinnerExercise.isEnabled = false
            binding.spinnerExercise.alpha = 0.5f

            // 스피너의 현재 선택된 아이템을 스케줄 운동으로 설정 (UI에 표시)
            val activityKey = selectedExercise // 이미 selectedExercise는 Bundle에서 받은 값으로 초기화됨
            val keys = ActivityUtils.activityMetadataMap.keys.toList()
            val pos = keys.indexOf(activityKey)
            if (pos != -1) {
                binding.spinnerExercise.setSelection(pos)
            }
        } else {
            binding.spinnerExercise.isEnabled = !isTraining // 트레이닝 중이 아니라면 활성화
            binding.spinnerExercise.alpha = 1.0f
        }
    }

    private fun updateTargetUi() {
        val isScheduled = scheduleId != null
        val targetType = ActivityUtils.getTargetType(selectedExercise)

        if (isScheduled) {
            val exerciseLabel = ActivityUtils.getLabel(selectedExercise)

            val targetText = when (targetType) {
                TargetType.REPS -> if (scheduleRepsTarget != null) "$exerciseLabel Target: ${scheduleRepsTarget} Reps" else "$exerciseLabel Scheduled"
                TargetType.DURATION -> if (scheduleDurationTarget != null) "$exerciseLabel Target: ${scheduleDurationTarget} Secs" else "$exerciseLabel Scheduled"
                else -> "$exerciseLabel Scheduled"
            }
            binding.tvSystemSubtitle.text = targetText
        } else {
            binding.tvSystemSubtitle.text = "Start your session"
        }
    }

    fun beginWorkout() {
        isTraining = true

        val now = System.currentTimeMillis()
        val activity = selectedExercise.lowercase(Locale.getDefault())


        counter = when (selectedExercise.lowercase(Locale.getDefault())) {
            "squat" -> SquatCounter().also { it.reset(now) }
            "plank" -> PlankTimer().also { it.reset(now) }
            "lunge" -> LungeCounter().also { it.reset(now) }
            else -> SquatCounter().also { it.reset(now) }
        }

        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
        binding.tvCountdown.visibility = View.GONE

        coachViewModel.beginTraining(activity, scheduleId)

        handleScheduleLocking()

        binding.tvFeedback.text = COACH_MSG_ANALYZING
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
        binding.tvFeedback.text = COACH_MSG_IDLE
        applyTrainingButtonStyle()
        updateTrainingUiState()
        binding.overlay.clear()
        counter = null
        binding.tvCountdown.visibility = View.GONE

        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
        bindCameraUseCases(includeAnalyzer = false)
    }

    private fun updateTrainingUiState() {
        val v = if (isTraining) View.VISIBLE else View.GONE
        binding.hudTopContainer.visibility = v
        binding.recordingIndicator.visibility = v
        binding.formScoreContainer.visibility = v
        binding.overlay.visibility = v
    }

    private fun applyTrainingButtonStyle() {
        if (isTraining) {
            binding.btnStartWorkout.text = "Pause Training"
            binding.btnStartWorkout.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_square)
            binding.btnStartWorkout.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.error_red)
            binding.btnStartWorkout.setTextColor(Color.WHITE)
            binding.btnStartWorkout.iconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
            binding.btnStartWorkout.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        } else {
            binding.btnStartWorkout.text = "Begin Training"
            binding.btnStartWorkout.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_begin_triangle)
            binding.btnStartWorkout.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.success_green)
            binding.btnStartWorkout.setTextColor(Color.WHITE)
            binding.btnStartWorkout.iconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
            binding.btnStartWorkout.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        }
    }

    private fun applyExerciseUi(exerciseName: String) {
        val lowerCaseName = exerciseName.lowercase(Locale.getDefault())
        val targetType = ActivityUtils.getTargetType(lowerCaseName)

        binding.tvCurrentExerciseEmoji.text = ActivityUtils.getEmoji(lowerCaseName)

        when (lowerCaseName) {
            "plank" -> binding.labelReps.text = "SECONDS"
            "squat", "lunge" -> binding.labelReps.text = "REPS"
            else -> binding.labelReps.text = "REPS"
        }

        binding.tvRepCount.text = if (lowerCaseName == "plank") "0.0" else "0"
        binding.tvXpPoints.text = "+0"
    }

    // ✅ rep 증가 시 중앙 팝업까지 처리
    private fun updateRepCount(count: Int) {
        val prev = repCount
        repCount = count
        points = count * 10
        coachViewModel.updateRepCount(count)

        val lowerName = selectedExercise.lowercase(Locale.getDefault())

        if (lowerName == "plank") {
            // 플랭크: 시간은 onResults에서 세팅, 여기서는 포인트만
            binding.tvXpPoints.text = "+$points"
        } else {
            // squat / lunge
            binding.tvRepCount.text = count.toString()
            binding.tvXpPoints.text = "+$points"

            // 이전 값보다 커졌을 때만 팝업 (rep 올라간 순간)
            if (isTraining && count > prev) {
                showRepPopup(count)
            }
        }
    }

    // ✅ 중앙 REP 팝업 애니메이션
    private fun showRepPopup(count: Int) {
        repPopupText.text = count.toString()
        repPopupText.visibility = View.VISIBLE
        repPopupText.alpha = 1f
        repPopupText.scaleX = 1f
        repPopupText.scaleY = 1f

        repPopupText.animate().cancel()
        repPopupText.animate()
            .scaleX(1.4f)
            .scaleY(1.4f)
            .alpha(0f)
            .setDuration(600L)
            .withEndAction {
                repPopupText.visibility = View.GONE
                repPopupText.alpha = 1f
                repPopupText.scaleX = 1f
                repPopupText.scaleY = 1f
            }
            .start()
    }

    // ---------------- Countdown ----------------

    private fun startCountdownThenBegin(seconds: Int = 10) {
        if (isCountingDown) return
        isCountingDown = true

        // ✅ (추가) ViewModel에 '세션 준비 중' 상태를 알립니다. (Bottom Nav Lock 시작)
        coachViewModel.setSessionPreparing(true)

        updateTrainingUiStateForCountdown(true)
        // binding.btnStartWorkout.isEnabled = false
        binding.tvCountdown.visibility = View.VISIBLE

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                val remain = ((ms + 999) / 1000L).toInt()
                binding.tvCountdown.text = remain.toString()
            }
            override fun onFinish() {
                // ✅ 로컬 카운트다운이 끝났으니 ViewModel의 '준비 중' 상태를 해제
                // 이제 VM의 beginTraining()이 호출될 준비가 되었다.
                coachViewModel.setSessionPreparing(false)

                binding.tvCountdown.visibility = View.GONE
                // binding.btnStartWorkout.isEnabled = true
                isCountingDown = false
                beginWorkout()
            }
        }.start()
        // UI 업데이트 (버튼 텍스트를 "Cancel" 등으로 변경하는 로직을 추가할 수도 있다.)
    }

    private fun updateTrainingUiStateForCountdown(show: Boolean) {
        if (show) {
            binding.hudTopContainer.visibility = View.GONE
            binding.recordingIndicator.visibility = View.GONE
            binding.formScoreContainer.visibility = View.GONE
            binding.overlay.visibility = View.GONE
            binding.tvFeedback.text = "Get ready... ⏳"
        } else {
            updateTrainingUiState()
        }
    }

    private fun cancelCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        isCountingDown = false
        binding.tvCountdown.visibility = View.GONE
        binding.tvFeedback.text = COACH_MSG_IDLE
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
