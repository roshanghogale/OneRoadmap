package com.newsproject.oneroadmap.Fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.newsproject.oneroadmap.R;

public class WebViewFragment extends Fragment {
    private static final String ARG_URL = "url";
    private static final String TAG = "WebViewFragment";

    private String url;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView urlText;
    private ImageButton copyUrlButton;

    public static WebViewFragment newInstance(String url) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView = view.findViewById(R.id.web_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvError = view.findViewById(R.id.tv_error);
        urlText = view.findViewById(R.id.url_text);
        copyUrlButton = view.findViewById(R.id.copy_url_button);

        setupEdgeToEdge(view);
        setupWebView();

        if (url == null || url.isEmpty()) {
            showError("Invalid URL");
            return;
        }

        if (isPdfUrl(url)) {

            View urlBar = view.findViewById(R.id.url_bar);
            if (urlBar != null) {
                urlBar.setVisibility(View.GONE);
            }

            loadUrl(url);   // 🔥 THIS WAS MISSING

        } else {

            setupUrlBar();
            urlText.setText(url);
            loadUrl(url);
        }
    }

    private boolean isPdfUrl(String url) {
        if (url == null) return false;

        url = url.toLowerCase();

        return url.endsWith(".pdf")
                || url.contains(".pdf?")
                || url.contains("application/pdf");
    }

    private void setupEdgeToEdge(View rootView) {

        // ❌ If PDF → DO NOT apply edge-to-edge
        if (isPdfUrl(url)) {
            rootView.setFitsSystemWindows(true);

            if (webView != null) {
                webView.setPadding(0, 0, 0, 0);
            }

            return;
        }

        // ✅ Normal websites → enable edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {

                int topInset = insets.getInsets(
                        WindowInsetsCompat.Type.statusBars()).top;

                int bottomInset = insets.getInsets(
                        WindowInsetsCompat.Type.navigationBars()).bottom;

                View urlBar = v.findViewById(R.id.url_bar);

                if (urlBar != null) {
                    urlBar.setPadding(
                            urlBar.getPaddingLeft(),
                            topInset,
                            urlBar.getPaddingRight(),
                            urlBar.getPaddingBottom()
                    );
                }

                if (webView != null) {
                    webView.setPadding(
                            webView.getPaddingLeft(),
                            webView.getPaddingTop(),
                            webView.getPaddingRight(),
                            bottomInset
                    );
                }

                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Enable mixed content for HTTPS pages with HTTP resources
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                tvError.setVisibility(View.GONE);
                if (urlText != null) {
                    urlText.setText(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                if (urlText != null) {
                    urlText.setText(url);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showError("Failed to load page: " + error.getDescription());
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Load all URLs in the same WebView
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void setupUrlBar() {
        copyUrlButton.setOnClickListener(v -> {
            String currentUrl = webView != null ? webView.getUrl() : url;
            if (currentUrl != null && !currentUrl.isEmpty()) {
                copyToClipboard(currentUrl);
                Toast.makeText(getContext(), "URL copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void copyToClipboard(String text) {
        if (getContext() != null) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("URL", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    private void loadUrl(String urlString) {

        if (urlString == null || urlString.isEmpty()) {
            showError("Invalid URL");
            return;
        }

        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "https://" + urlString;
        }

        try {

            // 🔥 If PDF → open with Google Docs Viewer (BEST METHOD)
            if (isPdfUrl(urlString)) {
                String googleDocsUrl =
                        "https://docs.google.com/gview?embedded=true&url=" + urlString;
                webView.loadUrl(googleDocsUrl);
            } else {
                webView.loadUrl(urlString);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading URL: " + urlString, e);
            showError("Error loading page");
        }
    }

    private void showError(String message) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
        if (webView != null) {
            webView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
    }
}

