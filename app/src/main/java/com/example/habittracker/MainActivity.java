package com.example.habittracker;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private FloatingActionButton addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Находим NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Настраиваем нижнюю навигацию
        View bottomNavigation = findViewById(R.id.bottomNavigation);
        View habitsButton = bottomNavigation.findViewById(R.id.habitsButton);
        addButton = findViewById(R.id.addButton);
        View statisticsButton = bottomNavigation.findViewById(R.id.statisticsButton);

        habitsButton.setOnClickListener(v -> {
            navController.navigate(R.id.habitsFragment);
        });

        addButton.setOnClickListener(v -> {
            AddHabitBottomSheetFragment fragment = new AddHabitBottomSheetFragment();
            
            // Получаем текущий фрагмент и проверяем, является ли он HabitsFragment
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof HabitsFragment) {
                fragment.setOnHabitAddedListener((AddHabitBottomSheetFragment.OnHabitAddedListener) currentFragment);
            }
            
            fragment.show(getSupportFragmentManager(), fragment.getTag());
        });

        statisticsButton.setOnClickListener(v -> {
            navController.navigate(R.id.statisticsFragment);
        });
    }
}