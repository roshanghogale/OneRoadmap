package com.newsproject.oneroadmap.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.newsproject.oneroadmap.Adapters.CurrentAffairsAdapter;
import com.newsproject.oneroadmap.Adapters.NewsAdapter;
import com.newsproject.oneroadmap.Adapters.RecentlyOpenedAdapter;
import com.newsproject.oneroadmap.Adapters.StoriesAdapter;
import com.newsproject.oneroadmap.Adapters.StoryAdapter;
import com.newsproject.oneroadmap.Adapters.StudentUpdateAdapter;
import com.newsproject.oneroadmap.Adapters.StudyMaterialAdapter;
import com.newsproject.oneroadmap.Models.CurrentAffairs;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.Models.News;
import com.newsproject.oneroadmap.Models.RecentlyOpenedItem;
import com.newsproject.oneroadmap.Models.Slider;
import com.newsproject.oneroadmap.Models.Story;
import com.newsproject.oneroadmap.Models.StudentUpdateItem;
import com.newsproject.oneroadmap.Models.StudyMaterial;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.BuildConfig;
import com.newsproject.oneroadmap.Models.JobViewModel;
import com.newsproject.oneroadmap.Utils.NewsUtils;
import com.newsproject.oneroadmap.database.RecentlyOpenedDatabaseHelper;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {
    private LinearLayout saveButton;
    private RecyclerView storyRecycler;
    private RecyclerView currentAffairsRecycler;
    private static RecyclerView storiesPlayer;
    private RecyclerView recentlyOpenedRecycler;
    private RecyclerView studentUpdatesRecycler;
    private RecyclerView recyclerStudyMaterial;
    private LinearLayout recentRecycler;
    private ScrollView mainScrollView;
    private ImageCarousel carousel;
    private StoryAdapter storyAdapter;
    private CurrentAffairsAdapter currentAffairsAdapter;
    private NewsAdapter newsAdapter;
    private RecentlyOpenedAdapter recentlyOpenedAdapter;
    private StudentUpdateAdapter studentAdapter1, studentAdapter2, studentAdapter3, studentAdapter4, studentAdapter5;
    private StudyMaterialAdapter studyMaterialAdapter;
    private List<Story> storyList = new ArrayList<>();
    private List<CurrentAffairs> currentAffairsList = new ArrayList<>();
    private List<JobUpdate> jobUpdatesList = new ArrayList<>();
    private List<News> newsList = new ArrayList<>();
    private List<RecentlyOpenedItem> recentlyOpenedList = new ArrayList<>();
    private List<StudentUpdateItem> studentUpdatesList = new ArrayList<>();
    private List<CarouselItem> carouselItemsList = new ArrayList<>();
    private List<StudyMaterial> studyMaterialsAll = new ArrayList<>();
    private List<StudyMaterial> currentStudyMaterials = new ArrayList<>();
    private CardView bankJobs, privateJobs, governmentJobs;
    private ImageView admitCard;
    private CircleImageView profileIcon;
    private TextView allStudentUpdates, userName;
    private SharedPreferences storyPrefs;
    private FirebaseFirestore db;
    private FirebaseStorage firebaseStorage;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = "HomeFragmentRunning";
    private boolean hasShownStoriesErrorToast = false;
    private boolean hasShownCurrentAffairsErrorToast = false;
    private boolean hasShownJobUpdatesErrorToast = false;
    private boolean hasShownStudentUpdatesErrorToast = false;
    private boolean hasShownRecentlyOpenedErrorToast = false;
    private boolean hasShownNewsErrorToast = false;
    private boolean hasShownCarouselItemsErrorToast = false;
    private boolean hasShownStudyMaterialsErrorToast = false;
    private boolean hasShownTop5JobsErrorToast = false;
    private JobViewModel jobViewModel;
    private OkHttpClient client;
    private LinearLayout govLinear, policeLinear, bankLinear, selfLinear;
    private Map<String, News> newsCache = new HashMap<>(); // Cache for news items
    private String top5PdfUrl = "";
    private RecentlyOpenedDatabaseHelper recentDb;
    private static ActivityResultLauncher<Intent> storyShareLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        Log.d(TAG, "ExecutorService created in onCreate");
        
        // Initialize share launcher for stories
        storyShareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // When user returns from sharing, add coins
                    SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    String userId = prefs.getString("userId", "");
                    if (userId != null && !userId.isEmpty()) {
                        com.newsproject.oneroadmap.Utils.DatabaseHelper dbHelper = new com.newsproject.oneroadmap.Utils.DatabaseHelper(requireContext());
                        int current = dbHelper.getUserCoins(userId);
                        com.newsproject.oneroadmap.Utils.CoinManager coinManager = new com.newsproject.oneroadmap.Utils.CoinManager(requireContext(), userId);
                        coinManager.addCoinsForShare(newCoins -> {
                            // Coins added successfully
                            //Toast.makeText(requireContext(), "You earned 100 coins!", Toast.LENGTH_SHORT).show();
                        });
                    }
                });

        // Initialize OkHttpClient
        int cacheSize = 10 * 1024 * 1024; // 10 MB cache
        Cache cache = new Cache(requireContext().getCacheDir(), cacheSize);
        client = new OkHttpClient.Builder()
                .cache(cache)
                .build();

        // Initialize JobViewModel
        jobViewModel = new ViewModelProvider(this).get(JobViewModel.class);
    }

// =====================
// STORY PLAYER HELPERS
// =====================

// =====================
// STORY PLAYER HELPERS
// =====================

    public static boolean isStoriesPlayerVisible() {
        return storiesPlayer != null && storiesPlayer.getVisibility() == View.VISIBLE;
    }

