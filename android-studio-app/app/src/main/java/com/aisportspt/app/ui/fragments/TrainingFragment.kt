package com.aisportspt.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.aisportspt.app.MainActivity
import com.aisportspt.app.R
import com.aisportspt.app.databinding.FragmentTrainingBinding
import com.aisportspt.app.model.TrainingPlan
import com.aisportspt.app.model.Exercise
import com.aisportspt.app.model.Difficulty
import com.aisportspt.app.ui.adapters.TrainingPlanAdapter
import com.aisportspt.app.ui.dialogs.CreateTrainingPlanDialogFragment

class TrainingFragment : Fragment() {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var trainingPlanAdapter: TrainingPlanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupUI()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        trainingPlanAdapter = TrainingPlanAdapter(
            onStartTraining = { plan ->
                startTrainingPlan(plan)
            },
            onViewDetails = { plan ->
                showTrainingPlanDetails(plan)
            }
        )
        
        binding.recyclerViewPlans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trainingPlanAdapter
        }
    }

    private fun setupUI() {
        binding.btnGenerateAiPlan.setOnClickListener {
            showCreateTrainingPlanDialog()
        }
        
        binding.btnCreateFirstPlan.setOnClickListener {
            showCreateTrainingPlanDialog()
        }
    }

    private fun observeViewModel() {
        val viewModel = (requireActivity() as MainActivity).getViewModel()
        
        viewModel.trainingPlans.observe(viewLifecycleOwner, Observer { plans ->
            updateUI(plans)
        })
        
        // Generate sample training plans if empty
        if (viewModel.trainingPlans.value.isNullOrEmpty()) {
            generateSampleTrainingPlans(viewModel)
        }
    }

    private fun updateUI(plans: List<TrainingPlan>) {
        if (plans.isEmpty()) {
            binding.recyclerViewPlans.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewPlans.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
            trainingPlanAdapter.updatePlans(plans)
        }
    }

    private fun showCreateTrainingPlanDialog() {
        val dialog = CreateTrainingPlanDialogFragment { plan ->
            val viewModel = (requireActivity() as MainActivity).getViewModel()
            viewModel.addTrainingPlan(plan)
        }
        dialog.show(parentFragmentManager, "CreateTrainingPlanDialog")
    }

    private fun startTrainingPlan(plan: TrainingPlan) {
        // TODO: Navigate to training execution screen
        // For now, show a simple confirmation
    }

    private fun showTrainingPlanDetails(plan: TrainingPlan) {
        // TODO: Show detailed view of training plan
        // This could be a dialog or a new fragment
    }

    private fun generateSampleTrainingPlans(viewModel: com.aisportspt.app.ui.viewmodels.MainViewModel) {
        val samplePlans = listOf(
            TrainingPlan(
                id = "1",
                sport = "골프",
                name = "골프 기초 향상 프로그램",
                duration = "45분",
                difficulty = Difficulty.INTERMEDIATE,
                focus = "스윙 정확도, 거리 향상",
                exercises = listOf(
                    Exercise(
                        id = "1",
                        name = "어드레스 자세 연습",
                        duration = "10분",
                        description = "올바른 어드레스 자세 반복 연습",
                        videoUrl = null
                    ),
                    Exercise(
                        id = "2",
                        name = "백스윙 연습",
                        duration = "15분",
                        description = "정확한 백스윙 궤도 연습",
                        videoUrl = null
                    ),
                    Exercise(
                        id = "3",
                        name = "다운스윙 연습",
                        duration = "15분",
                        description = "파워와 정확도를 위한 다운스윙",
                        videoUrl = null
                    ),
                    Exercise(
                        id = "4",
                        name = "팔로우스루 연습",
                        duration = "5분",
                        description = "완벽한 피니시 자세 연습",
                        videoUrl = null
                    )
                ),
                aiGenerated = true
            ),
            TrainingPlan(
                id = "2",
                sport = "볼링",
                name = "볼링 스코어 향상 훈련",
                duration = "30분",
                difficulty = Difficulty.BEGINNER,
                focus = "스페어 처리, 일관된 릴리즈",
                exercises = listOf(
                    Exercise(
                        id = "5",
                        name = "어프로치 연습",
                        duration = "10분",
                        description = "4스텝 어프로치 동작 연습",
                        videoUrl = null
                    ),
                    Exercise(
                        id = "6",
                        name = "릴리즈 포인트 연습",
                        duration = "10분",
                        description = "일관된 릴리즈를 위한 연습",
                        videoUrl = null
                    ),
                    Exercise(
                        id = "7",
                        name = "스페어 연습",
                        duration = "10분",
                        description = "다양한 스페어 상황 연습",
                        videoUrl = null
                    )
                ),
                aiGenerated = true
            )
        )
        
        samplePlans.forEach { plan ->
            viewModel.addTrainingPlan(plan)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}