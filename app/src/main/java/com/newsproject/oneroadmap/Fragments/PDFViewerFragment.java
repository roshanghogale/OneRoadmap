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
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
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
    private TextView progressText;
    private LinearLayout progressContainer;
    private TextView tvError;
    private TextView tvPageCount;
    private FloatingActionButton fabSave;
    private ImageButton btnClose;
    private File tempPdfFile;
    private CoinManager coinManager;
    private String userId;
    private Handler handler = new Handler();
    private int displayedCoins = 0;
    private int totalPages = 0;
    private int currentPage = 0;

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
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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
        progressText = v.findViewById(R.id.progress_text);
        progressContainer = v.findViewById(R.id.progress_container);
        tvError = v.findViewById(R.id.tv_error);
        tvPageCount = v.findViewById(R.id.tv_page_count);
        fabSave = v.findViewById(R.id.fab_save);
        btnClose = v.findViewById(R.id.btn_close);

        fabSave.setOnClickListener(view -> saveToDownloads());
        btnClose.setOnClickListener(view -> closeFragment());
        tvError.setOnClickListener(view -> requestStoragePermission()); // retry

        // Setup edge-to-edge for Android 15+ (API 35+)
        setupEdgeToEdge(v);

        requestStoragePermission();   // start permission flow
        return v;
    }
    
    private void setupEdgeToEdge(View view) {
        // For Android 15+ (API 35+), only ignore bottom safe edge
        // Keep top safe edge for status bar - ensure it's respected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                WindowInsetsCompat windowInsets = insets;
                
                // For Android 15+ (API 35+), only ignore bottom safe edge
                if (Build.VERSION.SDK_INT >= 35) {
                    // Get insets
                    int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    
                    // Apply top padding to root view to respect status bar (top safe edge)
                    v.setPadding(
                        v.getPaddingLeft(),
                        topInset,
                        v.getPaddingRight(),
                        v.getPaddingBottom()
                    );
                    
                    // Apply small fixed margin to close button (relative to the padded content area)
                    // Since root view already has top padding for status bar, just add a small margin
                    if (btnClose != null) {
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) btnClose.getLayoutParams();
                        // Convert 16dp to pixels for the margin
                        float density = getResources().getDisplayMetrics().density;
                        int margin16dp = (int) (16 * density);
                        params.topMargin = margin16dp;
                        btnClose.setLayoutParams(params);
                    }
                    
                    // For page count text, position it 24dp from actual bottom (ignoring safe area)
                    // The layout already has marginBottom="24dp", so it will be positioned correctly
                    // We just need to make sure the root view doesn't apply bottom padding
                    
                    // Return insets but consume navigation bar insets so bottom content extends to edge
                    // Keep status bar insets to respect top safe edge
                    Insets statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                    return new WindowInsetsCompat.Builder()
                            .setInsets(WindowInsetsCompat.Type.statusBars(), statusBarInsets)
                            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.NONE)
                            .build();
                } else {
                    // For Android 11-14, respect both top and bottom safe edges
                    int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    
                    // Apply top padding to root view to respect status bar
                    v.setPadding(
                        v.getPaddingLeft(),
                        topInset,
                        v.getPaddingRight(),
                        v.getPaddingBottom()
                    );
                    
                    // Apply small fixed margin to close button
                    if (btnClose != null) {
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) btnClose.getLayoutParams();
                        float density = getResources().getDisplayMetrics().density;
                        int margin16dp = (int) (16 * density);
                        params.topMargin = margin16dp;
                        btnClose.setLayoutParams(params);
                    }
                    
                    return windowInsets;
                }
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0+ (API 21+) - fallback for older devices
            if (getActivity() != null && getActivity().getWindow() != null) {
                getActivity().getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                );
            }
        }
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
        progressContainer.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressText.setText("0%");
        tvError.setVisibility(View.GONE);
        pdfView.setVisibility(View.GONE);
        fabSave.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                tempPdfFile = downloadToCache(pdfUrl);
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            progressContainer.setVisibility(View.GONE);
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

        int contentLength = conn.getContentLength();
        InputStream in = new BufferedInputStream(conn.getInputStream());
        File outFile = new File(requireContext().getCacheDir(),
                "pdf_tmp_" + System.currentTimeMillis() + ".pdf");
        FileOutputStream fos = new FileOutputStream(outFile);

        byte[] buf = new byte[8192];
        int len;
        long total = 0;
        int lastProgress = -1;
        
        while ((len = in.read(buf)) > 0) {
            fos.write(buf, 0, len);
            total += len;
            
            if (contentLength > 0 && isAdded() && getActivity() != null) {
                int progress = (int) ((total * 100) / contentLength);
                if (progress != lastProgress) {
                    lastProgress = progress;
                    final int finalProgress = progress;
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            progressBar.setProgress(finalProgress);
                            progressText.setText(finalProgress + "%");
                        }
                    });
                }
            }
        }

        fos.close(); in.close(); conn.disconnect();
        return outFile;
    }

    private void displayPdf(File file) {
        pdfView.setVisibility(View.VISIBLE);
        
        // Set minimum zoom to allow zooming out below full width
        // This must be called before loading the PDF
        pdfView.setMinZoom(0.5f); // Allow zooming out to 50% of original size
        pdfView.setMidZoom(1.0f); // Default zoom level
        pdfView.setMaxZoom(3.0f); // Allow zooming in up to 300%
        
        pdfView.fromFile(file)
                .defaultPage(0)
                .spacing(16) // Add gap between pages (in pixels)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true) // Enable double tap to zoom
                .onLoad(nbPages -> {
                    totalPages = nbPages;
                    currentPage = 1; // PDFView uses 0-based indexing, but we display 1-based
                    updatePageCount();
                    tvPageCount.setVisibility(View.VISIBLE);
                    
                    // Set initial zoom level to create space on left and right
                    // Users can zoom in to use full width or zoom out to see more space
                    pdfView.zoomTo(0.85f); // Adjust this value to control initial spacing
                    pdfView.moveTo(0, 0); // Reset position to top-left corner
                    
                    Log.d(TAG, "PDF loaded, pages: " + nbPages);
                })
                .onPageChange((page, pageCount) -> {
                    currentPage = page + 1; // Convert to 1-based for display
                    updatePageCount();
                })
                .load();
    }
    
    private void updatePageCount() {
        if (tvPageCount != null && totalPages > 0) {
            tvPageCount.setText("page " + currentPage + " / " + totalPages);
        }
    }

    private void showError(String msg) {
        progressContainer.setVisibility(View.GONE);
        pdfView.setVisibility(View.GONE);
        fabSave.setVisibility(View.GONE);
        tvPageCount.setVisibility(View.GONE);
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
    
    private void closeFragment() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
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