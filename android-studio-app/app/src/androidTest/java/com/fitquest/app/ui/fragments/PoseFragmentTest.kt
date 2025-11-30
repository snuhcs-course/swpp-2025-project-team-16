package com.fitquest.app.ui.fragments

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PoseFragmentTest {

    @Before
    fun setup() {
        launchFragmentInContainer<PoseFragment>(themeResId = R.style.Theme_FitQuest)
    }

    @Test
    fun test_initial_ui_is_displayed() {
        // Check headers
        onView(withId(R.id.tvSystemTitle)).check(matches(withText("POSE ESTIMATION")))
        onView(withId(R.id.tvSystemSubtitle)).check(matches(withText("Evaluate your pose")))

        // Check camera and control buttons
        onView(withId(R.id.cameraPreview)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSwitchCamera)).check(matches(isDisplayed()))
        onView(withId(R.id.btnCapture)).check(matches(isDisplayed()))
        onView(withId(R.id.btnUpload)).check(matches(isDisplayed()))

        // Check initial guide text is visible
        onView(withId(R.id.tvGuideText)).check(matches(isDisplayed()))
        onView(withId(R.id.tvGuideText)).check(matches(withText("Take a picture or upload from gallery")))

        // Check feedback card is visible

    }

    @Test
    fun test_capture_button_is_clickable() {
        onView(withId(R.id.btnCapture)).perform(click())
        // In a real test, we would use Espresso-Intents to verify that the camera intent is fired.
    }

    @Test
    fun test_upload_button_is_clickable() {
        onView(withId(R.id.btnUpload)).perform(click())
        // Similarly, use Espresso-Intents to verify the gallery intent.
    }

    @Test
    fun test_switch_camera_button_is_clickable() {
        onView(withId(R.id.btnSwitchCamera)).perform(click())
        // This should switch the camera preview, which can be hard to verify in a test
        // without more complex setup.
    }


}
