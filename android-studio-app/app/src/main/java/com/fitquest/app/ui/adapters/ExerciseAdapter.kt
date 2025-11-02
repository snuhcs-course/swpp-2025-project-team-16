package com.fitquest.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.model.Exercise

/**
 * Adapter for displaying exercises in the Schedule screen
 */
class ExerciseAdapter(
    private val onExerciseClick: (Exercise) -> Unit
) : ListAdapter<Exercise,ExerciseAdapter.ExerciseViewHolder>(DiffCallback()) {
    class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emojiText: TextView = view.findViewById(R.id.tvExerciseEmoji)
        val nameText: TextView = view.findViewById(R.id.tvExerciseName)
        val detailsText: TextView = view.findViewById(R.id.tvExerciseDetails)
        val progressText: TextView=view.findViewById(R.id.tvProgressText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = getItem(position)
        
        holder.emojiText.text = exercise.emoji
        holder.nameText.text = exercise.name

        holder.detailsText.text = exercise.detail
        holder.progressText.text=exercise.status
        holder.itemView.setOnClickListener {
            onExerciseClick(exercise)
        }
    }

    override fun getItemCount() = currentList.size
    class DiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem== newItem
    }

}
