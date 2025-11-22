package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatFragment extends Fragment {

    private final ArrayList<Query> Chats = new ArrayList<>();
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private ImageCarousel carousel;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LinearLayout linearleft, linearright;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        chatRecycler = view.findViewById(R.id.chat_recycler);
        carousel = view.findViewById(R.id.carousel);
        linearleft = view.findViewById(R.id.linearleft);

        FloatingActionButton fabMain = view.findViewById(R.id.fab_main);
        FloatingActionButton fabAskQuery = view.findViewById(R.id.fab_ask_query);
        FloatingActionButton fabMyQueries = view.findViewById(R.id.fab_my_queries);
        RelativeLayout fabContainer = view.findViewById(R.id.fab_container);

        LinearLayout fabAskQueryLayout = view.findViewById(R.id.fab_ask_query_layout);
        LinearLayout fabMyQueriesLayout = view.findViewById(R.id.fab_my_queries_layout);

        final boolean[] isFabOpen = {false};

        fabMain.setOnClickListener(v -> {
            if (!isFabOpen[0]) {
                fabContainer.setClickable(true);
                fabMyQueriesLayout.setVisibility(View.VISIBLE);
                fabAskQueryLayout.setVisibility(View.VISIBLE);
                fabMyQueriesLayout.animate().alpha(1f).translationY(0).setDuration(300).start();
                fabAskQueryLayout.animate().alpha(1f).translationY(0).setDuration(300).start();
                isFabOpen[0] = true;
                fabMain.setImageResource(R.drawable.close); // switch to X
            } else {
                fabContainer.setClickable(false);
                fabMyQueriesLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabMyQueriesLayout.setVisibility(View.GONE)).start();
                fabAskQueryLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabAskQueryLayout.setVisibility(View.GONE)).start();
                isFabOpen[0] = false;
                fabMain.setImageResource(R.drawable.add); // back to +
            }
        });

        fabAskQuery.setOnClickListener(v -> {
            AskQuery bottomSheet = new AskQuery();
            bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
            collapseFab(fabMyQueriesLayout, fabAskQueryLayout, fabMain, isFabOpen);
        });

        fabMyQueries.setOnClickListener(v -> {
            MyQueries fragment = new MyQueries();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
            collapseFab(fabMyQueriesLayout, fabAskQueryLayout, fabMain, isFabOpen);
        });

        fabContainer.setOnClickListener(v -> {
            fabContainer.setClickable(false);
            fabMyQueriesLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabMyQueriesLayout.setVisibility(View.GONE)).start();
            fabAskQueryLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabAskQueryLayout.setVisibility(View.GONE)).start();
            isFabOpen[0] = false;
            fabMain.setImageResource(R.drawable.add); // back to +
        });


        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isFabOpen[0]) {
                            collapseFab(fabMyQueriesLayout, fabAskQueryLayout, fabMain, isFabOpen);
                        } else {
                            // Default back behavior
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                }
        );

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", "");

        chatAdapter = new ChatAdapter(Chats, getContext(), userId);
        chatRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecycler.setAdapter(chatAdapter);

        ViewGroup.LayoutParams paramsleft = linearleft.getLayoutParams();

        // Apply margins for Android 15 (API 35) only
        if (Build.VERSION.SDK_INT >= 35) {
            ViewGroup.MarginLayoutParams marginLayoutParamsLeft = (ViewGroup.MarginLayoutParams) paramsleft;
            marginLayoutParamsLeft.setMargins(0, 98,0,0);
            linearleft.setLayoutParams(paramsleft);
        }

        fetchChatData();
        initSlider();

        return view;
    }

    private void collapseFab(View fabMyQueriesLayout, View fabAskQueryLayout, FloatingActionButton fabMain, boolean[] isFabOpen) {
        fabMyQueriesLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabMyQueriesLayout.setVisibility(View.GONE)).start();
        fabAskQueryLayout.animate().alpha(0f).translationY(50).setDuration(200).withEndAction(() -> fabAskQueryLayout.setVisibility(View.GONE)).start();
        isFabOpen[0] = false;
        fabMain.setImageResource(R.drawable.add); // back to +
    }

    private void fetchChatData() {
        ApiClient.getInstance().getQueries(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.w("QueriesAPI", "Failed to load queries", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    Log.w("QueriesAPI", "Unsuccessful response: " + response.code());
                    response.close();
                    return;
                }
                String body = response.body().string();
                response.close();
                try {
                    JSONArray array;
                    // Handle either raw array or wrapped object: {"queries":[...]}
                    if (body.trim().startsWith("[")) {
                        array = new JSONArray(body);
                    } else {
                        JSONObject wrapper = new JSONObject(body);
                        array = wrapper.optJSONArray("queries");
                        if (array == null) array = new JSONArray(); // fallback to empty
                    }
                    ArrayList<Query> parsed = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Log.d("chatdatafromserver", obj.toString());
                        parsed.add(parseQueryFromJson(obj));
                    }
                    // Check if fragment is still attached before accessing activity
                    android.app.Activity activity = getActivity();
                    if (activity != null && isAdded()) {
                        activity.runOnUiThread(() -> {
                            // Double-check fragment is still attached before updating UI
                            if (isAdded() && getActivity() != null) {
                                Chats.clear();
                                Chats.addAll(parsed);
                                chatAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (JSONException ex) {
                    Log.e("QueriesAPI", "JSON parse error", ex);
                }
            }
        });
    }

    private Query parseQueryFromJson(JSONObject obj) throws JSONException {
        String id = obj.optString("id", "");
        String userId = obj.optString("user_id", "");  // snake_case
        String name = obj.optString("name", "");
        String education = obj.optString("education", "");
        String type = obj.optString("type", "");
        String title = obj.optString("title", "");
        String uploadTimeStr = obj.optString("upload_time", null);
        Timestamp uploadTs = parseIsoToTimestamp(uploadTimeStr);

        // --- REPLY ---
        String replyText = obj.optString("reply_text", "").trim();
        String replyTsStr = obj.optString("reply_timestamp", null);
        Timestamp replyTs = parseIsoToTimestamp(replyTsStr);

        Reply reply = null;
        if (!replyText.isEmpty()) {
            String replyUserRs = obj.optString("reply_user_rs", "default_reply_icon");
            String replyUserName = "One Roadmap"; // or fetch from user_rs mapping if needed
            reply = new Reply(replyUserName, replyText,replyUserRs, replyTs != null ? replyTs : Timestamp.now());
        }

        // --- LIKES ---
        ArrayList<String> liked = new ArrayList<>();
        JSONArray likedArr = obj.optJSONArray("liked_by_users");
        if (likedArr != null) {
            for (int j = 0; j < likedArr.length(); j++) {
                liked.add(likedArr.optString(j));
            }
        }
        int likeCount = liked.size();

        // --- AVATAR ---
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
    // No need to convert server time strings into Timestamp for display; we use TimeAgoUtil instead.

    private String getRelativeTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        long timeInMillis = timestamp.toDate().getTime();
        return DateUtils.getRelativeTimeSpanString(
                timeInMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString();
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
                                carousel.addData(carouselItems);
                            });
                        } else {
                            Log.w("FirestoreError", "Error getting slider data: ", task.getException());
                        }
                    });
        });
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set bottom margins based on Android version
        setBottomMarginsBasedOnAndroidVersion();
    }
    

    private void setBottomMarginsBasedOnAndroidVersion() {
        if (android.os.Build.VERSION.SDK_INT >= 34) { // Android 15 is API 34
            // Set larger margins for Android 15+
            if (chatRecycler != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chatRecycler.getLayoutParams();
                params.bottomMargin = dpToPx(128);
                chatRecycler.setLayoutParams(params);
                Log.d("ChatFragment", "Set chatRecycler bottom margin to 128dp for Android 15+");
            }
            
            // Find and update FAB margin
            View fabMain = getView().findViewById(R.id.fab_main);
            if (fabMain != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fabMain.getLayoutParams();
                params.bottomMargin = dpToPx(144);
                fabMain.setLayoutParams(params);
                Log.d("ChatFragment", "Set fabMain bottom margin to 144dp for Android 15+");
            }
        }
    }
    
    /**
     * Converts dp to pixels
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
