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

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, h:mm:ss a", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
            Date date = sdf.parse(dateString);
            if (date != null) {
                return calculateTimeAgo(date);
            }
        } catch (Exception e) {
            // Try fallback ISO format just in case
            try {
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                iso.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = iso.parse(dateString);
                if (date != null) return calculateTimeAgo(date);
            } catch (Exception ignored) {
                // Try yyyy-MM-dd format (date only)
                try {
                    SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    dateOnly.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = dateOnly.parse(dateString);
                    if (date != null) return calculateTimeAgo(date);
                } catch (Exception ignored2) {}
            }
        }
        return "Unknown";
    }

    private static String calculateTimeAgo(Date date) {
        long now = System.currentTimeMillis();
        long diffInMillis = now - date.getTime();

        if (diffInMillis < 0) return "Just now"; // future date?

        long seconds = diffInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (years > 0) return years + "y ago";
        if (months > 0) return months + "mo ago";
        if (weeks > 0) return weeks + "w ago";
        if (days > 0) return days + "d ago";
        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";

        return "Just now";
    }
}