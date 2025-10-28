package com.fitquest.app.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.fitquest.app.LoginActivity
import com.fitquest.app.R

/**
 * SignupStep1Fragment - Step 3-1 of signup flow
 * 
 * Collects: email, username/nickname, password, confirm password
 */
class SignupStep1Fragment : Fragment() {

    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var nextButton: Button
    private lateinit var backButton: Button
    
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

        emailInput = view.findViewById(R.id.email_input)
        usernameInput = view.findViewById(R.id.username_input)
        passwordInput = view.findViewById(R.id.password_input)
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input)
        nextButton = view.findViewById(R.id.next_button)
        backButton = view.findViewById(R.id.back_button)

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
