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

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import java.util.ArrayList;
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("chats").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Chats.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getString("userId");
                            String username = document.getString("name");
                            String qualification = document.getString("education");
                            String category = document.getString("type");
                            String question = document.getString("title");
                            Timestamp timestamp = document.getTimestamp("uploadTime");
                            boolean isReplied = document.getBoolean("likeBoolean");
                            String documentId = document.getId();

                            String questionTime = getRelativeTime(timestamp);

                            String replyText = document.getString("replyText");
                            Timestamp replyTimestamp = document.getTimestamp("replyTimestamp");
                            String replyTime = getRelativeTime(replyTimestamp);

                            Reply reply = new Reply(
                                    "One Roadmap",
                                    replyText != null ? replyText : "",
                                    replyTimestamp != null ? replyTimestamp : Timestamp.now()
                            );

                            List<String> likedByUsers = document.get("likedByUsers") != null
                                    ? (List<String>) document.get("likedByUsers")
                                    : new ArrayList<>();

                            int likeCount = likedByUsers.size();

                            Chats.add(new Query(
                                    userId,
                                    username,
                                    qualification,
                                    category,
                                    question + " (" + questionTime + ")",
                                    timestamp != null ? timestamp : Timestamp.now(),
                                    "R.drawable.question_icon",
                                    reply,
                                    isReplied,
                                    likeCount,
                                    documentId,
                                    likedByUsers
                            ));
                        }
                        chatAdapter.notifyDataSetChanged();
                    } else {
                        Log.w("FirestoreError", "Error getting chat data: ", task.getException());
                    }
                });
    }

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
