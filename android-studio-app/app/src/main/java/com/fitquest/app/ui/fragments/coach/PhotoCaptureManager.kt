package com.fitquest.app.ui.fragments.coach

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.exifinterface.media.ExifInterface
import com.fitquest.app.ui.coachutils.counter.PlankTimer
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

class PhotoCaptureManager(
    private val context: Context
) {
    private val bottomRepPhotoFiles = mutableListOf<File>()
    private var lastCapturedRepIndex: Int = -1
    private var lastCaptureTimeMs: Long = 0L

    fun getPhotos(): List<File> = bottomRepPhotoFiles

    fun clear() {
        bottomRepPhotoFiles.clear()
        lastCapturedRepIndex = -1
        lastCaptureTimeMs = 0L
    }

    /**
     * 조건에 맞으면 사진 캡처 시도
     * @return 캡처를 시도했는지 여부
     */
    fun captureIfNeeded(
        imageCapture: ImageCapture?,
        executor: Executor,
        exerciseName: String,
        currentRep: Int,
        phase: String?
    ): Boolean {
        val nowMs = System.currentTimeMillis()
        val lowerName = exerciseName.lowercase()

        // 촬영 조건 확인
        val phaseOk = when (lowerName) {
            "plank" -> phase == PlankTimer.Phase.HOLDING.name
            else -> (phase == "BOTTOM" || phase == "DOWN_REACHED")
        }
        if (!phaseOk) return false

        // 간격 확인
        val intervalMs = when (lowerName) {
            "plank" -> CoachConstants.PLANK_CAPTURE_INTERVAL_MS
            else -> CoachConstants.MIN_CAPTURE_INTERVAL_MS
        }
        if (nowMs - lastCaptureTimeMs < intervalMs) return false

        // 스쿼트/런지는 같은 rep에서 중복 방지
        if (lowerName != "plank" && currentRep <= lastCapturedRepIndex) return false

        val capture = imageCapture ?: return false

        val file = File(
            context.externalCacheDir,
            "${CoachConstants.FILE_PREFIX_BOTTOM_REP}${currentRep}_${System.currentTimeMillis()}${CoachConstants.FILE_EXTENSION}"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        lastCaptureTimeMs = nowMs

        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    // 실패해도 앱 진행
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        correctImageOrientation(file)
                        bottomRepPhotoFiles.add(file)

                        if (lowerName != "plank") {
                            lastCapturedRepIndex = currentRep
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )

        return true
    }

    private fun correctImageOrientation(file: File) {
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        FileOutputStream(file).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        rotated.recycle()
    }
}