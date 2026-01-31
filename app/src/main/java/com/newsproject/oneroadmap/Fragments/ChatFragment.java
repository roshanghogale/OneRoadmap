package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.newsproject.oneroadmap.Adapters.ChatAdapter;
import com.newsproject.oneroadmap.Models.Query;
import com.newsproject.oneroadmap.Models.Reply;
import com.newsproject.oneroadmap.Models.Slider;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ApiClient;

import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
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
    private String selectedFilter = "all"; // "all" or query type value (can be Marathi or English)
    private String sortOption = "recent"; // "recent" or "popular"
    
    // Filter chips container
    private LinearLayout filterContainer;
    private Map<String, android.widget.TextView> filterChipMap = new HashMap<>();
    
    // Query types from AskQuery (Marathi)
    private final String[] queryTypes = {
            "करिअर प्रश्न",
            "महाराष्ट्र जॉब",
            "केंद्र सरकार जॉब",
            "बँकिंग जॉब",
            "प्राव्हेट जॉब",
            "इतर प्रश्न"
    };
    
    // Map Marathi query types to English type values used in Query model
    private final Map<String, String> queryTypeMap = new HashMap<String, String>() {{
        put("करिअर प्रश्न", "Career");
        put("महाराष्ट्र जॉब", "Maharashtra Government");
        put("केंद्र सरकार जॉब", "Central Government");
        put("बँकिंग जॉब", "Banking");
        put("प्राव्हेट जॉब", "Private");
        put("इतर प्रश्न", "Other");
    }};
    
    // Reverse map: English to Marathi (for filtering)
    private final Map<String, String> reverseQueryTypeMap = new HashMap<String, String>() {{
        put("Career", "करिअर प्रश्न");
        put("Maharashtra Government", "महाराष्ट्र जॉब");
        put("Central Government", "केंद्र सरकार जॉब");
        put("Banking", "बँकिंग जॉब");
        put("Private", "प्राव्हेट जॉब");
        put("Other", "इतर प्रश्न");
    }};
    
    // Sort buttons
    private android.widget.TextView sortPopular, sortRecent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        // Initialize views
        chatRecycler = view.findViewById(R.id.chat_recycler);
        carousel = view.findViewById(R.id.carousel);
        linearleft = view.findViewById(R.id.linearleft);
        filterContainer = view.findViewById(R.id.filter_container);
        sortPopular = view.findViewById(R.id.sort_popular);
        sortRecent = view.findViewById(R.id.sort_recent);
        
        // Setup filter chips
        setupFilterChips();
        
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

        getParentFragmentManager().setFragmentResultListener(
                "ask_query_result",
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {

                    boolean added = bundle.getBoolean("query_added", false);
                    if (added) {
                        refreshChatsAfterSubmit();
                    }
                }
        );


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

    private void refreshChatsAfterSubmit() {

        // Optional: reset filters so new chat is visible
        selectedFilter = "all";
        sortOption = "recent";
        updateFilterChipStates();
        updateSortButtonStates();

        // Clear old data
        Chats.clear();
        allChats.clear();
        chatAdapter.notifyDataSetChanged();

        // Reload from API
        fetchChatData();

        // Scroll to top so new chat is visible
        chatRecycler.post(() -> chatRecycler.scrollToPosition(0));
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

    private boolean isNetworkAvailable() {
        if (!isAdded() || getContext() == null) return false;

        ConnectivityManager cm =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void initSlider() {
        Log.d(TAG, "initSlider started (Chat - All Sliders API)");

        if (carousel == null || !isAdded()) {
            Log.e(TAG, "Carousel is null or fragment not attached");
            return;
        }

        ExecutorService localExecutor = Executors.newSingleThreadExecutor();
        localExecutor.execute(() -> {
            try {
                if (!isNetworkAvailable()) {
                    mainHandler.post(() -> {
                        List<CarouselItem> dummyItems = new ArrayList<>();
                        dummyItems.add(new CarouselItem("https://picsum.photos/400/200", "Chat Dummy 1"));
                        dummyItems.add(new CarouselItem("https://picsum.photos/401/200", "Chat Dummy 2"));
                        carousel.setData(dummyItems);
                        Toast.makeText(requireContext(), "No network, loaded dummy chat sliders", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String url = "https://admin.mahaalert.cloud/api/sliders"; // 🔥 ALL sliders
                Request request = new Request.Builder().url(url).build();

                ApiClient.getInstance()
                        .getInstance()
                        .client
                        .newCall(request)
                        .enqueue(new okhttp3.Callback() {

                            @Override
                            public void onFailure(okhttp3.Call call, IOException e) {
                                Log.e(TAG, "Chat slider API failed", e);
                                mainHandler.post(() ->
                                        Toast.makeText(requireContext(), "Failed to load chat sliders", Toast.LENGTH_SHORT).show()
                                );
                            }

                            @Override
                            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                                if (!response.isSuccessful()) {
                                    Log.e(TAG, "Chat slider bad response: " + response.code());
                                    mainHandler.post(() ->
                                            Toast.makeText(requireContext(), "Failed to load chat sliders", Toast.LENGTH_SHORT).show()
                                    );
                                    return;
                                }

                                String body = response.body().string();
                                Log.d(TAG, "Chat slider API response length: " + body.length());

                                try {
                                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                                    JsonArray arr = root != null ? root.getAsJsonArray("sliders") : null;

                                    if (arr == null) {
                                        Log.w(TAG, "No sliders array found");
                                        return;
                                    }

                                    List<CarouselItem> carouselItems = new ArrayList<>();

                                    for (int i = 0; i < arr.size(); i++) {
                                        Slider slider = new Gson().fromJson(arr.get(i), Slider.class);

                                        if (slider.getImageUrl() == null || slider.getImageUrl().isEmpty()) continue;
                                        if (slider.getPageType() == null) continue;

                                        // ✅ FILTER: ONLY CHAT SLIDERS
                                        if (!"chat".equalsIgnoreCase(slider.getPageType())) continue;

                                        String imageUrl = slider.getImageUrl().replace("http://", "https://");
                                        carouselItems.add(new CarouselItem(imageUrl, slider.getTitle()));
                                    }

                                    mainHandler.post(() -> {
                                        if (!isAdded()) return;

                                        carousel.setData(carouselItems); // ✅ SAFE & CORRECT

                                        if (carouselItems.isEmpty()) {
                                            Toast.makeText(requireContext(), "No chat sliders available", Toast.LENGTH_SHORT).show();
                                            Log.w(TAG, "No chat sliders after filtering");
                                        } else {
                                            Log.d(TAG, "Chat sliders loaded: " + carouselItems.size());
                                        }
                                    });

                                } catch (Exception e) {
                                    Log.e(TAG, "Chat slider parse error", e);
                                    mainHandler.post(() ->
                                            Toast.makeText(requireContext(), "Error parsing chat sliders", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                        });

            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in Chat initSlider()", e);
            } finally {
                localExecutor.shutdown();
            }
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
     * Setup filter chips dynamically based on query types
     */
    private void setupFilterChips() {
        if (filterContainer == null) return;
        
        filterContainer.removeAllViews();
        filterChipMap.clear();
        
        // Add "All" chip
        android.widget.TextView allChip = createFilterChip("All Query", "all");
        filterContainer.addView(allChip);
        filterChipMap.put("all", allChip);
        
        // Add chips for each query type
        for (String queryType : queryTypes) {
            android.widget.TextView chip = createFilterChip(queryType, queryTypeMap.get(queryType));
            filterContainer.addView(chip);
            filterChipMap.put(queryTypeMap.get(queryType), chip);
        }
        
        // Set "All" as selected by default
        updateFilterChipStates();
    }
    
    /**
     * Create a filter chip TextView
     */
    private android.widget.TextView createFilterChip(String text, String filterValue) {
        android.widget.TextView chip = new android.widget.TextView(getContext());
        chip.setText(text);
        chip.setTextSize(12);
        chip.setTypeface(ResourcesCompat.getFont(getContext(), R.font.yantramanav_regular));
        chip.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
        chip.setLayoutParams(params);
        
        chip.setOnClickListener(v -> {
            selectedFilter = filterValue;
            updateFilterChipStates();
            applyFilterAndSort();
        });
        
        return chip;
    }
    
    /**
     * Update filter chip states
     */
    private void updateFilterChipStates() {
        for (Map.Entry<String, android.widget.TextView> entry : filterChipMap.entrySet()) {
            String filterValue = entry.getKey();
            android.widget.TextView chip = entry.getValue();
            
            if (filterValue.equals(selectedFilter)) {
                chip.setBackgroundResource(R.drawable.rectangle_purple);
                chip.setTextColor(getResources().getColor(android.R.color.white, null));
            } else {
                chip.setBackgroundResource(R.drawable.rectangle_with_stroke);
                chip.setTextColor(getResources().getColor(R.color.text_primary, null));
            }
        }
    }
    
    /**
     * Apply filter and sort to chats (matching iOS implementation)
     * Filters by both Marathi and English type values for robustness
     */
    private void applyFilterAndSort() {
        ArrayList<Query> filteredChats = new ArrayList<>(allChats);
        
        // Apply filter by type
        if (!selectedFilter.equals("all")) {
            ArrayList<Query> temp = new ArrayList<>();
            
            // Get both Marathi and English values for the selected filter
            String marathiValue = selectedFilter; // Assume selectedFilter is English mapped value
            String englishValue = selectedFilter;
            
            // If selectedFilter is English, get the Marathi equivalent
            if (reverseQueryTypeMap.containsKey(selectedFilter)) {
                marathiValue = reverseQueryTypeMap.get(selectedFilter);
            }
            // If selectedFilter is Marathi, get the English equivalent
            if (queryTypeMap.containsKey(selectedFilter)) {
                englishValue = queryTypeMap.get(selectedFilter);
                marathiValue = selectedFilter;
            }
            
            for (Query query : filteredChats) {
                String queryType = query.getType();
                if (queryType != null) {
                    // Check if query type matches either Marathi or English value
                    if (queryType.equals(marathiValue) || queryType.equals(englishValue)) {
                        temp.add(query);
                    }
                }
            }
            filteredChats = temp;
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
