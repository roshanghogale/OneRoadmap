package com.newsproject.oneroadmap.Services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.gson.Gson;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.database.SavedJobsDatabaseHelper;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_SAVE_JOB = "com.newsproject.oneroadmap.ACTION_SAVE_JOB";
    public static final String EXTRA_JOB_DATA = "extra_job_data";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);

        if (ACTION_SAVE_JOB.equals(action)) {
            String jobJson = intent.getStringExtra(EXTRA_JOB_DATA);
            if (jobJson != null) {
                try {
                    JobUpdate job = new Gson().fromJson(jobJson, JobUpdate.class);
                    SavedJobsDatabaseHelper dbHelper = new SavedJobsDatabaseHelper(context);
                    dbHelper.saveJob(job);
                    Toast.makeText(context, "Job Saved Successfully! 😇", Toast.LENGTH_SHORT).show();
                    
                    // Dismiss notification after saving
                    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationId != -1) {
                        manager.cancel(notificationId);
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "Failed to save job", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
