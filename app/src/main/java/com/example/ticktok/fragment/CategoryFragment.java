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
import androidx.recyclerview.widget.ItemTouchHelper;

import com.example.ticktok.R;
import com.example.ticktok.adapter.TaskAdapter;
import com.example.ticktok.model.Task;
import com.example.ticktok.util.UserFirestorePaths;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_CATEGORY_ID = "arg_category_id";

    private String categoryId;
    private TaskAdapter taskAdapter;
    private TextView tvEmptyTasks;
    private ListenerRegistration taskListener;

    private ItemTouchHelper taskItemTouchHelper;

    private static final String TAG_EDIT_TASK_SHEET = "edit_task_sheet";

    public static CategoryFragment newInstance(String title, @Nullable String categoryId) {
        CategoryFragment fragment = new CategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_CATEGORY_ID, categoryId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            categoryId = args.getString(ARG_CATEGORY_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvTasks = view.findViewById(R.id.rvCategoryTasks);
        tvEmptyTasks = view.findViewById(R.id.tvEmptyTasks);

        taskAdapter = new TaskAdapter(
                this::onTaskMoreClicked,
                viewHolder -> {
                    if (taskItemTouchHelper != null) {
                        taskItemTouchHelper.startDrag(viewHolder);
                    }
                },
                this::onTaskCheckedChanged
        );
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                if (taskAdapter == null) {
                    return false;
                }
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                taskAdapter.moveItem(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // no-op
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                persistTaskOrder();
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // Drag is started via adapter long-press on the body.
                return false;
            }
        };
        taskItemTouchHelper = new ItemTouchHelper(callback);
        taskItemTouchHelper.attachToRecyclerView(rvTasks);
    }

    private void onTaskMoreClicked(@NonNull View anchorView, @NonNull Task task) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_task_actions, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> handleTaskAction(item, task));
        popupMenu.show();
    }

    private void onTaskCheckedChanged(@NonNull Task task, boolean isChecked) {
        if (!isAdded()) {
            return;
        }

        if (task.getId() == null || task.getId().trim().isEmpty()) {
            return;
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

    private void persistTaskOrder() {
        if (!isAdded() || taskAdapter == null) {
            return;
        }
        List<Task> items = taskAdapter.getCurrentItems();
        if (items.isEmpty()) {
            return;
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            return;
        }

        List<Task> valid = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Task task = items.get(i);
            if (task == null || task.getId() == null || task.getId().trim().isEmpty()) {
                continue;
            }
            task.setOrder(i + 1);
            valid.add(task);
        }

        persistTaskOrderBatched(tasksRef, valid, 0);
    }

    private void persistTaskOrderBatched(@NonNull CollectionReference tasksRef,
                                        @NonNull List<Task> tasks,
                                        int startIndex) {
        if (startIndex >= tasks.size()) {
            return;
        }
        int endIndexExclusive = Math.min(startIndex + 450, tasks.size());
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (int i = startIndex; i < endIndexExclusive; i++) {
            Task t = tasks.get(i);
            batch.update(tasksRef.document(t.getId().trim()), "order", t.getOrder());
        }

        batch.commit()
                .addOnSuccessListener(unused -> persistTaskOrderBatched(tasksRef, tasks, endIndexExclusive))
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.error_save_task_order, Toast.LENGTH_SHORT).show();
                    }
                });
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
        if (categoryId == null || categoryId.trim().isEmpty()) {
            taskAdapter.submitList(new ArrayList<>());
            showEmptyState(true);
            return;
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            taskAdapter.submitList(new ArrayList<>());
            showEmptyState(true);
            return;
        }

        stopTaskListener();
        taskListener = tasksRef
                .whereEqualTo("categoryId", categoryId)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded() || taskAdapter == null) {
                        return;
                    }
                    if (error != null || snapshot == null) {
                        showEmptyState(true);
                        return;
                    }

                    List<Task> tasks = mapSnapshotToTasks(snapshot);
                    Collections.sort(tasks, Comparator.comparingInt(Task::getOrder));
                    taskAdapter.submitList(tasks);
                    showEmptyState(tasks.isEmpty());
                });
    }

    private void stopTaskListener() {
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
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

    private void showEmptyState(boolean show) {
        if (tvEmptyTasks != null) {
            tvEmptyTasks.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}

