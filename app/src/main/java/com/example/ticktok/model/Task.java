package com.example.ticktok.model;

public class Task {

    private String id;
    private String title;
    private String categoryName;
    private long dueDate;
    private boolean isCompleted;
    private long createdAt;

    public Task() {
    }

    public Task(String id, String title, String categoryName, long dueDate, boolean isCompleted, long createdAt) {
        this.id = id;
        this.title = title;
        this.categoryName = categoryName;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategoryName() { return categoryName; }
    public long getDueDate() { return dueDate; }
    public boolean isCompleted() { return isCompleted; }
    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}