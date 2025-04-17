package com.example.habittracker;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddHabitBottomSheetFragment extends BottomSheetDialogFragment {
    private EditText habitNameInput;
    private Button saveButton;
    private Set<Integer> selectedDays = new HashSet<>();
    private OnHabitAddedListener habitAddedListener;

    public interface OnHabitAddedListener {
        void onHabitAdded(String name, Set<Integer> days);
    }

    public void setOnHabitAddedListener(OnHabitAddedListener listener) {
        this.habitAddedListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_habit, container, false);
        
        habitNameInput = view.findViewById(R.id.habitNameInput);
        saveButton = view.findViewById(R.id.saveButton);

        // Настройка кнопок дней недели
        int[] dayButtonIds = new int[]{
            R.id.mondayButton, R.id.tuesdayButton, R.id.wednesdayButton,
            R.id.thursdayButton, R.id.fridayButton, R.id.saturdayButton, R.id.sundayButton
        };

        for (int i = 0; i < dayButtonIds.length; i++) {
            final int dayIndex = i;
            TextView dayButton = view.findViewById(dayButtonIds[i]);
            dayButton.setOnClickListener(v -> toggleDaySelection(dayButton, dayIndex));
        }
        
        habitNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        saveButton.setOnClickListener(v -> {
            if (habitAddedListener != null) {
                habitAddedListener.onHabitAdded(habitNameInput.getText().toString(), selectedDays);
            }
            dismiss();
        });

        return view;
    }

    private void toggleDaySelection(TextView dayButton, int dayIndex) {
        if (selectedDays.contains(dayIndex)) {
            selectedDays.remove(dayIndex);
            dayButton.setBackgroundResource(R.drawable.day_button_background);
        } else {
            selectedDays.add(dayIndex);
            dayButton.setBackgroundResource(R.drawable.day_button_active_background);
        }
        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        saveButton.setEnabled(habitNameInput.length() > 0 && !selectedDays.isEmpty());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            setupFullHeight(bottomSheetDialog);
        });
        return dialog;
    }

    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.setLayoutParams(layoutParams);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
} 