package com.example.ticktok.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.model.Task;

import java.util.ArrayList;
import java.util.List;

public class EisenhowerTaskAdapter extends RecyclerView.Adapter<EisenhowerTaskAdapter.TaskViewHolder> {

    public interface OnTaskCheckedChangeListener {
        void onTaskCheckedChanged(@NonNull Task task, boolean isChecked);
    }

    private final List<Task> tasks = new ArrayList<>();
    @Nullable
    private final OnTaskCheckedChangeListener checkedChangeListener;

    public EisenhowerTaskAdapter() {
        this(null);
    }

    public EisenhowerTaskAdapter(@Nullable OnTaskCheckedChangeListener checkedChangeListener) {
        this.checkedChangeListener = checkedChangeListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_compact, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvTaskTitle.setText(task.getTitle() != null ? task.getTitle() : "");
        holder.cbTask.setOnCheckedChangeListener(null);
        holder.cbTask.setChecked(task.isCompleted());

        holder.cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Keep UI stable immediately
            task.setCompleted(isChecked);
            if (checkedChangeListener != null) {
                checkedChangeListener.onTaskCheckedChanged(task, isChecked);
            }
            notifyItemChanged(holder.getBindingAdapterPosition());
        });

        if (task.isCompleted()) {
            holder.tvTaskTitle.setAlpha(0.6f);
        } else {
            holder.tvTaskTitle.setAlpha(1f);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void submitList(@NonNull List<Task> newTasks) {
        tasks.clear();
        tasks.addAll(newTasks);
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final CheckBox cbTask;
        final TextView tvTaskTitle;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTask = itemView.findViewById(R.id.cbTask);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
        }
    }
}

