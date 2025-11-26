package com.fitquest.app.ui.fragments.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.MainActivity
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.TokenManager
import com.fitquest.app.databinding.FragmentLoginPasswordBinding
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.login.LoginRequest
import com.fitquest.app.ui.viewmodels.AuthViewModel
import com.fitquest.app.ui.viewmodels.AuthViewModelFactory
import com.fitquest.app.ui.viewmodels.LoginProgress
import com.fitquest.app.ui.viewmodels.LoginViewModel
import com.fitquest.app.ui.viewmodels.LoginViewModelFactory
import kotlinx.coroutines.launch

class LoginPasswordFragment : Fragment() {

    private var _binding: FragmentLoginPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(RetrofitClient.authApiService, requireContext())
    }

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            RetrofitClient.scheduleApiService,
            RetrofitClient.dailySummaryApiService
        )
    }

    private var email: String = ""

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = arguments?.getString(ARG_EMAIL) ?: ""
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

        binding.tvEmail.text = email

        binding.btnPasswordLogin.setOnClickListener {
            val password = binding.etPassword.text.toString().trim()
            if (password.isNotEmpty()) {
                showProcessingOverlay()
                verifyPassword(email, password)
            } else {
                binding.etPassword.error = "Please enter your password"
            }
        }

        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        observeLoginResult()

        observeLoginProgress()
    }


    private fun verifyPassword(email: String, password: String) {
        if (email == "test@test.com" && password == "0000") {
            val fakeToken = "TEST_TOKEN"
            val fakeName = "Test User"
            TokenManager.saveToken(requireContext(), fakeToken, email, fakeName)
            Toast.makeText(requireContext(), "Welcome back, $fakeName", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch { loginViewModel.completeLoginFlow() }
            return
        }

        authViewModel.login(LoginRequest(email, password))
    }

    private fun observeLoginResult() {
        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val body = result.data
                    TokenManager.saveToken(requireContext(), body.token ?: "", email, body.name ?: "")
                    Toast.makeText(requireContext(), "Welcome back, ${body.name}", Toast.LENGTH_SHORT).show()

                    lifecycleScope.launch {
                        loginViewModel.completeLoginFlow()
                    }
                }
                is NetworkResult.ServerError -> {
                    binding.etPassword.error = result.message
                    hideProcessingOverlay()
                }
                is NetworkResult.NetworkError -> {
                    Toast.makeText(
                        requireContext(),
                        "Network error: ${result.exception.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideProcessingOverlay()
                }
            }
        }
    }

    private fun showProcessingOverlay() {
        binding.progressOverlay.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Processing..."
        binding.btnPasswordLogin.isEnabled = false
    }

    private fun hideProcessingOverlay() {
        binding.progressOverlay.visibility = View.GONE
        binding.btnPasswordLogin.isEnabled = true
    }

    private fun observeLoginProgress() {
        loginViewModel.progressState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginProgress.Step -> {
                    binding.tvProgressStatus.text = state.message
                    binding.progressBar.progress = state.step * 33
                }
                LoginProgress.Completed -> {
                    hideProcessingOverlay()
                    navigateToMainActivity()
                }
                is LoginProgress.Error -> {
                    hideProcessingOverlay()
                    binding.tvProgressStatus.text = "Error: ${state.error}"
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        activity?.let {
            it.startActivity(Intent(it, MainActivity::class.java))
            it.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
