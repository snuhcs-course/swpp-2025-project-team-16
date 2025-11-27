package com.fitquest.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fitquest.app.R
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.data.remote.PoseResultSaveRequest
import com.fitquest.app.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import java.io.ByteArrayOutputStream

class PoseResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pose_result)

        val tvGood = findViewById<TextView>(R.id.tvResultGoodPoints)
        val tvImprove = findViewById<TextView>(R.id.tvResultImprovePoints)
        val tvCue = findViewById<TextView>(R.id.tvResultCue)
        val btnClose = findViewById<ImageButton>(R.id.btnResultClose)
        val btnSave = findViewById<Button>(R.id.btnResultSave)

        val good = intent.getStringExtra(EXTRA_GOOD_POINTS) ?: "None"
        val improve = intent.getStringExtra(EXTRA_IMPROVE_POINTS) ?: "None"
        val cue = intent.getStringExtra(EXTRA_CUE) ?: "None"
        val ivPose  = findViewById<ImageView>(R.id.ivPoseImage)


        tvGood.text = good
        tvImprove.text = improve
        tvCue.text = cue

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (!imagePath.isNullOrBlank()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                ivPose.setImageBitmap(bitmap)
                ivPose.visibility = View.VISIBLE
            } else {
                ivPose.visibility = View.GONE
            }
        } else {
            ivPose.visibility = View.GONE
        }


        btnClose.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            // TODO: 실제 저장 로직 붙이고 싶으면 여기에서 (예: 서버로 전송, 로컬 DB 기록)
            savePoseResult(good, improve, cue, imagePath)
            finish()
        }
    }

    private fun savePoseResult(
        good: String,
        improve: String,
        cue: String,
        imagePath: String?
    ) {
        if (imagePath.isNullOrBlank()) {
            Toast.makeText(this, "No image to save.", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap == null) {
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = bitmapToBase64(bitmap)
        val createdAt = LocalDateTime.now() // 🔹 저장 버튼 누른 시각

        val request = PoseResultSaveRequest(
            good_points = good,
            improvement_points = improve,
            improvement_methods = cue,
            created_at = createdAt,
            image_base64 = imageBase64
        )

        // 코루틴으로 네트워크 호출
        lifecycleScope.launch {
            var errorMessage: String? = null
            var success = false

            withContext(Dispatchers.IO) {
                try {
                    val resp = RetrofitClient.poseAnalysisApiService.savePoseResult(request)
                    success = resp.isSuccessful
                    if (!resp.isSuccessful) {
                        errorMessage = "HTTP ${resp.code()}"
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Unknown error"
                }
            }

            if (success) {
                Toast.makeText(
                    this@PoseResultActivity,
                    "Pose feedback saved.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this@PoseResultActivity,
                    "Failed to save: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun bitmapToBase64(bitmap: Bitmap): String {
        // PoseFragment와 비슷하게 리사이즈 + JPEG 압축
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




    companion object {
        const val EXTRA_GOOD_POINTS = "extra_good_points"
        const val EXTRA_IMPROVE_POINTS = "extra_improve_points"
        const val EXTRA_CUE = "extra_cue"
        const val EXTRA_IMAGE_PATH = "extra_image_path"   // ✅ 새로 추가
    }
}
