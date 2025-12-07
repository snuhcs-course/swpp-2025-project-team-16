package com.fitquest.app

import android.view.View
import androidx.annotation.IdRes
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.GrantPermissionRule
import com.fitquest.app.data.remote.AuthApiService
import com.fitquest.app.data.remote.DailySummaryApiService
import com.fitquest.app.data.remote.PoseAnalysisApiService
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.data.remote.ServiceLocator
import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.data.remote.UserApiService
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {
    @get:Rule
    val scenarioRule = ActivityScenarioRule(MainActivity::class.java)
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.CAMERA
        )

    private val mockApiService: AuthApiService = FakeApiService()
    private val mockScheduleApiService: ScheduleApiService = FakeScheduleApiService()
    private val mockSessionApiService: SessionApiService = FakeSessionApiService()
    private val mockUserApiService: UserApiService = FakeUserApiService()
    private val mockDailySummaryApiService: DailySummaryApiService = FakeDailySummaryService()
    private val mockPoseAnalysisApiService: PoseAnalysisApiService =FakePoseAnalysisApiService()


    @Before
    fun setUp() {
        ServiceLocator.authApiService = mockApiService
        ServiceLocator.scheduleApiService = mockScheduleApiService
        ServiceLocator.sessionApiService = mockSessionApiService
        ServiceLocator.userApiService = mockUserApiService
        ServiceLocator.dailySummaryApiService=mockDailySummaryApiService
        ServiceLocator.poseAnalysisApiService=mockPoseAnalysisApiService

    }
    fun clickBottomNavigationItem(@IdRes itemId: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(BottomNavigationView::class.java)
            }

            override fun getDescription(): String {
                return "Click on BottomNavigationView item"
            }

            override fun perform(uiController: UiController?, view: View?) {
                val bottomNavigationView = view as BottomNavigationView
                bottomNavigationView.selectedItemId = itemId
            }
        }
    }

    @Test
    fun test_activity_in_view() {
        scenarioRule.scenario.onActivity { activity ->
            // NavController 가져오기
            val navController = Navigation.findNavController(activity, R.id.mainFragmentContainer)

            // NavController가 attach 되었는지 확인
            assertNotNull(navController)

            // 현재 목적지가 nav_graph의 startDestination인지 검증
            val expectedStartDest = navController.graph.startDestinationId
            assertEquals(expectedStartDest, navController.currentDestination?.id)
        }
    }
    @Test
    fun test_journey_fragment(){
        onView(withId(R.id.bottomNavigation))
            .perform(clickBottomNavigationItem(R.id.journeyFragment))
        onView(withId(R.id.recyclerJourney))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0, click()
                )
            )
        onView(withId(R.id.btnStartSession)).perform(click())
        onView(withId(R.id.btnStartWorkout)).perform(click())
        Thread.sleep(15000)
        onView(withId(R.id.btnSwitchCamera)).perform(click())
        onView(withId(R.id.btnStartWorkout)).perform(click())
    }
    @Test
    fun test_aiCoachFragment(){
        onView(withId(R.id.bottomNavigation))
            .perform(clickBottomNavigationItem(R.id.aiCoachFragment))
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.btnSwitchCamera)).perform(click())
        Thread.sleep(15000)
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.btnLater)).perform(click())
        onView(withId(R.id.spinnerExercise)).perform(click())
        // 2. 드롭다운에서 "Push-ups" 항목 클릭
        onView(withText(containsString("Plank"))).perform(click())
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.btnSwitchCamera)).perform(click())
        onView(withId(R.id.btnSwitchCamera)).perform(click())
        Thread.sleep(15000)
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.btnLater)).perform(click())

        onView(withId(R.id.spinnerExercise)).perform(click())
        // 2. 드롭다운에서 "Push-ups" 항목 클릭
        onView(withText(containsString("Lunge"))).perform(click())
        onView(withId(R.id.btnStartWorkout)).perform(click())
        Thread.sleep(35000)
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.btnEvaluate)).perform(click())
        onView(withId(R.id.bottomNavigation))
            .perform(clickBottomNavigationItem(R.id.profileFragment))
        onView(withId(R.id.recyclerHistory))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0, click()
                )
            )


    }
    @Test
    fun test_PoseFragment(){
        onView(withId(R.id.bottomNavigation))
            .perform(clickBottomNavigationItem(R.id.poseFragment))
        onView(withId(R.id.spinnerExercisePose)).perform(click())

        // 2. 드롭다운에서 "Push-ups" 항목 클릭
        onView(withText(containsString("Plank"))).perform(click())

        // 3. Spinner에 선택된 값이 표시되는지 확인
        onView(allOf(withId(R.id.spinnerExercisePose), withSpinnerText(containsString("Plank"))))
            .check(matches(isDisplayed()))
        onView(withId(R.id.spinnerExercisePose)).perform(click())
        // 2. 드롭다운에서 "Push-ups" 항목 클릭
        onView(withText(containsString("Squat"))).perform(click())
        onView(withId(R.id.btnSwitchCamera)).perform(click())
        onView(withId(R.id.btnSwitchCamera)).perform(click())
        onView(withId(R.id.btnCapture)).perform(click())
        Thread.sleep(25000)
        onView(withId(R.id.btnResultClose)).perform(click())
        onView(withId(R.id.btnUpload)).perform(click())

    }
    @Test
    fun test_ProfileFragment(){
        onView(withId(R.id.bottomNavigation))
            .perform(clickBottomNavigationItem(R.id.profileFragment))
        onView(withId(R.id.btnViewRankings)).perform(click())
        onView(withId(R.id.btnCloseRank)).perform(click())
        onView(withId(R.id.recyclerHistory))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0, click()
                )
            )
        pressBack()
    }
}