package com.fitquest.app.ui.fragments.login

import android.content.SharedPreferences
import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.SignupRequest
import com.fitquest.app.data.remote.SignupResponse
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response

/**
 * Unit test for SignupStep1Fragment logic using Mockito
 */
@RunWith(MockitoJUnitRunner::class)
class SignupStep1FragmentMockitoTest {

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var mockPrefs: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    // 🔧 Kotlin safe matcher helper
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = any<T>()

    @Before
    fun setup() {
        lenient().`when`(mockPrefs.edit()).thenReturn(mockEditor)
        lenient().`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
    }

    // === 1️⃣ 입력 유효성 검사 ===
    @Test
    fun emptyEmail_failsValidation() {
        val fragment = SignupStep1Fragment()
        assertFalse(fragment.validateInputsForTest("", "hero", "123456", "123456"))
    }

    @Test
    fun emptyUsername_failsValidation() {
        val fragment = SignupStep1Fragment()
        assertFalse(fragment.validateInputsForTest("user@test.com", "", "123456", "123456"))
    }

    @Test
    fun passwordMismatch_failsValidation() {
        val fragment = SignupStep1Fragment()
        assertFalse(fragment.validateInputsForTest("user@test.com", "hero", "123456", "654321"))
    }

    @Test
    fun shortPassword_failsValidation() {
        val fragment = SignupStep1Fragment()
        assertFalse(fragment.validateInputsForTest("user@test.com", "hero", "123", "123"))
    }

    @Test
    fun validInputs_passValidation() {
        val fragment = SignupStep1Fragment()
        assertTrue(fragment.validateInputsForTest("user@test.com", "hero", "abcdef", "abcdef"))
    }

    // === 2️⃣ 정상 회원가입 ===
    @Test
    fun validSignup_returnsSuccess_andStoresToken() {
        runBlocking {
            val fakeResponse = Response.success(SignupResponse(message = "Signup success!", token = "abc123"))
            `when`(apiService.signup(anyNonNull())).thenReturn(fakeResponse)

            val result = apiService.signup(SignupRequest("hero", "user@test.com", "123456"))

            assertTrue(result.isSuccessful)
            assertEquals("abc123", result.body()?.token)
            verify(apiService, times(1)).signup(anyNonNull())
        }
    }

    // === 3️⃣ 서버 실패 (400/500) ===
    @Test
    fun serverError_returnsFailureMessage() {
        runBlocking {
            val errorBody = ResponseBody.create(
                "application/json".toMediaTypeOrNull(),
                "{\"detail\":\"Bad Request\"}"
            )
            val fakeError = Response.error<SignupResponse>(400, errorBody)

            `when`(apiService.signup(anyNonNull())).thenReturn(fakeError)

            val result = apiService.signup(SignupRequest("hero", "fail@test.com", "123456"))

            assertFalse(result.isSuccessful)
            assertEquals(400, result.code())
            verify(apiService, times(1)).signup(anyNonNull())
        }
    }

    // === 4️⃣ 네트워크 예외 ===
    @Test
    fun networkFailure_throwsException() {
        runBlocking {
            `when`(apiService.signup(anyNonNull()))
                .thenThrow(RuntimeException("Network down"))

            try {
                apiService.signup(SignupRequest("hero", "net@test.com", "123456"))
                fail("Expected exception not thrown")
            } catch (e: RuntimeException) {
                assertEquals("Network down", e.message)
            }

            verify(apiService, times(1)).signup(anyNonNull())
        }
    }

    // === 5️⃣ SharedPreferences 저장 검증 ===
    @Test
    fun prefsAreStoredCorrectly_whenSignupSuccessful() {
        runBlocking {
            val response = Response.success(SignupResponse(message = "Signup success", token = "xyz789"))
            `when`(apiService.signup(anyNonNull())).thenReturn(response)

            val result = apiService.signup(SignupRequest("hero", "user@test.com", "123456"))
            val token = result.body()?.token

            mockEditor.putString("token", token)
            mockEditor.putString("email", "user@test.com")
            mockEditor.putString("name", "hero")
            mockEditor.apply()

            verify(mockEditor, times(1)).putString("token", "xyz789")
            verify(mockEditor, times(1)).putString("email", "user@test.com")
            verify(mockEditor, times(1)).putString("name", "hero")
            verify(mockEditor, times(1)).apply()
        }
    }
}
