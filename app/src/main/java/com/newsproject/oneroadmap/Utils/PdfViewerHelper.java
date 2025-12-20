package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.newsproject.oneroadmap.Fragments.PDFViewerFragment;
import com.newsproject.oneroadmap.R;

public class PdfViewerHelper {
    
    /**
     * Opens a PDF URL in the app's internal PDF viewer
     * Works from both Fragment and Activity contexts
     */
    public static void openPdfInApp(Context context, String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            return;
        }
        
        // Ensure URL is properly formatted
        String fullUrl = buildFullUrl(pdfUrl);
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
            PDFViewerFragment pdfFragment = PDFViewerFragment.newInstance(fullUrl);
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_up,      // enter
                    R.anim.fade_out,         // exit
                    R.anim.fade_in,          // popEnter
                    R.anim.slide_out_down    // popExit
            );
            transaction.replace(R.id.fragment_container, pdfFragment);
            transaction.addToBackStack("pdf_viewer");
            transaction.commit();
        }
    }
    
    /**
     * Opens a PDF URL in the app's internal PDF viewer from a Fragment
     */
    public static void openPdfInApp(Fragment fragment, String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isEmpty() || fragment == null) {
            return;
        }
        
        // Ensure URL is properly formatted
        String fullUrl = buildFullUrl(pdfUrl);
        if (fullUrl == null) {
            return;
        }
        
        PDFViewerFragment pdfFragment = PDFViewerFragment.newInstance(fullUrl);
        FragmentManager fragmentManager = fragment.getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_up,      // enter
                R.anim.fade_out,         // exit
                R.anim.fade_in,          // popEnter
                R.anim.slide_out_down    // popExit
        );
        transaction.replace(R.id.fragment_container, pdfFragment);
        transaction.addToBackStack("pdf_viewer");
        transaction.commit();
    }
    
    /**
     * Checks if a URL is a PDF (by extension or content type)
     */
    public static boolean isPdfUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".pdf") || 
               lowerUrl.contains(".pdf?") ||
               lowerUrl.contains("pdf") && (lowerUrl.contains("notification") || 
                                            lowerUrl.contains("selection") || 
                                            lowerUrl.contains("syllabus"));
    }
    
    /**
     * Builds full URL from relative path
     */
    private static String buildFullUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        String url = filePath.startsWith("http") ? filePath : BuildConfig.BASE_URL + filePath;
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        return url;
    }
}

