package com.fitquest.app.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.TokenManager
import com.fitquest.app.databinding.FragmentSignupStep1Binding
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.login.SignupRequest
import com.fitquest.app.ui.viewmodels.AuthViewModel
import com.fitquest.app.ui.viewmodels.AuthViewModelFactory
import kotlin.getValue
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fitquest.app.data.remote.ServiceLocator
import com.fitquest.app.util.animateLogo

class SignupStep1Fragment : Fragment() {

    private var _binding: FragmentSignupStep1Binding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(ServiceLocator.authApiService, requireContext())
    }

    private val args: SignupStep1FragmentArgs by navArgs()
    private var email: String = ""
    private var inputEmail: String = ""
    private var inputName: String = ""
    private var inputPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = args.email
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEmail.setText(email)

        binding.btnContinue.setOnClickListener {
            if (validateInputs()) signupUser()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        observeSignupResult()

        animateLogo(binding.ivLogo)
    }

    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString()
        val username = binding.etHeroName.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        return when {
            email.isEmpty() -> {
                Toast.makeText(requireContext(),
                "Email is required",
                Toast.LENGTH_SHORT
            ).show(); false }
            username.isEmpty() -> {
                Toast.makeText(requireContext(),
                "Username is required",
                Toast.LENGTH_SHORT
            ).show(); false }
            password.isEmpty() -> {
                Toast.makeText(requireContext(),
                    "Password is required",
                    Toast.LENGTH_SHORT
            ).show(); false }
            password.length < 6 -> {
                Toast.makeText(requireContext(),
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show(); false }
            confirmPassword.isEmpty() -> {
                Toast.makeText(requireContext(),
                    "Confirm Password is required",
                    Toast.LENGTH_SHORT
                ).show(); false }
            password != confirmPassword -> {
                Toast.makeText(requireContext(),
                    "Passwords do not match",
                    Toast.LENGTH_SHORT
                ).show(); false }
            else -> true
        }
    }

    private fun signupUser() {
        inputEmail = binding.etEmail.text.toString().trim()
        inputName = binding.etHeroName.text.toString().trim()
        inputPassword = binding.etPassword.text.toString().trim()

        authViewModel.signup(SignupRequest(name = inputName, email = inputEmail, password = inputPassword))
    }

    private fun observeSignupResult() {
        authViewModel.signupResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Idle -> {}
                is NetworkResult.Success -> {
                    authViewModel.resetSignupResult()
                    val body = result.data
                    TokenManager.saveToken(requireContext(), body.token ?: "", inputEmail, inputName)
                    Toast.makeText(requireContext(), body.message, Toast.LENGTH_SHORT).show()
                    val action = SignupStep1FragmentDirections
                        .actionSignupStep1FragmentToSignupStep2Fragment(
                            email = inputEmail,
                            password = inputPassword,
                            username = inputName
                        )
                    findNavController().navigate(action)
                }
                is NetworkResult.ServerError -> {
                    authViewModel.resetSignupResult()
                    Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.NetworkError -> {
                    authViewModel.resetSignupResult()
                    Toast.makeText(requireContext(), "Network error: ${result.exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
