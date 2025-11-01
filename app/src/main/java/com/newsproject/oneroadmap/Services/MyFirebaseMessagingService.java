package com.newsproject.oneroadmap.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";
    private static final String CHANNEL_ID = "news_notification_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Extract data payload
            String title = remoteMessage.getData().get("title");
            String description = remoteMessage.getData().get("body");
            String imageUrl = remoteMessage.getData().get("image");
            String documentId = remoteMessage.getData().get("documentId"); // Document ID from FCM

            // Log extracted values for debugging
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Description: " + description);
            Log.d(TAG, "Image URL: " + imageUrl);
            Log.d(TAG, "Document ID: " + documentId);

            // Null checks and defaults
            title = (title != null && !title.isEmpty()) ? title : "News Update";
            description = (description != null && !description.isEmpty()) ? description : "Click to view details.";

            // Append document ID to description if present
            if (documentId != null && !documentId.isEmpty()) {
                description += "\nDoc ID: " + documentId;
            }

            // Fetch the image from URL
            Bitmap image = null;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Log.d(TAG, "Fetching image from URL...");
                image = getBitmapFromFirebaseStorage(imageUrl);
            } else {
                Log.w(TAG, "Image URL is empty or null.");
            }

            // Show the custom notification
            showCustomNotification(title, description, image);
        } else {
            Log.w(TAG, "No data payload received in the message.");
        }
    }

    // Helper method to fetch an image from a Firebase Storage URL
    public Bitmap getBitmapFromFirebaseStorage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            Log.d(TAG, "Connected to image URL.");
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching image from URL: " + imageUrl, e);
            return null;
        }
    }

    // Method to show a custom notification
    private void showCustomNotification(String title, String description, Bitmap image) {
        // Create an intent to launch MainActivity when the notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification); // Small icon from resources

        // Add Big Picture Style if image is available
        if (image != null) {
            Log.d(TAG, "Adding big picture style to notification.");
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(image)
                    .bigLargeIcon((Bitmap) null)); // Remove the large icon in expanded mode
        } else {
            Log.w(TAG, "Image is null. Notification will not include a big picture style.");
        }

        // Display the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // For Android Oreo and above, create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "News Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(1, builder.build());
    }
}
