package com.newsproject.oneroadmap.Services;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.newsproject.oneroadmap.Activities.MainActivity;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;

import org.json.JSONArray;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_SERVICE";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();
        if (data == null || data.isEmpty()) return;

        String type = data.get("type");
        if (type == null) type = "news";
        type = type.toLowerCase();

        String title = data.get("title");
        String body = data.get("body");

        String bannerUrl = data.get("banner_url");
        if (bannerUrl == null || bannerUrl.isEmpty()) {
            bannerUrl = data.get("image_url");
        }

        // 🔥 FIX: handle relative URLs
        if (bannerUrl != null && !bannerUrl.startsWith("http")) {
            bannerUrl = "http://test.todaybharti.in" + bannerUrl;
        }

        String iconUrl = data.get("icon_url");

        int soundRes = getSound(type);
        String channelId = "channel_" + type;
        String channelName = getChannelName(type);

        String finalBannerUrl = bannerUrl;
        String finalType = type;

        new Thread(() -> {

            Bitmap bannerBitmap = getBitmap(finalBannerUrl);
            Bitmap iconBitmap = getBitmap(iconUrl);

            sendNotification(title, body, bannerBitmap, iconBitmap,
                    channelId, channelName, soundRes, finalType, data);

        }).start();
    }

    private void sendNotification(
            String title,
            String body,
            Bitmap banner,
            Bitmap icon,
            String channelId,
            String channelName,
            int soundRes,
            String type,
            Map<String, String> data
    ) {

        int notificationId = (int) System.currentTimeMillis();

        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/" + soundRes);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.app_logo)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setSound(soundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        /*
        ================= STORY =================
        */
        if ("story".equals(type)) {

            if (icon != null) builder.setLargeIcon(icon);

            PendingIntent pi = getDefaultIntent(notificationId);
            builder.setContentIntent(pi);
            builder.addAction(0, "Story पहा ⏰", pi);
        }

        /*
        ================= SLIDER =================
        */
        else if ("slider".equals(type)) {

            if (banner != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(banner)
                        .setBigContentTitle(title));
            }

            builder.setContentText("");

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to", "home");
            intent.putExtra("notification_id", notificationId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    notificationId + 50,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.setContentIntent(pi);
        }

        /*
        ================= RESULT =================
        */
        else if ("result_hallticket".equals(type) || "result_hallticket_update".equals(type)) {

            if (icon != null) builder.setLargeIcon(icon);

            if (banner != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(banner)
                        .setBigContentTitle(title)
                        .setSummaryText(body));
            }

            // ❌ REMOVE GREY TEXT
            builder.setContentText("");

            // 🔥 SEND FULL DATA
            String resultJson = new Gson().toJson(data);

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to", "home");
            intent.putExtra("result_notification", "true");
            intent.putExtra("result_data", resultJson);
            intent.putExtra("notification_id", notificationId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    notificationId + 40,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // ✅ CLICK ACTION
            builder.setContentIntent(pi);

            // ✅ BUTTON
            builder.addAction(0, "View Details 📄", pi);
        }

        /*
        ================= NEWS =================
        */
        else if ("news".equals(type)) {

            if (banner != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(banner)
                        .setBigContentTitle(title));
            }

            // ✅ CLICK ACTION (CRITICAL FIX)
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to", "news_details");
            intent.putExtra("news_id", data.get("id"));
            intent.putExtra("notification_id", notificationId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    notificationId + 60,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.setContentIntent(pi);
        }

        /*
        ================= JOB UPDATE =================
        */
        else if ("job_update".equals(type)) {

            // 🔥 Ensure banner fallback
            if (banner == null && data.get("image_url") != null) {
                banner = getBitmap(data.get("image_url"));
            }

            // 🔥 Show BIG banner always if available
            if (banner != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(banner)
                        .setBigContentTitle(title)
                        .setSummaryText(body));
            }

            // 🔥 Fallback icon (very important UX)
            if (icon != null) {
                builder.setLargeIcon(icon);
            }

            JobUpdate job = createJobFromData(data);
            String jobJson = new Gson().toJson(job);

            // OPEN
            Intent openIntent = new Intent(this, MainActivity.class);
            openIntent.putExtra("navigate_to", "job_details");
            openIntent.putExtra("job_data", jobJson);
            openIntent.putExtra("notification_id", notificationId);

            PendingIntent openPI = PendingIntent.getActivity(
                    this, notificationId + 1, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // SAVE
            Intent saveIntent = new Intent(this, MainActivity.class);
            saveIntent.putExtra("navigate_to", "save_job");
            saveIntent.putExtra("job_data", jobJson);
            saveIntent.putExtra("notification_id", notificationId);

            PendingIntent savePI = PendingIntent.getActivity(
                    this, notificationId + 2, saveIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.setContentIntent(openPI);
            builder.addAction(0, "Save 😇", savePI);
            builder.addAction(0, "Open Post 🚀", openPI);
        }

        /*
        ================= CURRENT AFFAIRS =================
        */
        else if ("current_affairs".equals(type) || "current_affair".equals(type)) {

            if (banner != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(banner)
                        .setBigContentTitle(title));
            }

            String pdfUrl = data.get("pdf_url");

// 🔥 FIX localhost issue (IMPORTANT)
            if (pdfUrl != null && pdfUrl.contains("localhost")) {
                pdfUrl = pdfUrl.replace("http://localhost:3000", "http://test.todaybharti.in");
            }

            Intent pdfIntent = new Intent(this, MainActivity.class);
            pdfIntent.putExtra("navigate_to", "pdf_navigation");
            pdfIntent.putExtra("pdf_url", pdfUrl);
            pdfIntent.putExtra("notification_id", notificationId);
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pdfPI = PendingIntent.getActivity(
                    this,
                    notificationId + 10,
                    pdfIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

// Default click
            builder.setContentIntent(pdfPI);

// Button
            builder.addAction(0, "PDF ओपन करा 🎯", pdfPI);
        }

        /*
        ================= STUDENT UPDATE =================
        */
        else if ("student_update".equals(type)) {

            if (banner != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(banner)
                        .setBigContentTitle(title)
                        .setSummaryText(body));
            }

            // 🔥 REMOVE GREY TEXT
            builder.setContentText("");

            // 🔥 SEND FULL DATA
            String studentJson = new Gson().toJson(data);

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to", "student_update_details");
            intent.putExtra("student_data", studentJson);
            intent.putExtra("notification_id", notificationId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    notificationId + 30,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.setContentIntent(pi);
            builder.addAction(0, "View Details 🎓", pi);
        }

        /*
        ================= STUDY MATERIAL =================
        */
        else if ("study_material".equals(type)) {

            String pdfUrl = data.get("pdf_url");

// 🔥 FIX localhost issue (IMPORTANT)
            if (pdfUrl != null && pdfUrl.contains("localhost")) {
                pdfUrl = pdfUrl.replace("http://localhost:3000", "http://test.todaybharti.in");
            }

            Intent pdfIntent = new Intent(this, MainActivity.class);
            pdfIntent.putExtra("navigate_to", "pdf_navigation");
            pdfIntent.putExtra("pdf_url", pdfUrl);
            pdfIntent.putExtra("notification_id", notificationId);
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pdfPI = PendingIntent.getActivity(
                    this,
                    notificationId + 10,
                    pdfIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

// Default click
            builder.setContentIntent(pdfPI);
            builder.addAction(0, "PDF ओपन करा 🎯", pdfPI);
        }

        /*
        ================= CAREER ROADMAP =================
        */
        else if ("career_roadmap".equals(type)) {

            if (banner != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(banner)
                        .setBigContentTitle(title)
                        .setSummaryText(body)); // ✅ moved here
            }

            // ❌ REMOVE GREY TEXT
            builder.setContentText("");

            String pdfUrl = data.get("pdf_url");

            if (pdfUrl != null && pdfUrl.contains("localhost")) {
                pdfUrl = pdfUrl.replace("http://localhost:3000", "http://test.todaybharti.in");
            }

            Intent pdfIntent = new Intent(this, MainActivity.class);
            pdfIntent.putExtra("navigate_to", "pdf_navigation");
            pdfIntent.putExtra("pdf_url", pdfUrl);
            pdfIntent.putExtra("notification_id", notificationId);
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pdfPI = PendingIntent.getActivity(
                    this,
                    notificationId + 20,
                    pdfIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.setContentIntent(pdfPI);
            builder.addAction(0, "संपूर्ण रोडमॅप पहा 🔖", pdfPI);
        }

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        createChannel(manager, channelId, channelName, soundUri);

        manager.notify(notificationId, builder.build());
    }

    private PendingIntent getDefaultIntent(int notificationId) {

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /*
    ================= JOB PARSER =================
    */
    private JobUpdate createJobFromData(Map<String, String> data) {

        JobUpdate job = new JobUpdate();

        job.setDocumentId(data.get("id"));
        job.setPostName(data.get("post_name")); // FIX

        job.setTitle(data.get("title"));
        job.setSalary(data.get("salary"));
        job.setLastDateString(data.get("last_date"));

        job.setAgeRequirement(data.get("age_requirement"));
        job.setJobPlace(data.get("job_place"));
        job.setApplicationFees(data.get("application_fees"));
        job.setApplicationLink(data.get("application_link"));

        job.setType(data.get("job_type"));
        job.setSubType(data.get("sub_type"));

        job.setEducationRequirement(data.get("education_requirement"));
        job.setTotalPosts(data.get("total_posts"));
        job.setNote(data.get("note"));

        job.setIconUrl(data.get("icon_url"));
        job.setImageUrl(data.get("image_url"));

        job.setNotificationPdfLink(data.get("pdf_url"));
        job.setSelectionPdfLink(data.get("selection_pdf_url"));
        job.setSyllabusPdf(data.get("syllabus_pdf_url"));

        job.setEducationCategories(parseJsonArray(data.get("education_categories")));
        job.setBachelorDegrees(parseJsonArray(data.get("bachelor_degrees")));
        job.setMastersDegrees(parseJsonArray(data.get("masters_degrees")));

        return job;
    }

    private List<String> parseJsonArray(String raw) {

        List<String> list = new ArrayList<>();

        if (raw == null || raw.isEmpty()) return list;

        try {
            String cleaned = raw.replace("\\", "");
            JSONArray arr = new JSONArray(cleaned);

            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }

        } catch (Exception e) {
            Log.e(TAG, "Array parse error", e);
        }

        return list;
    }

    private int getSound(String type) {
        switch (type) {
            case "job_update":
            case "student_update":
                return R.raw.job_notification;
            case "career_roadmap":
                return R.raw.career_notification;
            case "result_hallticket":
                return R.raw.result_notification;
            case "current_affairs":
            case "study_material":
                return R.raw.current_notification;
            default:
                return R.raw.news_notification;
        }
    }

    private String getChannelName(String type) {
        switch (type) {
            case "job_update":
            case "student_update":
                return "Job & Student Updates";
            case "career_roadmap":
                return "Career Roadmaps";
            case "current_affairs":
                return "Current Affairs";
            case "result_hallticket":
                return "Results & Hall Tickets";
            case "study_material":
                return "Study Materials";
            case "slider":
                return "Promotions";
            case "story":
                return "Stories";
            default:
                return "Latest Updates";
        }
    }

    private void createChannel(NotificationManager manager, String id, String name, Uri sound) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(id, name,
                            NotificationManager.IMPORTANCE_HIGH);

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            channel.setSound(sound, attrs);

            manager.createNotificationChannel(channel);
        }
    }

    private Bitmap getBitmap(String urlStr) {

        if (urlStr == null || urlStr.isEmpty()) return null;

        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);

        } catch (Exception e) {
            Log.e(TAG, "Image load failed", e);
            return null;
        }
    }
}