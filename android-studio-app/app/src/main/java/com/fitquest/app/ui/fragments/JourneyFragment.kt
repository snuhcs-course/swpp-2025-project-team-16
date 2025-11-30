package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentJourneyBinding
import com.fitquest.app.databinding.ItemScheduleBinding
import com.fitquest.app.databinding.LayoutJourneyDaydetailBinding
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.model.Schedule
import com.fitquest.app.ui.adapters.DailyWorkoutAdapter
import com.fitquest.app.ui.viewmodels.JourneyViewModel
import com.fitquest.app.ui.viewmodels.JourneyViewModelFactory
import com.fitquest.app.util.ActivityUtils.getEmoji
import com.fitquest.app.util.ActivityUtils.getLabel
import com.fitquest.app.util.DateUtils.formatDate
import com.fitquest.app.util.DateUtils.formatTime
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

class JourneyFragment() : Fragment() {

    private var _binding: FragmentJourneyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JourneyViewModel by viewModels {
        JourneyViewModelFactory(RetrofitClient.scheduleApiService)
    }

    private lateinit var adapter: DailyWorkoutAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJourneyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DailyWorkoutAdapter { dailyItem -> showScheduleDetails(dailyItem) }
        val layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
            stackFromEnd = false
            reverseLayout = true
        }
        binding.recyclerJourney.layoutManager = layoutManager
        binding.recyclerJourney.adapter = adapter

        viewModel.dailyWorkouts.observe(viewLifecycleOwner) { dailyItems ->
            adapter.submitList(dailyItems)
            binding.tvEmpty.visibility = if (dailyItems.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadUpcomingSchedules()
    }


    private fun showScheduleDetails(dailyItem: DailyWorkoutItem) {
        val dialog = BottomSheetDialog(requireContext())
        val detailBinding = LayoutJourneyDaydetailBinding.inflate(layoutInflater)
        dialog.setContentView(detailBinding.root)

        detailBinding.tvDayTitle.text = formatDate(dailyItem.date)
        detailBinding.exerciseListContainer.removeAllViews()

        val currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        dailyItem.schedules.forEach { schedule ->
            val itemBinding = ItemScheduleBinding.inflate(layoutInflater)
            itemBinding.tvActivityLabel.text = getLabel(schedule.activity)
            itemBinding.tvActivityEmoji.text = getEmoji(schedule.activity)
            itemBinding.tvStartEnd.text = "${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}"
            itemBinding.tvTarget.text = when {
                schedule.repsTarget != null -> "Target: ${schedule.repsTarget} reps"
                schedule.durationTarget != null -> "Target: ${schedule.durationTarget} secs"
                else -> ""
            }

            if (currentTime.isAfter(LocalDateTime.of(schedule.scheduledDate, schedule.startTime)) && currentTime.isBefore(LocalDateTime.of(schedule.scheduledDate, schedule.endTime))) {
                itemBinding.btnStartSession.visibility = View.VISIBLE
                itemBinding.btnStartSession.setOnClickListener {
                    onStartSession(schedule)
                    dialog.dismiss()
                }
            } else {
                itemBinding.btnStartSession.visibility = View.GONE
            }

            detailBinding.exerciseListContainer.addView(itemBinding.root)
        }

        dialog.show()
    }

    private fun onStartSession(schedule: Schedule) {
        val repsTarget = schedule.repsTarget ?: -1
        val durationTarget = schedule.durationTarget ?: -1

        val action = JourneyFragmentDirections.actionJourneyFragmentToAiCoachFragment(
            scheduleId = schedule.id!!,
            activityKey = schedule.activity.lowercase(),
            repsTarget = repsTarget,
            durationTarget = durationTarget)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
