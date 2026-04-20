package com.example.ticktok.fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.ticktok.R;
import com.example.ticktok.model.Task;
import com.example.ticktok.util.UserFirestorePaths;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import java.util.Locale;

public class PomodoroFragment extends Fragment {

    private static final long FOCUS_DURATION_MILLIS = 25L * 60L * 1000L;
    private static final long BREAK_DURATION_MILLIS = 5L * 60L * 1000L;
    private static final int MIN_DURATION_MINUTES = 5;
    private static final int MAX_DURATION_MINUTES = 90;
    private static final long MIN_DURATION_MILLIS = MIN_DURATION_MINUTES * 60_000L;
    private static final long MAX_DURATION_MILLIS = MAX_DURATION_MINUTES * 60_000L;
    private static final String STATE_MODE = "state_mode";
    private static final String STATE_TOTAL_MILLIS = "state_total_millis";
    private static final String STATE_REMAINING_MILLIS = "state_remaining_millis";
    private static final String STATE_IS_RUNNING = "state_is_running";
    private static final String STATE_IS_PAUSED = "state_is_paused";
    private static final String STATE_SELECTED_FOCUS_MILLIS = "state_selected_focus_millis";
    private static final String STATE_SELECTED_TASK_ID = "state_selected_task_id";
    private static final String STATE_SELECTED_TASK_TITLE = "state_selected_task_title";

    private enum SessionMode {
        FOCUS,
        BREAK
    }

    private TextView tvTaskSelector;
    private TextView tvPomodoroTimer;
    private ProgressBar progressPomodoro;
    private AppCompatButton btnStartPomodoro;
    private AppCompatButton btnResetPomodoro;
    private View viewDurationPickerScrim;
    private MaterialCardView cardDurationPicker;
    private EditText etCustomDurationMinutes;
    private MaterialButton btnApplyDuration;
    private MaterialButton btnCancelDuration;

    private CountDownTimer countDownTimer;
    private SessionMode currentMode = SessionMode.FOCUS;
    private long totalMillis = FOCUS_DURATION_MILLIS;
    private long remainingMillis = FOCUS_DURATION_MILLIS;
    private long selectedFocusDurationMillis = FOCUS_DURATION_MILLIS;
    private long runningEndElapsedRealtime = -1L;
    private boolean isRunning;
    private boolean isPaused;
    @Nullable
    private Integer selectedPickerMinutes;
    @Nullable
    private String selectedTaskId;
    @Nullable
    private String selectedTaskTitle;
    private final List<MaterialButton> durationPresetButtons = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pomodoro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTaskSelector = view.findViewById(R.id.tvTaskSelector);
        tvPomodoroTimer = view.findViewById(R.id.tvPomodoroTimer);
        progressPomodoro = view.findViewById(R.id.progressPomodoro);
        btnStartPomodoro = view.findViewById(R.id.btnStartPomodoro);
        btnResetPomodoro = view.findViewById(R.id.btnResetPomodoro);
        viewDurationPickerScrim = view.findViewById(R.id.viewDurationPickerScrim);
        cardDurationPicker = view.findViewById(R.id.cardDurationPicker);
        etCustomDurationMinutes = view.findViewById(R.id.etCustomDurationMinutes);
        btnApplyDuration = view.findViewById(R.id.btnApplyDuration);
        btnCancelDuration = view.findViewById(R.id.btnCancelDuration);

        ImageButton btnPomodoroHistory = view.findViewById(R.id.btnPomodoroHistory);
        ImageButton btnPomodoroMore = view.findViewById(R.id.btnPomodoroMore);
        View pomodoroClockContainer = view.findViewById(R.id.pomodoroClockContainer);

        setupDurationPresetButtons(view);

