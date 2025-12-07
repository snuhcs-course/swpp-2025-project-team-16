package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.fitquest.app.model.NetworkResult
import com.fitquest.app.model.pose.PoseUploadRequest
import com.fitquest.app.model.pose.PoseUploadResponse
import com.fitquest.app.repository.PoseAnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*
import retrofit2.Response

@ExperimentalCoroutinesApi
class PoseViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule() // LiveData 동기화

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: PoseAnalysisRepository
    private lateinit var viewModel: PoseViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(PoseAnalysisRepository::class.java)
        viewModel = PoseViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uploadPose sets Success when repository returns successful response`() = runTest {
        val request = PoseUploadRequest(category = "squat", imageBase64 = "dummy")
        val responseBody = PoseUploadResponse(id = 1, goodPoints = "good")

        `when`(repository.uploadPose(request)).thenReturn(Response.success(responseBody))

        val results = mutableListOf<NetworkResult<PoseUploadResponse>>()
        val observer = Observer<NetworkResult<PoseUploadResponse>> { results.add(it) }
        viewModel.poseAnalysisResult.observeForever(observer)

        viewModel.uploadPose(request)

        advanceUntilIdle()

        assertTrue(results[0] is NetworkResult.Idle)
        assertTrue(results[1] is NetworkResult.Success)
        assertEquals(responseBody, (results[1] as NetworkResult.Success).data)

        viewModel.poseAnalysisResult.removeObserver(observer)
    }

    @Test
    fun `uploadPose sets ServerError when repository returns unsuccessful response`() = runTest {
        val request = PoseUploadRequest(category = "squat", imageBase64 = "dummy")
        val errorResponse = Response.error<PoseUploadResponse>(500,
            "Server Error".toResponseBody(null)
        )

        `when`(repository.uploadPose(request)).thenReturn(errorResponse)

        val results = mutableListOf<NetworkResult<PoseUploadResponse>>()
        val observer = Observer<NetworkResult<PoseUploadResponse>> { results.add(it) }
        viewModel.poseAnalysisResult.observeForever(observer)

        viewModel.uploadPose(request)
        advanceUntilIdle()

        assertTrue(results[0] is NetworkResult.Idle)
        assertTrue(results[1] is NetworkResult.ServerError)
        assertEquals(500, (results[1] as NetworkResult.ServerError).code)

        viewModel.poseAnalysisResult.removeObserver(observer)
    }

    @Test
    fun `uploadPose sets NetworkError when repository throws exception`() = runTest {
        val request = PoseUploadRequest(category = "squat", imageBase64 = "dummy")
        val exception = RuntimeException("Network failed") // IOException → RuntimeException

        `when`(repository.uploadPose(request)).thenThrow(exception)

        val results = mutableListOf<NetworkResult<PoseUploadResponse>>()
        val observer = Observer<NetworkResult<PoseUploadResponse>> { results.add(it) }
        viewModel.poseAnalysisResult.observeForever(observer)

        viewModel.uploadPose(request)
        advanceUntilIdle()

        assertTrue(results[0] is NetworkResult.Idle)
        assertTrue(results[1] is NetworkResult.NetworkError)
        assertEquals(exception, (results[1] as NetworkResult.NetworkError).exception)

        viewModel.poseAnalysisResult.removeObserver(observer)
    }

    @Test
    fun `resetPoseAnalysisResult sets Idle`() = runTest {
        viewModel.resetPoseAnalysisResult()
        assertTrue(viewModel.poseAnalysisResult.value is NetworkResult.Idle)
    }
}
