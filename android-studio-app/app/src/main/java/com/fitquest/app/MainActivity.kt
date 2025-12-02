package com.fitquest.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.databinding.ActivityMainBinding
import com.fitquest.app.model.InitProgress
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.fitquest.app.ui.viewmodels.AiCoachViewModelFactory
import com.fitquest.app.ui.viewmodels.MainActivityViewModel
import com.fitquest.app.ui.viewmodels.MainActivityViewModelFactory
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainActivityViewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory(
            RetrofitClient.scheduleApiService,
            RetrofitClient.dailySummaryApiService
        )
    }

    private val coachVm: AiCoachViewModel by viewModels {
        AiCoachViewModelFactory(RetrofitClient.sessionApiService)
    }

    private val navController by lazy {
        (binding.mainFragmentContainer.getFragment<NavHostFragment>()).navController
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainActivityViewModel.progressState.observe(this) { state ->
            when (state) {
                is InitProgress.Step -> {
                    binding.progressOverlay.visibility = View.VISIBLE
                    binding.tvProgressStatus.text = state.message
                    binding.progressBar.progress = state.step * 33
                }
                InitProgress.Completed -> {
                    binding.progressOverlay.visibility = View.GONE
                    navController.navigate(R.id.journeyFragment)
                }
                is InitProgress.Error -> {
                    binding.progressOverlay.visibility = View.GONE
                    Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        mainActivityViewModel.completeInitFlow()
        binding.bottomNavigation.setupWithNavController(navController)

        setupSessionLock()
        setupBackPress()
    }

    private fun setupSessionLock() {
        coachVm.sessionActive.observe(this) { locked ->
            setBottomNavEnabled(!locked)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (coachVm.sessionActive.value == true) {
                Toast.makeText(
                    this,
                    "You cannot move during the session. Please pause/terminate first.",
                    Toast.LENGTH_SHORT
                ).show()
                false
            } else {
                navController.navigate(item.itemId)
                true
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this) {
            if (coachVm.sessionActive.value == true) {
                Toast.makeText(
                    this@MainActivity,
                    "Backward is not available during the session.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (!navController.popBackStack()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }

    private fun setBottomNavEnabled(enabled: Boolean) {
        binding.bottomNavigation.menu.forEach { item ->
            item.isEnabled = enabled
        }
    }
}
