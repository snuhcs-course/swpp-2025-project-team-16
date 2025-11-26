package com.fitquest.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Test
    fun test_activity_in_view() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.loginFragmentContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun test_LoginEmailFragment_UI_components_are_displayed() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.btnEmailCheckQuest)).check(matches(isDisplayed()))
    }

    @Test
    fun test_navigateToPasswordStep_when_email_exists() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.etEmail)).perform(typeText("test@test.com"))
        onView(withId(R.id.btnEmailCheckQuest)).perform(click())

        // Check if the password fragment is displayed
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
    }
}