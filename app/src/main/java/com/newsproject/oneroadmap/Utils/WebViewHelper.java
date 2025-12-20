package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.newsproject.oneroadmap.Fragments.WebViewFragment;
import com.newsproject.oneroadmap.R;

public class WebViewHelper {
    
    /**
     * Opens a URL in the app's internal WebView
     * Works from both Fragment and Activity contexts
     */
    public static void openUrlInApp(Context context, String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        // Ensure URL is properly formatted
        String fullUrl = buildFullUrl(url);
        if (fullUrl == null) {
            return;
        }
        
        FragmentManager fragmentManager = null;
        
        // Try to get FragmentManager from FragmentActivity
        if (context instanceof FragmentActivity) {
            fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
        }
        
        if (fragmentManager == null) {
            // If context is not a FragmentActivity, try to get it from the current activity
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                if (activity instanceof FragmentActivity) {
                    fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
                }
            }
        }
        
        if (fragmentManager != null) {
            WebViewFragment webViewFragment = WebViewFragment.newInstance(fullUrl);
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_up,      // enter
                    R.anim.fade_out,         // exit
                    R.anim.fade_in,          // popEnter
                    R.anim.slide_out_down    // popExit
            );
            transaction.replace(R.id.fragment_container, webViewFragment);
            transaction.addToBackStack("web_view");
            transaction.commit();
        }
    }
    
    /**
     * Opens a URL in the app's internal WebView from a Fragment
     */
    public static void openUrlInApp(Fragment fragment, String url) {
        if (url == null || url.isEmpty() || fragment == null) {
            return;
        }
        
        // Ensure URL is properly formatted
        String fullUrl = buildFullUrl(url);
        if (fullUrl == null) {
            return;
        }
        
        WebViewFragment webViewFragment = WebViewFragment.newInstance(fullUrl);
        FragmentManager fragmentManager = fragment.getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_up,      // enter
                R.anim.fade_out,         // exit
                R.anim.fade_in,          // popEnter
                R.anim.slide_out_down    // popExit
        );
        transaction.replace(R.id.fragment_container, webViewFragment);
        transaction.addToBackStack("web_view");
        transaction.commit();
    }
    
    /**
     * Builds full URL from relative path
     */
    private static String buildFullUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        // If already a full URL, return as is
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }
        // Otherwise, prepend base URL
        String url = BuildConfig.BASE_URL + filePath;
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        return url;
    }
}

