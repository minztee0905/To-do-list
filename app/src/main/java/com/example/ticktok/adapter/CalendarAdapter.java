package com.example.ticktok.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(int position, @NonNull String dayValue);
    }

    private final List<String> days;
    @Nullable
    private final OnDayClickListener onDayClickListener;
    private final Set<Integer> dayPositionsWithTasks = new HashSet<>();
    private int selectedPosition = RecyclerView.NO_POSITION;

    public CalendarAdapter(@NonNull List<String> days, @Nullable OnDayClickListener onDayClickListener) {
        this.days = days;
        this.onDayClickListener = onDayClickListener;
    }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= days.size()) {
            return;
        }
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void setDayPositionsWithTasks(@NonNull Set<Integer> positions) {
        dayPositionsWithTasks.clear();
        dayPositionsWithTasks.addAll(positions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String dayValue = days.get(position);
        holder.tvDayNumber.setText(dayValue);

        boolean isEmptyCell = dayValue == null || dayValue.trim().isEmpty();
        boolean isSelected = !isEmptyCell && position == selectedPosition;
        boolean hasTaskDot = !isEmptyCell && dayPositionsWithTasks.contains(position);

        holder.viewSelectedCircle.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.viewTaskDot.setVisibility(hasTaskDot ? View.VISIBLE : View.GONE);
        holder.tvDayNumber.setAlpha(isEmptyCell ? 0.25f : 1f);

        holder.itemView.setOnClickListener(v -> {
            if (isEmptyCell) {
                return;
            }

            int oldSelected = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            if (oldSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldSelected);
            }
            notifyItemChanged(selectedPosition);

            if (onDayClickListener != null) {
                onDayClickListener.onDayClick(selectedPosition, dayValue);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDayNumber;
        final View viewSelectedCircle;
        final View viewTaskDot;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            viewSelectedCircle = itemView.findViewById(R.id.viewSelectedCircle);
            viewTaskDot = itemView.findViewById(R.id.viewTaskDot);
        }
    }
}
