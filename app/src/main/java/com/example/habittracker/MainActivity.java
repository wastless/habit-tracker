package com.example.habittracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.habittracker.data.HabitRepository;
import com.example.habittracker.firebase.FirebaseSyncManager;
import com.example.habittracker.firebase.FirebaseTestActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NavController navController;
    private FloatingActionButton addButton;
    private HabitRepository repository;
    private FirebaseSyncManager firebaseSyncManager;

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

        // Инициализация Firebase
        initializeFirebase();

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
        
        // Долгое нажатие на кнопку статистики открывает диагностику Firebase
        statisticsButton.setOnLongClickListener(v -> {
            openFirebaseTestActivity();
            return true;
        });
    }
    
    private void openFirebaseTestActivity() {
        Intent intent = new Intent(this, FirebaseTestActivity.class);
        startActivity(intent);
    }
    
    private void initializeFirebase() {
        try {
            Log.d(TAG, "Initializing Firebase...");
            
            // Проверка, инициализирован ли Firebase
            boolean isInitialized = false;
            try {
                FirebaseApp.getInstance();
                isInitialized = true;
                Log.d(TAG, "Firebase already initialized");
            } catch (IllegalStateException e) {
                isInitialized = false;
                Log.d(TAG, "Firebase not yet initialized");
            }
            
            // Инициализация Firebase, если ещё не инициализирован
            if (!isInitialized) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            }
            
            // Проверяем, что google-services.json правильно настроен
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            Log.d(TAG, "Firebase Project ID: " + options.getProjectId());
            Log.d(TAG, "Firebase API Key: " + (options.getApiKey() != null ? "present" : "missing"));
            Log.d(TAG, "Firebase App ID: " + options.getApplicationId());
            
            // Инициализация репозитория и менеджера синхронизации
            repository = new HabitRepository(this);
            firebaseSyncManager = FirebaseSyncManager.getInstance();
            firebaseSyncManager.initialize(this);
            
            Log.d(TAG, "Firebase sync manager initialized");
            Toast.makeText(this, "Синхронизация с облаком включена", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error: " + e.getMessage(), e);
            Toast.makeText(this, 
                    "Ошибка инициализации Firebase: " + e.getMessage() + 
                    "\nПроверьте, что google-services.json правильный.", 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Запускаем синхронизацию при возвращении к приложению
        if (repository != null) {
            repository.syncWithFirebase();
        }
    }
}