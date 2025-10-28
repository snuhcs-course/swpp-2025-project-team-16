# FitQuest Android App - Kotlin Skeleton

This is the skeleton structure for the FitQuest Android application in Kotlin.
The UI logic and flows are implemented in the web version (React/TypeScript).

## Design System - RPG Growth Concept

**Core Idea:** Turn bodyweight training into an RPG-style growth system
- Every exercise earns XP, levels, and titles
- Users visualize physical progress as character growth
- Character avatars evolve visually as users level up

**Color Palette:**
- Primary: Deep Blue (#0D47A1) - trust, energy, focus
- Reward: Gold (#FFD700) - achievements, XP
- Accent: Electric Cyan (#00E5FF) - HUD effects, highlights
- Background: Dark slate with blue tones

**UI Elements:**
- XP bars and progression meters
- Stat displays (level, streak, total quests)
- Character avatars with visual evolution
- Glowing effects and dynamic animations
- Victory markers and achievement badges

## Project Structure

```
android-studio-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/fitquest/app/
│   │   │   ├── MainActivity.kt       
│   │   │   ├── LoginActivity.kt  
│   │   │   │
│   │   │   ├── model/       
│   │   │   │   ├── User.kt
│   │   │   │   ├── Exercise.kt
│   │   │   │   ├── WorkoutPlan.kt
│   │   │   │   └── FitnessLevel.kt
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   ├── fragments/      
│   │   │   │   │   ├── JourneyFragment.kt      
│   │   │   │   │   ├── ScheduleFragment.kt      
│   │   │   │   │   ├── AiCoachFragment.kt      
│   │   │   │   │   ├── ProfileFragment.kt      
│   │   │   │   │   └── login/                   
│   │   │   │   │       ├── LoginEmailFragment.kt
│   │   │   │   │       ├── LoginPasswordFragment.kt
│   │   │   │   │       ├── SignupStep1Fragment.kt
│   │   │   │   │       └── SignupStep2Fragment.kt
│   │   │   │   │
│   │   │   │   ├── adapters/   
│   │   │   │   │   ├── WorkoutAdapter.kt
│   │   │   │   │   ├── ExerciseAdapter.kt
│   │   │   │   │   └── HistoryAdapter.kt
│   │   │   │   │
│   │   │   │   └── viewmodels/
│   │   │   │       ├── LoginViewModel.kt
│   │   │   │       ├── JourneyViewModel.kt
│   │   │   │       ├── ScheduleViewModel.kt
│   │   │   │       ├── AiCoachViewModel.kt
│   │   │   │       └── ProfileViewModel.kt
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.kt
│   │   │   │   ├── WorkoutRepository.kt
│   │   │   │   └── ExerciseRepository.kt
│   │   │   │
│   │   │   └── utils/
│   │   │       ├── Constants.kt
│   │   │       └── Extensions.kt
│   │   │
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_login.xml
│   │   │   │   ├── fragment_journey.xml
│   │   │   │   ├── fragment_schedule.xml
│   │   │   │   ├── fragment_ai_coach.xml
│   │   │   │   ├── fragment_profile.xml
│   │   │   │   ├── fragment_login_email.xml
│   │   │   │   ├── fragment_login_password.xml
│   │   │   │   ├── fragment_signup_step1.xml
│   │   │   │   ├── fragment_signup_step2.xml
│   │   │   │   ├── item_workout.xml
│   │   │   │   ├── item_exercise.xml
│   │   │   │   └── item_history.xml
│   │   │   │
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   ├── themes.xml
│   │   │   │   └── dimens.xml
│   │   │   │
│   │   │   └── drawable/
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
├── build.gradle.kts
└── settings.gradle.kts
```

## Implementation Notes

- All backend calls are marked with `// TODO: Backend implementation`
- UI designs follow the gamified approach from the web version
- Use Material Design 3 components for modern Android UI
- Implement ViewModels with LiveData/Flow for reactive UI updates
- Use Navigation Component for fragment navigation
- Implement CameraX for AI pose detection
