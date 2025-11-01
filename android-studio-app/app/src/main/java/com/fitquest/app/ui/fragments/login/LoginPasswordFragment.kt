package com.fitquest.app.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fitquest.app.LoginActivity
import com.fitquest.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * LoginPasswordFragment - Step 2 of login flow (for existing users)
 *
 * User enters password to login
 */
class LoginPasswordFragment : Fragment() {

    private lateinit var emailText: TextView
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var backButton: MaterialButton

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
    ): View? {
        return inflater.inflate(R.layout.fragment_login_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailText = view.findViewById(R.id.tvEmail)
        passwordInput = view.findViewById(R.id.etPassword)
        loginButton = view.findViewById(R.id.btnLogin)

        emailText.text = email

        loginButton.setOnClickListener {
            val password = passwordInput.text.toString()

            if (password.isNotEmpty()) {
                verifyPassword(email, password)
            } else {
                passwordInput.error = "Please enter your password"
            }
        }

    }

    private fun verifyPassword(email: String, password: String) {
        // TODO: Backend - Verify password from server
        val isValid = true // Replace with actual backend call

        if (isValid) {
            val activity = activity as? LoginActivity
            activity?.completeLogin()
        } else {
            passwordInput.error = "Incorrect password"
        }
    }
}
