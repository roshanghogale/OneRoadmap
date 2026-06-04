package com.newsproject.oneroadmap.Utils;

import com.google.firebase.Timestamp;

import android.util.Log;
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
        Date date = parseDateString(dateString, false);
        if (date != null) {
            return calculateTimeAgo(date);
        }
        Log.e("TimeAgoUtil", "Failed to parse dateString with all known formats: " + dateString);
        return "Unknown";
    }

    /** Job updates from the API use India local time without a timezone suffix. */
    public static String getTimeAgoForJobUpdate(String dateString) {
        Date date = parseDateString(dateString, true);
        if (date != null) {
            return calculateTimeAgo(date);
        }
        Log.e("TimeAgoUtil", "Failed to parse job update dateString: " + dateString);
        return "Unknown";
    }

    public static long parseJobUpdateMillis(String dateString) {
        Date date = parseDateString(dateString, true);
        return date != null ? date.getTime() : 0L;
    }

    private static Date parseDateString(String dateString, boolean jobUpdateTime) {
        if (dateString == null || dateString.isEmpty()) return null;

        Log.d("TimeAgoUtil", "Parsing dateString: " + dateString);

        String[] formats = {
                "yyyy-MM-dd HH:mm:ss.SSSSSS",
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy, h:mm:ss a",
                "dd/MM/yyyy, HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                sdf.setTimeZone(resolveTimeZone(format, jobUpdateTime));

                Date date = sdf.parse(dateString);
                if (date != null) {
                    Log.d("TimeAgoUtil", "Success with format [" + format + "]: " + date);
                    return date;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static TimeZone resolveTimeZone(String format, boolean jobUpdateTime) {
        if (format.contains("Z")) {
            return TimeZone.getTimeZone("UTC");
        }
        if (jobUpdateTime) {
            return TimeZone.getTimeZone("Asia/Kolkata");
        }
        if (format.startsWith("yyyy-MM-dd")) {
            return TimeZone.getTimeZone("UTC");
        }
        return TimeZone.getTimeZone("Asia/Kolkata");
    }

    private static String calculateTimeAgo(Date date) {
        long now = System.currentTimeMillis();
        long diffInMillis = now - date.getTime();

        Log.d("TimeAgoUtil", "Now: " + now + ", Date: " + date.getTime() + ", Diff: " + diffInMillis);

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
