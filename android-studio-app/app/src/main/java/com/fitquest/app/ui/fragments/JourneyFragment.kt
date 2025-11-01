package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fitquest.app.R
import com.google.android.material.bottomsheet.BottomSheetDialog

class JourneyFragment : Fragment() {

    private lateinit var timelineContainer: LinearLayout

    data class Exercise(
        val emoji: String,
        val name: String,
        val detail: String,
        val status: String
    )

    data class WorkoutDay(
        val date: String,
        val exercises: List<Exercise>,
        val xp: String
    )

    private val dummySchedule = listOf(
        WorkoutDay(
            "Nov 1",
            listOf(
                Exercise("💪", "Pike Push-ups", "10 reps × 3 sets", "Ready"),
                Exercise("🧱", "Wall Sit", "60s × 3 sets", "Ready"),
                Exercise("🚴", "Bicycle Crunches", "20 reps × 3 sets", "Ready")
            ),
            "+250 XP"
        ),
        WorkoutDay(
            "Nov 2",
            listOf(
                Exercise("🏋️", "Lunges", "15 reps × 3 sets", "Ready"),
                Exercise("🧍", "Wall Sit", "45s × 3 sets", "Ready"),
                Exercise("🔥", "Crunches", "25 reps × 3 sets", "Ready")
            ),
            "+180 XP"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_journey, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timelineContainer = view.findViewById(R.id.timelineContainer)
        populateTimeline()
    }

    private fun populateTimeline() {
        val inflater = LayoutInflater.from(requireContext())
        timelineContainer.removeAllViews()

        dummySchedule.forEachIndexed { index, workout ->
            val nodeView = inflater.inflate(R.layout.item_journey_daynode, timelineContainer, false)

            val leftCard = nodeView.findViewById<View>(R.id.summaryCardLeft)
            val rightCard = nodeView.findViewById<View>(R.id.summaryCardRight)
            val activeCard = if (index % 2 == 0) rightCard else leftCard
            val inactiveCard = if (index % 2 == 0) leftCard else rightCard
            inactiveCard.visibility = View.GONE
            activeCard.visibility = View.VISIBLE

            val tvDate = activeCard.findViewById<TextView>(R.id.tvDate)
            val tvWorkoutSummary = activeCard.findViewById<TextView>(R.id.tvWorkoutSummary)
            val tvXp = activeCard.findViewById<TextView>(R.id.tvXp)

            tvDate.text = workout.date
            tvWorkoutSummary.text = workout.exercises.joinToString(", ") { it.name }
            tvXp.text = workout.xp

            if (index == 0) activeCard.setBackgroundResource(R.drawable.card_glow_outer)

            // 클릭 → 바텀시트 열기
            activeCard.setOnClickListener {
                showWorkoutDetails(workout)
            }

            timelineContainer.addView(nodeView)
        }
    }

    /**
     * 하단 팝업(바텀시트)에 운동 목록 표시
     */
    private fun showWorkoutDetails(workout: WorkoutDay) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_journey_daydetail, null)
        dialog.setContentView(view)

        val tvDayTitle = view.findViewById<TextView>(R.id.tvDayTitle)
        val exerciseListContainer = view.findViewById<LinearLayout>(R.id.exerciseListContainer)
        val btnClose = view.findViewById<View>(R.id.btnClose)

        tvDayTitle.text = workout.date
        btnClose.setOnClickListener { dialog.dismiss() }

        // 운동 카드 여러 개 붙이기
        workout.exercises.forEach { ex ->
            val itemView = layoutInflater.inflate(R.layout.item_exercise, exerciseListContainer, false)
            itemView.findViewById<TextView>(R.id.tvExerciseEmoji).text = ex.emoji
            itemView.findViewById<TextView>(R.id.tvExerciseName).text = ex.name
            itemView.findViewById<TextView>(R.id.tvExerciseDetails).text = ex.detail
            itemView.findViewById<TextView>(R.id.tvProgressText).text = ex.status
            exerciseListContainer.addView(itemView)
        }

        dialog.show()
    }
}
