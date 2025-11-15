package com.fitquest.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fitquest.app.R
import com.fitquest.app.databinding.ItemScheduleBinding
import com.fitquest.app.model.Schedule
import com.fitquest.app.util.ActivityUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

class ScheduleAdapter(
    private var schedules: List<Schedule>,
    private val onStartSession: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    // 1. 새로 생성된 스케줄 ID를 저장할 변수 추가
    private var newlyGeneratedIds: Set<Int> = emptySet()

    inner class ScheduleViewHolder(val binding: ItemScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: Schedule) {
            binding.tvActivityLabel.text = ActivityUtils.getLabel(schedule.activity)
            binding.tvActivityEmoji.text = ActivityUtils.getEmoji(schedule.activity)

            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val formattedDate = try {
                // ... (기존 날짜/시간 포매팅 로직 유지)
                LocalDate.parse(schedule.scheduledDate).format(dateFormatter)
            } catch (e: Exception) { "-- --" }

            val formattedStartTime = try {
                LocalTime.parse(schedule.startTime).format(timeFormatter)
            } catch (e: Exception) { "--:--" }

            val formattedEndTime = try {
                LocalTime.parse(schedule.endTime).format(timeFormatter)
            } catch (e: Exception) { "--:--" }

            binding.tvStartEnd.text = binding.root.context.getString(
                R.string.schedule_time,
                formattedDate,
                formattedStartTime,
                formattedEndTime
            )

            binding.tvTarget.text = when {
                schedule.repsTarget != null -> "Target: ${schedule.repsTarget} reps"
                schedule.durationTarget != null -> "Target: ${schedule.durationTarget} secs"
                else -> ""
            }

            binding.btnStartSession.setOnClickListener { onStartSession(schedule) }

            // 테두리 강조 로직
            val context = binding.root.context
            val isNew = newlyGeneratedIds.contains(schedule.id)

            // MaterialCardView를 사용한다고 가정
            if (isNew) {
                // 새로 생성된 경우: 테두리 강조 (색상, 굵기)
                val highlightColor = ContextCompat.getColor(context, R.color.cyan)
                val highlightBg = ContextCompat.getColor(context, R.color.highlight_new_schedule)

                binding.cardViewRoot.setCardBackgroundColor(highlightBg)
                binding.cardViewRoot.setStrokeColor(highlightColor) // ✅ setStrokeColor() 사용
                binding.cardViewRoot.setStrokeWidth(3)             // ✅ setStrokeWidth() 사용

            } else {
                // 기본 상태: 원래 색상 및 테두리로 복원
                val normalBg = ContextCompat.getColor(context, R.color.surface_dark_translucent)
                val normalStroke = ContextCompat.getColor(context, R.color.cyan_20)

                binding.cardViewRoot.setCardBackgroundColor(normalBg)
                binding.cardViewRoot.setStrokeColor(normalStroke) // ✅ setStrokeColor() 사용
                binding.cardViewRoot.setStrokeWidth(1)           // ✅ setStrokeWidth() 사용
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ScheduleViewHolder(
            ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) =
        holder.bind(schedules[position])

    override fun getItemCount() = schedules.size

    fun updateList(newList: List<Schedule>) {
        schedules = newList
        notifyDataSetChanged()
    }

    // 3. 새로 생성된 ID 목록을 업데이트하는 함수 추가
    fun setNewlyGeneratedIds(newIds: Set<Int>) {
        val oldIds = this.newlyGeneratedIds
        this.newlyGeneratedIds = newIds

        // 성능 향상을 위해 변경된 아이템만 업데이트
        val changedIds = oldIds.union(newIds) // 이전과 현재 ID 모두 확인
        schedules.forEachIndexed { index, schedule ->
            if (schedule.id in changedIds) {
                // ID가 새로 추가/제거된 항목만 리바인딩
                notifyItemChanged(index)
            }
        }
    }
}