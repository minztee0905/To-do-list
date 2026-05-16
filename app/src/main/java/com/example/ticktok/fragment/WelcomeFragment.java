package com.example.ticktok.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ticktok.R;
import com.example.ticktok.activity.MainActivity;
import com.example.ticktok.model.Task;
import com.example.ticktok.util.UserFirestorePaths;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WelcomeFragment extends Fragment {

    private TextView tvCard1Count;
    private TextView tvCard2Count;
    private TextView tvCard3Count;
    private TextView tvCard4Count;

    private View card1;
    private View card2;
    private View card3;
    private View card4;

    private ListenerRegistration taskListener;

    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        card1 = view.findViewById(R.id.card1);
        card2 = view.findViewById(R.id.card2);
        card3 = view.findViewById(R.id.card3);
        card4 = view.findViewById(R.id.card4);

        tvCard1Count = view.findViewById(R.id.tvCard1Count);
        tvCard2Count = view.findViewById(R.id.tvCard2Count);
        tvCard3Count = view.findViewById(R.id.tvCard3Count);
        tvCard4Count = view.findViewById(R.id.tvCard4Count);

        bindCardClicks();
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
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setScreenTitle(getString(R.string.menu_welcome));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTaskListener();
        tvCard1Count = null;
        tvCard2Count = null;
        tvCard3Count = null;
        tvCard4Count = null;
        card1 = null;
        card2 = null;
        card3 = null;
        card4 = null;
    }

    private void bindCardClicks() {
        if (card1 != null) {
            card1.setOnClickListener(v -> openFilter(TaskFilterFragment.FilterType.TODAY));
        }
        if (card2 != null) {
            card2.setOnClickListener(v -> openFilter(TaskFilterFragment.FilterType.TOMORROW));
        }
        if (card3 != null) {
            card3.setOnClickListener(v -> openFilter(TaskFilterFragment.FilterType.OVERDUE));
        }
        if (card4 != null) {
            card4.setOnClickListener(v -> openFilter(TaskFilterFragment.FilterType.COMPLETED_TODAY));
        }
    }

    private void openFilter(@NonNull TaskFilterFragment.FilterType type) {
        if (!isAdded()) {
            return;
        }

        String title;
        switch (type) {
            case TODAY:
                title = getString(R.string.card_title_1);
                break;
            case TOMORROW:
                title = getString(R.string.card_title_2);
                break;
            case OVERDUE:
                title = getString(R.string.card_title_3);
                break;
            case COMPLETED_TODAY:
            default:
                title = getString(R.string.card_title_4);
                break;
        }

        TaskFilterFragment fragment = TaskFilterFragment.newInstance(type, title);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragmentWithTitle(fragment, title);
            return;
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void startTaskListener() {
        if (!isAdded()) {
            return;
        }

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            updateCounts(0, 0, 0, 0);
            return;
        }

        stopTaskListener();
        taskListener = tasksRef.addSnapshotListener((snapshot, error) -> {
            if (!isAdded()) {
                return;
            }
            if (error != null || snapshot == null) {
                updateCounts(0, 0, 0, 0);
                return;
            }

            List<Task> tasks = mapSnapshotToTasks(snapshot);
            computeAndUpdateCounts(tasks);
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

    private void computeAndUpdateCounts(@NonNull List<Task> tasks) {
        long todayStart = normalizeToStartOfDay(System.currentTimeMillis());
        long todayEnd = todayStart + MILLIS_PER_DAY;
        long tomorrowStart = todayStart + MILLIS_PER_DAY;

        int todayCount = 0;
        int tomorrowCount = 0;
        int overdueCount = 0;
        int completedTodayCount = 0;

        for (Task task : tasks) {
            boolean completed = task.isCompleted();

            Long dueDate = task.getDueDate();
            if (!completed && dueDate != null && dueDate > 0) {
                if (dueDate == todayStart) {
                    todayCount++;
                } else if (dueDate == tomorrowStart) {
                    tomorrowCount++;
                } else if (dueDate < todayStart) {
                    overdueCount++;
                }
            }

            if (completed) {
                Date completedAt = task.getCompletedAt();
                if (completedAt != null) {
                    long ts = completedAt.getTime();
                    if (ts >= todayStart && ts < todayEnd) {
                        completedTodayCount++;
                    }
                }
            }
        }

        updateCounts(todayCount, tomorrowCount, overdueCount, completedTodayCount);
    }

    private void updateCounts(int today, int upcoming, int important, int completedToday) {
        if (tvCard1Count != null) {
            tvCard1Count.setText(String.valueOf(today));
        }
        if (tvCard2Count != null) {
            tvCard2Count.setText(String.valueOf(upcoming));
        }
        if (tvCard3Count != null) {
            tvCard3Count.setText(String.valueOf(important));
        }
        if (tvCard4Count != null) {
            tvCard4Count.setText(String.valueOf(completedToday));
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
