package com.newsproject.oneroadmap.Models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Query {
    private String userId;
    private final String name;
    private final String education;
    private final String type;
    private final String title;
    private final Timestamp uploadTime;
    private final String imageUrl;
    private final Reply reply;
    private Boolean isLike;
    private int likeCount;
    private String documentId;
    private List<String> likedByUsers;

    public List<String> getLikedByUsers() {
        return likedByUsers;
    }

    public void setLikedByUsers(List<String> likedByUsers) {
        this.likedByUsers = likedByUsers;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Query(String userId, String name, String education, String type, String title, Timestamp uploadTime,
                 String imageUrl, Reply reply, Boolean isLike, int likeCount, String documentId, List<String> likedByUsers) {
        this.userId = userId;
        this.name = name;
        this.education = education;
        this.type = type;
        this.title = title;
        this.uploadTime = uploadTime;
        this.imageUrl = imageUrl;
        this.reply = reply;
        this.isLike = isLike;
        this.likeCount = likeCount;
        this.documentId = documentId;
        this.likedByUsers = likedByUsers;
    }

    public String getName() {
        return name;
    }

    public String getEducation() {
        return education;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public Timestamp getUploadTime() {
        return uploadTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Reply getReply() {
        return reply;
    }

    public Boolean getLike() {
        return isLike;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setLike(Boolean like) {
        isLike = like;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
