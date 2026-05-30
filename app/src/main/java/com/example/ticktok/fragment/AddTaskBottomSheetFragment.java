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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;

import com.example.ticktok.R;
import com.example.ticktok.model.Task;
import com.example.ticktok.reminder.ReminderManager;
import com.example.ticktok.util.UserFirestorePaths;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.CollectionReference;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddTaskBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_CATEGORY_ID = "arg_category_id";
    private static final String ARG_PREFILL_DUE_DATE = "arg_prefill_due_date";
    private static final String ARG_MODE = "arg_mode";
    private static final String ARG_TASK_ID = "arg_task_id";
    private static final String ARG_TASK_TITLE = "arg_task_title";
    private static final String ARG_TASK_DESCRIPTION = "arg_task_description";
    private static final String ARG_TASK_PRIORITY = "arg_task_priority";
    private static final String ARG_TASK_DUE_DATE = "arg_task_due_date";
    private static final String ARG_TASK_REMINDER_TIME = "arg_task_reminder_time";

    private static final int MODE_ADD = 0;
    private static final int MODE_EDIT = 1;
    private static final int PRIORITY_NONE = 0;
    private static final int PRIORITY_LOW = 1;
    private static final int PRIORITY_MEDIUM = 2;
    private static final int PRIORITY_HIGH = 3;

    private long selectedDueDate = 0;
    private long selectedReminderTime = 0;
    private int selectedPriority = PRIORITY_NONE;
    private EditText taskInput;
    private ImageButton btnFlag;
    private String categoryId;

    private boolean isEditMode;
    @Nullable
    private String editingTaskId;
    @Nullable
    private String prefillTitle;
    @Nullable
    private String prefillDescription;

    private long originalReminderTime = 0L;

    public AddTaskBottomSheetFragment() {
    }

    public AddTaskBottomSheetFragment(@Nullable String categoryId) {
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        setArguments(args);
    }

    public AddTaskBottomSheetFragment(@Nullable String categoryId, @Nullable Long prefillDueDate) {
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        if (prefillDueDate != null && prefillDueDate > 0) {
            args.putLong(ARG_PREFILL_DUE_DATE, prefillDueDate);
        }
        setArguments(args);
    }

    @NonNull
    public static AddTaskBottomSheetFragment newInstanceForEdit(@NonNull Task task) {
        AddTaskBottomSheetFragment fragment = new AddTaskBottomSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_EDIT);
        args.putString(ARG_TASK_ID, task.getId());
        args.putString(ARG_CATEGORY_ID, task.getCategoryId());
        args.putString(ARG_TASK_TITLE, task.getTitle());
        args.putString(ARG_TASK_DESCRIPTION, task.getDescription());
        args.putInt(ARG_TASK_PRIORITY, task.getPriority());
        if (task.getDueDate() != null && task.getDueDate() > 0) {
            args.putLong(ARG_TASK_DUE_DATE, task.getDueDate());
        }
        if (task.getReminderTime() != null && task.getReminderTime() > 0) {
            args.putLong(ARG_TASK_REMINDER_TIME, task.getReminderTime());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            int mode = args.getInt(ARG_MODE, MODE_ADD);
            isEditMode = mode == MODE_EDIT;
            if (isEditMode) {
                editingTaskId = args.getString(ARG_TASK_ID);
                prefillTitle = args.getString(ARG_TASK_TITLE);
                prefillDescription = args.getString(ARG_TASK_DESCRIPTION);
                selectedPriority = args.getInt(ARG_TASK_PRIORITY, PRIORITY_NONE);
                if (args.containsKey(ARG_TASK_DUE_DATE)) {
                    selectedDueDate = normalizeToStartOfDay(args.getLong(ARG_TASK_DUE_DATE, 0L));
                }

                if (args.containsKey(ARG_TASK_REMINDER_TIME)) {
                    selectedReminderTime = args.getLong(ARG_TASK_REMINDER_TIME, 0L);
                    originalReminderTime = selectedReminderTime;
                }
            }

            categoryId = args.getString(ARG_CATEGORY_ID);
            if (args.containsKey(ARG_PREFILL_DUE_DATE)) {
                selectedDueDate = normalizeToStartOfDay(args.getLong(ARG_PREFILL_DUE_DATE, 0L));
            }
        }
    }

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
            int bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
            FrameLayout bottomSheet = dialog.findViewById(bottomSheetId);
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
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> handleSendTask());
        }
        ImageButton btnCalendar = view.findViewById(R.id.btnCalendar);
        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> showDatePicker());
            if (selectedDueDate > 0) {
                btnCalendar.setColorFilter(android.graphics.Color.parseColor("#FF9800"));
            }
        }
        btnFlag = view.findViewById(R.id.btnFlag);
        if (btnFlag != null) {
            btnFlag.setOnClickListener(v -> showPriorityMenu(v));
            applySelectedPriorityColor();
        }

        ImageButton btnMore = view.findViewById(R.id.btnMore);
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> showReminderTimePicker());
            btnMore.setOnLongClickListener(v -> {
                selectedReminderTime = 0L;
                btnMore.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_white));
                Toast.makeText(requireContext(), R.string.reminder_cleared, Toast.LENGTH_SHORT).show();
                return true;
            });
            if (selectedReminderTime > 0) {
                btnMore.setColorFilter(android.graphics.Color.parseColor("#FF9800"));
            }
        }

        if (isEditMode) {
            String title = prefillTitle == null ? "" : prefillTitle.trim();
            String desc = prefillDescription == null ? "" : prefillDescription.trim();
            String combined;
            if (!desc.isEmpty()) {
                combined = title + "\n" + desc;
            } else {
                combined = title;
            }
            taskInput.setText(combined);
            taskInput.setSelection(taskInput.getText() != null ? taskInput.getText().length() : 0);
        }

        updateSendMicVisibility(btnMicrophone, btnSend, taskInput.getText());

        taskInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendMicVisibility(btnMicrophone, btnSend, s);
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
        if (selectedDueDate > 0) {
            c.setTimeInMillis(selectedDueDate);
        }
        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    java.util.Calendar selectedCalendar = java.util.Calendar.getInstance();
                    selectedCalendar.set(year1, monthOfYear, dayOfMonth);
                    selectedDueDate = normalizeToStartOfDay(selectedCalendar.getTimeInMillis());

                    if (selectedReminderTime > 0) {
                        java.util.Calendar reminderCal = java.util.Calendar.getInstance();
                        reminderCal.setTimeInMillis(selectedReminderTime);
                        int hour = reminderCal.get(java.util.Calendar.HOUR_OF_DAY);
                        int minute = reminderCal.get(java.util.Calendar.MINUTE);

                        java.util.Calendar newReminder = java.util.Calendar.getInstance();
                        newReminder.setTimeInMillis(selectedDueDate);
                        newReminder.set(java.util.Calendar.HOUR_OF_DAY, hour);
                        newReminder.set(java.util.Calendar.MINUTE, minute);
                        newReminder.set(java.util.Calendar.SECOND, 0);
                        newReminder.set(java.util.Calendar.MILLISECOND, 0);
                        selectedReminderTime = newReminder.getTimeInMillis();
                    }

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

    private void handleSendTask() {
        if (taskInput == null) {
            return;
        }

        String rawInput = taskInput.getText() != null ? taskInput.getText().toString().trim() : "";
        if (rawInput.isEmpty()) {
            Toast.makeText(requireContext(), R.string.add_task_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = extractTitle(rawInput);
        String description = extractDescription(rawInput);
        Long dueDateValue = selectedDueDate > 0
                ? normalizeToStartOfDay(selectedDueDate)
                : getStartOfTodayMillis();

        long reminderTimeValue = selectedReminderTime > 0 ? selectedReminderTime : 0L;
        if (reminderTimeValue > 0 && reminderTimeValue <= System.currentTimeMillis()) {

            reminderTimeValue = 0L;
            selectedReminderTime = 0L;
            View root = getView();
            if (root != null) {
                ImageButton btnMore = root.findViewById(R.id.btnMore);
                if (btnMore != null) {
                    btnMore.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_white));
                }
            }
            Toast.makeText(requireContext(), R.string.reminder_time_in_past_cleared, Toast.LENGTH_SHORT).show();
        }

        final long finalReminderTimeValue = reminderTimeValue;

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            Toast.makeText(requireContext(), R.string.auth_error_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode) {
            if (editingTaskId == null || editingTaskId.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.delete_task_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("title", title);
            updates.put("description", description);
            updates.put("priority", selectedPriority);
            updates.put("dueDate", dueDateValue);
            updates.put("categoryId", categoryId);
            updates.put("reminderTime", finalReminderTimeValue);

            tasksRef.document(editingTaskId.trim())
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        Task temp = new Task();
                        temp.setId(editingTaskId.trim());
                        temp.setTitle(title);
                        temp.setReminderTime(finalReminderTimeValue);
                        if (finalReminderTimeValue > System.currentTimeMillis()) {
                            ReminderManager.ensureNotificationPermission(requireActivity());
                        }
                        ReminderManager.setReminder(requireContext(), temp);

                        Toast.makeText(requireContext(), R.string.edit_task_success, Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        addNewTaskWithNextOrder(tasksRef, title, description, dueDateValue, finalReminderTimeValue);
    }

    private void addNewTaskWithNextOrder(@NonNull CollectionReference tasksRef,
                                        @NonNull String title,
                                        @NonNull String description,
                                        @NonNull Long dueDateValue,
                                        long reminderTimeValue) {
        int nextOrder = (int) (System.currentTimeMillis() / 1000L);
        Task task = new Task(title, description, categoryId, selectedPriority, dueDateValue, reminderTimeValue, nextOrder);
        task.setCompleted(false);
        task.setOrder(nextOrder);
        task.setCreatedAt(null);

        tasksRef
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update("createdAt", FieldValue.serverTimestamp());

                    task.setId(documentReference.getId());
                    if (reminderTimeValue > System.currentTimeMillis()) {
                        ReminderManager.ensureNotificationPermission(requireActivity());
                    }
                    ReminderManager.setReminder(requireContext(), task);

                    Toast.makeText(requireContext(), R.string.add_task_success, Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showReminderTimePicker() {
        restoreTaskInputFocus();


        long baseDate = selectedDueDate > 0
                ? selectedDueDate
                : getStartOfTodayMillis();

        final java.util.Calendar base = java.util.Calendar.getInstance();
        base.setTimeInMillis(baseDate);

        int initialHour;
        int initialMinute;
        if (selectedReminderTime > 0) {
            java.util.Calendar existing = java.util.Calendar.getInstance();
            existing.setTimeInMillis(selectedReminderTime);
            initialHour = existing.get(java.util.Calendar.HOUR_OF_DAY);
            initialMinute = existing.get(java.util.Calendar.MINUTE);
        } else {
            java.util.Calendar now = java.util.Calendar.getInstance();
            initialHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            initialMinute = now.get(java.util.Calendar.MINUTE);
        }

        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                requireContext(),
                (timeView, hourOfDay, minute) -> {
                    java.util.Calendar picked = java.util.Calendar.getInstance();
                    picked.setTimeInMillis(baseDate);
                    picked.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                    picked.set(java.util.Calendar.MINUTE, minute);
                    picked.set(java.util.Calendar.SECOND, 0);
                    picked.set(java.util.Calendar.MILLISECOND, 0);

                    selectedReminderTime = picked.getTimeInMillis();

                    View root = getView();
                    if (root != null) {
                        ImageButton btnMore = root.findViewById(R.id.btnMore);
                        if (btnMore != null) {
                            btnMore.setColorFilter(android.graphics.Color.parseColor("#FF9800"));
                        }
                    }
                },
                initialHour,
                initialMinute,
                true
        );
        timePickerDialog.show();
    }

    private void updateSendMicVisibility(@Nullable ImageButton btnMicrophone,
                                        @Nullable ImageButton btnSend,
                                        @Nullable CharSequence text) {
        boolean hasText = text != null && !text.toString().trim().isEmpty();
        if (btnMicrophone != null) {
            btnMicrophone.setVisibility(hasText ? View.GONE : View.VISIBLE);
        }
        if (btnSend != null) {
            btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
        }
    }

    private String extractTitle(String rawInput) {
        String[] lines = rawInput.split("\\n", 2);
        String firstLine = lines[0].trim();
        return firstLine.isEmpty() ? rawInput : firstLine;
    }

    private String extractDescription(String rawInput) {
        String[] lines = rawInput.split("\\n", 2);
        if (lines.length < 2) {
            return "";
        }
        return lines[1].trim();
    }

    private long normalizeToStartOfDay(long millis) {
        if (millis <= 0) {
            return 0L;
        }
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getStartOfTodayMillis() {
        return normalizeToStartOfDay(System.currentTimeMillis());
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

