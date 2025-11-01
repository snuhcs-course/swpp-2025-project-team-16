package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fitquest.app.R
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var rankOverlay: View
    private lateinit var btnViewRankings: MaterialButton

    // 예시 데이터 (나중에 DB 연동)
    private val dummyHistory = listOf(
        HistoryDay(
            "Oct 29", "+250 XP", "95%", "35 min",
            listOf(
                Exercise("💪", "Push-ups", "20 / 20", "+100 XP", "98%", "10 min"),
                Exercise("🏋️", "Squats", "25 / 25", "+80 XP", "94%", "12 min"),
                Exercise("🧘", "Plank", "3 min hold", "+70 XP", "93%", "13 min")
            )
        ),
        HistoryDay(
            "Oct 28", "+180 XP", "88%", "28 min",
            listOf(
                Exercise("🤸", "Lunges", "20 / 20", "+90 XP", "89%", "15 min"),
                Exercise("🏃", "Jumping Jacks", "50 / 50", "+90 XP", "87%", "13 min")
            )
        ),
    )

    data class HistoryDay(
        val date: String,
        val xp: String,
        val percent: String,
        val time: String,
        val exercises: List<Exercise>
    )

    data class Exercise(
        val emoji: String,
        val name: String,
        val done: String,
        val xp: String,
        val accuracy: String,
        val duration: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyContainer = view.findViewById(R.id.historyContainer)
        rankOverlay = view.findViewById(R.id.rankOverlay)
        btnViewRankings = view.findViewById(R.id.btnViewRankings)

        populateHistory()
        setupRankButton()
    }

    /**
     * ====== History 목록 채우기 ======
     */
    private fun populateHistory() {
        val inflater = LayoutInflater.from(requireContext())
        historyContainer.removeAllViews()

        dummyHistory.forEachIndexed { index, history ->
            val nodeView = inflater.inflate(R.layout.item_historynode, historyContainer, false)

            // 카드 쪽 (좌우 번갈아 배치)
            val leftCard = nodeView.findViewById<View>(R.id.summaryCardLeft)
            val rightCard = nodeView.findViewById<View>(R.id.summaryCardRight)
            val activeCard = if (index % 2 == 0) rightCard else leftCard
            val inactiveCard = if (index % 2 == 0) leftCard else rightCard
            inactiveCard.visibility = View.GONE
            activeCard.visibility = View.VISIBLE

            val tvDate = activeCard.findViewById<TextView>(R.id.tvDate)
            val tvXp = activeCard.findViewById<TextView>(R.id.tvXp)
            val tvPercent = activeCard.findViewById<TextView>(R.id.tvPercent)
            val tvTime = activeCard.findViewById<TextView>(R.id.tvTime)

            tvDate.text = history.date
            tvXp.text = history.xp
            tvPercent.text = history.percent
            tvTime.text = history.time

            // 클릭 시 상세 보기
            activeCard.setOnClickListener {
                showDayDetail(history)
            }

            historyContainer.addView(nodeView)
        }
    }

    /**
     * ====== 랭킹 버튼 클릭 ======
     */
    private fun setupRankButton() {
        btnViewRankings.setOnClickListener {
            rankOverlay.visibility = View.VISIBLE
            rankOverlay.alpha = 0f
            rankOverlay.animate().alpha(1f).setDuration(250).start()

            rankOverlay.findViewById<View>(R.id.btnCloseRank)?.setOnClickListener {
                rankOverlay.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { rankOverlay.visibility = View.GONE }
                    .start()
            }
        }
    }

    /**
     * ====== 운동 상세 BottomSheet ======
     */
    private fun showDayDetail(history: HistoryDay) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_history_daydetail, null)

        // 헤더
        dialogView.findViewById<TextView>(R.id.tvDayTitle).text = history.date
        dialogView.findViewById<TextView>(R.id.tvTotalXp).text = history.xp
        dialogView.findViewById<TextView>(R.id.tvCompletion).text = history.percent
        dialogView.findViewById<TextView>(R.id.tvTotalTime).text = history.time

        // 운동 리스트 채우기
        val container = dialogView.findViewById<LinearLayout>(R.id.exercisedoneListContainer)
        container.removeAllViews()

        history.exercises.forEach { ex ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_exercise_done, container, false)

            itemView.findViewById<TextView>(R.id.tvExerciseEmoji).text = ex.emoji
            itemView.findViewById<TextView>(R.id.tvExerciseName).text = ex.name
            itemView.findViewById<TextView>(R.id.tvExerciseDetails).text = "Completed: ${ex.done}"
            itemView.findViewById<TextView>(R.id.tvXp).text = ex.xp
            itemView.findViewById<TextView>(R.id.tvPercent).text = ex.accuracy
            itemView.findViewById<TextView>(R.id.tvTime).text = ex.duration

            container.addView(itemView)
        }

        // BottomSheetDialog 생성
        val dialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetStyle)
        dialog.setContentView(dialogView)
        dialog.show()
    }
}
