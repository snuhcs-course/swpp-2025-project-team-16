package com.aisportspt.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aisportspt.app.databinding.ActivityMainBinding
import com.aisportspt.app.ui.dialogs.AddSessionDialogFragment
import com.aisportspt.app.ui.dialogs.AddSportDialogFragment
import com.aisportspt.app.ui.fragments.AchievementFragment
import com.aisportspt.app.ui.fragments.AiCoachFragment
import com.aisportspt.app.ui.fragments.SportsFragment
import com.aisportspt.app.ui.fragments.TrainingFragment
import com.aisportspt.app.ui.viewmodels.MainViewModel
import com.aisportspt.app.model.Sport
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupBottomNavigation()
        setupFAB()
        
        // 기본 프래그먼트 로드
        if (savedInstanceState == null) {
            loadFragment(SportsFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_sports
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_sports -> {
                    loadFragment(SportsFragment())
                    true
                }
                R.id.nav_ai_coach -> {
                    loadFragment(AiCoachFragment())
                    true
                }
                R.id.nav_training -> {
                    loadFragment(TrainingFragment())
                    true
                }
                R.id.nav_achievements -> {
                    loadFragment(AchievementFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFAB() {
        binding.fabAdd.setOnClickListener {
            showAddSportDialog()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    fun showAddSportDialog() {
        val dialog = AddSportDialogFragment { sport ->
            viewModel.addSport(sport)
        }
        dialog.show(supportFragmentManager, "AddSportDialog")
    }
    
    fun showAddSessionDialog(sport: Sport) {
        val dialog = AddSessionDialogFragment(sport) { session ->
            viewModel.addSession(session)
        }
        dialog.show(supportFragmentManager, "AddSessionDialog")
    }
    
    fun getViewModel(): MainViewModel = viewModel
}