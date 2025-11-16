package com.fitquest.app.ui.fragments.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fitquest.app.R
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.fitquest.app.LoginActivity
import com.fitquest.app.data.remote.InitialCountRequest
import com.fitquest.app.data.remote.RetrofitClient
import com.fitquest.app.data.remote.TokenManager
import kotlinx.coroutines.launch
import com.fitquest.app.ui.coachutils.OverlayView
import com.fitquest.app.ui.coachutils.PoseLandmarkerHelper
import com.fitquest.app.ui.coachutils.counter.BaseCounter
import com.fitquest.app.ui.coachutils.counter.SquatCounter
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.exp

/**
 * SignupStep2Fragment - 카운트다운(10초) 후 실시간 포즈 분석 + 스쿼트 카운트
 * - CameraX Preview + ImageAnalysis
 * - PoseLandmarkerHelper(LIVE_STREAM)
 * - OverlayView에 스켈레톤 표시
 * - SquatCounter로 reps 계산
 * - 하체 keypoint visibility만으로 잠금(lock)/해제(unlock) 판단
 * - Logcat에 하체 keypoint visibility 실시간 출력
 */
class SignupStep2Fragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var cameraPreview: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvCountdown: TextView

    private lateinit var tvCountNumber: TextView
    private lateinit var tvCountLabel: TextView
    private lateinit var btnStop: MaterialButton

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private var isAnalyzing = false
    private var countdownTimer: CountDownTimer? = null
    private var isCountingDown = false

    // Squat counter
    private var counter: BaseCounter? = null

    // ---- 하체 visibility 기반 잠금 상태 ----
    private var trackingLocked = false
    private var badVisFrames = 0
    private var goodVisFrames = 0
    private var disarmUntilMs = 0L

    // ---- Logcat ----
    private val TAG = "SignupStep2"
    private var lastVisLogMs = 0L
    private val LOG_INTERVAL_MS = 0L // 0 = 매 프레임 로그, 예: 100으로 두면 100ms마다

    // 임계값(필요 시 조정)
    private val VIS_THRESH = 0.95f                         // visibility 임계치
    private val LOWER_NEEDED = intArrayOf(23,24,25,26,27,28) // 엉덩이/무릎/발목
    private val LOWER_REQUIRED = 5                          // 6개 중 몇 개 이상 보여야 하는지
    private val BAD_VIS_LIMIT = 12                          // 연속 나쁜 프레임(≈0.4s@30fps)
    private val GOOD_VIS_LIMIT = 12                         // 연속 좋은 프레임(≈0.4s@30fps)
    private val DISARM_MS_AFTER_UNLOCK = 300L               // 해제 직후 카운팅 금지 시간

    // 전달받을 값
    private var email: String? = null
    private var password: String? = null
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString(ARG_EMAIL)
            password = it.getString(ARG_PASSWORD)
            username = it.getString(ARG_USERNAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_signup_step2, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraPreview = view.findViewById(R.id.cameraPreview)
        overlayView = view.findViewById(R.id.overlay)
        tvCountdown = view.findViewById(R.id.tvCountdown)

        tvCountNumber = view.findViewById(R.id.tvCountNumber)
        tvCountLabel = view.findViewById(R.id.tvCountLabel)
        btnStop = view.findViewById(R.id.btnStop)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // PoseLandmarkerHelper 초기화 (executor에서)
        cameraExecutor!!.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                runningMode = RunningMode.LIVE_STREAM,
                context = requireContext(),
                poseLandmarkerHelperListener = this
            )
        }

        if (allPermissionsGranted()) {
            setupCameraProvider()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // 화면 진입 시 자동 10초 카운트다운 → 시작
        startCountdownThenBegin(10)

        btnStop.setOnClickListener {
            pauseAnalysis()
            stopSession()
        }
    }

    // --- CameraX setup ---

    private fun setupCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            // 처음엔 Preview만 (분석 X)
            bindCameraUseCases(includeAnalyzer = false)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(includeAnalyzer: Boolean) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(cameraPreview.surfaceProvider) }

        imageAnalyzer = if (includeAnalyzer) {
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(cameraPreview.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        detectPose(imageProxy)
                    }
                }
        } else null

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            val useCases = mutableListOf<UseCase>(preview)
            imageAnalyzer?.let { useCases.add(it) }
            provider.bindToLifecycle(this, cameraSelector, *useCases.toTypedArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (isAnalyzing && ::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = (lensFacing == CameraSelector.LENS_FACING_FRONT)
            )
        } else {
            imageProxy.close()
        }
    }

    // --- Pose callbacks ---

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (!isAnalyzing) return@runOnUiThread
            val result = resultBundle.results.firstOrNull() ?: return@runOnUiThread

            // 1) 오버레이 갱신 (잠금 여부와 상관없이 그림)
            overlayView.setResults(
                result,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM
            )
            overlayView.visibility = View.VISIBLE
            overlayView.invalidate()

            val lm = result.landmarks().firstOrNull() ?: return@runOnUiThread
            if (lm.size < 33) return@runOnUiThread

            // 2) 하체 visibility 점검
            val now = System.currentTimeMillis()
            val lowerCnt = lowerBodyVisibleCount(lm, VIS_THRESH)
            val visGood = (lowerCnt >= LOWER_REQUIRED)

