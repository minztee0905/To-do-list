package com.example.ticktok.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Event {

    @DocumentId
    private String id;
    private String title;
    private String icon;
    private Long targetDate;

    @ServerTimestamp
    private Date createdAt;

    public Event() {
    }

    public Event(String title, String icon, Long targetDate) {
        this.title = title;
        this.icon = icon;
        this.targetDate = targetDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Long getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Long targetDate) {
        this.targetDate = targetDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

