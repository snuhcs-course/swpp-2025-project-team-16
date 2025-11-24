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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.PoseResultActivity
import com.fitquest.app.data.remote.EvaluatePostureRequest
import com.fitquest.app.data.remote.RetrofitClient
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

    private var _binding: FragmentPoseBinding? = null
    private val binding get() = _binding!!

    // ==== Exercise selection ====
    private var selectedExercise: String =
        ActivityUtils.activityMetadataMap.keys.firstOrNull() ?: "squat"

    // Camera
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    // State
    private var countdownTimer: CountDownTimer? = null
    private var loadingTimer: CountDownTimer? = null
    private var lastPhotoFile: File? = null
    private var orientationListener: OrientationEventListener? = null

    // Gallery picker
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    // 결과 화면 다녀온 뒤 카메라/화면을 리셋할지 여부
    private var shouldResetCameraOnResume: Boolean = false

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

        cameraExecutor = Executors.newSingleThreadExecutor()

        // ==== Exercise Spinner Init (AiCoachFragment와 동일한 목록) ====
        val activityKeys = ActivityUtils.activityMetadataMap.keys.toList()
        val exerciseListWithEmoji = ActivityUtils.activityMetadataMap.values.map { metadata ->
            "${metadata.emoji} ${metadata.label}"
        }

        selectedExercise = activityKeys.firstOrNull() ?: "squat"

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            exerciseListWithEmoji
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerExercisePose.adapter = spinnerAdapter

        binding.spinnerExercisePose.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

        val poseExerciseKey = arguments?.getString("poseExerciseKey")
        val poseImagePath = arguments?.getString("poseImagePath")

        if (poseExerciseKey != null) {
            val idx = activityKeys.indexOf(poseExerciseKey)
            if (idx >= 0) {
                binding.spinnerExercisePose.setSelection(idx)
                selectedExercise = poseExerciseKey.lowercase()
            }
        }

        val preloadedFile = poseImagePath?.let { File(it) }?.takeIf { it.exists() }
        if (preloadedFile != null) {
            // ✅ AiCoach에서 온 사진으로 바로 업로드 진행
            lastPhotoFile = preloadedFile
            processAndUpload(preloadedFile)
        } else {

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
            // 기존 카메라 시작 로직
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
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
        binding.btnCapture.setOnClickListener { startCountdownAndCapture() }
        binding.btnUpload.setOnClickListener { openGalleryForImage() }
        binding.btnSwitchCamera.setOnClickListener { toggleCamera() }
    }

    // PoseResultActivity에서 돌아왔을 때 호출
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
            if (!provider.hasCamera(selector)) {
                Toast.makeText(
                    requireContext(),
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                        "이 기기에는 전면 카메라가 없습니다."
                    else
                        "이 기기에는 후면 카메라가 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

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

    private fun startLoadingProgress() {
        // progress bar 초기 설정
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvProgressPercent.visibility = View.VISIBLE

        binding.progressLoading.isIndeterminate = false
        binding.progressLoading.max = 100
        binding.progressLoading.progress = 0
        binding.tvProgressPercent.text = "0%"

        // 기존 타이머 있으면 정리
        loadingTimer?.cancel()

        val totalDuration = 60_000L      // 60초
        val targetProgress = 90          // 90%까지 채우기

        loadingTimer = object : CountDownTimer(totalDuration, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = totalDuration - millisUntilFinished
                val fraction = elapsed.toFloat() / totalDuration.toFloat()
                val progress = (fraction * targetProgress).toInt()
                binding.progressLoading.setProgressCompat(progress, true)
                binding.tvProgressPercent.text = "$progress%"
            }

            override fun onFinish() {
                // 60초가 다 지나도 아직 응답 안 왔으면 90%까지만 채워둠
                binding.progressLoading.setProgressCompat(targetProgress, true)
                binding.tvProgressPercent.text = "$targetProgress%"
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
        binding.btnUpload.isEnabled = false

        val bitmap = decodeBitmapWithExifCorrected(photoFile)
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Failed to decode image.", Toast.LENGTH_SHORT).show()
            binding.btnUpload.isEnabled = true
            return
        }

        // 🔹 회전/보정이 적용된 비트맵을 파일로 저장해서,
        //    이후에는 이 파일을 기준으로 사용
        val processedFile = saveBitmapToFile(bitmap)
        lastPhotoFile = processedFile   // <-- 여기서 덮어쓰기

        // 카메라 대신 정지 이미지 표시
        binding.tvGuideText.visibility = View.GONE
        binding.cameraPreview.visibility = View.GONE
        binding.imgAnalysisResult.visibility = View.VISIBLE
        binding.imgAnalysisResult.setImageBitmap(bitmap)

        // 카메라 해제 & 버튼 비활성화
        cameraProvider?.unbindAll()
        imageCapture = null
        binding.btnCapture.isEnabled = false
        binding.btnSwitchCamera.isEnabled = false

        val base64 = bitmapToBase64(bitmap)
        val fullUrl = "http://147.46.78.29:8004/pose-analyses/evaluate_posture/"

        startLoadingProgress()

        viewLifecycleOwner.lifecycleScope.launch {
            var goodPointsResult = ""
            var improvePointsResult = ""
            var cueResult = ""
            var errorMessage: String? = null

            withContext(Dispatchers.IO) {
                try {
                    val body = EvaluatePostureRequest(
                        category = selectedExercise,
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

            // 🔹 여기서 응답이 온 시점
            //    → 타이머 정지 + 100%로 채우고 숨기기
            loadingTimer?.cancel()
            binding.progressLoading.setProgressCompat(100, true)
            binding.tvProgressPercent.text = "100%"


            binding.progressLoading.visibility = View.GONE
            binding.tvProgressPercent.visibility = View.GONE
            binding.btnUpload.isEnabled = true

            if (errorMessage != null) {
                Toast.makeText(
                    requireContext(),
                    "Failed to evaluate pose: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            shouldResetCameraOnResume = true

            val intent = Intent(requireContext(), PoseResultActivity::class.java).apply {
                putExtra(PoseResultActivity.EXTRA_GOOD_POINTS, goodPointsResult)
                putExtra(PoseResultActivity.EXTRA_IMPROVE_POINTS, improvePointsResult)
                putExtra(PoseResultActivity.EXTRA_CUE, cueResult)
                lastPhotoFile?.absolutePath?.let { path ->
                    putExtra(PoseResultActivity.EXTRA_IMAGE_PATH, path)
                }
            }
            startActivity(intent)
        }
    }

    // === 결과에서 돌아온 후 카메라/화면 리셋 ===
    private fun resetCameraUiAndRestart() {
        // 정지 이미지 제거
        binding.imgAnalysisResult.setImageDrawable(null)
        binding.imgAnalysisResult.visibility = View.GONE

        // 가이드 텍스트 + 카메라 프리뷰 다시 표시
        binding.tvGuideText.visibility = View.VISIBLE
        binding.cameraPreview.visibility = View.VISIBLE

        // 버튼 다시 활성화
        binding.btnCapture.isEnabled = true
        binding.btnUpload.isEnabled = true
        binding.btnSwitchCamera.isEnabled = true

        // 로딩 초기화
        loadingTimer?.cancel()
        binding.progressLoading.visibility = View.GONE
        binding.progressLoading.progress = 0

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

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        // cache 디렉토리에 새 파일 생성
        val file = File(
            requireContext().cacheDir,
            "pose_processed_${System.currentTimeMillis()}.jpg"
        )
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
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
        loadingTimer?.cancel()
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
