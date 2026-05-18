package com.example.ticktok.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.activity.MainActivity;
import com.example.ticktok.adapter.TaskAdapter;
import com.example.ticktok.model.Task;
import com.example.ticktok.reminder.ReminderManager;
import com.example.ticktok.util.UserFirestorePaths;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class TaskFilterFragment extends Fragment {

    public enum FilterType {
        TODAY,
        TOMORROW,
        OVERDUE,
        COMPLETED_TODAY
    }

    private static final String ARG_FILTER_TYPE = "arg_filter_type";
    private static final String ARG_SCREEN_TITLE = "arg_screen_title";

    private static final String TAG_EDIT_TASK_SHEET = "edit_task_sheet";

    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    private FilterType filterType = FilterType.TODAY;
    @Nullable
    private String screenTitle;

    @Nullable
    private TaskAdapter taskAdapter;
    @Nullable
    private TextView tvEmptyTasks;

    @Nullable
    private ListenerRegistration taskListener;

    @NonNull
    public static TaskFilterFragment newInstance(@NonNull FilterType type, @NonNull String title) {
        TaskFilterFragment fragment = new TaskFilterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILTER_TYPE, type.name());
        args.putString(ARG_SCREEN_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String raw = args.getString(ARG_FILTER_TYPE);
            if (raw != null) {
                try {
                    filterType = FilterType.valueOf(raw);
                } catch (IllegalArgumentException ignored) {
                }
            }
            screenTitle = args.getString(ARG_SCREEN_TITLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Reuse the category list layout (RecyclerView + empty state).
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvTasks = view.findViewById(R.id.rvCategoryTasks);
        tvEmptyTasks = view.findViewById(R.id.tvEmptyTasks);

        taskAdapter = new TaskAdapter(
                this::onTaskMoreClicked,
                null,
                this::onTaskCheckedChanged
        );
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            String title = screenTitle != null && !screenTitle.trim().isEmpty()
                    ? screenTitle.trim()
                    : getString(R.string.menu_welcome);
            ((MainActivity) getActivity()).setScreenTitle(title);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startTaskListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopTaskListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTaskListener();
        taskAdapter = null;
        tvEmptyTasks = null;
    }

    private void startTaskListener() {
        if (!isAdded() || taskAdapter == null) {
            return;
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            taskAdapter.submitList(new ArrayList<>());
            showEmptyState(true);
            return;
        }

        stopTaskListener();
        taskListener = tasksRef.addSnapshotListener((snapshot, error) -> {
            if (!isAdded() || taskAdapter == null) {
                return;
            }
            if (error != null || snapshot == null) {
                taskAdapter.submitList(new ArrayList<>());
                showEmptyState(true);
                return;
            }

            List<Task> allTasks = mapSnapshotToTasks(snapshot);
            List<Task> filtered = filterTasks(allTasks);
            sortFiltered(filtered);

            taskAdapter.submitList(filtered);
            showEmptyState(filtered.isEmpty());
        });
    }

    private void stopTaskListener() {
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }
    }

    @NonNull
    private List<Task> filterTasks(@NonNull List<Task> tasks) {
        long todayStart = normalizeToStartOfDay(System.currentTimeMillis());
        long todayEnd = todayStart + MILLIS_PER_DAY;
        long tomorrowStart = todayStart + MILLIS_PER_DAY;

        List<Task> filtered = new ArrayList<>();
        for (Task task : tasks) {
            boolean completed = task.isCompleted();
            Long dueDate = task.getDueDate();

            switch (filterType) {
                case TODAY:
                    if (!completed && dueDate != null && dueDate == todayStart) {
                        filtered.add(task);
                    }
                    break;
                case TOMORROW:
                    if (!completed && dueDate != null && dueDate == tomorrowStart) {
                        filtered.add(task);
                    }
                    break;
                case OVERDUE:
                    if (!completed && dueDate != null && dueDate > 0 && dueDate < todayStart) {
                        filtered.add(task);
                    }
                    break;
                case COMPLETED_TODAY:
                    if (completed) {
                        Date completedAt = task.getCompletedAt();
                        if (completedAt != null) {
                            long ts = completedAt.getTime();
                            if (ts >= todayStart && ts < todayEnd) {
                                filtered.add(task);
                            }
                        }
                    }
                    break;
            }
        }
        return filtered;
    }

    private void sortFiltered(@NonNull List<Task> tasks) {
        if (tasks.size() <= 1) {
            return;
        }
        if (filterType == FilterType.COMPLETED_TODAY) {
            Collections.sort(tasks, (a, b) -> {
                Date da = a.getCompletedAt();
                Date db = b.getCompletedAt();
                long ta = da != null ? da.getTime() : 0L;
                long tb = db != null ? db.getTime() : 0L;
                return Long.compare(tb, ta);
            });
            return;
        }

        if (filterType == FilterType.OVERDUE) {
            Collections.sort(tasks, (a, b) -> {
                long da = a.getDueDate() != null ? a.getDueDate() : Long.MAX_VALUE;
                long db = b.getDueDate() != null ? b.getDueDate() : Long.MAX_VALUE;
                return Long.compare(da, db);
            });
            return;
        }

        Collections.sort(tasks, Comparator.comparingInt(Task::getOrder));
    }

    @NonNull
    private List<Task> mapSnapshotToTasks(@NonNull QuerySnapshot snapshot) {
        List<Task> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            Task task = doc.toObject(Task.class);
            task.setId(doc.getId());
            tasks.add(task);
        }
        return tasks;
    }

    private void onTaskMoreClicked(@NonNull View anchorView, @NonNull Task task) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_task_actions, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> handleTaskAction(item, task));
        popupMenu.show();
    }

    private boolean handleTaskAction(@NonNull MenuItem item, @NonNull Task task) {
        int id = item.getItemId();
        if (id == R.id.action_task_edit) {
            openEditTaskSheet(task);
            return true;
        }
        if (id == R.id.action_task_delete) {
            confirmDeleteTask(task);
            return true;
        }
        return false;
    }

    private void openEditTaskSheet(@NonNull Task task) {
        if (!isAdded()) {
            return;
        }
        if (task.getId() == null || task.getId().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.delete_task_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (requireActivity().getSupportFragmentManager().findFragmentByTag(TAG_EDIT_TASK_SHEET) != null) {
            return;
        }
        AddTaskBottomSheetFragment sheet = AddTaskBottomSheetFragment.newInstanceForEdit(task);
        sheet.show(requireActivity().getSupportFragmentManager(), TAG_EDIT_TASK_SHEET);
    }

    private void confirmDeleteTask(@NonNull Task task) {
        String title = task.getTitle() == null ? "" : task.getTitle();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_task_title)
                .setMessage(getString(R.string.delete_task_message, title))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteTask(task))
                .show();
    }

    private void deleteTask(@NonNull Task task) {
        if (task.getId() == null || task.getId().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.delete_task_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            Toast.makeText(requireContext(), R.string.auth_error_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        tasksRef.document(task.getId().trim())
                .delete()
                .addOnSuccessListener(unused -> {
                    ReminderManager.cancelReminder(requireContext(), task.getId().trim());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.delete_task_success, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.delete_task_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onTaskCheckedChanged(@NonNull Task task, boolean isChecked) {
        if (task.getId() == null || task.getId().trim().isEmpty()) {
            return;
        }

        if (isChecked) {
            ReminderManager.cancelReminder(requireContext(), task.getId().trim());
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            return;
        }

        tasksRef.document(task.getId().trim())
                .update(
                        // Write both keys to be compatible with Firestore POJO mapping.
                        "isCompleted", isChecked,
                        "completed", isChecked,
                        "completedAt", isChecked ? FieldValue.serverTimestamp() : null
                );
    }

    private void showEmptyState(boolean show) {
        if (tvEmptyTasks != null) {
            tvEmptyTasks.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private long normalizeToStartOfDay(long millis) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}



