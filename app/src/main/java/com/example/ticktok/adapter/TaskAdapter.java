package com.example.ticktok.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskMoreClickListener {
        void onTaskMoreClick(@NonNull View anchorView, @NonNull Task task);
    }

    public interface OnStartDragListener {
        void onStartDrag(@NonNull RecyclerView.ViewHolder viewHolder);
    }

    public interface OnTaskCheckedChangeListener {
        void onTaskCheckedChanged(@NonNull Task task, boolean isChecked);
    }

    public interface OnTaskClickListener {
        void onTaskClicked(@NonNull Task task);
    }

    private final List<Task> tasks = new ArrayList<>();
    @Nullable
    private final OnTaskMoreClickListener moreClickListener;
    @Nullable
    private final OnStartDragListener dragListener;
    @Nullable
    private final OnTaskCheckedChangeListener checkedChangeListener;
    @Nullable
    private final OnTaskClickListener clickListener;

    @Nullable
    private String highlightedTaskId;

    public TaskAdapter() {
        this(null);
    }

    public TaskAdapter(@Nullable OnTaskMoreClickListener moreClickListener) {
        this(moreClickListener, null, null);
    }

    public TaskAdapter(@Nullable OnTaskMoreClickListener moreClickListener,
                       @Nullable OnStartDragListener dragListener,
                       @Nullable OnTaskCheckedChangeListener checkedChangeListener) {
        this(moreClickListener, dragListener, checkedChangeListener, null);
    }

    public TaskAdapter(@Nullable OnTaskMoreClickListener moreClickListener,
                       @Nullable OnStartDragListener dragListener,
                       @Nullable OnTaskCheckedChangeListener checkedChangeListener,
                       @Nullable OnTaskClickListener clickListener) {
        this.moreClickListener = moreClickListener;
        this.dragListener = dragListener;
        this.checkedChangeListener = checkedChangeListener;
        this.clickListener = clickListener;
    }

    public void setHighlightedTaskId(@Nullable String taskId) {
        this.highlightedTaskId = taskId;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        if (highlightedTaskId != null
                && task.getId() != null
                && highlightedTaskId.trim().equals(task.getId().trim())) {
            holder.itemView.setBackgroundResource(R.drawable.bg_task_item_highlight);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_task_item);
        }

        holder.tvTaskTitle.setText(task.getTitle() != null ? task.getTitle() : "");
        holder.cbTask.setOnCheckedChangeListener(null);
        holder.cbTask.setChecked(task.isCompleted());

        holder.cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {

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

        if (holder.ivTaskMore != null) {
            holder.ivTaskMore.setOnClickListener(v -> {
                if (moreClickListener != null) {
                    moreClickListener.onTaskMoreClick(v, task);
                }
            });
        }

        holder.itemView.setOnClickListener(null);
        holder.itemView.setClickable(clickListener != null);
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onTaskClicked(task));
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (dragListener == null) {
                return false;
            }
            dragListener.onStartDrag(holder);
            return true;
        });
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

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= tasks.size()
                || toPosition < 0 || toPosition >= tasks.size()) {
            return;
        }
        if (fromPosition == toPosition) {
            return;
        }
        Collections.swap(tasks, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    public List<Task> getCurrentItems() {
        return new ArrayList<>(tasks);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final CheckBox cbTask;
        final TextView tvTaskTitle;
        final ImageView ivTaskMore;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTask = itemView.findViewById(R.id.cbTask);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            ivTaskMore = itemView.findViewById(R.id.ivTaskMore);
        }
    }
}

