package com.fitquest.app.ui.fragments

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.ScheduleApiService
import com.fitquest.app.data.remote.ScheduleResponse
import com.fitquest.app.model.Exercise
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
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
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.LocalDate

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ScheduleFragmentTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApiService: ScheduleApiService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        // By mocking the ApiService, we control the dependency of the real ViewModel
        // RetrofitClient.scheduleApiService = mockApiService
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
    fun `onViewCreated initializes views and loads initial schedules`() = runTest {
        setAuthToken("fake-token")
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.success(emptyList()))

        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        Mockito.verify(mockApiService).getUserSchedules("Bearer fake-token")
    }

    @Test
    fun `exercise list updates UI correctly`() = runTest {
        setAuthToken("fake-token")
        val today = LocalDate.now().toString()
        val mockExercises = listOf(Exercise(name = "Push-ups", detail = "10 reps", status = "todo"))
        val mockResponse = listOf(ScheduleResponse(id = "id1", date = today, exercises = mockExercises, startTime = "08:00", finishTime = "09:00", point = 100, isCompleted = false, feedback = ""))
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.success(mockResponse))

        // 1. Test with data
        val scenario = launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment {
            val recyclerView = it.view?.findViewById<RecyclerView>(R.id.exercise_recycler_view)
            val emptyState = it.view?.findViewById<View>(R.id.emptyState)
            assertEquals("RecyclerView should be visible when there is data", View.VISIBLE, recyclerView?.visibility)
            assertEquals("Empty state should be gone when there is data", View.GONE, emptyState?.visibility)
        }
    }

    @Test
    fun `UI is empty when schedule list is empty`() = runTest {
        setAuthToken("fake-token")
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.success(emptyList()))

        // Test with empty data
        val scenario = launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle()

        scenario.onFragment {
            val recyclerView = it.view?.findViewById<RecyclerView>(R.id.exercise_recycler_view)
            val emptyState = it.view?.findViewById<View>(R.id.emptyState)
            assertEquals("RecyclerView should be gone when data is empty", View.GONE, recyclerView?.visibility)
            assertEquals("Empty state should be visible when data is empty", View.VISIBLE, emptyState?.visibility)
        }
    }

    @Test
    fun `autoGenerateButton calls API correctly`() = runTest {
        setAuthToken("fake-token")
        // Mock the initial load and the generation calls
        whenever(mockApiService.getUserSchedules(any())).thenReturn(Response.success(emptyList()))
        whenever(mockApiService.generateUserSchedules(any(), any())).thenReturn(Response.success("SUCCESS"))

        val scenario = launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_FitQuest)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for initial load

        scenario.onFragment {
            it.view?.findViewById<Button>(R.id.auto_generate_button)?.performClick()
        }
        testDispatcher.scheduler.advanceUntilIdle() // Wait for generation coroutines to finish

        // The ViewModel's generateSchedule function loops 31 times
        Mockito.verify(mockApiService, Mockito.times(31)).generateUserSchedules(eq("Bearer fake-token"), any())
    }
}
