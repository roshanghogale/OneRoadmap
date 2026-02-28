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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.newsproject.oneroadmap.Adapters.RecentlyOpenedAdapter;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.CoinAccessController;
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
    private CoinAccessController coinAccessController;
    private String userId;
    private NativeAd mNativeAd;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_job_update_details, container, false);

        handler = new Handler(Looper.getMainLooper());
        dbHelper = new SavedJobsDatabaseHelper(requireContext());
        recentDb = new RecentlyOpenedDatabaseHelper(requireContext());
        saveButton = view.findViewById(R.id.imageView3); // Save icon
        fabShare = view.findViewById(R.id.fab_share); // Share FAB

        // Load Native Ad
        loadNativeAd(view);

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

    private void loadNativeAd(View rootView) {
        AdLoader adLoader = new AdLoader.Builder(requireContext(), "ca-app-pub-3940256099942544/2247696110") // Test ID
                .forNativeAd(nativeAd -> {
                    if (isAdded()) {
                        if (mNativeAd != null) {
                            mNativeAd.destroy();
                        }
                        mNativeAd = nativeAd;
                        ViewGroup adContainer = rootView.findViewById(R.id.native_ad_container);
                        NativeAdView adView = (NativeAdView) getLayoutInflater().inflate(R.layout.layout_native_ad, null);
                        populateNativeAdView(nativeAd, adView);
                        adContainer.removeAllViews();
                        adContainer.addView(adView);
                        adContainer.setVisibility(View.VISIBLE);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e(TAG, "Native ad failed to load: " + adError.getMessage());
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));

        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        adView.setNativeAd(nativeAd);
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
            noteValue.setTextColor(Color.parseColor("#000000"));
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
                coinAccessController.requestPdfAccess(url, null);


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

        TextView tvTitle   = view.findViewById(R.id.textView38);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<JobUpdate> allRecent = recentDb.getAllRecent();
        List<JobUpdate> filtered = new ArrayList<>();

        if (jobUpdate != null) {
            for (JobUpdate j : allRecent) {
                if (!j.getDocumentId().equals(jobUpdate.getDocumentId())) {
                    filtered.add(j);
                }
            }
        } else {
            filtered.addAll(allRecent);
        }

        int itemCount = filtered.size();
        boolean hasItems = itemCount > 0;

        tvTitle.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);

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
            recyclerView.setAdapter(null);
        }
    }

    @Override
    public void onDestroy() {
        if (mNativeAd != null) {
            mNativeAd.destroy();
        }
        super.onDestroy();
    }
}
