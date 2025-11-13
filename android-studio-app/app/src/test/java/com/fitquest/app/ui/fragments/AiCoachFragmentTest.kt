package com.fitquest.app.ui.fragments

import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.MainActivity
import com.fitquest.app.ui.viewmodels.AiCoachViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AiCoachFragmentTest {

    private lateinit var fragment: AiCoachFragment
    private lateinit var activity: MainActivity
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val viewModel: AiCoachViewModel = mock(AiCoachViewModel::class.java)

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            activity = it
            fragment = AiCoachFragment()
            activity.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow()
        }
        fragment.activityViewModels<AiCoachViewModel>().value.let {
            // Manually inject the mock ViewModel
        }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `onViewCreated should initialize views`() {
        assertNotNull(fragment.view)
        assertNotNull(fragment.view?.findViewById<View>(R.id.cameraPreview))
        assertNotNull(fragment.view?.findViewById<Button>(R.id.btnStartWorkout))
    }

    @Test
    fun `beginWorkout should start training state`() {
        fragment.view?.findViewById<Button>(R.id.btnStartWorkout)?.performClick()

        // Need to manually call methods for test due to countdown
        fragment.send { beginWorkout() }

        assertTrue(fragment.send { isTraining })
        assertEquals(0, fragment.send { repCount })
        verify(viewModel).beginTraining()
        val startPauseButton = fragment.view?.findViewById<Button>(R.id.btnStartWorkout)
        assertEquals("Pause Training", startPauseButton?.text)
    }


    @Test
    fun `pauseWorkout should stop training state`() {
        fragment.send { beginWorkout() } // Start first
        fragment.view?.findViewById<Button>(R.id.btnStartWorkout)?.performClick()

        assertFalse(fragment.send { isTraining })
        verify(viewModel).pauseTraining()
        val startPauseButton = fragment.view?.findViewById<Button>(R.id.btnStartWorkout)
        assertEquals("Begin Training", startPauseButton?.text)
    }


    @Test
    fun `updateRepCount should update UI and ViewModel`() {
        fragment.send { updateRepCount(5) }

        assertEquals(5, fragment.send { repCount })
        assertEquals(50, fragment.send { points })
        verify(viewModel).updateRepCount(5)

        val repCountText = fragment.view?.findViewById<TextView>(R.id.tvRepCount)
        val pointsText = fragment.view?.findViewById<TextView>(R.id.tvXpPoints)
        assertEquals("5", repCountText?.text)
        assertEquals("+50", pointsText?.text)
    }

    // Helper to access private members for testing
    private fun <T> AiCoachFragment.send(block: AiCoachFragment.() -> T): T {
        return block()
    }
}
