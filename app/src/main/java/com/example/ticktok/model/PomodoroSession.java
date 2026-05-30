package com.example.ticktok.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class PomodoroSession {


    @Nullable
    private String id;

    @Nullable
    private String userId;

    @Nullable
    private String taskName;


    private long duration;


    private long timestamp;


    public PomodoroSession() {
    }

    public PomodoroSession(@Nullable String id,
                           @Nullable String userId,
                           @Nullable String taskName,
                           long duration,
                           long timestamp) {
        this.id = id;
        this.userId = userId;
        this.taskName = taskName;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    @Nullable
    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(@Nullable String taskName) {
        this.taskName = taskName;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "PomodoroSession{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", taskName='" + taskName + '\'' +
                ", duration=" + duration +
                ", timestamp=" + timestamp +
                '}';
    }
}

