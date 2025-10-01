package com.example.workoutplanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    
    private CalendarView calendarView;
    private Button btnCreatePlan;
    private Button btnEditPlan;
    private ProgressBar progressAchievement;
    private TextView tvAchievementPercentage;
    private TextView tvCurrentMonth;
    
    // Mock data for workout plans (in a real app, this would come from a database)
    private Set<String> workoutDates = new HashSet<>();
    private int achievementRate = 68;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupMockData();
        setupClickListeners();
        updateUI();
    }
    
    private void initializeViews() {
        calendarView = findViewById(R.id.calendar_view);
        btnCreatePlan = findViewById(R.id.btn_create_plan);
        btnEditPlan = findViewById(R.id.btn_edit_plan);
        progressAchievement = findViewById(R.id.progress_achievement);
        tvAchievementPercentage = findViewById(R.id.tv_achievement_percentage);
        tvCurrentMonth = findViewById(R.id.tv_current_month);
    }
    
    private void setupMockData() {
        // Add mock workout dates (you would load this from a database in a real app)
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // October 2025 workout dates
        cal.set(2025, Calendar.OCTOBER, 5);
        workoutDates.add(sdf.format(cal.getTime()));
        
        cal.set(2025, Calendar.OCTOBER, 8);
        workoutDates.add(sdf.format(cal.getTime()));
        
        cal.set(2025, Calendar.OCTOBER, 12);
        workoutDates.add(sdf.format(cal.getTime()));
        
        cal.set(2025, Calendar.OCTOBER, 15);
        workoutDates.add(sdf.format(cal.getTime()));
        
        cal.set(2025, Calendar.OCTOBER, 19);
        workoutDates.add(sdf.format(cal.getTime()));
        
        cal.set(2025, Calendar.OCTOBER, 22);
        workoutDates.add(sdf.format(cal.getTime()));
        
        cal.set(2025, Calendar.OCTOBER, 26);
        workoutDates.add(sdf.format(cal.getTime()));
        
        cal.set(2025, Calendar.OCTOBER, 29);
        workoutDates.add(sdf.format(cal.getTime()));
    }
    
    private void setupClickListeners() {
        btnCreatePlan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreatePlanActivity.class);
            startActivity(intent);
        });
        
        btnEditPlan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditSelectActivity.class);
            intent.putExtra("workout_dates", workoutDates.toArray(new String[0]));
            startActivity(intent);
        });
        
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Handle date selection if needed
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String selectedDateStr = sdf.format(selectedDate.getTime());
            
            if (workoutDates.contains(selectedDateStr)) {
                // Show workout details or navigate to edit
                // You can implement this based on your requirements
            }
        });
    }
    
    private void updateUI() {
        // Update current month display
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy년 M월", Locale.KOREAN);
        tvCurrentMonth.setText(monthFormat.format(new Date()));
        
        // Update achievement progress
        progressAchievement.setProgress(achievementRate);
        tvAchievementPercentage.setText(achievementRate + "%");
        
        // Highlight workout dates on calendar
        // Note: CalendarView doesn't directly support highlighting specific dates
        // You might need to use a custom calendar library like MaterialCalendarView
        // or implement a custom solution
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from other activities
        updateUI();
    }
    
    // Helper method to check if a date has a workout plan
    public boolean hasWorkoutPlan(String dateString) {
        return workoutDates.contains(dateString);
    }
    
    // Method to add a new workout plan
    public void addWorkoutPlan(String dateString) {
        workoutDates.add(dateString);
        // In a real app, you would save this to a database
    }
    
    // Method to remove a workout plan
    public void removeWorkoutPlan(String dateString) {
        workoutDates.remove(dateString);
        // In a real app, you would remove this from a database
    }
}