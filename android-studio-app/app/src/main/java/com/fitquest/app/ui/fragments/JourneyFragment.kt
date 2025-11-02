package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.model.Exercise
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class JourneyFragment : Fragment() {

    private lateinit var timelineContainer: LinearLayout

    data class WorkoutDay(
        val date: String,
        val exercises: List<Exercise>,
        val xp: String
    )

    private var scheduleList: List<WorkoutDay> = emptyList()

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

        // ✅ 서버에서 일정 데이터 가져오기
        fetchScheduleFromServer()
    }

    private fun populateTimeline() {
        val inflater = LayoutInflater.from(requireContext())
        timelineContainer.removeAllViews()

        scheduleList.forEachIndexed { index, workout ->
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

            activeCard.setOnClickListener {
                showWorkoutDetails(workout)
            }

            timelineContainer.addView(nodeView)
        }
        val scrollView = view?.findViewById<ScrollView>(R.id.scrollTimeline)
        scrollView?.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showWorkoutDetails(workout: WorkoutDay) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_journey_daydetail, null)
        dialog.setContentView(view)

        val tvDayTitle = view.findViewById<TextView>(R.id.tvDayTitle)
        val exerciseListContainer = view.findViewById<LinearLayout>(R.id.exerciseListContainer)
        val btnClose = view.findViewById<View>(R.id.btnClose)

        tvDayTitle.text = workout.date
        btnClose.setOnClickListener { dialog.dismiss() }

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

    private fun fetchScheduleFromServer() {
        val prefs = requireContext().getSharedPreferences("auth", 0)
        val token = prefs.getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.journeyApiService.getUserSchedules("Bearer $token")

                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()

                    // ✅ 오늘 날짜 구하기 (yyyy-MM-dd 형식으로)
                    val today = java.time.LocalDate.now()

                    // ✅ 오늘 이후(또는 오늘 포함) 데이터만 필터링
                    val filtered = data.filter { workout ->
                        try {
                            val workoutDate = java.time.LocalDate.parse(workout.date)
                            !workoutDate.isBefore(today) // 오늘보다 이전 날짜는 제외
                        } catch (e: Exception) {
                            false // 날짜 파싱 실패 시 제외
                        }
                    }

                    // ✅ 서버 데이터 → UI용으로 매핑
                    scheduleList = filtered.map { workout ->
                        WorkoutDay(
                            date = workout.date,
                            xp = "+${workout.xp} XP",
                            exercises = workout.exercises.map {
                                Exercise("🏋️", it.name, it.detail, it.status)
                            }
                        )
                    }

                    populateTimeline()
                } else {
                    Log.e("Journey", "Server Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("Journey", "Network error: ${e.localizedMessage}")
            }
        }
    }

}
