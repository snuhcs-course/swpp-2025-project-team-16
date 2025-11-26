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
import com.fitquest.app.databinding.FragmentScheduleBinding
import com.fitquest.app.ui.adapters.ScheduleAdapter
import com.fitquest.app.ui.viewmodels.ScheduleViewModel
import com.fitquest.app.ui.viewmodels.ScheduleViewModelFactory

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(RetrofitClient.scheduleApiService)
    }
    private lateinit var adapter: ScheduleAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentScheduleBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ScheduleAdapter(listOf()) { schedule ->
            val repsTarget = schedule.repsTarget ?: -1
            val durationTarget = schedule.durationTarget ?: -1

            val action = ScheduleFragmentDirections.actionScheduleFragmentToAiCoachFragment(
                scheduleId = schedule.id!!,
                activityKey = schedule.activity.lowercase(),
                repsTarget = repsTarget,
                durationTarget = durationTarget
            )
            findNavController().navigate(action)
        }

        binding.recyclerViewSchedules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSchedules.adapter = adapter

//        binding.btnAutoGenerate.setOnClickListener { viewModel.autoGenerateSchedules() }

        viewModel.schedules.observe(viewLifecycleOwner) { list ->
            val oldListSize = adapter.itemCount // 이전 목록 크기
            adapter.updateList(list)

            // 스크롤 위치 조정 로직
            if (list.size > oldListSize && list.isNotEmpty()) {
                val newIds = viewModel.newlyGeneratedIds.value.orEmpty()
                val firstNewIndex = list.indexOfFirst { it.id in newIds }

                if (firstNewIndex != -1) {
                    binding.recyclerViewSchedules.scrollToPosition(firstNewIndex)
                }
            }
            adapter.updateList(list)
        }

        // 새로 생성된 ID 목록 관찰 및 어댑터에 전달
        viewModel.newlyGeneratedIds.observe(viewLifecycleOwner) { newIds ->
            adapter.setNewlyGeneratedIds(newIds)
        }

        viewModel.getSchedules()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
