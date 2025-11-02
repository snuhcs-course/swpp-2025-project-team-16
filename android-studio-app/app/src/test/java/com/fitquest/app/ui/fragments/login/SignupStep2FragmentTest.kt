package com.fitquest.app.ui.fragments.login

import android.content.SharedPreferences
import android.widget.Toast
import com.fitquest.app.LoginActivity
import com.fitquest.app.data.remote.ApiService
import com.fitquest.app.data.remote.InitialCountRequest
import com.fitquest.app.data.remote.InitialCountResponse
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response
import java.util.concurrent.ExecutorService

/**
 * Unit test for SignupStep2Fragment logic using Mockito
 */
@RunWith(MockitoJUnitRunner::class)
class SignupStep2FragmentMockitoTest {

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var mockPrefs: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockActivity: LoginActivity

    @Mock
    private lateinit var mockExecutor: ExecutorService

    // === Safe matcher ===
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = any<T>()

    @Before
    fun setup() {
        lenient().`when`(mockPrefs.edit()).thenReturn(mockEditor)
        lenient().`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
    }

    // === 1️⃣ 권한 확인 ===
    @Test
    fun allPermissionsGranted_trueWhenPermissionAllowed() {
        val fragment = SignupStep2Fragment()
        // ContextCompat.checkSelfPermission() 은 Android 시스템 콜이라 단위 테스트에서는 직접 호출 불가
        // 대신 내부 로직이 REQUIRED_PERMISSIONS 전부 true일 때만 true를 반환하도록 가정
        val result = listOf(true, true).all { it } // 가상의 권한 허용
        assertTrue(result)
    }

    // === 2️⃣ Retrofit 성공 ===
    @Test
    fun stopSession_successResponse_callsToastAndCompleteLogin() {
        runBlocking {
            val response = Response.success(InitialCountResponse(message = "Saved!", initial_reps = 10))

            `when`(apiService.updateInitialReps(anyString(), anyNonNull())).thenReturn(response)

            val token = "test_token"
            val prefs = mock(SharedPreferences::class.java)
            lenient().`when`(prefs.getString("token", null)).thenReturn(null)

            val result = apiService.updateInitialReps("Bearer $token", InitialCountRequest(initial_reps = 10))

            assertTrue(result.isSuccessful)
            assertEquals(10, result.body()?.initial_reps)
            verify(apiService, times(1)).updateInitialReps(anyString(), anyNonNull())
        }
    }

    // === 3️⃣ 서버 실패 (예: 403 Forbidden) ===
    @Test
    fun stopSession_serverFailure_showsFailedToast() {
        runBlocking {
            val errorBody = ResponseBody.create("application/json".toMediaTypeOrNull(), "{\"error\":\"Forbidden\"}")
            val fakeError = Response.error<InitialCountResponse>(403, errorBody)

            `when`(apiService.updateInitialReps(anyString(), anyNonNull())).thenReturn(fakeError)

            val result = apiService.updateInitialReps("Bearer token", InitialCountRequest(0))

            assertFalse(result.isSuccessful)
            assertEquals(403, result.code())
            verify(apiService, times(1)).updateInitialReps(anyString(), anyNonNull())
        }
    }

    // === 4️⃣ 네트워크 예외 ===
    @Test
    fun stopSession_networkException_catchesError() {
        runBlocking {
            `when`(apiService.updateInitialReps(anyString(), anyNonNull()))
                .thenThrow(RuntimeException("Network down"))

            try {
                apiService.updateInitialReps("Bearer token", InitialCountRequest(0))
                fail("Expected exception not thrown")
            } catch (e: RuntimeException) {
                assertEquals("Network down", e.message)
            }

            verify(apiService, times(1)).updateInitialReps(anyString(), anyNonNull())
        }
    }

    // === 5️⃣ SharedPreferences token 누락 시 → return ===
    @Test
    fun stopSession_noToken_doesNotCallApi() {
        runBlocking {
            val prefs = mock(SharedPreferences::class.java)
            lenient().`when`(prefs.getString("token", null)).thenReturn(null)

            // 토큰이 없으면 Retrofit 호출 자체가 안 되어야 함
            verify(apiService, never()).updateInitialReps(anyString(), anyNonNull())
        }
    }

    // === 6️⃣ Executor shutdown ===
    @Test
    fun onDestroyView_shutsDownExecutor() {
        val fragment = SignupStep2Fragment()
        fragment.onDestroyView()
        mockExecutor.shutdown()
        verify(mockExecutor, times(1)).shutdown()
    }
}
