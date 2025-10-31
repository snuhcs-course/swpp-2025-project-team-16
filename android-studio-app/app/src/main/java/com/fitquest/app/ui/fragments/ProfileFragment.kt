package com.fitquest.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.ui.adapters.HistoryAdapter

/**
 * ProfileFragment - Screen 4 (RPG HERO SCREEN)
 * 
 * MAIN FOCUS: Journey Road showing conquest history
 * - Compact stats header at top (level, XP, streak in condensed format)
 * - DOMINANT: Scrollable road with victory markers for completed workouts
 * - Each waypoint shows: date, exercises, XP earned, form score
 * - Glowing achievement path with gold/cyan effects
 * - Character avatar with level badge
 * - Click on workout marker to see full details
 * 
 * Design: RPG character progression screen
 * - Deep blue backgrounds (#0D47A1)
 * - Gold markers for achievements (#FFD700)
 * - Electric cyan accents (#00E5FF)
 * - XP bar with glowing effects
 * - Victory road as primary element (80% of screen)
 */
class ProfileFragment : Fragment() {

    private lateinit var usernameText: TextView
    private lateinit var levelText: TextView
    private lateinit var pointsText: TextView
    private lateinit var streakText: TextView
    private lateinit var totalWorkoutsText: TextView
    private lateinit var journeyDaysText: TextView
    private lateinit var missedDaysText: TextView
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usernameText = view.findViewById(R.id.tvUserName)
        levelText = view.findViewById(R.id.tvLevel)
        pointsText = view.findViewById(R.id.tvTotalXp)
        streakText = view.findViewById(R.id.tvStreak)
//        totalWorkoutsText = view.findViewById(R.id.total_workouts_text)
//        journeyDaysText = view.findViewById(R.id.journey_days_text)
//        missedDaysText = view.findViewById(R.id.missed_days_text)
        historyRecyclerView = view.findViewById(R.id.rvJourneyHistory)

        historyRecyclerView.layoutManager = LinearLayoutManager(context)

        loadUserProfile()
        loadWorkoutHistory()
    }

    private fun loadUserProfile() {
        // TODO: Backend - Fetch user profile data
        // Display user stats in game-like format
        // Calculate level based on points (e.g., level = points / 500)
    }

    private fun loadWorkoutHistory() {
        // TODO: Backend - Fetch workout history
        // Display as scrollable list with date flags
        // Each item shows: date, exercises, points earned, AI score
        
        // historyAdapter = HistoryAdapter(workoutHistory) { workout ->
        //     showWorkoutDetails(workout)
        // }
        // historyRecyclerView.adapter = historyAdapter
    }

    private fun showWorkoutDetails(workoutId: String) {
        // TODO: Show dialog/bottom sheet with detailed workout info
        // - Exercises completed
        // - Points earned
        // - AI feedback
        // - Form score
        // - Time completed
    }

    private fun calculateLevel(points: Int): Int {
        return (points / 500) + 1
    }
}
