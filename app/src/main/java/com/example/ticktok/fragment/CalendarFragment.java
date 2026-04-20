package com.example.ticktok.fragment;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.adapter.CalendarAdapter;
import com.example.ticktok.adapter.EisenhowerTaskAdapter;
import com.example.ticktok.adapter.TaskAdapter;
import com.example.ticktok.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    private RecyclerView rvCalendar;
    private CalendarAdapter calendarAdapter;
    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private View layoutEmptyState;
    private TextView tvTabCalendar;
    private TextView tvTabEisenhower;
    private View layoutCalendarContent;
    private View layoutEisenhowerContent;

    private RecyclerView rvQuadrant1;
    private RecyclerView rvQuadrant2;
    private RecyclerView rvQuadrant3;
    private RecyclerView rvQuadrant4;
    private TextView tvEmptyQuadrant1;
    private TextView tvEmptyQuadrant2;
    private TextView tvEmptyQuadrant3;
    private TextView tvEmptyQuadrant4;
    private EisenhowerTaskAdapter quadrantAdapter1;
    private EisenhowerTaskAdapter quadrantAdapter2;
    private EisenhowerTaskAdapter quadrantAdapter3;
    private EisenhowerTaskAdapter quadrantAdapter4;

    private final Calendar displayedMonth = Calendar.getInstance();
    private int leadingEmptyDays;
    private long selectedDateMillis = 0L;
    private ListenerRegistration taskListener;
    private ListenerRegistration monthTaskMarkerListener;
    private ListenerRegistration eisenhowerListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTabCalendar = view.findViewById(R.id.tvTabCalendar);
        tvTabEisenhower = view.findViewById(R.id.tvTabEisenhower);
        layoutCalendarContent = view.findViewById(R.id.layoutCalendarContent);
        layoutEisenhowerContent = view.findViewById(R.id.layoutEisenhowerContent);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        setupTabToggle();
        setupTaskList(view);
        setupCalendar(view);
        setupEisenhowerViews(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        startTaskListener();
        startMonthTaskMarkerListener();
        startEisenhowerListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopTaskListener();
        stopMonthTaskMarkerListener();
        stopEisenhowerListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTaskListener();
        stopMonthTaskMarkerListener();
        stopEisenhowerListener();
        rvCalendar = null;
        rvTasks = null;
        calendarAdapter = null;
        taskAdapter = null;
        layoutEmptyState = null;
        tvTabCalendar = null;
        tvTabEisenhower = null;
        layoutCalendarContent = null;
        layoutEisenhowerContent = null;

        rvQuadrant1 = null;
        rvQuadrant2 = null;
        rvQuadrant3 = null;
        rvQuadrant4 = null;
        tvEmptyQuadrant1 = null;
        tvEmptyQuadrant2 = null;
        tvEmptyQuadrant3 = null;
        tvEmptyQuadrant4 = null;
        quadrantAdapter1 = null;
        quadrantAdapter2 = null;
        quadrantAdapter3 = null;
        quadrantAdapter4 = null;
    }

    public Long getSelectedDateMillisForTask() {
        return selectedDateMillis > 0 ? selectedDateMillis : null;
    }

    private void setupTaskList(@NonNull View rootView) {
        rvTasks = rootView.findViewById(R.id.rvTasks);
        if (rvTasks == null) {
            return;
        }
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (rvTasks.getItemDecorationCount() == 0) {
            rvTasks.addItemDecoration(new VerticalSpaceItemDecoration(dpToPx(2)));
        }
        taskAdapter = new TaskAdapter();
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupCalendar(@NonNull View rootView) {
        rvCalendar = rootView.findViewById(R.id.rvCalendar);
        if (rvCalendar == null) {
            return;
        }

        if (rvCalendar.getLayoutManager() == null) {
            rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        }

        List<String> monthDays = daysInMonthList();
        calendarAdapter = new CalendarAdapter(monthDays, this::onCalendarDaySelected);
        rvCalendar.setAdapter(calendarAdapter);

        int todayPosition = findTodayPosition(monthDays);
        if (todayPosition != RecyclerView.NO_POSITION) {
            calendarAdapter.setSelectedPosition(todayPosition);
            onCalendarDaySelected(todayPosition, monthDays.get(todayPosition));
        } else {
            showEmptyState(true);
        }

        startMonthTaskMarkerListener();
    }

    private void setupEisenhowerViews(@NonNull View rootView) {
        rvQuadrant1 = rootView.findViewById(R.id.rvQuadrant1);
        rvQuadrant2 = rootView.findViewById(R.id.rvQuadrant2);
        rvQuadrant3 = rootView.findViewById(R.id.rvQuadrant3);
        rvQuadrant4 = rootView.findViewById(R.id.rvQuadrant4);

        tvEmptyQuadrant1 = rootView.findViewById(R.id.tvEmptyQuadrant1);
        tvEmptyQuadrant2 = rootView.findViewById(R.id.tvEmptyQuadrant2);
        tvEmptyQuadrant3 = rootView.findViewById(R.id.tvEmptyQuadrant3);
        tvEmptyQuadrant4 = rootView.findViewById(R.id.tvEmptyQuadrant4);

        quadrantAdapter1 = new EisenhowerTaskAdapter();
        quadrantAdapter2 = new EisenhowerTaskAdapter();
        quadrantAdapter3 = new EisenhowerTaskAdapter();
        quadrantAdapter4 = new EisenhowerTaskAdapter();

        bindQuadrantRecycler(rvQuadrant1, quadrantAdapter1);
        bindQuadrantRecycler(rvQuadrant2, quadrantAdapter2);
        bindQuadrantRecycler(rvQuadrant3, quadrantAdapter3);
        bindQuadrantRecycler(rvQuadrant4, quadrantAdapter4);

        showQuadrantEmpty(tvEmptyQuadrant1, true);
        showQuadrantEmpty(tvEmptyQuadrant2, true);
        showQuadrantEmpty(tvEmptyQuadrant3, true);
        showQuadrantEmpty(tvEmptyQuadrant4, true);
    }

    private void bindQuadrantRecycler(@Nullable RecyclerView recyclerView, @Nullable EisenhowerTaskAdapter adapter) {
        if (recyclerView == null || adapter == null) {
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void onCalendarDaySelected(int position, @NonNull String dayValue) {
        if (dayValue.trim().isEmpty()) {
            return;
        }

        int dayOfMonth;
        try {
            dayOfMonth = Integer.parseInt(dayValue);
        } catch (NumberFormatException ignored) {
            return;
        }

        Calendar selectedDay = (Calendar) displayedMonth.clone();
        selectedDay.set(Calendar.DAY_OF_MONTH, 1);
        selectedDay.add(Calendar.DAY_OF_MONTH, position - leadingEmptyDays);
        selectedDay.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
        selectedDay.set(Calendar.MINUTE, 0);
        selectedDay.set(Calendar.SECOND, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);

        selectedDateMillis = selectedDay.getTimeInMillis();
        startTaskListener();
    }

    private void startTaskListener() {
        if (!isAdded() || taskAdapter == null) {
            return;
        }
        if (selectedDateMillis <= 0) {
            taskAdapter.submitList(new ArrayList<>());
            showEmptyState(true);
            return;
        }

        stopTaskListener();
        long dayStart = selectedDateMillis;
        long dayEnd = dayStart + MILLIS_PER_DAY;

        taskListener = FirebaseFirestore.getInstance()
                .collection("tasks")
                .whereGreaterThanOrEqualTo("dueDate", dayStart)
                .whereLessThan("dueDate", dayEnd)
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

    private void startMonthTaskMarkerListener() {
        if (!isAdded() || calendarAdapter == null) {
            return;
        }

        stopMonthTaskMarkerListener();

        Calendar monthStartCalendar = (Calendar) displayedMonth.clone();
        monthStartCalendar.set(Calendar.DAY_OF_MONTH, 1);
        monthStartCalendar.set(Calendar.HOUR_OF_DAY, 0);
        monthStartCalendar.set(Calendar.MINUTE, 0);
        monthStartCalendar.set(Calendar.SECOND, 0);
        monthStartCalendar.set(Calendar.MILLISECOND, 0);

        long monthStart = monthStartCalendar.getTimeInMillis();

        Calendar nextMonthStartCalendar = (Calendar) monthStartCalendar.clone();
        nextMonthStartCalendar.add(Calendar.MONTH, 1);
        long nextMonthStart = nextMonthStartCalendar.getTimeInMillis();

        monthTaskMarkerListener = FirebaseFirestore.getInstance()
                .collection("tasks")
                .whereGreaterThanOrEqualTo("dueDate", monthStart)
                .whereLessThan("dueDate", nextMonthStart)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded() || calendarAdapter == null) {
                        return;
                    }
                    if (error != null || snapshot == null) {
                        calendarAdapter.setDayPositionsWithTasks(new HashSet<>());
                        return;
                    }

                    Set<Integer> markedPositions = mapSnapshotToMarkedDayPositions(snapshot);
                    calendarAdapter.setDayPositionsWithTasks(markedPositions);
                });
    }

    private void stopMonthTaskMarkerListener() {
        if (monthTaskMarkerListener != null) {
            monthTaskMarkerListener.remove();
            monthTaskMarkerListener = null;
        }
    }

    private void startEisenhowerListener() {
        if (!isAdded() || quadrantAdapter1 == null || quadrantAdapter2 == null
                || quadrantAdapter3 == null || quadrantAdapter4 == null) {
            return;
        }

        stopEisenhowerListener();
        eisenhowerListener = FirebaseFirestore.getInstance()
                .collection("tasks")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded() || quadrantAdapter1 == null || quadrantAdapter2 == null
                            || quadrantAdapter3 == null || quadrantAdapter4 == null) {
                        return;
                    }
                    if (error != null || snapshot == null) {
                        submitQuadrantLists(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                        return;
                    }

                    List<Task> allTasks = mapSnapshotToTasks(snapshot);
                    partitionAndRenderEisenhower(allTasks);
                });
    }

    private void stopEisenhowerListener() {
        if (eisenhowerListener != null) {
            eisenhowerListener.remove();
            eisenhowerListener = null;
        }
    }

    private void partitionAndRenderEisenhower(@NonNull List<Task> tasks) {
        List<Task> q1 = new ArrayList<>();
        List<Task> q2 = new ArrayList<>();
        List<Task> q3 = new ArrayList<>();
        List<Task> q4 = new ArrayList<>();

        for (Task task : tasks) {
            if (task == null || task.isCompleted()) {
                continue;
            }

            switch (task.getPriority()) {
                case 3:
                    // Priority 3 -> Quadrant I
                    q1.add(task);
                    break;
                case 2:
                    // Priority 2 -> Quadrant II
                    q2.add(task);
                    break;
                case 1:
                    // Priority 1 -> Quadrant III
                    q3.add(task);
                    break;
                case 0:
                default:
                    // Priority 0 (or invalid) -> Quadrant IV
                    q4.add(task);
                    break;
            }
        }

        submitQuadrantLists(q1, q2, q3, q4);
    }

    private void submitQuadrantLists(@NonNull List<Task> q1,
                                     @NonNull List<Task> q2,
                                     @NonNull List<Task> q3,
                                     @NonNull List<Task> q4) {
        quadrantAdapter1.submitList(q1);
        quadrantAdapter2.submitList(q2);
        quadrantAdapter3.submitList(q3);
        quadrantAdapter4.submitList(q4);

        showQuadrantEmpty(tvEmptyQuadrant1, q1.isEmpty());
        showQuadrantEmpty(tvEmptyQuadrant2, q2.isEmpty());
        showQuadrantEmpty(tvEmptyQuadrant3, q3.isEmpty());
        showQuadrantEmpty(tvEmptyQuadrant4, q4.isEmpty());
    }

    private void showQuadrantEmpty(@Nullable TextView view, boolean show) {
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @NonNull
    private Set<Integer> mapSnapshotToMarkedDayPositions(@NonNull QuerySnapshot snapshot) {
        Set<Integer> markedPositions = new HashSet<>();
        int displayedYear = displayedMonth.get(Calendar.YEAR);
        int displayedMonthValue = displayedMonth.get(Calendar.MONTH);

        for (QueryDocumentSnapshot doc : snapshot) {
            Task task = doc.toObject(Task.class);
            Long dueDate = task.getDueDate();
            if (dueDate == null) {
                continue;
            }

            Calendar dueCalendar = Calendar.getInstance();
            dueCalendar.setTimeInMillis(dueDate);
            if (dueCalendar.get(Calendar.YEAR) != displayedYear
                    || dueCalendar.get(Calendar.MONTH) != displayedMonthValue) {
                continue;
            }

            int dayOfMonth = dueCalendar.get(Calendar.DAY_OF_MONTH);
            int position = leadingEmptyDays + dayOfMonth - 1;
            if (position >= 0 && position < 42) {
                markedPositions.add(position);
            }
        }

        return markedPositions;
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
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (rvTasks != null) {
            rvTasks.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void setupTabToggle() {
        if (tvTabCalendar == null || tvTabEisenhower == null
                || layoutCalendarContent == null || layoutEisenhowerContent == null) {
            return;
        }

        tvTabCalendar.setOnClickListener(v -> switchTab(true));
        tvTabEisenhower.setOnClickListener(v -> switchTab(false));

        switchTab(true);
    }

    private void switchTab(boolean showCalendarTab) {
        if (tvTabCalendar == null || tvTabEisenhower == null
                || layoutCalendarContent == null || layoutEisenhowerContent == null) {
            return;
        }

        layoutCalendarContent.setVisibility(showCalendarTab ? View.VISIBLE : View.GONE);
        layoutEisenhowerContent.setVisibility(showCalendarTab ? View.GONE : View.VISIBLE);

        if (showCalendarTab) {
            tvTabCalendar.setBackgroundResource(R.drawable.bg_calendar_tab_selected);
            tvTabCalendar.setTextColor(requireContext().getColor(R.color.text_white));
            tvTabEisenhower.setBackground(null);
            tvTabEisenhower.setTextColor(0xCCFFFFFF);
        } else {
            tvTabEisenhower.setBackgroundResource(R.drawable.bg_calendar_tab_selected);
            tvTabEisenhower.setTextColor(requireContext().getColor(R.color.text_white));
            tvTabCalendar.setBackground(null);
            tvTabCalendar.setTextColor(0xCCFFFFFF);
        }
    }

    @NonNull
    private List<String> daysInMonthList() {
        List<String> days = new ArrayList<>();

        displayedMonth.setTimeInMillis(System.currentTimeMillis());
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        int maxDaysOfMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstDayOfWeek = displayedMonth.get(Calendar.DAY_OF_WEEK);

        leadingEmptyDays = firstDayOfWeek - Calendar.SUNDAY;
        for (int i = 0; i < leadingEmptyDays; i++) {
            days.add("");
        }

        for (int day = 1; day <= maxDaysOfMonth; day++) {
            days.add(String.valueOf(day));
        }

        while (days.size() < 42) {
            days.add("");
        }

        return days;
    }

    private int findTodayPosition(@NonNull List<String> monthDays) {
        String today = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        for (int i = 0; i < monthDays.size(); i++) {
            if (today.equals(monthDays.get(i))) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private static class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int verticalSpacePx;

        VerticalSpaceItemDecoration(int verticalSpacePx) {
            this.verticalSpacePx = verticalSpacePx;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            outRect.top = position == 0 ? 0 : verticalSpacePx;
        }
    }
}
