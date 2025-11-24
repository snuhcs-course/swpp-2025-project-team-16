package com.fitquest.app

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fitquest.app.R
import android.widget.ImageView

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
            Toast.makeText(this, "Pose feedback saved.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_GOOD_POINTS = "extra_good_points"
        const val EXTRA_IMPROVE_POINTS = "extra_improve_points"
        const val EXTRA_CUE = "extra_cue"
        const val EXTRA_IMAGE_PATH = "extra_image_path"   // ✅ 새로 추가
    }
}
