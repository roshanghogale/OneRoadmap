package com.newsproject.oneroadmap.Models;

public class RecentlyOpenedItem {
    private final int logoResId;
    private final String lastDate;
    private final String type;
    private final String title;

    public RecentlyOpenedItem(int logoResId, String lastDate, String type, String title) {
        this.logoResId = logoResId;
        this.lastDate = lastDate;
        this.type = type;
        this.title = title;
    }

    public int getLogoResId() {
        return logoResId;
    }

    public String getLastDate() {
        return lastDate;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }
}

