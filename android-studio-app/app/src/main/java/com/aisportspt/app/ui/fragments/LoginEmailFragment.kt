package com.aisportspt.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aisportspt.app.R
import com.aisportspt.app.databinding.FragmentLoginEmailBinding
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.aisportspt.app.data.remote.EmailCheckResponse
import com.aisportspt.app.data.remote.RetrofitClient

class LoginEmailFragment : Fragment() {

    private var _binding: FragmentLoginEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                binding.btnConfirm.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (hasText) R.color.nav_selected else R.color.achievement_silver
                    )
                )
            }
        })

        // 🔹 버튼 클릭 시 이메일 서버로 확인
        binding.btnConfirm.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "이메일을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkEmailFromServer(email)
        }
    }

    // ✅ 서버로 이메일 존재 여부 확인
    private fun checkEmailFromServer(email: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.checkEmail(mapOf("email" to email))
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.exists == true) {
                        goToPasswordFragment(email)
                    } else {
                        handleSignupButton()
                    }
                } else {
                    Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ 로그인 비밀번호 화면으로 이동
    private fun goToPasswordFragment(email: String) {
        val fragment = LoginPasswordFragment.newInstance(email)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.login_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // ✅ 회원가입 화면으로 이동
    private fun handleSignupButton() {
        if (binding.btnConfirm.text == "확인") {
            binding.btnConfirm.text = "회원가입"
            Toast.makeText(requireContext(), "등록되지 않은 이메일입니다. 회원가입을 진행하세요.", Toast.LENGTH_SHORT).show()
        } else {
            val fragment = SignupStep1Fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.login_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
