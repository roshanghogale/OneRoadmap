package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.newsproject.oneroadmap.Adapters.ChatAdapter;
import com.newsproject.oneroadmap.Models.Query;
import com.newsproject.oneroadmap.Models.Reply;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ApiClient;
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;


public class MyQueries extends Fragment {

    private final ArrayList<Query> Chats = new ArrayList<>();
    private LinearLayout linearLayout;
    private ImageView backButton;
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private Button askQuery;

    public MyQueries() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_queries, container, false);

        chatRecycler = view.findViewById(R.id.chat_recycler);
        backButton = view.findViewById(R.id.back_button3);
        askQuery = view.findViewById(R.id.ask_query_button2);
        linearLayout = view.findViewById(R.id.linearLayout11);

        ViewGroup.LayoutParams params = linearLayout.getLayoutParams();

        if (Build.VERSION.SDK_INT >= 35) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
            marginLayoutParams.setMargins(0, 100, 0, 0);
            linearLayout.setLayoutParams(params);
        }

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", "");

        chatAdapter = new ChatAdapter(Chats, getContext(), userId);
        chatRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecycler.setAdapter(chatAdapter);

        // Back button click listener
        backButton.setOnClickListener(v -> {
            Log.d("ReelsAdapter", "Back button pressed");
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                Log.w("ReelsAdapter", "No back stack entries to pop");
            }
        });

        askQuery.setOnClickListener(v -> {
            AskQuery fragment = new AskQuery();

            // Navigate to the JobUpdateDetails fragment
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, fragment); // Replace with your container ID
            transaction.addToBackStack(null);
            transaction.commit();
        });

        fetchChatData();

        return view;
    }

    private void fetchChatData() {
        Context ctx = getContext();
        if (ctx == null) return;
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sp.getString("userId", "");
        ApiClient.getInstance().getQueriesByUser(userId, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.w("QueriesAPI", "Failed to load user queries", e);
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
                    // Accept either raw array or {"queries":[...]}
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
                        Log.d("chatdatafromserver", obj.toString());
                        parsed.add(parseQueryFromJson(obj));
                    }
                    requireActivity().runOnUiThread(() -> {
                        Chats.clear();
                        Chats.addAll(parsed);
                        chatAdapter.notifyDataSetChanged();
                    });
                } catch (JSONException ex) {
                    Log.e("QueriesAPI", "JSON parse error", ex);
                }
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

    private Query parseQueryFromJson(JSONObject obj) throws JSONException {
        String id = obj.optString("id", obj.optString("_id", ""));
        String userId = obj.optString("userId", "");
        String name = obj.optString("name", "");
        String education = obj.optString("education", "");
        String type = obj.optString("type", "");
        String title = obj.optString("title", "");
        String uploadTimeStr = obj.optString("uploadTime", null);
        Timestamp uploadTs = parseIsoToTimestamp(uploadTimeStr);

        String replyText = obj.optString("replyText", "");
        String replyTsStr = obj.optString("replyTimestamp", null);
        String replyUserRs = obj.optString("reply_user_rs", "default_reply_icon");
        Reply reply = new Reply("One Roadmap", replyText,replyUserRs, Timestamp.now());

        ArrayList<String> liked = new ArrayList<>();
        JSONArray likedArr = obj.optJSONArray("likedByUsers");
        if (likedArr == null) likedArr = obj.optJSONArray("liked_by_users");
        if (likedArr != null) {
            for (int j = 0; j < likedArr.length(); j++) {
                liked.add(likedArr.optString(j));
            }
        }
        int likeCount = liked.size();

        String questionTime = TimeAgoUtil.getTimeAgo(uploadTimeStr);
        String userRs = obj.optString("userRs", obj.optString("user_rs", "girl_profile"));
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
}