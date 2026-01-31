package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.io.File;
import java.io.FileOutputStream;

public class ShareHelper {
    private final Context context;
    private final CoinManager coinManager;
    private final String userId;
    private ActivityResultLauncher<Intent> shareLauncher;
    
    public ShareHelper(Context context) {
        this.context = context;
        android.content.SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        this.userId = prefs.getString("userId", "");
        if (userId != null && !userId.isEmpty()) {
            this.coinManager = new CoinManager(context, userId);
        } else {
            this.coinManager = null;
        }
    }
    
    public void setShareLauncher(ActivityResultLauncher<Intent> launcher) {
        this.shareLauncher = launcher;
    }
    
    // Standard share message and link for all shares
    private static final String STANDARD_SHARE_MESSAGE = 
            "महाराष्ट्र व केंद्र शासनाच्या\n\nसर्व सरकारी जॉब - ची माहिती सरळ तुमच्या स्मार्ट फोनवर मिळवा 👇👇\n\nhttps://mahaalert.in/";
    
    /**
     * Main method — call this to share job with banner image
     * Always uses standard message, share_demo image, and mahaalert.in link
     */
    public void shareJobWithImage(String title, String url, String imageUrl) {
        // Always use standard message and share_demo image
        shareWithStandardImage();
    }
    
    /**
     * Share with standard share_demo image from drawables
     */
    private void shareWithStandardImage() {
        try {
            // Load share_demo image from drawables
            android.content.res.Resources res = context.getResources();
            int resId = res.getIdentifier("share_demo", "drawable", context.getPackageName());
            
            if (resId != 0) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(res, resId);
                if (bitmap != null) {
                    shareImageWithText(bitmap, STANDARD_SHARE_MESSAGE);
                    return;
                }
            }
            
            // Fallback to text only if image not found
            shareTextOnly(STANDARD_SHARE_MESSAGE);
        } catch (Exception e) {
            // Fallback to text only on error
            shareTextOnly(STANDARD_SHARE_MESSAGE);
        }
    }
    
    private void shareImageWithText(Bitmap bitmap, String text) {
        try {
            // Save bitmap to cache
            File cacheDir = new File(context.getCacheDir(), "shared_images");
            cacheDir.mkdirs();
            File imageFile = new File(cacheDir, "job_banner_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            out.flush();
            out.close();
            
            // Get URI via FileProvider
            Uri imageUri = FileProvider.getUriForFile(
                    context,
                    "com.newsproject.oneroadmap.fileprovider",
                    imageFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            // Put text first, then image - some apps (like WhatsApp) need text before stream
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Only use WhatsApp - set package explicitly
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("image/*");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, text);
            whatsappIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            whatsappIntent.setPackage("com.whatsapp");
            
            if (shareLauncher != null) {
                shareLauncher.launch(whatsappIntent);
            } else {
                context.startActivity(whatsappIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show();
            shareTextOnly(text);
        }
    }
    
    private void shareTextOnly(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.whatsapp");
        
        if (shareLauncher != null) {
            shareLauncher.launch(intent);
        } else {
            context.startActivity(intent);
        }
    }
    
    /**
     * Legacy method for backward compatibility
     * Always uses standard message, share_demo image, and mahaalert.in link
     */
    public void sharePost(String title, String url) {
        shareWithStandardImage();
    }
    
    /**
     * Call this when user returns from sharing (to give coins)
     */
    public void onShareCompleted() {
        if (coinManager != null) {
            coinManager.addCoinsForShare(newCoins -> {
                // Coins added successfully
            });
        }
    }
    
    // Method to be called from Activity.onActivityResult when requestCode is 100
    public static void handleShareResult(Context context, int requestCode, int resultCode) {
        if (requestCode == 100) {
            ShareHelper helper = new ShareHelper(context);
            helper.onShareCompleted();
        }
    }
}
