package com.example.ticktok.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final List<String> days;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public CalendarAdapter(@NonNull List<String> days) {
        this.days = days;
    }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= days.size()) {
            return;
        }
        selectedPosition = position;
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

        holder.viewSelectedCircle.setVisibility(isSelected ? View.VISIBLE : View.GONE);
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
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDayNumber;
        final View viewSelectedCircle;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            viewSelectedCircle = itemView.findViewById(R.id.viewSelectedCircle);
        }
    }
}

