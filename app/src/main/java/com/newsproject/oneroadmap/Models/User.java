package com.newsproject.oneroadmap.Models;

public class User {
    private String userId;
    private String name;
    private String gender;
    private String avatar;

    // NEW: 4 study-material booleans
    private boolean studyGovernment;
    private boolean studyPoliceDefence;
    private boolean studyBanking;
    private boolean studySelfImprovement;

    private String degree;
    private String postGraduation;
    private String district;
    private String taluka;
    private boolean currentAffairs;
    private boolean jobs;
    private String ageGroup;
    private String education;
    private String twelfth;

    public User() {} // Required for Firestore

    /** Constructor used by LoginPage3 & ProfileFragment */
    public User(String userId, String name, String gender, String avatar,
                boolean studyGovernment, boolean studyPoliceDefence,
                boolean studyBanking, boolean studySelfImprovement,
                String degree, String postGraduation,
                String district, String taluka,
                boolean currentAffairs, boolean jobs,
                String ageGroup, String education, String twelfth) {

        this.userId = userId;
        this.name = name;
        this.gender = gender;
        this.avatar = avatar;
        this.studyGovernment = studyGovernment;
        this.studyPoliceDefence = studyPoliceDefence;
        this.studyBanking = studyBanking;
        this.studySelfImprovement = studySelfImprovement;
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

    // ---------- Getters & Setters ----------
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    // Study-material booleans
    public boolean isStudyGovernment() { return studyGovernment; }
    public void setStudyGovernment(boolean v) { this.studyGovernment = v; }

    public boolean isStudyPoliceDefence() { return studyPoliceDefence; }
    public void setStudyPoliceDefence(boolean v) { this.studyPoliceDefence = v; }

    public boolean isStudyBanking() { return studyBanking; }
    public void setStudyBanking(boolean v) { this.studyBanking = v; }

    public boolean isStudySelfImprovement() { return studySelfImprovement; }
    public void setStudySelfImprovement(boolean v) { this.studySelfImprovement = v; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getPostGraduation() { return postGraduation; }
    public void setPostGraduation(String postGraduation) { this.postGraduation = postGraduation; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getTaluka() { return taluka; }
    public void setTaluka(String taluka) { this.taluka = taluka; }

    public boolean isCurrentAffairs() { return currentAffairs; }
    public void setCurrentAffairs(boolean currentAffairs) { this.currentAffairs = currentAffairs; }

    public boolean isJobs() { return jobs; }
    public void setJobs(boolean jobs) { this.jobs = jobs; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getTwelfth() { return twelfth; }
    public void setTwelfth(String twelfth) { this.twelfth = twelfth; }
}