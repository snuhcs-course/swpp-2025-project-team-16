package com.fitquest.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fitquest.app.ui.fragments.login.*

/**
 * LoginActivity handles the multi-step login/signup flow
 * 
 * Flow:
 * 1. LoginEmailFragment - Check if user exists
 * 2a. If exists -> LoginPasswordFragment
 * 2b. If new -> SignupStep1Fragment -> SignupStep2Fragment
 */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Start with email fragment
        if (savedInstanceState == null) {
            navigateToFragment(LoginEmailFragment())
        }
    }

    /**
     * Navigate to login password step
     */
    fun navigateToPasswordStep(email: String) {
        val fragment = LoginPasswordFragment.newInstance(email)
        navigateToFragment(fragment)
    }

    /**
     * Navigate to signup step 1
     */
    fun navigateToSignupStep1(email: String) {
        val fragment = SignupStep1Fragment.newInstance(email)
        navigateToFragment(fragment)
    }

    /**
     * Navigate to signup step 2
     */
    fun navigateToSignupStep2(email: String, password: String, username: String) {
        val fragment = SignupStep2Fragment.newInstance(email, password, username)
        navigateToFragment(fragment)
    }

    /**
     * Complete login and navigate to MainActivity
     */
    fun completeLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left
            )
            .replace(R.id.loginFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}