//            // ★ Logcat 출력 (간격 제한 적용)
//            if (LOG_INTERVAL_MS <= 0L || now - lastVisLogMs >= LOG_INTERVAL_MS) {
//                logLowerVisibility(lm, lowerCnt)
//                lastVisLogMs = now
//            }

            // 잠금/해제 상태 머신
            if (trackingLocked) {
                if (visGood) {
                    goodVisFrames++
                    if (goodVisFrames >= GOOD_VIS_LIMIT) {
                        trackingLocked = false
                        goodVisFrames = 0
                        badVisFrames = 0
                        disarmUntilMs = now + DISARM_MS_AFTER_UNLOCK
                        tvCountdown.visibility = View.GONE
                    }
                } else {
                    goodVisFrames = 0
                }
                // 잠금 상태에선 카운터 미갱신
                return@runOnUiThread
            } else {
                if (!visGood) {
                    badVisFrames++
                    if (badVisFrames >= BAD_VIS_LIMIT) {
                        trackingLocked = true
                        badVisFrames = 0
                        goodVisFrames = 0
                        tvCountdown.text = "STEP BACK"
                        tvCountdown.visibility = View.VISIBLE
                        return@runOnUiThread
                    }
                } else {
                    badVisFrames = 0
                }
            }

            // 3) 디스암 기간에는 카운터 동결
            if (now < disarmUntilMs) {
                return@runOnUiThread
            }

            // 4) 카운터 갱신
            val pts = FloatArray(33 * 3)
            for (i in 0 until 33) {
                pts[3 * i]     = ((lm[i].x() as? Number)?.toFloat()) ?: lm[i].x()
                pts[3 * i + 1] = ((lm[i].y() as? Number)?.toFloat()) ?: lm[i].y()
                pts[3 * i + 2] = ((lm[i].z() as? Number)?.toFloat()) ?: lm[i].z()
            }
            counter?.update(pts, now)

            // 5) UI 반영
            tvCountNumber.text = (counter?.count ?: 0).toString()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    // --- Start/Stop control ---

    private fun beginAnalysis() {
        isAnalyzing = true
        // 분석 시작 시 카운터 시작
        counter = SquatCounter().also { it.reset(System.currentTimeMillis()) }
        tvCountNumber.text = "0"
        // 잠금 관련 상태 초기화
        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
        tvCountdown.visibility = View.GONE

        bindCameraUseCases(includeAnalyzer = true)
    }

    private fun pauseAnalysis() {
        isAnalyzing = false
        overlayView.clear()
        overlayView.visibility = View.GONE
        counter = null

        // 잠금 상태 클리어
        trackingLocked = false
        badVisFrames = 0
        goodVisFrames = 0
        disarmUntilMs = 0L
        tvCountdown.visibility = View.GONE

        bindCameraUseCases(includeAnalyzer = false)
    }

    private fun startCountdownThenBegin(seconds: Int) {
        if (isCountingDown) return
        isCountingDown = true

        tvCountdown.visibility = View.VISIBLE
        tvCountdown.text = seconds.toString()

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                val remain = ((ms + 999) / 1000L).toInt()
                tvCountdown.text = remain.toString()
            }
            override fun onFinish() {
                tvCountdown.visibility = View.GONE
                isCountingDown = false
                beginAnalysis() // ← 카운트다운 끝나면 분석 & 카운터 시작
            }
        }.start()
    }

    // --- 기존 저장 로직 유지 ---
    private fun stopSession() {
        val initialCount = tvCountNumber.text.toString().toIntOrNull() ?: 0
        val token = TokenManager.getToken(requireContext()) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateInitialReps(
                    token = "Bearer $token",
                    body = InitialCountRequest(initial_reps = initialCount)
                )

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Saved: ${response.body()?.initial_reps}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        (activity as? LoginActivity)?.completeLogin()
    }

    // --- Permissions & lifecycle ---

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            setupCameraProvider()
            startCountdownThenBegin(10)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        countdownTimer = null
        cameraExecutor?.shutdown()
        if (::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.clearPoseLandmarker()
        }
    }

    // ---- 하체 visibility/로그 도우미 ----

    // (logit 또는 확률을 받아 0..1로 정규화)
    private fun toProbMaybeLogit(x: Float?): Float? {
        if (x == null || x.isNaN()) return null
        return if (x in 0f..1f) x else (1f / (1f + exp(-x)))
    }

    // 프레임 안에 있는지 보조 판정
    private fun inFrame(p: NormalizedLandmark): Boolean {
        val x = ((p.x() as? Number)?.toFloat()) ?: p.x()
        val y = ((p.y() as? Number)?.toFloat()) ?: p.y()
        return x in 0f..1f && y in 0f..1f
    }

    // 하체 keypoint 가시성 개수(0..6)
    private fun lowerBodyVisibleCount(
        lm: List<NormalizedLandmark>,
        thresh: Float = VIS_THRESH
    ): Int {
        var ok = 0
        for (i in LOWER_NEEDED) {
            val s = safeVis(lm[i])
            if (s >= thresh) ok++
        }
        return ok
    }

    // Logcat에 하체 visibility 출력(동일 규칙으로 표기)
    private fun logLowerVisibility(
        lm: List<NormalizedLandmark>,
        lowerCnt: Int
    ) {
        val ids = intArrayOf(23, 24, 25, 26, 27, 28) // hipL/R, kneeL/R, ankL/R
        val sb = StringBuilder()

        // 요약
        sb.append("VIS(th=").append(VIS_THRESH)
            .append(") ok=").append(lowerCnt).append("/").append(ids.size)
            .append(" locked=").append(if (trackingLocked) 1 else 0)
            .append(" bad=").append(badVisFrames)
            .append(" good=").append(goodVisFrames)

        // 각 관절 visibility / presence / safe / in-frame
        for (id in ids) {
            val p = lm[id]
            val visProb = toProbMaybeLogit((p.visibility() as? Number)?.toFloat())
            val presProb = toProbMaybeLogit((p.presence() as? Number)?.toFloat())
            val safe = safeVis(p)
            val inF = inFrame(p)

            sb.append(" | ").append(id).append(':')
            sb.append(if (visProb != null) String.format("%.2f", visProb) else "na")
            if (presProb != null) sb.append('(').append(String.format("%.2f", presProb)).append(')')
            sb.append("->").append(String.format("%.2f", safe))
            sb.append(if (inF) "[in]" else "[out]")
        }

        Log.d(TAG, sb.toString())
    }

    // visibility/presence를 일관된 확률(0..1)로 변환. 없으면 in-frame 보조 판정으로 대체
    private fun safeVis(p: NormalizedLandmark): Float {
        val visProb = toProbMaybeLogit((p.visibility() as? Number)?.toFloat())
        val presProb = toProbMaybeLogit((p.presence() as? Number)?.toFloat())
        val best = listOfNotNull(visProb, presProb).maxOrNull()
        if (best != null) return best.coerceIn(0f, 1f)

        // 둘 다 없을 때: in-frame이면 1, 아니면 0
        return if (inFrame(p)) 1f else 0f
    }

    companion object {
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"
        private const val ARG_USERNAME = "username"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun newInstance(email: String, password: String, username: String): SignupStep2Fragment {
            val fragment = SignupStep2Fragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            args.putString(ARG_PASSWORD, password)
            args.putString(ARG_USERNAME, username)
            fragment.arguments = args
            return fragment
        }
    }
}
