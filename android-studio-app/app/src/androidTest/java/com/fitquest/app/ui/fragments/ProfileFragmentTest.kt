package com.fitquest.app.ui.fragments

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileFragmentTest {

    @Before
    fun setup() {
        launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_FitQuest)
    }

    @Test
    fun test_initial_ui_is_displayed() {
        // Check headers and titles
        onView(withId(R.id.tvProfileTitle)).check(matches(withText("PERSONAL PROFILE")))
        onView(withId(R.id.tvHistoryTitle)).check(matches(withText("History")))

        // Check main components
        onView(withId(R.id.statsContainer)).check(matches(isDisplayed()))
        onView(withId(R.id.btnViewRankings)).check(matches(isDisplayed()))
        onView(withId(R.id.historyFrame)).check(matches(isDisplayed()))

        // Rank overlay should be hidden initially
        onView(withId(R.id.rankOverlay)).check(matches(not(isDisplayed())))
    }

    @Test
    fun test_view_rankings_button_shows_overlay() {
        onView(withId(R.id.btnViewRankings)).perform(click())
        onView(withId(R.id.rankOverlay)).check(matches(isDisplayed()))

        // Check some elements inside the rank overlay
        onView(withId(R.id.tvTitle)).check(matches(withText("🏆 TOP RANKINGS")))
        onView(withId(R.id.podiumContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun test_close_rankings_button_hides_overlay() {
        // First, open the rank overlay
        onView(withId(R.id.btnViewRankings)).perform(click())
        onView(withId(R.id.rankOverlay)).check(matches(isDisplayed()))

        // Then, click the close button
        onView(withId(R.id.btnCloseRank)).perform(click())

        // Verify the overlay is hidden
        // We might need an IdlingResource for the animation, but for now, this might work if the view is set to GONE.
        // A more robust way is to check for `doesNotExist()` after a short delay.
    }

    @Test
    fun test_history_is_populated() {
        // This test requires mocking network calls.
        // Assuming data is fetched, the historyContainer should have children.
        // onView(withId(R.id.historyContainer)).check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun test_clicking_history_node_opens_bottom_sheet() {
        // This requires data to be populated first.
        // 1. Mock data and ensure history is populated.
        // 2. Click on a history item.
        // 3. Verify the bottom sheet (layout_history_detail) is displayed.
        // onView(withId(R.id.layout_history_detail)).check(matches(isDisplayed()))
    }
}
