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
import com.newsproject.oneroadmap.Models.StudentUpdateItem;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.BuildConfig;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_SERVICE";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "FCM received from: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();

        if (data == null || data.isEmpty()) return;

        Log.d(TAG, "Payload: " + data.toString());

        String type = data.get("type");
        if (type == null) type = "news";
        type = type.toLowerCase();

        String title = data.get("title");
        String body = data.get("body");

        if (body == null || body.isEmpty()) {
            body = "Tap to view update";
        }

        String bannerUrl = resolveBannerUrl(data);
        String iconUrl = data.get("icon_url");

        int soundRes = getSoundForType(type);
        String channelId = "channel_" + type;
        String channelName = getChannelName(type);

        String finalType = type;
        String finalBannerUrl = bannerUrl;
        String finalIconUrl = iconUrl;
        String finalTitle = title;
        String finalBody = body;

        new Thread(() -> {

            Bitmap bannerBitmap = null;
            Bitmap iconBitmap = null;

            try {

                if (finalBannerUrl != null) {
                    Log.d(TAG, "Downloading banner: " + finalBannerUrl);
                    bannerBitmap = getBitmapFromUrl(finalBannerUrl);
                }

                if (finalIconUrl != null) {
                    iconBitmap = getBitmapFromUrl(finalIconUrl);
                }

            } catch (Exception e) {
                Log.e(TAG, "Image loading failed", e);
            }

            sendNotification(
                    finalTitle,
                    finalBody,
                    bannerBitmap,
                    iconBitmap,
                    channelId,
                    channelName,
                    soundRes,
                    finalType,
                    data
            );

        }).start();
    }

    private String resolveBannerUrl(Map<String, String> data) {

        String banner = data.get("banner_url");

        if (banner == null || banner.isEmpty())
            banner = data.get("image_url");

        if (banner == null || banner.isEmpty())
            banner = data.get("image");

        if (banner != null && !banner.startsWith("http")) {
            banner = BuildConfig.BASE_URL + banner;
        }

        Log.d(TAG, "Resolved banner URL: " + banner);

        return banner;
    }

    private int getSoundForType(String type) {

        switch (type) {

            case "job_update":
            case "student_update":
                return R.raw.job_notification;

            case "career_roadmap":
                return R.raw.career_notification;

            case "result_hallticket":
                return R.raw.result_notification;

            case "current_affairs":
            case "current_affair":
            case "study_material":
                return R.raw.current_notification;

            case "slider":
            case "story":
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

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("notification_id", notificationId);

        if ("job_update".equals(type)) {

            intent.putExtra("navigate_to", "job_details");
            intent.putExtra("job_data", new Gson().toJson(createJobFromData(data)));

        } else if ("story".equals(type)) {

            intent.putExtra("navigate_to", "home");

        } else if ("slider".equals(type)) {

            intent.putExtra("navigate_to", "home");
            intent.putExtra("slider_post_id", data.get("post_document_id"));

        } else if ("current_affairs".equals(type) || "study_material".equals(type)) {

            intent.putExtra("navigate_to", "pdf_navigation");
            intent.putExtra("pdf_url", data.get("pdf_url"));

        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri soundUri = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE +
                        "://" + getPackageName() +
                        "/" + soundRes
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.app_logo)
                        .setContentTitle(title)
                        .setSound(soundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

// ⭐ Only add description if NOT slider
        if (!"slider".equals(type)) {
            builder.setContentText(body);
        }

        if (icon != null) builder.setLargeIcon(icon);

        if (banner != null) {

            NotificationCompat.BigPictureStyle style =
                    new NotificationCompat.BigPictureStyle()
                            .bigPicture(banner)
                            .setBigContentTitle(title);

            if (!"slider".equals(type)) {
                style.setSummaryText(body);
            }

            builder.setStyle(style);

        } else {

            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        createChannel(manager, channelId, channelName, soundUri);

        manager.notify(notificationId, builder.build());
    }

    private void createChannel(NotificationManager manager, String id, String name, Uri sound) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            id,
                            name,
                            NotificationManager.IMPORTANCE_HIGH
                    );

            AudioAttributes attrs =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();

            channel.setSound(sound, attrs);

            manager.createNotificationChannel(channel);
        }
    }

    private JobUpdate createJobFromData(Map<String, String> data) {

        JobUpdate job = new JobUpdate();

        job.setTitle(data.get("title"));
        job.setDocumentId(data.get("id"));
        job.setEducationRequirement(data.get("education_requirement"));
        job.setImageUrl(data.get("image_url"));

        return job;
    }

    private Bitmap getBitmapFromUrl(String urlStr) {

        try {

            URL url = new URL(urlStr);

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();

            return BitmapFactory.decodeStream(input);

        } catch (Exception e) {

            Log.e(TAG, "Bitmap download failed", e);
            return null;
        }
    }
}