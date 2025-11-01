package com.newsproject.oneroadmap.Models;

import com.google.gson.annotations.SerializedName;

public class CareerRoadmapSlider {
    @SerializedName("id")
    private int id;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("url")
    private String url;

    @SerializedName("created_at")
    private String createdAt;

    // Getters
    public int getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUrl() {
        return url;
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

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}