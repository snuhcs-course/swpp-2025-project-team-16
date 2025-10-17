package com.aisportspt.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aisportspt.app.MainActivity
import com.aisportspt.app.databinding.FragmentLoginPasswordBinding
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import com.aisportspt.app.R


class LoginPasswordFragment : Fragment() {

    private var _binding: FragmentLoginPasswordBinding? = null
    private val binding get() = _binding!!

    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = arguments?.getString(ARG_EMAIL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                binding.btnLogin.apply {
                    isEnabled = hasText
                    setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (hasText) R.color.nav_selected else R.color.achievement_silver
                        )
                    )
                }
            }
        })

        binding.btnLogin.setOnClickListener {
            val password = binding.etPassword.text.toString().trim()
            if (isCorrectPassword(email, password)) {
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "비밀번호가 틀립니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isCorrectPassword(email: String?, password: String): Boolean {
        return email == "admin@test.com" && password == "1234"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EMAIL = "email"

        fun newInstance(email: String): LoginPasswordFragment {
            val fragment = LoginPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            fragment.arguments = args
            return fragment
        }
    }
}
