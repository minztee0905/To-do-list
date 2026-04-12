package com.example.ticktok.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.adapter.CalendarAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarFragment extends Fragment {

    private RecyclerView rvCalendar;
    private CalendarAdapter calendarAdapter;
    private TextView tvTabCalendar;
    private TextView tvTabEisenhower;
    private View layoutCalendarContent;
    private View layoutEisenhowerContent;

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

        setupTabToggle();

        rvCalendar = view.findViewById(R.id.rvCalendar);
        if (rvCalendar == null) {
            return;
        }

        if (rvCalendar.getLayoutManager() == null) {
            rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        }

        List<String> monthDays = daysInMonthList();
        calendarAdapter = new CalendarAdapter(monthDays);
        rvCalendar.setAdapter(calendarAdapter);

        int todayPosition = findTodayPosition(monthDays);
        if (todayPosition != RecyclerView.NO_POSITION) {
            calendarAdapter.setSelectedPosition(todayPosition);
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

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int maxDaysOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        int leadingEmpty = firstDayOfWeek - Calendar.SUNDAY;
        for (int i = 0; i < leadingEmpty; i++) {
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
}

