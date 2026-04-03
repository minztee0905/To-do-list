package com.example.ticktok.model;

public class Category {

    private final String id;
    private final String icon;
    private final String title;
    private final boolean defaultItem;

    public Category(String id, String icon, String title, boolean defaultItem) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.defaultItem = defaultItem;
    }

    public String getId() {
        return id;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDefaultItem() {
        return defaultItem;
    }
}

