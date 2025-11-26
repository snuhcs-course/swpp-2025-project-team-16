package com.fitquest.app.ui.fragments.login

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.ServiceLocator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class LoginPasswordFragmentTest {

    private val testEmail = "test@example.com"
    @Mock
    private lateinit var mockApiService: ApiService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        val fragmentArgs = LoginPasswordFragment.newInstance(testEmail).arguments
        launchFragmentInContainer<LoginPasswordFragment>(fragmentArgs, R.style.Theme_FitQuest)
    }

    @Test
    fun test_all_fields_are_displayed() {
        onView(withId(R.id.tvEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnPasswordLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()))
    }

    @Test
    fun test_password_is_required() {
        onView(withId(R.id.btnPasswordLogin)).perform(click())
        onView(withId(R.id.etPassword)).check(matches(isDisplayed())) // Placeholder, should check for error
    }

    @Test
    fun test_back_button_works() {
        onView(withId(R.id.btnBack)).perform(click())
        // Add assertion for navigation back
    }

    @Test
    fun test_login_with_valid_credentials() {
        onView(withId(R.id.etPassword)).perform(typeText("password"))
        onView(withId(R.id.btnPasswordLogin)).perform(click())
        // Add assertion for successful login
    }

    @Test
    fun test_login_with_invalid_credentials() {
        onView(withId(R.id.etPassword)).perform(typeText("wrong_password"))
        onView(withId(R.id.btnPasswordLogin)).perform(click())
        // Add assertion to check for error message
    }
}
