package com.aisportspt.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aisportspt.app.MainActivity
import com.aisportspt.app.databinding.FragmentSignupStep3Binding
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aisportspt.app.R
import com.aisportspt.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch


class SignupStep3Fragment : Fragment() {

    private var _binding: FragmentSignupStep3Binding? = null
    private val binding get() = _binding!!

    private var name: String? = null
    private var email: String? = null
    private var password: String? = null
    private var sports: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(ARG_NAME)
            email = it.getString(ARG_EMAIL)
            password = it.getString(ARG_PASSWORD)
            sports = it.getStringArrayList(ARG_SPORTS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 라디오 버튼 선택에 따라 버튼 활성화
        binding.rgLevel.setOnCheckedChangeListener { _, checkedId ->
            val selected = checkedId != -1
            binding.btnComplete.apply {
                isEnabled = selected
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (selected) R.color.nav_selected else R.color.achievement_silver
                    )
                )
            }
        }

        // 🔹 회원가입 완료 버튼 클릭
        binding.btnComplete.setOnClickListener {
            val level = when (binding.rgLevel.checkedRadioButtonId) {
                binding.rbBeginner.id -> "초급"
                binding.rbIntermediate.id -> "중급"
                binding.rbAdvanced.id -> "고급"
                else -> ""
            }

            signupAccount(level)
        }
    }

    // ✅ 서버로 계정 정보 전송
    private fun signupAccount(level: String) {
        val request = mapOf(
            "name" to (name ?: ""),
            "email" to (email ?: ""),
            "password" to (password ?: "")
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.signup(request)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "회원가입 완료!", Toast.LENGTH_SHORT).show()

                    // TODO: 나중에 스포츠/레벨 등 추가 정보 저장 API 호출
                    // 예시: RetrofitClient.apiService.saveProfile(level, sports)

                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "회원가입 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"
        private const val ARG_SPORTS = "sports"

        fun newInstance(name: String, email: String, password: String, sports: ArrayList<String>) =
            SignupStep3Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putString(ARG_EMAIL, email)
                    putString(ARG_PASSWORD, password)
                    putStringArrayList(ARG_SPORTS, sports)
                }
            }
    }
}