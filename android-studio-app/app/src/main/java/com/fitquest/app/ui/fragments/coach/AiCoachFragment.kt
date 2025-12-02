package com.fitquest.app.ui.fragments.coach

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentAiCoachBinding
import com.fitquest.app.model.WorkoutResult
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import com.fitquest.app.ui.coachutils.counter.BaseCounter
import com.fitquest.app.ui.coachutils.counter.LungeCounter
import com.fitquest.app.ui.coachutils.counter.PlankTimer
import com.fitquest.app.ui.coachutils.counter.SquatCounter
import com.fitquest.app.ui.fragments.shared.camera.CameraManager
import com.fitquest.app.ui.fragments.shared.camera.CameraPermissionHelper
import com.fitquest.app.ui.fragments.shared.exercise.ExerciseSpinnerManager
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.fitquest.app.ui.viewmodels.AiCoachViewModelFactory
import com.fitquest.app.util.ActivityUtils
import com.fitquest.app.util.TargetType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.BitmapFactory

class AiCoachFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    private var _binding: FragmentAiCoachBinding? = null
    private val binding get() = _binding!!

    private val coachViewModel: AiCoachViewModel by activityViewModels {
        AiCoachViewModelFactory(RetrofitClient.sessionApiService)
    }

    // Managers
    private lateinit var cameraManager: CameraManager
    private lateinit var exerciseSpinnerManager: ExerciseSpinnerManager
    private lateinit var trackingLockManager: TrackingLockManager
    private lateinit var photoCaptureManager: PhotoCaptureManager
    private lateinit var confettiManager: ConfettiManager
    private lateinit var uiManager: CoachUiManager

    // Camera & Pose
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    // State
    private var isTraining = false
    private var isCountingDown = false
    private var sessionStartTime: Long? = null
    private var repCount = 0
    private var points = 0

    private var counter: BaseCounter? = null
    private var countdownTimer: CountDownTimer? = null

    private lateinit var repPopupText: TextView

    // Schedule info
    private var scheduleId: Int? = null
    private var scheduleRepsTarget: Int? = null
    private var scheduleDurationTarget: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiCoachBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents()
        parseArguments()
        setupCamera()
        setupObservers()
        setupExerciseSpinner()
        setupButtons()
        initializeUi()
    }

    private fun initializeComponents() {
        repPopupText = binding.root.findViewById(R.id.tvRepPopup)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = CameraManager(requireContext(), viewLifecycleOwner)
        trackingLockManager = TrackingLockManager()
        photoCaptureManager = PhotoCaptureManager(requireContext())
        confettiManager = ConfettiManager(binding.confettiView)
        uiManager = CoachUiManager(requireContext(), binding)

        exerciseSpinnerManager = ExerciseSpinnerManager(
            requireContext(),
            binding.spinnerExercise
        )
    }

    private fun parseArguments() {
        arguments?.let {
            scheduleId = it.getInt(CoachConstants.ARG_SCHEDULE_ID, -1).takeIf { id -> id != -1 }
            scheduleRepsTarget = it.getInt(CoachConstants.ARG_REPS_TARGET, -1).takeIf { t -> t != -1 }
            scheduleDurationTarget = it.getInt(CoachConstants.ARG_DURATION_TARGET, -1).takeIf { t -> t != -1 }

            val scheduledActivity = it.getString(CoachConstants.ARG_ACTIVITY_KEY)?.lowercase()
            if (scheduledActivity != null) {
                exerciseSpinnerManager.setExercise(scheduledActivity)
            }
        }
    }

    private fun setupCamera() {
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

        if (CameraPermissionHelper.allPermissionsGranted(requireContext())) {
            startCameraPreview()
        } else {
            CameraPermissionHelper.requestPermissions(this)
        }
    }

    private fun startCameraPreview() {
        cameraManager.startCamera(binding.cameraPreview) { error ->
            showToast(error)
        }
    }

    private fun setupObservers() {
        coachViewModel.repCount.observe(viewLifecycleOwner) { count ->
            val targetType = ActivityUtils.getTargetType(exerciseSpinnerManager.getCurrentExercise())
            if (targetType == TargetType.REPS) {
                binding.tvRepCount.text = count.toString()
            }
        }

        coachViewModel.points.observe(viewLifecycleOwner) { pts ->
            binding.tvXpPoints.text = "+$pts"
        }

        coachViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                showToast(message)
            }
        }
    }

    private fun setupExerciseSpinner() {
        exerciseSpinnerManager.setup { selectedExercise ->
            if (!isTraining) {
                uiManager.applyExerciseUi(selectedExercise)
            }
        }

        handleScheduleLocking()
        updateTargetUi()
    }

    private fun setupButtons() {
        binding.btnStartWorkout.setOnClickListener {
            when {
                isCountingDown -> cancelCountdown()
                isTraining -> pauseWorkout()
                else -> startCountdownThenBegin(CoachConstants.COUNTDOWN_DURATION_SECONDS)
            }
        }

        binding.btnSwitchCamera.setOnClickListener {
            cameraManager.toggleCamera(
                previewView = binding.cameraPreview,
                analyzer = if (isTraining) createPoseAnalyzer() else null,
                onError = { error -> showToast(error) }
            )
        }

        binding.btnSwitchCamera.setImageResource(R.drawable.ic_switch_camera)
        androidx.core.widget.ImageViewCompat.setImageTintList(
            binding.btnSwitchCamera,
            ContextCompat.getColorStateList(requireContext(), android.R.color.white)
        )
    }

    private fun initializeUi() {
        uiManager.setFeedbackMessage(CoachConstants.COACH_MSG_IDLE)
        uiManager.applyTrainingButtonStyle(false)
        uiManager.updateTrainingUiVisibility(false)
    }

    // ================== Training Control ==================

    private fun startCountdownThenBegin(seconds: Int) {
        if (isCountingDown) return
        isCountingDown = true

        coachViewModel.setSessionPreparing(true)
        uiManager.updateCountdownUi(true)

        var remaining = seconds
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(
            seconds * 1000L,
            CoachConstants.COUNTDOWN_INTERVAL_MS
        ) {
            override fun onTick(ms: Long) {
                uiManager.updateCountdownText(remaining)
                remaining--
            }

            override fun onFinish() {
                coachViewModel.setSessionPreparing(false)
                uiManager.hideCountdown()
                isCountingDown = false
                beginWorkout()
            }
        }.start()
    }

    private fun cancelCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        isCountingDown = false
        uiManager.hideCountdown()
        uiManager.setFeedbackMessage(CoachConstants.COACH_MSG_IDLE)
        uiManager.applyTrainingButtonStyle(false)
        uiManager.updateTrainingUiVisibility(false)
        coachViewModel.cancelCountdown()
    }

    private fun beginWorkout() {
        isTraining = true
        sessionStartTime = System.currentTimeMillis()

        photoCaptureManager.clear()
        trackingLockManager.reset()

        val activity = exerciseSpinnerManager.getCurrentExercise().lowercase(Locale.getDefault())
        counter = createCounter(activity)

        coachViewModel.beginTraining(activity, scheduleId)
        handleScheduleLocking()

        uiManager.setFeedbackMessage(CoachConstants.COACH_MSG_ANALYZING)
        uiManager.applyTrainingButtonStyle(true)
        uiManager.updateTrainingUiVisibility(true)

        cameraManager.bindCameraUseCases(
            previewView = binding.cameraPreview,
            analyzer = createPoseAnalyzer(),
            onError = { error -> showToast(error) }
        )
    }

    private fun pauseWorkout() {
        if (isCountingDown) cancelCountdown()
        if (!isTraining) return

        isTraining = false

        val sessionDurationSec = sessionStartTime?.let {
            ((System.currentTimeMillis() - it) / 1000L).toInt()
        } ?: 0

        val workoutResult = createWorkoutResult(sessionDurationSec)
        coachViewModel.pauseTraining(workoutResult)

        handleScheduleLocking()
        cleanupAfterWorkout()
        showPoseEvalDialogIfAvailable()
    }

    private fun createCounter(activity: String): BaseCounter {
        val now = System.currentTimeMillis()
        return when (activity) {
            "squat" -> SquatCounter().also { it.reset(now) }
            "plank" -> PlankTimer().also { it.reset(now) }
            "lunge" -> LungeCounter().also { it.reset(now) }
            else -> SquatCounter().also { it.reset(now) }
        }
    }

    private fun createWorkoutResult(sessionDurationSec: Int): WorkoutResult {
        val targetType = ActivityUtils.getTargetType(exerciseSpinnerManager.getCurrentExercise())
        return when (targetType) {
            TargetType.DURATION -> {
                val duration = (counter as? PlankTimer)?.holdSeconds()?.toInt()
                WorkoutResult(
                    durationSeconds = duration,
                    sessionDurationSeconds = sessionDurationSec
                )
            }
            TargetType.REPS, null -> {
                val reps = counter?.count
                WorkoutResult(
                    repsCount = reps,
                    sessionDurationSeconds = sessionDurationSec
                )
            }
        }
    }

    private fun cleanupAfterWorkout() {
        uiManager.setFeedbackMessage(CoachConstants.COACH_MSG_IDLE)
        uiManager.applyTrainingButtonStyle(false)
        uiManager.updateTrainingUiVisibility(false)
        uiManager.clearOverlay()
        uiManager.hideCountdown()

        counter = null
        trackingLockManager.reset()

        cameraManager.bindCameraUseCases(
            previewView = binding.cameraPreview,
            analyzer = null,
            onError = { error -> showToast(error) }
        )
    }

    // ================== Pose Detection ==================

    private fun createPoseAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            detectPose(imageProxy)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (isTraining && ::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = (cameraManager.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT)
            )
        } else {
            imageProxy.close()
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (!isTraining) return@runOnUiThread

            val result = resultBundle.results.firstOrNull() ?: return@runOnUiThread
            val landmarks = result.landmarks().firstOrNull() ?: return@runOnUiThread

            if (landmarks.size < 33) return@runOnUiThread

            binding.overlay.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            binding.overlay.invalidate()

            val now = System.currentTimeMillis()

            // Tracking lock check
            val lockChange = trackingLockManager.updateLockState(landmarks, now)
            when (lockChange) {
                TrackingLockManager.LockStateChange.LOCKED -> {
                    uiManager.showTrackingLockMessage(true)
                    return@runOnUiThread
                }
                TrackingLockManager.LockStateChange.UNLOCKED -> {
                    uiManager.showTrackingLockMessage(false)
                }
                TrackingLockManager.LockStateChange.STILL_LOCKED -> {
                    return@runOnUiThread
                }
                TrackingLockManager.LockStateChange.NO_CHANGE -> {
                    // Continue
                }
            }

            if (trackingLockManager.shouldDisarm(now)) return@runOnUiThread

            // Convert landmarks to float array
            val pts = FloatArray(landmarks.size * 3)
            for (i in landmarks.indices) {
                pts[3 * i] = landmarks[i].x()
                pts[3 * i + 1] = landmarks[i].y()
                pts[3 * i + 2] = landmarks[i].z()
            }

            counter?.update(pts, now)

            val currentCount = counter?.count ?: 0
            val currentPhase = counter?.phase

            // Photo capture
            photoCaptureManager.captureIfNeeded(
                imageCapture = null, // Will be handled by CameraManager
                executor = ContextCompat.getMainExecutor(requireContext()),
                exerciseName = exerciseSpinnerManager.getCurrentExercise(),
                currentRep = currentCount,
                phase = currentPhase
            )

            updateRepCount(currentCount)
            binding.tvFeedback.text = "Phase: ${currentPhase ?: "-"}"
        }
    }

    override fun onError(error: String, errorCode: Int) {
        // Handle error if needed
    }

    // ================== UI Updates ==================

    private fun updateRepCount(count: Int) {
        val prev = repCount
        repCount = count

        val strategy = counter ?: return

        points = strategy.getXpPoints()
        coachViewModel.updateRepCount(count)

        binding.tvRepCount.text = strategy.getDisplayText()
        binding.tvXpPoints.text = "+$points"

        if (strategy.shouldShowRepPopup(prev, isTraining)) {
            uiManager.showRepPopup(repPopupText, count)
            checkMilestones(count)
        }
    }

    private fun checkMilestones(count: Int) {
        if (isTraining && scheduleRepsTarget != null && count == scheduleRepsTarget) {
            confettiManager.showGoalAchieved()
        } else if (count % 10 == 0) {
            confettiManager.showMilestone10()
        } else if (count % 5 == 0) {
            confettiManager.showMilestone()
        }
    }

    private fun handleScheduleLocking() {
        val isScheduled = scheduleId != null
        exerciseSpinnerManager.setEnabled(!isScheduled && !isTraining)
    }

    private fun updateTargetUi() {
        uiManager.updateTargetUi(
            isScheduled = scheduleId != null,
            exerciseName = exerciseSpinnerManager.getCurrentExercise(),
            repsTarget = scheduleRepsTarget,
            durationTarget = scheduleDurationTarget
        )
    }

    private fun showPoseEvalDialogIfAvailable() {
        val photos = photoCaptureManager.getPhotos()
        if (photos.isEmpty()) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_pose_eval_preview, null)
        val viewPager = dialogView.findViewById<ViewPager2>(R.id.viewPagerPosePhotos)
        val tvIndicator = dialogView.findViewById<TextView>(R.id.tvPageIndicator)
        val btnLater = dialogView.findViewById<Button>(R.id.btnLater)
        val btnEvaluate = dialogView.findViewById<Button>(R.id.btnEvaluate)

        val adapter = PosePhotoPagerAdapter(photos)
        viewPager.adapter = adapter

        var selectedIndex = 0

        fun updateIndicator(position: Int) {
            tvIndicator.text = "${position + 1} / ${photos.size}"
        }

        updateIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectedIndex = position
                updateIndicator(position)
            }
        })

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnLater.setOnClickListener {
            dialog.dismiss()
        }

        btnEvaluate.setOnClickListener {
            dialog.dismiss()
            val chosenFile = photos.getOrNull(selectedIndex)
            if (chosenFile != null) {
                navigateToPoseWithPhoto(chosenFile)
            }
        }

        dialog.show()
    }

    private fun navigateToPoseWithPhoto(photo: File) {
        val bundle = Bundle().apply {
            putString("poseImagePath", photo.absolutePath)
            putString("poseExerciseKey", exerciseSpinnerManager.getCurrentExercise())
        }
        findNavController().navigate(R.id.poseFragment, bundle)
    }

    private inner class PosePhotoPagerAdapter(
        private val photos: List<File>
    ) : RecyclerView.Adapter<PosePhotoPagerAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return PhotoViewHolder(imageView)
        }

        override fun getItemCount(): Int = photos.size

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val file = photos[position]
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            holder.imageView.setImageBitmap(bitmap)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        cameraExecutor.shutdown()
        _binding = null
    }
}