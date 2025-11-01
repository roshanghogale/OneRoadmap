package com.newsproject.oneroadmap.Models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Slider implements Parcelable {
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
    private List<String> educationCategories;

    @SerializedName("bachelor_degrees")
    private List<String> bachelorDegrees;

    @SerializedName("masters_degrees")
    private List<String> mastersDegrees;

    @SerializedName("district")
    private String district;

    @SerializedName("taluka")
    private String taluka;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // No-argument constructor
    public Slider() {
    }

    // Full constructor
    public Slider(int id, String title, String postDocumentId, String webUrl, String type, String pageType,
                  boolean isSpecific, String otherType, List<String> educationCategories,
                  List<String> bachelorDegrees, List<String> mastersDegrees, String district,
                  String taluka, String imageUrl, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.postDocumentId = postDocumentId;
        this.webUrl = webUrl;
        this.type = type;
        this.pageType = pageType;
        this.isSpecific = isSpecific;
        this.otherType = otherType;
        this.educationCategories = educationCategories;
        this.bachelorDegrees = bachelorDegrees;
        this.mastersDegrees = mastersDegrees;
        this.district = district;
        this.taluka = taluka;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPostDocumentId() { return postDocumentId; }
    public void setPostDocumentId(String postDocumentId) { this.postDocumentId = postDocumentId; }
    public String getWebUrl() { return webUrl; }
    public void setWebUrl(String webUrl) { this.webUrl = webUrl; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPageType() { return pageType; }
    public void setPageType(String pageType) { this.pageType = pageType; }
    public boolean isSpecific() { return isSpecific; }
    public void setSpecific(boolean specific) { isSpecific = specific; }
    public String getOtherType() { return otherType; }
    public void setOtherType(String otherType) { this.otherType = otherType; }
    public List<String> getEducationCategories() { return educationCategories; }
    public void setEducationCategories(List<String> educationCategories) { this.educationCategories = educationCategories; }
    public List<String> getBachelorDegrees() { return bachelorDegrees; }
    public void setBachelorDegrees(List<String> bachelorDegrees) { this.bachelorDegrees = bachelorDegrees; }
    public List<String> getMastersDegrees() { return mastersDegrees; }
    public void setMastersDegrees(List<String> mastersDegrees) { this.mastersDegrees = mastersDegrees; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getTaluka() { return taluka; }
    public void setTaluka(String taluka) { this.taluka = taluka; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // Parcelable implementation
    protected Slider(Parcel in) {
        id = in.readInt();
        title = in.readString();
        postDocumentId = in.readString();
        webUrl = in.readString();
        type = in.readString();
        pageType = in.readString();
        isSpecific = in.readByte() != 0;
        otherType = in.readString();
        educationCategories = in.createStringArrayList();
        bachelorDegrees = in.createStringArrayList();
        mastersDegrees = in.createStringArrayList();
        district = in.readString();
        taluka = in.readString();
        imageUrl = in.readString();
        createdAt = in.readString();
        updatedAt = in.readString();
    }

    public static final Creator<Slider> CREATOR = new Creator<Slider>() {
        @Override
        public Slider createFromParcel(Parcel in) {
            return new Slider(in);
        }

        @Override
        public Slider[] newArray(int size) {
            return new Slider[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(postDocumentId);
        dest.writeString(webUrl);
        dest.writeString(type);
        dest.writeString(pageType);
        dest.writeByte((byte) (isSpecific ? 1 : 0));
        dest.writeString(otherType);
        dest.writeStringList(educationCategories);
        dest.writeStringList(bachelorDegrees);
        dest.writeStringList(mastersDegrees);
        dest.writeString(district);
        dest.writeString(taluka);
        dest.writeString(imageUrl);
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
    }
}