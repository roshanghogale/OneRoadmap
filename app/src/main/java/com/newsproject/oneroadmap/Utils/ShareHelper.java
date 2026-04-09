package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;

public class ShareHelper {
    private final Context context;
    private ActivityResultLauncher<Intent> shareLauncher;
    
    public ShareHelper(Context context) {
        this.context = context;
    }
    
    public void setShareLauncher(ActivityResultLauncher<Intent> launcher) {
        this.shareLauncher = launcher;
    }
    
    private static final String STANDARD_SHARE_MESSAGE = 
            "महाराष्ट्र व केंद्र शासनाच्या\n\nसर्व सरकारी जॉब - ची माहिती सरळ तुमच्या स्मार्ट फोनवर मिळवा 👇👇\n\nhttps://mahaalert.in/";

    public void shareJobWithImage(String title, String jobId, String imageUrl) {

        String deepLink = "https://mahaalert.in/job?id=" + jobId;

        String message =
                "महाराष्ट्र व केंद्र शासनाच्या\n\n" +
                        "सर्व सरकारी जॉब माहिती 👇👇\n\n" +
                        deepLink;

        shareWithStandardImage(message);
    }

    public void shareWithStandardImage(String text) {
        try {
            android.content.res.Resources res = context.getResources();
            int resId = res.getIdentifier("share_demo", "drawable", context.getPackageName());

            if (resId != 0) {
                Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(res, resId);
                if (bitmap != null) {
                    shareImageWithText(bitmap, text);
                    return;
                }
            }
            shareTextOnly(text);
        } catch (Exception e) {
            shareTextOnly(text);
        }
    }
    
    private void shareImageWithText(Bitmap bitmap, String text) {
        try {
            File cacheDir = new File(context.getCacheDir(), "shared_images");
            cacheDir.mkdirs();
            File imageFile = new File(cacheDir, "job_banner_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            out.flush();
            out.close();
            
            Uri imageUri = FileProvider.getUriForFile(
                    context,
                    "com.newsproject.oneroadmap.fileprovider",
                    imageFile
            );
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setPackage("com.whatsapp"); // DIRECT WHATSAPP
            
            launchShare(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "WhatsApp not installed or error", Toast.LENGTH_SHORT).show();
            shareTextOnly(text);
        }
    }
    
    private void shareTextOnly(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setPackage("com.whatsapp"); // DIRECT WHATSAPP
        launchShare(intent);
    }

    private void launchShare(Intent intent) {
        try {
            if (shareLauncher != null) {
                shareLauncher.launch(intent);
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void sharePost(String title, String url) {
        shareWithStandardImage(title);
    }
}
