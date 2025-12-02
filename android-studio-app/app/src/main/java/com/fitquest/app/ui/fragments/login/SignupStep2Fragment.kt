package com.fitquest.app.ui.fragments.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.fitquest.app.MainActivity
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentSignupStep2Binding
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.login.InitialCountRequest
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import com.fitquest.app.ui.coachutils.counter.BaseCounter
import com.fitquest.app.ui.coachutils.counter.SquatCounter
import com.fitquest.app.ui.fragments.coach.TrackingLockManager
import com.fitquest.app.ui.fragments.shared.camera.CameraManager
import com.fitquest.app.ui.fragments.shared.camera.CameraPermissionHelper
import com.fitquest.app.ui.viewmodels.AuthViewModel
import com.fitquest.app.ui.viewmodels.AuthViewModelFactory
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SignupStep2Fragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    private var _binding: FragmentSignupStep2Binding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(RetrofitClient.authApiService, requireContext())
    }

    private val args: SignupStep2FragmentArgs by navArgs()

    // Managers
    private lateinit var cameraManager: CameraManager
    private lateinit var trackingLockManager: TrackingLockManager
    private lateinit var uiManager: SignupUiManager

    // Camera & Pose
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper

    // State
    private var isAnalyzing = false
    private var isCountingDown = false
    private var counter: BaseCounter? = null
    private var countdownTimer: CountDownTimer? = null

    // Args
    private var email: String = ""
    private var password: String = ""
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = args.email
        password = args.password
        username = args.username
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents()
        setupCamera()
        setupObservers()
        setupButtons()

        startCountdownThenBegin(SignupConstants.COUNTDOWN_DURATION_SECONDS)
    }

    private fun initializeComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = CameraManager(requireContext(), viewLifecycleOwner)
        trackingLockManager = TrackingLockManager()
        uiManager = SignupUiManager(binding)
    }

    private fun setupCamera() {
        cameraExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
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
        authViewModel.updateInitialRepsResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Idle -> Unit
                is NetworkResult.Success -> {
                    showToast("Saved: ${result.data.initial_reps}")
                    navigateToMainActivity()
                }
                is NetworkResult.ServerError -> {
                    showToast("Failed: ${result.code}")
                }
                is NetworkResult.NetworkError -> {
                    showToast("Network error: ${result.exception.localizedMessage}")
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnStop.setOnClickListener {
            pauseAnalysis()
            stopSession()
        }
    }

    // ================== Countdown & Analysis Control ==================

    private fun startCountdownThenBegin(seconds: Int) {
        if (isCountingDown) return
        isCountingDown = true

        var remaining = seconds
        uiManager.updateCountdownText(remaining)

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(
            seconds * 1000L,
            SignupConstants.COUNTDOWN_INTERVAL_MS
        ) {
            override fun onTick(ms: Long) {
                uiManager.updateCountdownText(remaining)
                remaining--
            }

            override fun onFinish() {
                uiManager.hideCountdown()
                isCountingDown = false
                beginAnalysis()
            }
        }.start()
    }

    private fun beginAnalysis() {
        isAnalyzing = true
        counter = SquatCounter().also { it.reset(System.currentTimeMillis()) }
        trackingLockManager.reset()

        uiManager.updateRepCount(0)

        cameraManager.bindCameraUseCases(
            previewView = binding.cameraPreview,
            analyzer = createPoseAnalyzer(),
            onError = { error -> showToast(error) }
        )
    }

    private fun pauseAnalysis() {
        isAnalyzing = false
        counter = null
        trackingLockManager.reset()

        uiManager.clearOverlay()

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
        if (isAnalyzing && ::poseLandmarkerHelper.isInitialized) {
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
            if (!isAnalyzing) return@runOnUiThread

            val result = resultBundle.results.firstOrNull() ?: return@runOnUiThread
            val landmarks = result.landmarks().firstOrNull() ?: return@runOnUiThread

            if (landmarks.size < 33) return@runOnUiThread

            // Update overlay
            binding.overlay.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            uiManager.setOverlayVisibility(true)
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
            uiManager.updateRepCount(currentCount)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            showToast(error)
        }
    }

    private fun stopSession() {
        val initialCount = binding.tvCountNumber.text.toString().toIntOrNull() ?: 0
        authViewModel.updateInitialReps(
            InitialCountRequest(initial_reps = initialCount)
        )
    }

    private fun navigateToMainActivity() {
        try {
            if (::poseLandmarkerHelper.isInitialized) {
                poseLandmarkerHelper.clearPoseLandmarker()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing poseLandmarker", e)
        } finally {
            cameraExecutor.shutdown()
            countdownTimer?.cancel()
            countdownTimer = null

            activity?.let {
                it.startActivity(Intent(it, MainActivity::class.java))
                it.finish()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CameraPermissionHelper.REQUEST_CODE_PERMISSIONS &&
            CameraPermissionHelper.allPermissionsGranted(requireContext())
        ) {
            startCameraPreview()
            startCountdownThenBegin(SignupConstants.COUNTDOWN_DURATION_SECONDS)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        countdownTimer = null
        cameraExecutor.shutdown()
        if (::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.clearPoseLandmarker()
        }
        _binding = null
    }

    companion object {
        private const val TAG = "SignupStep2"

        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"
        private const val ARG_USERNAME = "username"

        fun newInstance(email: String, password: String, username: String): SignupStep2Fragment {
            val fragment = SignupStep2Fragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            args.putString(ARG_PASSWORD, password)
            args.putString(ARG_USERNAME, username)
            fragment.arguments = args
            return fragment
        }
    }
}