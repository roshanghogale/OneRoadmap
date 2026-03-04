package com.newsproject.oneroadmap.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM_DEBUG";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        
        if (data.size() > 0) {
            String type = data.get("notificationType");
            if (type == null) type = data.get("type");
            if (type != null) type = type.toLowerCase();
            
            if (type == null) {
                String titleText = data.get("title") != null ? data.get("title").toLowerCase() : "";
                if (titleText.contains("upsc") || titleText.contains("exam") || titleText.contains("job")) {
                    type = "job_update";
                } else {
                    type = "news";
                }
            }

            String title = data.get("title");
            String body = data.get("body");
            
            String bannerUrl = data.get("image_url");
            if (bannerUrl == null) bannerUrl = data.get("imageUrl");
            if (bannerUrl == null) bannerUrl = data.get("image");
            
            String iconUrl = data.get("icon_url");
            if (iconUrl == null) iconUrl = data.get("iconUrl");

            int soundResId = getSoundForType(type);
            String channelId = "channel_" + type;
            String channelName = getChannelNameForType(type);

            Bitmap bannerBitmap = (bannerUrl != null && !bannerUrl.isEmpty()) ? getBitmapFromUrl(bannerUrl) : null;
            Bitmap iconBitmap = (iconUrl != null && !iconUrl.isEmpty()) ? getBitmapFromUrl(iconUrl) : null;

            sendNotification(title, body, bannerBitmap, iconBitmap, channelId, channelName, soundResId, type, data);
        }
    }

    private int getSoundForType(String type) {
        if (type == null) return R.raw.news_notification;
        switch (type) {
            case "job_update":
            case "student_update": return R.raw.job_notification;
            case "career_roadmap": return R.raw.career_notification;
            case "result_hallticket": return R.raw.result_notification;
            case "current_affairs":
            case "study_material": return R.raw.current_notification;
            default: return R.raw.news_notification;
        }
    }

    private String getChannelNameForType(String type) {
        if (type == null) return "General Updates";
        switch (type) {
            case "job_update":
            case "student_update": return "Job & Student Updates";
            case "career_roadmap": return "Career Roadmaps";
            case "current_affairs": return "Current Affairs";
            case "result_hallticket": return "Results & Hall Tickets";
            default: return "Latest News";
        }
    }

    private void sendNotification(String title, String body, Bitmap banner, Bitmap icon, String channelId, String channelName, int soundResId, String type, Map<String, String> data) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        boolean isJobType = "job_update".equals(type) || "student_update".equals(type);
        
        // Pass data to MainActivity for both tap and button click
        if (isJobType) {
            mainIntent.putExtra("navigate_to", "job_details");
            mainIntent.putExtra("job_data", new Gson().toJson(createJobFromData(data)));
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), mainIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + soundResId);
        
        String contentText = body;
        if (isJobType && data.containsKey("education_requirement")) {
            contentText = data.get("education_requirement");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (icon != null) builder.setLargeIcon(icon);

        if (banner != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(banner)
                    .setBigContentTitle(title)
                    .setSummaryText(contentText));
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
        }

        if (isJobType) {
            int notificationId = (int) System.currentTimeMillis();
            
            // 1. Save Action
            JobUpdate job = createJobFromData(data);
            String jobJson = new Gson().toJson(job);
            Intent saveIntent = new Intent(this, NotificationActionReceiver.class);
            saveIntent.setAction(NotificationActionReceiver.ACTION_SAVE_JOB);
            saveIntent.putExtra(NotificationActionReceiver.EXTRA_JOB_DATA, jobJson);
            saveIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent savePI = PendingIntent.getBroadcast(this, notificationId + 1, saveIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, "Save 😇", savePI);

            // 2. Open Post Action -> Now opens JobUpdateDetails over HomeFragment
            Intent openIntent = new Intent(this, MainActivity.class);
            openIntent.putExtra("navigate_to", "job_details");
            openIntent.putExtra("job_data", jobJson);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openPI = PendingIntent.getActivity(this, notificationId + 2, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, "Open Post 🚀", openPI);
            
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            setupChannel(manager, channelId, channelName, soundUri);
            manager.notify(notificationId, builder.build());
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            setupChannel(manager, channelId, channelName, soundUri);
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void setupChannel(NotificationManager manager, String id, String name, Uri sound) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
            channel.setSound(sound, attrs);
            manager.createNotificationChannel(channel);
        }
    }

    private JobUpdate createJobFromData(Map<String, String> data) {
        JobUpdate job = new JobUpdate();
        job.setTitle(data.get("title"));
        job.setDocumentId(data.get("id"));
        job.setEducationRequirement(data.get("education_requirement"));
        String img = data.get("image_url");
        if (img == null) img = data.get("imageUrl");
        if (img == null) img = data.get("image");
        job.setImageUrl(img);
        job.setApplicationLink(data.get("application_link"));
        job.setSalary(data.get("salary"));
        job.setLastDateString(data.get("last_date"));
        job.setPostName(data.get("post_name"));
        job.setJobPlace(data.get("job_place"));
        job.setTotalPosts(data.get("total_posts"));
        job.setAgeRequirement(data.get("age_requirement"));
        job.setApplicationFees(data.get("application_fees"));
        job.setNote(data.get("note"));
        return job;
    }

    public Bitmap getBitmapFromUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            return null;
        }
    }
}
