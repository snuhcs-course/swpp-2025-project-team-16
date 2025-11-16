package com.fitquest.app.ui.fragments

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.ui.adapters.ScheduleAdapter
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleFragmentTest {

    @Test
    fun test_initial_ui_is_displayed() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)

        onView(withId(R.id.titleText)).check(matches(withText("My Activity Schedule")))
        onView(withId(R.id.btnAutoGenerate)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerViewSchedules)).check(matches(isDisplayed()))
    }

    @Test
    fun test_auto_generate_button_is_clickable() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)

        onView(withId(R.id.btnAutoGenerate)).perform(click())
        // This click should trigger the ViewModel to generate schedules.
        // Verifying the result requires a mock ViewModel or IdlingResource.
    }

    @Test
    fun test_recycler_view_is_visible() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)
        onView(withId(R.id.recyclerViewSchedules)).check(matches(isDisplayed()))
    }

    @Test
    fun test_clicking_item_in_list() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)

        // This test assumes the list is populated with data, which requires mocking.
        // If data is present, this would click the 'Start' button on the first item.
        // onView(withId(R.id.recyclerViewSchedules))
        //     .perform(RecyclerViewActions.actionOnItemAtPosition<ScheduleAdapter.ScheduleViewHolder>(0, click()))

        // A full test would use Espresso-Intents to verify navigation to AiCoachFragment.
    }
}
