package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.newsproject.oneroadmap.Adapters.JobUpdateAdapter;
import com.newsproject.oneroadmap.Utils.BuildConfig;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.FilterUtils;
import com.newsproject.oneroadmap.Models.JobViewModel;
import com.newsproject.oneroadmap.databinding.DialogEducationFilterBinding;
import com.newsproject.oneroadmap.databinding.FragmentAllCategoryBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class AllCategory extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String JOB_UPDATES_ENDPOINT = BuildConfig.JOB_UPDATES;

    private FragmentAllCategoryBinding binding;
    private JobViewModel viewModel;
    private JobUpdateAdapter adapter;
    private DialogEducationFilterBinding dialogBinding;
    private AlertDialog educationDialog;
    private SharedPreferences sharedPreferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private OkHttpClient client;
    private Handler animationHandler;
    private Runnable animationRunnable;
    private Animation shakeAnimation;
    private boolean isFirstFabShow;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int cacheSize = 10 * 1024 * 1024; // 10 MB cache
        Cache cache = new Cache(requireContext().getCacheDir(), cacheSize);
        client = new OkHttpClient.Builder()
                .cache(cache)
                .build();
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize animation and handler
        animationHandler = new Handler(Looper.getMainLooper());
        shakeAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.shake);
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding != null && binding.fabScrollTop.getVisibility() == View.VISIBLE) {
                    binding.fabScrollTop.startAnimation(shakeAnimation);
                    if (!isFirstFabShow) {
                        animationHandler.postDelayed(this, 2000);
                    }
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAllCategoryBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(JobViewModel.class);
        viewModel.setCurrentJobType(null);

        binding.recyclerAllJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new JobUpdateAdapter(new ArrayList<>(), getParentFragmentManager());
        binding.recyclerAllJobs.setAdapter(adapter);

        // Reset FAB animation state
        isFirstFabShow = true;

        // Reset selected chip to ensure no chip is selected by default
        viewModel.resetSelectedChip();

        setupListeners();
        observeViewModel();

        // Set "All Category" as default tab and load all jobs
        viewModel.setActiveTab("all", requireContext());
        if (viewModel.getJobs().getValue() == null || viewModel.getJobs().getValue().isEmpty()) {
            viewModel.loadJobs(client, JOB_UPDATES_ENDPOINT, requireContext());
        } else {
            viewModel.showAllJobs();
        }
        viewModel.restoreFilter(requireContext());

        return binding.getRoot();
    }

    private void setupListeners() {
        binding.tabForYou.setOnClickListener(v -> {
            System.out.println("AllCategory: Switching to 'forYou' tab");
            viewModel.setActiveTab("forYou", requireContext());
            scrollToTop();
        });
        binding.tabAllCategory.setOnClickListener(v -> {
            System.out.println("AllCategory: Switching to 'all' tab");
            viewModel.setActiveTab("all", requireContext());
            scrollToTop();
        });
        binding.chipGovernment.setOnClickListener(v -> viewModel.handleMainTypeChipClick("government", requireContext()));
        binding.chipBanking.setOnClickListener(v -> viewModel.handleMainTypeChipClick("banking", requireContext()));
        binding.chipPrivate.setOnClickListener(v -> viewModel.handleMainTypeChipClick("private", requireContext()));
        binding.chipEducation.setOnClickListener(v -> showEducationDialog());
        binding.retryButton.setOnClickListener(v -> {
            viewModel.loadJobs(client, JOB_UPDATES_ENDPOINT, requireContext());
            scrollToTop();
        });

        // FAB visibility based on NestedScrollView scroll
        binding.scrollContainer.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > 500) { // Show FAB after scrolling 500 pixels down
                binding.fabScrollTop.show();
                animationHandler.removeCallbacks(animationRunnable);
                long delay = isFirstFabShow ? 3000 : 2000;
                animationHandler.postDelayed(animationRunnable, delay);
                isFirstFabShow = false;
            } else {
                binding.fabScrollTop.hide();
                binding.fabScrollTop.clearAnimation();
                animationHandler.removeCallbacks(animationRunnable);
            }
        });

        // RecyclerView scroll listener for pagination only
        binding.recyclerAllJobs.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    if (!viewModel.isLoading() && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        viewModel.loadMoreJobs(client, JOB_UPDATES_ENDPOINT, requireContext());
                    }
                }
            }
        });

        binding.fabScrollTop.setOnClickListener(v -> scrollToTop());
    }

    private void scrollToTop() {
        if (binding.scrollContainer != null) {
            binding.scrollContainer.smoothScrollTo(0, 0);
        }
        if (!binding.recyclerAllJobs.isComputingLayout()) {
            binding.recyclerAllJobs.scrollToPosition(0);
        } else {
            binding.recyclerAllJobs.post(() -> binding.recyclerAllJobs.scrollToPosition(0));
        }
    }

    private void observeViewModel() {
        viewModel.getJobs().observe(getViewLifecycleOwner(), jobs -> {
            if (!binding.recyclerAllJobs.isComputingLayout()) {
                System.out.println("AllCategory: Observed jobs size: " + (jobs != null ? jobs.size() : 0));
                adapter.updateJobs(jobs);
                binding.emptyStateContainer.setVisibility(jobs != null && jobs.isEmpty() && !viewModel.isLoading() ? View.VISIBLE : View.GONE);
                binding.recyclerAllJobs.setVisibility(jobs != null && jobs.isEmpty() && !viewModel.isLoading() ? View.GONE : View.VISIBLE);
                scrollToTop();
            }
        });

        viewModel.getActiveTab().observe(getViewLifecycleOwner(), tab -> {
            Drawable forYouDrawable = binding.tabForYou.getBackground();
            Drawable allDrawable = binding.tabAllCategory.getBackground();
            boolean isTransitionDrawable = forYouDrawable instanceof TransitionDrawable && allDrawable instanceof TransitionDrawable;

            System.out.println("AllCategory: Active tab changed to: " + tab);
            if ("all".equals(tab)) {
                binding.chipsContainer.setVisibility(View.VISIBLE);
                binding.tabAllCategory.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                binding.tabForYou.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                if (isTransitionDrawable) {
                    TransitionDrawable allBg = (TransitionDrawable) allDrawable;
                    TransitionDrawable forYouBg = (TransitionDrawable) forYouDrawable;
                    allBg.startTransition(300);
                    forYouBg.reverseTransition(300);
                } else {
                    binding.tabAllCategory.setBackgroundResource(R.drawable.tab_selected);
                    binding.tabForYou.setBackgroundResource(R.drawable.tab_unselected);
                }
            } else {
                binding.chipsContainer.setVisibility(View.GONE);
                binding.tabAllCategory.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                binding.tabForYou.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                if (isTransitionDrawable) {
                    TransitionDrawable allBg = (TransitionDrawable) allDrawable;
                    TransitionDrawable forYouBg = (TransitionDrawable) forYouDrawable;
                    allBg.reverseTransition(300);
                    forYouBg.startTransition(300);
                } else {
                    binding.tabAllCategory.setBackgroundResource(R.drawable.tab_unselected);
                    binding.tabForYou.setBackgroundResource(R.drawable.tab_selected);
                }
            }
        });

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            adapter.setLoading(isLoading && adapter.getItemCount() > 0);
            binding.loadingProgress.setVisibility(isLoading && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            binding.recyclerAllJobs.setVisibility(isLoading && adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            binding.emptyStateContainer.setVisibility(adapter.getItemCount() == 0 && !isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                binding.emptyStateContainer.setVisibility(View.VISIBLE);
                binding.retryButton.setVisibility(View.VISIBLE);
                binding.recyclerAllJobs.setVisibility(View.GONE);
                binding.loadingProgress.setVisibility(View.GONE);
            }
        });

        viewModel.getSelectedChip().observe(getViewLifecycleOwner(), selected -> {
            updateChipColors(selected);
            scrollToTop();
        });
    }

    private void updateChipColors(String selected) {
        int white = ContextCompat.getColor(requireContext(), android.R.color.white);
        int yellow = ContextCompat.getColor(requireContext(), R.color.yellow);
        int green = ContextCompat.getColor(requireContext(), R.color.green);

        binding.chipGovernment.setCardBackgroundColor(white);
        binding.chipBanking.setCardBackgroundColor(white);
        binding.chipPrivate.setCardBackgroundColor(white);
        binding.chipEducation.setCardBackgroundColor(white);

        if (selected != null) {
            MaterialCardView selectedChip = getChipForCategory(selected);
            if (selectedChip != null) {
                selectedChip.setCardBackgroundColor(yellow);
            }
        }
    }

    private MaterialCardView getChipForCategory(String category) {
        if (category == null) return null;
        switch (category) {
            case "government":
                return binding.chipGovernment;
            case "banking":
                return binding.chipBanking;
            case "private":
                return binding.chipPrivate;
            case "education":
                return binding.chipEducation;
            default:
                return null;
        }
    }

    private void showEducationDialog() {
        if (!isAdded()) return;

        if (educationDialog == null) {
            dialogBinding = DialogEducationFilterBinding.inflate(LayoutInflater.from(requireContext()));
            educationDialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogBinding.getRoot())
                    .create();

            // ✅ Apply rounded-corner background
            if (educationDialog.getWindow() != null) {
                educationDialog.getWindow().setBackgroundDrawableResource(R.drawable.rectangle_white);
                educationDialog.getWindow().setElevation(16f); // Optional: adds shadow for Material look
                educationDialog.getWindow().getDecorView().setPadding(40, 40, 40, 40);
            }

            // Setup spinners
            ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, FilterUtils.getEducationOptions());
            educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dialogBinding.spinnerEducation.setAdapter(educationAdapter);

            ArrayAdapter<String> defaultDegreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, Arrays.asList("Select Degree"));
            ArrayAdapter<String> defaultPostGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, Arrays.asList("Select Post Graduation"));
            dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
            dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);

            dialogBinding.spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String category = FilterUtils.getEducationOptions().get(position);
                    if (!category.equals("Select Education Category")) {
                        List<String> degrees = FilterUtils.getDegreeMap().getOrDefault(category, Arrays.asList("Select Degree", "Other"));
                        ArrayAdapter<String> degreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, degrees);
                        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerDegree.setAdapter(degreeAdapter);

                        List<String> postGrads = FilterUtils.getPostGradMap().getOrDefault(category, Arrays.asList("Select Post Graduation", "None"));
                        ArrayAdapter<String> postGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, postGrads);
                        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerPostGrad.setAdapter(postGradAdapter);
                    } else {
                        dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
                        dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
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

            educationDialog.setOnShowListener(d -> binding.overlay.setVisibility(View.VISIBLE));
            educationDialog.setOnDismissListener(d -> binding.overlay.setVisibility(View.GONE));
        }

        // Restore saved filter selections
        JobViewModel.Filter currentFilter = viewModel.getFilter().getValue();
        if (currentFilter != null && !currentFilter.getCategory().isEmpty() && !currentFilter.getCategory().equals("Select Education Category")) {
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
                    }
                }
                ArrayAdapter<String> postGradAdapter = (ArrayAdapter<String>) dialogBinding.spinnerPostGrad.getAdapter();
                if (postGradAdapter != null && !currentFilter.getPostGrad().isEmpty()) {
                    int postGradPosition = postGradAdapter.getPosition(currentFilter.getPostGrad());
                    if (postGradPosition != -1) {
                        dialogBinding.spinnerPostGrad.setSelection(postGradPosition);
                    }
                }
            }
        }

        educationDialog.show();
    }


    @Override
    public void onPause() {
        super.onPause();
        binding.overlay.setVisibility(View.GONE);
        if (educationDialog != null && educationDialog.isShowing()) {
            educationDialog.dismiss();
        }
        if (binding != null) {
            binding.fabScrollTop.clearAnimation();
        }
        animationHandler.removeCallbacks(animationRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.fabScrollTop.clearAnimation();
        }
        animationHandler.removeCallbacks(animationRunnable);
        binding = null;
        dialogBinding = null;
        educationDialog = null;
        client.dispatcher().cancelAll();
        executor.shutdown();
    }
}