package com.newsproject.oneroadmap.Models;

public class User {
    private String userId;
    private String name;
    private String gender;
    private String avatar;
    private boolean upsc;
    private boolean mpsc;
    private String degree;
    private String postGraduation;
    private String district;
    private String taluka;
    private boolean currentAffairs; // Changed to boolean
    private boolean jobs;           // Changed to boolean
    private String ageGroup;
    private String education;
    private String twelfth;         // Added twelfth field

    public User() {} // Required for Firestore

    public User(String userId, String name, String gender, String avatar, boolean upsc, boolean mpsc,
                String degree, String postGraduation, String district, String taluka,
                boolean currentAffairs, boolean jobs, String ageGroup, String education, String twelfth) {
        this.userId = userId;
        this.name = name;
        this.gender = gender;
        this.avatar = avatar;
        this.upsc = upsc;
        this.mpsc = mpsc;
        this.degree = degree;
        this.postGraduation = postGraduation;
        this.district = district;
        this.taluka = taluka;
        this.currentAffairs = currentAffairs;
        this.jobs = jobs;
        this.ageGroup = ageGroup;
        this.education = education;
        this.twelfth = twelfth;
    }

    // Getters and Setters
    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isUpsc() {
        return upsc;
    }

    public void setUpsc(boolean upsc) {
        this.upsc = upsc;
    }

    public boolean isMpsc() {
        return mpsc;
    }

    public void setMpsc(boolean mpsc) {
        this.mpsc = mpsc;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getPostGraduation() {
        return postGraduation;
    }

    public void setPostGraduation(String postGraduation) {
        this.postGraduation = postGraduation;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getTaluka() {
        return taluka;
    }

    public void setTaluka(String taluka) {
        this.taluka = taluka;
    }

    public boolean isCurrentAffairs() { // Changed to boolean
        return currentAffairs;
    }

    public void setCurrentAffairs(boolean currentAffairs) {
        this.currentAffairs = currentAffairs;
    }

    public boolean isJobs() { // Changed to boolean
        return jobs;
    }

    public void setJobs(boolean jobs) {
        this.jobs = jobs;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getTwelfth() { // Added getter
        return twelfth;
    }

    public void setTwelfth(String twelfth) { // Added setter
        this.twelfth = twelfth;
    }
}