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
import android.util.Log
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class SignupStep1Fragment : Fragment() {

    private var _binding: FragmentSignupStep1Binding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(RetrofitClient.authApiService, requireContext())
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
    }

    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString()
        val username = binding.etHeroName.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        return when {
            email.isEmpty() -> { binding.etEmail.error = "Email is required"; false }
            username.isEmpty() -> { binding.etHeroName.error = "Username is required"; false }
            password.isEmpty() -> { binding.etPassword.error = "Password is required"; false }
            password != confirmPassword -> { binding.etConfirmPassword.error = "Passwords do not match"; false }
            password.length < 6 -> { binding.etPassword.error = "Password must be at least 6 characters"; false }
            else -> true
        }
    }

    private fun signupUser() {
        inputEmail = binding.etEmail.text.toString().trim()
        inputName = binding.etHeroName.text.toString().trim()
        inputPassword = binding.etPassword.text.toString().trim()

        Log.d("SignupStep1", "REQUEST → email=$inputEmail, name=$inputName, pw=$inputPassword")

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
                    Toast.makeText(requireContext(), body.message ?: "Signup success!", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Signup failed: ${result.code}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.NetworkError -> {
                    authViewModel.resetSignupResult()
                    Toast.makeText(requireContext(), "Network error: ${result.exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SignupStep1Fragment.kt 안에 추가 (validateInputs 아래쪽에)
    internal fun validateInputsForTest(
        email: String,
        username: String,
        password: String,
        confirm: String
    ): Boolean {
        return when {
            email.isEmpty() -> false
            username.isEmpty() -> false
            password.isEmpty() -> false
            password != confirm -> false
            password.length < 6 -> false
            else -> true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
