package com.fitquest.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.fitquest.app.data.remote.AuthApiService
import com.fitquest.app.data.remote.DailySummaryApiService
import com.fitquest.app.data.remote.PoseAnalysisApiService
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.data.remote.ServiceLocator
import com.fitquest.app.data.remote.SessionApiService
import com.fitquest.app.data.remote.UserApiService
import com.fitquest.app.model.Schedule
import com.fitquest.app.ui.fragments.login.LoginEmailFragment
//import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
//import org.mockito.Mock
//import org.mockito.Mockito
//import org.mockito.MockitoAnnotations
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

@MediumTest
@RunWith(AndroidJUnit4::class)

class LoginActivityTest {
    @get:Rule
    val scenarioRule = ActivityScenarioRule(LoginActivity::class.java)
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
    fun setUp(){
        ServiceLocator.authApiService = mockApiService
        ServiceLocator.scheduleApiService = mockScheduleApiService
        ServiceLocator.sessionApiService = mockSessionApiService
        ServiceLocator.userApiService = mockUserApiService
        ServiceLocator.dailySummaryApiService=mockDailySummaryApiService
        ServiceLocator.poseAnalysisApiService=mockPoseAnalysisApiService
    }

    @Test
    fun test_admin() {

        //이메일 입력
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        onView(withId(R.id.etEmail)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        //뒤로가기
        onView(withId(R.id.btnBack)).perform(click())
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

        onView(withId(R.id.btnPasswordLogin)).perform(click())

        // 비밀번호 입력 실패
        onView(withId(R.id.etPassword)).perform(typeText("0001") , closeSoftKeyboard())
        onView(withId(R.id.btnPasswordLogin)).perform(click())
        //비밀번호 입력 성공
        onView(withId(R.id.etPassword)).perform(typeText("0000"), closeSoftKeyboard())
        onView(withId(R.id.btnPasswordLogin)).perform(click())
    }

    @Test
    fun test_preregistered() {
        onView(withId(R.id.etEmail)).perform(typeText("snu@snu.com"),closeSoftKeyboard())
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        onView(withId(R.id.etPassword)).perform(typeText("0001"),closeSoftKeyboard())
        onView(withId(R.id.btnPasswordLogin)).perform(click())

        onView(withId(R.id.etPassword)).perform(typeText("0000"),closeSoftKeyboard())
        onView(withId(R.id.btnPasswordLogin)).perform(click())
    }

    @Test
    fun test_login_error() {

        onView(withId(R.id.etEmail)).perform(typeText("snu@snu.com"),closeSoftKeyboard())

        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

        onView(withId(R.id.etPassword)).perform(typeText("error"),closeSoftKeyboard())
        onView(withId(R.id.btnPasswordLogin)).perform(click())

    }
    @Test
    fun test_email_error()  {

        onView(withId(R.id.etEmail)).perform(typeText("error@error.com"),closeSoftKeyboard())
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

    }
    @Test
    fun test_signup()  {

        onView(withId(R.id.etEmail)).perform(typeText("signup@signup.com"), closeSoftKeyboard())
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        onView(withId(R.id.btnBack)).perform(click())
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

        // username 입력
        onView(withId(R.id.btnContinue)).perform(click())
        onView(withId(R.id.etHeroName)).perform(typeText("test"), closeSoftKeyboard())
        // password 입력
        onView(withId(R.id.btnContinue)).perform(click())
        onView(withId(R.id.etPassword)).perform(typeText("0000"), closeSoftKeyboard())
        onView(withId(R.id.etConfirmPassword)).perform(typeText("0000"), closeSoftKeyboard())
        // password의 길이가 너무 짧습니다
        onView(withId(R.id.btnContinue)).perform(click())
        onView(withId(R.id.etPassword)).perform(typeText("123456"), closeSoftKeyboard())
        // password가 일치하지 않습니다
        onView(withId(R.id.btnContinue)).perform(click())
        onView(withId(R.id.etConfirmPassword)).perform(typeText("123456"), closeSoftKeyboard())
        //정상
        onView(withId(R.id.btnContinue)).perform(click())
        Thread.sleep(15000)
        onView(withId(R.id.btnStop)).perform(click())
    }

    @Test
    fun test_signup_error()  {

        onView(withId(R.id.etEmail)).perform(typeText("signup@signup.com"),closeSoftKeyboard())
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

        onView(withId(R.id.etHeroName)).perform(typeText("error"),closeSoftKeyboard())
        // password 입력

        onView(withId(R.id.etPassword)).perform(typeText("123456"),closeSoftKeyboard())
        onView(withId(R.id.etConfirmPassword)).perform(typeText("123456"),closeSoftKeyboard())
        //정상
        onView(withId(R.id.btnContinue)).perform(click())
        onView(withId(R.id.etHeroName)).perform(typeText("else"),closeSoftKeyboard())
        onView(withId(R.id.btnContinue)).perform(click())

    }
}