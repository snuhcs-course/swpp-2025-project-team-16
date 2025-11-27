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
import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.EmailCheckResponse
import com.fitquest.app.data.remote.LoginRequest
import com.fitquest.app.data.remote.LoginResponse
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.data.remote.ServiceLocator
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

    private val mockApiService: ApiService= FakeApiService()
    private val mockScheduleApiService: ScheduleApiService= FakeScheduleApiService()

    @Before
    fun setUp(){
        ServiceLocator.apiService=mockScheduleApiService
        ServiceLocator.authApiService=mockApiService
    }

    @Test
    fun test_testScenario() {

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
    fun test_LoginEmailFragment_UI_components_are_displayed() {
        onView(withId(R.id.etEmail)).perform(typeText("snu@snu.com"))
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        onView(withId(R.id.etPassword)).perform(typeText("0001"))
        onView(withId(R.id.btnPasswordLogin)).perform(click())

        onView(withId(R.id.etPassword)).perform(typeText("0000"))
        onView(withId(R.id.btnPasswordLogin)).perform(click())
    }

    @Test
    fun test_navigateToPasswordStep_when_email_exists() {

        onView(withId(R.id.etEmail)).perform(typeText("snu@snu.com"))

        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

        onView(withId(R.id.etPassword)).perform(typeText("error"))
        onView(withId(R.id.btnPasswordLogin)).perform(click())

    }
    @Test
    fun test_navigateToPasswordStep_when_email_does_not_exist()  {

        onView(withId(R.id.etEmail)).perform(typeText("error@error.com"))
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

    }
    @Test
    fun test_navigateToSignUpStep_when_email_does_not_exist()  {

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
        onView(withId(R.id.btnStop)).perform(click())
    }

    @Test
    fun test_navigateToSignUpStep_when_error()  {

        onView(withId(R.id.etEmail)).perform(typeText("signup@signup.com"))
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

        onView(withId(R.id.etHeroName)).perform(typeText("error"))
        // password 입력

        onView(withId(R.id.etPassword)).perform(typeText("123456"))
        onView(withId(R.id.etConfirmPassword)).perform(typeText("123456"))
        //정상
        onView(withId(R.id.btnContinue)).perform(click())
        onView(withId(R.id.etHeroName)).perform(typeText("else"))
        onView(withId(R.id.btnContinue)).perform(click())

    }
}