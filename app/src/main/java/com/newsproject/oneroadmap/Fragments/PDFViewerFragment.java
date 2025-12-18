package com.newsproject.oneroadmap.Fragments;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.CoinManager;
import com.newsproject.oneroadmap.Utils.DatabaseHelper;
import com.newsproject.oneroadmap.Utils.ShareHelper;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.widget.Button;
import android.view.LayoutInflater;
import android.content.SharedPreferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PDFViewerFragment extends Fragment {

    private static final String ARG_PDF_URL = "pdf_url";
    private static final String TAG = "PDFViewerFragment";

    private String pdfUrl;
    private PDFView pdfView;
    private ProgressBar progressBar;
    private TextView tvError;
    private FloatingActionButton fabSave;
    private File tempPdfFile;
    private CoinManager coinManager;
    private String userId;
    private Handler handler = new Handler();
    private int displayedCoins = 0;

    // Modern permission launchers
    private ActivityResultLauncher<String[]> legacyStorageLauncher;
    private ActivityResultLauncher<Intent> manageStorageLauncher;
    private ActivityResultLauncher<Intent> shareLauncher;

    public static PDFViewerFragment newInstance(String pdfUrl) {
        PDFViewerFragment f = new PDFViewerFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PDF_URL, pdfUrl);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) pdfUrl = getArguments().getString(ARG_PDF_URL);
        
        // Get userId from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        if (userId != null && !userId.isEmpty()) {
            coinManager = new CoinManager(requireContext(), userId);
        }
        
        initPermissionLaunchers();
    }

    private void initPermissionLaunchers() {
        // 1. Legacy storage (API ≤ 32)
        legacyStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE));
                    if (granted) downloadAndLoadPdf();
                    else showError("Storage permission required");
                });

        // 2. MANAGE_EXTERNAL_STORAGE (API 30+)
        manageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                r -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                        downloadAndLoadPdf();
                    } else {
                        showError("All-files access required");
                    }
                });
        
        // 3. Share launcher for WhatsApp sharing
        shareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // When user returns from sharing, add coins and show dialog
                    // If callback is triggered, it means WhatsApp was opened, so add coins
                    if (coinManager != null && userId != null && !userId.isEmpty()) {
                        int current = coinManager.getCoins();
                        coinManager.addCoinsForShare(newCoins -> {
                            showCoinEarnedDialog(current, newCoins);
                        });
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pdf_viewer, container, false);

        pdfView = v.findViewById(R.id.pdf_view);
        progressBar = v.findViewById(R.id.progress_bar);
        tvError = v.findViewById(R.id.tv_error);
        fabSave = v.findViewById(R.id.fab_save);

        fabSave.setOnClickListener(view -> saveToDownloads());
        tvError.setOnClickListener(view -> requestStoragePermission()); // retry

        requestStoragePermission();   // start permission flow
        return v;
    }

    /** --------------------------------------------------------------
     *  Permission flow – only MANAGE_EXTERNAL_STORAGE on Android 11+
     *  -------------------------------------------------------------- */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {               // Android 11+
            if (Environment.isExternalStorageManager()) {
                downloadAndLoadPdf();
            } else {
                Intent i = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                manageStorageLauncher.launch(i);
            }
        } else {                                                            // Android 10 and lower
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                downloadAndLoadPdf();
            } else {
                legacyStorageLauncher.launch(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                });
            }
        }
    }

    /** --------------------------------------------------------------
     *  Download → temp cache → show PDF
     *  -------------------------------------------------------------- */
    private void downloadAndLoadPdf() {
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        pdfView.setVisibility(View.GONE);
        fabSave.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                tempPdfFile = downloadToCache(pdfUrl);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            progressBar.setVisibility(View.GONE);
                            if (tempPdfFile != null && tempPdfFile.exists()) {
                                displayPdf(tempPdfFile);
                                fabSave.setVisibility(View.VISIBLE);
                            } else {
                                showError("Download failed – tap to retry");
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "download error", e);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            showError("Download error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private File downloadToCache(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }

        InputStream in = new BufferedInputStream(conn.getInputStream());
        File outFile = new File(requireContext().getCacheDir(),
                "pdf_tmp_" + System.currentTimeMillis() + ".pdf");
        FileOutputStream fos = new FileOutputStream(outFile);

        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);

        fos.close(); in.close(); conn.disconnect();
        return outFile;
    }

    private void displayPdf(File file) {
        pdfView.setVisibility(View.VISIBLE);
        pdfView.fromFile(file)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .onLoad(nbPages -> Log.d(TAG, "PDF loaded, pages: " + nbPages))
                .load();
    }

    private void showError(String msg) {
        progressBar.setVisibility(View.GONE);
        pdfView.setVisibility(View.GONE);
        fabSave.setVisibility(View.GONE);
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    /** --------------------------------------------------------------
     *  FAB → Save to public Downloads (uses DownloadManager – no extra perm)
     *  Check coins first, deduct if enough, show dialog if not
     *  -------------------------------------------------------------- */
    private void saveToDownloads() {
        if (tempPdfFile == null) return;
        
        // Check if user has enough coins
        if (coinManager == null || !coinManager.hasEnoughCoins(CoinManager.getCoinsPerDownload())) {
            showNotEnoughCoinsDialog();
            return;
        }
        
        // Get current coins before deduction
        int currentCoins = coinManager.getCoins();
        
        // Deduct coins and proceed with download
        coinManager.deductCoinsForDownload(newCoins -> {
            // Show coin deduction dialog
            showCoinDeductionDialog(currentCoins, newCoins);
            
            // Coins deducted, proceed with download
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(pdfUrl));
            req.setTitle("PDF Document");
            req.setDescription("Saving to Downloads");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "PDF_" + System.currentTimeMillis() + ".pdf");

            DownloadManager dm = (DownloadManager) requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(req);

            Toast.makeText(requireContext(), "PDF saved to Downloads", Toast.LENGTH_LONG).show();
        });
    }
    
    private void showNotEnoughCoinsDialog() {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.not_enough_coins_dialog, null);
        
        Button shareButton = view.findViewById(R.id.btn_share);
        Button cancelButton = view.findViewById(R.id.btn_cancel);
        TextView messageText = view.findViewById(R.id.message_text);
        
        // Update message with current coins
        if (coinManager != null) {
            int current = coinManager.getCoins();
            int required = CoinManager.getCoinsPerDownload();
            String message = "You need " + required + " coins to download this PDF.\n\n" +
                           "You have: " + current + " coins\n\n" +
                           "Share this post to earn 100 coins!";
            if (messageText != null) {
                messageText.setText(message);
            }
        }
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        shareButton.setOnClickListener(v -> {
            dialog.dismiss();
            shareToWhatsApp();
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void shareToWhatsApp() {
        ShareHelper shareHelper = new ShareHelper(requireContext());
        shareHelper.setShareLauncher(shareLauncher);
        shareHelper.sharePost("Check out this PDF from One Roadmap!", pdfUrl);
    }
    
    private void showCoinEarnedDialog(int start, int end) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.coin_dialog_layout, null);
        TextView count = view.findViewById(R.id.coin_count);
        Button ok = view.findViewById(R.id.ok_button);
        count.setText("Coins: " + start);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        displayedCoins = start;
        handler.post(new Runnable() {
            @Override public void run() {
                if (!isAdded() || getActivity() == null) return;
                if (displayedCoins < end) {
                    displayedCoins++;
                    count.setText("Coins: " + displayedCoins);
                    handler.postDelayed(this, 20);
                }
            }
        });
    }
    
    private void showCoinDeductionDialog(int startCoins, int endCoins) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.coin_dialog_layout, null);
        TextView count = view.findViewById(R.id.coin_count);
        Button ok = view.findViewById(R.id.ok_button);
        count.setText("Coins: " + startCoins);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        final int[] displayedCoins = {startCoins};
        Handler handler = new Handler(android.os.Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override public void run() {
                if (!isAdded() || getContext() == null) return;
                if (displayedCoins[0] > endCoins) {
                    displayedCoins[0]--;
                    count.setText("Coins: " + displayedCoins[0]);
                    handler.postDelayed(this, 20);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tempPdfFile != null && tempPdfFile.exists()) tempPdfFile.delete();
    }
}