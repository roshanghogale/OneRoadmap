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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

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
import com.newsproject.oneroadmap.Utils.NewsUtils;
import com.newsproject.oneroadmap.databinding.DialogEducationFilterBinding;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class BankingJobs extends Fragment {
    private static final String TAG = "BankingJobs";
    private static final String JOB_UPDATES_ENDPOINT = BuildConfig.JOB_UPDATES_ENDPOINT;
    private static final String PREFS_NAME = "UserPrefs";
    private ImageCarousel carousel;
    private ImageView backButton;
    private com.google.android.material.appbar.AppBarLayout appBarLayout;
    private RecyclerView allJobRecycler;
    private TextView privateBank, govtBank;
    private CardView chipEducation;
    private View emptyStateContainer, loadingProgress, retryButton;
    private FloatingActionButton fabScrollTop;
    private JobViewModel viewModel;
    private JobUpdateAdapter adapter;
    private OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DialogEducationFilterBinding dialogBinding;
    private AlertDialog educationDialog;
    private List<CarouselItem> carouselItemsList = new ArrayList<>();
    private Map<String, News> newsCache = new HashMap<>(); // Cache for news items

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int cacheSize = 10 * 1024 * 1024; // 10 MB cache
        Cache cache = new Cache(requireContext().getCacheDir(), cacheSize);
        client = new OkHttpClient.Builder()
                .cache(cache)
                .build();
        Log.d(TAG, "onCreate: Initialized OkHttpClient with cache size: " + cacheSize);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating fragment_banking_jobs layout");
        View view = inflater.inflate(R.layout.fragment_banking_jobs, container, false);
        carousel = view.findViewById(R.id.carousel);
        appBarLayout = view.findViewById(R.id.appbar);
        allJobRecycler = view.findViewById(R.id.recycler_job_updates);
        backButton = view.findViewById(R.id.back_button);
        privateBank = view.findViewById(R.id.private_bank);
        govtBank = view.findViewById(R.id.govt_bank);
        chipEducation = view.findViewById(R.id.chip_education);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        loadingProgress = view.findViewById(R.id.loading_progress);
        retryButton = view.findViewById(R.id.retry_button);
        fabScrollTop = view.findViewById(R.id.fab_scroll_top);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(JobViewModel.class);
        Log.d(TAG, "onCreateView: Initialized JobViewModel");

        initSlider();
        initLists();
        setupListeners();

        return view;
    }

    private void initLists() {
        // Set up RecyclerView
        allJobRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new JobUpdateAdapter(new ArrayList<>(), getParentFragmentManager());
        allJobRecycler.setAdapter(adapter);
        Log.d(TAG, "initLists: RecyclerView and JobUpdateAdapter initialized");

        // Load all banking jobs at once (no pagination)
        Log.d(TAG, "initLists: Calling loadAllBankingJobs");
        viewModel.loadAllBankingJobs(client, JOB_UPDATES_ENDPOINT, requireContext());

        // Observe ViewModel changes
        observeViewModel();
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "setupListeners: Back button clicked");
            getParentFragmentManager().popBackStack();
        });

        privateBank.setOnClickListener(v -> {
            Log.d(TAG, "setupListeners: Private Bank chip clicked");
            viewModel.handleTypeChipClick("banking", "private-bank", requireContext());
        });

        govtBank.setOnClickListener(v -> {
            Log.d(TAG, "setupListeners: Govt Bank chip clicked");
            viewModel.handleTypeChipClick("banking", "govt-bank", requireContext());
        });

        chipEducation.setOnClickListener(v -> {
            Log.d(TAG, "setupListeners: Education chip clicked, showing education dialog");
            showEducationDialog();
        });

        if (retryButton != null) {
            retryButton.setOnClickListener(v -> {
                Log.d(TAG, "setupListeners: Retry button clicked, reloading all banking jobs");
                viewModel.loadAllBankingJobs(client, JOB_UPDATES_ENDPOINT, requireContext());
                scrollToTop();
            });
        }

        // Setup Floating Action Button
        if (fabScrollTop != null) {
            fabScrollTop.setOnClickListener(v -> {
                Log.d(TAG, "setupListeners: FAB clicked, scrolling to top");
                scrollToTop();
            });
        }

        // Setup RecyclerView scroll listener for FAB visibility
        allJobRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() > 5) {
                    if (fabScrollTop != null) {
                        fabScrollTop.show();
                    }
                } else {
                    if (fabScrollTop != null) {
                        fabScrollTop.hide();
                    }
                }
            }
        });
    }

    private void scrollToTop() {
        // Expand the AppBar (header) to ensure the entire page goes to the top
        if (appBarLayout != null) {
            appBarLayout.setExpanded(true, true);
        }
        // Then ensure RecyclerView is at top as well
        if (!allJobRecycler.isComputingLayout()) {
            allJobRecycler.scrollToPosition(0);
        } else {
            allJobRecycler.post(() -> allJobRecycler.scrollToPosition(0));
        }
    }

    private void observeViewModel() {
        viewModel.getJobs().observe(getViewLifecycleOwner(), jobs -> {
            if (!allJobRecycler.isComputingLayout()) {
                int jobCount = jobs != null ? jobs.size() : 0;
                Log.d(TAG, "jobs_count: " + jobCount);
                adapter.updateJobs(jobs);
                allJobRecycler.setVisibility(jobCount > 0 ? View.VISIBLE : View.GONE);
                if (emptyStateContainer != null) {
                    emptyStateContainer.setVisibility(jobCount == 0 && !viewModel.isLoading() ? View.VISIBLE : View.GONE);
                }
                scrollToTop();
            }
        });

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "observeViewModel: Loading state changed, isLoading: " + isLoading +
                    ", current item count: " + adapter.getItemCount());
            adapter.setLoading(isLoading && adapter.getItemCount() > 0);
            if (loadingProgress != null) {
                loadingProgress.setVisibility(isLoading && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
            allJobRecycler.setVisibility(isLoading && adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(adapter.getItemCount() == 0 && !isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.e(TAG, "observeViewModel: Error message received: " + message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                if (emptyStateContainer != null && retryButton != null) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    retryButton.setVisibility(View.VISIBLE);
                    allJobRecycler.setVisibility(View.GONE);
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                }
            }
        });

        viewModel.getSelectedChip().observe(getViewLifecycleOwner(), selected -> {
            Log.d(TAG, "observeViewModel: Selected chip changed: " + selected);
            updateChipColors(selected);
            if (selected != null) {
                scrollToTop();
            }
        });
    }

    private void updateChipColors(String selected) {
        int defaultColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.white);

        // Reset all chips to default state
        privateBank.setBackgroundResource(R.drawable.rectangle_with_stroke);
        privateBank.setTextColor(defaultColor);
        govtBank.setBackgroundResource(R.drawable.rectangle_with_stroke);
        govtBank.setTextColor(defaultColor);
        chipEducation.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chip_default));

        if (selected != null) {
            Log.d(TAG, "updateChipColors: Updating chip colors for selection: " + selected);
            if (selected.equals("banking-private-bank")) {
                privateBank.setBackgroundResource(R.drawable.rectangle_filled);
                privateBank.setTextColor(selectedColor);
            } else if (selected.equals("banking-govt-bank")) {
                govtBank.setBackgroundResource(R.drawable.rectangle_filled);
                govtBank.setTextColor(selectedColor);
            } else if (selected.equals("education")) {
                chipEducation.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chip_selected));
            }
        }
    }

    private void showEducationDialog() {
        if (!isAdded()) {
            Log.w(TAG, "showEducationDialog: Fragment not attached, skipping dialog");
            return;
        }

        Log.d(TAG, "showEducationDialog: Creating education dialog");
        if (educationDialog == null) {
            dialogBinding = DialogEducationFilterBinding.inflate(LayoutInflater.from(requireContext()));
            educationDialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogBinding.getRoot())
                    .create();

            // Setup spinners
            ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, FilterUtils.getEducationOptions());
            educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dialogBinding.spinnerEducation.setAdapter(educationAdapter);
            Log.d(TAG, "showEducationDialog: Education spinner initialized with options: " + FilterUtils.getEducationOptions());

            ArrayAdapter<String> defaultDegreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, Arrays.asList("Select Degree"));
            ArrayAdapter<String> defaultPostGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, Arrays.asList("Select Post Graduation"));
            dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
            dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);

            dialogBinding.spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String category = FilterUtils.getEducationOptions().get(position);
                    Log.d(TAG, "showEducationDialog: Education category selected: " + category);
                    if (!category.equals("Select Education Category")) {
                        List<String> degrees = FilterUtils.getDegreeMap().getOrDefault(category, Arrays.asList("Select Degree", "Other"));
                        ArrayAdapter<String> degreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, degrees);
                        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerDegree.setAdapter(degreeAdapter);
                        Log.d(TAG, "showEducationDialog: Degree spinner updated with: " + degrees);

                        List<String> postGrads = FilterUtils.getPostGradMap().getOrDefault(category, Arrays.asList("Select Post Graduation", "None"));
                        ArrayAdapter<String> postGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, postGrads);
                        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerPostGrad.setAdapter(postGradAdapter);
                        Log.d(TAG, "showEducationDialog: PostGrad spinner updated with: " + postGrads);
                    } else {
                        dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
                        dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);
                        Log.d(TAG, "showEducationDialog: Resetting degree and postGrad spinners to default");
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.d(TAG, "showEducationDialog: No education category selected");
                }
            });

            dialogBinding.btnCancel.setOnClickListener(v -> {
                Log.d(TAG, "showEducationDialog: Cancel button clicked");
                educationDialog.dismiss();
            });

            dialogBinding.btnSave.setOnClickListener(v -> {
                String education = dialogBinding.spinnerEducation.getSelectedItem().toString();
                String degree = dialogBinding.spinnerDegree.getSelectedItem().toString();
                String postGrad = dialogBinding.spinnerPostGrad.getSelectedItem().toString();
                Log.d(TAG, "showEducationDialog: Save button clicked, education: " + education +
                        ", degree: " + degree + ", postGrad: " + postGrad);

                if (education.equals("Select Education Category") || degree.equals("Select Degree")) {
                    Log.w(TAG, "showEducationDialog: Invalid selection, showing toast");
                    Toast.makeText(requireContext(), "Please select education category and degree!", Toast.LENGTH_SHORT).show();
                    return;
                }

                viewModel.setFilter(requireContext(), education, degree, postGrad);
                educationDialog.dismiss();
                scrollToTop();
            });

            educationDialog.setOnShowListener(d -> {
                Log.d(TAG, "showEducationDialog: Dialog shown");
            });
            educationDialog.setOnDismissListener(d -> {
                Log.d(TAG, "showEducationDialog: Dialog dismissed");
            });
        }

        // Restore saved filter selections
        JobViewModel.Filter currentFilter = viewModel.getFilter().getValue();
        if (currentFilter != null && !currentFilter.getCategory().isEmpty() && !currentFilter.getCategory().equals("Select Education Category")) {
            Log.d(TAG, "showEducationDialog: Restoring filter - category: " + currentFilter.getCategory() +
                    ", degree: " + currentFilter.getDegree() + ", postGrad: " + currentFilter.getPostGrad());
            ArrayAdapter<String> educationAdapter = (ArrayAdapter<String>) dialogBinding.spinnerEducation.getAdapter();
            int position = educationAdapter.getPosition(currentFilter.getCategory());
            if (position != -1) {
                dialogBinding.spinnerEducation.setSelection(position);
                dialogBinding.spinnerEducation.getOnItemSelectedListener().onItemSelected(dialogBinding.spinnerEducation, null, position, 0);
                ArrayAdapter<String> degreeAdapter = (ArrayAdapter<String>) dialogBinding.spinnerDegree.getAdapter();
                if (degreeAdapter != null && !currentFilter.getDegree().isEmpty()) {
                    int degreePosition = degreeAdapter.getPosition(currentFilter.getDegree());
                    if (degreePosition != -1) {
                        dialogBinding.spinnerDegree.setSelection(degreePosition);
                        Log.d(TAG, "showEducationDialog: Restored degree selection: " + currentFilter.getDegree());
                    }
                }
                ArrayAdapter<String> postGradAdapter = (ArrayAdapter<String>) dialogBinding.spinnerPostGrad.getAdapter();
                if (postGradAdapter != null && !currentFilter.getPostGrad().isEmpty()) {
                    int postGradPosition = postGradAdapter.getPosition(currentFilter.getPostGrad());
                    if (postGradPosition != -1) {
                        dialogBinding.spinnerPostGrad.setSelection(postGradPosition);
                        Log.d(TAG, "showEducationDialog: Restored postGrad selection: " + currentFilter.getPostGrad());
                    }
                }
            }
        }

        educationDialog.show();
    }

    private void initSlider() {
        Log.d(TAG, "initSlider started (Banking API)");

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
                        dummyItems.add(new CarouselItem("https://picsum.photos/400/200", "Banking Dummy 1"));
                        dummyItems.add(new CarouselItem("https://picsum.photos/401/200", "Banking Dummy 2"));
                        dummyItems.add(new CarouselItem("https://picsum.photos/402/200", "Banking Dummy 3"));
                        carousel.addData(dummyItems);
                        Toast.makeText(requireContext(), "No network, loaded dummy banking sliders", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Loaded dummy banking carousel items");
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

                String url = "https://admin.mahaalert.cloud/api/sliders/banking";
                Request request = new Request.Builder().url(url).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Failed to fetch banking carousel items: " + e.getMessage());
                        mainHandler.post(() ->
                                Toast.makeText(requireContext(), "Failed to load banking sliders", Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Unexpected response: " + response.code());
                            mainHandler.post(() ->
                                    Toast.makeText(requireContext(), "Failed to load banking sliders", Toast.LENGTH_SHORT).show()
                            );
                            return;
                        }

                        String body = response.body().string();
                        Log.d(TAG, "Banking carousel API response length: " + body.length());

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
                                                JobViewModel.fetchJobUpdate(id, jobUpdateCache, requireContext(), null);
                                            }
                                        } else if ("news".equalsIgnoreCase(slider.getType())) {
                                            String id = slider.getPostDocumentId();
                                            if (id != null && !id.trim().isEmpty()) {
                                                NewsUtils.fetchNews(id, newsCache, requireContext(), null);
                                            }
                                        }
                                        Log.d(TAG, "Added banking slider: " + slider.getTitle());
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
                                carouselItemsList.clear();
                                carouselItemsList.addAll(carouselItems);
                                carousel.addData(carouselItemsList);
                                carousel.invalidate();

                                if (carouselItemsList.isEmpty()) {
                                    Toast.makeText(requireContext(), "No banking sliders available", Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "No banking carousel items found");
                                } else {
                                    Log.d(TAG, "Banking carousel loaded successfully, count: " + carouselItemsList.size());
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
                            Log.e(TAG, "Failed to parse banking carousel response: " + e.getMessage(), e);
                            mainHandler.post(() ->
                                    Toast.makeText(requireContext(), "Failed to parse banking sliders", Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in initSlider: " + e.getMessage(), e);
                mainHandler.post(() ->
                        Toast.makeText(requireContext(), "Error loading banking sliders", Toast.LENGTH_SHORT).show()
                );
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