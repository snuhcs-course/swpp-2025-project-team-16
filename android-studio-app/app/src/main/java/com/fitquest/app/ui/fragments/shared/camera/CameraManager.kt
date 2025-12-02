package com.fitquest.app.ui.fragments.shared.camera

import android.content.Context
import android.util.Log
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set

    fun startCamera(
        previewView: PreviewView,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(previewView, null, onError)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get camera provider", e)
                onError("Failed to initialize camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindCameraUseCases(
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer? = null,
        onError: (String) -> Unit
    ) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageAnalyzer = if (analyzer != null) {
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }
        } else null

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
            .build()

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            if (!provider.hasCamera(selector)) {
                val message = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    "This device does not have a front camera."
                } else {
                    "This device does not have a rear camera."
                }
                onError(message)
                return
            }

            provider.unbindAll()

            // UseCase 리스트 구성
            val useCases = mutableListOf<UseCase>(preview)
            imageAnalyzer?.let { useCases.add(it) }
            imageCapture?.let { useCases.add(it) }

            provider.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
        } catch (exc: Exception) {
            Log.e(TAG, "Camera bind failed", exc)
            onError("Failed to bind camera: ${exc.message}")
        }
    }

    fun toggleCamera(
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer? = null,
        onError: (String) -> Unit
    ) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraUseCases(previewView, analyzer, onError)
    }

    fun capturePhoto(
        outputFile: File,
        executor: Executor,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera not initialized")
            return
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }

        val output = ImageCapture.OutputFileOptions.Builder(outputFile)
            .setMetadata(metadata)
            .build()

        capture.takePicture(
            output,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    onError("Capture failed: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSuccess(outputFile)
                }
            }
        )
    }

    fun setTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
    }

    fun unbindAll() {
        cameraProvider?.unbindAll()
        imageCapture = null
        imageAnalyzer = null
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}