        if (btnStartPomodoro != null) {
            btnStartPomodoro.setOnClickListener(v -> handleStartButtonClick());
        }
        if (btnPomodoroHistory != null) {
            btnPomodoroHistory.setOnClickListener(v ->
                    Toast.makeText(requireContext(), getString(R.string.pomodoro_history_coming_soon), Toast.LENGTH_SHORT).show()
            );
        }
        if (btnPomodoroMore != null) {
            btnPomodoroMore.setOnClickListener(v ->
                    Toast.makeText(requireContext(), getString(R.string.pomodoro_options_coming_soon), Toast.LENGTH_SHORT).show()
            );
        }
        if (btnResetPomodoro != null) {
            btnResetPomodoro.setOnClickListener(v -> handleResetButtonClick());
        }
        if (tvTaskSelector != null) {
            tvTaskSelector.setOnClickListener(v -> showIncompleteTaskPicker());
        }
        if (pomodoroClockContainer != null) {
            pomodoroClockContainer.setOnClickListener(v -> showDurationPicker());
        }
        if (tvPomodoroTimer != null) {
            tvPomodoroTimer.setOnClickListener(v -> showDurationPicker());
        }
        if (viewDurationPickerScrim != null) {
            viewDurationPickerScrim.setOnClickListener(v -> hideDurationPicker());
        }
        if (btnCancelDuration != null) {
            btnCancelDuration.setOnClickListener(v -> hideDurationPicker());
        }
        if (btnApplyDuration != null) {
            btnApplyDuration.setOnClickListener(v -> handleApplyDurationClick());
        }
        if (etCustomDurationMinutes != null) {
            etCustomDurationMinutes.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    hideKeyboardFor(v);
                }
            });
        }

        restoreTimerState(savedInstanceState);
        if (savedInstanceState == null) {
            restoreFromSessionStore();
        }
        reconcileRunningStateWithClock();
        renderTimerUI();

        if (isRunning) {
            startTimer(remainingMillis);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_MODE, currentMode.name());
        outState.putLong(STATE_TOTAL_MILLIS, totalMillis);
        outState.putLong(STATE_REMAINING_MILLIS, remainingMillis);
        outState.putBoolean(STATE_IS_RUNNING, isRunning);
        outState.putBoolean(STATE_IS_PAUSED, isPaused);
        outState.putLong(STATE_SELECTED_FOCUS_MILLIS, selectedFocusDurationMillis);
        outState.putString(STATE_SELECTED_TASK_ID, selectedTaskId);
        outState.putString(STATE_SELECTED_TASK_TITLE, selectedTaskTitle);
        syncStateToSessionStore();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelTimer();
        tvTaskSelector = null;
        tvPomodoroTimer = null;
        progressPomodoro = null;
        btnStartPomodoro = null;
        btnResetPomodoro = null;
        viewDurationPickerScrim = null;
        cardDurationPicker = null;
        etCustomDurationMinutes = null;
        btnApplyDuration = null;
        btnCancelDuration = null;
        durationPresetButtons.clear();
    }

    private void handleStartButtonClick() {
        if (isRunning) {
            pauseTimer();
            return;
        }
        if (isPaused) {
            startTimer(remainingMillis);
            return;
        }
        startTimer(totalMillis);
    }

    private void restoreTimerState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        String savedMode = savedInstanceState.getString(STATE_MODE, SessionMode.FOCUS.name());
        try {
            currentMode = SessionMode.valueOf(savedMode);
        } catch (IllegalArgumentException ignored) {
            currentMode = SessionMode.FOCUS;
        }

        long defaultTotal = currentMode == SessionMode.FOCUS
                ? FOCUS_DURATION_MILLIS
                : BREAK_DURATION_MILLIS;
        totalMillis = Math.max(0L, savedInstanceState.getLong(STATE_TOTAL_MILLIS, defaultTotal));
        remainingMillis = Math.max(0L, savedInstanceState.getLong(STATE_REMAINING_MILLIS, totalMillis));
        isRunning = savedInstanceState.getBoolean(STATE_IS_RUNNING, false);
        isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED, false);
        selectedFocusDurationMillis = clampDurationMillis(
                savedInstanceState.getLong(STATE_SELECTED_FOCUS_MILLIS, FOCUS_DURATION_MILLIS)
        );
        selectedTaskId = savedInstanceState.getString(STATE_SELECTED_TASK_ID);
        selectedTaskTitle = savedInstanceState.getString(STATE_SELECTED_TASK_TITLE);

        if (isRunning) {
            isPaused = false;
        }
    }

    private void handleResetButtonClick() {
        cancelTimer();
        isRunning = false;
        isPaused = false;
        runningEndElapsedRealtime = -1L;
        currentMode = SessionMode.FOCUS;
        totalMillis = selectedFocusDurationMillis;
        remainingMillis = selectedFocusDurationMillis;
        renderTimerUI();
    }

    private void startTimer(long startMillis) {
        cancelTimer();

        remainingMillis = startMillis;
        isRunning = true;
        isPaused = false;
        runningEndElapsedRealtime = SystemClock.elapsedRealtime() + startMillis;
        updateStartButtonState();

        countDownTimer = new CountDownTimer(startMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                renderTimerUI();
            }

            @Override
            public void onFinish() {
                remainingMillis = 0L;
                renderTimerUI();
                onSessionCompleted();
            }
        }.start();
    }

    private void pauseTimer() {
        cancelTimer();
        isRunning = false;
        isPaused = true;
        runningEndElapsedRealtime = -1L;
        updateStartButtonState();
    }

    private void onSessionCompleted() {
        isRunning = false;
        isPaused = false;

        if (currentMode == SessionMode.FOCUS) {
            currentMode = SessionMode.BREAK;
            totalMillis = BREAK_DURATION_MILLIS;
            remainingMillis = BREAK_DURATION_MILLIS;
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.pomodoro_focus_done), Toast.LENGTH_SHORT).show();
            }
        } else {
            currentMode = SessionMode.FOCUS;
            totalMillis = selectedFocusDurationMillis;
            remainingMillis = selectedFocusDurationMillis;
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.pomodoro_break_done), Toast.LENGTH_SHORT).show();
            }
        }

        renderTimerUI();
        startTimer(totalMillis);
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void renderTimerUI() {
        if (tvPomodoroTimer != null) {
            tvPomodoroTimer.setText(formatTime(remainingMillis));
        }
        if (tvTaskSelector != null) {
            tvTaskSelector.setText(getTaskSelectorLabel());
        }
        if (progressPomodoro != null) {
            int percent = totalMillis <= 0
                    ? 0
                    : (int) Math.max(0, Math.min(100, (remainingMillis * 100L) / totalMillis));
            progressPomodoro.setProgress(percent);
        }
        updateStartButtonState();
        updateResetButtonVisibility();
        syncStateToSessionStore();
    }

    private void updateStartButtonState() {
        if (btnStartPomodoro == null) {
            return;
        }
        if (isRunning) {
            btnStartPomodoro.setText(R.string.pomodoro_pause);
        } else if (isPaused) {
            btnStartPomodoro.setText(R.string.pomodoro_resume);
        } else {
            btnStartPomodoro.setText(R.string.pomodoro_start);
        }
    }

    private void updateResetButtonVisibility() {
        if (btnResetPomodoro == null) {
            return;
        }

        boolean hasStartedSession = isRunning
                || isPaused
                || currentMode != SessionMode.FOCUS
                || remainingMillis < totalMillis;

        btnResetPomodoro.setVisibility(hasStartedSession ? View.VISIBLE : View.GONE);
    }

    private void setupDurationPresetButtons(@NonNull View root) {
        durationPresetButtons.clear();

        List<Integer> presetIds = Arrays.asList(
                R.id.btnDuration5,
                R.id.btnDuration15,
                R.id.btnDuration25,
                R.id.btnDuration45,
                R.id.btnDuration90
        );

        for (Integer id : presetIds) {
            MaterialButton button = root.findViewById(id);
            if (button == null) {
                continue;
            }
            durationPresetButtons.add(button);
            button.setOnClickListener(v -> {
                Object minutesTag = button.getTag();
                if (minutesTag == null) {
                    return;
                }
                try {
                    selectedPickerMinutes = Integer.parseInt(String.valueOf(minutesTag));
                    if (etCustomDurationMinutes != null) {
                        etCustomDurationMinutes.clearFocus();
                        etCustomDurationMinutes.setText("");
                    }
                    hideKeyboardFor(button);
                    updateDurationPresetSelection();
                } catch (NumberFormatException ignored) {
                    selectedPickerMinutes = null;
                    updateDurationPresetSelection();
                }
            });
        }
    }

    private void showDurationPicker() {
        if (viewDurationPickerScrim == null || cardDurationPicker == null) {
            return;
        }

        selectedPickerMinutes = null;
        if (etCustomDurationMinutes != null) {
            long currentMinutes = Math.max(MIN_DURATION_MINUTES,
                    Math.min(MAX_DURATION_MINUTES, totalMillis / 60_000L));
            etCustomDurationMinutes.setText(String.valueOf(currentMinutes));
            etCustomDurationMinutes.setSelection(etCustomDurationMinutes.getText() != null
                    ? etCustomDurationMinutes.getText().length()
                    : 0);
        }
        updateDurationPresetSelection();

        viewDurationPickerScrim.setVisibility(View.VISIBLE);
        cardDurationPicker.setVisibility(View.VISIBLE);
    }

    private void hideDurationPicker() {
        dismissKeyboardAndClearPickerFocus();
        if (viewDurationPickerScrim != null) {
            viewDurationPickerScrim.setVisibility(View.GONE);
        }
        if (cardDurationPicker != null) {
            cardDurationPicker.setVisibility(View.GONE);
        }
    }

    private void handleApplyDurationClick() {
        Integer pickedMinutes = resolvePickedMinutes();
        if (pickedMinutes == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.pomodoro_duration_pick_first), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (pickedMinutes < MIN_DURATION_MINUTES || pickedMinutes > MAX_DURATION_MINUTES) {
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.pomodoro_duration_invalid), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        long newDurationMillis = pickedMinutes * 60_000L;
        dismissKeyboardAndClearPickerFocus();

        if (isRunning && isAdded()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pomodoro_duration_running_title)
                    .setMessage(R.string.pomodoro_duration_running_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.pomodoro_apply_duration, (dialog, which) -> {
                        pauseTimer();
                        applyDurationMillis(newDurationMillis, true);
                    })
                    .show();
            return;
        }

        applyDurationMillis(newDurationMillis, isPaused);
    }

    private void dismissKeyboardAndClearPickerFocus() {
        if (etCustomDurationMinutes != null) {
            hideKeyboardFor(etCustomDurationMinutes);
            etCustomDurationMinutes.clearFocus();
        }
    }

    private void hideKeyboardFor(@Nullable View anchor) {
        if (!isAdded() || anchor == null) {
            return;
        }
        Context context = requireContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(anchor.getWindowToken(), 0);
        }
    }

    @Nullable
    private Integer resolvePickedMinutes() {
        if (etCustomDurationMinutes != null) {
            CharSequence customValue = etCustomDurationMinutes.getText();
            if (!TextUtils.isEmpty(customValue)) {
                try {
                    return Integer.parseInt(customValue.toString().trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return selectedPickerMinutes;
    }

    private void applyDurationMillis(long newDurationMillis, boolean keepPaused) {
        newDurationMillis = clampDurationMillis(newDurationMillis);
        cancelTimer();
        totalMillis = newDurationMillis;
        remainingMillis = newDurationMillis;
        isRunning = false;
        isPaused = keepPaused;
        runningEndElapsedRealtime = -1L;

        if (currentMode == SessionMode.FOCUS) {
            selectedFocusDurationMillis = newDurationMillis;
        }

        hideDurationPicker();
        renderTimerUI();
    }

    private long clampDurationMillis(long durationMillis) {
        return Math.max(MIN_DURATION_MILLIS, Math.min(MAX_DURATION_MILLIS, durationMillis));
    }

    private void restoreFromSessionStore() {
        currentMode = SessionStore.currentMode;
        totalMillis = SessionStore.totalMillis;
        remainingMillis = SessionStore.remainingMillis;
        selectedFocusDurationMillis = SessionStore.selectedFocusDurationMillis;
        isRunning = SessionStore.isRunning;
        isPaused = SessionStore.isPaused;
        runningEndElapsedRealtime = SessionStore.runningEndElapsedRealtime;
        selectedTaskId = SessionStore.selectedTaskId;
        selectedTaskTitle = SessionStore.selectedTaskTitle;
    }

    private void syncStateToSessionStore() {
        SessionStore.currentMode = currentMode;
        SessionStore.totalMillis = totalMillis;
        SessionStore.remainingMillis = remainingMillis;
        SessionStore.selectedFocusDurationMillis = selectedFocusDurationMillis;
        SessionStore.isRunning = isRunning;
        SessionStore.isPaused = isPaused;
        SessionStore.runningEndElapsedRealtime = runningEndElapsedRealtime;
        SessionStore.selectedTaskId = selectedTaskId;
        SessionStore.selectedTaskTitle = selectedTaskTitle;
    }

    private void reconcileRunningStateWithClock() {
        if (!isRunning || runningEndElapsedRealtime <= 0L) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (now < runningEndElapsedRealtime) {
            remainingMillis = runningEndElapsedRealtime - now;
            return;
        }

        long overflowMillis = now - runningEndElapsedRealtime;
        SessionMode nextMode = getNextMode(currentMode);
        long nextDurationMillis = getDurationForMode(nextMode);

        while (overflowMillis >= nextDurationMillis) {
            overflowMillis -= nextDurationMillis;
            nextMode = getNextMode(nextMode);
            nextDurationMillis = getDurationForMode(nextMode);
        }

        currentMode = nextMode;
        totalMillis = nextDurationMillis;
        remainingMillis = nextDurationMillis - overflowMillis;
        runningEndElapsedRealtime = now + remainingMillis;
        isRunning = true;
        isPaused = false;
    }

    @NonNull
    private SessionMode getNextMode(@NonNull SessionMode mode) {
        return mode == SessionMode.FOCUS ? SessionMode.BREAK : SessionMode.FOCUS;
    }

    private long getDurationForMode(@NonNull SessionMode mode) {
        return mode == SessionMode.FOCUS ? selectedFocusDurationMillis : BREAK_DURATION_MILLIS;
    }

    private static final class SessionStore {
        private static SessionMode currentMode = SessionMode.FOCUS;
        private static long totalMillis = FOCUS_DURATION_MILLIS;
        private static long remainingMillis = FOCUS_DURATION_MILLIS;
        private static long selectedFocusDurationMillis = FOCUS_DURATION_MILLIS;
        private static boolean isRunning;
        private static boolean isPaused;
        private static long runningEndElapsedRealtime = -1L;
        @Nullable
        private static String selectedTaskId;
        @Nullable
        private static String selectedTaskTitle;

        private SessionStore() {
        }
    }

    private void showIncompleteTaskPicker() {
        if (!isAdded()) {
            return;
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            Toast.makeText(requireContext(), getString(R.string.auth_error_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        tasksRef
                .get()
                .addOnSuccessListener(this::showTaskPickerDialog)
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), getString(R.string.pomodoro_tasks_load_failed), Toast.LENGTH_SHORT).show();
                });
    }

    private void showTaskPickerDialog(@NonNull QuerySnapshot snapshot) {
        if (!isAdded()) {
            return;
        }

        List<Task> incompleteTasks = new ArrayList<>();
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
            Task task = doc.toObject(Task.class);
            task.setId(doc.getId());
            if (task.isCompleted()) {
                continue;
            }
            String title = task.getTitle();
            if (title == null || title.trim().isEmpty()) {
                continue;
            }
            incompleteTasks.add(task);
        }

        sortTasksForPicker(incompleteTasks);

        if (incompleteTasks.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.pomodoro_tasks_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedIndex = -1;
        if (selectedTaskId != null) {
            for (int i = 0; i < incompleteTasks.size(); i++) {
                if (selectedTaskId.equals(incompleteTasks.get(i).getId())) {
                    checkedIndex = i;
                    break;
                }
            }
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_pomodoro_task_picker, null, false);
        ListView listView = dialogView.findViewById(R.id.lvPomodoroTasks);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseTaskPicker);

        final int[] selectedIndexHolder = new int[]{checkedIndex};
        ArrayAdapter<Task> adapter = new ArrayAdapter<Task>(
                requireContext(),
                R.layout.item_pomodoro_task_picker,
                incompleteTasks
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = LayoutInflater.from(getContext()).inflate(R.layout.item_pomodoro_task_picker, parent, false);
                }

                TextView tvTitle = row.findViewById(R.id.tvTaskPickerItemTitle);
                TextView tvMeta = row.findViewById(R.id.tvTaskPickerMeta);
                TextView tvCheck = row.findViewById(R.id.tvTaskPickerCheck);

                Task task = getItem(position);
                String title = task != null && task.getTitle() != null
                        ? task.getTitle().trim()
                        : "";

                if (tvTitle != null) {
                    tvTitle.setText(title);
                }
                if (tvMeta != null) {
                    tvMeta.setText(buildTaskMeta(task));
                }

                boolean isSelected = position == selectedIndexHolder[0];
                row.setActivated(isSelected);
                if (tvCheck != null) {
                    tvCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                }

                return row;
            }
        };
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if (checkedIndex >= 0) {
            listView.setItemChecked(checkedIndex, true);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedIndexHolder[0] = position;
            adapter.notifyDataSetChanged();
            Task selectedTask = incompleteTasks.get(position);
            selectedTaskId = selectedTask.getId();
            selectedTaskTitle = selectedTask.getTitle() == null
                    ? null
                    : selectedTask.getTitle().trim();
            renderTimerUI();
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    @NonNull
    private String getTaskSelectorLabel() {
        if (!TextUtils.isEmpty(selectedTaskTitle)) {
            return selectedTaskTitle;
        }
        return getString(R.string.pomodoro_mode_focus);
    }

    private void sortTasksForPicker(@NonNull List<Task> tasks) {
        if (tasks.size() <= 1) {
            return;
        }

        final long todayStartMillis = getTodayStartMillis();
        Collections.sort(tasks, (left, right) -> {
            // 1) Higher priority first.
            int byPriority = Integer.compare(right.getPriority(), left.getPriority());
            if (byPriority != 0) {
                return byPriority;
            }

            // 2) Closer due date to today first (absolute distance).
            long leftDistance = getDistanceToToday(todayStartMillis, left.getDueDate());
            long rightDistance = getDistanceToToday(todayStartMillis, right.getDueDate());
            int byDistance = Long.compare(leftDistance, rightDistance);
            if (byDistance != 0) {
                return byDistance;
            }

            // 3) Tasks with due date first, then earlier due date.
            Long leftDue = left.getDueDate();
            Long rightDue = right.getDueDate();
            if (leftDue == null && rightDue != null) {
                return 1;
            }
            if (leftDue != null && rightDue == null) {
                return -1;
            }
            if (leftDue != null && rightDue != null) {
                int byDueDate = Long.compare(leftDue, rightDue);
                if (byDueDate != 0) {
                    return byDueDate;
                }
            }

            // 4) Stable visual fallback by title.
            String leftTitle = left.getTitle() == null ? "" : left.getTitle().trim();
            String rightTitle = right.getTitle() == null ? "" : right.getTitle().trim();
            return leftTitle.compareToIgnoreCase(rightTitle);
        });
    }

    private long getTodayStartMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getDistanceToToday(long todayStartMillis, @Nullable Long dueDateMillis) {
        if (dueDateMillis == null) {
            return Long.MAX_VALUE;
        }
        long distance = dueDateMillis - todayStartMillis;
        if (distance == Long.MIN_VALUE) {
            return Long.MAX_VALUE - 1;
        }
        return Math.abs(distance);
    }

    @NonNull
    private String buildTaskMeta(@Nullable Task task) {
        if (task == null) {
            return getString(R.string.pomodoro_task_no_due_date);
        }

        String priority = getPriorityLabel(task.getPriority());
        String dueDate = getDueDateLabel(task.getDueDate());
        return getString(R.string.pomodoro_task_meta_format, priority, dueDate);
    }

    @NonNull
    private String getPriorityLabel(int priority) {
        switch (priority) {
            case 3:
                return getString(R.string.priority_high);
            case 2:
                return getString(R.string.priority_medium);
            case 1:
                return getString(R.string.priority_low);
            default:
                return getString(R.string.priority_none);
        }
    }

    @NonNull
    private String getDueDateLabel(@Nullable Long dueDateMillis) {
        if (dueDateMillis == null || dueDateMillis <= 0L) {
            return getString(R.string.pomodoro_task_no_due_date);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dueDateMillis);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        return String.format(Locale.getDefault(), "%02d/%02d", day, month);
    }

    private void updateDurationPresetSelection() {
        for (MaterialButton button : durationPresetButtons) {
            if (button == null) {
                continue;
            }
            boolean isSelected = false;
            Object minutesTag = button.getTag();
            if (selectedPickerMinutes != null && minutesTag != null) {
                try {
                    int buttonMinutes = Integer.parseInt(String.valueOf(minutesTag));
                    isSelected = selectedPickerMinutes == buttonMinutes;
                } catch (NumberFormatException ignored) {
                    // Keep default false when button tag is malformed.
                }
            }
            button.setStrokeWidth(isSelected ? 3 : 1);
            button.setAlpha(isSelected ? 1f : 0.75f);
        }
    }

    @NonNull
    private String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
