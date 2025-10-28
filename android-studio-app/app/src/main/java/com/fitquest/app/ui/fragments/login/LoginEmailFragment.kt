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
 * LoginEmailFragment - Step 1 of login flow
 * 
 * User enters email address
 * - If email exists in DB -> navigate to LoginPasswordFragment
 * - If email is new -> navigate to SignupStep1Fragment
 */
class LoginEmailFragment : Fragment() {

    private lateinit var emailInput: EditText
    private lateinit var continueButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailInput = view.findViewById(R.id.tilEmail)
        continueButton = view.findViewById(R.id.tvEmail)

        continueButton.setOnClickListener {
            val email = emailInput.text.toString()
            
            if (email.isNotEmpty()) {
                checkEmailExists(email)
            } else {
                // Show error
                emailInput.error = "Please enter your email"
            }
        }
    }

    private fun checkEmailExists(email: String) {
        // TODO: Backend - Check if email exists in database
        val userExists = false // Replace with actual backend call
        
        val activity = activity as? LoginActivity
        if (userExists) {
            // User exists, go to password screen
            activity?.navigateToPasswordStep(email)
        } else {
            // New user, go to signup
            activity?.navigateToSignupStep1(email)
        }
    }
}
