package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.login.*
import com.fitquest.app.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        repository = mock()
        viewModel = AuthViewModel(repository)
    }

    // ---------------------- SIGNUP ----------------------

    @Test
    fun `signup returns Success when token exists`() = runTest {
        val request = SignupRequest("test@test.com", "1234", "kim")
        val response = SignupResponse("Signup success!", "valid_token")
        whenever(repository.signup(any())).thenReturn(Response.success(response))

        viewModel.signup(request)

        advanceUntilIdle()

        val result = viewModel.signupResult.value
        Assert.assertTrue(result is NetworkResult.Success)
        Assert.assertEquals("valid_token", (result as NetworkResult.Success).data.token)
    }

    @Test
    fun `signup returns ServerError when token is empty`() = runTest {
        val request = SignupRequest("test@test.com", "1234", "kim")
        val response = SignupResponse("Signup success!", null)
        whenever(repository.signup(any())).thenReturn(Response.success(response))

        viewModel.signup(request)

        advanceUntilIdle()

        val result = viewModel.signupResult.value
        Assert.assertTrue(result is NetworkResult.ServerError)
    }

    // ---------------------- LOGIN ----------------------

    @Test
    fun `login returns Success when token exists`() = runTest {
        val request = LoginRequest("test@test.com", "1234")
        val response = LoginResponse("login_token", "kim", 1, 100, null)
        whenever(repository.login(any())).thenReturn(Response.success(response))

        viewModel.login(request)

        advanceUntilIdle()

        val result = viewModel.loginResult.value
        Assert.assertTrue(result is NetworkResult.Success)
        Assert.assertEquals("login_token", (result as NetworkResult.Success).data.token)
    }

    @Test
    fun `login returns ServerError when token is empty`() = runTest {
        val response = LoginResponse(null, "kim", 1, 100, "Invalid credentials")
        whenever(repository.login(any())).thenReturn(Response.success(response))

        viewModel.login(LoginRequest("a", "b"))

        advanceUntilIdle()

        val result = viewModel.loginResult.value
        Assert.assertTrue(result is NetworkResult.ServerError)
    }

    // ---------------------- CHECK EMAIL ----------------------

    @Test
    fun `checkEmail returns Success when response body exists`() = runTest {
        val response = EmailCheckResponse(exists = true)
        whenever(repository.checkEmail(any())).thenReturn(Response.success(response))

        viewModel.checkEmail(mapOf("email" to "test@test.com"))

        advanceUntilIdle()

        val result = viewModel.checkEmailResult.value
        Assert.assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `checkEmail returns ServerError when body is null`() = runTest {
        whenever(repository.checkEmail(any())).thenReturn(Response.success(null))

        viewModel.checkEmail(mapOf("email" to "test@test.com"))

        advanceUntilIdle()

        val result = viewModel.checkEmailResult.value
        Assert.assertTrue(result is NetworkResult.ServerError)
    }

    // ---------------------- UPDATE INITIAL REPS ----------------------

    @Test
    fun `updateInitialReps returns Success when body exists`() = runTest {
        val response = InitialCountResponse("ok", 10)
        whenever(repository.updateInitialReps(any())).thenReturn(Response.success(response))

        viewModel.updateInitialReps(InitialCountRequest(10))

        advanceUntilIdle()

        val result = viewModel.updateInitialRepsResult.value
        Assert.assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `updateInitialReps returns ServerError when body is null`() = runTest {
        whenever(repository.updateInitialReps(any())).thenReturn(Response.success(null))

        viewModel.updateInitialReps(InitialCountRequest(10))

        advanceUntilIdle()

        val result = viewModel.updateInitialRepsResult.value
        Assert.assertTrue(result is NetworkResult.ServerError)
    }

    // ---------------------- RESET ----------------------

    @Test
    fun `resetCheckEmailResult sets Idle`() {
        viewModel.resetCheckEmailResult()
        Assert.assertEquals(NetworkResult.Idle, viewModel.checkEmailResult.value)
    }

    @Test
    fun `resetSignupResult sets Idle`() {
        viewModel.resetSignupResult()
        Assert.assertEquals(NetworkResult.Idle, viewModel.signupResult.value)
    }
}
