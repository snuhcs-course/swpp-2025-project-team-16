package com.fitquest.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.data.remote.ServiceLocator
import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.data.remote.UserApiService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {
    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.CAMERA
        )

    private val mockApiService: ApiService = FakeApiService()
    private val mockScheduleApiService: ScheduleApiService = FakeScheduleApiService()
    private val mockSessionApiService: SessionApiService = FakeSessionApiService()
    private val mockUserApiService: UserApiService = FakeUserApiService()
    @Before
    fun setUp() {
        ServiceLocator.authApiService = mockApiService
        ServiceLocator.apiService = mockScheduleApiService
        ServiceLocator.sessionApiService = mockSessionApiService
        ServiceLocator.userApiService = mockUserApiService
    }
    @Test
    fun test_activity_in_view() {
        onView(withId(R.id.mainFragmentContainer)).check(matches(isDisplayed()))
    }
}