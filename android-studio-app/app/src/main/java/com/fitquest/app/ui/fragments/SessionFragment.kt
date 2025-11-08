import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.fitquest.app.databinding.FragmentAiCoachBinding
import com.fitquest.app.repository.SessionRepository
import com.fitquest.app.ui.fragments.SessionFragmentArgs
import com.fitquest.app.ui.viewmodels.SessionViewModel

class SessionFragment : Fragment() {

    private var _binding: FragmentAiCoachBinding? = null
    private val binding get() = _binding!!

    private val args: SessionFragmentArgs by navArgs()

    private val viewModel: SessionViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SessionViewModel(SessionRepository()) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiCoachBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scheduleId = args.scheduleId.takeIf { it != -1 }

        // Begin Training
        binding.btnStartWorkout.setOnClickListener {
            val activity = "Push-ups" // TODO: 선택한 액티비티
            viewModel.startSession(activity, scheduleId)
        }

        // End Training
        binding.btnToggleCamera.setOnClickListener {
            viewModel.endSession(
                reps = 20, // 예시
                duration = 60 // 예시 초
            )
        }

        // Observe session
        viewModel.currentSession.observe(viewLifecycleOwner) { session ->
            // TODO: HUD 업데이트, reps/duration 표시
            Log.d("SessionFragment", "Current session: $session")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
