package com.fitquest.app.ui.fragments.pose

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentPoseBinding
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.pose.PoseAnalysis
import com.fitquest.app.model.pose.PoseUploadRequest
import com.fitquest.app.ui.fragments.shared.camera.CameraManager
import com.fitquest.app.ui.fragments.shared.camera.CameraPermissionHelper
import com.fitquest.app.ui.fragments.shared.exercise.ExerciseSpinnerManager
import com.fitquest.app.ui.viewmodels.PoseViewModel
import com.fitquest.app.ui.viewmodels.PoseViewModelFactory
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseFragment : Fragment() {

    private var _binding: FragmentPoseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoseViewModel by viewModels {
        PoseViewModelFactory(RetrofitClient.poseAnalysisApiService)
    }

    private lateinit var cameraManager: CameraManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var exerciseSpinnerManager: ExerciseSpinnerManager
    private lateinit var cameraExecutor: ExecutorService

    private var lastPhotoFile: File? = null

    private var countdownTimer: CountDownTimer? = null
    private var loadingTimer: CountDownTimer? = null
    private var orientationListener: OrientationEventListener? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents()
        setupObservers()
        setupExerciseSpinner()
        handlePreloadedImage()
        setupButtons()
    }

    private fun initializeComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = CameraManager(requireContext(), viewLifecycleOwner)
        imageProcessor = ImageProcessor(requireContext())
        exerciseSpinnerManager = ExerciseSpinnerManager(
            requireContext(),
            binding.spinnerExercisePose
        )

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGalleryResult(result.resultCode, result.data?.data)
        }

        setupOrientationListener()
    }

    private fun setupObservers() {
        viewModel.poseAnalysisResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Idle -> Unit
                is NetworkResult.Success -> {
                    viewModel.resetPoseAnalysisResult()
                    handleAnalysisSuccess(result.data)
                }
                is NetworkResult.ServerError -> {
                    viewModel.resetPoseAnalysisResult()
                    handleAnalysisError("Server error: ${result.code}")
                }
                is NetworkResult.NetworkError -> {
                    viewModel.resetPoseAnalysisResult()
                    handleAnalysisError("Network error: ${result.exception.localizedMessage}")
                }
            }
        }
    }

    private fun setupExerciseSpinner() {
        exerciseSpinnerManager.setup { selectedExercise ->
        }
    }

    private fun handlePreloadedImage() {
        val poseExerciseKey = arguments?.getString("poseExerciseKey")
        val poseImagePath = arguments?.getString("poseImagePath")

        if (poseExerciseKey != null) {
            exerciseSpinnerManager.setExercise(poseExerciseKey)
        }

        val preloadedFile = poseImagePath?.let { File(it) }?.takeIf { it.exists() }
        if (preloadedFile != null) {
            lastPhotoFile = preloadedFile
            processAndUpload(preloadedFile)
        } else {
            startCameraIfPermissionsGranted()
        }
    }

    private fun setupButtons() {
        binding.btnCapture.setOnClickListener {
            startCountdownAndCapture()
        }

        binding.btnUpload.setOnClickListener {
            openGalleryForImage()
        }

        binding.btnSwitchCamera.setOnClickListener {
            cameraManager.toggleCamera(
                previewView = binding.cameraPreview,
                analyzer = null,
                onError = { error -> showToast(error) }
            )
        }
    }

    private fun setupOrientationListener() {
        orientationListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                cameraManager.setTargetRotation(rotation)
            }
        }
        orientationListener?.enable()
    }

    private fun startCameraIfPermissionsGranted() {
        if (CameraPermissionHelper.allPermissionsGranted(requireContext())) {
            cameraManager.startCamera(binding.cameraPreview) { error ->
                showToast(error)
            }
        } else {
            CameraPermissionHelper.requestPermissions(this)
        }
    }

    private fun startCountdownAndCapture() {
        var seconds = PoseConstants.COUNTDOWN_INITIAL_SECONDS
        binding.tvCountdown.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(
            PoseConstants.COUNTDOWN_DURATION_MS,
            PoseConstants.COUNTDOWN_INTERVAL_MS
        ) {
            override fun onTick(ms: Long) {
                binding.tvCountdown.text = seconds.toString()
                seconds--
            }

            override fun onFinish() {
                binding.tvCountdown.visibility = View.GONE
                capturePhoto()
                binding.btnCapture.isEnabled = true
            }
        }.start()
    }

    private fun capturePhoto() {
        val file = File(
            requireContext().externalCacheDir,
            "${PoseConstants.FILE_PREFIX_CAMERA}${System.currentTimeMillis()}${PoseConstants.FILE_EXTENSION}"
        )

        cameraManager.capturePhoto(
            outputFile = file,
            executor = ContextCompat.getMainExecutor(requireContext()),
            onSuccess = { savedFile ->
                lastPhotoFile = savedFile
                processAndUpload(savedFile)
            },
            onError = { error ->
                showToast(error)
            }
        )
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun handleGalleryResult(resultCode: Int, uri: Uri?) {
        if (resultCode == Activity.RESULT_OK && uri != null) {
            val file = imageProcessor.createFileFromUri(uri)
            if (file != null) {
                lastPhotoFile = file
                processAndUpload(file)
            } else {
                showToast("Failed to load selected image.")
            }
        }
    }

    private fun processAndUpload(photoFile: File) {
        disableUploadButton()

        val bitmap = imageProcessor.decodeBitmapWithExifCorrected(photoFile)
        if (bitmap == null) {
            showToast("Failed to decode image.")
            enableUploadButton()
            return
        }

        val processedFile = imageProcessor.saveBitmapToFile(bitmap)
        lastPhotoFile = processedFile

        displayProcessedImage(bitmap)
        disableCameraControls()

        val base64 = imageProcessor.bitmapToBase64(bitmap)
        startLoadingProgress()

        val request = PoseUploadRequest(
            category = exerciseSpinnerManager.getCurrentExercise(),
            imageBase64 = base64
        )
        viewModel.uploadPose(request)
    }

    private fun startLoadingProgress() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvProgressPercent.visibility = View.VISIBLE
        binding.progressLoading.isIndeterminate = false
        binding.progressLoading.max = PoseConstants.LOADING_MAX_PROGRESS
        binding.progressLoading.progress = 0
        binding.tvProgressPercent.text = "0%"

        loadingTimer?.cancel()
        loadingTimer = object : CountDownTimer(
            PoseConstants.LOADING_DURATION_MS,
            PoseConstants.LOADING_INTERVAL_MS
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = PoseConstants.LOADING_DURATION_MS - millisUntilFinished
                val fraction = elapsed.toFloat() / PoseConstants.LOADING_DURATION_MS.toFloat()
                val progress = (fraction * PoseConstants.LOADING_TARGET_PROGRESS).toInt()
                binding.progressLoading.progress = progress
                binding.tvProgressPercent.text = "$progress%"
            }

            override fun onFinish() {
                binding.progressLoading.progress = PoseConstants.LOADING_TARGET_PROGRESS
                binding.tvProgressPercent.text = "${PoseConstants.LOADING_TARGET_PROGRESS}%"
            }
        }.start()
    }

    private fun handleAnalysisSuccess(analysis: PoseAnalysis) {
        completeLoadingProgress()

        val bottomSheet = PoseResultBottomSheet.newInstance(
            goodPoints = analysis.goodPoints.ifBlank { "None" },
            improvePoints = analysis.improvementPoints.ifBlank { "None" },
            cue = analysis.improvementMethods?.ifBlank { "None" } ?: "None",
            imagePath = lastPhotoFile?.absolutePath
        )

        bottomSheet.setOnDismissCallback {
            resetCameraUiAndRestart()
        }

        bottomSheet.show(childFragmentManager, "PoseResultBottomSheet")
    }

    private fun handleAnalysisError(message: String) {
        stopLoadingProgress()
        enableUploadButton()
        showToast(message)
    }

    private fun completeLoadingProgress() {
        loadingTimer?.cancel()
        binding.progressLoading.progress = PoseConstants.LOADING_MAX_PROGRESS
        binding.tvProgressPercent.text = "${PoseConstants.LOADING_MAX_PROGRESS}%"
        binding.progressLoading.visibility = View.GONE
        binding.tvProgressPercent.visibility = View.GONE
        enableUploadButton()
    }

    private fun stopLoadingProgress() {
        loadingTimer?.cancel()
        binding.progressLoading.visibility = View.GONE
        binding.tvProgressPercent.visibility = View.GONE
    }

    private fun displayProcessedImage(bitmap: Bitmap) {
        binding.tvGuideText.visibility = View.GONE
        binding.cameraPreview.visibility = View.GONE
        binding.imgAnalysisResult.visibility = View.VISIBLE
        binding.imgAnalysisResult.setImageBitmap(bitmap)
    }

    private fun disableCameraControls() {
        cameraManager.unbindAll()
        binding.btnCapture.isEnabled = false
        binding.btnSwitchCamera.isEnabled = false
    }

    private fun enableUploadButton() {
        binding.btnUpload.isEnabled = true
    }

    private fun disableUploadButton() {
        binding.btnUpload.isEnabled = false
    }

    private fun resetCameraUiAndRestart() {
        binding.imgAnalysisResult.setImageDrawable(null)
        binding.imgAnalysisResult.visibility = View.GONE

        binding.tvGuideText.visibility = View.VISIBLE
        binding.cameraPreview.visibility = View.VISIBLE

        binding.btnCapture.isEnabled = true
        binding.btnUpload.isEnabled = true
        binding.btnSwitchCamera.isEnabled = true

        stopLoadingProgress()
        binding.progressLoading.progress = 0

        startCameraIfPermissionsGranted()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        loadingTimer?.cancel()
        orientationListener?.disable()
        cameraExecutor.shutdown()
        _binding = null
    }
}