package com.example.ticktok.model;

public class Category {

    private final String id;
    private final String icon;
    private final String title;
    private final int order;

    public Category(String id, String icon, String title, int order) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.order = order;
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

    public int getOrder() {
        return order;
    }
}

