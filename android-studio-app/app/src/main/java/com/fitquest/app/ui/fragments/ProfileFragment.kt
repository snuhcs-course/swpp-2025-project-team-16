package com.fitquest.app.ui.fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.model.Exercise
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var rankOverlay: View
    private lateinit var btnViewRankings: MaterialButton
    private lateinit var rankListContainer: LinearLayout

    // podium (top3)
    private lateinit var tvFirstName: TextView
    private lateinit var tvSecondName: TextView
    private lateinit var tvThirdName: TextView

    // ======= Dummy History (임시) =======
    data class HistoryDay(
        val date: String,
        val xp: String,
        val percent: String,
        val time: String,
        val exercises: List<Exercise>
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchHistoryFromServer() {
        val prefs = requireContext().getSharedPreferences("auth", 0)
        val token = prefs.getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.profileApiService.getUserHistory("Bearer $token")
                if (response.isSuccessful) {
                    val historyList = response.body() ?: emptyList()

                    // ✅ 오늘 날짜
                    val today = java.time.LocalDate.now()

                    // ✅ 오늘 포함 "이전" 데이터만 필터링
                    val filtered = historyList.filter { item ->
                        try {
                            val date = java.time.LocalDate.parse(item.date)
                            !date.isAfter(today)   // 오늘 이후면 제외
                        } catch (e: Exception) {
                            false // 날짜 파싱 실패 시 제외
                        }
                    }

                    // ✅ UI용 매핑
                    val mappedList = filtered.map { item ->
                        HistoryDay(
                            date = item.date,
                            xp = "+200 XP",  // 서버 XP 미구현 시 임시값
                            percent = "100%",
                            time = "${item.start_time?.substring(0, 5)} - ${item.end_time?.substring(0, 5)}",
                            exercises = listOf(
                                Exercise("🏋️", item.name, "done", "+100 XP", accuracy = "100%", duration =  "30 min")
                            )
                        )
                    }

                    updateHistoryUI(mappedList)
                } else {
                    Log.e("HistoryFetch", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HistoryFetch", "Network error: ${e.localizedMessage}")
            }
        }
    }


    private fun updateHistoryUI(data: List<HistoryDay>) {
        val inflater = LayoutInflater.from(requireContext())
        historyContainer.removeAllViews()

        data.forEachIndexed { index, history ->
            val nodeView = inflater.inflate(R.layout.item_historynode, historyContainer, false)
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

            activeCard.setOnClickListener { showDayDetail(history) }
            historyContainer.addView(nodeView)
        }
    }

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
        rankListContainer = rankOverlay.findViewById(R.id.rankListContainer)

        // podium 이름 3개 찾기
        tvFirstName = rankOverlay.findViewById(R.id.tvFirstName)
        tvSecondName = rankOverlay.findViewById(R.id.tvSecondName)
        tvThirdName = rankOverlay.findViewById(R.id.tvThirdName)

        fetchHistoryFromServer()
        setupRankButton()
    }

    private fun setupRankButton() {
        btnViewRankings.setOnClickListener {
            rankOverlay.visibility = View.VISIBLE
            rankOverlay.alpha = 0f
            rankOverlay.animate().alpha(1f).setDuration(250).start()

            fetchRankData() // 서버 요청

            rankOverlay.findViewById<View>(R.id.btnCloseRank)?.setOnClickListener {
                rankOverlay.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { rankOverlay.visibility = View.GONE }
                    .start()
            }
        }
    }

    private fun fetchRankData() {
        val prefs = requireContext().getSharedPreferences("auth", 0)
        val token = prefs.getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.profileApiService.getRankings("Bearer $token")
                if (response.isSuccessful) {
                    val ranks = response.body() ?: emptyList()

                    // Clear existing
                    rankListContainer.removeAllViews()

                    // 상위 3명 podium
                    if (ranks.isNotEmpty()) {
                        tvFirstName.text = "1st • ${ranks.getOrNull(0)?.name ?: "-"}"
                        tvSecondName.text = "2nd • ${ranks.getOrNull(1)?.name ?: "-"}"
                        tvThirdName.text = "3rd • ${ranks.getOrNull(2)?.name ?: "-"}"
                    }

                    // 4등 이후 리스트
                    val inflater = LayoutInflater.from(requireContext())
                    for (i in 3 until ranks.size) {
                        val rank = ranks[i]
                        val tv = TextView(requireContext()).apply {
                            text = "${rank.rank}. ${rank.name} — ${rank.xp} XP"
                            setTextColor(resources.getColor(R.color.text_primary, null))
                            textSize = 13f
                            gravity = Gravity.CENTER
                            setPadding(0, 6, 0, 6)
                        }
                        rankListContainer.addView(tv)
                    }
                } else {
                    Log.e("RankFetch", "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("RankFetch", "Error: ${e.localizedMessage}")
            }
        }
    }

    private fun showDayDetail(history: HistoryDay) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_history_detail, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvDayTitle).text = history.date
        view.findViewById<TextView>(R.id.tvTotalXp).text = history.xp
        view.findViewById<TextView>(R.id.tvCompletion).text = history.percent
        view.findViewById<TextView>(R.id.tvTotalTime).text = history.time

        val container = view.findViewById<LinearLayout>(R.id.exercisedoneListContainer)
        container.removeAllViews()

        history.exercises.forEach { ex ->
            val item = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_exercisedone, container, false)
            item.findViewById<TextView>(R.id.tvExerciseEmoji).text = ex.emoji
            item.findViewById<TextView>(R.id.tvExerciseName).text = ex.name
            item.findViewById<TextView>(R.id.tvExerciseDetails).text = "Completed: ${ex.done}"
            item.findViewById<TextView>(R.id.tvXp).text = ex.xp
            item.findViewById<TextView>(R.id.tvPercent).text = ex.accuracy
            item.findViewById<TextView>(R.id.tvTime).text = ex.duration
            container.addView(item)
        }

        dialog.show()
    }

}