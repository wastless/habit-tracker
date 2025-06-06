package com.example.habittracker.data;

import android.content.Context;
import android.os.AsyncTask;

import com.example.habittracker.data.database.AppDatabase;
import com.example.habittracker.data.database.Habit;
import com.example.habittracker.data.database.HabitDao;
import com.example.habittracker.data.database.HabitLog;
import com.example.habittracker.data.database.HabitLogDao;
import com.example.habittracker.firebase.FirebaseSyncManager;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HabitRepository {
    private final HabitDao habitDao;
    private final HabitLogDao habitLogDao;
    private final ExecutorService executorService;
    private final FirebaseSyncManager firebaseSyncManager;
    private final boolean syncEnabled;

    public HabitRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        habitDao = db.habitDao();
        habitLogDao = db.habitLogDao();
        executorService = Executors.newFixedThreadPool(4);
        
        // Инициализация Firebase Sync Manager
        firebaseSyncManager = FirebaseSyncManager.getInstance();
        syncEnabled = true; // Можно сделать настройку в приложении
    }

    // Habit operations
    public void insertHabit(Habit habit, OnHabitInsertListener listener) {
        executorService.execute(() -> {
            long id = habitDao.insert(habit);
            habit.setId(id);
            
            // Синхронизация с Firebase
            if (syncEnabled) {
                firebaseSyncManager.uploadHabitToFirebase(habit);
            }
            
            if (listener != null) {
                listener.onHabitInserted(habit);
            }
        });
    }

    public void updateHabit(Habit habit) {
        executorService.execute(() -> {
            habitDao.update(habit);
            
            // Синхронизация с Firebase
            if (syncEnabled) {
                firebaseSyncManager.uploadHabitToFirebase(habit);
            }
        });
    }

    public void deleteHabit(Habit habit) {
        executorService.execute(() -> habitDao.delete(habit));
    }

    public void getAllHabits(OnHabitsLoadedListener listener) {
        executorService.execute(() -> {
            List<Habit> habits = habitDao.getAllHabits();
            listener.onHabitsLoaded(habits);
        });
    }

    public void getHabitById(long habitId, OnHabitLoadedListener listener) {
        executorService.execute(() -> {
            Habit habit = habitDao.getHabitById(habitId);
            listener.onHabitLoaded(habit);
        });
    }

    // HabitLog operations
    public void insertHabitLog(HabitLog habitLog) {
        executorService.execute(() -> {
            habitLogDao.insert(habitLog);
            
            // Синхронизация с Firebase
            if (syncEnabled) {
                firebaseSyncManager.uploadHabitLogToFirebase(habitLog);
            }
        });
    }

    public void updateHabitLog(HabitLog habitLog) {
        executorService.execute(() -> {
            habitLogDao.update(habitLog);
            
            // Синхронизация с Firebase
            if (syncEnabled) {
                firebaseSyncManager.uploadHabitLogToFirebase(habitLog);
            }
        });
    }

    public void deleteHabitLog(HabitLog habitLog) {
        executorService.execute(() -> habitLogDao.delete(habitLog));
    }

    public void getLogsForHabit(long habitId, OnHabitLogsLoadedListener listener) {
        executorService.execute(() -> {
            List<HabitLog> logs = habitLogDao.getLogsForHabit(habitId);
            listener.onHabitLogsLoaded(logs);
        });
    }

    public void getLogForHabitOnDate(long habitId, Date date, OnHabitLogLoadedListener listener) {
        executorService.execute(() -> {
            HabitLog log = habitLogDao.getLogForHabitOnDate(habitId, date);
            listener.onHabitLogLoaded(log);
        });
    }

    public void toggleHabitCompletion(long habitId, Date date, boolean completed, OnHabitLogUpdatedListener listener) {
        executorService.execute(() -> {
            HabitLog log = habitLogDao.getLogForHabitOnDate(habitId, date);
            if (log == null) {
                log = new HabitLog(habitId, date, completed);
                habitLogDao.insert(log);
            } else {
                log.setCompleted(completed);
                habitLogDao.update(log);
            }
            
            // Синхронизация с Firebase
            if (syncEnabled) {
                firebaseSyncManager.syncHabitCompletion(log);
            }
            
            if (listener != null) {
                listener.onHabitLogUpdated(log);
            }
        });
    }
    
    // Запуск синхронизации с Firebase
    public void syncWithFirebase() {
        if (syncEnabled) {
            firebaseSyncManager.syncData();
        }
    }

    // Callback interfaces
    public interface OnHabitInsertListener {
        void onHabitInserted(Habit habit);
    }

    public interface OnHabitsLoadedListener {
        void onHabitsLoaded(List<Habit> habits);
    }

    public interface OnHabitLoadedListener {
        void onHabitLoaded(Habit habit);
    }

    public interface OnHabitLogsLoadedListener {
        void onHabitLogsLoaded(List<HabitLog> logs);
    }

    public interface OnHabitLogLoadedListener {
        void onHabitLogLoaded(HabitLog log);
    }

    public interface OnHabitLogUpdatedListener {
        void onHabitLogUpdated(HabitLog log);
    }
} 