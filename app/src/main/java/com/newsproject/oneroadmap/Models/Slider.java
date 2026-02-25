package com.newsproject.oneroadmap.Models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Slider {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("post_document_id")
    private String postDocumentId;

    @SerializedName("web_url")
    private String webUrl;

    @SerializedName("type")
    private String type;

    @SerializedName("page_type")
    private String pageType;

    @SerializedName("is_specific")
    private boolean isSpecific;

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

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    /* ---------------- SAFE GETTERS ---------------- */

    public List<String> getEducationCategoriesSafe() {
        return jsonToList(educationCategories);
    }

    public List<String> getBachelorDegreesSafe() {
        return jsonToList(bachelorDegrees);
    }

    public List<String> getMastersDegreesSafe() {
        return jsonToList(mastersDegrees);
    }

    public List<String> getDistrictSafe() {
        return jsonToList(district);
    }

    public List<String> getTalukaSafe() {
        return jsonToList(taluka);
    }

    public List<String> getAgeGroupsSafe() {
        return jsonToList(ageGroups);
    }

    public List<String> getBhartyTypesSafe() {
        return jsonToList(bhartyTypes);
    }

    /* ---------------- HELPERS ---------------- */

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

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getPostDocumentId() { return postDocumentId; }
    public String getWebUrl() { return webUrl; }
    public String getType() { return type; }
    public String getPageType() { return pageType; }
    public boolean isSpecific() { return isSpecific; }
    public String getOtherType() { return otherType; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
