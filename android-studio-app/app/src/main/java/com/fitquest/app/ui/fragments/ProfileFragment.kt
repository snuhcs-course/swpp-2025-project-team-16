package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.ServiceLocator
import com.fitquest.app.data.remote.UserApiService
import com.fitquest.app.databinding.FragmentProfileBinding
import com.fitquest.app.databinding.ItemExercisedoneBinding
import com.fitquest.app.databinding.LayoutHistoryDetailBinding
import com.fitquest.app.model.DailyHistoryItem
import com.fitquest.app.model.Schedule
import com.fitquest.app.model.Session
import com.fitquest.app.ui.adapters.HistoryAdapter
import com.fitquest.app.ui.viewmodels.HistoryViewModel
import com.fitquest.app.ui.viewmodels.HistoryViewModelFactory
import com.fitquest.app.ui.viewmodels.UserViewModel
import com.fitquest.app.ui.viewmodels.UserViewModelFactory
import com.fitquest.app.util.ActivityUtils.calculateAverageCompletionPercent
import com.fitquest.app.util.ActivityUtils.calculateCompletionPercent
import com.fitquest.app.util.ActivityUtils.calculateEarnedXpForSchedule
import com.fitquest.app.util.ActivityUtils.calculateEarnedXpForSession
import com.fitquest.app.util.ActivityUtils.calculateTotalEarnedXp
import com.fitquest.app.util.ActivityUtils.getEmoji
import com.fitquest.app.util.DateUtils.formatDate
import com.fitquest.app.util.DateUtils.formatTotalTime
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.LocalTime

