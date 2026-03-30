package com.newsproject.oneroadmap.Fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.newsproject.oneroadmap.R;

public class WebViewFragment extends Fragment {
    private static final String ARG_URL = "url";
    private static final String TAG = "WebViewFragment";
    private static final String AD_UNIT_ID = "ca-app-pub-1998155307869144/5141101529"; // Test ID
    private String url;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView urlText;
    private ImageButton copyUrlButton;
    private InterstitialAd mInterstitialAd;
    private OnBackPressedCallback onBackPressedCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

        if (getActivity() instanceof com.newsproject.oneroadmap.Activities.MainActivity) {
            ((com.newsproject.oneroadmap.Activities.MainActivity) getActivity()).hideBottomNavigation();
        }

        webView = view.findViewById(R.id.web_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvError = view.findViewById(R.id.tv_error);
        urlText = view.findViewById(R.id.url_text);
        copyUrlButton = view.findViewById(R.id.copy_url_button);

        setupEdgeToEdge(view);
        setupWebView();
        loadInterstitialAd();
        setupBackPressHandler();

        if (url == null || url.isEmpty()) {
            showError("Invalid URL");
            return;
        }

        if (isPdfUrl(url)) {
            View urlBar = view.findViewById(R.id.url_bar);
            if (urlBar != null) {
                urlBar.setVisibility(View.GONE);
            }
            loadUrl(url);
        } else {
            setupUrlBar();
            urlText.setText(url);
            loadUrl(url);
        }
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(requireContext(), AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.d(TAG, "Ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, "Ad failed to load: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }

    private void setupBackPressHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    if (isPdfUrl(url) && mInterstitialAd != null) {
                        showAdAndExit();
                    } else {
                        exitFragment();
                    }
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
    }

    private void showAdAndExit() {
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed");
                mInterstitialAd = null;
                // Use mainHandler to ensure popBackStack runs instantly on the UI thread
                mainHandler.post(() -> exitFragment());
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                Log.d(TAG, "Ad failed to show: " + adError.getMessage());
                mInterstitialAd = null;
                mainHandler.post(() -> exitFragment());
            }
        });
        mInterstitialAd.show(requireActivity());
    }

    private void exitFragment() {
        if (isAdded() && getParentFragmentManager() != null) {
            onBackPressedCallback.setEnabled(false);
            getParentFragmentManager().popBackStack();
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
        if (isPdfUrl(url)) {
            rootView.setFitsSystemWindows(true);
            if (webView != null) {
                webView.setPadding(0, 0, 0, 0);
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                View urlBar = v.findViewById(R.id.url_bar);
                if (urlBar != null) {
                    urlBar.setPadding(urlBar.getPaddingLeft(), topInset, urlBar.getPaddingRight(), urlBar.getPaddingBottom());
                }
                if (webView != null) {
                    webView.setPadding(webView.getPaddingLeft(), webView.getPaddingTop(), webView.getPaddingRight(), bottomInset);
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
            if (isPdfUrl(urlString)) {
                String googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=" + urlString;
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

        if (getActivity() instanceof com.newsproject.oneroadmap.Activities.MainActivity) {
            ((com.newsproject.oneroadmap.Activities.MainActivity) getActivity()).hideBottomNavigation();
        }

        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (getActivity() instanceof com.newsproject.oneroadmap.Activities.MainActivity) {
            ((com.newsproject.oneroadmap.Activities.MainActivity) getActivity()).showBottomNavigation();
        }

        mainHandler.removeCallbacksAndMessages(null);

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
