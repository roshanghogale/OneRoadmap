package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.newsproject.oneroadmap.Adapters.ChatAdapter;
import com.newsproject.oneroadmap.Models.Query;
import com.newsproject.oneroadmap.Models.Reply;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ApiClient;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private final ArrayList<Query> Chats = new ArrayList<>();
    private final ArrayList<Query> allChats = new ArrayList<>(); // Store all chats for filtering
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private ImageCarousel carousel;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LinearLayout linearleft;
    private boolean isFabOpen = false;
    
    // Filter and sort state
    private String selectedFilter = "all"; // "all", "career", "maharashtra", "banking"
    private String sortOption = "recent"; // "recent" or "popular"
    
    // Filter and sort buttons
    private android.widget.TextView filterAll, filterCareer, filterMaharashtra, filterBanking;
    private android.widget.TextView sortPopular, sortRecent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        // Initialize views
        chatRecycler = view.findViewById(R.id.chat_recycler);
        carousel = view.findViewById(R.id.carousel);
        linearleft = view.findViewById(R.id.linearleft);
        
        // Initialize filter and sort buttons
        filterAll = view.findViewById(R.id.filter_all);
        filterCareer = view.findViewById(R.id.filter_career);
        filterMaharashtra = view.findViewById(R.id.filter_maharashtra);
        filterBanking = view.findViewById(R.id.filter_banking);
        sortPopular = view.findViewById(R.id.sort_popular);
        sortRecent = view.findViewById(R.id.sort_recent);
        
        // Setup filter button click listeners
        filterAll.setOnClickListener(v -> {
            selectedFilter = "all";
            updateFilterButtonStates();
            applyFilterAndSort();
        });
        
        filterCareer.setOnClickListener(v -> {
            selectedFilter = "career";
            updateFilterButtonStates();
            applyFilterAndSort();
        });
        
        filterMaharashtra.setOnClickListener(v -> {
            selectedFilter = "maharashtra";
            updateFilterButtonStates();
            applyFilterAndSort();
        });
        
        filterBanking.setOnClickListener(v -> {
            selectedFilter = "banking";
            updateFilterButtonStates();
            applyFilterAndSort();
        });
        
        // Setup sort button click listeners
        sortPopular.setOnClickListener(v -> {
            sortOption = "popular";
            updateSortButtonStates();
            applyFilterAndSort();
        });
        
        sortRecent.setOnClickListener(v -> {
            sortOption = "recent";
            updateSortButtonStates();
            applyFilterAndSort();
        });
        
        // Set initial button states
        updateFilterButtonStates();
        updateSortButtonStates();

        FloatingActionButton fabMain = view.findViewById(R.id.fab_main);
        FloatingActionButton fabAskQuery = view.findViewById(R.id.fab_ask_query);
        FloatingActionButton fabMyQueries = view.findViewById(R.id.fab_my_queries);
        RelativeLayout fabContainer = view.findViewById(R.id.fab_container);
        LinearLayout fabAskQueryLayout = view.findViewById(R.id.fab_ask_query_layout);
        LinearLayout fabMyQueriesLayout = view.findViewById(R.id.fab_my_queries_layout);

        isFabOpen = false;

        // Setup FAB click listeners
        fabMain.setOnClickListener(v -> {
            if (!isFabOpen) {
                fabContainer.setClickable(true);
                fabMyQueriesLayout.setVisibility(View.VISIBLE);
                fabAskQueryLayout.setVisibility(View.VISIBLE);
                fabMyQueriesLayout.animate().alpha(1f).translationY(0).setDuration(300).start();
                fabAskQueryLayout.animate().alpha(1f).translationY(0).setDuration(300).start();
                isFabOpen = true;
                fabMain.setImageResource(R.drawable.close);
            } else {
                fabContainer.setClickable(false);
                fabMyQueriesLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabMyQueriesLayout.setVisibility(View.GONE)).start();
                fabAskQueryLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabAskQueryLayout.setVisibility(View.GONE)).start();
                isFabOpen = false;
                fabMain.setImageResource(R.drawable.add);
            }
        });

        fabAskQuery.setOnClickListener(v -> {
            AskQuery bottomSheet = new AskQuery();
            bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
            collapseFab(fabMyQueriesLayout, fabAskQueryLayout, fabMain);
        });

        fabMyQueries.setOnClickListener(v -> {
            MyQueries fragment = new MyQueries();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
            collapseFab(fabMyQueriesLayout, fabAskQueryLayout, fabMain);
        });

        fabContainer.setOnClickListener(v -> {
            fabContainer.setClickable(false);
            fabMyQueriesLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabMyQueriesLayout.setVisibility(View.GONE)).start();
            fabAskQueryLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabAskQueryLayout.setVisibility(View.GONE)).start();
            isFabOpen = false;
            fabMain.setImageResource(R.drawable.add);
        });

        // Initialize adapter
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", "");
        chatAdapter = new ChatAdapter(Chats, getContext(), userId);
        chatAdapter.setOnLikeToggleListener(query -> {
            // Update the query in allChats list
            for (int i = 0; i < allChats.size(); i++) {
                Query q = allChats.get(i);
                if (q.getDocumentId() != null && q.getDocumentId().equals(query.getDocumentId())) {
                    allChats.set(i, query);
                    break;
                }
            }
            // Re-apply filter and sort
            applyFilterAndSort();
        });
        chatRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecycler.setAdapter(chatAdapter);

        // Apply margins for Android 15 (API 35) only
        if (Build.VERSION.SDK_INT >= 35 && linearleft != null) {
            ViewGroup.LayoutParams paramsleft = linearleft.getLayoutParams();
            ViewGroup.MarginLayoutParams marginLayoutParamsLeft = (ViewGroup.MarginLayoutParams) paramsleft;
            marginLayoutParamsLeft.setMargins(0, 98, 0, 0);
            linearleft.setLayoutParams(paramsleft);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Register back press callback
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        View view = getView();
                        if (view != null && isFabOpen) {
                            View fabMain = view.findViewById(R.id.fab_main);
                            View fabMyQueriesLayout = view.findViewById(R.id.fab_my_queries_layout);
                            View fabAskQueryLayout = view.findViewById(R.id.fab_ask_query_layout);
                            
                            if (fabMain != null && fabMyQueriesLayout != null && fabAskQueryLayout != null) {
                                collapseFab(fabMyQueriesLayout, fabAskQueryLayout, (FloatingActionButton) fabMain);
                            } else {
                                setEnabled(false);
                                requireActivity().onBackPressed();
                            }
                        } else {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                }
        );
        
        // Set bottom margins based on Android version
        setBottomMarginsBasedOnAndroidVersion();
        
        // Load data
        fetchChatData();
        initSlider();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ensure RecyclerView is visible and refresh data if needed
        if (chatRecycler != null) {
            chatRecycler.setVisibility(View.VISIBLE);
        }
        if (chatAdapter != null && Chats.isEmpty()) {
            fetchChatData();
        } else if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }

    private void collapseFab(View fabMyQueriesLayout, View fabAskQueryLayout, FloatingActionButton fabMain) {
        fabMyQueriesLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabMyQueriesLayout.setVisibility(View.GONE)).start();
        fabAskQueryLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabAskQueryLayout.setVisibility(View.GONE)).start();
        isFabOpen = false;
        fabMain.setImageResource(R.drawable.add);
    }

    private void fetchChatData() {
        ApiClient.getInstance().getQueries(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.w(TAG, "Failed to load queries", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Unsuccessful response: " + response.code());
                    response.close();
                    return;
                }
                String body = response.body().string();
                response.close();
                try {
                    JSONArray array;
                    if (body.trim().startsWith("[")) {
                        array = new JSONArray(body);
                    } else {
                        JSONObject wrapper = new JSONObject(body);
                        array = wrapper.optJSONArray("queries");
                        if (array == null) array = new JSONArray();
                    }
                    ArrayList<Query> parsed = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        parsed.add(parseQueryFromJson(obj));
                    }
                    
                    android.app.Activity activity = getActivity();
                    if (activity != null && isAdded()) {
                        activity.runOnUiThread(() -> {
                            if (isAdded() && getActivity() != null && getView() != null && chatAdapter != null) {
                                // Store all chats for filtering
                                allChats.clear();
                                allChats.addAll(parsed);
                                
                                // Apply filter and sort
                                applyFilterAndSort();
                                
                                if (chatRecycler != null) {
                                    chatRecycler.setVisibility(View.VISIBLE);
                                }
                                Log.d(TAG, "Data loaded: " + parsed.size() + " items");
                            }
                        });
                    }
                } catch (JSONException ex) {
                    Log.e(TAG, "JSON parse error", ex);
                }
            }
        });
    }

    private Query parseQueryFromJson(JSONObject obj) throws JSONException {
        String id = obj.optString("id", "");
        String userId = obj.optString("user_id", "");
        String name = obj.optString("name", "");
        String education = obj.optString("education", "");
        String type = obj.optString("type", "");
        String title = obj.optString("title", "");
        String uploadTimeStr = obj.optString("upload_time", null);
        Timestamp uploadTs = parseIsoToTimestamp(uploadTimeStr);

        String replyText = obj.optString("reply_text", "").trim();
        String replyTsStr = obj.optString("reply_timestamp", null);
        Timestamp replyTs = parseIsoToTimestamp(replyTsStr);

        Reply reply = null;
        if (!replyText.isEmpty()) {
            String replyUserRs = obj.optString("reply_user_rs", "default_reply_icon");
            String replyUserName = "One Roadmap";
            reply = new Reply(replyUserName, replyText, replyUserRs, replyTs != null ? replyTs : Timestamp.now());
        }

        ArrayList<String> liked = new ArrayList<>();
        JSONArray likedArr = obj.optJSONArray("liked_by_users");
        if (likedArr != null) {
            for (int j = 0; j < likedArr.length(); j++) {
                liked.add(likedArr.optString(j));
            }
        }
        int likeCount = liked.size();

        String userRs = obj.optString("user_rs", "girl_profile");

        return new Query(
                userId,
                name,
                education,
                type,
                title,
                uploadTs != null ? uploadTs : Timestamp.now(),
                userRs,
                reply,
                false,
                likeCount,
                id,
                liked
        );
    }

    private Timestamp parseIsoToTimestamp(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(iso);
            if (date != null) return new Timestamp(date);
        } catch (Exception ignored) {}
        try {
            java.text.SimpleDateFormat sdfMs = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            sdfMs.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdfMs.parse(iso);
            if (date != null) return new Timestamp(date);
        } catch (Exception ignored2) {}
        return null;
    }

    private void initSlider() {
        executorService.execute(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("slider").get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<CarouselItem> carouselItems = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String imageUrl = document.getString("imageUrl");
                                String caption = document.getString("title");
                                carouselItems.add(new CarouselItem(imageUrl, caption));
                            }
                            mainHandler.post(() -> {
                                if (carousel != null && isAdded() && getView() != null) {
                                    carousel.addData(carouselItems);
                                }
                            });
                        } else {
                            Log.w(TAG, "Error getting slider data: ", task.getException());
                        }
                    });
        });
    }

    private void setBottomMarginsBasedOnAndroidVersion() {
        if (Build.VERSION.SDK_INT >= 34) {
            if (chatRecycler != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chatRecycler.getLayoutParams();
                params.bottomMargin = dpToPx(128);
                chatRecycler.setLayoutParams(params);
            }
            
            View view = getView();
            if (view != null) {
                View fabMain = view.findViewById(R.id.fab_main);
                if (fabMain != null) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fabMain.getLayoutParams();
                    params.bottomMargin = dpToPx(144);
                    fabMain.setLayoutParams(params);
                }
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    /**
     * Apply filter and sort to chats (matching iOS implementation)
     */
    private void applyFilterAndSort() {
        ArrayList<Query> filteredChats = new ArrayList<>(allChats);
        
        // Apply filter by type
        if (!selectedFilter.equals("all")) {
            String filterType = null;
            switch (selectedFilter) {
                case "career":
                    filterType = "Career";
                    break;
                case "maharashtra":
                    filterType = "Maharashtra Government";
                    break;
                case "banking":
                    filterType = "Banking";
                    break;
            }
            
            if (filterType != null) {
                ArrayList<Query> temp = new ArrayList<>();
                for (Query query : filteredChats) {
                    if (filterType.equals(query.getType())) {
                        temp.add(query);
                    }
                }
                filteredChats = temp;
            }
        }
        
        // Apply sorting
        if (sortOption.equals("popular")) {
            // Sort by most liked (likedByUsers count descending)
            filteredChats.sort((q1, q2) -> {
                int likes1 = q1.getLikedByUsers() != null ? q1.getLikedByUsers().size() : 0;
                int likes2 = q2.getLikedByUsers() != null ? q2.getLikedByUsers().size() : 0;
                return Integer.compare(likes2, likes1); // Descending order
            });
        } else {
            // Sort by most recent (uploadTime descending)
            filteredChats.sort((q1, q2) -> {
                com.google.firebase.Timestamp ts1 = q1.getUploadTime();
                com.google.firebase.Timestamp ts2 = q2.getUploadTime();
                if (ts1 == null && ts2 == null) return 0;
                if (ts1 == null) return 1;
                if (ts2 == null) return -1;
                return ts2.compareTo(ts1); // Descending order (newest first)
            });
        }
        
        // Update displayed chats
        Chats.clear();
        Chats.addAll(filteredChats);
        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Update filter button background states
     */
    private void updateFilterButtonStates() {
        if (filterAll == null || filterCareer == null || filterMaharashtra == null || filterBanking == null) {
            return;
        }
        
        // Reset all filter buttons
        filterAll.setBackgroundResource(R.drawable.rectangle_with_stroke);
        filterAll.setTextColor(getResources().getColor(R.color.text_primary, null));
        
        filterCareer.setBackgroundResource(R.drawable.rectangle_with_stroke);
        filterCareer.setTextColor(getResources().getColor(R.color.text_primary, null));
        
        filterMaharashtra.setBackgroundResource(R.drawable.rectangle_with_stroke);
        filterMaharashtra.setTextColor(getResources().getColor(R.color.text_primary, null));
        
        filterBanking.setBackgroundResource(R.drawable.rectangle_with_stroke);
        filterBanking.setTextColor(getResources().getColor(R.color.text_primary, null));
        
        // Set selected filter button
        switch (selectedFilter) {
            case "all":
                filterAll.setBackgroundResource(R.drawable.rectangle_purple);
                filterAll.setTextColor(getResources().getColor(android.R.color.white, null));
                break;
            case "career":
                filterCareer.setBackgroundResource(R.drawable.rectangle_purple);
                filterCareer.setTextColor(getResources().getColor(android.R.color.white, null));
                break;
            case "maharashtra":
                filterMaharashtra.setBackgroundResource(R.drawable.rectangle_purple);
                filterMaharashtra.setTextColor(getResources().getColor(android.R.color.white, null));
                break;
            case "banking":
                filterBanking.setBackgroundResource(R.drawable.rectangle_purple);
                filterBanking.setTextColor(getResources().getColor(android.R.color.white, null));
                break;
        }
    }
    
    /**
     * Update sort button background states
     */
    private void updateSortButtonStates() {
        if (sortPopular == null || sortRecent == null) {
            return;
        }
        
        // Reset all sort buttons
        sortPopular.setBackgroundResource(R.drawable.rectangle_with_stroke);
        sortPopular.setTextColor(getResources().getColor(R.color.text_primary, null));
        
        sortRecent.setBackgroundResource(R.drawable.rectangle_with_stroke);
        sortRecent.setTextColor(getResources().getColor(R.color.text_primary, null));
        
        // Set selected sort button
        if (sortOption.equals("popular")) {
            sortPopular.setBackgroundResource(R.drawable.rectangle_purple);
            sortPopular.setTextColor(getResources().getColor(android.R.color.white, null));
        } else {
            sortRecent.setBackgroundResource(R.drawable.rectangle_purple);
            sortRecent.setTextColor(getResources().getColor(android.R.color.white, null));
        }
    }
}
