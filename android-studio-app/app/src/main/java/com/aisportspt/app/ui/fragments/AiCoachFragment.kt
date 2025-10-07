package com.aisportspt.app.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.aisportspt.app.MainActivity
import com.aisportspt.app.R
import com.aisportspt.app.databinding.FragmentAiCoachBinding
import com.aisportspt.app.ui.adapters.FeedbackAdapter
import com.aisportspt.app.model.AIPoseFeedback
import com.aisportspt.app.ui.fragments.coachutils.PoseLandmarkerHelper
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import androidx.fragment.app.activityViewModels
import com.aisportspt.app.ui.fragments.coachutils.MainViewModel

class AiCoachFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    private var _binding: FragmentAiCoachBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
//    private lateinit var backgroundExecutor: ExecutorService
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private val viewModel: MainViewModel by activityViewModels()


    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbackList = mutableListOf<AIPoseFeedback>()
    
    private var isAnalyzing = false
    private var selectedSport = "골프"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiCoachBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()

        // ❸ 실행 스레드 준비
        cameraExecutor = Executors.newSingleThreadExecutor()
//        backgroundExecutor = Executors.newSingleThreadExecutor()

        // ❹ 헬퍼 초기화 (LIVE_STREAM)
        cameraExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,                     // 실시간 모드
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentDelegate = viewModel.currentDelegate,               // CPU/GPU
                poseLandmarkerHelperListener = this                        // 콜백
            )
        }
        
        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
    }

    private fun setupUI() {
        setupSportSpinner()
        setupFeedbackRecyclerView()
        setupButtons()
    }

    private fun setupSportSpinner() {
        val viewModel = (requireActivity() as MainActivity).getViewModel()
        
        viewModel.sports.observe(viewLifecycleOwner, Observer { sports ->
            val sportNames = sports.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sportNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSport.adapter = adapter
        })
    }

    private fun setupFeedbackRecyclerView() {
        feedbackAdapter = FeedbackAdapter()
        
//        binding.recyclerViewFeedback?.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = feedbackAdapter
//        }
    }

    private fun setupButtons() {
        binding.btnStartAnalysis.setOnClickListener {
            if (isAnalyzing) {
                stopAnalysis()
            } else {
                startAnalysis()
            }
        }
        
        binding.btnReset.setOnClickListener {
            resetAnalysis()
        }
        
        binding.btnFlipCamera.setOnClickListener {
            flipCamera()
        }
    }

    private fun observeViewModel() {
        // Observe any additional data from ViewModel if needed
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
//            }

            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.cameraPreview.display.rotation)
                .build()
                .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }
            
//            imageAnalyzer = ImageAnalysis.Builder().build().also {
//                // TODO: Set up ML Kit pose detection analyzer
//                // it.setAnalyzer(cameraExecutor, PoseAnalyzer())
//
//            }
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.cameraPreview.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // 중요!
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { image ->
                        detectPose(image) // ❻ 프레임 분석
                    }
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                // Handle error
            }
            
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = (cameraFacing == CameraSelector.LENS_FACING_FRONT)
            ) // 결과는 콜백 onResults()로 옴 :contentReference[oaicite:7]{index=7}
        } else {
            imageProxy.close()
        }
    }

    // ❼ 결과 수신 → OverlayView에 그리기
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            binding.overlay.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            ) // OverlayView가 포인트/라인/무릎각도까지 그림 :contentReference[oaicite:8]{index=8}:contentReference[oaicite:9]{index=9}
            binding.overlay.invalidate()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        // GPU 오류 시 CPU로 강등 등 핸들링 가능
    }

    private fun startAnalysis() {
        isAnalyzing = true
        
        // Update UI
        binding.btnStartAnalysis.text = "분석 중..."
        binding.btnStartAnalysis.isEnabled = false
        binding.aiGuideArea.visibility = View.VISIBLE
        binding.phaseIndicator.visibility = View.VISIBLE
        binding.recordingIndicator.visibility = View.VISIBLE
        
        // Show empty feedback initially
        binding.emptyFeedbackLayout.visibility = View.GONE
//        binding.recyclerViewFeedback?.visibility = View.VISIBLE
        
        // Start pose analysis (mock implementation)
        startMockAnalysis()
    }

    private fun stopAnalysis() {
        isAnalyzing = false
        
        // Update UI
        binding.btnStartAnalysis.text = "분석 시작"
        binding.btnStartAnalysis.isEnabled = true
        binding.recordingIndicator.visibility = View.GONE
    }

    private fun resetAnalysis() {
        stopAnalysis()
        
        // Reset UI
        binding.aiGuideArea.visibility = View.GONE
        binding.phaseIndicator.visibility = View.GONE
        binding.textScore.text = "0%"
        binding.progressScore.progress = 0
        binding.textAccuracyPercent.text = "0%"
        
        // Clear feedback
        feedbackList.clear()
        feedbackAdapter.updateFeedback(feedbackList)
        
        binding.emptyFeedbackLayout.visibility = View.VISIBLE
//        binding.recyclerViewFeedback?.visibility = View.GONE
    }

    private fun flipCamera() {
        // TODO: Implement camera flip functionality
        // This would require rebuilding the camera with front/back selector
    }

    private fun startMockAnalysis() {
        // Mock AI analysis with sample feedback
        val sampleFeedback = listOf(
            AIPoseFeedback(
                message = "좋은 자세입니다! 백스윙 각도가 완벽합니다.",
                confidence = 92,
                type = "good",
                timestamp = System.currentTimeMillis()
            ),
            AIPoseFeedback(
                message = "무릎을 조금 더 구부려보세요.",
                confidence = 88,
                type = "warning",
                timestamp = System.currentTimeMillis() - 2000
            ),
            AIPoseFeedback(
                message = "상체의 회전이 훌륭합니다!",
                confidence = 95,
                type = "good",
                timestamp = System.currentTimeMillis() - 4000
            )
        )
        
        feedbackList.addAll(sampleFeedback)
        feedbackAdapter.updateFeedback(feedbackList)
        
        // Update UI with mock data
        updateAnalysisUI(85, "백스윙")
    }

    private fun updateAnalysisUI(score: Int, phase: String) {
        binding.textScore.text = "${score}%"
        binding.progressScore.progress = score
        binding.textAccuracyPercent.text = "${score}%"
        binding.textCurrentPhase.text = "현재 단계: $phase"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
//        backgroundExecutor.shutdown()
        _binding = null
    }
}