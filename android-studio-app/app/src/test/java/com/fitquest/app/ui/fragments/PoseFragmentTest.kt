package com.fitquest.app.ui.fragments

import android.os.Build
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.R
import com.fitquest.app.MainActivity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Field

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class PoseFragmentTest {

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var fragment: PoseFragment

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java).onActivity {
            fragment = PoseFragment()
            it.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow()
        }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `onViewCreated should initialize views`() {
        scenario.onActivity {
            assertNotNull(fragment.view?.findViewById<PreviewView>(R.id.cameraPreview))
            assertNotNull(fragment.view?.findViewById<ImageButton>(R.id.btnCapture))
            assertNotNull(fragment.view?.findViewById<ImageButton>(R.id.btnSwitchCamera))
            assertNotNull(fragment.view?.findViewById<TextView>(R.id.tvCountdown))
        }
    }

    @Test
    fun `initial UI state should be correct`() {
        scenario.onActivity {
            val countdown = fragment.view?.findViewById<TextView>(R.id.tvCountdown)
            assertEquals(View.GONE, countdown?.visibility)
        }
    }

    @Test
    fun `toggleCamera should switch lens facing`() {
        scenario.onActivity {
            val initialLensFacing = fragment.getPrivateField<Int>("lensFacing")
            assertEquals(CameraSelector.LENS_FACING_FRONT, initialLensFacing)

            // Call the private method toggleCamera()
            fragment.javaClass.getDeclaredMethod("toggleCamera").apply {
                isAccessible = true
                invoke(fragment)
            }

            val toggledLensFacing = fragment.getPrivateField<Int>("lensFacing")
            assertEquals(CameraSelector.LENS_FACING_BACK, toggledLensFacing)

            // Toggle back
            fragment.javaClass.getDeclaredMethod("toggleCamera").apply {
                isAccessible = true
                invoke(fragment)
            }

            val finalLensFacing = fragment.getPrivateField<Int>("lensFacing")
            assertEquals(CameraSelector.LENS_FACING_FRONT, finalLensFacing)
        }
    }

    private fun <T> Any.getPrivateField(name: String): T {
        val field: Field = this.javaClass.getDeclaredField(name)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }
}
