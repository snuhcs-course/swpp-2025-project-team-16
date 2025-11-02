package com.fitquest.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.model.WorkoutPlan

/**
 * Adapter for displaying workout plans in the Journey screen
 */
class WorkoutAdapter(
    private val workoutPlans: List<WorkoutPlan>,
    private val onWorkoutClick: (WorkoutPlan) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    class WorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tvWorkoutTime)
        val exerciseCountText: TextView = view.findViewById(R.id.tvExerciseCount)
        val statusText: TextView = view.findViewById(R.id.tvDuration)
        // TODO: Add flag icon, emoji displays, etc.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workoutPlans[position]
        
        holder.dateText.text = workout.date
        holder.exerciseCountText.text = "${workout.exercises.size} exercises"
        holder.statusText.text = if (workout.isCompleted) "✓ Completed" else "Upcoming"
        
        holder.itemView.setOnClickListener {
            onWorkoutClick(workout)
        }
        
        // TODO: Style based on whether it's today, past, or future
        // TODO: Add animations and gamified visuals
    }

    override fun getItemCount() = workoutPlans.size
}
