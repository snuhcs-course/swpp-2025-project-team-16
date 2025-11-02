package com.fitquest.app.ui.fragments.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fitquest.app.R
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.fitquest.app.LoginActivity
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
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textureView = view.findViewById(R.id.textureView)
        tvCountNumber = view.findViewById(R.id.tvCountNumber)
        tvCountLabel = view.findViewById(R.id.tvCountLabel)
        btnStop = view.findViewById(R.id.btnStop)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(
            requireActivity(),
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )

        btnStop.setOnClickListener { stopSession() }
    }

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

    private fun stopSession() {
        // TODO: 세션 종료 시 결과 저장 or 다음 화면 이동
        val activity = activity as? LoginActivity
        activity?.completeLogin()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
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
