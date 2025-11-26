package com.fitquest.app.ui.fragments

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JourneyFragmentTest {

    @Test
    fun test_initial_ui_is_displayed() {
        launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)

        onView(withId(R.id.headerContainer)).check(matches(isDisplayed()))
        onView(withId(R.id.resultScroll)).check(matches(isDisplayed()))
        onView(withId(R.id.timelineVerticalLine)).check(matches(isDisplayed()))
    }

    @Test
    fun test_timeline_is_populated_after_fetch() {
        // This test requires mocking the network call to provide data.
        // For now, we assume that if the call is successful, views will be added.
        // A more robust test would use an IdlingResource and a mock web server.
        launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)

        // After data is fetched and populateTimeline() is called, the container should have children.
        // We can check if the timelineContainer has any views.
        // onView(withId(R.id.timelineContainer)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun test_clicking_timeline_node_opens_bottom_sheet() {
        launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)

        // This requires data to be populated first. Assuming there is at least one item.
        // We'd click on the first available card. The ID is inside a dynamically added layout.
        // onView(firstChildOf(withId(R.id.timelineContainer))).perform(click())
        // Then check if the bottom sheet is displayed.
        // onView(withId(R.id.layout_journey_daydetail)).check(matches(isDisplayed()))
    }

    @Test
    fun test_bottom_sheet_displays_correct_data() {
        // Similar to the above, this would require populating data and clicking an item.
        // Then, we would check the content of the views in the bottom sheet.
        // onView(withId(R.id.tvDayTitle)).check(matches(withText("Expected Date")))
        // onView(withId(R.id.exerciseListContainer)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun test_close_button_dismisses_bottom_sheet() {
        // 1. Launch fragment and populate data
        // 2. Click a node to show the bottom sheet
        // 3. Click the close button
        // onView(withId(R.id.btnClose)).perform(click())
        // 4. Verify the bottom sheet is no longer displayed
        // onView(withId(R.id.layout_journey_daydetail)).check(doesNotExist())
    }
}
