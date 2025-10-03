package com.aisportspt.app.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.aisportspt.app.databinding.DialogModifyPlanBinding

class ModifyPlanDialogFragment(
    private val onPlanModified: (String, String) -> Unit // (날짜, 시간대)
) : DialogFragment() {

    private var _binding: DialogModifyPlanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogModifyPlanBinding.inflate(layoutInflater)

        // 🔹 지금은 기능 구현 안 하고 버튼만 연결
        binding.btnBack.setOnClickListener {
            dismiss() // 뒤로가기 누르면 닫기
        }

        binding.btnCompleteEdit.setOnClickListener {
            // TODO: CalendarView에서 날짜 가져오기
            // TODO: RadioGroup에서 시간대 가져오기
            // 지금은 그냥 콜백 호출만!
            onPlanModified("2025-10-03", "오전 (6:00 - 12:00)")

            Toast.makeText(requireContext(), "계획이 수정되었습니다!", Toast.LENGTH_SHORT).show()

            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
