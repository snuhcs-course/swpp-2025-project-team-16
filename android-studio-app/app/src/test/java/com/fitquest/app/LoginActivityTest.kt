package com.fitquest.app

import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitquest.app.ui.fragments.login.LoginEmailFragment
import com.fitquest.app.ui.fragments.login.LoginPasswordFragment
import com.fitquest.app.ui.fragments.login.SignupStep1Fragment
import com.fitquest.app.ui.fragments.login.SignupStep2Fragment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LoginActivityTest {

    private lateinit var scenario: ActivityScenario<LoginActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(LoginActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `activity should start with LoginEmailFragment`() {
        scenario.onActivity { activity ->
            // Ensure the initial fragment transaction from onCreate is complete
            activity.supportFragmentManager.executePendingTransactions()
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.loginFragmentContainer)
            assertNotNull("Initial fragment should not be null", fragment)
            assertTrue("Initial fragment should be of type LoginEmailFragment", fragment is LoginEmailFragment)
        }
    }

    @Test
    fun `navigateToPasswordStep should display LoginPasswordFragment`() {
        scenario.onActivity { activity ->
            activity.navigateToPasswordStep("test@example.com")
            // Force the fragment transaction to execute synchronously
            activity.supportFragmentManager.executePendingTransactions()

            val fragment = activity.supportFragmentManager.findFragmentById(R.id.loginFragmentContainer)
            assertNotNull("Fragment should not be null after navigating to password step", fragment)
            assertTrue("Fragment should be LoginPasswordFragment", fragment is LoginPasswordFragment)
            assertEquals("Email argument should be passed correctly", "test@example.com", fragment?.arguments?.getString("email"))
        }
    }

    @Test
    fun `navigateToSignupStep1 should display SignupStep1Fragment`() {
        scenario.onActivity { activity ->
            activity.navigateToSignupStep1("new@example.com")
            // Force the fragment transaction to execute synchronously
            activity.supportFragmentManager.executePendingTransactions()

            val fragment = activity.supportFragmentManager.findFragmentById(R.id.loginFragmentContainer)
            assertNotNull("Fragment should not be null after navigating to signup step 1", fragment)
            assertTrue("Fragment should be SignupStep1Fragment", fragment is SignupStep1Fragment)
            assertEquals("Email argument should be passed correctly", "new@example.com", fragment?.arguments?.getString("email"))
        }
    }

    @Test
    fun `navigateToSignupStep2 should display SignupStep2Fragment`() {
        scenario.onActivity { activity ->
            activity.navigateToSignupStep2("new@example.com", "password123", "testuser")
            // Force the fragment transaction to execute synchronously
            activity.supportFragmentManager.executePendingTransactions()

            val fragment = activity.supportFragmentManager.findFragmentById(R.id.loginFragmentContainer)
            assertNotNull("Fragment should not be null after navigating to signup step 2", fragment)
            assertTrue("Fragment should be SignupStep2Fragment", fragment is SignupStep2Fragment)
            assertEquals("Email argument should be correct", "new@example.com", fragment?.arguments?.getString("email"))
            assertEquals("Password argument should be correct", "password123", fragment?.arguments?.getString("password"))
            assertEquals("Username argument should be correct", "testuser", fragment?.arguments?.getString("username"))
        }
    }

    @Test
    fun `completeLogin should start MainActivity and finish`() {
        scenario.onActivity { activity ->
            activity.completeLogin()
            val shadowActivity = Shadows.shadowOf(activity)
            val nextIntent = shadowActivity.nextStartedActivity

            assertNotNull("A new activity should have been started", nextIntent)
            assertEquals("The next activity should be MainActivity", MainActivity::class.java.name, nextIntent.component?.className)
            assertTrue("LoginActivity should be finishing", activity.isFinishing)
        }
    }
}
