package com.fitquest.app.ui.fragments.login

import android.view.View
import com.fitquest.app.databinding.FragmentSignupStep2Binding

class SignupUiManager(
    private val binding: FragmentSignupStep2Binding
) {
    fun updateCountdownText(seconds: Int) {
        binding.tvCountdown.text = seconds.toString()
        binding.tvCountdown.visibility = View.VISIBLE
    }

    fun hideCountdown() {
        binding.tvCountdown.visibility = View.GONE
    }

    fun showTrackingLockMessage(show: Boolean) {
        if (show) {
            binding.tvCountdown.text = SignupConstants.MSG_STEP_BACK
            binding.tvCountdown.visibility = View.VISIBLE
        } else {
            binding.tvCountdown.visibility = View.GONE
        }
    }

    fun updateRepCount(count: Int) {
        binding.tvCountNumber.text = count.toString()
    }

    fun setOverlayVisibility(visible: Boolean) {
        binding.overlay.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun clearOverlay() {
        binding.overlay.clear()
        binding.overlay.visibility = View.GONE
    }
}