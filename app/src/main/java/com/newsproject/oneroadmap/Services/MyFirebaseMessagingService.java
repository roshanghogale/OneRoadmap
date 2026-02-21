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
import com.newsproject.oneroadmap.Activities.MainActivity;
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
            Log.d(TAG, "--------------------------------------------------");
            Log.d(TAG, "NEW NOTIFICATION RECEIVED");
            for (Map.Entry<String, String> entry : data.entrySet()) {
                Log.d(TAG, "Key: " + entry.getKey() + " | Value: " + entry.getValue());
            }
            Log.d(TAG, "--------------------------------------------------");

            // 1. Try to get type from 'notificationType' or 'type' keys
            String type = data.get("notificationType");
            if (type == null) {
                type = data.get("type");
            }
            
            // 2. If type is still null, guess from title as a fallback
            if (type == null) {
                String title = data.get("title") != null ? data.get("title").toLowerCase() : "";
                if (title.contains("upsc") || title.contains("exam")) {
                    type = "student_update";
                } else if (title.contains("roadmap") || title.contains("path")) {
                    type = "career_roadmap";
                } else if (title.contains("current affairs")) {
                    type = "current_affairs";
                } else if (title.contains("result") || title.contains("ssc")) {
                    type = "result_hallticket";
                } else if (title.contains("story")) {
                    type = "story";
                } else if (title.contains("news") || title.contains("job")) {
                    type = "news";
                }
            }

            String title = data.get("title");
            String body = data.get("body");
            
            // 3. Check for 'image_url', 'imageUrl', and 'image' keys
            String imageUrl = data.get("image_url");
            if (imageUrl == null || imageUrl.isEmpty()) imageUrl = data.get("imageUrl");
            if (imageUrl == null || imageUrl.isEmpty()) imageUrl = data.get("image"); 

            int soundResId = getSoundForType(type);
            String channelId = "channel_" + (type != null ? type : "default");
            String channelName = getChannelNameForType(type);

            Log.d(TAG, "Final Processed Type: " + type + " | Sound: " + getResources().getResourceEntryName(soundResId));

            Bitmap image = (imageUrl != null && !imageUrl.isEmpty()) ? getBitmapFromUrl(imageUrl) : null;
            sendNotification(title, body, image, channelId, channelName, soundResId);
        }
    }

    private int getSoundForType(String type) {
        if (type == null) return R.raw.news_notification;
        switch (type) {
            case "student_update":
                return R.raw.job_notification;
            case "slider":
            case "story":
            case "current_affairs":
            case "study_material":
                return R.raw.current_notification;
            case "career_roadmap":
                return R.raw.career_notification;
            case "result_hallticket":
                return R.raw.result_notification;
            case "news":
            case "general":
            default:
                return R.raw.news_notification;
        }
    }

    private String getChannelNameForType(String type) {
        if (type == null) return "General Updates";
        switch (type) {
            case "student_update": return "Job & Student Updates";
            case "career_roadmap": return "Career Roadmaps";
            case "current_affairs": return "Current Affairs";
            case "result_hallticket": return "Results & Hall Tickets";
            case "news": return "Latest News";
            case "study_material": return "Study Materials";
            case "story": return "Stories";
            case "slider": return "Promotions";
            default: return "App Updates";
        }
    }

    private void sendNotification(String title, String body, Bitmap image, String channelId, String channelName, int soundResId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + soundResId);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (image != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(image).bigLargeIcon((Bitmap) null));
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
            channel.setSound(soundUri, audioAttributes);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    public Bitmap getBitmapFromUrl(String imageUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching image: " + imageUrl, e);
            return null;
        }
    }
}