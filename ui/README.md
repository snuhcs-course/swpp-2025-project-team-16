# 운동 계획 관리 앱 - Android 구현

이 폴더에는 React 웹 앱과 동일한 UI를 안드로이드 네이티브 앱으로 구현한 XML 레이아웃 파일들과 Java 코드가 포함되어 있습니다.

## 파일 구조

### XML 레이아웃 파일들
- `activity_main.xml` - 메인 화면 (달력, 버튼들, 성취율)
- `activity_create_plan.xml` - 계획 생성 화면 (요일 선택, 시간대 선택)
- `activity_edit_select.xml` - 계획 수정 날짜 선택 화면
- `activity_edit_modify.xml` - 계획 수정 화면 (새 날짜/시간 선택)

### 리소스 파일들
- `colors.xml` - 앱에서 사용하는 컬러 정의
- `styles.xml` - 텍스트 스타일, 버튼 스타일, 테마 정의
- `drawables.xml` - 배경, 아이콘, 프로그레스 바 등의 drawable 리소스

### Java 액티비티 파일들
- `MainActivity.java` - 메인 화면 로직
- `CreatePlanActivity.java` - 계획 생성 화면 로직
- `AndroidManifest.xml` - 앱 설정 및 액티비티 등록

## 주요 구현 사항

### 1. 디자인 시스템
- React 앱의 Tailwind 컬러 시스템을 Android 컬러 리소스로 변환
- Material Design 3 컴포넌트 사용
- 카드뷰, 버튼, 프로그레스바 등 일관된 디자인

### 2. 화면 구성
- **메인 화면**: 달력, 액션 버튼, 성취율 프로그레스바
- **계획 생성**: 요일 체크박스, 시간대 라디오버튼
- **계획 수정**: 기존 계획 선택 → 새 날짜/시간 선택

### 3. 상호작용
- 버튼 상태 관리 (선택 완료 버튼 활성화/비활성화)
- 체크박스와 라디오버튼 상태 추적
- 액티비티 간 데이터 전달

## 안드로이드 스튜디오에서 사용하는 방법

1. **새 프로젝트 생성**
   - Android Studio에서 새 프로젝트 생성
   - Minimum SDK: API 24 (Android 7.0) 이상 권장

2. **파일 복사**
   ```
   app/src/main/res/layout/ → XML 레이아웃 파일들
   app/src/main/res/values/ → colors.xml, styles.xml
   app/src/main/res/drawable/ → drawables.xml 내용을 개별 파일로 분리
   app/src/main/java/com/example/workoutplanner/ → Java 파일들
   app/src/main/ → AndroidManifest.xml
   ```

3. **의존성 추가**
   ```gradle
   implementation 'com.google.android.material:material:1.10.0'
   implementation 'androidx.cardview:cardview:1.0.0'
   ```

4. **아이콘 리소스 추가**
   - `ic_arrow_left.xml` 벡터 드로어블 생성
   - Material Icons에서 화살표 아이콘 다운로드

## 추가 구현 필요사항

### 1. 데이터베이스 연동
```java
// Room 데이터베이스 사용 예시
@Entity(tableName = "workout_plans")
public class WorkoutPlan {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String selectedDays;
    public String timeSlot;
    public Date createdDate;
}
```

### 2. 달력 강조 표시
- MaterialCalendarView 라이브러리 사용 권장
- 운동 계획이 있는 날짜 시각적 표시

### 3. 알림 기능
```java
// NotificationManager를 사용한 운동 알림
private void scheduleWorkoutNotification() {
    // 운동 시간에 맞춰 알림 설정
}
```

### 4. 데이터 지속성
```java
// SharedPreferences 또는 Room 데이터베이스 사용
SharedPreferences prefs = getSharedPreferences("workout_plans", MODE_PRIVATE);
```

## 컴포넌트 매핑

| React 컴포넌트 | Android 컴포넌트 |
|----------------|------------------|
| Card | CardView |
| Button | MaterialButton |
| Calendar | CalendarView |
| Progress | ProgressBar |
| Badge | TextView with background |
| Checkbox | CheckBox |
| RadioGroup | RadioGroup + RadioButton |
| ArrowLeft | ImageButton with vector drawable |

## 반응형 디자인
- `layout-sw600dp/` 폴더에 태블릿용 레이아웃 추가 가능
- `values-night/` 폴더에 다크 테마 컬러 추가 가능

이 구현을 통해 웹 앱과 동일한 사용자 경험을 안드로이드 네이티브 앱에서도 제공할 수 있습니다.