package com.example.ticktok.model;

public class Category {

    private String id;
    private String icon;
    private String title;
    private int order;

    public Category() {
    }

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

    public void setId(String id) {
        this.id = id;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}

