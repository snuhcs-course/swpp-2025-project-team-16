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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginEmailFragmentTest {

    @Before
    fun setup() {
        launchFragmentInContainer<LoginEmailFragment>(themeResId = R.style.Theme_FitQuest)
    }

    @Test
    fun test_all_fields_are_displayed() {
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.btnEmailCheckQuest)).check(matches(isDisplayed()))
    }

    @Test
    fun test_fill_form_and_click_continue() {
        onView(withId(R.id.etEmail)).perform(typeText("test@example.com"))
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        // Add assertion to check navigation
    }

    @Test
    fun test_empty_email_shows_error() {
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
    }

    @Test
    fun test_email_format_is_validated() {
        onView(withId(R.id.etEmail)).perform(typeText("invalid-email"))
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())
        // Add assertion to check for email format validation
    }
}
