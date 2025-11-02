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
class LoginEmailFragment : Fragment() {

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
        continueButton = view.findViewById(R.id.btnBeginQuest)

        continueButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                checkEmailExists(email)
            } else {
                emailInput.error = "Please enter your email"
            }
        }
    }

    private fun checkEmailExists(email: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.checkEmail(mapOf("email" to email))
                }

                if (response.isSuccessful) {
                    val body: EmailCheckResponse? = response.body()
                    val exists = body?.exists == true

                    val activity = activity as? LoginActivity
                    if (exists) {
                        // ✅ 이미 존재하는 유저 → 비밀번호 화면으로 이동
                        activity?.navigateToPasswordStep(email)
                    } else {
                        // ✅ 새로운 유저 → 회원가입 단계로 이동
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
