package com.fitquest.app.ui.fragments

import android.content.Context
import android.os.Build
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.data.remote.HistoryResponse
import com.fitquest.app.data.remote.ProfileApiService
import com.fitquest.app.data.remote.RankResponse
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.UserStatsResponse
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.LocalDate

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ProfileFragmentTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApiService: ProfileApiService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        // RetrofitClient.profileApiService = mockApiService

        // Mock a successful response for getUserStats, as it's called in onViewCreated.
        runTest {
            val mockStats = UserStatsResponse(rank = 1, level = 5, total_time = "10h 30m", xp = 1500)
            whenever(mockApiService.getUserStats(any())).thenReturn(Response.success(mockStats))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setAuthToken(token: String?) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().putString("token", token).commit()
    }

    @Test
    fun `onViewCreated initializes views and triggers fetches`() = runTest {
        setAuthToken("fake-token")
        whenever(mockApiService.getUserHistory(any())).thenReturn(Response.success(emptyList()))

        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment {
            assertNotNull(it.view?.findViewById<LinearLayout>(R.id.historyContainer))
            verifyBlocking(mockApiService) { getUserHistory("Bearer fake-token") }
            verifyBlocking(mockApiService) { getUserStats("Bearer fake-token") }
        }
    }

    @Test
    fun `btnViewRankings click shows overlay and fetches rank data`() = runTest {
        setAuthToken("fake-token")
        whenever(mockApiService.getRankings(any())).thenReturn(Response.success(emptyList()))

        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment {
            val rankButton = it.view?.findViewById<MaterialButton>(R.id.btnViewRankings)
            rankButton?.performClick()
            testDispatcher.scheduler.advanceUntilIdle()
            verifyBlocking(mockApiService) { getRankings("Bearer fake-token") }
        }
    }

    @Test
    fun `history data is fetched and displayed correctly`() = runTest {
        setAuthToken("fake-token")
        val pastDate = LocalDate.now().minusDays(5).toString()
        val historyData = listOf(
            HistoryResponse(id = 1, date = pastDate, name = "Push-up", is_finished = true, start_time = "10:00", end_time = "10:30")
        )
        whenever(mockApiService.getUserHistory(any())).thenReturn(Response.success(historyData))

        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment {
            val historyContainer = it.view?.findViewById<LinearLayout>(R.id.historyContainer)
            assertEquals("History item from the past should be displayed", 1, historyContainer?.childCount)
        }
    }

    @Test
    fun `rank data is fetched and displayed correctly`() = runTest {
        setAuthToken("fake-token")
        val rankData = listOf(
            RankResponse(rank = 1, name = "Alice", xp = 1000, level = 5),
            RankResponse(rank = 2, name = "Bob", xp = 900, level = 4)
        )
        whenever(mockApiService.getRankings(any())).thenReturn(Response.success(rankData))

        val scenario = launchFragmentInContainer<ProfileFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment {
            it.view?.findViewById<MaterialButton>(R.id.btnViewRankings)?.performClick()
            testDispatcher.scheduler.advanceUntilIdle()

            val firstName = it.view?.findViewById<TextView>(R.id.tvFirstName)
            assertEquals("1st • Alice", firstName?.text)
        }
    }
}
