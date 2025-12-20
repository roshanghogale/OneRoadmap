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

        // Initialize views
        webView = view.findViewById(R.id.web_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvError = view.findViewById(R.id.tv_error);
        urlText = view.findViewById(R.id.url_text);
        copyUrlButton = view.findViewById(R.id.copy_url_button);

        // Setup edge-to-edge for Android 15+
        setupEdgeToEdge(view);

        // Setup WebView
        setupWebView();

        // Setup URL bar
        setupUrlBar();

        // Load URL
        if (url != null && !url.isEmpty()) {
            urlText.setText(url);
            loadUrl(url);
        } else {
            showError("Invalid URL");
        }
    }

    private void setupEdgeToEdge(View view) {
        // Handle edge-to-edge for Android 11+ (API 30+)
        // This also covers Android 15+ (API 35+) when available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                WindowInsetsCompat windowInsets = insets;
                int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int bottomInset = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

                // Apply padding to URL bar to handle status bar
                View urlBar = v.findViewById(R.id.url_bar);
                if (urlBar != null) {
                    urlBar.setPadding(
                        urlBar.getPaddingLeft(),
                        topInset,
                        urlBar.getPaddingRight(),
                        urlBar.getPaddingBottom()
                    );
                }

                // Apply bottom padding to WebView to handle navigation bar
                if (webView != null) {
                    webView.setPadding(
                        webView.getPaddingLeft(),
                        webView.getPaddingTop(),
                        webView.getPaddingRight(),
                        bottomInset
                    );
                }

                return windowInsets;
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

        // Ensure URL has proper protocol
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "https://" + urlString;
        }

        try {
            webView.loadUrl(urlString);
        } catch (Exception e) {
            Log.e(TAG, "Error loading URL: " + urlString, e);
            showError("Error loading page: " + e.getMessage());
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

