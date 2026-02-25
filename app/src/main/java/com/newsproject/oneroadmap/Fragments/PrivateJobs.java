package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.newsproject.oneroadmap.Adapters.JobUpdateAdapter;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.Models.JobViewModel;
import com.newsproject.oneroadmap.Models.News;
import com.newsproject.oneroadmap.Models.Slider;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.BuildConfig;
import com.newsproject.oneroadmap.Utils.FilterUtils;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AlertDialog;
import androidx.viewbinding.ViewBinding;

import com.newsproject.oneroadmap.Utils.NewsUtils;
import com.newsproject.oneroadmap.databinding.DialogEducationFilterBinding;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PrivateJobs extends Fragment {
    private static final String TAG = "PrivateJobs";
    private static final String JOB_UPDATES_ENDPOINT = BuildConfig.JOB_UPDATES;
    private ImageCarousel carousel;
    private ImageView backButton;
    private RecyclerView allJobRecycler;
    private FloatingActionButton fabScrollTop;
    private TextView privateInternship, privateRegular;
    private CardView chipEducation;
    private JobViewModel viewModel;
    private JobUpdateAdapter adapter;
    private OkHttpClient client;
    private DialogEducationFilterBinding dialogBinding;
    private AlertDialog educationDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<CarouselItem> carouselItemsList = new ArrayList<>();
    private Map<String, News> newsCache = new HashMap<>(); // Cache for news items

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_private_jobs, container, false);
        carousel = view.findViewById(R.id.carousel);
        allJobRecycler = view.findViewById(R.id.recycler_job_updates);
        backButton = view.findViewById(R.id.back_button);
        privateInternship = view.findViewById(R.id.private_internship);
        privateRegular = view.findViewById(R.id.private_regular);
        chipEducation = view.findViewById(R.id.chip_education);
        fabScrollTop = view.findViewById(R.id.fab_scroll_top);

        // OkHttp with cache
        int cacheSize = 10 * 1024 * 1024;
        Cache cache = new Cache(requireContext().getCacheDir(), cacheSize);
        client = new OkHttpClient.Builder().cache(cache).build();

        viewModel = new ViewModelProvider(this).get(JobViewModel.class);

        initSlider();
        initLists();
        setupListeners();

        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private void initLists() {
        allJobRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new JobUpdateAdapter(new ArrayList<>(), getParentFragmentManager());
        allJobRecycler.setAdapter(adapter);

        // Load all private jobs at once (no pagination)
        viewModel.loadAllPrivateJobs(client, JOB_UPDATES_ENDPOINT, requireContext());

        observeViewModel();
        viewModel.restoreFilter(requireContext());
    }

    private void setupListeners() {
        privateInternship.setOnClickListener(v -> viewModel.handleTypeChipClick("private", "internship", requireContext()));
        privateRegular.setOnClickListener(v -> viewModel.handleTypeChipClick("private", "regular-job", requireContext()));

        if (fabScrollTop != null) {
            fabScrollTop.setOnClickListener(v -> scrollToTop());
        }

        chipEducation.setOnClickListener(v -> showEducationDialog());

        allJobRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && lm.findFirstVisibleItemPosition() > 5) {
                    if (fabScrollTop != null) fabScrollTop.show();
                } else {
                    if (fabScrollTop != null) fabScrollTop.hide();
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.getJobs().observe(getViewLifecycleOwner(), items -> {
            int jobCount = items != null ? items.size() : 0;
            Log.d(TAG, "jobs_count: " + jobCount);
            adapter.updateJobs(items);
        });
        viewModel.getSelectedChip().observe(getViewLifecycleOwner(), selected -> {
            int defaultColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
            int selectedColor = ContextCompat.getColor(requireContext(), R.color.white);
            privateInternship.setBackgroundResource(R.drawable.rectangle_with_stroke);
            privateInternship.setTextColor(defaultColor);
            privateRegular.setBackgroundResource(R.drawable.rectangle_with_stroke);
            privateRegular.setTextColor(defaultColor);
            chipEducation.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chip_default));

            if (selected != null) {
                if (selected.equals("private-internship")) {
                    privateInternship.setBackgroundResource(R.drawable.rectangle_filled);
                    privateInternship.setTextColor(selectedColor);
                } else if (selected.equals("private-regular-job")) {
                    privateRegular.setBackgroundResource(R.drawable.rectangle_filled);
                    privateRegular.setTextColor(selectedColor);
                } else if ("education".equals(selected)) {
                    chipEducation.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chip_selected));
                }
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            adapter.setLoading(isLoading);
        });
    }

    private void showEducationDialog() {
        if (educationDialog == null) {
            dialogBinding = DialogEducationFilterBinding.inflate(LayoutInflater.from(requireContext()));
            educationDialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogBinding.getRoot())
                    .create();

            android.widget.ArrayAdapter<String> educationAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item, FilterUtils.getEducationOptions());
            educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dialogBinding.spinnerEducation.setAdapter(educationAdapter);

            android.widget.ArrayAdapter<String> defaultDegreeAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item, java.util.Arrays.asList("Select Degree"));
            android.widget.ArrayAdapter<String> defaultPostGradAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item, java.util.Arrays.asList("Select Post Graduation"));
            dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
            dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);

            dialogBinding.spinnerEducation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String category = FilterUtils.getEducationOptions().get(position);
                    if (!category.equals("Select Education Category")) {
                        java.util.List<String> degrees = FilterUtils.getDegreeMap().getOrDefault(category, java.util.Arrays.asList("Select Degree", "Other"));
                        android.widget.ArrayAdapter<String> degreeAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item, degrees);
                        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerDegree.setAdapter(degreeAdapter);

                        java.util.List<String> postGrads = FilterUtils.getPostGradMap().getOrDefault(category, java.util.Arrays.asList("Select Post Graduation", "None"));
                        android.widget.ArrayAdapter<String> postGradAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item, postGrads);
                        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerPostGrad.setAdapter(postGradAdapter);
                    } else {
                        dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
                        dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            dialogBinding.btnCancel.setOnClickListener(v -> educationDialog.dismiss());
            dialogBinding.btnSave.setOnClickListener(v -> {
                String education = dialogBinding.spinnerEducation.getSelectedItem().toString();
                String degree = dialogBinding.spinnerDegree.getSelectedItem().toString();
                String postGrad = dialogBinding.spinnerPostGrad.getSelectedItem().toString();
                if (education.equals("Select Education Category") || degree.equals("Select Degree")) {
                    Toast.makeText(requireContext(), "Please select education category and degree!", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.setFilter(requireContext(), education, degree, postGrad);
                educationDialog.dismiss();
                scrollToTop();
            });
        }

        // Restore saved
        JobViewModel.Filter currentFilter = viewModel.getFilter().getValue();
        if (currentFilter != null && !currentFilter.getCategory().isEmpty() && !currentFilter.getCategory().equals("Select Education Category")) {
            android.widget.ArrayAdapter<String> educationAdapter = (android.widget.ArrayAdapter<String>) dialogBinding.spinnerEducation.getAdapter();
            int position = educationAdapter.getPosition(currentFilter.getCategory());
            if (position != -1) {
                dialogBinding.spinnerEducation.setSelection(position);
                dialogBinding.spinnerEducation.getOnItemSelectedListener().onItemSelected(dialogBinding.spinnerEducation, null, position, 0);
                android.widget.ArrayAdapter<String> degreeAdapter = (android.widget.ArrayAdapter<String>) dialogBinding.spinnerDegree.getAdapter();
                if (degreeAdapter != null && !currentFilter.getDegree().isEmpty()) {
                    int degreePosition = degreeAdapter.getPosition(currentFilter.getDegree());
                    if (degreePosition != -1) dialogBinding.spinnerDegree.setSelection(degreePosition);
                }
                android.widget.ArrayAdapter<String> postGradAdapter = (android.widget.ArrayAdapter<String>) dialogBinding.spinnerPostGrad.getAdapter();
                if (postGradAdapter != null && !currentFilter.getPostGrad().isEmpty()) {
                    int postGradPosition = postGradAdapter.getPosition(currentFilter.getPostGrad());
                    if (postGradPosition != -1) dialogBinding.spinnerPostGrad.setSelection(postGradPosition);
                }
            }
        }

        educationDialog.show();
    }

    private void initSlider() {
        Log.d(TAG, "initSlider started (Private Jobs API)");

        if (carousel == null) {
            Log.e(TAG, "Carousel is null, check layout or initialization");
            mainHandler.post(() ->
                    Toast.makeText(requireContext(), "Carousel not initialized", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        // Create local executor for this method
        ExecutorService localExecutor = Executors.newSingleThreadExecutor();
        localExecutor.execute(() -> {
            try {
                if (!isNetworkAvailable()) {
                    Log.w(TAG, "No network available, loading dummy carousel items");
                    mainHandler.post(() -> {
                        List<CarouselItem> dummyItems = new ArrayList<>();
                        dummyItems.add(new CarouselItem("https://picsum.photos/400/200", "Private Dummy 1"));
                        dummyItems.add(new CarouselItem("https://picsum.photos/401/200", "Private Dummy 2"));
                        dummyItems.add(new CarouselItem("https://picsum.photos/402/200", "Private Dummy 3"));
                        carousel.addData(dummyItems);
                        Toast.makeText(requireContext(), "No network, loaded dummy private sliders", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Loaded dummy private carousel items");
                    });
                    return;
                }

                SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                String userEducation = sharedPreferences.getString("education", "");
                String userDegree = sharedPreferences.getString("degree", "");
                String userTwelfth = sharedPreferences.getString("twelfth", "");
                String userPostGraduation = sharedPreferences.getString("postGraduation", "");
                String userDistrict = sharedPreferences.getString("district", "");
                String userTaluka = sharedPreferences.getString("taluka", "");
                String userAgeGroup = sharedPreferences.getString("ageGroup", "");

                boolean studyGov = sharedPreferences.getBoolean("study_Government", false);
                boolean studyPolice = sharedPreferences.getBoolean("study_Police_Defence", false);
                boolean studyBank = sharedPreferences.getBoolean("study_Banking", false);

                String url = BuildConfig.BASE_URL + BuildConfig.SLIDERS_PRIVATE;
                Request request = new Request.Builder().url(url).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Failed to fetch private carousel items: " + e.getMessage());
                        mainHandler.post(() -> {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(requireContext(), "Failed to load private sliders", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Unexpected response: " + response.code());
                            mainHandler.post(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "Failed to load private sliders", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }

                        String body = response.body().string();
                        Log.d(TAG, "Private carousel API response length: " + body.length());

                        try {
                            JsonObject root = new Gson().fromJson(body, JsonObject.class);
                            List<Slider> sliders = new ArrayList<>();
                            Map<String, JobUpdate> jobUpdateCache = new HashMap<>();

                            if (root != null && root.has("sliders")) {
                                JsonArray arr = root.getAsJsonArray("sliders");
                                Log.d(TAG, "Total sliders fetched: " + arr.size());

                                for (int i = 0; i < arr.size(); i++) {
                                    JsonObject o = arr.get(i).getAsJsonObject();
                                    Slider slider = new Gson().fromJson(o, Slider.class);
                                    if (slider.getImageUrl() == null || slider.getImageUrl().isEmpty()) {
                                        continue;
                                    }

                                    boolean shouldShow = true;
                                    if (slider.isSpecific()) {
                                        boolean hasSpecificCriteria = (slider.getOtherType() != null && !slider.getOtherType().isEmpty()) ||
                                                !slider.getEducationCategoriesSafe().isEmpty() ||
                                                !slider.getBachelorDegreesSafe().isEmpty() ||
                                                !slider.getMastersDegreesSafe().isEmpty() ||
                                                !slider.getTalukaSafe().isEmpty() ||
                                                !slider.getAgeGroupsSafe().isEmpty() ||
                                                !slider.getBhartyTypesSafe().isEmpty();

                                        if (hasSpecificCriteria) {
                                            shouldShow = false;

                                            // 1. Degree/Education Match
                                            List<String> bDegrees = slider.getBachelorDegreesSafe();
                                            List<String> mDegrees = slider.getMastersDegreesSafe();
                                            List<String> eCats = slider.getEducationCategoriesSafe();
                                            
                                            if (!userDegree.isEmpty() && bDegrees.contains(userDegree)) shouldShow = true;
                                            if (!userPostGraduation.isEmpty() && mDegrees.contains(userPostGraduation)) shouldShow = true;
                                            if (!userEducation.isEmpty() && eCats.contains(userEducation)) shouldShow = true;
                                            if (!userTwelfth.isEmpty() && eCats.contains("10th_12th")) shouldShow = true;

                                            // 2. Taluka/District Match
                                            if (!shouldShow && !userTaluka.isEmpty() && slider.getTalukaSafe().contains(userTaluka)) shouldShow = true;
                                            if (!shouldShow && !userDistrict.isEmpty() && slider.getDistrictSafe().contains(userDistrict)) shouldShow = true;

                                            // 3. Age Group Match
                                            if (!shouldShow && !userAgeGroup.isEmpty() && slider.getAgeGroupsSafe().contains(userAgeGroup)) shouldShow = true;

                                            // 4. Bharty Types Match
                                            if (!shouldShow) {
                                                List<String> bTypes = slider.getBhartyTypesSafe();
                                                for (String type : bTypes) {
                                                    if (type.equalsIgnoreCase("Government") && studyGov) { shouldShow = true; break; }
                                                    if (type.equalsIgnoreCase("Police & Defence") && studyPolice) { shouldShow = true; break; }
                                                    if (type.equalsIgnoreCase("Banking") && studyBank) { shouldShow = true; break; }
                                                }
                                            }
                                        }
                                    }

                                    if (shouldShow) {
                                        sliders.add(slider);
                                        if (isAdded() && getContext() != null) {
                                            if ("post".equalsIgnoreCase(slider.getType())) {
                                                String id = slider.getPostDocumentId();
                                                if (id != null && !id.trim().isEmpty()) {
                                                    JobViewModel.fetchJobUpdate(id, jobUpdateCache, requireContext(), null);
                                                }
                                            } else if ("news".equalsIgnoreCase(slider.getType())) {
                                                String id = slider.getPostDocumentId();
                                                if (id != null && !id.trim().isEmpty()) {
                                                    NewsUtils.fetchNews(id, newsCache, requireContext(), null);
                                                }
                                            }
                                        }
                                        Log.d(TAG, "Added private slider: " + slider.getTitle());
                                    }
                                }
                            } else {
                                Log.w(TAG, "No sliders array found in response or root is null");
                            }

                            List<CarouselItem> carouselItems = new ArrayList<>();
                            for (Slider slider : sliders) {
                                String imageUrl = slider.getImageUrl().replace("http://", "https://");
                                carouselItems.add(new CarouselItem(imageUrl, slider.getTitle()));
                            }

                            mainHandler.post(() -> {
                                if (!isAdded() || getContext() == null) return;
                                
                                carouselItemsList.clear();
                                carouselItemsList.addAll(carouselItems);
                                carousel.addData(carouselItemsList);
                                carousel.invalidate();

                                if (carouselItemsList.isEmpty()) {
                                    Toast.makeText(requireContext(), "No private sliders available", Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "No private carousel items found");
                                } else {
                                    Log.d(TAG, "Private carousel loaded successfully, count: " + carouselItemsList.size());
                                }

                                carousel.setCarouselListener(new CarouselListener() {
                                    @Override
                                    public void onLongClick(int i, @NonNull CarouselItem carouselItem) {
                                    }

                                    @Override
                                    public void onBindViewHolder(@NonNull ViewBinding viewBinding, @NonNull CarouselItem carouselItem, int i) {
                                    }

                                    @Nullable
                                    @Override
                                    public ViewBinding onCreateViewHolder(@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup) {
                                        return null;
                                    }

                                    @Override
                                    public void onClick(int position, CarouselItem carouselItem) {
                                        if (!isAdded() || getContext() == null) return;
                                        if (position < 0 || position >= sliders.size()) return;
                                        Slider selectedSlider = sliders.get(position);
                                        String id = selectedSlider.getPostDocumentId();
                                        if (id == null || id.trim().isEmpty()) {
                                            Toast.makeText(requireContext(), "Content unavailable", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
                                        progressDialog.setMessage("Loading...");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();

                                        if ("post".equalsIgnoreCase(selectedSlider.getType())) {
                                            JobUpdate job = jobUpdateCache.get(id);
                                            if (job != null) {
                                                JobViewModel.navigateToJobDetails(job, requireContext(), progressDialog);
                                            } else {
                                                JobViewModel.fetchJobUpdate(id, jobUpdateCache, requireContext(), () ->
                                                        JobViewModel.navigateToJobDetails(jobUpdateCache.get(id), requireContext(), progressDialog));
                                            }
                                        } else if ("news".equalsIgnoreCase(selectedSlider.getType())) {
                                            News news = newsCache.get(id);
                                            if (news != null) {
                                                NewsUtils.showNewsDialog(news, requireContext(), progressDialog);
                                            } else {
                                                NewsUtils.fetchNews(id, newsCache, requireContext(), () ->
                                                        NewsUtils.showNewsDialog(newsCache.get(id), requireContext(), progressDialog));
                                            }
                                        }
                                    }
                                });
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse private carousel response: " + e.getMessage(), e);
                            mainHandler.post(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "Failed to parse private sliders", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in initSlider: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "Error loading private sliders", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                localExecutor.shutdown();
            }
        });
    }

    private boolean isNetworkAvailable() {
        Log.d(TAG, "Checking network availability");
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isAvailable = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Log.d(TAG, "Network available: " + isAvailable);
        return isAvailable;
    }

    private void scrollToTop() {
        // Expand header then scroll list to top
        View appBar = requireView().findViewById(R.id.appbar);
        if (appBar instanceof com.google.android.material.appbar.AppBarLayout) {
            ((com.google.android.material.appbar.AppBarLayout) appBar).setExpanded(true, true);
        }
        if (!allJobRecycler.isComputingLayout()) {
            allJobRecycler.scrollToPosition(0);
        } else {
            allJobRecycler.post(() -> allJobRecycler.scrollToPosition(0));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up resources");
        client.dispatcher().cancelAll();
        if (educationDialog != null && educationDialog.isShowing()) {
            educationDialog.dismiss();
        }
        dialogBinding = null;
        educationDialog = null;
    }
}
