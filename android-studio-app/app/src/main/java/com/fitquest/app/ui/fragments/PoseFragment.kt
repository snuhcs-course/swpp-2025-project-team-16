package com.fitquest.app.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.R
import com.fitquest.app.PoseResultActivity
import com.fitquest.app.data.remote.EvaluatePostureRequest
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentJourneyBinding
import com.fitquest.app.databinding.FragmentPoseBinding
import com.fitquest.app.util.ActivityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseFragment : Fragment() {

    // ==== Exercise Spinner ====
    private lateinit var spinnerExercisePose: Spinner
    private var selectedExercise: String =
        ActivityUtils.activityMetadataMap.keys.firstOrNull() ?: "squat"

    // Camera
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    // UI
    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: ImageButton
    private lateinit var btnUpload: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var tvCountdown: TextView
    private lateinit var imgAnalysisResult: ImageView
    private lateinit var tvGuideText: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvInfoMessage: TextView

    private var countdownTimer: CountDownTimer? = null
    private var lastPhotoFile: File? = null
    private var orientationListener: OrientationEventListener? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    // 결과 화면 다녀온 뒤 카메라/화면을 리셋할지 여부
    private var shouldResetCameraOnResume: Boolean = false

    private var _binding: FragmentPoseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ==== Bind Views ====
        spinnerExercisePose = view.findViewById(R.id.spinnerExercisePose)
        previewView = view.findViewById(R.id.cameraPreview)
        btnCapture = view.findViewById(R.id.btnCapture)
        btnUpload = view.findViewById(R.id.btnUpload)
        btnSwitchCamera = view.findViewById(R.id.btnSwitchCamera)
        tvCountdown = view.findViewById(R.id.tvCountdown)
        imgAnalysisResult = view.findViewById(R.id.imgAnalysisResult)
        tvGuideText = view.findViewById(R.id.tvGuideText)
        progressLoading = view.findViewById(R.id.progressLoading)
        tvInfoMessage = view.findViewById(R.id.tvInfoMessage)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // ==== Exercise Spinner Init (AiCoachFragment와 동일한 목록) ====
        val activityKeys = ActivityUtils.activityMetadataMap.keys.toList()
        val exerciseListWithEmoji = ActivityUtils.activityMetadataMap.values.map { metadata ->
            "${metadata.emoji} ${metadata.label}"
        }

        // 기본 선택 운동
        selectedExercise = activityKeys.firstOrNull() ?: "squat"

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            exerciseListWithEmoji
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExercisePose.adapter = spinnerAdapter

        spinnerExercisePose.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                pos: Int,
                id: Long
            ) {
                val key = activityKeys.getOrNull(pos) ?: "squat"
                selectedExercise = key.lowercase()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedExercise = activityKeys.firstOrNull() ?: "squat"
            }
        }

        // ==== Gallery Picker ====
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val file = createFileFromUri(uri)
                    if (file != null) {
                        lastPhotoFile = file
                        processAndUpload(file)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load selected image.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // ==== Camera Permission ====
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // ==== Orientation Listener ====
        orientationListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when {
                    orientation in 45..134  -> Surface.ROTATION_270
                    orientation in 135..224 -> Surface.ROTATION_180
                    orientation in 225..314 -> Surface.ROTATION_90
                    else                    -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rotation
            }
        }
        orientationListener?.enable()

        // ==== Buttons ====
        btnCapture.setOnClickListener { startCountdownAndCapture() }
        btnUpload.setOnClickListener { openGalleryForImage() }
        btnSwitchCamera.setOnClickListener { toggleCamera() }
    }

    // 사용자가 PoseResultActivity에서 돌아왔을 때 호출
    override fun onResume() {
        super.onResume()
        if (shouldResetCameraOnResume) {
            shouldResetCameraOnResume = false
            resetCameraUiAndRestart()
        }
    }

    // ================= CAMERA =================
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(binding.cameraPreview.display?.rotation ?: Surface.ROTATION_0)
            .build()

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            if (!provider.hasCamera(selector)) return
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, imageCapture)
        } catch (exc: Exception) {
            Log.e("PoseFragment", "Camera bind failed", exc)
            Toast.makeText(
                requireContext(),
                "Failed to bind camera: ${exc.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT
        bindCameraUseCases()
    }

    // ================= COUNTDOWN =================
    private fun startCountdownAndCapture() {
        var seconds = 10
        binding.tvCountdown.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(10_000, 1000) {
            override fun onTick(ms: Long) {
                tvCountdown.text = seconds.toString()
                seconds--
            }

            override fun onFinish() {
                binding.tvCountdown.visibility = View.GONE
                capturePhoto()
                binding.btnCapture.isEnabled = true
            }
        }.start()
    }

    // ================= TAKE PHOTO =================
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        val file = File(
            requireContext().externalCacheDir,
            "pose_${System.currentTimeMillis()}.jpg"
        )

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }

        val output = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(metadata).build()

        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        "Capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    lastPhotoFile = file
                    processAndUpload(file)
                }
            }
        )
    }

    // ================= GALLERY =================
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun createFileFromUri(uri: Uri): File? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(
                requireContext().cacheDir,
                "gallery_${System.currentTimeMillis()}.jpg"
            )
            file.outputStream().use { out -> input.copyTo(out) }
            file
        } catch (e: Exception) {
            Log.e("PoseFragment", "Failed to create file from uri", e)
            null
        }
    }

    // ================= PROCESS + UPLOAD =================
    private fun processAndUpload(photoFile: File) {
        btnUpload.isEnabled = false

        val bitmap = decodeBitmapWithExifCorrected(photoFile)
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Failed to decode image.", Toast.LENGTH_SHORT).show()
            btnUpload.isEnabled = true
            return
        }

        // 카메라 대신 정지 이미지 표시
        tvGuideText.visibility = View.GONE
        previewView.visibility = View.GONE
        imgAnalysisResult.visibility = View.VISIBLE
        imgAnalysisResult.setImageBitmap(bitmap)

        // 카메라 해제 & 버튼 비활성화
        cameraProvider?.unbindAll()
        imageCapture = null
        binding.btnCapture.isEnabled = false
        binding.btnSwitchCamera.isEnabled = false

        val base64 = bitmapToBase64(bitmap)
        val fullUrl = "http://147.46.78.29:8004/pose/evaluate_posture/"

        progressLoading.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            var goodPointsResult = ""
            var improvePointsResult = ""
            var cueResult = ""
            var errorMessage: String? = null

            withContext(Dispatchers.IO) {
                try {
                    val body = EvaluatePostureRequest(
                        category = selectedExercise,   // <-- AiCoach와 동일 key 사용
                        image_base64 = base64
                    )
                    val resp = RetrofitClient.apiService.evaluatePosture(fullUrl, body)

                    if (resp.isSuccessful) {
                        val data = resp.body()
                        if (data == null) {
                            errorMessage = "Empty response."
                        } else if (data.status == "success") {
                            goodPointsResult = data.good_points.ifBlank { "None" }
                            improvePointsResult = data.improvement_points.ifBlank { "None" }
                            cueResult = data.improvement_methods?.ifBlank { "None" } ?: "None"
                        } else {
                            errorMessage = "Server status: ${data.status}"
                        }
                    } else {
                        errorMessage =
                            "HTTP ${resp.code()} - ${resp.errorBody()?.string().orEmpty()}"
                    }
                } catch (e: Exception) {
                    Log.e("PoseFragment", "evaluatePosture failed", e)
                    errorMessage = "Network error: ${e.message}"
                }
            }

            progressLoading.visibility = View.GONE
            btnUpload.isEnabled = true

            if (errorMessage != null) {
                Toast.makeText(
                    requireContext(),
                    "Failed to evaluate pose: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            // 🎯 결과 화면 갔다가 돌아오면 카메라/화면을 리셋하기 위해 플래그 켜두기
            shouldResetCameraOnResume = true

            // 결과 액티비티로 이동
            val intent = Intent(requireContext(), PoseResultActivity::class.java).apply {
                putExtra(PoseResultActivity.EXTRA_GOOD_POINTS, goodPointsResult)
                putExtra(PoseResultActivity.EXTRA_IMPROVE_POINTS, improvePointsResult)
                putExtra(PoseResultActivity.EXTRA_CUE, cueResult)
            }
            startActivity(intent)
        }
    }

    // === 결과에서 돌아온 후 카메라/화면 리셋 ===
    private fun resetCameraUiAndRestart() {
        // 정지 이미지 제거
        imgAnalysisResult.setImageDrawable(null)
        imgAnalysisResult.visibility = View.GONE

        // 가이드 텍스트 + 카메라 프리뷰 다시 표시
        tvGuideText.visibility = View.VISIBLE
        previewView.visibility = View.VISIBLE

        // 버튼 다시 활성화
        btnCapture.isEnabled = true
        btnUpload.isEnabled = true
        btnSwitchCamera.isEnabled = true

        // 로딩/메시지 초기화
        progressLoading.visibility = View.GONE

        // 카메라 다시 시작
        if (allPermissionsGranted()) {
            startCamera()
        }
    }

    // ================= Utils =================
    private fun decodeBitmapWithExifCorrected(file: File): Bitmap? {
        val src = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val exif = try {
            ExifInterface(file.absolutePath)
        } catch (e: Exception) {
            return src
        }

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f);  m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE-> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> { /* no-op */ }
        }

        return try {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        } catch (e: Exception) {
            src
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = downscale(bitmap, 720)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val maxDim = maxOf(w, h)
        if (maxDim <= maxSide) return src
        val scale = maxSide.toFloat() / maxDim.toFloat()
        val nw = (w * scale).toInt()
        val nh = (h * scale).toInt()
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        orientationListener?.disable()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
    }
}
