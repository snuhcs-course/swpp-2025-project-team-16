package com.fitquest.app.ui.fragments

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiCoachFragmentTest {

    @Test
    fun test_initial_ui_state_is_correct() {
        launchFragmentInContainer<AiCoachFragment>(themeResId = R.style.Theme_FitQuest)

        // Check that initial controls are visible
        onView(withId(R.id.spinnerExercise)).check(matches(isDisplayed()))
        onView(withId(R.id.btnStartWorkout)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSwitchCamera)).check(matches(isDisplayed()))

        // Check that workout-in-progress HUD elements are not visible
        onView(withId(R.id.hudTopContainer)).check(matches(not(isDisplayed())))
        onView(withId(R.id.recordingIndicator)).check(matches(not(isDisplayed())))
        onView(withId(R.id.formScoreContainer)).check(matches(not(isDisplayed())))
        onView(withId(R.id.overlay)).check(matches(not(isDisplayed())))

        // Check initial text on buttons and labels
        onView(withId(R.id.btnStartWorkout)).check(matches(withText("Begin Training")))
        onView(withId(R.id.tvRepCount)).check(matches(withText("0")))
        onView(withId(R.id.tvFeedback)).check(matches(withText("Position yourself in frame")))
    }

    @Test
    fun test_start_button_initiates_countdown() {
        launchFragmentInContainer<AiCoachFragment>(themeResId = R.style.Theme_FitQuest)

        onView(withId(R.id.btnStartWorkout)).perform(click())

        // Countdown UI should appear
        onView(withId(R.id.tvCountdown)).check(matches(isDisplayed()))
        onView(withId(R.id.tvFeedback)).check(matches(withText("Get ready... ⏳")))
        // In a real test, we'd use an IdlingResource to wait for the countdown to finish
        // and then verify the transition to the 'training' state.
    }

    @Test
    fun test_cancel_countdown_returns_to_idle() {
        launchFragmentInContainer<AiCoachFragment>(themeResId = R.style.Theme_FitQuest)

        // Start countdown
        onView(withId(R.id.btnStartWorkout)).perform(click())
        onView(withId(R.id.tvCountdown)).check(matches(isDisplayed()))

        // Click again to cancel
        onView(withId(R.id.btnStartWorkout)).perform(click())

        // Check if UI returned to idle state
        onView(withId(R.id.tvCountdown)).check(matches(not(isDisplayed())))
        onView(withId(R.id.btnStartWorkout)).check(matches(withText("Begin Training")))
        onView(withId(R.id.tvFeedback)).check(matches(withText("Position yourself in frame")))
    }

    @Test
    fun test_exercise_spinner_selection_updates_ui() {
        launchFragmentInContainer<AiCoachFragment>(themeResId = R.style.Theme_FitQuest)

        // Select "Plank" from the spinner
        onView(withId(R.id.spinnerExercise)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("⏱️ Plank"))).perform(click())

        // Check if the UI updated for a time-based exercise
        onView(withId(R.id.labelReps)).check(matches(withText("SECONDS")))
        onView(withId(R.id.tvRepCount)).check(matches(withText("0.0")))
        onView(withId(R.id.tvCurrentExerciseEmoji)).check(matches(withText("⏱️")))
    }

    @Test
    fun test_switch_camera_button_is_clickable() {
        launchFragmentInContainer<AiCoachFragment>(themeResId = R.style.Theme_FitQuest)

        onView(withId(R.id.btnSwitchCamera))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun test_ui_for_scheduled_workout_is_locked() {
        val args = Bundle().apply {
            putInt("scheduleId", 1)
            putString("activityKey", "lunge")
            putInt("repsTarget", 15)
        }
        launchFragmentInContainer<AiCoachFragment>(fragmentArgs = args, themeResId = R.style.Theme_FitQuest)

        // Spinner should be disabled
        onView(withId(R.id.spinnerExercise)).check(matches(not(isEnabled())))

        // Subtitle should show schedule info
        onView(withId(R.id.tvSystemSubtitle)).check(matches(withText("Lunge Target: 15 Reps")))

        // Emoji and label should match the scheduled exercise
        onView(withId(R.id.tvCurrentExerciseEmoji)).check(matches(withText("🦵")))
        onView(withId(R.id.labelReps)).check(matches(withText("REPS")))
    }
}
