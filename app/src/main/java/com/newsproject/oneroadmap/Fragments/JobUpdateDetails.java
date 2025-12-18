package com.newsproject.oneroadmap.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.newsproject.oneroadmap.Adapters.RecentlyOpenedAdapter;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;
import com.newsproject.oneroadmap.Utils.ShareHelper;
import com.newsproject.oneroadmap.Utils.DatabaseHelper;
import com.newsproject.oneroadmap.Utils.CoinManager;
import com.newsproject.oneroadmap.database.RecentlyOpenedDatabaseHelper;
import com.newsproject.oneroadmap.database.SavedJobsDatabaseHelper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class JobUpdateDetails extends Fragment {
    private static final String TAG = "JobUpdateDetails";
    private Handler handler;
    private JobUpdate jobUpdate;
    private SavedJobsDatabaseHelper dbHelper;
    private RecentlyOpenedDatabaseHelper recentDb;
    private ImageView saveButton;
    private FloatingActionButton fabShare;
    private ShareHelper shareHelper;
    private ActivityResultLauncher<Intent> shareLauncher;
    private DatabaseHelper coinDbHelper;
    private String userId;
    private int displayedCoins = 0;

    public JobUpdateDetails() {
        // Required empty public constructor
    }

    public static JobUpdateDetails newInstance(JobUpdate jobUpdate) {
        JobUpdateDetails fragment = new JobUpdateDetails();
        Bundle args = new Bundle();
        if (jobUpdate != null) {
            args.putParcelable("jobUpdate", jobUpdate);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Deprecated
    public static JobUpdateDetails newInstance(String documentId) {
        JobUpdateDetails fragment = new JobUpdateDetails();
        Bundle args = new Bundle();
        args.putString("documentId", documentId);
        fragment.setArguments(args);
        return fragment;
    }

    public JobUpdate getFullJobUpdate() {
        if (jobUpdate != null) {
            return jobUpdate;
        }
        if (getArguments() != null) {
            jobUpdate = getArguments().getParcelable("jobUpdate");
        }
        return jobUpdate;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jobUpdate = getFullJobUpdate();
        if (jobUpdate == null) {
            Log.w(TAG, "No JobUpdate object provided, fragment may fail to load");
        }
        
        // Get userId for coin management
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        coinDbHelper = new DatabaseHelper(requireContext());
        
        // Initialize ShareHelper
        shareHelper = new ShareHelper(requireContext());
        
        // Initialize share launcher
        shareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // When user returns from sharing, add coins and show dialog
                    // If callback is triggered, it means WhatsApp was opened, so add coins
                    android.util.Log.d(TAG, "Share result received, resultCode: " + result.getResultCode());
                    if (userId != null && !userId.isEmpty()) {
                        int current = coinDbHelper.getUserCoins(userId);
                        android.util.Log.d(TAG, "Current coins before share: " + current);
                        // Use CoinManager to add coins (handles both local DB and server)
                        CoinManager coinManager = new CoinManager(requireContext(), userId);
                        coinManager.addCoinsForShare(newCoins -> {
                            android.util.Log.d(TAG, "Coins added! New coins: " + newCoins);
                            // Coins added and saved to server, show dialog
                            showCoinAnimationDialog(current, newCoins);
                        });
                    } else {
                        android.util.Log.w(TAG, "userId is null or empty, cannot add coins");
                    }
                });
        
        shareHelper.setShareLauncher(shareLauncher);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_job_update_details, container, false);

        handler = new Handler(Looper.getMainLooper());
        dbHelper = new SavedJobsDatabaseHelper(requireContext());
        recentDb = new RecentlyOpenedDatabaseHelper(requireContext());
        saveButton = view.findViewById(R.id.imageView3); // Save icon
        fabShare = view.findViewById(R.id.fab_share); // Share FAB

        // Set initial icon
        updateSaveButtonIcon();

        // Save/Unsave on click
        saveButton.setOnClickListener(v -> {
            if (jobUpdate == null) return;

            if (dbHelper.isJobSaved(jobUpdate.getDocumentId())) {
                dbHelper.deleteJob(jobUpdate.getDocumentId());
                Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.saveJob(jobUpdate);
                Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
            }
            updateSaveButtonIcon();
        });
        
        // Share button click
        fabShare.setOnClickListener(v -> {
            if (jobUpdate != null) {
                String title = jobUpdate.getPostName() != null ? jobUpdate.getPostName() : "Government Job Alert";
                String url = jobUpdate.getApplicationLink();
                String imageUrl = jobUpdate.getImageUrl(); // Banner image
                shareHelper.shareJobWithImage(title, url, imageUrl);
            }
        });

        if (jobUpdate != null) {
            Log.d(TAG, "Using passed JobUpdate object for ID: " + jobUpdate.getDocumentId());
            populateUIFromObject(view);
        } else {
            String documentId = getArguments() != null ? getArguments().getString("documentId") : null;
            if (documentId != null) {
                Log.d(TAG, "Fetching from Firebase with documentId: " + documentId);
                fetchJobUpdate(documentId, view);
            } else {
                Toast.makeText(requireContext(), "No job data provided", Toast.LENGTH_SHORT).show();
                return view;
            }
        }

        return view;
    }

    private void updateSaveButtonIcon() {
        if (jobUpdate != null && dbHelper.isJobSaved(jobUpdate.getDocumentId())) {
            saveButton.setImageResource(R.drawable.save_filled);
        } else {
            saveButton.setImageResource(R.drawable.save);
        }
    }

    private void populateUIFromObject(View view) {
        // Load image
        ImageView imageView25 = view.findViewById(R.id.imageView25);
        Glide.with(requireContext())
                .load(jobUpdate.getImageUrl())
                .placeholder(R.drawable.job_details)
                .error(R.drawable.job_details)
                .into(imageView25);

        // Basic Details
        TextView postNameValue = view.findViewById(R.id.post_value);
        TextView educationRequirementValue = view.findViewById(R.id.education_requirement_value);
        TextView ageRequirementValue = view.findViewById(R.id.age_requirement_value);
        TextView jobPlaceValue = view.findViewById(R.id.job_place_value);
        TextView applicationFeesValue = view.findViewById(R.id.application_fees_value);
        TextView lastDateValue = view.findViewById(R.id.last_date_value);
        TextView noteValue = view.findViewById(R.id.note_value);

        postNameValue.setText(jobUpdate.getPostName());
        educationRequirementValue.setText(jobUpdate.getEducationRequirementText());
        ageRequirementValue.setText(jobUpdate.getAgeRequirement());
        jobPlaceValue.setText(jobUpdate.getJobPlace());
        applicationFeesValue.setText(jobUpdate.getFormattedApplicationFees());
        lastDateValue.setText(jobUpdate.getFormattedLastDateMarathi());
        
        // Handle Note visibility and text
        LinearLayout noteContainer = view.findViewById(R.id.note_container);
        String note = jobUpdate.getNote();
        if (note != null && !note.isEmpty()) {
            noteContainer.setVisibility(View.VISIBLE);
            noteValue.setText(note);
            // Ensure purple color for note text
            noteValue.setTextColor(Color.parseColor("#5645C0"));
        } else {
            noteContainer.setVisibility(View.GONE);
        }

        // === LINK VISIBILITY & CLICK HANDLING ===
        setupLink(view, R.id.application_link_container, R.id.textView45, jobUpdate.getApplicationLink(), "अर्जाची लिंक");
        setupLink(view, R.id.notification_pdf_container, R.id.textView34, jobUpdate.getNotificationPdfLink(), "नोटिफिकेशन PDF");
        setupLink(view, R.id.selection_pdf_container, R.id.textView48, jobUpdate.getSelectionPdfLink(), "सिलेक्शन PDF");
        setupLink(view, R.id.syllabus_pdf_container, R.id.textView44, jobUpdate.getSyllabusPdf(), "अभ्यासक्रम PDF");

        // Save to Recently Opened FIRST
        recentDb.addOrUpdateJob(jobUpdate);

        // Then load the list (excludes current job)
        loadRecentlyOpened(view);
    }

    private void setupLink(View root, int containerId, int textViewId, String url, String defaultText) {
        View container = root.findViewById(containerId);
        TextView textView = root.findViewById(textViewId);

        if (url != null && !url.trim().isEmpty()) {
            container.setVisibility(View.VISIBLE);
            textView.setText(defaultText);
            textView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });
        } else {
            container.setVisibility(View.GONE);
        }
    }

    private void fetchJobUpdate(String documentId, View view) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("jobUpdate").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded()) {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "Document data: " + documentSnapshot.getData());
                            jobUpdate = documentSnapshot.toObject(JobUpdate.class);
                            if (jobUpdate != null) {
                                jobUpdate.setDocumentId(documentId);
                                jobUpdate.setTimeAgo(TimeAgoUtil.getTimeAgo(jobUpdate.getTimestamp()));
                                populateUIFromObject(view);  // This handles save + load
                            } else {
                                Log.e(TAG, "Failed to deserialize JobUpdate for document: " + documentId);
                                Toast.makeText(requireContext(), "Failed to load job data", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "Document does not exist: " + documentId);
                            Toast.makeText(requireContext(), "Job data not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Log.e(TAG, "Error fetching document: " + documentId, e);
                        String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        if (errorMessage.contains("The query requires an index")) {
                            errorMessage += ". Please create the index in the Firebase console.";
                        }
                        Toast.makeText(requireContext(), "Failed to load job: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadRecentlyOpened(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recent_post_recyler);

        // ---- 1. find the views that belong to the block -----------------
        TextView tvTitle   = view.findViewById(R.id.textView38);   // "Recent Result"
        // (optional) you could also hide the whole container if you wrap them

        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<JobUpdate> allRecent = recentDb.getAllRecent();
        List<JobUpdate> filtered = new ArrayList<>();

        // Exclude current job
        if (jobUpdate != null) {
            for (JobUpdate j : allRecent) {
                if (!j.getDocumentId().equals(jobUpdate.getDocumentId())) {
                    filtered.add(j);
                }
            }
        } else {
            filtered.addAll(allRecent);
        }

        // ---- 2. decide visibility ---------------------------------------
        int itemCount = filtered.size();
        boolean hasItems = itemCount > 0;

        tvTitle.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);

        // ---- 3. set adapter (only when there are items) -----------------
        if (hasItems) {
            RecentlyOpenedAdapter adapter = new RecentlyOpenedAdapter(filtered, job -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, JobUpdateDetails.newInstance(job))
                        .addToBackStack(null)
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        } else {
            recyclerView.setAdapter(null);   // clear any previous adapter
        }
    }
    
    private void showCoinAnimationDialog(int start, int end) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.coin_dialog_layout, null);
        TextView count = view.findViewById(R.id.coin_count);
        Button ok = view.findViewById(R.id.ok_button);
        count.setText("Coins: " + start);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        displayedCoins = start;
        handler.post(new Runnable() {
            @Override public void run() {
                if (displayedCoins < end) {
                    displayedCoins++;
                    count.setText("Coins: " + displayedCoins);
                    handler.postDelayed(this, 20);
                }
            }
        });
    }
    
}