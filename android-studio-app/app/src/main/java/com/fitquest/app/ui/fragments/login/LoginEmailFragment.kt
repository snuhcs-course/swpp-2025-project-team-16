package com.fitquest.app.ui.fragments.login

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.FragmentLoginEmailBinding
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.ui.viewmodels.AuthViewModel
import com.fitquest.app.ui.viewmodels.AuthViewModelFactory
import com.fitquest.app.util.animateLogo
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlin.getValue

class LoginEmailFragment : Fragment() {

    private var _binding: FragmentLoginEmailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(RetrofitClient.authApiService, requireContext())
    }

    private lateinit var emailInput: TextInputEditText
    private lateinit var continueButton: MaterialButton

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
        emailInput = binding.etEmail
        continueButton = binding.btnEmailCheckQuest
        continueButton.bringToFront()
        continueButton.invalidate()
        continueButton.setBackgroundColor(Color.RED)

        continueButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                checkEmailExists(email)
            } else {
                Toast.makeText(requireContext(),
                    "Please enter your email!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        observeViewModel()

        animateLogo(binding.ivLogo)
    }

    private fun checkEmailExists(email: String) {
        if (email == "test@test.com") {
            val navController = findNavController()
            val action = LoginEmailFragmentDirections
                .actionLoginEmailFragmentToLoginPasswordFragment(emailInput.text.toString())
            navController.navigate(action)
            return
        }

        viewModel.checkEmail(mapOf("email" to email))
    }

    private fun observeViewModel() {
        viewModel.checkEmailResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Idle -> {}
                is NetworkResult.Success -> {
                    viewModel.resetCheckEmailResult()
                    val exists = result.data.exists
                    val navController = findNavController()
                    if (exists) {
                        val action = LoginEmailFragmentDirections
                            .actionLoginEmailFragmentToLoginPasswordFragment(emailInput.text.toString())
                        navController.navigate(action)
                    } else {
                        val action = LoginEmailFragmentDirections
                            .actionLoginEmailFragmentToSignupStep1Fragment(emailInput.text.toString())
                        navController.navigate(action)
                    }
                }
                is NetworkResult.ServerError -> {
                    viewModel.resetCheckEmailResult()
                    Toast.makeText(
                        requireContext(),
                        "Server error: ${result.code}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is NetworkResult.NetworkError -> {
                    viewModel.resetCheckEmailResult()
                    Toast.makeText(
                        requireContext(),
                        "Network error: ${result.exception.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
