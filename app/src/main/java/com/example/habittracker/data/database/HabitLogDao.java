package com.example.habittracker.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.Date;
import java.util.List;

@Dao
public interface HabitLogDao {
    @Insert
    long insert(HabitLog habitLog);

    @Update
    void update(HabitLog habitLog);

    @Delete
    void delete(HabitLog habitLog);

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId ORDER BY date DESC")
    List<HabitLog> getLogsForHabit(long habitId);

    @Query("SELECT * FROM habit_logs WHERE habit_id = :habitId AND date BETWEEN :startDate AND :endDate")
    List<HabitLog> getLogsForHabitInDateRange(long habitId, Date startDate, Date endDate);

    @Query("SELECT * FROM habit_logs WHERE date = :date AND habit_id = :habitId")
    HabitLog getLogForHabitOnDate(long habitId, Date date);

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habit_id = :habitId AND completed = 1")
    int getCompletedCountForHabit(long habitId);
} 