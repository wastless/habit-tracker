package com.example.habittracker.data.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import java.util.Date;

@Entity(tableName = "habit_logs",
        foreignKeys = @ForeignKey(
                entity = Habit.class,
                parentColumns = "id",
                childColumns = "habit_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("habit_id")})
public class HabitLog {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "habit_id")
    private long habitId;

    @ColumnInfo(name = "date")
    private Date date;

    @ColumnInfo(name = "completed")
    private boolean completed;

    public HabitLog(long habitId, Date date, boolean completed) {
        this.habitId = habitId;
        this.date = date;
        this.completed = completed;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getHabitId() {
        return habitId;
    }

    public void setHabitId(long habitId) {
        this.habitId = habitId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
} 