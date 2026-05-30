package com.example.ticktok.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.model.FocusRecord;

import java.util.ArrayList;
import java.util.List;

public class FocusRecordAdapter extends RecyclerView.Adapter<FocusRecordAdapter.FocusRecordViewHolder> {

    private final List<FocusRecord> records = new ArrayList<>();

    @NonNull
    @Override
    public FocusRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_focus_record, parent, false);
        return new FocusRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FocusRecordViewHolder holder, int position) {
        FocusRecord record = records.get(position);
        holder.tvTitle.setText(record.getTitle());
        holder.tvSubtitle.setText(record.getSubtitle());
        holder.tvDuration.setText(record.getDurationLabel());
        holder.tvTag.setText(record.getTag());
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void submitList(@NonNull List<FocusRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    static class FocusRecordViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvDuration;
        final TextView tvTag;

        FocusRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRecordTitle);
            tvSubtitle = itemView.findViewById(R.id.tvRecordSubtitle);
            tvDuration = itemView.findViewById(R.id.tvRecordDuration);
            tvTag = itemView.findViewById(R.id.tvRecordTag);
        }
    }
}

