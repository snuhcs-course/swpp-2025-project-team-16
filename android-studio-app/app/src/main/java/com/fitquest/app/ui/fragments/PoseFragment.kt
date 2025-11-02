package com.fitquest.app.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fitquest.app.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: ImageButton
    private lateinit var btnUpload: ImageButton
    private lateinit var tvCountdown: TextView
    private lateinit var imgAnalysisResult: ImageView
    private lateinit var tvGuideText: TextView
    private lateinit var tvGoodPoints: TextView
    private lateinit var tvImprovePoints: TextView
    private lateinit var tvCue: TextView

    private var countdownTimer: CountDownTimer? = null

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
        tvCountdown = view.findViewById(R.id.tvCountdown)
        imgAnalysisResult = view.findViewById(R.id.imgAnalysisResult)
        tvGuideText = view.findViewById(R.id.tvGuideText)
        tvGoodPoints = view.findViewById(R.id.tvGoodPoints)
        tvImprovePoints = view.findViewById(R.id.tvImprovePoints)
        tvCue = view.findViewById(R.id.tvCue)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // === Capture button: start 10-sec countdown ===
        btnCapture.setOnClickListener {
            startCountdownAndCapture()
        }

        // === Upload button: simulate DB load & update right panel ===
        btnUpload.setOnClickListener {
            showAnalysisResult()
        }
    }

    // =============== CAMERA INITIALIZATION ==================
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("PoseFragment", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
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
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("PoseFragment", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("PoseFragment", "Photo saved: ${photoFile.absolutePath}")
                }
            }
        )
        showAnalysisImage()
    }

    // =============== SHOW RESULT ==================
    private fun showAnalysisImage() {
        tvGuideText.visibility = View.GONE
        imgAnalysisResult.visibility = View.VISIBLE

        // TODO: Replace with real pose analysis bitmap later
        imgAnalysisResult.setImageResource(R.drawable.sample_pose_result)
    }
    private fun showAnalysisResult() {
        // TODO: Replace with real pose analysis bitmap later
        tvGoodPoints.text = "✅ Great shoulder alignment detected"
        tvImprovePoints.text = "⚠️  Try to straighten your left leg"
        tvCue.text = "💡 Keep your core tight and head up"
    }

    // =============== PERMISSIONS ==================
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
