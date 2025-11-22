package com.newsproject.oneroadmap.Models;

import com.google.firebase.Timestamp;

public class Reply {
    private String name;
    private String title;
    private String replyUserRs;
    private Timestamp uploadTime;  // Timestamp to hold Firestore's timestamp

    // Default constructor for Firestore
    public Reply() {
    }

    public Reply(String name, String title, String replyUserRs, Timestamp uploadTime) {
        this.name = name;
        this.title = title;
        this.replyUserRs = replyUserRs;
        this.uploadTime = uploadTime;
    }

    public String getTitle() {
        return title;
    }

    public Timestamp getUploadTime() {
        return uploadTime;
    }


    public String getName() {
        return name;
    }

    public String getReplyUserRs() {
        return replyUserRs;
    }

    public void setReplyUserRs(String replyUserRs) {
        this.replyUserRs = replyUserRs;
    }
}
