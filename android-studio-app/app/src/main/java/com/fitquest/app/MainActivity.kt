package com.fitquest.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fitquest.app.ui.fragments.*
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity hosts the 4 main screens with bottom navigation
 *
 * Screens:
 * 1. JourneyFragment - The road view with workout flags
 * 2. ScheduleFragment - Schedule creation and modification
 * 3. AiCoachFragment - AI pose detection with camera
 * 4. ProfileFragment - User stats and workout history
 * 5. PoseFragment - Pose detection with camera
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private val coachVm: AiCoachViewModel by viewModels()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNavigation)
        setupBottomNavigation()

        // 세션 잠금 상태 관찰 → 탭 비활성화(시각적/물리적 차단)
        coachVm.sessionActive.observe(this) { locked ->
            setBottomNavEnabled(!locked)
        }

        // 뒤로가기 차단 (세션 중에는 뒤로가기 불가)
        onBackPressedDispatcher.addCallback(this) {
            if (coachVm.sessionActive.value == true) {
                Toast.makeText(this@MainActivity, "세션 진행 중에는 뒤로가기를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(JourneyFragment())
        }
    }

    private fun setupBottomNavigation() {
        // 탭 선택 차단
        bottomNav.setOnItemSelectedListener { item ->
            if (coachVm.sessionActive.value == true) {
                Toast.makeText(this, "세션 진행 중에는 이동할 수 없습니다. 먼저 일시정지/종료하세요.", Toast.LENGTH_SHORT).show()
                return@setOnItemSelectedListener false
            }
            when (item.itemId) {
                R.id.nav_journey -> {
                    loadFragment(JourneyFragment())
                    true
                }
                R.id.nav_schedule -> {
                    loadFragment(ScheduleFragment())
                    true
                }
                R.id.nav_pose -> {
                    loadFragment(PoseFragment())
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

        // 현재 탭 재선택(리로딩)도 세션 중 차단
        bottomNav.setOnItemReselectedListener {
            if (coachVm.sessionActive.value == true) {
                Toast.makeText(this, "세션 진행 중에는 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
            // 잠금 아닐 때는 기본 동작(스크롤 탑 등) 유지하거나 무시
        }
    }

    // 프로그램적 전환도 세션 중 차단(방어막)
    private fun loadFragment(fragment: Fragment) {
        if (coachVm.sessionActive.value == true) {
            Toast.makeText(this, "세션 진행 중에는 화면 전환이 차단됩니다.", Toast.LENGTH_SHORT).show()
            return
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.mainFragmentContainer, fragment)
            .commit()
    }

    private fun setBottomNavEnabled(enabled: Boolean) {
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            menu.getItem(i).isEnabled = enabled
        }
    }
}
