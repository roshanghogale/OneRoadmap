// File: SavedJobsFragment.java
package com.newsproject.oneroadmap.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.newsproject.oneroadmap.Adapters.JobUpdateAdapter;
import com.newsproject.oneroadmap.database.SavedJobsDatabaseHelper;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;
import java.util.ArrayList;
import java.util.List;

public class SavedJobsFragment extends Fragment {

    private RecyclerView recyclerView;
    private JobUpdateAdapter adapter;
    private List<JobUpdate> savedJobs = new ArrayList<>();
    private SavedJobsDatabaseHelper dbHelper;
    private TextView emptyText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_jobs, container, false);

        recyclerView = view.findViewById(R.id.saved_recycler);
        emptyText = view.findViewById(R.id.empty_text);
        dbHelper = new SavedJobsDatabaseHelper(requireContext());

        setupRecyclerView();
        loadSavedJobs();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new JobUpdateAdapter(savedJobs, getParentFragmentManager());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadSavedJobs() {
        savedJobs.clear();
        savedJobs.addAll(dbHelper.getAllSavedJobs());
        adapter.updateJobs(savedJobs);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (savedJobs.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedJobs(); // Refresh when returning
    }
}