package com.newsproject.oneroadmap.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CareerRoadmaps {
    @SerializedName("id")
    private int id;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("pdf_url")
    private String pdfUrl;

    @SerializedName("title")
    private String title;

    @SerializedName("type")
    private String type;

    @SerializedName("education_categories")
    private List<String> educationCategories;

    @SerializedName("bachelor_degrees")
    private List<String> bachelorDegrees;

    @SerializedName("masters_degrees")
    private List<String> mastersDegrees;

    @SerializedName("created_at")
    private String createdAt;

    // Not serialized, derived from educationCategories
    private String educationCategory;

    // Required no-argument constructor
    public CareerRoadmaps() {
    }

    // Constructor with basic fields
    public CareerRoadmaps(String imageUrl, String title, String pdfUrl, String type) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.pdfUrl = pdfUrl;
        this.type = type;
    }

    // Constructor for fallback
    public CareerRoadmaps(String imageUrl, String educationCategory, String title, String pdfUrl,
                          String type, List<String> educationCategories, List<String> bachelorDegrees,
                          List<String> mastersDegrees, String createdAt) {
        this.imageUrl = imageUrl;
        this.educationCategory = educationCategory;
        this.title = title;
        this.pdfUrl = pdfUrl;
        this.type = type;
        this.educationCategories = educationCategories;
        this.bachelorDegrees = bachelorDegrees;
        this.mastersDegrees = mastersDegrees;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public List<String> getEducationCategories() {
        return educationCategories;
    }

    public String getEducationCategory() {
        return educationCategory;
    }

    public List<String> getBachelorDegrees() {
        return bachelorDegrees;
    }

    public List<String> getMastersDegrees() {
        return mastersDegrees;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setEducationCategories(List<String> educationCategories) {
        this.educationCategories = educationCategories;
    }

    public void setEducationCategory(String educationCategory) {
        this.educationCategory = educationCategory;
    }

    public void setBachelorDegrees(List<String> bachelorDegrees) {
        this.bachelorDegrees = bachelorDegrees;
    }

    public void setMastersDegrees(List<String> mastersDegrees) {
        this.mastersDegrees = mastersDegrees;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}