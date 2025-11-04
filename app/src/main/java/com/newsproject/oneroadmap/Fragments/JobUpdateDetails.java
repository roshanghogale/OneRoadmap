package com.newsproject.oneroadmap.Fragments;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.newsproject.oneroadmap.Adapters.RecentlyOpenedAdapter;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.Models.RecentlyOpenedItem;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;
import com.newsproject.oneroadmap.database.SavedJobsDatabaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobUpdateDetails extends Fragment {
    private static final String TAG = "JobUpdateDetails";
    private Handler handler;
    private JobUpdate jobUpdate;
    private SavedJobsDatabaseHelper dbHelper;
    private ImageView saveButton;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_job_update_details, container, false);

        handler = new Handler(Looper.getMainLooper());
        dbHelper = new SavedJobsDatabaseHelper(requireContext());
        saveButton = view.findViewById(R.id.imageView3); // Save icon

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
        noteValue.setText(jobUpdate.getNote() != null && !jobUpdate.getNote().isEmpty() ? jobUpdate.getNote() : "No note available");

        // === LINK VISIBILITY & CLICK HANDLING ===
        setupLink(view, R.id.application_link_container, R.id.textView45, jobUpdate.getApplicationLink(), "अर्जाची लिंक");
        setupLink(view, R.id.notification_pdf_container, R.id.textView34, jobUpdate.getNotificationPdfLink(), "नोटिफिकेशन PDF");
        setupLink(view, R.id.selection_pdf_container, R.id.textView48, jobUpdate.getSelectionPdfLink(), "सिलेक्शन PDF");
        setupLink(view, R.id.syllabus_pdf_container, R.id.textView44, jobUpdate.getSyllabusPdf(), "अभ्यासक्रम PDF");

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
        if (jobUpdate != null) {
            Log.d(TAG, "JobUpdate already available, skipping Firebase fetch");
            populateUIFromObject(view);
            return;
        }

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
                                populateUIFromObject(view);
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<RecentlyOpenedItem> dummyList = new ArrayList<>();
        dummyList.add(new RecentlyOpenedItem(R.drawable.hdfc_bank3x, "३१ सप्टेंबर २०२४", "सार्वजनिक सेवा विभाग", "प्रशासकीय सहाय्यक"));
        dummyList.add(new RecentlyOpenedItem(R.drawable.hdfc_bank3x, "१५ ऑक्टोबर २०२४", "महानगरपालिका", "लिपिक"));
        dummyList.add(new RecentlyOpenedItem(R.drawable.hdfc_bank3x, "०५ नोव्हेंबर २०२४", "पोलीस भरती", "कॉन्स्टेबल"));
        dummyList.add(new RecentlyOpenedItem(R.drawable.hdfc_bank3x, "३१ सप्टेंबर २०२४", "सार्वजनिक सेवा विभाग", "प्रशासकीय सहाय्यक"));
        dummyList.add(new RecentlyOpenedItem(R.drawable.hdfc_bank3x, "१५ ऑक्टोबर २०२४", "महानगरपालिका", "लिपिक"));
        dummyList.add(new RecentlyOpenedItem(R.drawable.hdfc_bank3x, "०५ नोव्हेंबर २०२४", "पोलीस भरती", "कॉन्स्टेबल"));

        RecentlyOpenedAdapter adapter = new RecentlyOpenedAdapter(dummyList);
        recyclerView.setAdapter(adapter);
    }
}