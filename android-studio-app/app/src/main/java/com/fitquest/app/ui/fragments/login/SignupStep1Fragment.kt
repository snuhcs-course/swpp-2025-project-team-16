package com.fitquest.app.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.LoginActivity
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.SignupRequest
import com.fitquest.app.data.remote.SignupResponse
import com.fitquest.app.data.remote.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SignupStep1Fragment - Step 3-1 of signup flow
 *
 * Collects: email, username/nickname, password, confirm password
 */
class SignupStep1Fragment : Fragment() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var nextButton: MaterialButton
    private lateinit var backButton: MaterialButton

    private var email: String = ""

    companion object {
        private const val ARG_EMAIL = "email"
        fun newInstance(email: String): SignupStep1Fragment {
            val fragment = SignupStep1Fragment()
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
    ): View? = inflater.inflate(R.layout.fragment_signup_step1, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailInput = view.findViewById(R.id.etEmail)
        usernameInput = view.findViewById(R.id.etHeroName)
        passwordInput = view.findViewById(R.id.etPassword)
        confirmPasswordInput = view.findViewById(R.id.etConfirmPassword)
        nextButton = view.findViewById(R.id.btnContinue)
        backButton = view.findViewById(R.id.btnBack)

        emailInput.setText(email)

        nextButton.setOnClickListener {
            if (validateInputs()) signupUser()
        }

        backButton.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun validateInputs(): Boolean {
        val email = emailInput.text.toString()
        val username = usernameInput.text.toString()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        return when {
            email.isEmpty() -> { emailInput.error = "Email is required"; false }
            username.isEmpty() -> { usernameInput.error = "Username is required"; false }
            password.isEmpty() -> { passwordInput.error = "Password is required"; false }
            password != confirmPassword -> { confirmPasswordInput.error = "Passwords do not match"; false }
            password.length < 6 -> { passwordInput.error = "Password must be at least 6 characters"; false }
            else -> true
        }
    }

    private fun signupUser() {
        val email = emailInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.signup(SignupRequest(name = username, email = email, password = password))
                }

                if (response.isSuccessful) {
                    val body: SignupResponse? = response.body()
                    if (!body?.token.isNullOrEmpty()) {
                        TokenManager.saveToken(requireContext(), body!!.token, email, username)
                    }
                    Toast.makeText(requireContext(), body?.message ?: "Signup success!", Toast.LENGTH_SHORT).show()
                    (activity as? LoginActivity)?.navigateToSignupStep2(email, password, username)
                } else {
                    Toast.makeText(requireContext(), "Signup failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
}
