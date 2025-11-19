package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitquest.app.databinding.FragmentJourneyBinding
import com.fitquest.app.databinding.ItemExerciseBinding
import com.fitquest.app.databinding.LayoutJourneyDaydetailBinding
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.repository.ScheduleRepository
import com.fitquest.app.ui.adapters.DailyWorkoutAdapter
import com.fitquest.app.ui.viewmodels.JourneyViewModel
import com.fitquest.app.ui.viewmodels.JourneyViewModelFactory
import com.fitquest.app.util.ActivityUtils.getEmoji
import com.google.android.material.bottomsheet.BottomSheetDialog

class JourneyFragment : Fragment() {

    private var _binding: FragmentJourneyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JourneyViewModel by viewModels {
        JourneyViewModelFactory(ScheduleRepository())
    }

    private lateinit var adapter: DailyWorkoutAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJourneyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DailyWorkoutAdapter { dailyItem -> showWorkoutDetails(dailyItem) }
        binding.recyclerJourney.layoutManager = LinearLayoutManager(context)
        binding.recyclerJourney.adapter = adapter

        viewModel.dailyWorkouts.observe(viewLifecycleOwner) { dailyItems ->
            adapter.submitList(dailyItems)
            // RecyclerView가 비었을 때 TextView 보여주기
            binding.tvEmpty.visibility = if (dailyItems.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadUpcomingSchedules()
    }


    private fun showWorkoutDetails(dailyItem: DailyWorkoutItem) {
        val dialog = BottomSheetDialog(requireContext())
        val detailBinding = LayoutJourneyDaydetailBinding.inflate(layoutInflater)
        dialog.setContentView(detailBinding.root)

        detailBinding.tvDayTitle.text = dailyItem.dateLabel
        detailBinding.exerciseListContainer.removeAllViews()

        dailyItem.exercises.forEach { ex ->
            val itemBinding = ItemExerciseBinding.inflate(layoutInflater)
            itemBinding.tvExerciseEmoji.text = getEmoji(ex.name)
            itemBinding.tvExerciseName.text = ex.name
            itemBinding.tvProgressText.text = ex.status
            itemBinding.tvExerciseDetails.text = when {
                ex.targetCount != null -> "${ex.targetCount} reps"
                ex.targetDuration != null -> "${ex.targetDuration} sec"
                else -> ""
            }
            detailBinding.exerciseListContainer.addView(itemBinding.root)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
