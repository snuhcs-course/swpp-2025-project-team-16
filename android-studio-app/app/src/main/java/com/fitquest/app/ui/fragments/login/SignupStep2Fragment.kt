package com.fitquest.app.ui.fragments.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fitquest.app.R
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * SignupStep2Fragment - AI Intro / Camera Session
 *
 * 로그인 직후 표시되는 화면.
 * - 카메라 자동 실행
 * - 상단: 안내 문구
 * - 중앙: 카메라 프리뷰
 * - 하단: Count + Stop 버튼
 *
 * TODO:
 *  - Pose detection 및 rep counting 연동
 *  - Stop 클릭 시 세션 결과 전송 or 저장 로직 추가
 */
class SignupStep2Fragment : Fragment() {

    private lateinit var textureView: TextureView
    private lateinit var tvCountNumber: TextView
    private lateinit var tvCountLabel: TextView
    private lateinit var btnStop: MaterialButton

    private var cameraExecutor: ExecutorService? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 기존 XML 유지 (fragment_signup_step2.xml)
        return inflater.inflate(R.layout.fragment_signup_step2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View 연결
        textureView = view.findViewById(R.id.textureView)
        tvCountNumber = view.findViewById(R.id.tvCountNumber)
        tvCountLabel = view.findViewById(R.id.tvCountLabel)
        btnStop = view.findViewById(R.id.btnStop)

        // Executor 초기화
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 권한 확인 후 카메라 실행
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // Stop 버튼 클릭 리스너
        btnStop.setOnClickListener {
            stopSession()
        }
    }

    /**
     * CameraX 미리보기 시작
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            preview.setSurfaceProvider { surfaceRequest ->
                val surface = Surface(textureView.surfaceTexture)
                surfaceRequest.provideSurface(surface, cameraExecutor!!, { surface.release() })
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Stop 버튼 클릭 시 세션 종료
     */
    private fun stopSession() {
        // TODO: 세션 종료 로직 (결과 화면으로 이동 or 팝업 표시)
        // ex) 운동 횟수 / 점수 / 소요시간 등을 저장
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
    }

    /**
     * 카메라 권한 확인
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
