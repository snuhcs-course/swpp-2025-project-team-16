package com.fitquest.app.ui.fragments.coach

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fitquest.app.R
import com.fitquest.app.databinding.FragmentAiCoachBinding
import com.fitquest.app.util.ActivityUtils
import com.fitquest.app.util.TargetType
import com.google.android.material.button.MaterialButton
import java.util.Locale

class CoachUiManager(
    private val context: Context,
    private val binding: FragmentAiCoachBinding
) {
    fun applyTrainingButtonStyle(isTraining: Boolean) {
        if (isTraining) {
            binding.btnStartWorkout.apply {
                text = "Stop Training"
                icon = ContextCompat.getDrawable(context, R.drawable.ic_pause_square)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.error_red)
                setTextColor(Color.WHITE)
                iconTint = ContextCompat.getColorStateList(context, android.R.color.white)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            }
        } else {
            binding.btnStartWorkout.apply {
                text = "Begin Training"
                icon = ContextCompat.getDrawable(context, R.drawable.ic_begin_triangle)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.success_green)
                setTextColor(Color.WHITE)
                iconTint = ContextCompat.getColorStateList(context, android.R.color.white)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            }
        }
    }

    fun applyExerciseUi(exerciseName: String) {
        val lowerCaseName = exerciseName.lowercase(Locale.getDefault())

        binding.tvCurrentExerciseEmoji.text = ActivityUtils.getEmoji(lowerCaseName)

        when (lowerCaseName) {
            "plank" -> binding.labelReps.text = "SECONDS"
            "squat", "lunge" -> binding.labelReps.text = "REPS"
            else -> binding.labelReps.text = "REPS"
        }

        binding.tvRepCount.text = if (lowerCaseName == "plank") "0.0" else "0"
        binding.tvXpPoints.text = "+0"
    }

    fun updateTrainingUiVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.hudTopContainer.visibility = visibility
        binding.recordingIndicator.visibility = visibility
        binding.formScoreContainer.visibility = visibility
        binding.overlay.visibility = visibility
    }

    fun updateCountdownUi(showCountdown: Boolean) {
        if (showCountdown) {
            binding.hudTopContainer.visibility = View.GONE
            binding.recordingIndicator.visibility = View.GONE
            binding.formScoreContainer.visibility = View.GONE
            binding.overlay.visibility = View.GONE
            binding.tvFeedback.text = CoachConstants.COACH_MSG_READY
        } else {
            updateTrainingUiVisibility(true)
        }
    }

    fun updateTargetUi(
        isScheduled: Boolean,
        exerciseName: String,
        repsTarget: Int?,
        durationTarget: Int?
    ) {
        if (isScheduled) {
            val exerciseLabel = ActivityUtils.getLabel(exerciseName)
            val targetType = ActivityUtils.getTargetType(exerciseName)

            val targetText = when (targetType) {
                TargetType.REPS -> if (repsTarget != null) {
                    "$exerciseLabel Target: $repsTarget Reps"
                } else {
                    "$exerciseLabel Scheduled"
                }
                TargetType.DURATION -> if (durationTarget != null) {
                    "$exerciseLabel Target: $durationTarget Secs"
                } else {
                    "$exerciseLabel Scheduled"
                }
                else -> "$exerciseLabel Scheduled"
            }
            binding.tvSystemSubtitle.text = targetText
        } else {
            binding.tvSystemSubtitle.text = "Start your session"
        }
    }

    fun showRepPopup(repPopupText: TextView, count: Int) {
        repPopupText.text = count.toString()
        repPopupText.visibility = View.VISIBLE
        repPopupText.alpha = 1f
        repPopupText.scaleX = 1f
        repPopupText.scaleY = 1f

        repPopupText.animate().cancel()
        repPopupText.animate()
            .scaleX(CoachConstants.REP_POPUP_SCALE)
            .scaleY(CoachConstants.REP_POPUP_SCALE)
            .alpha(0f)
            .setDuration(CoachConstants.REP_POPUP_DURATION_MS)
            .withEndAction {
                repPopupText.visibility = View.GONE
                repPopupText.alpha = 1f
                repPopupText.scaleX = 1f
                repPopupText.scaleY = 1f
            }
            .start()
    }

    fun showTrackingLockMessage(show: Boolean) {
        if (show) {
            binding.tvCountdown.text = CoachConstants.COACH_MSG_STEP_BACK
            binding.tvCountdown.visibility = View.VISIBLE
        } else {
            binding.tvCountdown.visibility = View.GONE
        }
    }

    fun updateCountdownText(seconds: Int) {
        binding.tvCountdown.text = seconds.toString()
        binding.tvCountdown.visibility = View.VISIBLE
    }

    fun hideCountdown() {
        binding.tvCountdown.visibility = View.GONE
    }

    fun setFeedbackMessage(message: String) {
        binding.tvFeedback.text = message
    }

    fun clearOverlay() {
        binding.overlay.clear()
    }
}