package com.example.habittracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.habittracker.data.HabitRepository;
import com.example.habittracker.data.database.Habit;

import java.util.List;

public class StatisticsFragment extends Fragment {
    private LinearLayout statsContainer;
    private TextView emptyStatsText;
    private HabitRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);
        
        statsContainer = view.findViewById(R.id.statisticsContainer);
        emptyStatsText = view.findViewById(R.id.emptyStatsText);
        
        // Initialize repository
        repository = new HabitRepository(requireContext());
        
        // Load statistics
        loadStatistics();
        
        return view;
    }
    
    private void loadStatistics() {
        // Clear existing stats
        statsContainer.removeAllViews();
        
        // Load habits and their stats
        repository.getAllHabits(habits -> {
            requireActivity().runOnUiThread(() -> {
                if (habits.isEmpty()) {
                    emptyStatsText.setVisibility(View.VISIBLE);
                    statsContainer.setVisibility(View.GONE);
                } else {
                    emptyStatsText.setVisibility(View.GONE);
                    statsContainer.setVisibility(View.VISIBLE);
                    
                    for (Habit habit : habits) {
                        addHabitStatistics(habit);
                    }
                }
            });
        });
    }
    
    private void addHabitStatistics(Habit habit) {
        View statView = getLayoutInflater().inflate(R.layout.item_statistic, statsContainer, false);
        TextView habitName = statView.findViewById(R.id.habitName);
        TextView completionCount = statView.findViewById(R.id.completionCount);
        
        habitName.setText(habit.getName());
        
        // Get completion count from database
        repository.getLogsForHabit(habit.getId(), logs -> {
            int completedCount = 0;
            for (int i = 0; i < logs.size(); i++) {
                if (logs.get(i).isCompleted()) {
                    completedCount++;
                }
            }
            
            final int count = completedCount;
            requireActivity().runOnUiThread(() -> {
                completionCount.setText(String.format("Completed %d times", count));
            });
        });
        
        statsContainer.addView(statView);
    }
} 