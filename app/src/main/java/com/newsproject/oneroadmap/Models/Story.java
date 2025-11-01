package com.newsproject.oneroadmap.Models;

import com.google.firebase.Timestamp;

public class Story {
    private String documentId; // New field for Firestore document ID
    private String title;
    private String imageUrl;
    private Timestamp uploadTime;
    private String iconUrl;
    private boolean isMainStory;
    private String relativeTime;
    private boolean viewed;
    private String type; // e.g., post, promotion
    private String postDocumentId; // backend job update id
    private String webUrl; // for promotion/external links

    public Story() {}

    public Story(String documentId, String title, Timestamp uploadTime, String imageUrl, String iconUrl, boolean isMainStory, boolean viewed) {
        this.documentId = documentId;
        this.title = title;
        this.uploadTime = uploadTime;
        this.imageUrl = imageUrl;
        this.iconUrl = iconUrl;
        this.isMainStory = isMainStory;
        this.viewed = viewed;
        this.relativeTime = calculateRelativeTime(uploadTime);
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    private String calculateRelativeTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        long now = System.currentTimeMillis();
        long timeMillis = timestamp.toDate().getTime();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Timestamp getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Timestamp uploadTime) {
        this.uploadTime = uploadTime;
        this.relativeTime = calculateRelativeTime(uploadTime);
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean isMainStory() {
        return isMainStory;
    }

    public void setMainStory(boolean mainStory) {
        isMainStory = mainStory;
    }

    public String getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(String relativeTime) {
        this.relativeTime = relativeTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPostDocumentId() {
        return postDocumentId;
    }

    public void setPostDocumentId(String postDocumentId) {
        this.postDocumentId = postDocumentId;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
}