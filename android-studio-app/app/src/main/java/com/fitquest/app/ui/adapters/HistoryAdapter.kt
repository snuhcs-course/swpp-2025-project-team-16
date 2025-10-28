package com.fitquest.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.model.WorkoutHistory

/**
 * Adapter for displaying workout history in the Profile screen
 */
class HistoryAdapter(
    private val workoutHistory: List<WorkoutHistory>,
    private val onHistoryClick: (WorkoutHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.date_text)
        val pointsText: TextView = view.findViewById(R.id.points_text)
        val scoreText: TextView = view.findViewById(R.id.score_text)
        val exerciseCountText: TextView = view.findViewById(R.id.exercise_count_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = workoutHistory[position]
        
        holder.dateText.text = history.date
        holder.pointsText.text = "${history.points} pts"
        holder.scoreText.text = "${history.aiScore}%"
        holder.exerciseCountText.text = "${history.exercises.size} exercises"
        
        holder.itemView.setOnClickListener {
            onHistoryClick(history)
        }
        
        // TODO: Add visual indicators (flag icons, badges)
        // TODO: Display emojis from exercises
    }

    override fun getItemCount() = workoutHistory.size
}
