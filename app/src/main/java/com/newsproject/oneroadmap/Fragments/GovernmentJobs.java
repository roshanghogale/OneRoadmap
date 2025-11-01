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

public class GovernmentJobs extends Fragment {
    private static final String TAG = "GovtJobs";
    private static final String JOB_UPDATES_ENDPOINT = BuildConfig.JOB_UPDATES_ENDPOINT;

    private ImageCarousel carousel;
    private ImageView backButton;
    private RecyclerView allJobRecycler;
    private FloatingActionButton fabScrollTop;
    private TextView mahaGovt, centralGovt;
    private CardView chipEducation;
    private JobViewModel viewModel;
    private JobUpdateAdapter adapter;
    private OkHttpClient client;
    private DialogEducationFilterBinding dialogBinding;
    private AlertDialog educationDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<CarouselItem> carouselItemsList = new ArrayList<>();
    private Map<String, News> newsCache = new HashMap<>();
    private Call sliderCall; // Track the network call

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_government_jobs, container, false);
        carousel = view.findViewById(R.id.carousel);
        allJobRecycler = view.findViewById(R.id.recycler_job_updates);
        backButton = view.findViewById(R.id.back_button);
        mahaGovt = view.findViewById(R.id.maha_govt);
        centralGovt = view.findViewById(R.id.central_govt);
        fabScrollTop = view.findViewById(R.id.fab_scroll_top);
        chipEducation = view.findViewById(R.id.chip_education);

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

        viewModel.loadAllGovernmentJobs(client, JOB_UPDATES_ENDPOINT, requireContext());
        observeViewModel();
        viewModel.restoreFilter(requireContext());
    }

    private void setupListeners() {
        mahaGovt.setOnClickListener(v -> viewModel.handleTypeChipClick("government", "Maha", requireContext()));
        centralGovt.setOnClickListener(v -> viewModel.handleTypeChipClick("government", "Central", requireContext()));

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
            mahaGovt.setBackgroundResource(R.drawable.rectangle_with_stroke);
            mahaGovt.setTextColor(defaultColor);
            centralGovt.setBackgroundResource(R.drawable.rectangle_with_stroke);
            centralGovt.setTextColor(defaultColor);
            chipEducation.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chip_default));
            if (selected != null) {
                if (selected.equals("government-Maha")) {
                    mahaGovt.setBackgroundResource(R.drawable.rectangle_filled);
                    mahaGovt.setTextColor(selectedColor);
                } else if (selected.equals("government-Central")) {
                    centralGovt.setBackgroundResource(R.drawable.rectangle_filled);
                    centralGovt.setTextColor(selectedColor);
                } else if ("education".equals(selected)) {
                    chipEducation.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chip_selected));
                }
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && getContext() != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> adapter.setLoading(isLoading));
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
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Please select education category and degree!", Toast.LENGTH_SHORT).show();
                    }
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
        Log.d(TAG, "initSlider started (Government Jobs API)");

        if (carousel == null) {
            Log.e(TAG, "Carousel is null, check layout or initialization");
            mainHandler.post(() -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Carousel not initialized", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Create local executor for this method
        ExecutorService localExecutor = Executors.newSingleThreadExecutor();
        localExecutor.execute(() -> {
            try {
                if (!isNetworkAvailable()) {
                    Log.w(TAG, "No network available, loading dummy carousel items");
                    mainHandler.post(() -> {
                        if (getContext() != null) {
                            List<CarouselItem> dummyItems = new ArrayList<>();
                            dummyItems.add(new CarouselItem("https://picsum.photos/400/200", "Government Dummy 1"));
                            dummyItems.add(new CarouselItem("https://picsum.photos/401/200", "Government Dummy 2"));
                            dummyItems.add(new CarouselItem("https://picsum.photos/402/200", "Government Dummy 3"));
                            carousel.addData(dummyItems);
                            Toast.makeText(getContext(), "No network, loaded dummy government sliders", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Loaded dummy government carousel items");
                        }
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

                String url = "https://test.gangainstitute.in/api/sliders/government";
                Request request = new Request.Builder().url(url).build();
                sliderCall = client.newCall(request); // Track the call
                sliderCall.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (call.isCanceled()) {
                            Log.d(TAG, "Slider call was canceled");
                            return;
                        }
                        Log.e(TAG, "Failed to fetch government carousel items: " + e.getMessage());
                        mainHandler.post(() -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Failed to load government sliders", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (call.isCanceled()) {
                            Log.d(TAG, "Slider call was canceled");
                            response.close();
                            return;
                        }
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Unexpected response: " + response.code());
                            mainHandler.post(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to load government sliders", Toast.LENGTH_SHORT).show();
                                }
                            });
                            response.close();
                            return;
                        }

                        String body = response.body().string();
                        Log.d(TAG, "Government carousel API response length: " + body.length());

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

                                    boolean isUniversal = false, educationMatch = false, locationMatch = false;
                                    List<String> sliderEducationCategories = slider.getEducationCategories();
                                    List<String> sliderBachelorDegrees = slider.getBachelorDegrees();
                                    List<String> sliderMastersDegrees = slider.getMastersDegrees();
                                    String sliderDistrict = slider.getDistrict() != null ? slider.getDistrict() : "";
                                    String sliderTaluka = slider.getTaluka() != null ? slider.getTaluka() : "";

                                    if (!slider.isSpecific()) {
                                        isUniversal = true;
                                    } else {
                                        if (sliderEducationCategories != null &&
                                                (sliderEducationCategories.contains("All") || sliderEducationCategories.contains("10th_12th"))) {
                                            isUniversal = true;
                                        } else {
                                            if (!userEducation.isEmpty() && sliderEducationCategories != null &&
                                                    sliderEducationCategories.contains(userEducation)) educationMatch = true;
                                            if (!userDegree.isEmpty() && sliderBachelorDegrees != null &&
                                                    sliderBachelorDegrees.contains(userDegree)) educationMatch = true;
                                            if (!userPostGraduation.isEmpty() && sliderMastersDegrees != null &&
                                                    sliderMastersDegrees.contains(userPostGraduation)) educationMatch = true;
                                            if (!userTwelfth.isEmpty() && sliderEducationCategories != null &&
                                                    sliderEducationCategories.contains("10th_12th")) educationMatch = true;
                                        }

                                        if (slider.isSpecific() && !userDistrict.isEmpty() && userDistrict.equals(sliderDistrict) &&
                                                !userTaluka.isEmpty() && userTaluka.equals(sliderTaluka)) {
                                            locationMatch = true;
                                        }
                                    }

                                    if (isUniversal || educationMatch || locationMatch) {
                                        sliders.add(slider);
                                        if ("post".equalsIgnoreCase(slider.getType())) {
                                            String id = slider.getPostDocumentId();
                                            if (id != null && !id.trim().isEmpty()) {
                                                JobViewModel.fetchJobUpdate(id, jobUpdateCache, getContext(), null);
                                            }
                                        } else if ("news".equalsIgnoreCase(slider.getType())) {
                                            String id = slider.getPostDocumentId();
                                            if (id != null && !id.trim().isEmpty()) {
                                                NewsUtils.fetchNews(id, newsCache, getContext(), null);
                                            }
                                        }
                                        Log.d(TAG, "Added government slider: " + slider.getTitle());
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
                                if (getContext() != null) {
                                    carouselItemsList.clear();
                                    carouselItemsList.addAll(carouselItems);
                                    carousel.addData(carouselItemsList);
                                    carousel.invalidate();

                                    if (carouselItemsList.isEmpty()) {
                                        Toast.makeText(getContext(), "No government sliders available", Toast.LENGTH_SHORT).show();
                                        Log.w(TAG, "No government carousel items found");
                                    } else {
                                        Log.d(TAG, "Government carousel loaded successfully, count: " + carouselItemsList.size());
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
                                            if (position < 0 || position >= sliders.size()) return;
                                            Slider selectedSlider = sliders.get(position);
                                            String id = selectedSlider.getPostDocumentId();
                                            if (id == null || id.trim().isEmpty()) {
                                                if (getContext() != null) {
                                                    Toast.makeText(getContext(), "Content unavailable", Toast.LENGTH_SHORT).show();
                                                }
                                                return;
                                            }

                                            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
                                            progressDialog.setMessage("Loading...");
                                            progressDialog.setCancelable(false);
                                            progressDialog.show();

                                            if ("post".equalsIgnoreCase(selectedSlider.getType())) {
                                                JobUpdate job = jobUpdateCache.get(id);
                                                if (job != null) {
                                                    JobViewModel.navigateToJobDetails(job, getContext(), progressDialog);
                                                } else {
                                                    JobViewModel.fetchJobUpdate(id, jobUpdateCache, getContext(), () ->
                                                            JobViewModel.navigateToJobDetails(jobUpdateCache.get(id), getContext(), progressDialog));
                                                }
                                            } else if ("news".equalsIgnoreCase(selectedSlider.getType())) {
                                                News news = newsCache.get(id);
                                                if (news != null) {
                                                    NewsUtils.showNewsDialog(news, getContext(), progressDialog);
                                                } else {
                                                    NewsUtils.fetchNews(id, newsCache, getContext(), () ->
                                                            NewsUtils.showNewsDialog(newsCache.get(id), getContext(), progressDialog));
                                                }
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse government carousel response: " + e.getMessage(), e);
                            mainHandler.post(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to parse government sliders", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } finally {
                            response.close();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in initSlider: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading government sliders", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                localExecutor.shutdown();
            }
        });
    }

    private boolean isNetworkAvailable() {
        Log.d(TAG, "Checking network availability");
        Context context = getContext();
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isAvailable = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Log.d(TAG, "Network available: " + isAvailable);
        return isAvailable;
    }

    private void scrollToTop() {
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
        if (sliderCall != null) {
            sliderCall.cancel(); // Cancel the network call
        }
        client.dispatcher().cancelAll();
        if (educationDialog != null && educationDialog.isShowing()) {
            educationDialog.dismiss();
        }
        dialogBinding = null;
        educationDialog = null;
    }
}