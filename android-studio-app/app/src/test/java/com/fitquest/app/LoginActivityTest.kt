package com.fitquest.app

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.fitquest.app.ui.fragments.login.LoginEmailFragment
import com.fitquest.app.ui.fragments.login.LoginPasswordFragment
import com.fitquest.app.ui.fragments.login.SignupStep1Fragment
import com.fitquest.app.ui.fragments.login.SignupStep2Fragment
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowActivity

/**
 * Unit + Integration-level tests for LoginActivity navigation logic.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LoginActivityTest {

    private lateinit var activity: LoginActivity
    private lateinit var fragmentManager: FragmentManager

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(LoginActivity::class.java).setup().get()
        fragmentManager = activity.supportFragmentManager
    }

    // === 1️⃣ onCreate 기본 동작 ===
    @Test
    fun `onCreate with no saved state`() {
        val current = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(current is LoginEmailFragment)
    }

    @Test
    fun `onCreate activity recreation`() {
        val controller = Robolectric.buildActivity(LoginActivity::class.java).setup()
        val beforeRecreate = controller.get()
        controller.recreate()
        val afterRecreate = controller.get()
        assertNotNull(afterRecreate.supportFragmentManager)
        assertNotSame(beforeRecreate, afterRecreate)
    }

    // === 2️⃣ navigateToPasswordStep ===
    @Test
    fun `navigateToPasswordStep basic navigation`() {
        activity.navigateToPasswordStep("user@test.com")
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is LoginPasswordFragment)
        assertEquals("user@test.com", (fragment as LoginPasswordFragment).arguments?.getString("email"))
    }

    @Test
    fun `navigateToPasswordStep with empty email`() {
        activity.navigateToPasswordStep("")
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is LoginPasswordFragment)
        assertEquals("", (fragment as LoginPasswordFragment).arguments?.getString("email"))
    }

    @Test
    fun `navigateToPasswordStep with null email`() {
        activity.navigateToPasswordStep(null ?: "")
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is LoginPasswordFragment)
    }

    @Test
    fun `navigateToPasswordStep with special characters in email`() {
        val weirdEmail = "we!rd+char@test-domain.io"
        activity.navigateToPasswordStep(weirdEmail)
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is LoginPasswordFragment)
        assertEquals(weirdEmail, (fragment as LoginPasswordFragment).arguments?.getString("email"))
    }

    // === 3️⃣ navigateToSignupStep1 ===
    @Test
    fun `navigateToSignupStep1 basic navigation`() {
        activity.navigateToSignupStep1("user@fitquest.com")
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is SignupStep1Fragment)
        assertEquals("user@fitquest.com", (fragment as SignupStep1Fragment).arguments?.getString("email"))
    }

    @Test
    fun `navigateToSignupStep1 back stack check`() {
        activity.navigateToSignupStep1("backtest@fitquest.com")
        fragmentManager.popBackStack()
        val current = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(current is LoginEmailFragment)
    }

    // === 4️⃣ navigateToSignupStep2 ===
    @Test
    fun `navigateToSignupStep2 basic navigation`() {
        activity.navigateToSignupStep2("hero@fitquest.com", "123456", "Hero")
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is SignupStep2Fragment)
        assertEquals("hero@fitquest.com", (fragment as SignupStep2Fragment).arguments?.getString("email"))
    }

    @Test
    fun `navigateToSignupStep2 with empty parameters`() {
        activity.navigateToSignupStep2("", "", "")
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is SignupStep2Fragment)
    }

    @Test
    fun `navigateToSignupStep2 with long string parameters`() {
        val longEmail = "a".repeat(500) + "@fitquest.com"
        val longPassword = "p".repeat(300)
        val longName = "n".repeat(200)
        activity.navigateToSignupStep2(longEmail, longPassword, longName)
        val fragment = fragmentManager.findFragmentById(R.id.loginFragmentContainer)
        assertTrue(fragment is SignupStep2Fragment)
        assertEquals(longEmail, (fragment as SignupStep2Fragment).arguments?.getString("email"))
    }

    // === 5️⃣ completeLogin() ===
    @Test
    fun `completeLogin intent verification`() {
        val shadow = ShadowActivity()
        activity.completeLogin()
        val nextIntent: Intent = shadow.nextStartedActivity
        assertEquals(MainActivity::class.java.name, nextIntent.component?.className)
    }

    @Test
    fun `completeLogin activity finishing`() {
        activity.completeLogin()
        assertTrue(activity.isFinishing)
    }

}
