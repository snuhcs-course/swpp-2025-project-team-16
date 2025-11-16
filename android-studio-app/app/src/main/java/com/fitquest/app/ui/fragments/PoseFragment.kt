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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.R
import com.fitquest.app.data.remote.EvaluatePostureRequest
import com.fitquest.app.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // 현재 렌즈 방향 (기본: 전면)
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: ImageButton
    private lateinit var btnUpload: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var tvCountdown: TextView
    private lateinit var imgAnalysisResult: ImageView
    private lateinit var tvGuideText: TextView
    private lateinit var tvGoodPoints: TextView
    private lateinit var tvImprovePoints: TextView
    private lateinit var tvCue: TextView

    private var countdownTimer: CountDownTimer? = null
    private var lastPhotoFile: File? = null
    private var orientationListener: OrientationEventListener? = null

    // 갤러리에서 이미지 선택용 런처
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pose, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- View binding ---
        previewView = view.findViewById(R.id.cameraPreview)
        btnCapture = view.findViewById(R.id.btnCapture)
        btnUpload = view.findViewById(R.id.btnUpload)
        btnSwitchCamera = view.findViewById(R.id.btnSwitchCamera)
        tvCountdown = view.findViewById(R.id.tvCountdown)
        imgAnalysisResult = view.findViewById(R.id.imgAnalysisResult)
        tvGuideText = view.findViewById(R.id.tvGuideText)
        tvGoodPoints = view.findViewById(R.id.tvGoodPoints)
        tvImprovePoints = view.findViewById(R.id.tvImprovePoints)
        tvCue = view.findViewById(R.id.tvCue)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 갤러리 선택 런처 등록
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
                        tvCue.text = "이미지를 불러오지 못했습니다."
                    }
                } else {
                    tvCue.text = "이미지가 선택되지 않았습니다."
                }
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // 디바이스 회전 반영 → 캡처 회전 정확히
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

        // 촬영 버튼: 10초 카운트다운 후 촬영
        btnCapture.setOnClickListener { startCountdownAndCapture() }

        // 업로드 버튼: 갤러리에서 사진 선택 후 서버 업로드
        btnUpload.setOnClickListener {
            openGalleryForImage()
        }

        // 카메라 전환 버튼
        btnSwitchCamera.setOnClickListener { toggleCamera() }
    }

    // =============== CAMERA ==================
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
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
            .build()

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            if (!provider.hasCamera(selector)) {
                tvCue.text = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    "이 기기에는 전면 카메라가 없습니다."
                else
                    "이 기기에는 후면 카메라가 없습니다."
                return
            }

            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, imageCapture)
        } catch (exc: Exception) {
            Log.e("PoseFragment", "Camera bind failed", exc)
            tvCue.text = "카메라 바인딩 실패: ${exc.message}"
        }
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT
        bindCameraUseCases()
    }

    // =============== COUNTDOWN TIMER ==================
    private fun startCountdownAndCapture() {
        tvCountdown.visibility = View.VISIBLE
        btnCapture.isEnabled = false
        var seconds = 10

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(10_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = seconds.toString()
                seconds--
            }

            override fun onFinish() {
                tvCountdown.visibility = View.GONE
                capturePhoto()
                btnCapture.isEnabled = true
            }
        }.start()
    }

    // =============== TAKE PHOTO ==================
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            requireContext().externalCacheDir,
            "pose_${System.currentTimeMillis()}.jpg"
        )

        // 전면 카메라일 때 좌우반전 메타데이터 세팅
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("PoseFragment", "Photo capture failed: ${exc.message}", exc)
                    tvCue.text = "사진 촬영 실패: ${exc.message}"
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("PoseFragment", "Photo saved: ${photoFile.absolutePath}")
                    lastPhotoFile = photoFile
                    processAndUpload(photoFile)
                }
            }
        )
    }

    // =============== GALLERY PICK ==================
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun createFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(
                requireContext().cacheDir,
                "gallery_${System.currentTimeMillis()}.jpg"
            )
            tempFile.outputStream().use { out ->
                inputStream.use { it.copyTo(out) }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("PoseFragment", "Failed to create file from uri", e)
            null
        }
    }

    // =============== PROCESS + UPLOAD ==================
    private fun processAndUpload(photoFile: File) {
        // 업로드 중엔 업로드 버튼 비활성화
        btnUpload.isEnabled = false

        // 1) 파일 → Bitmap (EXIF 보정 포함)
        val bitmap = decodeBitmapWithExifCorrected(photoFile)
        if (bitmap == null) {
            tvCue.text = "이미지 로드 실패"
            // 실패했으니 다시 업로드 가능하도록 되돌리기
            btnUpload.isEnabled = true
            return
        }

        // 카메라 영역에 정지 이미지 표시 & 카메라 비활성화
        tvGuideText.visibility = View.GONE
        imgAnalysisResult.visibility = View.VISIBLE
        previewView.visibility = View.GONE
        imgAnalysisResult.setImageBitmap(bitmap)

        // 카메라 사용 중지
        cameraProvider?.unbindAll()
        imageCapture = null
        btnCapture.isEnabled = false
        btnSwitchCamera.isEnabled = false

        // 2) Bitmap → Base64 (다운스케일+압축으로 전송량 절감)
        val base64 = bitmapToBase64(bitmap)

        // 3) 서버로 POST (코루틴 + Retrofit)
        val fullUrl = "http://147.46.78.29:8004/pose/evaluate_posture/"
        tvCue.text = "업로드 중..."

        viewLifecycleOwner.lifecycleScope.launch {
            val responseText = withContext(Dispatchers.IO) {
                try {
                    val body = EvaluatePostureRequest(
                        category = "squat",
                        image_base64 = base64
                    )
                    val resp = RetrofitClient.apiService.evaluatePosture(fullUrl, body)

                    if (resp.isSuccessful) {
                        val data = resp.body()
                        if (data == null) {
                            "빈 응답입니다."
                        } else if (data.status == "success") {
                            val good = data.good_points.ifBlank { "없음" }
                            val improve = data.improvement_points.ifBlank { "없음" }
                            val methods = data.improvement_methods?.ifBlank { "없음" } ?: "없음"

                            "✅ Good Points:\n$good\n\n⚠️ Improvement Points:\n$improve\n\n💡 Methods:\n$methods"
                        } else {
                            "서버 반환 상태: ${data.status}"
                        }
                    } else {
                        "HTTP ${resp.code()} - ${resp.errorBody()?.string().orEmpty()}"
                    }
                } catch (e: Exception) {
                    Log.e("PoseFragment", "evaluatePosture failed", e)
                    "네트워크 오류: ${e.message}"
                }
            }

            // UI 반영
            tvCue.text = ""
            tvGoodPoints.text = ""
            tvImprovePoints.text = ""
            tvCue.text = responseText

            // ✅ 업로드 완료/실패 후 다시 업로드 가능
            btnUpload.isEnabled = true
        }
    }



    // === Utils ===

    // EXIF(회전/미러) 보정 포함 디코드
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
            // 메모리 부족 등 시 원본이라도 반환
            src
        }
    }

    // 전송량 줄이기: 긴 변 720으로 다운스케일 + JPEG 85
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

    // =============== PERMISSIONS ==================
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        cameraExecutor.shutdown()
        orientationListener?.disable()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
