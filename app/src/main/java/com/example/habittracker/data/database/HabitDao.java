package com.example.habittracker.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface HabitDao {
    @Insert
    long insert(Habit habit);

    @Update
    void update(Habit habit);

    @Delete
    void delete(Habit habit);

    @Query("SELECT * FROM habits ORDER BY created_at DESC")
    List<Habit> getAllHabits();

    @Query("SELECT * FROM habits WHERE id = :habitId")
    Habit getHabitById(long habitId);
} 