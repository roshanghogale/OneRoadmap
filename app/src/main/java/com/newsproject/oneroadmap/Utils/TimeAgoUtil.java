package com.newsproject.oneroadmap.Utils;

import android.util.Log;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeAgoUtil {
    private static final String TAG = "TimeAgoUtil";

    // Existing method for Firebase Timestamp
    public static String getTimeAgo(Timestamp timestamp) {
        if (timestamp == null) {
            Log.w(TAG, "getTimeAgo(Timestamp): Timestamp is null");
            return "Unknown";
        }

        Date date = timestamp.toDate();
        return calculateTimeAgo(date);
    }

    // New method for API date string
    public static String getTimeAgo(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            Log.w(TAG, "getTimeAgo(String): Date string is null or empty");
            return "Unknown";
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date date = isoFormat.parse(dateString);
            if (date == null) {
                Log.w(TAG, "getTimeAgo(String): Failed to parse date string: " + dateString);
                return "Unknown";
            }
            return calculateTimeAgo(date);
        } catch (Exception e) {
            Log.e(TAG, "getTimeAgo(String): Error parsing date string: " + dateString, e);
            return "Unknown";
        }
    }

    // Common method to calculate time ago from a Date object
    private static String calculateTimeAgo(Date date) {
        long now = System.currentTimeMillis();
        long diffInMillis = now - date.getTime();

        long seconds = diffInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (years > 0) {
            return years + "y ago";
        } else if (months > 0) {
            return months + "mo ago";
        } else if (weeks > 0) {
            return weeks + "w ago";
        } else if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "Just now";
        }
    }
}