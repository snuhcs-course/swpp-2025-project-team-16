package com.example.workoutplanner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class CreatePlanActivity extends AppCompatActivity {
    
    private ImageButton btnBack;
    private Button btnCompleteCreation;
    
    // Checkboxes for days of week
    private CheckBox cbMonday;
    private CheckBox cbTuesday;
    private CheckBox cbWednesday;
    private CheckBox cbThursday;
    private CheckBox cbFriday;
    private CheckBox cbSaturday;
    private CheckBox cbSunday;
    
    // Radio group for time slots
    private RadioGroup rgTimeSlots;
    private RadioButton rbMorning;
    private RadioButton rbAfternoon;
    private RadioButton rbEvening;
    
    private List<CheckBox> dayCheckboxes = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_plan);
        
        initializeViews();
        setupClickListeners();
        updateCompleteButtonState();
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnCompleteCreation = findViewById(R.id.btn_complete_creation);
        
        // Initialize day checkboxes
        cbMonday = findViewById(R.id.cb_monday);
        cbTuesday = findViewById(R.id.cb_tuesday);
        cbWednesday = findViewById(R.id.cb_wednesday);
        cbThursday = findViewById(R.id.cb_thursday);
        cbFriday = findViewById(R.id.cb_friday);
        cbSaturday = findViewById(R.id.cb_saturday);
        cbSunday = findViewById(R.id.cb_sunday);
        
        // Add checkboxes to list for easier management
        dayCheckboxes.add(cbMonday);
        dayCheckboxes.add(cbTuesday);
        dayCheckboxes.add(cbWednesday);
        dayCheckboxes.add(cbThursday);
        dayCheckboxes.add(cbFriday);
        dayCheckboxes.add(cbSaturday);
        dayCheckboxes.add(cbSunday);
        
        // Initialize radio group and buttons
        rgTimeSlots = findViewById(R.id.rg_time_slots);
        rbMorning = findViewById(R.id.rb_morning);
        rbAfternoon = findViewById(R.id.rb_afternoon);
        rbEvening = findViewById(R.id.rb_evening);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnCompleteCreation.setOnClickListener(v -> {
            if (isFormValid()) {
                createWorkoutPlan();
            } else {
                Toast.makeText(this, "요일과 시간대를 모두 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up listeners for checkboxes to update button state
        for (CheckBox checkbox : dayCheckboxes) {
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateCompleteButtonState());
        }
        
        // Set up listener for radio group to update button state
        rgTimeSlots.setOnCheckedChangeListener((group, checkedId) -> updateCompleteButtonState());
    }
    
    private boolean isFormValid() {
        // Check if at least one day is selected
        boolean isDaySelected = false;
        for (CheckBox checkbox : dayCheckboxes) {
            if (checkbox.isChecked()) {
                isDaySelected = true;
                break;
            }
        }
        
        // Check if a time slot is selected
        boolean isTimeSelected = rgTimeSlots.getCheckedRadioButtonId() != -1;
        
        return isDaySelected && isTimeSelected;
    }
    
    private void updateCompleteButtonState() {
        btnCompleteCreation.setEnabled(isFormValid());
    }
    
    private void createWorkoutPlan() {
        List<String> selectedDays = new ArrayList<>();
        String selectedTimeSlot = "";
        
        // Collect selected days
        if (cbMonday.isChecked()) selectedDays.add("월요일");
        if (cbTuesday.isChecked()) selectedDays.add("화요일");
        if (cbWednesday.isChecked()) selectedDays.add("수요일");
        if (cbThursday.isChecked()) selectedDays.add("목요일");
        if (cbFriday.isChecked()) selectedDays.add("금요일");
        if (cbSaturday.isChecked()) selectedDays.add("토요일");
        if (cbSunday.isChecked()) selectedDays.add("일요일");
        
        // Get selected time slot
        int selectedTimeId = rgTimeSlots.getCheckedRadioButtonId();
        if (selectedTimeId == R.id.rb_morning) {
            selectedTimeSlot = "오전 (6:00 - 12:00)";
        } else if (selectedTimeId == R.id.rb_afternoon) {
            selectedTimeSlot = "오후 (12:00 - 18:00)";
        } else if (selectedTimeId == R.id.rb_evening) {
            selectedTimeSlot = "저녁 (18:00 - 24:00)";
        }
        
        // In a real app, you would save this data to a database
        // For now, we'll just show a success message
        String message = "계획이 생성되었습니다!\n" +
                        "선택된 요일: " + String.join(", ", selectedDays) + "\n" +
                        "시간대: " + selectedTimeSlot;
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        // TODO: Save the plan to database/preferences
        savePlanToDatabase(selectedDays, selectedTimeSlot);
        
        // Return to main activity
        finish();
    }
    
    private void savePlanToDatabase(List<String> selectedDays, String selectedTimeSlot) {
        // This is where you would implement the actual database saving logic
        // For example, using Room database, SharedPreferences, or a web API
        
        // Example using SharedPreferences (simple approach):
        /*
        SharedPreferences prefs = getSharedPreferences("workout_plans", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Save selected days as a comma-separated string
        editor.putString("selected_days", String.join(",", selectedDays));
        editor.putString("selected_time_slot", selectedTimeSlot);
        editor.putLong("plan_created_date", System.currentTimeMillis());
        
        editor.apply();
        */
        
        // Or using Room database:
        /*
        WorkoutPlan plan = new WorkoutPlan();
        plan.setSelectedDays(selectedDays);
        plan.setTimeSlot(selectedTimeSlot);
        plan.setCreatedDate(new Date());
        
        // Save to database in background thread
        new Thread(() -> {
            AppDatabase.getInstance(this).workoutPlanDao().insert(plan);
        }).start();
        */
    }
}