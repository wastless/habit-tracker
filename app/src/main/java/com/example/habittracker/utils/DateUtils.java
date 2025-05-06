package com.example.habittracker.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DateUtils {
    
    /**
     * Возвращает день недели (0-6, где 0 = Понедельник, 6 = Воскресенье) для указанной даты
     */
    public static int getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        
        // Calendar.DAY_OF_WEEK начинается с воскресенья (1) и заканчивается субботой (7)
        // Конвертируем к нашему формату (0 = Понедельник, 6 = Воскресенье)
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        // Смещаем, чтобы получить 0 = Понедельник
        return day == 0 ? 6 : day - 1;
    }
    
    /**
     * Проверяет, должна ли привычка с данной частотой показываться в текущий день недели
     */
    public static boolean shouldShowHabitToday(String frequency, Date date) {
        if (frequency == null || frequency.isEmpty()) {
            return true; // Если частота не указана, показываем каждый день
        }
        
        int dayOfWeek = getDayOfWeek(date);
        Set<Integer> daysSet = parseDaysFromFrequency(frequency);
        
        return daysSet.contains(dayOfWeek);
    }
    
    /**
     * Преобразует строку с частотой в множество дней недели
     */
    public static Set<Integer> parseDaysFromFrequency(String frequency) {
        Set<Integer> days = new HashSet<>();
        
        if (frequency != null && !frequency.isEmpty()) {
            String[] dayStrings = frequency.split(",");
            for (String dayString : dayStrings) {
                try {
                    days.add(Integer.parseInt(dayString.trim()));
                } catch (NumberFormatException e) {
                    // Игнорируем неверный формат
                }
            }
        }
        
        return days;
    }
} 