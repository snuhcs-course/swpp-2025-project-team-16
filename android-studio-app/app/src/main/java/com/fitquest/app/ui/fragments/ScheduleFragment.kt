package com.fitquest.app.ui.fragments

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.ui.adapters.ExerciseAdapter
import com.fitquest.app.ui.viewmodels.ScheduleViewModel
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.util.Locale

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
class ScheduleFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var emptyState:View
    private lateinit var autoGenerateButton: Button
    private lateinit var customPlanButton: Button
    private val viewModel: ScheduleViewModel by viewModels()
    
    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendar_view)

        exerciseRecyclerView = view.findViewById(R.id.exercise_recycler_view)

        autoGenerateButton = view.findViewById(R.id.auto_generate_button)

        customPlanButton = view.findViewById(R.id.custom_plan_button)

        exerciseRecyclerView.layoutManager = LinearLayoutManager(context)

        emptyState=view.findViewById(R.id.emptyState)

        exerciseAdapter= ExerciseAdapter { showExerciseLibrary(selectedDate) }

        exerciseRecyclerView.adapter=exerciseAdapter

        viewModel.exercises.observe(viewLifecycleOwner){list->
            if(list.isNullOrEmpty()){
                exerciseRecyclerView.visibility=View.GONE
                emptyState.visibility=View.VISIBLE
            }else{
                emptyState.visibility=View.GONE
                exerciseRecyclerView.visibility=View.VISIBLE
                exerciseAdapter.submitList(list)
            }
        }

        setUp()

        val cal=Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(cal.time)
        viewModel.loadScheduleForDate(selectedDate)
        calendarView.setOnDateChangedListener { _, date,_ ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            cal.set(date.year,date.month,date.day)
            selectedDate = sdf.format(cal.time)
            viewModel.loadScheduleForDate(selectedDate)
        }

        autoGenerateButton.setOnClickListener {
            generateSchedule()
        }

        customPlanButton.setOnClickListener {
            showExerciseLibrary(selectedDate)
        }


    }



    private fun showExerciseLibrary(selectedDate:String) {
        // TODO: Show dialog/bottom sheet with exercise library
        // User can select exercises to add to schedule
        viewModel.loadScheduleForDate(selectedDate)
        if((viewModel.exercises.value?.isEmpty()) ?: true){
            Toast.makeText(requireContext(), "날짜를 다시 선택해 주세요", Toast.LENGTH_SHORT).show()
            return
        }
        else{
            //TODO
        }
    }
    private fun setUp(){
        val prefs = requireContext().getSharedPreferences("auth", 0)
        val token = prefs.getString("token", null) ?: return
        viewModel.loadAllSchedules(token)
        if(viewModel.message.value!="SUCCESS"){
            Toast.makeText(requireContext(), viewModel.message.value, Toast.LENGTH_SHORT).show()
        }
    }
    private fun generateSchedule(){
        val prefs = requireContext().getSharedPreferences("auth", 0)
        val token = prefs.getString("token", null) ?: return
        viewModel.generateSchedule(token)
        viewModel.loadScheduleForDate(selectedDate)
        if(viewModel.message.value=="SUCCESS"){
            Toast.makeText(requireContext(),"계획이 생성되었습니다!",Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(requireContext(),viewModel.message.value,Toast.LENGTH_SHORT).show()
        }

    }
}
