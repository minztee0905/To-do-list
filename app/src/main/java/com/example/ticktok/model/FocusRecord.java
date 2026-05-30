package com.example.ticktok.model;

import androidx.annotation.NonNull;

public class FocusRecord {
    @NonNull
    private final String title;
    @NonNull
    private final String subtitle;
    @NonNull
    private final String durationLabel;
    @NonNull
    private final String tag;

    public FocusRecord(@NonNull String title,
                       @NonNull String subtitle,
                       @NonNull String durationLabel,
                       @NonNull String tag) {
        this.title = title;
        this.subtitle = subtitle;
        this.durationLabel = durationLabel;
        this.tag = tag;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getSubtitle() {
        return subtitle;
    }

    @NonNull
    public String getDurationLabel() {
        return durationLabel;
    }

    @NonNull
    public String getTag() {
        return tag;
    }
}

