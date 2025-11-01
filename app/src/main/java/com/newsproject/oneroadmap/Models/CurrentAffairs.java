package com.newsproject.oneroadmap.Models;

public class CurrentAffairs {
    private String date;
    private String imageUrl;
    private String pdfUrl;

    public CurrentAffairs() {
        // Empty constructor for Firestore
    }

    public CurrentAffairs(String date, String imageUrl, String pdfUrl) {
        this.date = date;
        this.imageUrl = imageUrl;
        this.pdfUrl = pdfUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}