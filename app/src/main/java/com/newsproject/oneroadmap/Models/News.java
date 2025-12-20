package com.newsproject.oneroadmap.Models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

// News model based on server data structure
public class News {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("title_description")
    private String titleDescription;

    @SerializedName("sub_title")
    private String subTitle;

    @SerializedName("type")
    private String type;

    @SerializedName("date")
    private String date;

    @SerializedName("web_url")
    private String webUrl;

    @SerializedName("description")
    private Description description;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private Date createdAt;

    @SerializedName("updated_at")
    private Date updatedAt;

    public static class Description {
        @SerializedName("titleDescription")
        private String titleDescription;

        @SerializedName("subTitle")
        private String subTitle;

        @SerializedName("paragraph1")
        private String paragraph1;

        @SerializedName("paragraph2")
        private String paragraph2;

        public String getTitleDescription() {
            return titleDescription;
        }

        public String getSubTitle() {
            return subTitle;
        }

        public String getParagraph1() {
            return paragraph1;
        }

        public String getParagraph2() {
            return paragraph2;
        }
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleDescription() {
        return titleDescription;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public Description getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}