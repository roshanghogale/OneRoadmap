package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;

/**
 * Utility class to handle sharing to Instagram.
 * This class is strictly text-only to ensure Instagram displays the content correctly.
 * All links are routed through the /job deep link path to ensure the app opens.
 */
public class ShareHelper {
    private final Context context;
    private ActivityResultLauncher<Intent> shareLauncher;

    // Use /job as the base to ensure the app opens via deep link even for generic shares
    private static final String DEFAULT_DEEP_LINK = "https://mahaalert.in/job";

    private static final String STANDARD_SHARE_MESSAGE =
            "महाराष्ट्र व केंद्र शासनाच्या\n\n" +
            "सर्व सरकारी जॉब - ची माहिती सरळ तुमच्या स्मार्ट फोनवर मिळवा 👇👇\n\n" +
            DEFAULT_DEEP_LINK;

    public ShareHelper(Context context) {
        this.context = context;
    }

    public void setShareLauncher(ActivityResultLauncher<Intent> launcher) {
        this.shareLauncher = launcher;
    }

    /**
     * Shares a job update with its title and a specific deep link.
     */
    public void shareJobWithImage(String title, String jobId, String imageUrl) {
        String message;
        
        if (jobId != null && !jobId.isEmpty()) {
            // Job Specific Share with ID
            String deepLink = DEFAULT_DEEP_LINK + "?id=" + jobId;
            message = title + "\n\n" +
                      "महाराष्ट्र व केंद्र शासनाच्या सर्व सरकारी जॉब माहिती 👇👇\n\n" +
                      deepLink;
        } else {
            // Fallback for News or Generic Share with a Title
            message = title + "\n\n" + STANDARD_SHARE_MESSAGE;
        }

        shareTextOnly(message);
    }

    /**
     * Legacy method for compatibility. Now forces text-only sharing.
     */
    public void shareWithStandardImage(String text) {
        shareTextOnly(text);
    }

    /**
     * Generic share method. Appends standard promo and link.
     */
    public void sharePost(String title, String url) {
        if (title != null && !title.isEmpty()) {
            shareTextOnly(title + "\n\n" + STANDARD_SHARE_MESSAGE);
        } else {
            shareStandardMessageOnly();
        }
    }

    /**
     * Strictly shares only the standard promotional message and app deep link.
     * Use this for Profile, Stories, and Coin dialogs.
     */
    public void shareStandardMessageOnly() {
        shareTextOnly(STANDARD_SHARE_MESSAGE);
    }

    /**
     * The core sharing method. Sends a text-only intent to Instagram.
     */
    private void shareTextOnly(String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setPackage("com.instagram.android");
            
            if (shareLauncher != null) {
                shareLauncher.launch(intent);
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Instagram is not installed", Toast.LENGTH_SHORT).show();
        }
    }
}