// =========================
// ONLY STORY-RELATED PARTS SHOWN FULLY
// REST OF YOUR FILE REMAINS EXACTLY SAME
// =========================

    public static void playStory(
            Context context,
            int startPosition,
            List<Story> storyList,
            StoryAdapter storyAdapter
    ) {
        if (storiesPlayer == null || storyList == null || storyList.isEmpty()) return;

        storiesPlayer.setVisibility(View.VISIBLE);

        StoriesAdapter storiesAdapter =
                new StoriesAdapter(context, storyList, storyAdapter, storiesPlayer, storyShareLauncher);

        storiesPlayer.setAdapter(storiesAdapter);
        storiesAdapter.setCurrentVisiblePosition(startPosition);

        LinearLayoutManager lm =
                (LinearLayoutManager) storiesPlayer.getLayoutManager();

        if (lm != null) {
            lm.scrollToPosition(startPosition);
        }

        // 🔥 FORCE FIRST STORY TO START (CRITICAL FIX)
        storiesPlayer.post(() -> {
            RecyclerView.ViewHolder vh =
                    storiesPlayer.findViewHolderForAdapterPosition(startPosition);

            if (vh instanceof StoriesAdapter.StoryViewHolder) {
                ((StoriesAdapter.StoryViewHolder) vh).onBecameVisible();
            }
        });

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        if (storiesPlayer.getOnFlingListener() == null) {
            snapHelper.attachToRecyclerView(storiesPlayer);
        }

        storiesPlayer.clearOnScrollListeners();
        storiesPlayer.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(
                    @NonNull RecyclerView rv,
                    int newState
            ) {

                // 🔥 STOP ALL WHEN SWIPE STARTS
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    for (int i = 0; i < rv.getChildCount(); i++) {
                        RecyclerView.ViewHolder vh =
                                rv.getChildViewHolder(rv.getChildAt(i));
                        if (vh instanceof StoriesAdapter.StoryViewHolder) {
                            ((StoriesAdapter.StoryViewHolder) vh).onBecameInvisible();
                        }
                    }
                    return;
                }

                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;

                View snapView = snapHelper.findSnapView(rv.getLayoutManager());
                if (snapView == null) return;

                int snappedPos = lm.getPosition(snapView);
                if (snappedPos == RecyclerView.NO_POSITION) return;

                storiesAdapter.setCurrentVisiblePosition(snappedPos);

                // ▶️ START ONLY SNAPPED STORY
                for (int i = 0; i < rv.getChildCount(); i++) {
                    RecyclerView.ViewHolder vh =
                            rv.getChildViewHolder(rv.getChildAt(i));

                    if (vh instanceof StoriesAdapter.StoryViewHolder) {
                        StoriesAdapter.StoryViewHolder holder =
                                (StoriesAdapter.StoryViewHolder) vh;

                        if (holder.getAdapterPosition() == snappedPos) {
                            holder.onBecameVisible();
                        } else {
                            holder.onBecameInvisible();
                        }
                    }
                }
            }
        });

        // Hide bottom navigation
        if (context instanceof FragmentActivity) {
            View bottom =
                    ((FragmentActivity) context)
                            .findViewById(R.id.bottom_navigation);
            if (bottom != null) bottom.setVisibility(View.GONE);
        }
    }

    public static void stopStory(Context context) {
        if (storiesPlayer == null) return;

        RecyclerView.Adapter adapter = storiesPlayer.getAdapter();

        if (adapter instanceof StoriesAdapter) {
            for (int i = 0; i < storiesPlayer.getChildCount(); i++) {
                RecyclerView.ViewHolder vh =
                        storiesPlayer.getChildViewHolder(
                                storiesPlayer.getChildAt(i)
                        );

                if (vh instanceof StoriesAdapter.StoryViewHolder) {
                    ((StoriesAdapter.StoryViewHolder) vh).onBecameInvisible();
                }
            }
        }

        storiesPlayer.setAdapter(null);
        storiesPlayer.setVisibility(View.GONE);

        if (context instanceof FragmentActivity) {
            View bottom =
                    ((FragmentActivity) context)
                            .findViewById(R.id.bottom_navigation);
            if (bottom != null) bottom.setVisibility(View.VISIBLE);
        }
    }

    public static void updateAdapter(
            Context context,
            int position,
            StoryAdapter storyAdapter
    ) {
        if (storiesPlayer == null) return;

        if (position + 1 < storyAdapter.getItemCount()) {
            storiesPlayer.smoothScrollToPosition(position + 1);
        } else {
            stopStory(context);
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
        db = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storyPrefs = requireContext().getSharedPreferences("StoryPrefs", Context.MODE_PRIVATE);
        recentDb = new RecentlyOpenedDatabaseHelper(requireContext());
        initViews(view);
        loadData();

        // Set bottom margins based on Android version
        setBottomMarginsBasedOnAndroidVersion();

        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bottom_navigation_light));
        Log.d(TAG, "Set bottom navigation background color");
    }

    private void initViews(View view) {
        Log.d(TAG, "initViews called");
        saveButton = view.findViewById(R.id.save_button);
        mainScrollView = view.findViewById(R.id.main_scroll);
        storyRecycler = view.findViewById(R.id.story_recycler);
        currentAffairsRecycler = view.findViewById(R.id.current_affairs_recycler);
        storiesPlayer = view.findViewById(R.id.storiesPlayer);
        recentRecycler = view.findViewById(R.id.recent_recycler);
        recentlyOpenedRecycler = view.findViewById(R.id.recently_opened_recycler);
        studentUpdatesRecycler = view.findViewById(R.id.student_updates_recycler);
        recyclerStudyMaterial = view.findViewById(R.id.free_study_material_recycler);
        bankJobs = view.findViewById(R.id.card_all_bank_jobs);
        governmentJobs = view.findViewById(R.id.card_all_govt_jobs);
        privateJobs = view.findViewById(R.id.card_all_private_jobs);
        carousel = view.findViewById(R.id.carousel);
        admitCard = view.findViewById(R.id.result_hallticket);
        profileIcon = view.findViewById(R.id.profile_image);
        allStudentUpdates = view.findViewById(R.id.all_student_updates);
        userName = view.findViewById(R.id.user_name);

        // Locate the four category linears
        LinearLayout studyCards = view.findViewById(R.id.study_material_cards_linear);
        LinearLayout row1 = (LinearLayout) studyCards.getChildAt(1); // Index 0 is header, 1 is first row
        govLinear = (LinearLayout) row1.getChildAt(0);
        policeLinear = (LinearLayout) row1.getChildAt(1);
        LinearLayout row2 = (LinearLayout) studyCards.getChildAt(2); // Second row
        bankLinear = (LinearLayout) row2.getChildAt(0);
        selfLinear = (LinearLayout) row2.getChildAt(1);

        // Load user data from SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userNameText = sharedPreferences.getString("userName", "Guest");
        String[] userNames = userNameText.split(" ");
        userName.setText(userNames[0]);
        Log.d(TAG, "Set userName text: " + userNames[0]);

        // Load avatar from SharedPreferences
        String avatarName = sharedPreferences.getString("avatar", "");
        if (!avatarName.isEmpty()) {
            int avatarResId = getResources().getIdentifier(avatarName, "drawable", requireContext().getPackageName());
            if (avatarResId != 0) { // Check if the resource ID is valid
                Glide.with(requireContext())
                        .load(avatarResId)
                        .placeholder(R.mipmap.ic_launcher_round) // Default placeholder
                        .error(R.mipmap.ic_launcher_round) // Fallback if loading fails
                        .into(profileIcon);
                Log.d(TAG, "Loaded avatar from SharedPreferences: " + avatarName + " (ID: " + avatarResId + ")");
            } else {
                // Resource not found, use default image
                profileIcon.setImageResource(R.mipmap.ic_launcher_round);
                Log.e(TAG, "Avatar resource not found for name: " + avatarName);
            }
        } else {
            // No avatar name in SharedPreferences, use default image
            profileIcon.setImageResource(R.mipmap.ic_launcher_round);
            Log.d(TAG, "No avatar found in SharedPreferences, using default image");
        }

        // Setup RecyclerViews
        LinearLayoutManager storyLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        storyRecycler.setLayoutManager(storyLayoutManager);
        storyAdapter = new StoryAdapter(getContext(), storyList);
        storyRecycler.setAdapter(storyAdapter);
        Log.d(TAG, "Set up storyRecycler");

        LinearLayoutManager currentAffairsLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        currentAffairsRecycler.setLayoutManager(currentAffairsLayoutManager);
        currentAffairsAdapter = new CurrentAffairsAdapter(currentAffairsList, getContext(), pdfUrl -> {
            openPdfViewer(pdfUrl);
        });
        currentAffairsRecycler.setAdapter(currentAffairsAdapter);
        Log.d(TAG, "Set up currentAffairsRecycler");

        LinearLayoutManager recentlyOpenedLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recentlyOpenedRecycler.setLayoutManager(recentlyOpenedLayoutManager);
        newsAdapter = new NewsAdapter(newsList, getContext());
        recentlyOpenedRecycler.setAdapter(newsAdapter);
        Log.d(TAG, "Set up recentlyOpenedRecycler with NewsAdapter");

        LinearLayoutManager studentUpdatesLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        studentUpdatesRecycler.setLayoutManager(studentUpdatesLayoutManager);
        studentAdapter1 = new StudentUpdateAdapter(studentUpdatesList, getContext());
        studentUpdatesRecycler.setAdapter(studentAdapter1);
        Log.d(TAG, "Set up studentUpdatesRecycler");

        recyclerStudyMaterial.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        studyMaterialAdapter = new StudyMaterialAdapter(currentStudyMaterials, getContext(), pdfUrl -> {
            openPdfViewer(pdfUrl);
        });
        recyclerStudyMaterial.setAdapter(studyMaterialAdapter);
        Log.d(TAG, "Set up recyclerStudyMaterial");

        LinearLayoutManager storiesPlayerLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        storiesPlayer.setLayoutManager(storiesPlayerLayoutManager);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(storiesPlayer);
        Log.d(TAG, "Set up storiesPlayer");

        setupClickListeners();
        Log.d(TAG, "Click listeners set up");
    }

    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners called");
        saveButton.setOnClickListener(v -> {
            // No JobUpdate or DBHelper needed here – we just open the list
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SavedJobsFragment())
                    .addToBackStack(null)          // optional – lets user press Back
                    .commit();
        });

        allStudentUpdates.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new AllBannersList());
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to AllBannersList");
        });

        // Add click listener for student updates
        studentAdapter1.setOnItemClickListener(item -> {
            showStudentUpdateDialog(item);
        });

        admitCard.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new Result_HallTitcket());
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to Result_HallTitcket");
        });

        bankJobs.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new BankingJobs());
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to BankingJobs");
        });

        privateJobs.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new PrivateJobs());
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to PrivateJobs");
        });

        governmentJobs.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new GovernmentJobs());
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to GovernmentJobs");
        });

        profileIcon.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new ProfileFragment());
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to ProfileFragment");
        });

        // Category click listeners with consistent type strings
        govLinear.setOnClickListener(v -> {
            Log.d(TAG, "Category clicked: government");
            filterAndDisplay("government");
        });
        policeLinear.setOnClickListener(v -> {
            Log.d(TAG, "Category clicked: police_defence");
            filterAndDisplay("police_defence");
        });
        bankLinear.setOnClickListener(v -> {
            Log.d(TAG, "Category clicked: banking");
            filterAndDisplay("banking");
        });
        selfLinear.setOnClickListener(v -> {
            Log.d(TAG, "Category clicked: self_improvement");
            filterAndDisplay("self_improvement");
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadData() {
        Log.d(TAG, "loadData called");
        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            Log.w(TAG, "ExecutorService is shutdown or terminated, creating new one");
            executorService = Executors.newSingleThreadExecutor();
        }
        try {
            executorService.execute(() -> {
                loadStories();
                loadCurrentAffairsData();
                loadStudentUpdates();
                loadNews();
                loadCarouselItems();
                loadStudyMaterials();
            });
            // Load jobs on the main thread and initialize observer
            if (isNetworkAvailable()) {
                jobViewModel.loadJobs(client, BuildConfig.JOB_UPDATES_ENDPOINT, requireContext());
                loadJobUpdates(); // Ensure observer is set up
            } else {
                mainHandler.post(() -> {
                    if (!hasShownJobUpdatesErrorToast) {
                        Toast.makeText(getContext(), "No network, cannot load job updates", Toast.LENGTH_SHORT).show();
                        hasShownJobUpdatesErrorToast = true;
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error submitting task to ExecutorService", e);
            mainHandler.post(() -> {
                Toast.makeText(getContext(), "Failed to load data due to internal error", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadStories() {
        Log.d(TAG, "loadStories started (API)");

        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available, loading dummy stories");
            List<Story> dummyStories = new ArrayList<>();
            dummyStories.add(new Story(
                    "dummy1",
                    "Dummy Story",
                    null,
                    "https://example.com/dummy.jpg",
                    "https://example.com/icon.jpg",
                    false,
                    false
            ));

            mainHandler.post(() -> {
                storyList.clear();
                storyList.addAll(dummyStories);
                storyAdapter.notifyDataSetChanged();

                for (Story s : storyList) {
                    Log.d("STORY_FINAL_DUMMY",
                            "id=" + s.getDocumentId()
                                    + ", type=" + s.getType()
                                    + ", webUrl=" + s.getWebUrl()
                                    + ", isMain=" + s.isMainStory()
                    );
                }
            });
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userEducation = prefs.getString("education", "");
        String userDegree = prefs.getString("degree", "");
        String userPostGrad = prefs.getString("postGraduation", "");
        String userDistrict = prefs.getString("district", "");
        String userTaluka = prefs.getString("taluka", "");

        String url = BuildConfig.BASE_URL + "/api/stories";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch stories", e);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Stories API failed: " + response.code());
                    return;
                }

                String body = response.body().string();
                JsonObject root = new Gson().fromJson(body, JsonObject.class);

                List<Story> mains = new ArrayList<>();
                List<Story> others = new ArrayList<>();

                if (root != null && root.has("stories")) {
                    JsonArray arr = root.getAsJsonArray("stories");

                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject o = arr.get(i).getAsJsonObject();

                        String id = getString(o, "id");
                        String title = getString(o, "title");
                        String iconUrlRaw = getString(o, "icon_url");
                        String bannerUrlRaw = getString(o, "banner_url");
                        String videoUrlRaw = getString(o, "video_url");
                        String mediaType = getString(o, "media_type", "image");
                        String type = getString(o, "type");
                        String postDocumentId = getString(o, "post_document_id");
                        String webUrl = getString(o, "web_url");
                        String createdAt = getString(o, "created_at");
                        boolean isMain = o.has("is_main_story") && o.get("is_main_story").getAsBoolean();

                        // 🔴 LOG RAW API DATA
                        Log.d("STORY_RAW_API",
                                "id=" + id
                                        + ", type=" + type
                                        + ", web_url=" + webUrl
                                        + ", banner_url=" + bannerUrlRaw
                                        + ", video_url=" + videoUrlRaw
                        );

                        String iconUrl = buildFullUrl(iconUrlRaw);
                        String bannerUrl = buildFullUrl(bannerUrlRaw);
                        String videoUrl = buildFullUrl(videoUrlRaw);

                        // 🟡 LOG AFTER URL BUILDING
                        Log.d("STORY_URL_BUILT",
                                "id=" + id
                                        + ", icon=" + iconUrl
                                        + ", banner=" + bannerUrl
                                        + ", video=" + videoUrl
                        );

                        if (iconUrl == null || (bannerUrl == null && videoUrl == null)) continue;

                        boolean viewed = id != null && storyPrefs.getBoolean("viewed_" + id, false);
                        String imageUrlForStory =
                                "video".equalsIgnoreCase(mediaType) && videoUrl != null
                                        ? videoUrl
                                        : bannerUrl;

                        Story story = new Story(id, title, null, imageUrlForStory, iconUrl, isMain, viewed);
                        story.setType(type);
                        story.setPostDocumentId(postDocumentId);
                        story.setWebUrl(webUrl);
                        story.setBannerUrl(bannerUrl);
                        story.setVideoUrl(videoUrl);
                        story.setMediaType(mediaType);

                        if (createdAt != null) {
                            story.setRelativeTime(computeRelativeFromString(createdAt));
                        }

                        // 🟢 LOG FINAL STORY OBJECT
                        Log.d("STORY_FINAL_OBJ",
                                "id=" + story.getDocumentId()
                                        + ", type=" + story.getType()
                                        + ", webUrl=" + story.getWebUrl()
                                        + ", image=" + story.getImageUrl()
                        );

                        boolean eduOk = true;
                        boolean districtOk = true;
                        boolean talukaOk = true;

                        if (eduOk && districtOk && talukaOk) {
                            if (isMain) mains.add(story);
                            else others.add(story);
                        }
                    }
                }

                Collections.sort(mains, (a, b) -> Long.compare(b.getCreatedAtTimestamp(), a.getCreatedAtTimestamp()));
                Collections.sort(others, (a, b) -> Long.compare(b.getCreatedAtTimestamp(), a.getCreatedAtTimestamp()));

                mainHandler.post(() -> {
                    storyList.clear();
                    storyList.addAll(mains);
                    storyList.addAll(others);
                    storyAdapter.notifyDataSetChanged();

                    Log.d(TAG,
                            "Stories loaded → total="
                                    + storyList.size()
                                    + ", mains=" + mains.size()
                                    + ", others=" + others.size()
                    );
                });
            }
        });
    }

    /** Helper */
    private String getString(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private String getString(JsonObject o, String key, String def) {
        String v = getString(o, key);
        return v != null ? v : def;
    }


    private void loadCurrentAffairsData() {
        Log.d(TAG, "loadCurrentAffairsData started (API)");
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available, skipping current affairs load");
            mainHandler.post(() -> {
                if (!hasShownCurrentAffairsErrorToast) {
                    Toast.makeText(getContext(), "No network, cannot load current affairs", Toast.LENGTH_SHORT).show();
                    hasShownCurrentAffairsErrorToast = true;
                }
            });
            return;
        }

        String url = BuildConfig.BASE_URL + "/api/current-affairs";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch current affairs: " + e.getMessage());
                mainHandler.post(() -> {
                    if (!hasShownCurrentAffairsErrorToast) {
                        Toast.makeText(getContext(), "Failed to load current affairs", Toast.LENGTH_SHORT).show();
                        hasShownCurrentAffairsErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected response: " + response.code());
                    mainHandler.post(() -> {
                        if (!hasShownCurrentAffairsErrorToast) {
                            Toast.makeText(getContext(), "Failed to load current affairs", Toast.LENGTH_SHORT).show();
                            hasShownCurrentAffairsErrorToast = true;
                        }
                    });
                    return;
                }

                String body = response.body().string();
                Log.d(TAG, "Current affairs API response length: " + body.length());
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<CurrentAffairs> fetched = new ArrayList<>();
                    if (root != null && root.has("currentAffairs")) {
                        JsonArray arr = root.getAsJsonArray("currentAffairs");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.get(i).getAsJsonObject();
                            String dateRaw = o.has("date") && !o.get("date").isJsonNull() ? o.get("date").getAsString() : null;
                            String date = dateRaw != null && dateRaw.length() >= 10 ? dateRaw.substring(0, 10) : dateRaw; // yyyy-MM-dd
                            String imageUrl = o.has("image_url") && !o.get("image_url").isJsonNull() ? o.get("image_url").getAsString() : null;
                            String pdfUrl = o.has("pdf_url") && !o.get("pdf_url").isJsonNull() ? o.get("pdf_url").getAsString() : null;
                            imageUrl = buildFullUrl(imageUrl);
                            pdfUrl = buildFullUrl(pdfUrl);
                            if (date == null || date.trim().isEmpty() || imageUrl == null || imageUrl.isEmpty() || pdfUrl == null || pdfUrl.isEmpty()) {
                                continue;
                            }
                            fetched.add(new CurrentAffairs(date, imageUrl, pdfUrl));
                        }
                    }

                    final List<CurrentAffairs> finalFetched = fetched;
                    mainHandler.post(() -> {
                        currentAffairsList.clear();
                        currentAffairsList.addAll(finalFetched);
                        currentAffairsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Current affairs loaded (API), count: " + currentAffairsList.size());
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse current affairs API response: " + e.getMessage());
                    mainHandler.post(() -> {
                        if (!hasShownCurrentAffairsErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse current affairs", Toast.LENGTH_SHORT).show();
                            hasShownCurrentAffairsErrorToast = true;
                        }
                    });
                }
            }
        });
    }

    private void openPdfViewer(String pdfUrl) {
        PDFViewerFragment pdfFragment = PDFViewerFragment.newInstance(pdfUrl);
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_up,      // enter
                        R.anim.fade_out,         // exit
                        R.anim.fade_in,          // popEnter
                        R.anim.slide_out_down    // popExit
                )
                .replace(R.id.fragment_container, pdfFragment)  // your container ID
                .addToBackStack("pdf_viewer")
                .commit();
    }

    private String buildFullUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        String url = filePath.startsWith("http") ? filePath : BuildConfig.BASE_URL + filePath;
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        return url;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private long parseCreatedAtTimestamp(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) return 0;

        Long epochMillis = null;
        // 1) Try ISO-8601 with offset or zone
        try {
            epochMillis = java.time.OffsetDateTime.parse(createdAt).toInstant().toEpochMilli();
        } catch (Exception ignored) {}

        if (epochMillis == null) {
            try {
                epochMillis = java.time.ZonedDateTime.parse(createdAt).toInstant().toEpochMilli();
            } catch (Exception ignored) {}
        }

        // 2) Try Instant
        if (epochMillis == null) {
            try {
                epochMillis = java.time.Instant.parse(createdAt).toEpochMilli();
            } catch (Exception ignored) {}
        }

        // 3) Try local datetime without zone, assume UTC
        if (epochMillis == null) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(createdAt, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                epochMillis = ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
            } catch (Exception ignored) {}
        }

        // 4) Try common custom patterns
        if (epochMillis == null) {
            String[] patterns = new String[] {
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
            };
            for (String p : patterns) {
                try {
                    java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(p).withZone(java.time.ZoneOffset.UTC);
                    epochMillis = java.time.Instant.from(f.parse(createdAt)).toEpochMilli();
                    break;
                } catch (Exception ignored) {}
            }
        }

        // 5) If it's just a number, treat as epoch seconds or millis
        if (epochMillis == null) {
            try {
                long numeric = Long.parseLong(createdAt.trim());
                if (createdAt.trim().length() <= 10) {
                    epochMillis = numeric * 1000L;
                } else {
                    epochMillis = numeric;
                }
            } catch (Exception ignored) {}
        }

        return epochMillis != null ? epochMillis : 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String computeRelativeFromString(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) return "";

        Long epochMillis = null;
        // 1) Try ISO-8601 with offset or zone
        try {
            epochMillis = java.time.OffsetDateTime.parse(createdAt).toInstant().toEpochMilli();
        } catch (Exception ignored) {}

        if (epochMillis == null) {
            try {
                epochMillis = java.time.ZonedDateTime.parse(createdAt).toInstant().toEpochMilli();
            } catch (Exception ignored) {}
        }

        // 2) Try Instant
        if (epochMillis == null) {
            try {
                epochMillis = java.time.Instant.parse(createdAt).toEpochMilli();
            } catch (Exception ignored) {}
        }

        // 3) Try local datetime without zone, assume UTC
        if (epochMillis == null) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(createdAt, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                epochMillis = ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
            } catch (Exception ignored) {}
        }

        // 4) Try common custom patterns
        if (epochMillis == null) {
            String[] patterns = new String[] {
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
            };
            for (String p : patterns) {
                try {
                    java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(p).withZone(java.time.ZoneOffset.UTC);
                    epochMillis = java.time.Instant.from(f.parse(createdAt)).toEpochMilli();
                    break;
                } catch (Exception ignored) {}
            }
        }

        // 5) If it's just a number, treat as epoch seconds or millis
        if (epochMillis == null) {
            try {
                long numeric = Long.parseLong(createdAt.trim());
                if (createdAt.trim().length() <= 10) {
                    epochMillis = numeric * 1000L;
                } else {
                    epochMillis = numeric;
                }
            } catch (Exception ignored) {}
        }

        if (epochMillis == null) return "";

        long now = System.currentTimeMillis();
        long diff = Math.max(0, now - epochMillis);
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (seconds < 60) return seconds + "s ago";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days < 7) return days + "d ago";
        return (days / 7) + "w ago";
    }

    private void loadJobUpdates() {
        Log.d(TAG, "loadJobUpdates started");
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available, skipping job updates load");
            mainHandler.post(() -> {
                if (!hasShownJobUpdatesErrorToast) {
                    Toast.makeText(getContext(), "No network, cannot load job updates", Toast.LENGTH_SHORT).show();
                    hasShownJobUpdatesErrorToast = true;
                }
            });
            return;
        }

        // Observe job updates
        jobViewModel.getJobs().observe(getViewLifecycleOwner(), jobs -> {
            if (jobs != null) {
                Log.d(TAG, "Job updates observed, total count: " + jobs.size());
                jobUpdatesList.clear();
                // Take the 5 most recent jobs (assuming sorted by timestamp descending)
                int limit = Math.min(5, jobs.size());
                jobUpdatesList.addAll(jobs.subList(0, limit));
                updateRecentRecycler();
                Log.d(TAG, "Loaded " + jobUpdatesList.size() + " recent job updates");
            } else {
                Log.w(TAG, "No job updates available");
                mainHandler.post(() -> {
                    if (!hasShownJobUpdatesErrorToast) {
                        Toast.makeText(getContext(), "No job updates available", Toast.LENGTH_SHORT).show();
                        hasShownJobUpdatesErrorToast = true;
                    }
                });
            }
        });

        jobViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.e(TAG, "Error loading job updates: " + message);
                mainHandler.post(() -> {
                    if (!hasShownJobUpdatesErrorToast) {
                        Toast.makeText(getContext(), "Failed to load job updates: " + message, Toast.LENGTH_SHORT).show();
                        hasShownJobUpdatesErrorToast = true;
                    }
                });
            }
        });
    }

    private void updateRecentRecycler() {
        Log.d(TAG, "updateRecentRecycler called");
        final List<JobUpdate> finalJobUpdatesList = new ArrayList<>(jobUpdatesList);
        mainHandler.post(() -> {
            // Check if fragment is still attached and views are available
            if (!isAdded() || getContext() == null || recentRecycler == null) {
                Log.w(TAG, "updateRecentRecycler: Fragment not attached or views null, skipping update");
                return;
            }
            
            recentRecycler.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (JobUpdate item : finalJobUpdatesList) {
                View itemView = inflater.inflate(R.layout.job_update_item, recentRecycler, false);
                TextView titleView = itemView.findViewById(R.id.title);
                TextView lastDate = itemView.findViewById(R.id.txt_lastDate);
                TextView timeAgo = itemView.findViewById(R.id.txt_time_ago);
                ImageView logo = itemView.findViewById(R.id.imageView18);

                titleView.setText(item.getTitle());
                lastDate.setText(item.getFormattedLastDateMarathi());
                timeAgo.setText(item.getTimeAgo());
                Glide.with(getContext()).load(item.getIconUrl()).into(logo);

                itemView.setOnClickListener(v -> {
                    if (!isAdded() || getContext() == null) {
                        return;
                    }
                    JobUpdateDetails fragment = JobUpdateDetails.newInstance(item); // Pass full JobUpdate object
                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment_container, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                    Log.d(TAG, "Navigated to JobUpdateDetails with job: " + item.getTitle());
                });

                recentRecycler.addView(itemView);
            }
            Log.d(TAG, "Updated recentRecycler with " + finalJobUpdatesList.size() + " items");
        });
    }

    private void loadStudentUpdates() {
        Log.d(TAG, "loadStudentUpdates started (API)");
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available, skipping student updates load");
            mainHandler.post(() -> {
                if (!hasShownStudentUpdatesErrorToast) {
                    Toast.makeText(getContext(), "No network, cannot load student updates", Toast.LENGTH_SHORT).show();
                    hasShownStudentUpdatesErrorToast = true;
                }
            });
            return;
        }

        String url = BuildConfig.BASE_URL + "/api/student-updates";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch student updates: " + e.getMessage());
                mainHandler.post(() -> {
                    if (!hasShownStudentUpdatesErrorToast) {
                        Toast.makeText(getContext(), "Failed to load student updates", Toast.LENGTH_SHORT).show();
                        hasShownStudentUpdatesErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected response: " + response.code());
                    mainHandler.post(() -> {
                        if (!hasShownStudentUpdatesErrorToast) {
                            Toast.makeText(getContext(), "Failed to load student updates", Toast.LENGTH_SHORT).show();
                            hasShownStudentUpdatesErrorToast = true;
                        }
                    });
                    return;
                }

                String body = response.body().string();
                Log.d(TAG, "Student updates API response length: " + body.length());
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<StudentUpdateItem> fetched = new ArrayList<>();
                    if (root != null && root.has("studentUpdates")) {
                        JsonArray arr = root.getAsJsonArray("studentUpdates");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.get(i).getAsJsonObject();
                            int id = o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsInt() : 0;
                            String title = o.has("title") && !o.get("title").isJsonNull() ? o.get("title").getAsString() : "";
                            String education = o.has("education") && !o.get("education").isJsonNull() ? o.get("education").getAsString() : "";
                            String ageRestriction = o.has("age_restriction") && !o.get("age_restriction").isJsonNull() ? o.get("age_restriction").getAsString() : "";
                            String applicationMethod = o.has("application_method") && !o.get("application_method").isJsonNull() ? o.get("application_method").getAsString() : "";
                            String description = o.has("description") && !o.get("description").isJsonNull() ? o.get("description").getAsString() : "";
                            String applicationLink = o.has("application_link") && !o.get("application_link").isJsonNull() ? o.get("application_link").getAsString() : "";
                            String lastDate = o.has("last_date") && !o.get("last_date").isJsonNull() ? o.get("last_date").getAsString() : "";
                            String imageUrl = o.has("image_url") && !o.get("image_url").isJsonNull() ? o.get("image_url").getAsString() : "";
                            String iconUrl = o.has("icon_url") && !o.get("icon_url").isJsonNull() ? o.get("icon_url").getAsString() : "";
                            String notificationPdfUrl = o.has("notification_pdf_url") && !o.get("notification_pdf_url").isJsonNull() ? o.get("notification_pdf_url").getAsString() : "";
                            String selectionPdfUrl = o.has("selection_pdf_url") && !o.get("selection_pdf_url").isJsonNull() ? o.get("selection_pdf_url").getAsString() : "";
                            String createdAt = o.has("created_at") && !o.get("created_at").isJsonNull() ? o.get("created_at").getAsString() : "";

                            imageUrl = buildFullUrl(imageUrl);
                            iconUrl = buildFullUrl(iconUrl);
                            notificationPdfUrl = buildFullUrl(notificationPdfUrl);
                            selectionPdfUrl = buildFullUrl(selectionPdfUrl);
                            applicationLink = buildFullUrl(applicationLink);

                            if (imageUrl == null || imageUrl.isEmpty()) continue;

                            fetched.add(new StudentUpdateItem(id, title, education, ageRestriction, applicationMethod,
                                    description, applicationLink, lastDate, imageUrl, iconUrl, notificationPdfUrl, selectionPdfUrl, createdAt));
                            Log.d(TAG, "student update image url " + imageUrl);
                        }
                    }

                    final List<StudentUpdateItem> finalFetched = fetched;
                    final StudentUpdateAdapter finalStudentAdapter1 = studentAdapter1;
                    final StudentUpdateAdapter finalStudentAdapter2 = studentAdapter2;
                    final StudentUpdateAdapter finalStudentAdapter3 = studentAdapter3;
                    final StudentUpdateAdapter finalStudentAdapter4 = studentAdapter4;
                    final StudentUpdateAdapter finalStudentAdapter5 = studentAdapter5;
                    mainHandler.post(() -> {
                        studentUpdatesList.clear();
                        studentUpdatesList.addAll(finalFetched);
                        if (finalStudentAdapter1 != null) finalStudentAdapter1.notifyDataSetChanged();
                        if (finalStudentAdapter2 != null) finalStudentAdapter2.notifyDataSetChanged();
                        if (finalStudentAdapter3 != null) finalStudentAdapter3.notifyDataSetChanged();
                        if (finalStudentAdapter4 != null) finalStudentAdapter4.notifyDataSetChanged();
                        if (finalStudentAdapter5 != null) finalStudentAdapter5.notifyDataSetChanged();
                        Log.d(TAG, "Student updates loaded (API), count: " + studentUpdatesList.size());
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse student updates API response: " + e.getMessage());
                    mainHandler.post(() -> {
                        if (!hasShownStudentUpdatesErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse student updates", Toast.LENGTH_SHORT).show();
                            hasShownStudentUpdatesErrorToast = true;
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // When fragment is not in foreground, stop all story playback
        try {
            stopStory(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop stories in onPause: " + e.getMessage(), e);
        }
    }

    public void showStudentUpdateDialog(@NonNull StudentUpdateItem item) {
        Context context = this.getContext();
        if (context == null) return;

        // Create Dialog with BlurDialogTheme
        Dialog dialog = new Dialog(context, R.style.BlurDialogTheme);
        dialog.setContentView(R.layout.student_update);

        // Set window attributes to match news dialog
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.dimAmount = 0.6f;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        dialog.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // Initialize views
        CircleImageView iconImageView = dialog.findViewById(R.id.circleImageView2);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        TextView titleText = dialog.findViewById(R.id.title_text);
        TextView educationValue = dialog.findViewById(R.id.education_requirement_value);
        TextView ageValue = dialog.findViewById(R.id.age_requirement_value);
        TextView descriptionText = dialog.findViewById(R.id.textView6);
        androidx.cardview.widget.CardView openLinkButton = dialog.findViewById(R.id.open_link_button);
        androidx.cardview.widget.CardView selectionPdfButton = dialog.findViewById(R.id.selection_pdf_button);
        
        // Close button click
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Load icon image
        if (item.getIconUrl() != null && !item.getIconUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getIconUrl())
                    .placeholder(R.drawable.app_logo)
                    .error(R.drawable.app_logo)
                    .into(iconImageView);
        } else {
            iconImageView.setImageResource(R.drawable.app_logo);
        }

        // Set text data
        titleText.setText(item.getTitle() != null ? item.getTitle() : "N/A");
        educationValue.setText(item.getEducation() != null ? item.getEducation() : "N/A");
        ageValue.setText(item.getAgeRestriction() != null ? item.getAgeRestriction() : "N/A");
        descriptionText.setText(item.getDescription() != null ? item.getDescription() : "N/A");

        // 🔗 Open Link Button Click
        openLinkButton.setOnClickListener(v -> {
            String link = item.getApplicationLink();
            if (link != null && !link.isEmpty()) {
                dialog.dismiss(); // Close dialog before opening WebView
                com.newsproject.oneroadmap.Utils.WebViewHelper.openUrlInApp(this, link);
            } else {
                Toast.makeText(context, "अर्जाची लिंक उपलब्ध नाही", Toast.LENGTH_SHORT).show();
            }
        });

        // 📄 Selection PDF Button Click
        selectionPdfButton.setOnClickListener(v -> {
            String pdfUrl = item.getSelectionPdfUrl();
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                dialog.dismiss(); // Close dialog before opening PDF viewer
                com.newsproject.oneroadmap.Utils.PdfViewerHelper.openPdfInApp(this, pdfUrl);
            } else {
                Toast.makeText(context, "सिलेक्शन PDF उपलब्ध नाही", Toast.LENGTH_SHORT).show();
            }
        });

        // Show dialog
        dialog.setCancelable(true);
        dialog.show();
    }

    private void loadStudyMaterials() {
        Log.d(TAG, "loadStudyMaterials started (API)");
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available, skipping study materials load");
            mainHandler.post(() -> {
                if (!hasShownStudyMaterialsErrorToast) {
                    Toast.makeText(getContext(), "No network, cannot load study materials", Toast.LENGTH_SHORT).show();
                    hasShownStudyMaterialsErrorToast = true;
                }
            });
            return;
        }

        String url = BuildConfig.BASE_URL + "/api/study-materials/";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch study materials: " + e.getMessage());
                mainHandler.post(() -> {
                    if (!hasShownStudyMaterialsErrorToast) {
                        Toast.makeText(getContext(), "Failed to load study materials", Toast.LENGTH_SHORT).show();
                        hasShownStudyMaterialsErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected response: " + response.code());
                    mainHandler.post(() -> {
                        if (!hasShownStudyMaterialsErrorToast) {
                            Toast.makeText(getContext(), "Failed to load study materials", Toast.LENGTH_SHORT).show();
                            hasShownStudyMaterialsErrorToast = true;
                        }
                    });
                    return;
                }

                String body = response.body().string();
                Log.d(TAG, "Study materials API response length: " + body.length());
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<StudyMaterial> fetched = new ArrayList<>();
                    if (root != null && root.has("studyMaterials")) {
                        JsonArray arr = root.getAsJsonArray("studyMaterials");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.get(i).getAsJsonObject();
                            String id = o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsString() : null;
                            String title = o.has("title") && !o.get("title").isJsonNull() ? o.get("title").getAsString() : null;
                            String type = o.has("type") && !o.get("type").isJsonNull() ? o.get("type").getAsString() : null;
                            String imageUrl = o.has("image_url") && !o.get("image_url").isJsonNull() ? o.get("image_url").getAsString() : null;
                            String pdfUrl = o.has("pdf_url") && !o.get("pdf_url").isJsonNull() ? o.get("pdf_url").getAsString() : null;
                            imageUrl = buildFullUrl(imageUrl);
                            pdfUrl = buildFullUrl(pdfUrl);
                            if (id == null || title == null || type == null || imageUrl == null || imageUrl.isEmpty() || pdfUrl == null || pdfUrl.isEmpty()) {
                                continue;
                            }
                            fetched.add(new StudyMaterial(id, title, type, imageUrl, pdfUrl));
                        }
                    }

                    final List<StudyMaterial> finalFetched = fetched;
                    mainHandler.post(() -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached, skipping filterAndDisplay");
                            return;
                        }
                        studyMaterialsAll.clear();
                        studyMaterialsAll.addAll(finalFetched);
                        filterAndDisplay("government"); // Default to government
                        Log.d(TAG, "Study materials loaded (API), count: " + studyMaterialsAll.size());
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse study materials API response: " + e.getMessage());
                    mainHandler.post(() -> {
                        if (!hasShownStudyMaterialsErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse study materials", Toast.LENGTH_SHORT).show();
                            hasShownStudyMaterialsErrorToast = true;
                        }
                    });
                }
            }
        });
    }

    private void filterAndDisplay(String type) {
        Log.d(TAG, "Filtering study materials for category: " + type);
        currentStudyMaterials.clear();
        String normalizedType = type; // Normalize input type to match API data
        switch (type) {
            case "self_improvement":
                normalizedType = "self improvement";
                break;
            case "police_defence":
                normalizedType = "police & defence";
                break;
        }
        for (StudyMaterial material : studyMaterialsAll) {
            if (material.getType().equals(normalizedType)) {
                currentStudyMaterials.add(material);
            }
        }
        if (studyMaterialAdapter != null) {
            studyMaterialAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "Data fetch status for " + type + ": " + (currentStudyMaterials.isEmpty() ? "No data fetched" : "Fetched " + currentStudyMaterials.size() + " items"));
        if (currentStudyMaterials.isEmpty()) {
            mainHandler.post(() -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "No study materials available for " + type, Toast.LENGTH_SHORT).show();
                }
            });
        }
        updateCategoryBackgrounds(type);
    }

    private void updateCategoryBackgrounds(String activeType) {
        // Check if fragment is attached and views are available
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "updateCategoryBackgrounds: Fragment not attached or context null, skipping update");
            return;
        }
        
        if (govLinear == null || policeLinear == null || bankLinear == null || selfLinear == null) {
            Log.w(TAG, "updateCategoryBackgrounds: Views are null, skipping update");
            return;
        }
        
        Context context = getContext();
        
        // Update government category
        govLinear.setBackgroundResource(activeType.equals("government") ? R.drawable.study_material_active : R.drawable.study_material_inactive);
        if (govLinear.getChildCount() > 1) {
            TextView govText = (TextView) govLinear.getChildAt(1);
            govText.setTextColor(ContextCompat.getColor(context, activeType.equals("government") ? R.color.white : R.color.black));
        }

        // Update police_defence category
        policeLinear.setBackgroundResource(activeType.equals("police_defence") ? R.drawable.study_material_active : R.drawable.study_material_inactive);
        if (policeLinear.getChildCount() > 1) {
            TextView policeText = (TextView) policeLinear.getChildAt(1);
            policeText.setTextColor(ContextCompat.getColor(context, activeType.equals("police_defence") ? R.color.white : R.color.black));
        }

        // Update banking category
        bankLinear.setBackgroundResource(activeType.equals("banking") ? R.drawable.study_material_active : R.drawable.study_material_inactive);
        if (bankLinear.getChildCount() > 1) {
            TextView bankText = (TextView) bankLinear.getChildAt(1);
            bankText.setTextColor(ContextCompat.getColor(context, activeType.equals("banking") ? R.color.white : R.color.black));
        }

        // Update self_improvement category
        selfLinear.setBackgroundResource(activeType.equals("self_improvement") ? R.drawable.study_material_active : R.drawable.study_material_inactive);
        if (selfLinear.getChildCount() > 1) {
            TextView selfText = (TextView) selfLinear.getChildAt(1);
            selfText.setTextColor(ContextCompat.getColor(context, activeType.equals("self_improvement") ? R.color.white : R.color.black));
        }
    }

    private void loadNews() {
        Log.d(TAG, "loadNews started (API)");
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available, skipping news load");
            mainHandler.post(() -> {
                if (!hasShownNewsErrorToast) {
                    Toast.makeText(getContext(), "No network, cannot load news", Toast.LENGTH_SHORT).show();
                    hasShownNewsErrorToast = true;
                }
                // Hide the section if no network
                View titleContainer = getView() != null ? getView().findViewById(R.id.all_linear5) : null;
                if (titleContainer != null) {
                    titleContainer.setVisibility(View.GONE);
                    recentlyOpenedRecycler.setVisibility(View.GONE);
                }
            });
            return;
        }

        String url = BuildConfig.BASE_URL + "/api/news";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch news: " + e.getMessage());
                mainHandler.post(() -> {
                    if (!hasShownNewsErrorToast) {
                        Toast.makeText(getContext(), "Failed to load news", Toast.LENGTH_SHORT).show();
                        hasShownNewsErrorToast = true;
                    }
                    // Hide the section on error
                    View titleContainer = getView() != null ? getView().findViewById(R.id.all_linear5) : null;
                    if (titleContainer != null) {
                        titleContainer.setVisibility(View.GONE);
                        recentlyOpenedRecycler.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected response: " + response.code());
                    mainHandler.post(() -> {
                        if (!hasShownNewsErrorToast) {
                            Toast.makeText(getContext(), "Failed to load news", Toast.LENGTH_SHORT).show();
                            hasShownNewsErrorToast = true;
                        }
                        // Hide the section on error
                        View titleContainer = getView() != null ? getView().findViewById(R.id.all_linear5) : null;
                        if (titleContainer != null) {
                            titleContainer.setVisibility(View.GONE);
                            recentlyOpenedRecycler.setVisibility(View.GONE);
                        }
                    });
                    return;
                }

                String body = response.body().string();
                Log.d(TAG, "News API response length: " + body.length());
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<News> fetched = new ArrayList<>();
                    if (root != null) {
                        // Handle different response formats
                        JsonArray newsArray = null;
                        if (root.has("news") && root.get("news").isJsonArray()) {
                            newsArray = root.getAsJsonArray("news");
                        } else if (root.has("data") && root.get("data").isJsonArray()) {
                            newsArray = root.getAsJsonArray("data");
                        } else if (root.isJsonArray()) {
                            newsArray = root.getAsJsonArray();
                        }
                        
                        if (newsArray != null) {
                            for (int i = 0; i < newsArray.size(); i++) {
                                JsonObject newsObj = newsArray.get(i).getAsJsonObject();
                                News news = new Gson().fromJson(newsObj, News.class);
                                // Build full URL for image (same as JobUpdate)
                                if (news.getImageUrl() != null) {
                                    news.setImageUrl(buildFullUrl(news.getImageUrl()));
                                }
                                fetched.add(news);
                            }
                        } else if (root.has("news") && root.get("news").isJsonObject()) {
                            // Single news item
                            JsonObject newsObj = root.getAsJsonObject("news");
                            News news = new Gson().fromJson(newsObj, News.class);
                            // Build full URL for image (same as JobUpdate)
                            if (news.getImageUrl() != null) {
                                news.setImageUrl(buildFullUrl(news.getImageUrl()));
                            }
                            fetched.add(news);
                        }
                    }

                    // Sort news by most recent first (by createdAt date, descending)
                    fetched.sort((news1, news2) -> {
                        Date date1 = news1.getCreatedAt();
                        Date date2 = news2.getCreatedAt();
                        
                        // If both have createdAt dates, compare them
                        if (date1 != null && date2 != null) {
                            return date2.compareTo(date1); // Descending order (newest first)
                        }
                        
                        // If only one has createdAt, prioritize it
                        if (date1 != null) return -1;
                        if (date2 != null) return 1;
                        
                        // If neither has createdAt, try date string
                        String dateStr1 = news1.getDate();
                        String dateStr2 = news2.getDate();
                        if (dateStr1 != null && dateStr2 != null) {
                            return dateStr2.compareTo(dateStr1); // Descending order
                        }
                        
                        return 0;
                    });
                    
                    final List<News> finalFetched = fetched;
                    mainHandler.post(() -> {
                        View titleContainer = getView() != null ? getView().findViewById(R.id.all_linear5) : null;
                        boolean hasItems = !finalFetched.isEmpty();
                        
                        if (titleContainer != null) {
                            titleContainer.setVisibility(hasItems ? View.VISIBLE : View.GONE);
                            recentlyOpenedRecycler.setVisibility(hasItems ? View.VISIBLE : View.GONE);
                        }
                        
                        if (hasItems) {
                            newsList.clear();
                            newsList.addAll(finalFetched);
                            if (newsAdapter != null) {
                                newsAdapter.notifyDataSetChanged();
                            }
                            Log.d(TAG, "News loaded (API), count: " + newsList.size());
                        } else {
                            Log.d(TAG, "No news items found");
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse news API response: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        if (!hasShownNewsErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse news", Toast.LENGTH_SHORT).show();
                            hasShownNewsErrorToast = true;
                        }
                        // Hide the section on parse error
                        View titleContainer = getView() != null ? getView().findViewById(R.id.all_linear5) : null;
                        if (titleContainer != null) {
                            titleContainer.setVisibility(View.GONE);
                            recentlyOpenedRecycler.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void loadCarouselItems() {
        Log.d(TAG, "loadCarouselItems started (Home API)");

        if (carousel == null) {
            Log.e(TAG, "ImageCarousel is null, check layout or initialization");
            mainHandler.post(() -> {
                if (!hasShownCarouselItemsErrorToast) {
                    Toast.makeText(getContext(), "Carousel not initialized", Toast.LENGTH_SHORT).show();
                    hasShownCarouselItemsErrorToast = true;
                }
            });
            return;
        }

        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available, loading dummy carousel items");
            mainHandler.post(() -> {
                if (!hasShownCarouselItemsErrorToast) {
                    Toast.makeText(getContext(), "No network, cannot load carousel items", Toast.LENGTH_SHORT).show();
                    hasShownCarouselItemsErrorToast = true;
                }
                List<CarouselItem> dummyCarouselItems = new ArrayList<>();
                dummyCarouselItems.add(new CarouselItem("https://picsum.photos/200/300", "Test Image 1"));
                dummyCarouselItems.add(new CarouselItem("https://picsum.photos/200/301", "Test Image 2"));
                dummyCarouselItems.add(new CarouselItem("https://picsum.photos/200/302", "Test Image 3"));
                final List<CarouselItem> finalDummyCarouselItems = dummyCarouselItems;
                carouselItemsList.clear();
                carouselItemsList.addAll(finalDummyCarouselItems);
                carousel.addData(carouselItemsList);
                carousel.invalidate();
            });
            return;
        }

        final Map<String, JobUpdate> jobUpdateCache = new HashMap<>();
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userEducation = sharedPreferences.getString("education", "");
        String userDegree = sharedPreferences.getString("degree", "");
        String userTwelfth = sharedPreferences.getString("twelfth", "");
        String userPostGraduation = sharedPreferences.getString("postGraduation", "");
        String userDistrict = sharedPreferences.getString("district", "");
        String userTaluka = sharedPreferences.getString("taluka", "");

        String url = "https://admin.mahaalert.cloud/api/sliders/home";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch home carousel items: " + e.getMessage());
                mainHandler.post(() -> {
                    if (!hasShownCarouselItemsErrorToast) {
                        Toast.makeText(getContext(), "Failed to load home carousel items", Toast.LENGTH_SHORT).show();
                        hasShownCarouselItemsErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected response: " + response.code());
                    mainHandler.post(() -> {
                        if (!hasShownCarouselItemsErrorToast) {
                            Toast.makeText(getContext(), "Failed to load home carousel items: " + response.code(), Toast.LENGTH_SHORT).show();
                            hasShownCarouselItemsErrorToast = true;
                        }
                    });
                    return;
                }

                String body = response.body().string();
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<Slider> sliders = new ArrayList<>();
                    if (root != null && root.has("sliders")) {
                        JsonArray arr = root.getAsJsonArray("sliders");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.get(i).getAsJsonObject();
                            Slider slider = new Gson().fromJson(o, Slider.class);
                            if (slider.getImageUrl() == null || slider.getImageUrl().isEmpty()) continue;

                            boolean isUniversal = false, educationMatch = false, locationMatch = false;
                            List<String> sliderEducationCategories = slider.getEducationCategoriesSafe();
                            List<String> sliderBachelorDegrees = slider.getBachelorDegreesSafe();
                            List<String> sliderMastersDegrees = slider.getMastersDegreesSafe();
                            String sliderDistrict = slider.getDistrictSafe();
                            String sliderTaluka = slider.getTalukaSafe();


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
                                }
                            }
                        }
                    }

                    List<CarouselItem> carouselItems = new ArrayList<>();
                    for (Slider slider : sliders) {
                        String imageUrl = slider.getImageUrl().replace("http://", "https://");
                        carouselItems.add(new CarouselItem(imageUrl, slider.getTitle()));
                    }

                    final List<CarouselItem> finalCarouselItems = carouselItems;
                    final List<Slider> finalSliders = sliders;
                    final Map<String, JobUpdate> finalJobUpdateCache = jobUpdateCache;
                    mainHandler.post(() -> {
                        carouselItemsList.clear();
                        carouselItemsList.addAll(finalCarouselItems);
                        carousel.addData(carouselItemsList);
                        carousel.invalidate();

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
                                if (position < 0 || position >= finalSliders.size()) return;
                                Slider selectedSlider = finalSliders.get(position);
                                String id = selectedSlider.getPostDocumentId();
                                if (id == null || id.trim().isEmpty()) {
                                    Toast.makeText(getContext(), "Content unavailable", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
                                progressDialog.setMessage("Loading...");
                                progressDialog.setCancelable(false);
                                progressDialog.show();

                                final String finalId = id;
                                final Slider finalSelectedSlider = selectedSlider;
                                if ("post".equalsIgnoreCase(finalSelectedSlider.getType())) {
                                    JobUpdate job = finalJobUpdateCache.get(finalId);
                                    if (job != null) {
                                        JobViewModel.navigateToJobDetails(job, getContext(), progressDialog);
                                    } else {
                                        JobViewModel.fetchJobUpdate(finalId, finalJobUpdateCache, getContext(), () -> {
                                            JobUpdate fetchedJob = finalJobUpdateCache.get(finalId);
                                            if (fetchedJob != null) {
                                                JobViewModel.navigateToJobDetails(fetchedJob, getContext(), progressDialog);
                                            }
                                        });
                                    }
                                } else if ("news".equalsIgnoreCase(finalSelectedSlider.getType())) {
                                    News news = newsCache.get(finalId);
                                    if (news != null) {
                                        NewsUtils.showNewsDialog(news, getContext(), progressDialog);
                                    } else {
                                        NewsUtils.fetchNews(finalId, newsCache, getContext(), () -> {
                                            News fetchedNews = newsCache.get(finalId);
                                            if (fetchedNews != null) {
                                                NewsUtils.showNewsDialog(fetchedNews, getContext(), progressDialog);
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse home carousel items: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        if (!hasShownCarouselItemsErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse home carousel items", Toast.LENGTH_SHORT).show();
                            hasShownCarouselItemsErrorToast = true;
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        storiesPlayer = null;
        Log.d(TAG, "onDestroyView called, storiesPlayer set to null");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "onDestroy called, executor service shut down");
        }
    }

    private void setBottomMarginsBasedOnAndroidVersion() {
        if (android.os.Build.VERSION.SDK_INT >= 34) { // Android 15 is API 34
            // Set larger margins for Android 15+
            if (mainScrollView != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mainScrollView.getLayoutParams();
                params.bottomMargin = dpToPx(48);
                mainScrollView.setLayoutParams(params);
                Log.d(TAG, "Set mainScrollView bottom margin to 48dp for Android 15+");
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}