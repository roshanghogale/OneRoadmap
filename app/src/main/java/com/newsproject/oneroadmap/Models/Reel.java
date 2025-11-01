package com.newsproject.oneroadmap.Models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Reel {
    private String title, videoUrl, videoPath, description, timeAgo, documentIdWebUrl, iconUrl, documentId, uploadDate;
    @PropertyName("timestamp")
    private Timestamp createdAt;

    public Reel() {}

    public Reel(String title, String videoUrl, String videoPath, String description, String timeAgo, String documentIdWebUrl, String iconUrl, String documentId, String uploadDate, Timestamp createdAt) {
        this.title = title;
        this.videoUrl = videoUrl;
        this.videoPath = videoPath;
        this.description = description;
        this.timeAgo = timeAgo;
        this.documentIdWebUrl = documentIdWebUrl;
        this.iconUrl = iconUrl;
        this.documentId = documentId;
        this.uploadDate = uploadDate;
        this.createdAt = createdAt;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }

    public String getDocumentIdWebUrl() { return documentIdWebUrl; }
    public void setDocumentIdWebUrl(String documentIdWebUrl) { this.documentIdWebUrl = documentIdWebUrl; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUploadDate() { return uploadDate; }
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }

    @PropertyName("timestamp")
    public Timestamp getCreatedAt() { return createdAt; }

    @PropertyName("timestamp")
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}