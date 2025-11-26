package com.fitquest.app.ui.fragments.login

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.LoginActivity
import com.fitquest.app.R
import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.EmailCheckResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LoginEmailFragment - Step 1 of login flow
 *
 * User enters email address
 * - If email exists in DB -> navigate to LoginPasswordFragment
 * - If email is new -> navigate to SignupStep1Fragment
 */
class LoginEmailFragment(private val apiService: ApiService) : Fragment() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var continueButton: MaterialButton


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emailInput = view.findViewById(R.id.etEmail)
        continueButton = view.findViewById(R.id.btnEmailCheckQuest)
        continueButton.bringToFront()
        continueButton.invalidate()
        continueButton.setBackgroundColor(Color.RED)

        continueButton.setOnClickListener {
            Log.d("DEBUG", "button = $continueButton")
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                checkEmailExists(email)
            } else {
                emailInput.error = "Please enter your email!"
            }
        }
    }

    private fun checkEmailExists(email: String) {
        if (email == "test@test.com") {
            val activity = activity as? LoginActivity
            activity?.navigateToPasswordStep(email)
            return
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.checkEmail(mapOf("email" to email))
                }

                if (response.isSuccessful) {
                    val body: EmailCheckResponse? = response.body()
                    val exists = body?.exists == true

                    val activity = activity as? LoginActivity
                    if (exists) {
                        activity?.navigateToPasswordStep(email)
                    } else {
                        activity?.navigateToSignupStep1(email)
                    }
                } else {
                    Toast.makeText(requireContext(), "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
