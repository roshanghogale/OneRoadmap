package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import java.util.Arrays;
import java.util.List;

/**
 * Shares job/news content via WhatsApp or Instagram.
 */
public class ShareHelper {
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b";
    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private static final String INSTAGRAM_LITE_PACKAGE = "com.instagram.lite";

    private static final List<String> WHATSAPP_PACKAGES = Arrays.asList(
            WHATSAPP_PACKAGE,
            WHATSAPP_BUSINESS_PACKAGE
    );

    private static final List<String> INSTAGRAM_PACKAGES = Arrays.asList(
            INSTAGRAM_PACKAGE,
            INSTAGRAM_LITE_PACKAGE
    );

    private static final String DEFAULT_DEEP_LINK = "https://mahaalert.in/job";

    private static final String STANDARD_SHARE_MESSAGE =
            "महाराष्ट्र व केंद्र शासनाच्या\n\n" +
            "सर्व सरकारी जॉब - ची माहिती सरळ तुमच्या स्मार्ट फोनवर मिळवा 👇👇\n\n" +
            DEFAULT_DEEP_LINK;

    private final Context context;
    private ActivityResultLauncher<Intent> shareLauncher;

    public ShareHelper(Context context) {
        this.context = context;
    }

    public void setShareLauncher(ActivityResultLauncher<Intent> launcher) {
        this.shareLauncher = launcher;
    }

    public boolean shareJobWithImage(String title, String jobId, String imageUrl) {
        return shareViaWhatsAppOrInstagram(buildJobShareMessage(title, jobId));
    }

    public String buildJobShareMessage(String title, String jobId) {
        if (jobId != null && !jobId.isEmpty()) {
            String deepLink = DEFAULT_DEEP_LINK + "?id=" + jobId;
            return title + "\n\n" +
                    "महाराष्ट्र व केंद्र शासनाच्या सर्व सरकारी जॉब माहिती 👇👇\n\n" +
                    deepLink;
        }
        return title + "\n\n" + STANDARD_SHARE_MESSAGE;
    }

    public boolean isWhatsAppInstalled() {
        return resolveSharePackage(WHATSAPP_PACKAGES) != null;
    }

    public boolean isInstagramInstalled() {
        return resolveSharePackage(INSTAGRAM_PACKAGES) != null;
    }

    public boolean shareToWhatsApp(String message) {
        String packageName = resolveSharePackage(WHATSAPP_PACKAGES);
        if (packageName == null) {
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
            return false;
        }
        return launchShare(message, packageName);
    }

    public boolean shareToInstagram(String message) {
        String packageName = resolveSharePackage(INSTAGRAM_PACKAGES);
        if (packageName == null) {
            Toast.makeText(context, "Instagram is not installed", Toast.LENGTH_SHORT).show();
            return false;
        }
        return launchShare(message, packageName);
    }

    public boolean shareViaWhatsAppOrInstagram(String message) {
        if (isWhatsAppInstalled()) {
            return shareToWhatsApp(message);
        }
        if (isInstagramInstalled()) {
            return shareToInstagram(message);
        }
        Toast.makeText(context, "WhatsApp or Instagram is not installed", Toast.LENGTH_SHORT).show();
        return false;
    }

    public void shareWithStandardImage(String text) {
        shareViaWhatsAppOrInstagram(text);
    }

    public void sharePost(String title, String url) {
        if (title != null && !title.isEmpty()) {
            shareViaWhatsAppOrInstagram(title + "\n\n" + STANDARD_SHARE_MESSAGE);
        } else {
            shareStandardMessageOnly();
        }
    }

    public void shareStandardMessageOnly() {
        shareViaWhatsAppOrInstagram(STANDARD_SHARE_MESSAGE);
    }

    private String resolveSharePackage(List<String> packageNames) {
        PackageManager pm = context.getPackageManager();
        for (String packageName : packageNames) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage(packageName);
            ResolveInfo info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                return packageName;
            }
        }
        return null;
    }

    private boolean launchShare(String message, String packageName) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setPackage(packageName);

        try {
            if (shareLauncher != null) {
                shareLauncher.launch(intent);
            } else {
                context.startActivity(intent);
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open app", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
