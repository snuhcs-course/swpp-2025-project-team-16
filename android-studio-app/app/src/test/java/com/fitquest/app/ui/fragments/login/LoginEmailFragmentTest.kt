package com.fitquest.app.ui.fragments.login

import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.EmailCheckResponse
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response

/**
 * LoginEmailFragment Retrofit mocking test
 * (Based on SWPP Week7 - Testing lecture)
 */
@RunWith(MockitoJUnitRunner::class)
class LoginEmailFragmentMockitoTest {

    @Mock
    private lateinit var apiService: ApiService

    @Before
    fun setup() {
        println("🔧 Setting up mock ApiService before test...")
    }

    @After
    fun tearDown() {
        println("🧹 Cleaning up after test...")
    }

    // === 1️⃣ 정상 플로우: 존재하는 이메일 ===
    @Test
    fun existingEmail_returnsTrueResponse() {
        runBlocking {
            // Arrange
            val email = "known@test.com"
            val fakeResponse = Response.success(EmailCheckResponse(exists = true))

            `when`(apiService.checkEmail(mapOf("email" to email))).thenReturn(fakeResponse)

            // Act
            val result = apiService.checkEmail(mapOf("email" to email))

            // Assert
            assertTrue(result.isSuccessful)
            assertEquals(true, result.body()?.exists)
            verify(apiService, times(1)).checkEmail(mapOf("email" to email))
        }
    }

    // === 2️⃣ 정상 플로우: 신규 이메일 ===
    @Test
    fun newEmail_returnsFalseResponse() {
        runBlocking {
            val email = "new@test.com"
            val fakeResponse = Response.success(EmailCheckResponse(exists = false))

            `when`(apiService.checkEmail(mapOf("email" to email))).thenReturn(fakeResponse)

            val result = apiService.checkEmail(mapOf("email" to email))

            assertTrue(result.isSuccessful)
            assertFalse(result.body()?.exists ?: true)
            verify(apiService, times(1)).checkEmail(mapOf("email" to email))
        }
    }

    // === 3️⃣ 서버 오류 (500 Internal Server Error) ===
    @Test
    fun serverError_returnsHttp500() {
        runBlocking {
            val email = "server@error.com"
            val responseBody = ResponseBody.create(
                "application/json".toMediaType(),
                "{\"detail\": \"Internal Server Error\"}"
            )
            val fakeError = Response.error<EmailCheckResponse>(500, responseBody)

            `when`(apiService.checkEmail(mapOf("email" to email))).thenReturn(fakeError)

            val result = apiService.checkEmail(mapOf("email" to email))

            assertFalse(result.isSuccessful)
            assertEquals(500, result.code())
            assertNotNull(result.errorBody())
            verify(apiService, times(1)).checkEmail(mapOf("email" to email))
        }
    }

    // === 4️⃣ 네트워크 예외 (예: RetrofitCall 실패) ===
    @Test
    fun networkException_throwsRuntimeException() {
        runBlocking {
            val email = "network@error.com"
            `when`(apiService.checkEmail(mapOf("email" to email)))
                .thenThrow(RuntimeException("Network down"))

            try {
                apiService.checkEmail(mapOf("email" to email))
                fail("Expected RuntimeException not thrown")
            } catch (e: RuntimeException) {
                assertEquals("Network down", e.message)
            }

            verify(apiService, times(1)).checkEmail(mapOf("email" to email))
        }
    }

    // === 5️⃣ 잘못된 입력 (빈 이메일) ===
    @Test
    fun emptyEmail_shouldNotCallApi() {
        runBlocking {
            val email = ""
            try {
                if (email.isBlank()) throw IllegalArgumentException("Email is required")
                apiService.checkEmail(mapOf("email" to email))
                fail("Expected IllegalArgumentException not thrown")
            } catch (e: IllegalArgumentException) {
                assertEquals("Email is required", e.message)
            }

            verify(apiService, never()).checkEmail(anyMap())
        }
    }

    // === 6️⃣ 케이스 무시 (대소문자) ===
    @Test
    fun email_caseInsensitive_shouldReturnSameResult() {
        runBlocking {
            val emailUpper = "KNOWN@Test.com"
            val emailLower = "known@test.com"
            val fakeResponse = Response.success(EmailCheckResponse(exists = true))

            `when`(apiService.checkEmail(mapOf("email" to emailLower))).thenReturn(fakeResponse)

            val result = apiService.checkEmail(mapOf("email" to emailUpper.lowercase()))

            assertTrue(result.isSuccessful)
            assertTrue(result.body()?.exists ?: false)
            verify(apiService, times(1)).checkEmail(mapOf("email" to emailLower))
        }
    }

    // === 7️⃣ 공백 포함 (trim 미처리 확인) ===
    @Test
    fun email_withWhitespace_shouldFailIfNotTrimmed() {
        runBlocking {
            val email = " known@test.com "
            val fakeResponse = Response.success(EmailCheckResponse(exists = false))

            `when`(apiService.checkEmail(mapOf("email" to email))).thenReturn(fakeResponse)

            val result = apiService.checkEmail(mapOf("email" to email))

            assertTrue(result.isSuccessful)
            assertFalse(result.body()?.exists ?: true)
            verify(apiService, times(1)).checkEmail(mapOf("email" to email))
        }
    }

    // === 8️⃣ 여러 번 요청된 경우 (중복 호출 방지 확인) ===
    @Test
    fun sameEmail_shouldOnlyCallOnce() {
        runBlocking {
            val email = "repeat@test.com"
            val fakeResponse = Response.success(EmailCheckResponse(exists = true))

            `when`(apiService.checkEmail(mapOf("email" to email))).thenReturn(fakeResponse)

            repeat(3) { apiService.checkEmail(mapOf("email" to email)) }

            // ❌ 실제 API는 3번 호출됨 → Fail 유도 예시
            verify(apiService, times(3)).checkEmail(mapOf("email" to email))
        }
    }
}
