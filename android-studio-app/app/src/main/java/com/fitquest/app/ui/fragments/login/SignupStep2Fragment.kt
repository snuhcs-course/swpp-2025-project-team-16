package com.fitquest.app.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.fitquest.app.LoginActivity
import com.fitquest.app.R
import com.fitquest.app.model.FitnessLevel

/**
 * SignupStep2Fragment - Step 3-2 of signup flow (CHOOSE YOUR CLASS)
 * 
 * User selects fitness level as RPG class:
 * - Novice (Beginner) - Shield icon, cyan theme
 * - Warrior (Intermediate) - Swords icon, gold theme
 * - Champion (Advanced) - Lightning icon, bright gold theme
 * 
 * Design: RPG class selection
 * - Dark slate background
 * - Cards with hover/select animations
 * - Icon + class name + description
 * - Selected card glows with class color
 */
class SignupStep2Fragment : Fragment() {

    private lateinit var beginnerCard: View
    private lateinit var intermediateCard: View
    private lateinit var advancedCard: View
    private lateinit var startButton: Button
    private lateinit var backButton: Button
    
    private var email: String = ""
    private var password: String = ""
    private var username: String = ""
    private var selectedLevel: FitnessLevel? = null

    companion object {
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"
        private const val ARG_USERNAME = "username"
        
        fun newInstance(email: String, password: String, username: String): SignupStep2Fragment {
            val fragment = SignupStep2Fragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            args.putString(ARG_PASSWORD, password)
            args.putString(ARG_USERNAME, username)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = arguments?.getString(ARG_EMAIL) ?: ""
        password = arguments?.getString(ARG_PASSWORD) ?: ""
        username = arguments?.getString(ARG_USERNAME) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        beginnerCard = view.findViewById(R.id.btnBeginner)
        intermediateCard = view.findViewById(R.id.btnIntermediate)
        advancedCard = view.findViewById(R.id.btnAdvanced)
        startButton = view.findViewById(R.id.btnStart)
        backButton = view.findViewById(R.id.btnBack)

        beginnerCard.setOnClickListener {
            selectLevel(FitnessLevel.BEGINNER)
        }

        intermediateCard.setOnClickListener {
            selectLevel(FitnessLevel.INTERMEDIATE)
        }

        advancedCard.setOnClickListener {
            selectLevel(FitnessLevel.ADVANCED)
        }

        startButton.setOnClickListener {
            if (selectedLevel != null) {
                completeSignup()
            } else {
                // Show error - no level selected
            }
        }

        backButton.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun selectLevel(level: FitnessLevel) {
        selectedLevel = level
        
        // Update UI to show selection
        // TODO: Add visual feedback (highlight selected card)
        updateCardSelection()
    }

    private fun updateCardSelection() {
        // TODO: Update card backgrounds/borders to show selection
        // Reset all cards
        // Highlight selected card based on selectedLevel
    }

    private fun completeSignup() {
        // TODO: Backend - Create user account
        // Save user data: email, password, username, selectedLevel
        
        val activity = activity as? LoginActivity
        activity?.completeLogin()
    }
}
