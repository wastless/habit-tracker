package com.example.habittracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Set;

public class HabitsFragment extends Fragment implements AddHabitBottomSheetFragment.OnHabitAddedListener {
    private LinearLayout habitsContainer;
    private LinearLayout emptyStateContainer;
    private TextView noHabitsText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habits, container, false);
        
        habitsContainer = view.findViewById(R.id.habitsContainer);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        noHabitsText = view.findViewById(R.id.noHabitsText);

        // Убеждаемся, что контейнер пустой при старте
        habitsContainer.removeAllViews();
        emptyStateContainer.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onHabitAdded(String name, Set<Integer> days) {
        // Скрываем контейнер с текстом "Нет привычек"
        emptyStateContainer.setVisibility(View.GONE);
        
        // Создаем новую привычку
        View habitView = getLayoutInflater().inflate(R.layout.item_habit, habitsContainer, false);
        TextView habitTitle = habitView.findViewById(R.id.habitTitle);
        habitTitle.setText(name);

        // Настраиваем кнопку удаления
        View deleteButton = habitView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            habitsContainer.removeView(habitView);
            
            // Показываем контейнер с текстом "Нет привычек", если удалили последнюю привычку
            if (habitsContainer.getChildCount() == 0) {
                emptyStateContainer.setVisibility(View.VISIBLE);
            }
        });

        // Добавляем привычку в контейнер
        habitsContainer.addView(habitView);
    }
} 