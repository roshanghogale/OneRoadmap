package com.newsproject.oneroadmap.Models;

import com.google.firebase.Timestamp;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Story {
    @SerializedName("id")
    private String documentId;

    @SerializedName("title")
    private String title;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("upload_time")
    private Timestamp uploadTime;

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("is_main_story")
    private boolean isMainStory;

    @SerializedName("relative_time")
    private String relativeTime;

    private boolean viewed;

    @SerializedName("type")
    private String type;

    @SerializedName("post_document_id")
    private String postDocumentId;

    @SerializedName("web_url")
    private String webUrl;

    @SerializedName("video_url")
    private String videoUrl;

    @SerializedName("banner_url")
    private String bannerUrl;

    @SerializedName("media_type")
    private String mediaType;

    @SerializedName("created_at_timestamp")
    private long createdAtTimestamp;

    @SerializedName("other_type")
    private String otherType;

    @SerializedName("education_categories")
    private JsonElement educationCategories;

    @SerializedName("bachelor_degrees")
    private JsonElement bachelorDegrees;

    @SerializedName("masters_degrees")
    private JsonElement mastersDegrees;

    @SerializedName("district")
    private JsonElement district;

    @SerializedName("taluka")
    private JsonElement taluka;

    @SerializedName("age_groups")
    private JsonElement ageGroups;

    @SerializedName("bharty_types")
    private JsonElement bhartyTypes;

    public Story() {}

    /* ---------------- SAFE GETTERS ---------------- */

    public List<String> getEducationCategoriesSafe() { return jsonToList(educationCategories); }
    public List<String> getBachelorDegreesSafe() { return jsonToList(bachelorDegrees); }
    public List<String> getMastersDegreesSafe() { return jsonToList(mastersDegrees); }
    public List<String> getDistrictSafe() { return jsonToList(district); }
    public List<String> getTalukaSafe() { return jsonToList(taluka); }
    public List<String> getAgeGroupsSafe() { return jsonToList(ageGroups); }
    public List<String> getBhartyTypesSafe() { return jsonToList(bhartyTypes); }

    private List<String> jsonToList(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null || element.isJsonNull()) return list;
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> {
                if (!e.isJsonNull()) list.add(e.getAsString());
            });
        } else if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        }
        return list;
    }

    /* ---------------- NORMAL GETTERS & SETTERS ---------------- */

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isViewed() { return viewed; }
    public void setViewed(boolean viewed) { this.viewed = viewed; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Timestamp getUploadTime() { return uploadTime; }
    public void setUploadTime(Timestamp uploadTime) { this.uploadTime = uploadTime; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public boolean isMainStory() { return isMainStory; }
    public void setMainStory(boolean mainStory) { isMainStory = mainStory; }

    public String getRelativeTime() { return relativeTime; }
    public void setRelativeTime(String relativeTime) { this.relativeTime = relativeTime; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPostDocumentId() { return postDocumentId; }
    public void setPostDocumentId(String postDocumentId) { this.postDocumentId = postDocumentId; }

    public String getWebUrl() { return webUrl; }
    public void setWebUrl(String webUrl) { this.webUrl = webUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp; }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }

    public String getOtherType() { return otherType; }
    public void setOtherType(String otherType) { this.otherType = otherType; }
}
