package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.ui.adapters.WorkoutAdapter

/**
 * JourneyFragment - Screen 1 (QUEST PATH)
 * 
 * Displays upcoming quest path with waypoint markers
 * - Scrollable view showing future workout dates
 * - Glowing cyan path connecting waypoints
 * - Today's quest prominently highlighted with glow effect
 * - Motivational warrior-themed message at top
 * - Click on waypoint to see exercise details
 * 
 * Design: RPG quest map
 * - Deep blue gradient background
 * - Cyan glowing path line
 * - Gold/cyan waypoint markers
 * - Today's marker pulses with electric cyan
 * - Slate/blue card for quest details
 */
class JourneyFragment : Fragment() {

    private lateinit var workoutRecyclerView: RecyclerView
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_journey, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workoutRecyclerView = view.findViewById(R.id.workout_recycler_view)
        workoutRecyclerView.layoutManager = LinearLayoutManager(context)

        // TODO: Fetch workout plans from backend
        // workoutAdapter = WorkoutAdapter(workoutPlans)
        // workoutRecyclerView.adapter = workoutAdapter

        loadWorkoutPlans()
    }

    private fun loadWorkoutPlans() {
        // TODO: Backend - Fetch upcoming workout plans
        // Display in RecyclerView with flag icons
        // Show motivational message based on user's progress
    }

    private fun showWorkoutDetails(date: String) {
        // TODO: Show dialog/bottom sheet with exercise details for selected date
    }
}
