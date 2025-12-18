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
        android.content.SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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
    
    /**
     * Main method — call this to share job with banner image
     */
    public void shareJobWithImage(String title, String url, String imageUrl) {
        if (title == null) title = "Latest Government Job Alert";
        if (url == null) url = "https://play.google.com/store/apps/details?id=com.newsproject.oneroadmap";
        
        String fullText = title + "\n\n" + url + "\n\nShared via One Roadmap App";
        
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            shareTextOnly(fullText);
            return;
        }
        
        // Download image using Glide and share
        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        shareImageWithText(bitmap, fullText);
                    }
                    
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // Image failed → share text only
                        shareTextOnly(fullText);
                    }
                    
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
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
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 1. Try WhatsApp
            Intent whatsappIntent = (Intent) shareIntent.clone();
            whatsappIntent.setPackage("com.whatsapp");
            try {
                if (shareLauncher != null) {
                    shareLauncher.launch(whatsappIntent);
                } else {
                    context.startActivity(whatsappIntent);
                }
                return;
            } catch (Exception e) { /* ignore */ }
            
            // 2. Try WhatsApp Business
            whatsappIntent.setPackage("com.whatsapp.w4b");
            try {
                if (shareLauncher != null) {
                    shareLauncher.launch(whatsappIntent);
                } else {
                    context.startActivity(whatsappIntent);
                }
                return;
            } catch (Exception e) { /* ignore */ }
            
            // 3. Show chooser with all apps (WhatsApp will appear at top)
            if (shareLauncher != null) {
                shareLauncher.launch(Intent.createChooser(shareIntent, "Share Job Update"));
            } else {
                context.startActivity(Intent.createChooser(shareIntent, "Share Job Update"));
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
        
        // Try WhatsApp first
        intent.setPackage("com.whatsapp");
        try {
            if (shareLauncher != null) {
                shareLauncher.launch(intent);
            } else {
                context.startActivity(intent);
            }
            return;
        } catch (Exception e) {}
        
        intent.setPackage("com.whatsapp.w4b");
        try {
            if (shareLauncher != null) {
                shareLauncher.launch(intent);
            } else {
                context.startActivity(intent);
            }
            return;
        } catch (Exception e) {}
        
        // Final fallback
        if (shareLauncher != null) {
            shareLauncher.launch(Intent.createChooser(intent, "Share via"));
        } else {
            context.startActivity(Intent.createChooser(intent, "Share via"));
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public void sharePost(String title, String url) {
        shareJobWithImage(title, url, null);
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
