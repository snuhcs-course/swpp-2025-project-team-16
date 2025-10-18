package com.aisportspt.app.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
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
import com.aisportspt.app.ui.fragments.coachutils.Pose3DView

import com.aisportspt.app.ui.fragments.coachutils.counter.SquatCounter
import com.aisportspt.app.ui.fragments.coachutils.counter.SitUpCounter
import com.aisportspt.app.ui.fragments.coachutils.counter.PushUpCounter
import com.aisportspt.app.ui.fragments.coachutils.counter.PlankTimer
import com.aisportspt.app.ui.fragments.coachutils.counter.BaseCounter


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

    private var pose3DView: Pose3DView? = null

    // --- Pose smoothing state ---
    private var emaPts: FloatArray? = null
    private var lastPoseTs: Long = 0L

    // XY/Z 서로 다른 alpha (0~1, 클수록 반응 빠름)
    private val alphaXYBase = 0.95f
    private val alphaZBase  = 0.9f

    // 프레임 간 시간에 따라 alpha를 보정 (프레임레이트 변동 대비)
    private fun adaptAlpha(base: Float, dtMs: Long): Float {
        // 60fps 기준(16.7ms)에서 base가 그대로 되도록 지수적 보정
        val steps = (dtMs.coerceIn(5, 100).toDouble() / 16.7)
        return (1.0 - Math.pow((1.0 - base).toDouble(), steps)).toFloat()
    }

    // 갑작스러운 튐 제한 (정규화 좌표 기준 per-frame 최대 이동량)
    private val maxStepXY = 0.15f
    private val maxStepZ  = 0.25f

    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbackList = mutableListOf<AIPoseFeedback>()
    
    private var isAnalyzing = false
    private var selectedSport = "골프"

    // 그림(0~32) 인덱스 그대로 연결한 스켈레톤 간선 목록
    val edges = intArrayOf(
        // 얼굴/귀/눈썹 라인(그림 그대로)
        7, 3,  3, 2,  2, 1,  1, 0,  0, 4,  4, 5,  5, 6,  6, 8,
        // 입
        9, 10,

        // 어깨/몸통
        12, 11,    // 양 어깨 연결
        11, 23,    // 좌 어깨 -> 좌 힙(몸통 측면)
        12, 24,    // 우 어깨 -> 우 힙(몸통 측면)
        23, 24,    // 힙 라인

        // 왼팔(그림 기준: 11-13-15, 손목에서 세 갈래)
        11, 13,  13, 15,
        15, 19,  // index
        15, 21,  // thumb
        15, 17,  // pinky

        // 오른팔(그림 기준: 12-14-16, 손목에서 세 갈래)
        12, 14,  14, 16,
        16, 20,  // index
        16, 22,  // thumb
        16, 18,  // pinky

        // 왼다리(그림 기준: 23-25-27, 발 삼각형)
        23, 25,  25, 27,
        27, 29,  27, 31,  29, 31,

        // 오른다리(그림 기준: 24-26-28, 발 삼각형)
        24, 26,  26, 28,
        28, 30,  28, 32,  30, 32
    )

    private var counter: BaseCounter? = null

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

        // onViewCreated 마지막 부분 or setupUI 이후
        pose3DView = Pose3DView(requireContext())
        binding.aiGuideArea.addView(
            pose3DView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

//        counter.reset(System.currentTimeMillis())
    }

    private fun setupUI() {
        setupSportSpinner()
        setupFeedbackRecyclerView()
        setupButtons()
    }

    private fun setupSportSpinner() {
        ////        val viewModel = (requireActivity() as MainActivity).getViewModel()
////
////        viewModel.sports.observe(viewLifecycleOwner, Observer { sports ->
////            val sportNames = sports.map { it.name }
////            val adapter = ArrayAdapter(
////                requireContext(),
////                android.R.layout.simple_spinner_item,
////                sportNames
////            )
////            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
////            binding.spinnerSport.adapter = adapter
////        })
        val sportsArray = resources.getStringArray(R.array.sports_array)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sportsArray
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSport.adapter = adapter

        // ✅ 선택 이벤트 추가
        binding.spinnerSport.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedSport = sportsArray[position]   // 선택된 종목 저장
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무 것도 선택되지 않은 경우 (보통 무시)
            }
        }
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

            // === ❷ 결과 Landmark 세트 가져오기 ===
            val result = resultBundle.results.firstOrNull() ?: return@runOnUiThread
            val worldLms = result.worldLandmarks().firstOrNull() ?: return@runOnUiThread
            val normLms = result.landmarks().firstOrNull() ?: return@runOnUiThread

            // === ❸ worldLandmarks → 3D 시각화용 ===
            val ptsWorld = FloatArray(worldLms.size * 3)
            for (i in worldLms.indices) {
                ptsWorld[3 * i]     = worldLms[i].x()
                ptsWorld[3 * i + 1] = worldLms[i].y()
                ptsWorld[3 * i + 2] = worldLms[i].z()
            }

            // === ❹ landmarks (정규화 3D) → counter 계산용 ===
            val ptsNorm = FloatArray(normLms.size * 3)
            for (i in normLms.indices) {
                ptsNorm[3 * i]     = normLms[i].x()
                ptsNorm[3 * i + 1] = normLms[i].y()
                ptsNorm[3 * i + 2] = normLms[i].z()
            }

            val now = System.currentTimeMillis()

            // === ❺ counter에는 정규화 landmark(3D) 전달 ===
            counter?.update(ptsNorm, now)

            // === ❻ UI 갱신 ===
            binding.textCurrentPhase.text = "단계: ${counter?.phase}"
            if (selectedSport == "플랭크") {
                val plank = counter as? PlankTimer
                val seconds = plank?.totalGoodMs?.div(1000.0) ?: 0.0
                binding.textAccuracyPercent.text = String.format("%.1f s", seconds)
            } else {
                binding.textAccuracyPercent.text = "${counter?.count}"
            }

            // === ❼ 3D 시각화는 worldLandmarks 기준 ===
            pose3DView?.updateSkeleton(ptsWorld, edges)
        }


    }

    override fun onError(error: String, errorCode: Int) {
        // GPU 오류 시 CPU로 강등 등 핸들링 가능
    }

    private fun startAnalysis() {
        isAnalyzing = true

        counter = when (selectedSport) {
            "스쿼트" -> SquatCounter()
            "싯업" -> SitUpCounter()
            "푸쉬업" -> PushUpCounter()
            "플랭크" -> PlankTimer()
            else -> SquatCounter() // 기본값
        }
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

        counter?.reset(System.currentTimeMillis())
    }

    private fun stopAnalysis() {
        isAnalyzing = false
        
        // Update UI
        binding.btnStartAnalysis.text = "분석 시작"
        binding.btnStartAnalysis.isEnabled = true
        binding.recordingIndicator.visibility = View.GONE
        counter = null
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
        pose3DView = null
        _binding = null
    }

    private fun smoothPose(raw: FloatArray, nowMs: Long, visList: FloatArray? = null): FloatArray {
        // 첫 프레임이면 그대로 초기화
        val out = emaPts?.clone() ?: raw.clone()
        if (emaPts == null) {
            emaPts = out
            lastPoseTs = nowMs
            return out
        }

        val dt = (nowMs - lastPoseTs).coerceAtLeast(1L)
        lastPoseTs = nowMs

        val aXY = adaptAlpha(alphaXYBase, dt)
        val aZ  = adaptAlpha(alphaZBase, dt)

        // 가중치: visibility 있을 경우 신뢰도 반영(없으면 1.0)
        // MediaPipe Tasks의 landmark에 visibility()/presence()가 없을 수도 있으니 optional
        fun visAt(i: Int): Float = visList?.get(i) ?: 1f

        for (i in 0 until raw.size / 3) {
            val ix = 3*i; val iy = ix + 1; val iz = ix + 2

            val w = visAt(i).coerceIn(0f, 1f) // 0~1

            // XY
            val dx = (raw[ix]   - out[ix])
            val dy = (raw[iy]   - out[iy])
            val dz = (raw[iz]   - out[iz])

            // per-frame 클램프(큰 점프 억제)
            val clampedDx = dx.coerceIn(-maxStepXY, maxStepXY)
            val clampedDy = dy.coerceIn(-maxStepXY, maxStepXY)
            val clampedDz = dz.coerceIn(-maxStepZ , maxStepZ )

            // EWMA with visibility weighting: alpha_eff = w*alpha
            val ax = aXY * w; val ay = aXY * w; val az = aZ * w

            out[ix] += ax * clampedDx
            out[iy] += ay * clampedDy
            out[iz] += az * clampedDz
        }

        emaPts = out
        return out
    }
}