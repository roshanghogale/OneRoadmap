package com.newsproject.oneroadmap.Utils;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeAgoUtil {

    public static String getTimeAgo(Timestamp timestamp) {
        if (timestamp == null) return "Unknown";
        return calculateTimeAgo(timestamp.toDate());
    }

    public static String getTimeAgo(Date date) {
        if (date == null) return "Unknown";
        return calculateTimeAgo(date);
    }

    public static String getTimeAgo(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "Unknown";

        // List of common formats including the one from your server
        String[] formats = {
                "yyyy-MM-dd HH:mm:ss.SSSSSS",
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy, h:mm:ss a", // ✅ Matches your example: 28/12/2025, 5:47:52 am
                "dd/MM/yyyy, HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                if (format.contains("Z") || format.startsWith("yyyy-MM-dd")) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                } else {
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                }
                
                Date date = sdf.parse(dateString);
                if (date != null) {
                    return calculateTimeAgo(date);
                }
            } catch (Exception ignored) {
            }
        }
        
        return "Unknown";
    }

    private static String calculateTimeAgo(Date date) {
        long now = System.currentTimeMillis();
        long diffInMillis = now - date.getTime();

        if (diffInMillis < 0) return "Just now";

        long seconds = diffInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (years > 0) return years + (years == 1 ? " year ago" : " years ago");
        if (months > 0) return months + (months == 1 ? " month ago" : " months ago");
        if (weeks > 0) return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        if (days > 0) return days + (days == 1 ? " day ago" : " days ago");
        if (hours > 0) return hours + (hours == 1 ? " hour ago" : " hours ago");
        if (minutes > 0) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");

        return "Just now";
    }
}
