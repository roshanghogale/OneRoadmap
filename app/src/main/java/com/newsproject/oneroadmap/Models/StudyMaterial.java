package com.newsproject.oneroadmap.Models;

public class StudyMaterial {
    private String id;
    private String title;
    private String type;
    private String imageUrl;
    private String pdfUrl;

    public StudyMaterial(String id, String title, String type, String imageUrl, String pdfUrl) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.imageUrl = imageUrl;
        this.pdfUrl = pdfUrl;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    // Setters if needed
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}