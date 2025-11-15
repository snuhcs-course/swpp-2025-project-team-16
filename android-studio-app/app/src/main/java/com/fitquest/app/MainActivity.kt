package com.fitquest.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fitquest.app.repository.SessionRepository
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.fitquest.app.ui.viewmodels.AiCoachViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var navController: NavController

    private val coachVm: AiCoachViewModel by viewModels {
        AiCoachViewModelFactory(SessionRepository())
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ NavHostFragment에서 NavController 가져오기
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.mainFragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        bottomNav = findViewById(R.id.bottomNavigation)

        // ✅ Bottom Navigation과 NavController 자동 연결
        bottomNav.setupWithNavController(navController)

        setupSessionLock()
        setupBackPress()
    }

    private fun setupSessionLock() {
        coachVm.sessionActive.observe(this) { locked ->
            setBottomNavEnabled(!locked)
        }

        // ✅ 세션 활성화 시 네비게이션 이벤트 차단
        bottomNav.setOnItemSelectedListener { item ->
            if (coachVm.sessionActive.value == true) {
                Toast.makeText(
                    this,
                    "세션 진행 중에는 이동할 수 없습니다. 먼저 일시정지/종료하세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnItemSelectedListener false
            }

            // NavController가 자동으로 처리하도록
            navController.navigate(item.itemId)
            true
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this) {
            if (coachVm.sessionActive.value == true) {
                Toast.makeText(
                    this@MainActivity,
                    "세션 진행 중에는 뒤로가기를 사용할 수 없습니다.",
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
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            menu.getItem(i).isEnabled = enabled
        }
    }
}
