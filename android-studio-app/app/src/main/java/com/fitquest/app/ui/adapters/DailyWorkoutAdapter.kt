package com.fitquest.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.databinding.ItemJourneyDaynodeBinding
import com.fitquest.app.model.DailyWorkoutItem
import com.fitquest.app.util.ActivityUtils.calculateTotalTargetXp
import com.fitquest.app.util.ActivityUtils.formatActivitiesSummary
import com.fitquest.app.util.DateUtils.formatDate

class DailyWorkoutAdapter(
    private val onItemClick: (DailyWorkoutItem) -> Unit
) : ListAdapter<DailyWorkoutItem, DailyWorkoutAdapter.DailyWorkoutViewHolder>(DiffCallback()) {

    inner class DailyWorkoutViewHolder(private val binding: ItemJourneyDaynodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyWorkoutItem, position: Int) {
            val rightCardBinding = binding.summaryCardRight
            val leftCardBinding = binding.summaryCardLeft

            if (position % 2 == 0) {
                leftCardBinding.root.visibility = View.GONE
                rightCardBinding.root.visibility = View.VISIBLE
            } else {
                leftCardBinding.root.visibility = View.VISIBLE
                rightCardBinding.root.visibility = View.GONE
            }

            val currentCardBinding = if (position % 2 == 0) rightCardBinding else leftCardBinding

            currentCardBinding.tvDate.text = formatDate(item.date)
            currentCardBinding.tvWorkoutSummary.text = formatActivitiesSummary(item.schedules)
            currentCardBinding.tvXp.text = "+${calculateTotalTargetXp(item.schedules)} XP"

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyWorkoutItem>() {
        override fun areItemsTheSame(oldItem: DailyWorkoutItem, newItem: DailyWorkoutItem) =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: DailyWorkoutItem, newItem: DailyWorkoutItem) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyWorkoutViewHolder {
        val binding = ItemJourneyDaynodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DailyWorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyWorkoutViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
}