package com.example.ticktok.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.adapter.TaskAdapter;
import com.example.ticktok.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_CATEGORY_ID = "arg_category_id";

    private String categoryId;
    private TaskAdapter taskAdapter;
    private TextView tvEmptyTasks;
    private ListenerRegistration taskListener;

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

        taskAdapter = new TaskAdapter();
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);
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

        stopTaskListener();
        taskListener = FirebaseFirestore.getInstance()
                .collection("tasks")
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

