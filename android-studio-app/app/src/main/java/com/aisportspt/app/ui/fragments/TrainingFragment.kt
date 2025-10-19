package com.aisportspt.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aisportspt.app.databinding.FragmentTrainingBinding
import com.aisportspt.app.ui.dialogs.CreateTrainingPlanDialogFragment
import com.aisportspt.app.ui.dialogs.ModifyDateDialogFragment
import com.aisportspt.app.ui.dialogs.ModifyPlanDialogFragment
import java.text.SimpleDateFormat
import java.util.*
class TrainingFragment : Fragment() {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!

    // 샘플 데이터
    private val workoutDates = HashSet<String>()
    private var achievementRate = 68

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        val view = binding.root

        setupMockData()
        setupClickListeners()
        updateUI()

        return view
    }

    private fun setupMockData() {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        cal.set(2025, Calendar.OCTOBER, 5)
        workoutDates.add(sdf.format(cal.time))
        cal.set(2025, Calendar.OCTOBER, 8)
        workoutDates.add(sdf.format(cal.time))
        cal.set(2025, Calendar.OCTOBER, 12)
        workoutDates.add(sdf.format(cal.time))
    }

    private fun setupClickListeners() {
        binding.btnCreatePlan.setOnClickListener {
            CreateTrainingPlanDialogFragment()
                .show(childFragmentManager, "CreatePlanDialog")
        }

        binding.btnEditPlan.setOnClickListener {
            ModifyDateDialogFragment { selectedDate ->
                // 날짜 선택 완료되면 → 시간대 다이얼로그 실행
                ModifyPlanDialogFragment { selectedDateFinal, selectedTimeSlot ->
                    // TODO :
                }.show(childFragmentManager, "ModifyPlanDialog")
            }.show(childFragmentManager, "ModifyDateDialog")
        }



        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDateStr = sdf.format(selectedDate.time)

            if (workoutDates.contains(selectedDateStr)) {
                // TODO: 선택된 날짜에 훈련 계획이 있으면 상세보기 열기
            }
        }
    }

    private fun updateUI() {
        // TODO:
        val monthFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREAN)

        binding.progressAchievement.progress = achievementRate
        binding.tvAchievementPercentage.text = "$achievementRate%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
