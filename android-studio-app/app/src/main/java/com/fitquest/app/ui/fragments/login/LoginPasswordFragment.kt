package com.fitquest.app.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.LoginActivity
import com.fitquest.app.R
import com.fitquest.app.data.remote.LoginRequest
import com.fitquest.app.data.remote.LoginResponse
import com.fitquest.app.data.remote.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

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
        backButton = view.findViewById(R.id.btnBack)

        emailText.text = email

        loginButton.setOnClickListener {
            val password = passwordInput.text.toString().trim()

            if (password.isNotEmpty()) {
                verifyPassword(email, password)
            } else {
                passwordInput.error = "Please enter your password"
            }
        }

        backButton.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun verifyPassword(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.login(LoginRequest(email, password))
                }

                if (response.isSuccessful) {
                    val body: LoginResponse? = response.body()
                    if (body?.token != null) {
                        val token = body.token
                        val name = body.name ?: ""

                        // SharedPreferences 예시 (나중에 DataStore로 바꿔도 됨)
                        val prefs = requireContext().getSharedPreferences("auth", 0)
                        prefs.edit {
                            putString("token", token)
                                .putString("email", email)
                                .putString("name", name)
                        }

                        Toast.makeText(requireContext(), "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                        (activity as? LoginActivity)?.completeLogin()
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
}
