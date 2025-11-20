package com.fitquest.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.databinding.ItemHistorynodeBinding
import com.fitquest.app.model.DailyHistoryItem

class HistoryAdapter(
    private val onItemClick: (DailyHistoryItem) -> Unit
) : ListAdapter<DailyHistoryItem, HistoryAdapter.HistoryViewHolder>(DiffCallback()) {

    inner class HistoryViewHolder(private val binding: ItemHistorynodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dailyItem: DailyHistoryItem, position: Int) {

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

            currentCardBinding.tvDate.text = dailyItem.dateLabel
            //currentCardBinding.tvWorkoutSummary.text = formatExercisesSummary(dailyItem)
            //currentCardBinding.tvXp.text = "+${calculateDailyHistoryTotalEarnedXp(dailyItem.exercises)} XP"
            //currentCardBinding.tvPercent.text = "${calculateDailyHistoryAverageCompletion(dailyItem.exercises)} %"
            //currentCardBinding.tvTime.text = "${calculateDailyHistoryTotalDuration(dailyItem.exercises)} min"

            binding.root.setOnClickListener { onItemClick(dailyItem) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyHistoryItem>() {
        override fun areItemsTheSame(oldItem: DailyHistoryItem, newItem: DailyHistoryItem) =
            oldItem.dateLabel == newItem.dateLabel

        override fun areContentsTheSame(oldItem: DailyHistoryItem, newItem: DailyHistoryItem) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistorynodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
}
