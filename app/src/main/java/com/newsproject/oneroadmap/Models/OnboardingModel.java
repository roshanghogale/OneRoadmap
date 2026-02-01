package com.newsproject.oneroadmap.Models;

public class OnboardingModel {

    public int centerImage;
    public int icon1;
    public int icon2;
    public int icon3;
    public int icon4;

    public String title;
    public String subtitle;

    public OnboardingModel(
            int centerImage,
            int icon1,
            int icon2,
            int icon3,
            int icon4,
            String title,
            String subtitle
    ) {
        this.centerImage = centerImage;
        this.icon1 = icon1;
        this.icon2 = icon2;
        this.icon3 = icon3;
        this.icon4 = icon4;
        this.title = title;
        this.subtitle = subtitle;
    }
}
