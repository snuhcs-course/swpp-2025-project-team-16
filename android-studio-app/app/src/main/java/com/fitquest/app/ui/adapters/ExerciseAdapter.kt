package com.fitquest.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.model.Exercise

/**
 * Adapter for displaying exercises in the Schedule screen
 */
class ExerciseAdapter(
    private val exercises: List<Exercise>,
    private val onExerciseClick: (Exercise) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emojiText: TextView = view.findViewById(R.id.emoji_text)
        val nameText: TextView = view.findViewById(R.id.name_text)
        val detailsText: TextView = view.findViewById(R.id.details_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        
        holder.emojiText.text = exercise.emoji
        holder.nameText.text = exercise.name
        
        val details = buildString {
            if (exercise.reps != null) append("${exercise.reps} reps")
            if (exercise.duration != null) append("${exercise.duration}s")
            if (exercise.sets != null) append(" × ${exercise.sets} sets")
        }
        holder.detailsText.text = details
        
        holder.itemView.setOnClickListener {
            onExerciseClick(exercise)
        }
    }

    override fun getItemCount() = exercises.size
}
