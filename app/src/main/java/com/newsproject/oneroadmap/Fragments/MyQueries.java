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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.newsproject.oneroadmap.Adapters.ChatAdapter;
import com.newsproject.oneroadmap.Models.Query;
import com.newsproject.oneroadmap.Models.Reply;
import com.newsproject.oneroadmap.R;

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
}