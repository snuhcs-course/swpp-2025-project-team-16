package com.fitquest.app.ui.fragments

import android.content.Context
import android.os.Build
import android.widget.LinearLayout
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.data.remote.ExerciseResponse
import com.fitquest.app.data.remote.JourneyApiService
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.WorkoutDayResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.LocalDate

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class JourneyFragmentTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApiService: JourneyApiService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        // RetrofitClient.journeyApiService = mockApiService
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
    fun `onCreateView inflates layout successfully`() {
        val scenario = launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)
        scenario.onFragment { fragment ->
            assertNotNull("View should be inflated", fragment.view)
        }
    }

    @Test
    fun `onViewCreated initializes views`() {
        val scenario = launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)
        scenario.onFragment { fragment ->
            val timelineContainer = fragment.view?.findViewById<LinearLayout>(R.id.timelineContainer)
            assertNotNull("Timeline container should be initialized", timelineContainer)
        }
    }

    @Test
    fun `fetchScheduleFromServer does not call API when no auth token`() = runTest {
        setAuthToken(null)

        launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        Mockito.verify(mockApiService, Mockito.never()).getUserSchedules(any())
    }

    @Test
    fun `fetchScheduleFromServer populates timeline on successful response`() = runTest {
        setAuthToken("fake-token")
        val today = LocalDate.now().toString()
        val mockData = listOf(
            WorkoutDayResponse(today, 100, listOf(ExerciseResponse("Push-up", "10 reps", "Done")))
        )
        whenever(mockApiService.getUserSchedules("Bearer fake-token")).thenReturn(Response.success(mockData))

        val scenario = launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment { fragment ->
            val timeline = fragment.view?.findViewById<LinearLayout>(R.id.timelineContainer)
            assertEquals("Timeline should have 1 child for one workout day", 1, timeline?.childCount)
        }
    }

    @Test
    fun `fetchScheduleFromServer filters out past dates`() = runTest {
        setAuthToken("fake-token")
        val today = LocalDate.now()
        val pastDate = today.minusDays(1).toString()
        val futureDate = today.plusDays(1).toString()
        val mockData = listOf(
            WorkoutDayResponse(pastDate, 50, emptyList()),
            WorkoutDayResponse(today.toString(), 100, emptyList()),
            WorkoutDayResponse(futureDate, 150, emptyList())
        )
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.success(mockData))

        val scenario = launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment { fragment ->
            val timeline = fragment.view?.findViewById<LinearLayout>(R.id.timelineContainer)
            assertEquals("Only today and future dates should be shown", 2, timeline?.childCount)
        }
    }

    @Test
    fun `fetchScheduleFromServer handles API error response`() = runTest {
        setAuthToken("fake-token")
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.error(404, "Not Found".toResponseBody()))

        val scenario = launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment { fragment ->
            val timeline = fragment.view?.findViewById<LinearLayout>(R.id.timelineContainer)
            assertEquals("Timeline should be empty on API error", 0, timeline?.childCount)
        }
    }

    @Test
    fun `fetchScheduleFromServer handles network exception`() = runTest {
        setAuthToken("fake-token")
        whenever(mockApiService.getUserSchedules(any())).thenThrow(RuntimeException("Network failed"))

        val scenario = launchFragmentInContainer<JourneyFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment { fragment ->
            val timeline = fragment.view?.findViewById<LinearLayout>(R.id.timelineContainer)
            assertEquals("Timeline should be empty on network exception", 0, timeline?.childCount)
        }
    }
}
