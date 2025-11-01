package com.newsproject.oneroadmap.Models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.gson.annotations.SerializedName;
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JobUpdate implements Parcelable {
    @SerializedName("title")
    private String title;

    @SerializedName("salary")
    private String salary;  // ← Changed to String

    @SerializedName("last_date")
    private String lastDateString;

    private Timestamp lastDateTimestamp;

    @SerializedName("time_ago")
    private String timeAgo;

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("post_name")
    private String postName;

    @SerializedName("education_categories")
    private List<String> educationCategories;

    @SerializedName("bachelor_degrees")
    private List<String> bachelorDegrees;

    @SerializedName("masters_degrees")
    private List<String> mastersDegrees;

    @SerializedName("age_requirement")
    private String ageRequirement;

    @SerializedName("job_place")
    private String jobPlace;

    @SerializedName("application_fees")
    private String applicationFees;  // ← Changed to String

    @SerializedName("application_link")
    private String applicationLink;

    @SerializedName("pdf_url")
    private String notificationPdfLink;

    @SerializedName("selection_pdf_url")
    private String selectionPdfLink;

    @SerializedName("syllabus_pdf_url")
    private String syllabusPdf;

    @SerializedName("id")
    private String documentId;

    @SerializedName("type")
    private String type;

    @SerializedName("sub_type")
    private String subType;

    @SerializedName("description")
    private String description;

    @SerializedName("note")
    private String note;

    @SerializedName("created_at")
    private String createdAtString;

    @SerializedName("education_requirement")
    private String educationRequirement;  // ← NEW

    @SerializedName("total_posts")
    private String totalPosts;  // ← NEW

    @ServerTimestamp
    private Timestamp timestamp;

    public JobUpdate() {}

    public JobUpdate(String title, String salary, String lastDateString, Timestamp lastDateTimestamp, String timeAgo,
                     String iconUrl, String imageUrl, String postName, List<String> educationCategories,
                     List<String> bachelorDegrees, List<String> mastersDegrees, String ageRequirement,
                     String jobPlace, String applicationFees, String applicationLink, String notificationPdfLink,
                     String selectionPdfLink, String syllabusPdf, String documentId, String type, String subType,
                     String description, String note, String createdAtString, String educationRequirement,
                     String totalPosts, Timestamp timestamp) {
        this.title = title;
        this.salary = salary;
        this.lastDateString = lastDateString;
        this.lastDateTimestamp = lastDateTimestamp;
        this.timeAgo = timeAgo;
        this.iconUrl = iconUrl;
        this.imageUrl = imageUrl;
        this.postName = postName;
        this.educationCategories = educationCategories;
        this.bachelorDegrees = bachelorDegrees;
        this.mastersDegrees = mastersDegrees;
        this.ageRequirement = ageRequirement;
        this.jobPlace = jobPlace;
        this.applicationFees = applicationFees;
        this.applicationLink = applicationLink;
        this.notificationPdfLink = notificationPdfLink;
        this.selectionPdfLink = selectionPdfLink;
        this.syllabusPdf = syllabusPdf;
        this.documentId = documentId;
        this.type = type;
        this.subType = subType;
        this.description = description;
        this.note = note;
        this.createdAtString = createdAtString;
        this.educationRequirement = educationRequirement;
        this.totalPosts = totalPosts;
        this.timestamp = timestamp;
    }

    protected JobUpdate(Parcel in) {
        title = in.readString();
        salary = in.readString();
        lastDateString = in.readString();
        lastDateTimestamp = in.readParcelable(Timestamp.class.getClassLoader());
        timeAgo = in.readString();
        iconUrl = in.readString();
        imageUrl = in.readString();
        postName = in.readString();
        educationCategories = in.createStringArrayList();
        bachelorDegrees = in.createStringArrayList();
        mastersDegrees = in.createStringArrayList();
        ageRequirement = in.readString();
        jobPlace = in.readString();
        applicationFees = in.readString();
        applicationLink = in.readString();
        notificationPdfLink = in.readString();
        selectionPdfLink = in.readString();
        syllabusPdf = in.readString();
        documentId = in.readString();
        type = in.readString();
        subType = in.readString();
        description = in.readString();
        note = in.readString();
        createdAtString = in.readString();
        educationRequirement = in.readString();
        totalPosts = in.readString();
        timestamp = in.readParcelable(Timestamp.class.getClassLoader());
    }

    public static final Creator<JobUpdate> CREATOR = new Creator<JobUpdate>() {
        @Override
        public JobUpdate createFromParcel(Parcel in) {
            return new JobUpdate(in);
        }

        @Override
        public JobUpdate[] newArray(int size) {
            return new JobUpdate[size];
        }
    };

    // --- FORMATTED GETTERS ---
    public String getFormattedSalary() {
        return salary != null && !salary.trim().isEmpty() ? salary.trim() : "N/A";
    }

    public String getFormattedApplicationFees() {
        return applicationFees != null && !applicationFees.trim().isEmpty() ? applicationFees.trim() : "N/A";
    }

    public String getFormattedLastDate() {
        return getFormattedLastDateInternal(Locale.US, false);
    }

    public String getTimeAgo() {
        if (timestamp != null) {
            return TimeAgoUtil.getTimeAgo(timestamp);
        }
        if (createdAtString != null && !createdAtString.isEmpty()) {
            return TimeAgoUtil.getTimeAgo(createdAtString);
        }
        return "Unknown";
    }

    public String getEducationSummary() {
        if (educationRequirement != null && !educationRequirement.trim().isEmpty()) {
            return educationRequirement.trim();
        }
        if (educationCategories != null && !educationCategories.isEmpty()) {
            return String.join(", ", educationCategories);
        }
        return "N/A";
    }

    // --- GETTERS & SETTERS ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getLastDateString() { return lastDateString; }
    public void setLastDateString(String lastDateString) { this.lastDateString = lastDateString; }

    public Timestamp getLastDateTimestamp() { return lastDateTimestamp; }
    public void setLastDateTimestamp(Timestamp lastDateTimestamp) { this.lastDateTimestamp = lastDateTimestamp; }

    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }

    public Map<String, List<String>> getEducationRequirement() {
        Map<String, List<String>> educationMap = new HashMap<>();
        educationMap.put("categories", educationCategories != null ? educationCategories : new ArrayList<>());
        educationMap.put("bachelors", bachelorDegrees != null ? bachelorDegrees : new ArrayList<>());
        educationMap.put("masters", mastersDegrees != null ? mastersDegrees : new ArrayList<>());
        return educationMap;
    }

    public void setEducationRequirement(Map<String, List<String>> educationRequirement) {
        this.educationCategories = educationRequirement.get("categories");
        this.bachelorDegrees = educationRequirement.get("bachelors");
        this.mastersDegrees = educationRequirement.get("masters");
    }

    public String getAgeRequirement() { return ageRequirement; }
    public void setAgeRequirement(String ageRequirement) { this.ageRequirement = ageRequirement; }

    public String getJobPlace() { return jobPlace; }
    public void setJobPlace(String jobPlace) { this.jobPlace = jobPlace; }

    public String getApplicationFees() { return applicationFees; }
    public void setApplicationFees(String applicationFees) { this.applicationFees = applicationFees; }

    public String getApplicationLink() { return applicationLink; }
    public void setApplicationLink(String applicationLink) { this.applicationLink = applicationLink; }

    public String getNotificationPdfLink() { return notificationPdfLink; }
    public void setNotificationPdfLink(String notificationPdfLink) { this.notificationPdfLink = notificationPdfLink; }

    public String getSelectionPdfLink() { return selectionPdfLink; }
    public void setSelectionPdfLink(String selectionPdfLink) { this.selectionPdfLink = selectionPdfLink; }

    public String getSyllabusPdf() { return syllabusPdf; }
    public void setSyllabusPdf(String syllabusPdf) { this.syllabusPdf = syllabusPdf; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getType() { return type != null ? type.toLowerCase() : null; }
    public void setType(String type) { this.type = type; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getEducationCategories() { return educationCategories; }
    public void setEducationCategories(List<String> educationCategories) { this.educationCategories = educationCategories; }

    public List<String> getBachelorDegrees() { return bachelorDegrees; }
    public void setBachelorDegrees(List<String> bachelorDegrees) { this.bachelorDegrees = bachelorDegrees; }

    public List<String> getMastersDegrees() { return mastersDegrees; }
    public void setMastersDegrees(List<String> mastersDegrees) { this.mastersDegrees = mastersDegrees; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedAtString() { return createdAtString; }
    public void setCreatedAtString(String createdAtString) { this.createdAtString = createdAtString; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getEducationRequirementText() { return educationRequirement; }
    public void setEducationRequirement(String educationRequirement) { this.educationRequirement = educationRequirement; }

    public String getTotalPosts() { return totalPosts; }
    public void setTotalPosts(String totalPosts) { this.totalPosts = totalPosts; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(title);
        parcel.writeString(salary);
        parcel.writeString(lastDateString);
        parcel.writeParcelable(lastDateTimestamp, flags);
        parcel.writeString(timeAgo);
        parcel.writeString(iconUrl);
        parcel.writeString(imageUrl);
        parcel.writeString(postName);
        parcel.writeStringList(educationCategories);
        parcel.writeStringList(bachelorDegrees);
        parcel.writeStringList(mastersDegrees);
        parcel.writeString(ageRequirement);
        parcel.writeString(jobPlace);
        parcel.writeString(applicationFees);
        parcel.writeString(applicationLink);
        parcel.writeString(notificationPdfLink);
        parcel.writeString(selectionPdfLink);
        parcel.writeString(syllabusPdf);
        parcel.writeString(documentId);
        parcel.writeString(type);
        parcel.writeString(subType);
        parcel.writeString(description);
        parcel.writeString(note);
        parcel.writeString(createdAtString);
        parcel.writeString(educationRequirement);
        parcel.writeString(totalPosts);
        parcel.writeParcelable(timestamp, flags);
    }

    // --- FIXED: MARATHI LAST DATE (NO -1 DAY) ---
    public String getFormattedLastDateMarathi() {
        return getFormattedLastDateInternal(new Locale("mr", "IN"), true);
    }

    // --- PRIVATE: UTC-SAFE DATE FORMATTER ---
    private String getFormattedLastDateInternal(Locale locale, boolean useMarathiDigits) {
        Date date = null;

        if (lastDateString != null && !lastDateString.isEmpty()) {
            // Try YYYY-MM-DD first (most common now)
            try {
                SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                localFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                date = localFormat.parse(lastDateString);
            } catch (Exception e1) {
                // Fallback: ISO UTC
                try {
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    date = isoFormat.parse(lastDateString);
                } catch (Exception e2) {
                    return "N/A";
                }
            }
        }

        if (date == null) return "N/A";

        // Format in IST
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", locale);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));

        String formatted = sdf.format(date);

        if (useMarathiDigits) {
            formatted = toMarathiDigits(formatted);
        }

        return formatted;
    }

    // --- MARATHI DIGITS (UNCHANGED) ---
    private String toMarathiDigits(String input) {
        if (input == null) return null;
        return input
                .replace('0', '०')
                .replace('1', '१')
                .replace('2', '२')
                .replace('3', '३')
                .replace('4', '४')
                .replace('5', '५')
                .replace('6', '६')
                .replace('7', '७')
                .replace('8', '८')
                .replace('9', '९');
    }
}