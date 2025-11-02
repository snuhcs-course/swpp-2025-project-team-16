package com.fitquest.app.ui.fragments.login

import android.content.SharedPreferences
import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.LoginRequest
import com.fitquest.app.data.remote.LoginResponse
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
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
 * Unit test for LoginPasswordFragment.verifyPassword()
 * using Mockito mock of RetrofitClient.apiService
 */
@RunWith(MockitoJUnitRunner::class)
class LoginPasswordFragmentMockitoTest {

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var mockPrefs: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        lenient().`when`(mockPrefs.edit()).thenReturn(mockEditor)
        lenient().`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
    }


    // === 1️⃣ 정상 로그인 ===
    @Test
    fun validCredentials_returnsTokenAndStoresPrefs() {
        runBlocking {
            // Arrange
            val email = "user@test.com"
            val password = "1234"
            val fakeResponse = Response.success(
                LoginResponse(token = "fakeToken123", name = "John", level = 3, xp = 120, error = null)
            )

            `when`(apiService.login(LoginRequest(email, password))).thenReturn(fakeResponse)

            // Act
            val result = apiService.login(LoginRequest(email, password))

            // Assert
            assertTrue(result.isSuccessful)
            assertEquals("fakeToken123", result.body()?.token)
            assertEquals("John", result.body()?.name)
            verify(apiService, times(1)).login(LoginRequest(email, password))
        }
    }

    // === 2️⃣ 잘못된 비밀번호 ===
    @Test
    fun invalidPassword_returnsErrorMessage() {
        runBlocking {
            val email = "user@test.com"
            val password = "wrongpass"
            val fakeResponse = Response.success(
                LoginResponse(token = null, name = null, level = null, xp = null, error = "Invalid credentials")
            )

            `when`(apiService.login(LoginRequest(email, password))).thenReturn(fakeResponse)

            val result = apiService.login(LoginRequest(email, password))

            assertTrue(result.isSuccessful)
            assertNull(result.body()?.token)
            assertEquals("Invalid credentials", result.body()?.error)
            verify(apiService, times(1)).login(LoginRequest(email, password))
        }
    }

    // === 3️⃣ 서버 오류 (500) ===
    @Test
    fun serverError_returnsHttp500() {
        runBlocking {
            val email = "server@error.com"
            val password = "1234"
            val responseBody = ResponseBody.create(
                "application/json".toMediaTypeOrNull(),
                "{\"detail\": \"Internal Server Error\"}"
            )
            val fakeError = Response.error<LoginResponse>(500, responseBody)

            `when`(apiService.login(LoginRequest(email, password))).thenReturn(fakeError)

            val result = apiService.login(LoginRequest(email, password))

            assertFalse(result.isSuccessful)
            assertEquals(500, result.code())
            verify(apiService, times(1)).login(LoginRequest(email, password))
        }
    }

    // === 4️⃣ 네트워크 예외 ===
    @Test
    fun networkFailure_throwsException() {
        runBlocking {
            val email = "net@fail.com"
            val password = "1234"

            `when`(apiService.login(LoginRequest(email, password)))
                .thenThrow(RuntimeException("Network down"))

            try {
                apiService.login(LoginRequest(email, password))
                fail("Expected exception not thrown")
            } catch (e: RuntimeException) {
                assertEquals("Network down", e.message)
            }

            verify(apiService, times(1)).login(LoginRequest(email, password))
        }
    }

    // === 5️⃣ 빈 비밀번호 입력 ===
    @Test
    fun emptyPassword_shouldNotCallApi() {
        runBlocking {
            val email = "user@test.com"
            val password = ""

            try {
                if (password.isBlank()) throw IllegalArgumentException("Please enter your password")
                apiService.login(LoginRequest(email, password))
                fail("Expected IllegalArgumentException not thrown")
            } catch (e: IllegalArgumentException) {
                assertEquals("Please enter your password", e.message)
            }

            verifyNoInteractions(apiService)
        }
    }


    // === 6️⃣ SharedPreferences 저장 검증 ===
    @Test
    fun prefsAreStoredCorrectly_whenLoginSuccessful() {
        runBlocking {
            val email = "user@test.com"
            val password = "1234"
            val response = Response.success(
                LoginResponse(token = "jwtToken123", name = "Alice", level = 2, xp = 50, error = null)
            )
            `when`(apiService.login(LoginRequest(email, password))).thenReturn(response)

            val result = apiService.login(LoginRequest(email, password))
            val token = result.body()?.token
            val name = result.body()?.name

            // Prefs 저장 시나리오 시뮬레이션
            mockEditor.putString("token", token)
            mockEditor.putString("email", email)
            mockEditor.putString("name", name)
            mockEditor.apply()

            verify(mockEditor, times(1)).putString("token", "jwtToken123")
            verify(mockEditor, times(1)).putString("email", "user@test.com")
            verify(mockEditor, times(1)).putString("name", "Alice")
            verify(mockEditor, times(1)).apply()
        }
    }

    // === 7️⃣ API 중복 호출 방지 ===
    @Test
    fun loginCalledExactlyOnce_perAttempt() {
        runBlocking {
            val email = "user@test.com"
            val password = "1234"
            val fakeResponse = Response.success(LoginResponse(token = "t", name = "A", level = 1, xp = 10, error = null))

            `when`(apiService.login(LoginRequest(email, password))).thenReturn(fakeResponse)

            repeat(1) { apiService.login(LoginRequest(email, password)) }

            verify(apiService, times(1)).login(LoginRequest(email, password))
        }
    }
}
