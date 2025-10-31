package com.fitquest.app.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fitquest.app.LoginActivity
import com.fitquest.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

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
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        emailInput = view.findViewById(R.id.etEmail)
        usernameInput = view.findViewById(R.id.etHeroName)
        passwordInput = view.findViewById(R.id.etPassword)
        confirmPasswordInput = view.findViewById(R.id.etConfirmPassword)
        nextButton = view.findViewById(R.id.btnContinue)
        backButton = view.findViewById(R.id.btnBack)

        // Pre-fill email
        emailInput.setText(email)

        nextButton.setOnClickListener {
            if (validateInputs()) {
                val activity = activity as? LoginActivity
                activity?.navigateToSignupStep2(
                    emailInput.text.toString(),
                    passwordInput.text.toString(),
                    usernameInput.text.toString()
                )
            }
        }

        backButton.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun validateInputs(): Boolean {
        val email = emailInput.text.toString()
        val username = usernameInput.text.toString()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        when {
            email.isEmpty() -> {
                emailInput.error = "Email is required"
                return false
            }
            username.isEmpty() -> {
                usernameInput.error = "Username is required"
                return false
            }
            password.isEmpty() -> {
                passwordInput.error = "Password is required"
                return false
            }
            password != confirmPassword -> {
                confirmPasswordInput.error = "Passwords do not match"
                return false
            }
            password.length < 6 -> {
                passwordInput.error = "Password must be at least 6 characters"
                return false
            }
        }

        return true
    }
}
