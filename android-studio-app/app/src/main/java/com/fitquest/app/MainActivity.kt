package com.fitquest.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fitquest.app.ui.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity hosts the 4 main screens with bottom navigation
 * 
 * Screens:
 * 1. JourneyFragment - The road view with workout flags
 * 2. ScheduleFragment - Schedule creation and modification
 * 3. AiCoachFragment - AI pose detection with camera
 * 4. ProfileFragment - User stats and workout history
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNavigation)
        setupBottomNavigation()

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(JourneyFragment())
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_journey -> {
                    loadFragment(JourneyFragment())
                    true
                }
                R.id.nav_schedule -> {
                    loadFragment(ScheduleFragment())
                    true
                }
                R.id.nav_ai_coach -> {
                    loadFragment(AiCoachFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.mainFragmentContainer, fragment)
            .commit()
    }
}