class ProfileFragment() : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(ServiceLocator.dailySummaryApiService, ServiceLocator.scheduleApiService,
            ServiceLocator.sessionApiService)
    }

    private val userViewModel: UserViewModel by viewModels {
        UserViewModelFactory(ServiceLocator.userApiService)
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
            binding.tvEmpty.visibility = if (dailyItems.isEmpty()) View.VISIBLE else View.GONE
        }
        historyViewModel.loadHistory()

        // 3. 랭킹 관련 UI 초기화 (기존 findViewById 로직 유지)
        rankOverlay = binding.rankOverlay.root
        btnViewRankings = binding.btnViewRankings
        rankListContainer = rankOverlay.findViewById(R.id.rankListContainer)

        tvFirstName = rankOverlay.findViewById(R.id.tvFirstName)
        tvSecondName = rankOverlay.findViewById(R.id.tvSecondName)
        tvThirdName = rankOverlay.findViewById(R.id.tvThirdName)

        setupRankButton()
        observeUserData()

        userViewModel.getProfile()
        userViewModel.getRankings()
    }

    private fun observeUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.userProfile.collectLatest { user ->
                user?.let {
                    binding.statLevel.tvStatRank.text = it.rank.toString()
                    binding.statLevel.tvStatLevel.text = LevelUtils.calculateLevel(it.xp).toString()
                    binding.statLevel.tvStatTime.text = formatTotalTime(it.totalTime)
                    binding.statLevel.tvStatXP.text = it.xp.toString()

                    val (current, max) = LevelUtils.levelProgress(it.xp)
                    val percent = (current * 100 / max)

                    binding.progressLevel.progress = percent
                    binding.tvLevelProgress.text = "$percent%"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.rankings.collectLatest { ranks ->
                rankListContainer.removeAllViews()

                tvFirstName.text = "1st • ${ranks.getOrNull(0)?.name ?: "-"}"
                tvSecondName.text = "2nd • ${ranks.getOrNull(1)?.name ?: "-"}"
                tvThirdName.text = "3rd • ${ranks.getOrNull(2)?.name ?: "-"}"

                ranks.drop(3).forEachIndexed { index, rank ->
                    val tv = TextView(requireContext()).apply {
                        text = "${index + 4}. ${rank.name} — ${rank.xp} XP"
                        setTextColor(resources.getColor(R.color.text_primary, null))
                        textSize = 13f
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 6, 0, 6)
                    }
                    rankListContainer.addView(tv)
                }
            }
        }
    }

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

    private fun showHistoryDetails(dailyItem: DailyHistoryItem) {
        val dialog = BottomSheetDialog(requireContext())
        val detailBinding = LayoutHistoryDetailBinding.inflate(layoutInflater)
        dialog.setContentView(detailBinding.root)

        detailBinding.tvDayTitle.text = formatDate(dailyItem.date)
        detailBinding.tvDailySummary.text = dailyItem.summaryText
        detailBinding.tvTotalXp.text = "+${calculateTotalEarnedXp(dailyItem.schedules, dailyItem.sessions)}"
//        detailBinding.tvCompletion.text = "+${calculateAverageCompletionPercent(dailyItem.schedules)}"
        detailBinding.exercisedoneListContainer.removeAllViews()

        val combinedItems = (dailyItem.schedules.map { it as Any } + dailyItem.sessions.map { it as Any })
            .sortedBy {
                when(it) {
                    is Schedule -> it.startTime
                    is Session -> it.createdAt?.toLocalTime() ?: LocalTime.MIN
                    else -> LocalTime.MIN
                }
            }

        combinedItems.forEach { item ->
            val itemBinding = ItemExercisedoneBinding.inflate(layoutInflater)
            when(item) {
                is Schedule -> {
                    itemBinding.tvExerciseEmoji.text = getEmoji(item.activity)
                    itemBinding.tvExerciseName.text = item.activity
                    val text = when {
                        item.repsTarget != null -> "${item.repsDone} / ${item.repsTarget} reps"
                        item.durationTarget != null -> "${item.durationDone} / ${item.durationTarget} sec"
                        else -> ""
                    }
                    itemBinding.tvExerciseDetails.text = "${item.status}: $text"
                    itemBinding.tvXp.text = "${calculateEarnedXpForSchedule(item)} XP"
                    itemBinding.tvPercent.text = "${calculateCompletionPercent(item)} %"
                }
                is Session -> {
                    itemBinding.tvExerciseEmoji.text = getEmoji(item.activity)
                    itemBinding.tvExerciseName.text = item.activity
                    itemBinding.tvExerciseDetails.text = when {
                        item.repsCount != null -> "${item.repsCount} reps"
                        item.duration != null -> "${item.duration} sec"
                        else -> ""
                    }
                    itemBinding.tvXp.text = "${calculateEarnedXpForSession(item)} XP"
                    itemBinding.tvPercent.visibility = View.GONE
                }
            }
            detailBinding.exercisedoneListContainer.addView(itemBinding.root)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    object LevelUtils {

        // 1~10 레벨까지의 누적 XP 테이블
        private val levelTable = listOf(
            0,    // Lv1
            100,  // Lv2
            300,  // Lv3
            600,  // Lv4
            1000, // Lv5
            1500, // Lv6
            2100, // Lv7
            2800, // Lv8
            3600, // Lv9
            4500  // Lv10
        )

        fun calculateLevel(xp: Int): Int {
            for (i in levelTable.indices) {
                if (xp < levelTable[i]) {
                    return i   // index = level-1
                }
            }

            var level = levelTable.size // 즉 10
            var requiredXp = levelTable.last() // 4500부터 시작

            while (true) {
                val nextRequired = requiredXp + (level * 100)
                if (xp < nextRequired) {
                    return level
                }
                requiredXp = nextRequired
                level++
            }
        }

        fun levelProgress(xp: Int): Pair<Int, Int> {
            var level = 1
            var requiredXp = 0

            while (true) {
                val next = requiredXp + (level * 100)
                if (xp < next) {
                    return Pair(xp - requiredXp, next - requiredXp)
                }
                requiredXp = next
                level++
            }
        }
    }


}
