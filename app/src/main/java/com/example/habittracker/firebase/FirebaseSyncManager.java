package com.example.habittracker.firebase;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.habittracker.data.HabitRepository;
import com.example.habittracker.data.database.Habit;
import com.example.habittracker.data.database.HabitLog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс для синхронизации привычек с Firebase Realtime Database
 */
public class FirebaseSyncManager {
    private static final String TAG = "FirebaseSyncManager";
    private static final String HABITS_REF = "habits";
    private static final String HABIT_LOGS_REF = "habit_logs";
    
    private static FirebaseSyncManager instance;
    private FirebaseAuthManager authManager;
    private FirebaseDatabase database;
    private DatabaseReference habitsRef;
    private DatabaseReference habitLogsRef;
    private HabitRepository localRepository;
    
    private boolean isSyncing = false;
    
    private FirebaseSyncManager() {
        authManager = FirebaseAuthManager.getInstance();
        database = FirebaseDatabase.getInstance("https://habittracker-1bbd8-default-rtdb.europe-west1.firebasedatabase.app/");
    }
    
    public static synchronized FirebaseSyncManager getInstance() {
        if (instance == null) {
            instance = new FirebaseSyncManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        localRepository = new HabitRepository(context);
        
        // Инициализируем Firebase Auth
        if (!authManager.isUserAuthenticated()) {
            authManager.signInAnonymously(context);
        }
        
        // Установка слушателя авторизации
        authManager.setAuthStateListener(new FirebaseAuthManager.AuthStateListener() {
            @Override
            public void onUserAuthenticated(com.google.firebase.auth.FirebaseUser user) {
                // Когда пользователь авторизован, настраиваем ссылки на данные
                setupDatabaseReferences(user.getUid());
                // И запускаем синхронизацию
                syncData();
            }
            
            @Override
            public void onUserNotAuthenticated() {
                // Сбрасываем ссылки
                habitsRef = null;
                habitLogsRef = null;
            }
        });
    }
    
    private void setupDatabaseReferences(String userId) {
        habitsRef = database.getReference(HABITS_REF).child(userId);
        habitLogsRef = database.getReference(HABIT_LOGS_REF).child(userId);
    }
    
    /**
     * Синхронизация данных между локальной БД и Firebase
     */
    public void syncData() {
        if (!authManager.isUserAuthenticated() || isSyncing) {
            return;
        }
        
        isSyncing = true;
        
        // Загружаем привычки из Firebase
        habitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<FirebaseHabit> firebaseHabits = new ArrayList<>();
                
                for (DataSnapshot habitSnapshot : dataSnapshot.getChildren()) {
                    FirebaseHabit habit = habitSnapshot.getValue(FirebaseHabit.class);
                    if (habit != null) {
                        habit.setFirebaseId(habitSnapshot.getKey());
                        firebaseHabits.add(habit);
                    }
                }
                
                // Теперь получаем локальные привычки и синхронизируем
                localRepository.getAllHabits(localHabits -> {
                    mergeHabits(localHabits, firebaseHabits);
                    isSyncing = false;
                });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error syncing habits: " + databaseError.getMessage());
                isSyncing = false;
            }
        });
    }
    
    /**
     * Слияние локальных и удаленных привычек
     */
    private void mergeHabits(List<Habit> localHabits, List<FirebaseHabit> firebaseHabits) {
        // Мапы для быстрого поиска
        Map<String, FirebaseHabit> firebaseHabitsMap = new HashMap<>();
        Map<Long, Habit> localHabitsMap = new HashMap<>();
        
        // Заполняем мапу Firebase привычек
        for (FirebaseHabit habit : firebaseHabits) {
            firebaseHabitsMap.put(habit.getFirebaseId(), habit);
        }
        
        // Заполняем мапу локальных привычек и обновляем Firebase
        for (Habit localHabit : localHabits) {
            localHabitsMap.put(localHabit.getId(), localHabit);
            
            // Добавляем или обновляем в Firebase
            uploadHabitToFirebase(localHabit);
        }
        
        // Проверяем, есть ли в Firebase привычки, которых нет локально
        for (FirebaseHabit firebaseHabit : firebaseHabits) {
            boolean foundLocally = false;
            
            for (Habit localHabit : localHabits) {
                // Сравниваем по имени и частоте, так как у нас нет прямого соответствия ID
                if (localHabit.getName().equals(firebaseHabit.getName()) && 
                    localHabit.getFrequency().equals(firebaseHabit.getFrequency())) {
                    foundLocally = true;
                    break;
                }
            }
            
            if (!foundLocally) {
                // Если привычки нет локально, добавляем
                Habit newLocalHabit = new Habit(
                        firebaseHabit.getName(),
                        firebaseHabit.getFrequency()
                );
                
                // Устанавливаем дату создания
                newLocalHabit.setCreatedAt(new Date(firebaseHabit.getCreatedAt()));
                
                // Сохраняем локально
                localRepository.insertHabit(newLocalHabit, null);
            }
        }
    }
    
    /**
     * Загрузка привычки в Firebase
     */
    public void uploadHabitToFirebase(Habit habit) {
        if (!authManager.isUserAuthenticated() || habitsRef == null) {
            return;
        }
        
        FirebaseHabit firebaseHabit = new FirebaseHabit(
                habit.getName(),
                habit.getFrequency(),
                habit.getCreatedAt().getTime()
        );
        
        habitsRef.child(String.valueOf(habit.getId())).setValue(firebaseHabit);
    }
    
    /**
     * Загрузка лога выполнения привычки в Firebase
     */
    public void uploadHabitLogToFirebase(HabitLog habitLog) {
        if (!authManager.isUserAuthenticated() || habitLogsRef == null) {
            return;
        }
        
        FirebaseHabitLog firebaseLog = new FirebaseHabitLog(
                habitLog.getHabitId(),
                habitLog.getDate().getTime(),
                habitLog.isCompleted()
        );
        
        String logKey = String.valueOf(habitLog.getHabitId()) + "_" + habitLog.getDate().getTime();
        habitLogsRef.child(logKey).setValue(firebaseLog);
    }
    
    /**
     * Включение синхронизации при завершении привычки
     */
    public void syncHabitCompletion(HabitLog habitLog) {
        uploadHabitLogToFirebase(habitLog);
    }
    
    /**
     * Вложенный класс для представления привычки в Firebase
     */
    public static class FirebaseHabit {
        private String firebaseId;
        private String name;
        private String frequency;
        private long createdAt;
        
        public FirebaseHabit() {
            // Пустой конструктор для Firebase
        }
        
        public FirebaseHabit(String name, String frequency, long createdAt) {
            this.name = name;
            this.frequency = frequency;
            this.createdAt = createdAt;
        }

        public String getFirebaseId() {
            return firebaseId;
        }

        public void setFirebaseId(String firebaseId) {
            this.firebaseId = firebaseId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Вложенный класс для представления лога привычки в Firebase
     */
    public static class FirebaseHabitLog {
        private long habitId;
        private long date;
        private boolean completed;
        
        public FirebaseHabitLog() {
            // Пустой конструктор для Firebase
        }
        
        public FirebaseHabitLog(long habitId, long date, boolean completed) {
            this.habitId = habitId;
            this.date = date;
            this.completed = completed;
        }

        public long getHabitId() {
            return habitId;
        }

        public void setHabitId(long habitId) {
            this.habitId = habitId;
        }

        public long getDate() {
            return date;
        }

        public void setDate(long date) {
            this.date = date;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }
} 