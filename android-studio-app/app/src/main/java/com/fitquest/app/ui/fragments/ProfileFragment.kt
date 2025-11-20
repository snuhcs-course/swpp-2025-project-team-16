package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentProfileBinding
import com.fitquest.app.databinding.ItemExercisedoneBinding
import com.fitquest.app.databinding.LayoutHistoryDetailBinding
import com.fitquest.app.model.DailyHistoryItem
import com.fitquest.app.ui.adapters.HistoryAdapter
import com.fitquest.app.ui.viewmodels.HistoryViewModel
import com.fitquest.app.ui.viewmodels.HistoryViewModelFactory
import com.fitquest.app.util.ActivityUtils.getEmoji
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(RetrofitClient.scheduleApiService)
    }

    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var rankOverlay: View
    private lateinit var btnViewRankings: MaterialButton
    private lateinit var rankListContainer: LinearLayout

    // podium (top3)
    private lateinit var tvFirstName: TextView
    private lateinit var tvSecondName: TextView
    private lateinit var tvThirdName: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyAdapter = HistoryAdapter { dailyItem -> showHistoryDetails(dailyItem) }
        binding.recyclerHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerHistory.adapter = historyAdapter

        historyViewModel.dailyHistories.observe(viewLifecycleOwner) { dailyItems ->
            historyAdapter.submitList(dailyItems)
            // RecyclerView가 비었을 때 TextView 보여주기
            binding.tvEmpty.visibility = if (dailyItems.isEmpty()) View.VISIBLE else View.GONE
        }

        historyViewModel.loadPastSchedules()

        // 3. 랭킹 관련 UI 초기화 (기존 findViewById 로직 유지)
        rankOverlay = binding.rankOverlay.root
        btnViewRankings = binding.btnViewRankings
        rankListContainer = rankOverlay.findViewById(R.id.rankListContainer)

        tvFirstName = rankOverlay.findViewById(R.id.tvFirstName)
        tvSecondName = rankOverlay.findViewById(R.id.tvSecondName)
        tvThirdName = rankOverlay.findViewById(R.id.tvThirdName)

        fetchUserStats()
        setupRankButton()
    }

    private fun fetchUserStats() {
        // TODO: 나중에 구현
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

    private fun showHistoryDetails(dailyItem: DailyHistoryItem) {
        val dialog = BottomSheetDialog(requireContext())
        val detailBinding = LayoutHistoryDetailBinding.inflate(layoutInflater)
        dialog.setContentView(detailBinding.root)

        detailBinding.tvDayTitle.text = dailyItem.dateLabel
//        detailBinding.tvTotalXp.text = "+${calculateDailyHistoryTotalEarnedXp(dailyItem.exercises)} XP"
//        detailBinding.tvCompletion.text = "+${calculateDailyHistoryAverageCompletion(dailyItem.exercises)} %"
//        detailBinding.tvTotalTime.text = "+${calculateDailyHistoryTotalDuration(dailyItem.exercises)} min"
//        detailBinding.exercisedoneListContainer.removeAllViews()

        dailyItem.exercises.forEach { ex ->
            val itemBinding = ItemExercisedoneBinding.inflate(layoutInflater)
            itemBinding.tvExerciseEmoji.text = getEmoji(ex.activity)
            itemBinding.tvExerciseName.text = ex.activity
            val text = when {
                ex.repsTarget != null -> "${ex.repsDone} / ${ex.repsTarget} reps"
                ex.durationTarget != null -> "${ex.durationDone} / ${ex.durationTarget} sec"
                else -> ""
            }
            itemBinding.tvExerciseDetails.text = "${ex.status}: ${text}"
//            itemBinding.tvXp.text = "${calculateScheduleEarnedXp(ex)} XP"
//            itemBinding.tvPercent.text = "${calculateScheduleCompletionPercent(ex)} %"
//            itemBinding.tvTime.text = "${calculateScheduleDuration(ex)} min"
//            detailBinding.exercisedoneListContainer.addView(itemBinding.root)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
