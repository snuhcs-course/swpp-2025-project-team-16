package com.aisportspt.app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.aisportspt.app.R
import com.aisportspt.app.databinding.DialogCreateTrainingPlanBinding
import com.aisportspt.app.model.*
import java.util.*

class CreateTrainingPlanDialogFragment(
    private val onPlanCreated: (TrainingPlan) -> Unit
) : DialogFragment() {

    private var _binding: DialogCreateTrainingPlanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateTrainingPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
    }

    private fun setupUI() {
        // 스포츠 스피너 설정
        val sports = resources.getStringArray(R.array.sports_array)
        val sportAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sports
        )
        sportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSport.adapter = sportAdapter

        // 난이도 스피너 설정
        val difficulties = arrayOf("초급", "중급", "고급")
        val difficultyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            difficulties
        )
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDifficulty.adapter = difficultyAdapter

        // 기본값 설정
        binding.editDuration.setText("45")

        // AI 생성 체크박스 기본 체크
        binding.checkboxAiGenerated.isChecked = true

        // 버튼 리스너
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            createTrainingPlan()
        }
    }

    private fun createTrainingPlan() {
        val planName = binding.editPlanName.text.toString().trim()
        val sport = binding.spinnerSport.selectedItem.toString()
        val durationText = binding.editDuration.text.toString().trim()
        val focus = binding.editFocus.text.toString().trim()
        val difficultyPosition = binding.spinnerDifficulty.selectedItemPosition
        val isAiGenerated = binding.checkboxAiGenerated.isChecked

        // 유효성 검사
        if (planName.isEmpty()) {
            binding.editPlanName.error = "계획 이름을 입력해주세요"
            return
        }

        if (durationText.isEmpty()) {
            binding.editDuration.error = "소요 시간을 입력해주세요"
            return
        }

        if (focus.isEmpty()) {
            binding.editFocus.error = "집중 영역을 입력해주세요"
            return
        }

        val difficulty = when (difficultyPosition) {
            0 -> Difficulty.BEGINNER
            1 -> Difficulty.INTERMEDIATE
            2 -> Difficulty.ADVANCED
            else -> Difficulty.INTERMEDIATE
        }

        // AI 생성된 운동들 생성
        val exercises = generateExercises(sport, difficulty)

        val trainingPlan = TrainingPlan(
            id = UUID.randomUUID().toString(),
            sport = sport,
            name = planName,
            duration = "${durationText}분",
            difficulty = difficulty,
            focus = focus,
            exercises = exercises,
            aiGenerated = isAiGenerated
        )

        onPlanCreated(trainingPlan)
        dismiss()
    }

    private fun generateExercises(sport: String, difficulty: Difficulty): List<Exercise> {
        return when (sport) {
            "골프" -> generateGolfExercises(difficulty)
            "볼링" -> generateBowlingExercises(difficulty)
            "테니스" -> generateTennisExercises(difficulty)
            else -> generateDefaultExercises(sport, difficulty)
        }
    }

    private fun generateGolfExercises(difficulty: Difficulty): List<Exercise> {
        val baseExercises = listOf(
            Exercise("1", "어드레스 자세 연습", "10분", description = "올바른 어드레스 자세 반복 연습"),
            Exercise("2", "백스윙 연습", "15분", description = "정확한 백스윙 궤도 연습"),
            Exercise("3", "다운스윙 연습", "15분", description = "파워와 정확도를 위한 다운스윙"),
            Exercise("4", "팔로우스루 연습", "5분", description = "완벽한 피니시 자세 연습")
        )

        return when (difficulty) {
            Difficulty.ADVANCED -> baseExercises + listOf(
                Exercise("5", "숏게임 연습", "10분", description = "치핑과 피칭 연습"),
                Exercise("6", "퍼팅 연습", "10분", description = "다양한 거리의 퍼팅 연습")
            )
            else -> baseExercises
        }
    }

    private fun generateBowlingExercises(difficulty: Difficulty): List<Exercise> {
        return listOf(
            Exercise("1", "어프로치 연습", "10분", description = "4스텝 어프로치 동작 연습"),
            Exercise("2", "릴리즈 포인트 연습", "10분", description = "일관된 릴리즈를 위한 연습"),
            Exercise("3", "스페어 연습", "10분", description = "다양한 스페어 상황 연습")
        )
    }

    private fun generateTennisExercises(difficulty: Difficulty): List<Exercise> {
        return listOf(
            Exercise("1", "포핸드 연습", "15분", description = "기본 포핸드 스트로크 연습"),
            Exercise("2", "백핸드 연습", "15분", description = "기본 백핸드 스트로크 연습"),
            Exercise("3", "서브 연습", "15분", description = "정확한 서브 연습")
        )
    }

    private fun generateDefaultExercises(sport: String, difficulty: Difficulty): List<Exercise> {
        return listOf(
            Exercise("1", "기본 자세 연습", "15분", description = "$sport 기본 자세 연습"),
            Exercise("2", "기본 동작 연습", "20분", description = "$sport 기본 동작 반복 연습"),
            Exercise("3", "응용 연습", "10분", description = "$sport 응용 동작 연습")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}