package com.example.ticktok.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private static final String TAG_EDIT_TASK_SHEET = "tag_edit_task_sheet";

    private EditText etSearch;
    private ImageButton btnClearSearch;

    private RecyclerView rvResults;
    private TextView tvEmpty;
    private TaskAdapter taskAdapter;

    private final List<Task> allTasks = new ArrayList<>();
    private ListenerRegistration tasksListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etSearch = view.findViewById(R.id.etSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        rvResults = view.findViewById(R.id.rvSearchResults);
        tvEmpty = view.findViewById(R.id.tvEmptySearch);

        view.setOnClickListener(v -> hideKeyboardAndClearFocus());

        if (etSearch == null || btnClearSearch == null || rvResults == null || tvEmpty == null) {
            return;
        }

        taskAdapter = new TaskAdapter(
                this::onTaskMoreClicked,
                null,
                this::onTaskCheckedChanged,
                this::onTaskClicked
        );
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(taskAdapter);

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s != null ? s.toString() : "");
            }
        });

        // Initial state.
        applyFilter("");
    }

    @Override
    public void onStart() {
        super.onStart();
        startTaskListener();
    }

    @Override
    public void onStop() {
        stopTaskListener();
        super.onStop();
    }

    private void hideKeyboardAndClearFocus() {
        if (etSearch == null) {
            return;
        }
        etSearch.clearFocus();

        Context context = getContext();
        if (context == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
    }

    @Override
    public void onPause() {
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        super.onPause();
    }

    private void startTaskListener() {
        stopTaskListener();

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            return;
        }

        tasksListener = tasksRef.addSnapshotListener((snapshot, error) -> {
            if (!isAdded()) {
                return;
            }
            if (error != null || snapshot == null) {
                // Keep UI quiet; Search should not be noisy.
                return;
            }

            allTasks.clear();
            allTasks.addAll(mapSnapshotToTasks(snapshot));

            // Keep a stable ordering for a nicer UX.
            allTasks.sort(Comparator
                    .comparing(Task::isCompleted)
                    .thenComparingLong((Task t) -> t.getDueDate() != null ? t.getDueDate() : Long.MAX_VALUE)
                    .thenComparingInt(Task::getOrder));

            String query = etSearch != null && etSearch.getText() != null
                    ? etSearch.getText().toString()
                    : "";
            applyFilter(query);
        });
    }

    private void stopTaskListener() {
        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
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

    private void applyFilter(@NonNull String rawQuery) {
        if (taskAdapter == null || tvEmpty == null || rvResults == null) {
            return;
        }

        String query = normalizeForSearch(rawQuery);
        if (TextUtils.isEmpty(query)) {
            taskAdapter.submitList(Collections.emptyList());
            rvResults.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.search_empty_hint);
            return;
        }

        List<Task> filtered = new ArrayList<>();
        for (Task t : allTasks) {
            if (t == null) {
                continue;
            }
            String haystack = normalizeForSearch(
                    (t.getTitle() == null ? "" : t.getTitle())
                            + " "
                            + (t.getDescription() == null ? "" : t.getDescription())
            );
            if (haystack.contains(query)) {
                filtered.add(t);
            }
        }

        taskAdapter.submitList(filtered);
        boolean isEmpty = filtered.isEmpty();
        rvResults.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            tvEmpty.setText(R.string.search_no_results);
        }
    }

    @NonNull
    private String normalizeForSearch(@Nullable String s) {
        if (s == null) {
            return "";
        }
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private void onTaskMoreClicked(@NonNull View anchorView, @NonNull Task task) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchorView, Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.menu_task_actions, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> handleTaskAction(item.getItemId(), task));
        popupMenu.show();
    }

    private boolean handleTaskAction(int itemId, @NonNull Task task) {
        if (itemId == R.id.action_task_edit) {
            openEditTaskSheet(task);
            return true;
        }
        if (itemId == R.id.action_task_delete) {
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
        if (!isAdded()) {
            return;
        }
        if (task.getId() == null || task.getId().trim().isEmpty()) {
            return;
        }

        if (isChecked) {
            ReminderManager.cancelReminder(requireContext(), task.getId().trim());
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            Toast.makeText(requireContext(), R.string.auth_error_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        tasksRef.document(task.getId().trim())
                .update(
                        // Write both keys to be compatible with Firestore POJO mapping.
                        "isCompleted", isChecked,
                        "completed", isChecked,
                        "completedAt", isChecked ? FieldValue.serverTimestamp() : null
                )
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.delete_task_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onTaskClicked(@NonNull Task task) {
        if (!isAdded()) {
            return;
        }
        String categoryId = task.getCategoryId();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return;
        }

        String taskId = task.getId();

        hideKeyboardAndClearFocus();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openCategoryById(categoryId.trim(), taskId);
        }
    }
}


