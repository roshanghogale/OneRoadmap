package com.newsproject.oneroadmap.Models;

import com.google.firebase.firestore.PropertyName;

public class StudentUpdateItem {
    private int id;
    private String title;
    private String education;
    @PropertyName("age_restriction")
    private String ageRestriction;
    @PropertyName("application_method")
    private String applicationMethod;
    private String description;
    @PropertyName("application_link")
    private String applicationLink;
    @PropertyName("last_date")
    private String lastDate;
    @PropertyName("image_url")
    private String imageUrl;
    @PropertyName("icon_url")
    private String iconUrl;
    @PropertyName("notification_pdf_url")
    private String notificationPdfUrl;
    @PropertyName("selection_pdf_url")
    private String selectionPdfUrl;
    @PropertyName("created_at")
    private String createdAt;

    // Empty constructor for Firestore
    public StudentUpdateItem() {
    }

    public StudentUpdateItem(int id, String title, String education, String ageRestriction,
                             String applicationMethod, String description, String applicationLink,
                             String lastDate, String imageUrl, String iconUrl, String notificationPdfUrl,
                             String selectionPdfUrl, String createdAt) {
        this.id = id;
        this.title = title;
        this.education = education;
        this.ageRestriction = ageRestriction;
        this.applicationMethod = applicationMethod;
        this.description = description;
        this.applicationLink = applicationLink;
        this.lastDate = lastDate;
        this.imageUrl = imageUrl;
        this.iconUrl = iconUrl;
        this.notificationPdfUrl = notificationPdfUrl;
        this.selectionPdfUrl = selectionPdfUrl;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    @PropertyName("age_restriction")
    public String getAgeRestriction() {
        return ageRestriction;
    }

    @PropertyName("age_restriction")
    public void setAgeRestriction(String ageRestriction) {
        this.ageRestriction = ageRestriction;
    }

    @PropertyName("application_method")
    public String getApplicationMethod() {
        return applicationMethod;
    }

    @PropertyName("application_method")
    public void setApplicationMethod(String applicationMethod) {
        this.applicationMethod = applicationMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("application_link")
    public String getApplicationLink() {
        return applicationLink;
    }

    @PropertyName("application_link")
    public void setApplicationLink(String applicationLink) {
        this.applicationLink = applicationLink;
    }

    @PropertyName("last_date")
    public String getLastDate() {
        return lastDate;
    }

    @PropertyName("last_date")
    public void setLastDate(String lastDate) {
        this.lastDate = lastDate;
    }

    @PropertyName("image_url")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("image_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("icon_url")
    public String getIconUrl() {
        return iconUrl;
    }

    @PropertyName("icon_url")
    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    @PropertyName("notification_pdf_url")
    public String getNotificationPdfUrl() {
        return notificationPdfUrl;
    }

    @PropertyName("notification_pdf_url")
    public void setNotificationPdfUrl(String notificationPdfUrl) {
        this.notificationPdfUrl = notificationPdfUrl;
    }

    @PropertyName("selection_pdf_url")
    public String getSelectionPdfUrl() {
        return selectionPdfUrl;
    }

    @PropertyName("selection_pdf_url")
    public void setSelectionPdfUrl(String selectionPdfUrl) {
        this.selectionPdfUrl = selectionPdfUrl;
    }

    @PropertyName("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    @PropertyName("created_at")
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}