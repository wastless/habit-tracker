package com.example.habittracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.habittracker.data.HabitRepository;
import com.example.habittracker.data.database.Habit;
import com.example.habittracker.data.database.HabitLog;
import com.example.habittracker.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class HabitsFragment extends Fragment implements AddHabitBottomSheetFragment.OnHabitAddedListener {
    private LinearLayout habitsContainer;
    private LinearLayout emptyStateContainer;
    private TextView noHabitsText;
    private HabitRepository repository;
    private Date currentDate = new Date(); // Текущая дата для отображения привычек
    
    // Кнопки дней недели
    private TextView[] dayButtons = new TextView[7];
    private int selectedDayOfWeek; // 0 = Пн, 6 = Вс

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habits, container, false);
        
        habitsContainer = view.findViewById(R.id.habitsContainer);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        noHabitsText = view.findViewById(R.id.noHabitsText);

        // Инициализация кнопок дней недели
        dayButtons[0] = view.findViewById(R.id.mondayButton);
        dayButtons[1] = view.findViewById(R.id.tuesdayButton);
        dayButtons[2] = view.findViewById(R.id.wednesdayButton);
        dayButtons[3] = view.findViewById(R.id.thursdayButton);
        dayButtons[4] = view.findViewById(R.id.fridayButton);
        dayButtons[5] = view.findViewById(R.id.saturdayButton);
        dayButtons[6] = view.findViewById(R.id.sundayButton);
        
        // Устанавливаем текущий день недели
        selectedDayOfWeek = DateUtils.getDayOfWeek(currentDate);
        updateDayButtonsUI();
        
        // Добавляем обработчики нажатий для кнопок дней
        for (int i = 0; i < dayButtons.length; i++) {
            final int dayIndex = i;
            dayButtons[i].setOnClickListener(v -> selectDay(dayIndex));
        }

        // Initialize repository
        repository = new HabitRepository(requireContext());

        // Load habits for today
        loadHabitsForSelectedDay();

        return view;
    }
    
    private void selectDay(int dayIndex) {
        // Если выбран другой день
        if (selectedDayOfWeek != dayIndex) {
            selectedDayOfWeek = dayIndex;
            updateDayButtonsUI();
            
            // Обновляем дату для выбранного дня недели
            updateDateForSelectedDay();
            
            // Загружаем привычки для выбранного дня
            loadHabitsForSelectedDay();
        }
    }
    
    private void updateDayButtonsUI() {
        // Обновляем UI для кнопок дней недели
        for (int i = 0; i < dayButtons.length; i++) {
            if (i == selectedDayOfWeek) {
                dayButtons[i].setBackgroundResource(R.drawable.day_button_active_background);
            } else {
                dayButtons[i].setBackgroundResource(R.drawable.day_button_background);
            }
        }
    }
    
    private void updateDateForSelectedDay() {
        // Создаем календарь на основе текущего дня
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date()); // Сегодня
        
        // Получаем текущий день недели (в Java Calendar понедельник = 2, воскресенье = 1)
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Преобразуем к нашему формату (0 = Пн, 6 = Вс)
        currentDayOfWeek = currentDayOfWeek == 1 ? 6 : currentDayOfWeek - 2;
        
        // Вычисляем разницу между выбранным днем и текущим
        int dayDiff = selectedDayOfWeek - currentDayOfWeek;
        
        // Смещаем дату
        calendar.add(Calendar.DAY_OF_MONTH, dayDiff);
        currentDate = calendar.getTime();
    }

    private void loadHabitsForSelectedDay() {
        // Clear existing habits
        habitsContainer.removeAllViews();
        
        // Load all habits from database
        repository.getAllHabits(habits -> {
            // Фильтруем привычки, которые должны отображаться в выбранный день
            List<Habit> filteredHabits = new ArrayList<>();
            for (Habit habit : habits) {
                if (DateUtils.shouldShowHabitToday(habit.getFrequency(), currentDate)) {
                    filteredHabits.add(habit);
                }
            }
            
            requireActivity().runOnUiThread(() -> {
                if (filteredHabits.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    noHabitsText.setText("Нет привычек на этот день");
                } else {
                    emptyStateContainer.setVisibility(View.GONE);
                    for (Habit habit : filteredHabits) {
                        addHabitToUI(habit);
                    }
                }
            });
        });
    }

    private void addHabitToUI(Habit habit) {
        View habitView = getLayoutInflater().inflate(R.layout.item_habit, habitsContainer, false);
        TextView habitTitle = habitView.findViewById(R.id.habitTitle);
        habitTitle.setText(habit.getName());

        // Get checkbox from layout
        CheckBox checkBox = habitView.findViewById(R.id.habitCheckbox);
        
        // Check completion status for selected date
        repository.getLogForHabitOnDate(habit.getId(), currentDate, log -> {
            requireActivity().runOnUiThread(() -> {
                if (log != null) {
                    checkBox.setChecked(log.isCompleted());
                }
            });
        });
        
        // Set checkbox listener
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repository.toggleHabitCompletion(habit.getId(), currentDate, isChecked, null);
        });

        // Set delete button listener
        View deleteButton = habitView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            repository.deleteHabit(habit);
            habitsContainer.removeView(habitView);
            
            // Show empty state if needed
            if (habitsContainer.getChildCount() == 0) {
                emptyStateContainer.setVisibility(View.VISIBLE);
                noHabitsText.setText("Нет привычек на этот день");
            }
        });

        // Add habit to container
        habitsContainer.addView(habitView);
    }

    @Override
    public void onHabitAdded(String name, Set<Integer> days) {
        // Convert days to frequency string
        StringBuilder frequency = new StringBuilder();
        for (Integer day : days) {
            frequency.append(day).append(",");
        }
        if (frequency.length() > 0) {
            frequency.deleteCharAt(frequency.length() - 1);
        }
        
        // Create new habit and save to database
        Habit habit = new Habit(name, frequency.toString());
        repository.insertHabit(habit, insertedHabit -> {
            // Проверяем, должна ли привычка отображаться в выбранный день
            if (DateUtils.shouldShowHabitToday(insertedHabit.getFrequency(), currentDate)) {
                requireActivity().runOnUiThread(() -> {
                    // Hide empty state
                    emptyStateContainer.setVisibility(View.GONE);
                    
                    // Add to UI
                    addHabitToUI(insertedHabit);
                });
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Обновляем выбранный день и список привычек при возвращении к фрагменту
        selectedDayOfWeek = DateUtils.getDayOfWeek(new Date());
        updateDayButtonsUI();
        updateDateForSelectedDay();
        loadHabitsForSelectedDay();
    }
} 