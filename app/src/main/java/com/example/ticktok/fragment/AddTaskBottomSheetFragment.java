package com.example.ticktok.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;

import com.example.ticktok.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddTaskBottomSheetFragment extends BottomSheetDialogFragment {
    private static final int PRIORITY_NONE = 0;
    private static final int PRIORITY_LOW = 1;
    private static final int PRIORITY_MEDIUM = 2;
    private static final int PRIORITY_HIGH = 3;

    private long selectedDueDate = 0;
    private int selectedPriority = PRIORITY_NONE;
    private EditText taskInput;
    private ImageButton btnFlag;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            );
        }

        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) {
                return;
            }

            bottomSheet.setBackgroundResource(android.R.color.transparent);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskInput = view.findViewById(R.id.etTaskInput);
        if (taskInput == null) {
            return;
        }

        ImageButton btnMicrophone = view.findViewById(R.id.btnMicrophone);
        ImageButton btnSend = view.findViewById(R.id.btnSend);
        ImageButton btnCalendar = view.findViewById(R.id.btnCalendar);
        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> showDatePicker());
        }
        btnFlag = view.findViewById(R.id.btnFlag);
        if (btnFlag != null) {
            btnFlag.setOnClickListener(v -> showPriorityMenu(v));
            applySelectedPriorityColor();
        }
        taskInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) {
                    if (btnMicrophone != null) btnMicrophone.setVisibility(View.GONE);
                    if (btnSend != null) btnSend.setVisibility(View.VISIBLE);
                } else {
                    if (btnMicrophone != null) btnMicrophone.setVisibility(View.VISIBLE);
                    if (btnSend != null) btnSend.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        taskInput.requestFocus();
        taskInput.post(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(taskInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        taskInput = null;
        btnFlag = null;
    }

    private void showDatePicker() {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    java.util.Calendar selectedCalendar = java.util.Calendar.getInstance();
                    selectedCalendar.set(year1, monthOfYear, dayOfMonth);
                    selectedDueDate = selectedCalendar.getTimeInMillis();

                    View root = getView();
                    if (root != null) {
                        ImageButton btnCalendar = root.findViewById(R.id.btnCalendar);
                        if (btnCalendar != null) {
                            btnCalendar.setColorFilter(android.graphics.Color.parseColor("#FF9800"));
                        }
                    }
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showPriorityMenu(View anchor) {
        restoreTaskInputFocus();

        PriorityOption[] options = new PriorityOption[] {
                new PriorityOption(PRIORITY_HIGH, getString(R.string.priority_high), "#F44336"),
                new PriorityOption(PRIORITY_MEDIUM, getString(R.string.priority_medium), "#FFC107"),
                new PriorityOption(PRIORITY_LOW, getString(R.string.priority_low), "#2196F3"),
                new PriorityOption(PRIORITY_NONE, getString(R.string.priority_none), "#9E9E9E")
        };

        ListPopupWindow popupWindow = new ListPopupWindow(
                requireContext(),
                null,
                0,
                R.style.Widget_TickTok_PriorityListPopupWindow
        );
        popupWindow.setAnchorView(anchor);
        popupWindow.setModal(false);
        popupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        popupWindow.setContentWidth(dpToPx(196));
        popupWindow.setDropDownGravity(Gravity.START);
        popupWindow.setAdapter(createPriorityAdapter(options));
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            selectedPriority = options[position].value;
            applySelectedPriorityColor();
            popupWindow.dismiss();
        });
        popupWindow.setOnDismissListener(this::restoreTaskInputFocus);
        popupWindow.show();
        restoreTaskInputFocus();
    }

    private ListAdapter createPriorityAdapter(PriorityOption[] options) {
        return new ArrayAdapter<PriorityOption>(requireContext(), 0, options) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View itemView = convertView;
                if (itemView == null) {
                    itemView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_priority_option, parent, false);
                }
                PriorityOption option = getItem(position);
                if (option == null) {
                    return itemView;
                }

                ImageView icon = itemView.findViewById(R.id.ivPriorityIcon);
                TextView title = itemView.findViewById(R.id.tvPriorityTitle);
                if (icon != null) {
                    icon.setColorFilter(android.graphics.Color.parseColor(option.tintHex));
                }
                if (title != null) {
                    title.setText(option.title);
                }
                return itemView;
            }
        };
    }

    private void applySelectedPriorityColor() {
        if (btnFlag == null) {
            return;
        }
        btnFlag.setColorFilter(resolvePriorityColor(selectedPriority));
    }

    private void restoreTaskInputFocus() {
        if (taskInput == null) {
            return;
        }
        taskInput.requestFocus();

        Context context = getContext();
        if (context == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(taskInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private int resolvePriorityColor(int priority) {
        switch (priority) {
            case PRIORITY_HIGH:
                return android.graphics.Color.parseColor("#F44336");
            case PRIORITY_MEDIUM:
                return android.graphics.Color.parseColor("#FFC107");
            case PRIORITY_LOW:
                return android.graphics.Color.parseColor("#2196F3");
            case PRIORITY_NONE:
            default:
                return ContextCompat.getColor(requireContext(), R.color.text_white);
        }
    }

    private static class PriorityOption {
        final int value;
        final String title;
        final String tintHex;

        PriorityOption(int value, String title, String tintHex) {
            this.value = value;
            this.title = title;
            this.tintHex = tintHex;
        }

        @NonNull
        @Override
        public String toString() {
            return title;
        }
    }
}

