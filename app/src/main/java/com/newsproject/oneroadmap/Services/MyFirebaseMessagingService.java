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
            Log.d(TAG, "onMessageReceived Payload: " + data.toString());

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
            String channelId = "channel_" + (type != null ? type : "default");
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
            case "slider":
            case "story":
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
            case "study_material": return "Study Materials";
            case "slider": return "Promotions";
            case "story": return "Stories";
            default: return "Latest News";
        }
    }

    private void sendNotification(String title, String body, Bitmap banner, Bitmap icon, String channelId, String channelName, int soundResId, String type, Map<String, String> data) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        boolean isJobType = "job_update".equals(type);
        boolean isNewsType = "news".equals(type);
        boolean isResultType = "result_hallticket".equals(type);
        boolean isCurrentAffairsType = "current_affairs".equals(type);
        boolean isCareerRoadmapType = "career_roadmap".equals(type);
        boolean isStudyMaterialType = "study_material".equals(type);
        boolean isStudentUpdateType = "student_update".equals(type);
        boolean isHomeRedirectType = "slider".equals(type) || "story".equals(type);
        
        if (isJobType) {
            mainIntent.putExtra("navigate_to", "job_details");
            mainIntent.putExtra("job_data", new Gson().toJson(createJobFromData(data)));
        } else if (isNewsType) {
            mainIntent.putExtra("navigate_to", "news_details");
            mainIntent.putExtra("news_id", data.get("id"));
        } else if (isCurrentAffairsType || isCareerRoadmapType || isStudyMaterialType) {
            mainIntent.putExtra("navigate_to", "pdf_navigation");
            mainIntent.putExtra("pdf_url", data.get("pdf_url"));
        } else if (isStudentUpdateType) {
            mainIntent.putExtra("navigate_to", "student_update_details");
            mainIntent.putExtra("student_data", new Gson().toJson(createStudentUpdateFromData(data)));
        } else if (isHomeRedirectType || isResultType) {
            mainIntent.putExtra("navigate_to", "home");
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), mainIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + soundResId);
        
        String notificationTitle = title;
        String contentText = body;

        if (isJobType && data.containsKey("education_requirement")) {
            contentText = data.get("education_requirement");
        } else if (isCurrentAffairsType) {
            notificationTitle = "Today's Current Affair";
            if (data.containsKey("date")) {
                contentText = data.get("date");
            }
        } else if (isCareerRoadmapType) {
            if (data.containsKey("type")) {
                contentText = data.get("type");
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(notificationTitle)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (icon != null) {
            builder.setLargeIcon(icon);
        }

        if (isResultType) {
            builder.setStyle(null);
        } else if (banner != null) {
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                    .bigPicture(banner)
                    .setBigContentTitle(notificationTitle);
            
            if (isJobType || isCurrentAffairsType || isCareerRoadmapType || isStudyMaterialType || isStudentUpdateType) {
                bigPictureStyle.setSummaryText(contentText);
            }
            
            builder.setStyle(bigPictureStyle);
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
        }

        // Add Action Buttons
        if (isCurrentAffairsType || isStudyMaterialType) {
            builder.addAction(0, "PDF ओपन करा 🎯", pendingIntent);
        } else if (isCareerRoadmapType) {
            builder.addAction(0, "संपूर्ण रोडमॅप पहा 🔖", pendingIntent);
        } else if (isStudentUpdateType) {
            builder.addAction(0, "संपूर्ण माहिती पहा 🚀", pendingIntent);
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        setupChannel(manager, channelId, channelName, soundUri);

        if (isJobType) {
            int notificationId = (int) System.currentTimeMillis();
            JobUpdate job = createJobFromData(data);
            String jobJson = new Gson().toJson(job);
            Intent saveIntent = new Intent(this, NotificationActionReceiver.class);
            saveIntent.setAction(NotificationActionReceiver.ACTION_SAVE_JOB);
            saveIntent.putExtra(NotificationActionReceiver.EXTRA_JOB_DATA, jobJson);
            saveIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent savePI = PendingIntent.getBroadcast(this, notificationId + 1, saveIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, "Save 😇", savePI);

            Intent openIntent = new Intent(this, MainActivity.class);
            openIntent.putExtra("navigate_to", "job_details");
            openIntent.putExtra("job_data", jobJson);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openPI = PendingIntent.getActivity(this, notificationId + 2, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, "Open Post 🚀", openPI);
            
            manager.notify(notificationId, builder.build());
        } else {
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

    private StudentUpdateItem createStudentUpdateFromData(Map<String, String> data) {
        StudentUpdateItem item = new StudentUpdateItem();
        try { item.setId(Integer.parseInt(data.get("id"))); } catch (Exception ignored) {}
        item.setTitle(data.get("title"));
        item.setEducation(data.get("education"));
        item.setDescription(data.get("description"));
        item.setApplicationLink(data.get("application_link"));
        item.setLastDate(data.get("last_date"));
        item.setImageUrl(data.get("image_url"));
        item.setIconUrl(data.get("icon_url"));
        item.setNotificationPdfUrl(data.get("notification_pdf_url"));
        item.setSelectionPdfUrl(data.get("selection_pdf_url"));
        item.setCreatedAt(data.get("created_at"));
        return item;
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
