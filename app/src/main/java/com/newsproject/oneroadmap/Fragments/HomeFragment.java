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
import com.newsproject.oneroadmap.Utils.CoinAccessController;
import com.newsproject.oneroadmap.Utils.NewsUtils;
import com.newsproject.oneroadmap.Utils.ShareHelper;
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
    private CoinAccessController coinAccessController;
    private ShareHelper shareHelper;
    private ActivityResultLauncher<Intent> shareLauncher;
    private String userId;
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
    private boolean hasShownCurrentAffairsErrorToast = false;
    private boolean hasShownJobUpdatesErrorToast = false;
    private boolean hasShownStudentUpdatesErrorToast = false;
    private boolean hasShownNewsErrorToast = false;
    private boolean hasShownCarouselItemsErrorToast = false;
    private boolean hasShownStudyMaterialsErrorToast = false;
    private JobViewModel jobViewModel;
    private OkHttpClient client;
    private LinearLayout govLinear, policeLinear, bankLinear;
    private Map<String, News> newsCache = new HashMap<>(); // Cache for news items
    private RecentlyOpenedDatabaseHelper recentDb;
    private static ActivityResultLauncher<Intent> storyShareLauncher;
    private TextView tagCareerRoadmap, tagResultHallTicket, tagGovtJobs;
    private TextView tagBankingJobs, tagAllJobs, tagPrivateJobs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executorService = Executors.newSingleThreadExecutor();
        Log.d(TAG, "ExecutorService created in onCreate");
        
        // Initialize share launcher for stories
        storyShareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (!isAdded()) return;
                    // When user returns from sharing, add coins
                    SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    String userId = prefs.getString("userId", "");
                    if (userId != null && !userId.isEmpty()) {
                        com.newsproject.oneroadmap.Utils.DatabaseHelper dbHelper = new com.newsproject.oneroadmap.Utils.DatabaseHelper(requireContext());
                        int current = dbHelper.getUserCoins(userId);
                        com.newsproject.oneroadmap.Utils.CoinManager coinManager = new com.newsproject.oneroadmap.Utils.CoinManager(requireContext(), userId);
                        coinManager.addCoinsForShare(newCoins -> {
                            // Coins added successfully
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
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", "");

        shareHelper = new ShareHelper(requireContext());

        coinAccessController = new CoinAccessController(
                this,
                userId,
                shareHelper
        );

        shareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (coinAccessController != null) {
                        coinAccessController.onShareCompleted();
                    }
                });

        shareHelper.setShareLauncher(shareLauncher);
    }

    public static boolean isStoriesPlayerVisible() {
        return storiesPlayer != null && storiesPlayer.getVisibility() == View.VISIBLE;
    }

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

        setBottomMarginsBasedOnAndroidVersion();

        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bottom_navigation_light));
        Log.d(TAG, "Set bottom navigation background color");
    }

    private void initViews(View view) {
        Log.d(TAG, "initViews called");
        tagCareerRoadmap    = view.findViewById(R.id.tag_career_roadmap);
        tagResultHallTicket = view.findViewById(R.id.tag_result_hallticket);
        tagGovtJobs         = view.findViewById(R.id.tag_govt_jobs);

        tagBankingJobs      = view.findViewById(R.id.tag_banking_jobs);
        tagAllJobs          = view.findViewById(R.id.tag_all_jobs);
        tagPrivateJobs      = view.findViewById(R.id.tag_private_jobs);

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

        LinearLayout studyCards = view.findViewById(R.id.study_material_cards_linear);
        LinearLayout row1 = (LinearLayout) studyCards.getChildAt(1);
        govLinear = (LinearLayout) row1.getChildAt(0);
        policeLinear = (LinearLayout) row1.getChildAt(1);
        LinearLayout row2 = (LinearLayout) studyCards.getChildAt(2);
        bankLinear = (LinearLayout) row2.getChildAt(0);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userNameText = sharedPreferences.getString("name", "Guest");
        String[] userNames = userNameText.split(" ");
        userName.setText(userNames[0]);
        Log.d(TAG, "Set userName text: " + userNames[0]);

        String avatarName = sharedPreferences.getString("avatar", "");
        if (!avatarName.isEmpty()) {
            int avatarResId = getResources().getIdentifier(avatarName, "drawable", requireContext().getPackageName());
            if (avatarResId != 0) {
                Glide.with(requireContext())
                        .load(avatarResId)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(profileIcon);
                Log.d(TAG, "Loaded avatar from SharedPreferences: " + avatarName + " (ID: " + avatarResId + ")");
            } else {
                profileIcon.setImageResource(R.mipmap.ic_launcher_round);
                Log.e(TAG, "Avatar resource not found for name: " + avatarName);
            }
        } else {
            profileIcon.setImageResource(R.mipmap.ic_launcher_round);
            Log.d(TAG, "No avatar found in SharedPreferences, using default image");
        }

        LinearLayoutManager storyLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        storyRecycler.setLayoutManager(storyLayoutManager);
        storyAdapter = new StoryAdapter(getContext(), storyList);
        storyRecycler.setAdapter(storyAdapter);

        LinearLayoutManager currentAffairsLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        currentAffairsRecycler.setLayoutManager(currentAffairsLayoutManager);
        currentAffairsAdapter = new CurrentAffairsAdapter(currentAffairsList, getContext(), pdfUrl -> {
            coinAccessController.requestPdfAccess(pdfUrl, null);
        });
        currentAffairsRecycler.setAdapter(currentAffairsAdapter);

        LinearLayoutManager recentlyOpenedLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recentlyOpenedRecycler.setLayoutManager(recentlyOpenedLayoutManager);
        newsAdapter = new NewsAdapter(newsList, getContext());
        recentlyOpenedRecycler.setAdapter(newsAdapter);

        LinearLayoutManager studentUpdatesLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        studentUpdatesRecycler.setLayoutManager(studentUpdatesLayoutManager);
        studentAdapter1 = new StudentUpdateAdapter(studentUpdatesList, getContext());
        studentUpdatesRecycler.setAdapter(studentAdapter1);

        recyclerStudyMaterial.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        studyMaterialAdapter = new StudyMaterialAdapter(currentStudyMaterials, getContext(), pdfUrl -> {
            coinAccessController.requestPdfAccess(pdfUrl, null);
        });
        recyclerStudyMaterial.setAdapter(studyMaterialAdapter);

        LinearLayoutManager storiesPlayerLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        storiesPlayer.setLayoutManager(storiesPlayerLayoutManager);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(storiesPlayer);

        setupClickListeners();
    }

    private void setupClickListeners() {
        tagCareerRoadmap.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MainFragment())
                    .addToBackStack(null)
                    .commit();
        });

        tagResultHallTicket.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new Result_HallTitcket())
                    .addToBackStack(null)
                    .commit();
        });

        tagGovtJobs.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new GovernmentJobs())
                    .addToBackStack(null)
                    .commit();
        });

        tagBankingJobs.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BankingJobs())
                    .addToBackStack(null)
                    .commit();
        });

        tagAllJobs.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AllCategory())
                    .addToBackStack(null)
                    .commit();
        });

        tagPrivateJobs.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new PrivateJobs())
                    .addToBackStack(null)
                    .commit();
        });

        saveButton.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SavedJobsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        allStudentUpdates.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new AllBannersList());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        studentAdapter1.setOnItemClickListener(item -> {
            showStudentUpdateDialog(item);
        });

        admitCard.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new Result_HallTitcket());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        bankJobs.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new BankingJobs());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        privateJobs.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new PrivateJobs());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        governmentJobs.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new GovernmentJobs());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        profileIcon.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new ProfileFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        govLinear.setOnClickListener(v -> filterAndDisplay("government"));
        policeLinear.setOnClickListener(v -> filterAndDisplay("police_defence"));
        bankLinear.setOnClickListener(v -> filterAndDisplay("banking"));
    }

    private boolean isNetworkAvailable() {
        Context context = getContext();
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadData() {
        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newSingleThreadExecutor();
        }
        try {
            executorService.execute(() -> {
                if (!isAdded()) return;
                loadStories();
                loadCurrentAffairsData();
                loadStudentUpdates();
                loadNews();
                loadCarouselItems();
                loadStudyMaterials();
            });
            if (isNetworkAvailable()) {
                jobViewModel.loadJobs(client, BuildConfig.JOB_UPDATES, requireContext());
                loadJobUpdates();
            } else {
                mainHandler.post(() -> {
                    if (isAdded() && !hasShownJobUpdatesErrorToast) {
                        Toast.makeText(getContext(), "No network, cannot load job updates", Toast.LENGTH_SHORT).show();
                        hasShownJobUpdatesErrorToast = true;
                    }
                });
            }
        } catch (Exception e) {
            mainHandler.post(() -> {
                if (isAdded()) Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadStories() {
        if (!isNetworkAvailable()) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userDegree = prefs.getString("degree", "");
        String userPostGrad = prefs.getString("postGraduation", "");
        String userTaluka = prefs.getString("taluka", "");
        String userAgeGroup = prefs.getString("ageGroup", "");
        
        boolean studyGov = prefs.getBoolean("study_Government", false);
        boolean studyPolice = prefs.getBoolean("study_Police_Defence", false);
        boolean studyBank = prefs.getBoolean("study_Banking", false);

        String url = BuildConfig.BASE_URL + BuildConfig.STORIES;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch stories", e);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || !isAdded()) return;

                String body = response.body().string();
                JsonObject root = new Gson().fromJson(body, JsonObject.class);

                List<Story> mains = new ArrayList<>();
                List<Story> others = new ArrayList<>();

                if (root != null && root.has("stories")) {
                    JsonArray arr = root.getAsJsonArray("stories");

                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject o = arr.get(i).getAsJsonObject();
                        Story story = new Gson().fromJson(o, Story.class);
                        
                        story.setIconUrl(buildFullUrl(story.getIconUrl()));
                        story.setBannerUrl(buildFullUrl(story.getBannerUrl()));
                        story.setVideoUrl(buildFullUrl(story.getVideoUrl()));
                        story.setImageUrl("video".equalsIgnoreCase(story.getMediaType()) ? story.getVideoUrl() : story.getBannerUrl());

                        if (story.getIconUrl() == null || story.getImageUrl() == null) continue;

                        boolean viewed = story.getDocumentId() != null && storyPrefs.getBoolean("viewed_" + story.getDocumentId(), false);
                        story.setViewed(viewed);

                        boolean shouldShow = true;
                        if (story.isMainStory()) {
                            boolean hasSpecificCriteria = (story.getOtherType() != null && !story.getOtherType().isEmpty()) ||
                                    !story.getBachelorDegreesSafe().isEmpty() ||
                                    !story.getMastersDegreesSafe().isEmpty() ||
                                    !story.getTalukaSafe().isEmpty() ||
                                    !story.getAgeGroupsSafe().isEmpty() ||
                                    !story.getBhartyTypesSafe().isEmpty();

                            if (hasSpecificCriteria) {
                                shouldShow = false;
                                List<String> bDegrees = story.getBachelorDegreesSafe();
                                List<String> mDegrees = story.getMastersDegreesSafe();
                                if (!userDegree.isEmpty() && bDegrees.contains(userDegree)) shouldShow = true;
                                if (!userPostGrad.isEmpty() && mDegrees.contains(userPostGrad)) shouldShow = true;
                                if (!shouldShow && !userTaluka.isEmpty() && story.getTalukaSafe().contains(userTaluka)) shouldShow = true;
                                if (!shouldShow && !userAgeGroup.isEmpty() && story.getAgeGroupsSafe().contains(userAgeGroup)) shouldShow = true;
                                if (!shouldShow) {
                                    List<String> bTypes = story.getBhartyTypesSafe();
                                    for (String type : bTypes) {
                                        if (type.equalsIgnoreCase("Government") && studyGov) { shouldShow = true; break; }
                                        if (type.equalsIgnoreCase("Police & Defence") && studyPolice) { shouldShow = true; break; }
                                        if (type.equalsIgnoreCase("Banking") && studyBank) { shouldShow = true; break; }
                                    }
                                }
                            }
                        } else {
                            shouldShow = true;
                        }

                        if (shouldShow) {
                            if (story.isMainStory()) mains.add(story);
                            else others.add(story);
                        }
                    }
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    storyList.clear();
                    storyList.addAll(mains);
                    storyList.addAll(others);
                    storyAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void loadCurrentAffairsData() {
        if (!isNetworkAvailable()) {
            mainHandler.post(() -> {
                if (isAdded() && !hasShownCurrentAffairsErrorToast) {
                    Toast.makeText(getContext(), "No network", Toast.LENGTH_SHORT).show();
                    hasShownCurrentAffairsErrorToast = true;
                }
            });
            return;
        }

        String url = BuildConfig.BASE_URL + BuildConfig.CURRENT_AFFAIRS;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    if (isAdded() && !hasShownCurrentAffairsErrorToast) {
                        Toast.makeText(getContext(), "Failed to load current affairs", Toast.LENGTH_SHORT).show();
                        hasShownCurrentAffairsErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || !isAdded()) return;

                String body = response.body().string();
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<CurrentAffairs> fetched = new ArrayList<>();
                    if (root != null && root.has("currentAffairs")) {
                        JsonArray arr = root.getAsJsonArray("currentAffairs");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.get(i).getAsJsonObject();
                            String dateRaw = o.has("date") && !o.get("date").isJsonNull() ? o.get("date").getAsString() : null;
                            String date = dateRaw != null && dateRaw.length() >= 10 ? dateRaw.substring(0, 10) : dateRaw;
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
                        if (!isAdded()) return;
                        currentAffairsList.clear();
                        currentAffairsList.addAll(finalFetched);
                        currentAffairsAdapter.notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        if (isAdded() && !hasShownCurrentAffairsErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse current affairs", Toast.LENGTH_SHORT).show();
                            hasShownCurrentAffairsErrorToast = true;
                        }
                    });
                }
            }
        });
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
        try { epochMillis = java.time.OffsetDateTime.parse(createdAt).toInstant().toEpochMilli(); } catch (Exception ignored) {}
        if (epochMillis == null) { try { epochMillis = java.time.ZonedDateTime.parse(createdAt).toInstant().toEpochMilli(); } catch (Exception ignored) {} }
        if (epochMillis == null) { try { epochMillis = java.time.Instant.parse(createdAt).toEpochMilli(); } catch (Exception ignored) {} }
        if (epochMillis == null) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(createdAt, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                epochMillis = ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
            } catch (Exception ignored) {}
        }
        if (epochMillis == null) {
            String[] patterns = new String[] { "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" };
            for (String p : patterns) {
                try {
                    java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(p).withZone(java.time.ZoneOffset.UTC);
                    epochMillis = java.time.Instant.from(f.parse(createdAt)).toEpochMilli();
                    break;
                } catch (Exception ignored) {}
            }
        }
        if (epochMillis == null) {
            try {
                long numeric = Long.parseLong(createdAt.trim());
                if (createdAt.trim().length() <= 10) epochMillis = numeric * 1000L;
                else epochMillis = numeric;
            } catch (Exception ignored) {}
        }
        return epochMillis != null ? epochMillis : 0;
    }

    private void loadJobUpdates() {
        if (!isNetworkAvailable()) return;
        jobViewModel.getJobs().observe(getViewLifecycleOwner(), jobs -> {
            if (jobs != null) {
                jobUpdatesList.clear();
                int limit = Math.min(5, jobs.size());
                jobUpdatesList.addAll(jobs.subList(0, limit));
                updateRecentRecycler();
            }
        });
        jobViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && isAdded() && !hasShownJobUpdatesErrorToast) {
                Toast.makeText(getContext(), "Failed to load job updates", Toast.LENGTH_SHORT).show();
                hasShownJobUpdatesErrorToast = true;
            }
        });
    }

    private void updateRecentRecycler() {
        final List<JobUpdate> finalJobUpdatesList = new ArrayList<>(jobUpdatesList);
        mainHandler.post(() -> {
            if (!isAdded() || getContext() == null || recentRecycler == null) return;
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
                    if (!isAdded()) return;
                    JobUpdateDetails fragment = JobUpdateDetails.newInstance(item);
                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment_container, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                });
                recentRecycler.addView(itemView);
            }
        });
    }

    private void loadStudentUpdates() {
        if (!isNetworkAvailable()) return;
        String url = BuildConfig.BASE_URL + BuildConfig.STUDENT_UPDATES;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    if (isAdded() && !hasShownStudentUpdatesErrorToast) {
                        Toast.makeText(getContext(), "Failed to load student updates", Toast.LENGTH_SHORT).show();
                        hasShownStudentUpdatesErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || !isAdded()) return;
                String body = response.body().string();
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
                            fetched.add(new StudentUpdateItem(id, title, education, applicationMethod, description, applicationLink, lastDate, imageUrl, iconUrl, notificationPdfUrl, selectionPdfUrl, createdAt));
                        }
                    }
                    final List<StudentUpdateItem> finalFetched = fetched;
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        studentUpdatesList.clear();
                        studentUpdatesList.addAll(finalFetched);
                        if (studentAdapter1 != null) studentAdapter1.notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        if (isAdded() && !hasShownStudentUpdatesErrorToast) {
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
        try {
            stopStory(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop stories in onPause");
        }
    }

    public void showStudentUpdateDialog(@NonNull StudentUpdateItem item) {
        Context context = this.getContext();
        if (context == null) return;
        Dialog dialog = new Dialog(context, R.style.BlurDialogTheme);
        dialog.setContentView(R.layout.student_update);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.dimAmount = 0.6f;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        dialog.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        CircleImageView iconImageView = dialog.findViewById(R.id.circleImageView2);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        TextView titleText = dialog.findViewById(R.id.title_text);
        TextView educationValue = dialog.findViewById(R.id.education_requirement_value);
        TextView descriptionText = dialog.findViewById(R.id.textView6);
        androidx.cardview.widget.CardView openLinkButton = dialog.findViewById(R.id.open_link_button);
        androidx.cardview.widget.CardView selectionPdfButton = dialog.findViewById(R.id.selection_pdf_button);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        if (item.getIconUrl() != null && !item.getIconUrl().isEmpty()) {
            Glide.with(context).load(item.getIconUrl()).placeholder(R.drawable.app_logo).error(R.drawable.app_logo).into(iconImageView);
        } else {
            iconImageView.setImageResource(R.drawable.app_logo);
        }
        titleText.setText(item.getTitle() != null ? item.getTitle() : "N/A");
        educationValue.setText(item.getEducation() != null ? item.getEducation() : "N/A");
        descriptionText.setText(item.getDescription() != null ? item.getDescription() : "N/A");
        openLinkButton.setOnClickListener(v -> {
            String link = item.getApplicationLink();
            if (link != null && !link.isEmpty()) {
                dialog.dismiss();
                com.newsproject.oneroadmap.Utils.WebViewHelper.openUrlInApp(this, link);
            } else {
                Toast.makeText(context, "link not available", Toast.LENGTH_SHORT).show();
            }
        });
        selectionPdfButton.setOnClickListener(v -> {
            String pdfUrl = item.getSelectionPdfUrl();
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                coinAccessController.requestPdfAccess(pdfUrl, () -> dialog.dismiss());
            } else {
                Toast.makeText(context, "PDF not available", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    private void loadStudyMaterials() {
        if (!isNetworkAvailable()) return;
        String url = BuildConfig.BASE_URL + BuildConfig.STUDY_MATERIALS;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    if (isAdded() && !hasShownStudyMaterialsErrorToast) {
                        Toast.makeText(getContext(), "Failed to load study materials", Toast.LENGTH_SHORT).show();
                        hasShownStudyMaterialsErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || !isAdded()) return;
                String body = response.body().string();
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
                            if (id == null || title == null || type == null || imageUrl == null || imageUrl.isEmpty() || pdfUrl == null || pdfUrl.isEmpty()) continue;
                            fetched.add(new StudyMaterial(id, title, type, imageUrl, pdfUrl));
                        }
                    }
                    final List<StudyMaterial> finalFetched = fetched;
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        studyMaterialsAll.clear();
                        studyMaterialsAll.addAll(finalFetched);
                        filterAndDisplay("government");
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        if (isAdded() && !hasShownStudyMaterialsErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse study materials", Toast.LENGTH_SHORT).show();
                            hasShownStudyMaterialsErrorToast = true;
                        }
                    });
                }
            }
        });
    }

    private void filterAndDisplay(String type) {
        currentStudyMaterials.clear();
        String normalizedType = type;
        if (type.equals("police_defence")) normalizedType = "police & defence";
        for (StudyMaterial material : studyMaterialsAll) {
            if (material.getType().equals(normalizedType)) currentStudyMaterials.add(material);
        }
        if (studyMaterialAdapter != null) studyMaterialAdapter.notifyDataSetChanged();
        if (currentStudyMaterials.isEmpty()) {
            mainHandler.post(() -> {
                if (isAdded()) Toast.makeText(getContext(), "No materials for " + type, Toast.LENGTH_SHORT).show();
            });
        }
        updateCategoryBackgrounds(type);
    }

    private void updateCategoryBackgrounds(String activeType) {
        if (!isAdded() || getContext() == null || govLinear == null) return;
        Context context = getContext();
        govLinear.setBackgroundResource(activeType.equals("government") ? R.drawable.study_material_active : R.drawable.study_material_inactive);
        if (govLinear.getChildCount() > 1) ((TextView) govLinear.getChildAt(1)).setTextColor(ContextCompat.getColor(context, activeType.equals("government") ? R.color.white : R.color.black));
        policeLinear.setBackgroundResource(activeType.equals("police_defence") ? R.drawable.study_material_active : R.drawable.study_material_inactive);
        if (policeLinear.getChildCount() > 1) ((TextView) policeLinear.getChildAt(1)).setTextColor(ContextCompat.getColor(context, activeType.equals("police_defence") ? R.color.white : R.color.black));
        bankLinear.setBackgroundResource(activeType.equals("banking") ? R.drawable.study_material_active : R.drawable.study_material_inactive);
        if (bankLinear.getChildCount() > 1) ((TextView) bankLinear.getChildAt(1)).setTextColor(ContextCompat.getColor(context, activeType.equals("banking") ? R.color.white : R.color.black));
    }

    private void loadNews() {
        if (!isNetworkAvailable()) {
            mainHandler.post(() -> {
                if (isAdded() && !hasShownNewsErrorToast) {
                    Toast.makeText(getContext(), "No network", Toast.LENGTH_SHORT).show();
                    hasShownNewsErrorToast = true;
                }
            });
            return;
        }
        String url = BuildConfig.BASE_URL + BuildConfig.NEWS;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    if (isAdded() && !hasShownNewsErrorToast) {
                        Toast.makeText(getContext(), "Failed to load news", Toast.LENGTH_SHORT).show();
                        hasShownNewsErrorToast = true;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || !isAdded()) return;
                String body = response.body().string();
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<News> fetched = new ArrayList<>();
                    if (root != null) {
                        JsonArray newsArray = null;
                        if (root.has("news") && root.get("news").isJsonArray()) newsArray = root.getAsJsonArray("news");
                        else if (root.has("data") && root.get("data").isJsonArray()) newsArray = root.getAsJsonArray("data");
                        else if (root.isJsonArray()) newsArray = root.getAsJsonArray();
                        if (newsArray != null) {
                            for (int i = 0; i < newsArray.size(); i++) {
                                JsonObject newsObj = newsArray.get(i).getAsJsonObject();
                                News news = new Gson().fromJson(newsObj, News.class);
                                if (news.getImageUrl() != null) news.setImageUrl(buildFullUrl(news.getImageUrl()));
                                fetched.add(news);
                            }
                        }
                    }
                    fetched.sort((n1, n2) -> {
                        Date d1 = n1.getCreatedAt(); Date d2 = n2.getCreatedAt();
                        if (d1 != null && d2 != null) return d2.compareTo(d1);
                        return 0;
                    });
                    final List<News> finalFetched = fetched;
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        newsList.clear();
                        newsList.addAll(finalFetched);
                        if (newsAdapter != null) newsAdapter.notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        if (isAdded() && !hasShownNewsErrorToast) {
                            Toast.makeText(getContext(), "Failed to parse news", Toast.LENGTH_SHORT).show();
                            hasShownNewsErrorToast = true;
                        }
                    });
                }
            }
        });
    }

    private void loadCarouselItems() {
        if (!isNetworkAvailable()) return;
        final Map<String, JobUpdate> jobUpdateCache = new HashMap<>();
        String url = BuildConfig.BASE_URL + BuildConfig.SLIDERS_HOME;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { Log.e(TAG, "Failed to fetch carousel"); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || !isAdded()) return;
                String body = response.body().string();
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<Slider> finalSliders = new ArrayList<>();
                    if (root != null && root.has("sliders")) {
                        JsonArray arr = root.getAsJsonArray("sliders");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.get(i).getAsJsonObject();
                            Slider slider = new Gson().fromJson(o, Slider.class);
                            slider.setImageUrl(buildFullUrl(slider.getImageUrl()));
                            finalSliders.add(slider);
                        }
                    }
                    List<CarouselItem> carouselItems = new ArrayList<>();
                    for (Slider slider : finalSliders) carouselItems.add(new CarouselItem(slider.getImageUrl(), slider.getTitle()));
                    mainHandler.post(() -> {
                        if (!isAdded() || carousel == null) return;
                        carouselItemsList.clear(); carouselItemsList.addAll(carouselItems);
                        carousel.setData(carouselItemsList);
                        carousel.setCarouselListener(new CarouselListener() {
                            @Override public void onClick(int position, CarouselItem carouselItem) {
                                if (!isAdded() || position < 0 || position >= finalSliders.size()) return;
                                Slider s = finalSliders.get(position);
                                if (s.getPostDocumentId() == null) return;
                                android.app.ProgressDialog pd = new android.app.ProgressDialog(getContext());
                                pd.setMessage("Loading..."); pd.show();
                                if ("post".equalsIgnoreCase(s.getType())) {
                                    JobViewModel.fetchJobUpdate(s.getPostDocumentId(), jobUpdateCache, getContext(), () -> {
                                        JobUpdate j = jobUpdateCache.get(s.getPostDocumentId());
                                        if (j != null) JobViewModel.navigateToJobDetails(j, getContext(), pd);
                                        else pd.dismiss();
                                    });
                                }
                            }
                            @Override public void onLongClick(int i, @NonNull CarouselItem carouselItem) {}
                            @Override public void onBindViewHolder(@NonNull ViewBinding viewBinding, @NonNull CarouselItem carouselItem, int i) {}
                            @Nullable @Override public ViewBinding onCreateViewHolder(@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup) { return null; }
                        });
                    });
                } catch (Exception e) { Log.e(TAG, "Failed to parse carousel"); }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        storiesPlayer = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }

    private void setBottomMarginsBasedOnAndroidVersion() {
        if (android.os.Build.VERSION.SDK_INT >= 34 && mainScrollView != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mainScrollView.getLayoutParams();
            params.bottomMargin = dpToPx(48);
            mainScrollView.setLayoutParams(params);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
