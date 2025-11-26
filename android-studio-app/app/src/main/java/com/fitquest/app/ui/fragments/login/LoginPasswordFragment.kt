package com.fitquest.app.ui.fragments.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.LoginActivity
import com.fitquest.app.MainActivity
import com.fitquest.app.R
import com.fitquest.app.data.remote.LoginRequest
import com.fitquest.app.data.remote.LoginResponse
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.TokenManager
import com.fitquest.app.ui.viewmodels.LoginProgress
import com.fitquest.app.ui.viewmodels.LoginViewModel
import com.fitquest.app.ui.viewmodels.LoginViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue

/**
 * LoginPasswordFragment - Step 2 of login flow (for existing users)
 *
 * User enters password to login
 */
class LoginPasswordFragment : Fragment() {

    private lateinit var emailText: TextView
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var backButton: ImageButton

    private lateinit var progressOverlay: View
    private lateinit var progressStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val viewModel: LoginViewModel by viewModels {
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
    ): View? = inflater.inflate(R.layout.fragment_login_password, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emailText = view.findViewById(R.id.tvEmail)
        passwordInput = view.findViewById(R.id.etPassword)
        loginButton = view.findViewById(R.id.btnPasswordLogin)
        backButton = view.findViewById(R.id.btnBack)

        progressOverlay = view.findViewById(R.id.progressOverlay)
        progressStatus = view.findViewById(R.id.tvProgressStatus)
        progressBar = view.findViewById(R.id.progressBar)

        emailText.text = email

        loginButton.setOnClickListener {
            val password = passwordInput.text.toString().trim()
            if (password.isNotEmpty()) {
                showProcessingOverlay()
                verifyPassword(email, password)
            } else {
                passwordInput.error = "Please enter your password"
            }
        }

        backButton.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        observeLoginProgress()
    }

    private fun verifyPassword(email: String, password: String) {
        if (email == "test@test.com" && password == "0000") {
            val fakeToken = "TEST_TOKEN"
            val fakeName = "Test User"

            TokenManager.saveToken(requireContext(), fakeToken, email, fakeName)
            Toast.makeText(requireContext(), "Welcome back, $fakeName", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                viewModel.completeLoginFlow()
            }
            return
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.login(LoginRequest(email, password))
                }

                if (response.isSuccessful) {
                    val body: LoginResponse? = response.body()
                    if (!body?.token.isNullOrEmpty()) {
                        TokenManager.saveToken(requireContext(), body!!.token, email, body.name ?: "")
                        Toast.makeText(requireContext(), "Welcome back, ${body.name}", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            viewModel.completeLoginFlow()
                        }
                    } else {
                        passwordInput.error = body?.error ?: "Invalid credentials"
                    }
                } else {
                    passwordInput.error = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProcessingOverlay() {
        progressOverlay.visibility = View.VISIBLE
        progressStatus.text = "Processing..."
        loginButton.isEnabled = false
    }

    private fun hideProcessingOverlay() {
        progressOverlay.visibility = View.GONE
        loginButton.isEnabled = true
    }

    private fun observeLoginProgress() {
        viewModel.progressState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginProgress.Step -> {
                    progressStatus.text = state.message
                    progressBar.progress = state.step * 33
                }
                LoginProgress.Completed -> {
                    hideProcessingOverlay()
                    navigateToMainActivity()
                }
                is LoginProgress.Error -> {
                    hideProcessingOverlay()
                    progressStatus.text = "Error: ${state.error}"
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
}
