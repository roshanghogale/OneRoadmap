package com.newsproject.oneroadmap.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResultItem {
    private String id;
    private String title;
    private String category;
    private String type;
    private String time; // Relative time (e.g., "3m ago")
    private String lastDate;
    private String iconUrl;
    private String imageUrl;
    private String examName;
    private String description1;
    private String description2;
    private String educationRequirement;
    private List<Map<String, String>> websiteUrls;
    private Date createdAt;
    private Date examDate;

    // Empty constructor
    public ResultItem() {
    }

    public ResultItem(String id, String title, String category, String type, Date createdAt, String lastDate, String iconUrl, String imageUrl, String examName, String description1, String description2, String educationRequirement, List<Map<String, String>> websiteUrls) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.type = type;
        this.createdAt = createdAt;
        this.time = createdAt != null ? calculateRelativeTime(createdAt) : "Unknown";
        this.lastDate = lastDate;
        this.iconUrl = iconUrl;
        this.imageUrl = imageUrl;
        this.examName = examName;
        this.description1 = description1;
        this.description2 = description2;
        this.educationRequirement = educationRequirement;
        this.websiteUrls = websiteUrls;
    }

    // Utility method to calculate relative time (e.g., "3m ago")
    private String calculateRelativeTime(Date timestampDate) {
        long now = System.currentTimeMillis();
        long timeMillis = timestampDate.getTime();
        long diffMillis = now - timeMillis;

        long seconds = diffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return seconds + "s ago";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days < 7) {
            return days + "d ago";
        } else {
            return days / 7 + "w ago";
        }
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLastDate() {
        return lastDate;
    }

    public void setLastDate(String lastDate) {
        this.lastDate = lastDate;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public String getDescription1() {
        return description1;
    }

    public void setDescription1(String description1) {
        this.description1 = description1;
    }

    public String getDescription2() {
        return description2;
    }

    public void setDescription2(String description2) {
        this.description2 = description2;
    }

    public String getEducationRequirement() {
        return educationRequirement;
    }

    public void setEducationRequirement(String educationRequirement) {
        this.educationRequirement = educationRequirement;
    }

    public List<Map<String, String>> getWebsiteUrls() {
        return websiteUrls;
    }

    public void setWebsiteUrls(List<Map<String, String>> websiteUrls) {
        this.websiteUrls = websiteUrls;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        this.time = calculateRelativeTime(createdAt);
    }

    public Date getExamDate() {
        return examDate;
    }

    public void setExamDate(Date examDate) {
        this.examDate = examDate;
    }
}