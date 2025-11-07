package com.fitquest.app.ui.fragments

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.model.Exercise
import com.fitquest.app.model.WorkoutPlan
import com.fitquest.app.ui.adapters.ExerciseAdapter
import com.fitquest.app.ui.viewmodels.ScheduleViewModel
import com.google.android.material.button.MaterialButton
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import android.util.Log

/**
 * ScheduleFragment - Screen 2 (TRAINING PLANNER)
 *
 * Quest planning and customization
 * - Calendar with cyan-themed date selector
 * - Quick actions: AI Generate (sparkles icon) / Custom Plan (edit icon)
 * - Exercise arsenal library (grid of exercises with emojis)
 * - Scheduled exercises shown as cards
 * - Save button (green gradient)
 *
 * Design: Strategic planning interface
 * - Deep blue/slate cards
 * - Cyan borders and accents
 * - Green for generation/save actions
 * - Exercise cards with emoji icons
 * - Clean, organized layout
 */
class ScheduleFragment : Fragment(R.layout.fragment_schedule) {

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: ExerciseAdapter
    private lateinit var calendarView: MaterialCalendarView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // === ViewModel, View 초기화 ===
        viewModel = ViewModelProvider(requireActivity())[ScheduleViewModel::class.java]
        recyclerView = view.findViewById(R.id.exercise_recycler_view)
        emptyState = view.findViewById(R.id.emptyState)
        calendarView = view.findViewById(R.id.calendar_view)

        val autoGenerateButton = view.findViewById<MaterialButton>(R.id.auto_generate_button)
        val customPlanButton = view.findViewById<MaterialButton>(R.id.custom_plan_button)


        // ✅ 1️⃣ 화면 진입 시 전체 스케줄 자동 로드
        loadAllSchedules()

        // ✅ 2️⃣ 캘린더 날짜 클릭 시 해당 날짜 스케줄 로드
        calendarView.setOnDateChangedListener { _, date, _ ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selected = sdf.format(date.date)
            viewModel.loadScheduleForDate(selected)
        }

        // ✅ 3️⃣ “AI Generate” 버튼 클릭 시 서버에 생성 요청
        autoGenerateButton.setOnClickListener {
            generateSchedule()
        }

        customPlanButton?.setOnClickListener {
            // TODO: 커스텀 플랜 열기
        }

        // === RecyclerView 세팅 ===
        adapter = ExerciseAdapter { exercise ->
            Toast.makeText(requireContext(), "${exercise.name} 클릭됨", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        // ✅ 4️⃣ ViewModel의 exercises 변화 observe → RecyclerView 업데이트
        viewModel.exercises.observe(viewLifecycleOwner) { exercises ->
            if (exercises.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                adapter.submitList(exercises)
            }
        }
    }

    // === 스케줄 자동 생성 (POST) ===
    private fun generateSchedule() {
        val prefs = requireContext().getSharedPreferences("auth", 0)
        val token = prefs.getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.scheduleApiService.generateUserSchedules("Bearer $token")
                if (response.isSuccessful) {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    viewModel.loadScheduleForDate(today)
                    Toast.makeText(requireContext(), "계획이 생성되었습니다!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "생성 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "네트워크 오류: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // === 전체 스케줄 불러오기 (GET) ===
    private fun loadAllSchedules() {
        val prefs = requireContext().getSharedPreferences("auth", 0)
        val token = prefs.getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.scheduleApiService.getUserSchedules("Bearer $token")
                if (response.isSuccessful) {
                    val result = response.body()
                    if (!result.isNullOrEmpty()) {
                        val workoutPlans = result.map { work ->
                            val exercises = work.exercises.map {
                                Exercise(it.name, it.detail, it.status)
                            }
                            WorkoutPlan(
                                work.id, work.date, exercises,
                                work.isCompleted, work.point, work.feedback,
                                work.startTime, work.finishTime
                            )
                        }

                        viewModel.updateWorkoutPlans(workoutPlans)

                        // 현재 날짜 스케줄 표시
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        viewModel.loadScheduleForDate(today)

                        Toast.makeText(requireContext(), "스케줄을 불러왔습니다!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "스케줄이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "불러오기 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "네트워크 오류: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}