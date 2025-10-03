package com.aisportspt.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aisportspt.app.R
import com.aisportspt.app.databinding.FragmentLoginEmailBinding

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

        binding.btnConfirm.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "이메일을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isRegisteredEmail(email)) {
                // 비밀번호 입력 화면으로 이동
                val fragment = LoginPasswordFragment.newInstance(email)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.login_container, fragment)
                    .addToBackStack(null)
                    .commit()

            } else {
                if (binding.btnConfirm.text == "확인") {
                    binding.btnConfirm.text = "회원가입"
                    Toast.makeText(requireContext(), "등록되지 않은 이메일입니다. 회원가입을 진행하세요.", Toast.LENGTH_SHORT).show()
                } else {
                    // 회원가입 Step1 화면으로 이동
                    val fragment = SignupStep1Fragment()
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.login_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }


    private fun isRegisteredEmail(email: String): Boolean {
        return email == "admin@test.com" // admin만 성공 처리
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
