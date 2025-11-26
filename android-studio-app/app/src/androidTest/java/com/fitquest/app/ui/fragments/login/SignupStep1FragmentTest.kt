package com.fitquest.app.ui.fragments.login

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.data.remote.ApiService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class SignupStep1FragmentTest {

    private val testEmail = "test@example.com"
    @Mock
    private lateinit var mockApiService: ApiService
    @Before
    fun setup() {

        MockitoAnnotations.openMocks(this)
        val fragmentArgs = SignupStep1Fragment.newInstance(testEmail).arguments
        launchFragmentInContainer<SignupStep1Fragment>(fragmentArgs, R.style.Theme_FitQuest)
    }

    @Test
    fun test_all_fields_are_displayed() {
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etHeroName)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.etConfirmPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnContinue)).check(matches(isDisplayed()))
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()))
    }

    @Test
    fun test_empty_fields_show_error() {
        onView(withId(R.id.btnContinue)).perform(click())
        // Add assertions for error messages
    }

    @Test
    fun test_password_mismatch_shows_error() {
        onView(withId(R.id.etEmail)).perform(typeText(testEmail))
        onView(withId(R.id.etHeroName)).perform(typeText("TestUser"))
        onView(withId(R.id.etPassword)).perform(typeText("password123"))
        onView(withId(R.id.etConfirmPassword)).perform(typeText("password456"))
        onView(withId(R.id.btnContinue)).perform(click())
        // Add assertion for password mismatch error
    }

    @Test
    fun test_short_password_shows_error() {
        onView(withId(R.id.etEmail)).perform(typeText(testEmail))
        onView(withId(R.id.etHeroName)).perform(typeText("TestUser"))
        onView(withId(R.id.etPassword)).perform(typeText("123"))
        onView(withId(R.id.etConfirmPassword)).perform(typeText("123"))
        onView(withId(R.id.btnContinue)).perform(click())
        // Add assertion for short password error
    }

    @Test
    fun test_back_button_works() {
        onView(withId(R.id.btnBack)).perform(click())
        // Add assertion for navigation back
    }

    @Test
    fun test_successful_signup_navigates_to_next_step() {
        onView(withId(R.id.etEmail)).perform(typeText(testEmail))
        onView(withId(R.id.etHeroName)).perform(typeText("TestUser"))
        onView(withId(R.id.etPassword)).perform(typeText("password123"))
        onView(withId(R.id.etConfirmPassword)).perform(typeText("password123"))
        onView(withId(R.id.btnContinue)).perform(click())
        // Add assertion for navigation to the next step
    }
}
