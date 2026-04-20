package com.example.ticktok.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;
    private static final int COLOR_COUNTDOWN_ORANGE = Color.parseColor("#FF9800");
    private static final int COLOR_COUNTDOWN_RED = Color.parseColor("#F44336");
    private static final int COLOR_COUNTDOWN_GREEN = Color.parseColor("#4CAF50");
    private static final int COLOR_COUNTDOWN_GRAY = Color.parseColor("#A0A0A0");

    private final List<Event> events = new ArrayList<>();

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvEventIcon.setText(resolveIcon(event));
        holder.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "");

        Long targetDate = event.getTargetDate();
        long daysRemaining = calculateDaysRemaining(targetDate);
        holder.tvEventCountdown.setText(formatCountdown(targetDate, daysRemaining));
        holder.tvEventCountdown.setTextColor(resolveCountdownColor(targetDate, daysRemaining));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void submitList(@NonNull List<Event> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    private String resolveIcon(@NonNull Event event) {
        String icon = event.getIcon();
        if (icon == null || icon.trim().isEmpty()) {
            return "●";
        }
        return icon;
    }

    private long calculateDaysRemaining(Long targetDate) {
        if (targetDate == null) {
            return Long.MIN_VALUE;
        }
        long now = System.currentTimeMillis();
        long targetDayIndex = toLocalDayIndex(targetDate);
        long nowDayIndex = toLocalDayIndex(now);
        return targetDayIndex - nowDayIndex;
    }

    private long toLocalDayIndex(long timeMillis) {
        TimeZone timeZone = TimeZone.getDefault();
        long adjusted = timeMillis + timeZone.getOffset(timeMillis);
        return Math.floorDiv(adjusted, MILLIS_PER_DAY);
    }

    @NonNull
    private String formatCountdown(Long targetDate, long daysRemaining) {
        if (targetDate == null) {
            return "--";
        }
        if (daysRemaining > 0) {
            return "Còn " + daysRemaining + " ngày";
        }
        if (daysRemaining == 0) {
            return "Hôm nay";
        }
        return "Trễ " + Math.abs(daysRemaining) + " ngày";
    }

    private int resolveCountdownColor(Long targetDate, long daysRemaining) {
        if (targetDate == null) {
            return COLOR_COUNTDOWN_GRAY;
        }
        if (daysRemaining == 0) {
            return COLOR_COUNTDOWN_GREEN;
        }
        if (daysRemaining < 0) {
            return COLOR_COUNTDOWN_RED;
        }
        return COLOR_COUNTDOWN_ORANGE;
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView tvEventIcon;
        final TextView tvEventTitle;
        final TextView tvEventCountdown;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventIcon = itemView.findViewById(R.id.tvEventIcon);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventCountdown = itemView.findViewById(R.id.tvEventCountdown);
        }
    }
}
