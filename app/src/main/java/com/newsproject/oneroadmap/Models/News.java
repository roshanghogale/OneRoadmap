package com.newsproject.oneroadmap.Models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

// News model based on server data structure
public class News {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("type")
    private String type;

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
        @SerializedName("paragraph1")
        private String paragraph1;

        @SerializedName("paragraph2")
        private String paragraph2;

        @SerializedName("paragraph3")
        private String paragraph3;

        @SerializedName("paragraph4")
        private String paragraph4;

        public String getParagraph1() {
            return paragraph1;
        }

        public String getParagraph2() {
            return paragraph2;
        }

        public String getParagraph3() {
            return paragraph3;
        }

        public String getParagraph4() {
            return paragraph4;
        }
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}