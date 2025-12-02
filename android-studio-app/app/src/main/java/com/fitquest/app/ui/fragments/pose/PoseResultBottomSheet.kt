package com.fitquest.app.ui.fragments.pose

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fitquest.app.databinding.BottomSheetPoseResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PoseResultBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPoseResultBinding? = null
    private val binding get() = _binding!!

    private var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPoseResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goodPoints = arguments?.getString(ARG_GOOD_POINTS) ?: "None"
        val improvePoints = arguments?.getString(ARG_IMPROVE_POINTS) ?: "None"
        val cue = arguments?.getString(ARG_CUE) ?: "None"
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)

        binding.tvResultGoodPoints.text = goodPoints
        binding.tvResultImprovePoints.text = improvePoints
        binding.tvResultCue.text = cue

        // 이미지 표시
        if (!imagePath.isNullOrBlank()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                binding.ivPoseImage.setImageBitmap(bitmap)
                binding.ivPoseImage.visibility = View.VISIBLE
            } else {
                binding.ivPoseImage.visibility = View.GONE
            }
        } else {
            binding.ivPoseImage.visibility = View.GONE
        }

        binding.btnResultClose.setOnClickListener {
            dismiss()
        }

        binding.btnResultSave.setOnClickListener {
            // TODO: 저장 로직 (필요시)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissCallback?.invoke()
        _binding = null
    }

    fun setOnDismissCallback(callback: () -> Unit) {
        onDismissCallback = callback
    }

    companion object {
        private const val ARG_GOOD_POINTS = "good_points"
        private const val ARG_IMPROVE_POINTS = "improve_points"
        private const val ARG_CUE = "cue"
        private const val ARG_IMAGE_PATH = "image_path"

        fun newInstance(
            goodPoints: String,
            improvePoints: String,
            cue: String,
            imagePath: String?
        ): PoseResultBottomSheet {
            return PoseResultBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_GOOD_POINTS, goodPoints)
                    putString(ARG_IMPROVE_POINTS, improvePoints)
                    putString(ARG_CUE, cue)
                    putString(ARG_IMAGE_PATH, imagePath)
                }
            }
        }
    }
}