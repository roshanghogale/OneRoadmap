package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;
import com.newsproject.oneroadmap.Adapters.CareerRoadmapsAdapter;
import com.newsproject.oneroadmap.Models.CareerRoadmapSlider;
import com.newsproject.oneroadmap.Models.CareerRoadmaps;
import com.newsproject.oneroadmap.R;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainFragment extends Fragment {
    private static final String BASE_URL = "https://admin.mahaalert.cloud/api/";
    private static final String TAG = "MainFragment";

    private RecyclerView careerRoadmapsRecycler;
    private ProgressBar loadingProgress;
    private ArrayList<CareerRoadmaps> careerRoadmapsList;
    private ArrayList<CareerRoadmaps> originalRoadmapsList;
    private CareerRoadmapsAdapter adapter;
    private ExecutorService executorService;
    private ImageView careerSliderImage, topImage;
    private TextView topText;
    private LinearLayout startupCard, jobCard;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private String userEducationCategory = "All";
    private List<String> educationOptions = new ArrayList<>();

    private boolean isJobCardFirstClick = true;
    private boolean isStartupCardFirstClick = true;
    private boolean isEducationFilterActive = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        careerRoadmapsRecycler = view.findViewById(R.id.career_roadmap_recycler);
        careerSliderImage = view.findViewById(R.id.career_slider_image);
        loadingProgress = view.findViewById(R.id.loading_progress);
        startupCard = view.findViewById(R.id.startup_card);
        jobCard = view.findViewById(R.id.job_card);
        topImage = view.findViewById(R.id.top_image);
        topText = view.findViewById(R.id.top_text);

        careerRoadmapsList = new ArrayList<>();
        originalRoadmapsList = new ArrayList<>();
        adapter = new CareerRoadmapsAdapter(careerRoadmapsList, requireContext());
        careerRoadmapsRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        careerRoadmapsRecycler.setAdapter(adapter);

        executorService = Executors.newSingleThreadExecutor();

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userEducationCategory = prefs.getString("education_category", "All");
        Log.d(TAG, "User education category: " + userEducationCategory);

        setupCareerRoadmaps();
        setupCareerSlider();
        setupCardClicks();
        setBottomMarginsBasedOnAndroidVersion();

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        // Cancel pending tasks to prevent accessing detached fragment
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Ensure executor is shut down if not already
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // --------------------------- LOAD ROADMAPS ---------------------------

    private void setupCareerRoadmaps() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> loadingProgress.setVisibility(View.VISIBLE));

        executorService.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BASE_URL + "career-roadmaps/")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response.code());

                    String json = response.body().string();
                    Log.d(TAG, "setupCareerRoadmaps: API response received");
                    CareerRoadmapResponse careerRoadmapResponse = gson.fromJson(json, CareerRoadmapResponse.class);
                    List<CareerRoadmaps> roadmaps = careerRoadmapResponse.getCareerRoadmaps();

                    mainHandler.post(() -> {
                        if (!isAdded()) {
                            Log.w(TAG, "Fragment not attached, skipping roadmap update");
                            return;
                        }
                        careerRoadmapsList.clear();
                        originalRoadmapsList.clear();
                        Set<String> uniqueCategories = new HashSet<>();
                        uniqueCategories.add("All");

                        if (roadmaps != null && !roadmaps.isEmpty()) {
                            for (CareerRoadmaps roadmap : roadmaps) {
                                if ("startup".equalsIgnoreCase(roadmap.getType()) || "career".equalsIgnoreCase(roadmap.getType())) {
                                    if (roadmap.getEducationCategories() != null)
                                        uniqueCategories.addAll(roadmap.getEducationCategories());

                                    fixUrls(roadmap);
                                    originalRoadmapsList.add(roadmap);
                                }
                            }
                        }

                        if (originalRoadmapsList.isEmpty()) {
                            originalRoadmapsList.add(new CareerRoadmaps(
                                    "https://via.placeholder.com/300x150.png",
                                    "Unknown Category",
                                    "Fallback Title",
                                    "https://example.com/fallback.pdf",
                                    "fallback",
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    "2025-10-19"
                            ));
                        }

                        educationOptions = new ArrayList<>(uniqueCategories);
                        Collections.sort(educationOptions);

                        // Always sort latest first when loaded
                        sortByNewest(originalRoadmapsList);
                        filterByType("all");

                        loadingProgress.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, skipping error toast");
                        return;
                    }
                    Log.e(TAG, "setupCareerRoadmaps: Error " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load career roadmaps.", Toast.LENGTH_SHORT).show();
                    loadingProgress.setVisibility(View.GONE);
                });
            }
        });
    }

    // --------------------------- CARD CLICKS ---------------------------

    private void setupCardClicks() {
        jobCard.setOnClickListener(v -> {
            if (isJobCardFirstClick) {
                isEducationFilterActive = true;
                filterByType("career");
                isJobCardFirstClick = false;
                isStartupCardFirstClick = true;
            } else {
                isEducationFilterActive = false;
                filterByType("all");
                isJobCardFirstClick = true;
            }
            updateCardForegrounds();
        });

        startupCard.setOnClickListener(v -> {
            if (isStartupCardFirstClick) {
                isEducationFilterActive = true;
                filterByType("startup");
                isStartupCardFirstClick = false;
                isJobCardFirstClick = true;
            } else {
                isEducationFilterActive = false;
                filterByType("all");
                isStartupCardFirstClick = true;
            }
            updateCardForegrounds();
        });
    }

    private void updateCardForegrounds() {
        Drawable borderDrawable = getResources().getDrawable(R.drawable.rectangle_white_solid_border);

        startupCard.setForeground(
                isEducationFilterActive && !isStartupCardFirstClick ? borderDrawable : null
        );

        jobCard.setForeground(
                isEducationFilterActive && !isJobCardFirstClick ? borderDrawable : null
        );
    }

    // --------------------------- FILTER LOGIC ---------------------------

    private void filterByType(String type) {
        careerRoadmapsList.clear();

        if ("all".equals(type)) {
            sortByNewest(originalRoadmapsList);
            careerRoadmapsList.addAll(originalRoadmapsList);
        } else {
            List<CareerRoadmaps> filtered = new ArrayList<>();
            for (CareerRoadmaps roadmap : originalRoadmapsList) {
                if (roadmap.getType() != null && roadmap.getType().equalsIgnoreCase(type)) {
                    if (isEducationFilterActive && !userEducationCategory.equals("All")) {
                        List<String> educationCategories = roadmap.getEducationCategories();
                        if (educationCategories != null && educationCategories.contains(userEducationCategory)) {
                            filtered.add(roadmap);
                        }
                    } else {
                        filtered.add(roadmap);
                    }
                }
            }

            sortByNewest(filtered);
            careerRoadmapsList.addAll(filtered);
        }

        adapter.notifyDataSetChanged();
        careerRoadmapsRecycler.scrollToPosition(0);
    }

    // --------------------------- SLIDER ---------------------------

    private void setupCareerSlider() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> loadingProgress.setVisibility(View.VISIBLE));

        executorService.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BASE_URL + "career-roadmap-sliders")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response.code());

                    String json = response.body().string();
                    CareerRoadmapSliderResponse sliderResponse = gson.fromJson(json, CareerRoadmapSliderResponse.class);
                    CareerRoadmapSlider[] sliders = sliderResponse.getCareerRoadmapSliders();
                    CareerRoadmapSlider slider = sliders != null && sliders.length > 0 ? sliders[0] : null;

                    mainHandler.post(() -> {
                        if (!isAdded()) {
                            Log.w(TAG, "Fragment not attached, skipping slider update");
                            return;
                        }
                        Context context = getContext();
                        if (context == null) {
                            Log.w(TAG, "Context is null, skipping slider update");
                            return;
                        }

                        if (slider != null && slider.getImageUrl() != null && !slider.getImageUrl().isEmpty()) {
                            String sliderUrl = slider.getImageUrl();
                            if (sliderUrl.startsWith("/"))
                                sliderUrl = "https://admin.mahaalert.cloud" + sliderUrl;
                            else if (sliderUrl.startsWith("http://"))
                                sliderUrl = sliderUrl.replace("http://", "https://");

                            Glide.with(context)
                                    .load(sliderUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .error(R.drawable.placeholder_image)
                                    .into(careerSliderImage);

                            if (slider.getUrl() != null && !slider.getUrl().isEmpty()) {
                                String webUrl = slider.getUrl();
                                if (!webUrl.startsWith("http"))
                                    webUrl = "https://" + webUrl;
                                final String finalWebUrl = webUrl;
                                careerSliderImage.setOnClickListener(v -> {
                                    if (isAdded()) {
                                        com.newsproject.oneroadmap.Utils.WebViewHelper.openUrlInApp(this, finalWebUrl);
                                    }
                                });
                            }
                        } else {
                            Glide.with(context)
                                    .load(R.drawable.placeholder_image)
                                    .into(careerSliderImage);
                        }
                        loadingProgress.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, skipping error toast");
                        return;
                    }
                    Log.e(TAG, "setupCareerSlider: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load slider.", Toast.LENGTH_SHORT).show();
                    loadingProgress.setVisibility(View.GONE);
                });
            }
        });
    }

    // --------------------------- UTILITIES ---------------------------

    private void sortByNewest(List<CareerRoadmaps> list) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Collections.sort(list, (r1, r2) -> {
            try {
                Date d1 = sdf.parse(r1.getCreatedAt());
                Date d2 = sdf.parse(r2.getCreatedAt());
                if (d1 != null && d2 != null) {
                    return d2.compareTo(d1); // Newest first
                }
            } catch (ParseException e) {
                Log.w(TAG, "Date parsing error: " + e.getMessage());
            }
            return 0;
        });
    }

    private void fixUrls(CareerRoadmaps roadmap) {
        if (roadmap.getImageUrl() != null && roadmap.getImageUrl().startsWith("http://"))
            roadmap.setImageUrl(roadmap.getImageUrl().replace("http://", "https://"));
        if (roadmap.getPdfUrl() != null && roadmap.getPdfUrl().startsWith("http://"))
            roadmap.setPdfUrl(roadmap.getPdfUrl().replace("http://", "https://"));
    }

    private void setBottomMarginsBasedOnAndroidVersion() {
        if (Build.VERSION.SDK_INT >= 34) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) careerRoadmapsRecycler.getLayoutParams();
            params.bottomMargin = dpToPx(128);
            careerRoadmapsRecycler.setLayoutParams(params);
            ViewGroup.MarginLayoutParams params2 = (ViewGroup.MarginLayoutParams) topImage.getLayoutParams();
            params2.topMargin = dpToPx(36);
            topImage.setLayoutParams(params2);
            ViewGroup.MarginLayoutParams params3 = (ViewGroup.MarginLayoutParams) topText.getLayoutParams();
            params3.topMargin = dpToPx(46);
            topText.setLayoutParams(params3);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class CareerRoadmapResponse {
        private List<CareerRoadmaps> careerRoadmaps;
        public List<CareerRoadmaps> getCareerRoadmaps() { return careerRoadmaps; }
    }

    private static class CareerRoadmapSliderResponse {
        private CareerRoadmapSlider[] careerRoadmapSliders;
        public CareerRoadmapSlider[] getCareerRoadmapSliders() { return careerRoadmapSliders; }
    }
}