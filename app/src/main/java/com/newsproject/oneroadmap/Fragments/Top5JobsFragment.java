package com.newsproject.oneroadmap.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.List;

public class Top5JobsFragment extends Fragment {

    private static final String ARG_JOBS = "jobs";
    private List<JobUpdate> jobUpdates = new ArrayList<>();
    private ViewPager2 viewPager;
    private static final String TAG = "Top5JobsFragment";

    public static Top5JobsFragment newInstance(List<JobUpdate> jobs) {
        Top5JobsFragment fragment = new Top5JobsFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_JOBS, new ArrayList<>(jobs));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jobUpdates = getArguments().getParcelableArrayList(ARG_JOBS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_top5_jobs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.cardPager);
        viewPager.setAdapter(new Top5JobsAdapter(jobUpdates));

        // Configure ViewPager2 for stacked/Tinder-like effect
        viewPager.setOffscreenPageLimit(3);
        viewPager.setClipToPadding(false);
        viewPager.setClipChildren(false);
        viewPager.setPadding(32, 16, 32, 16);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(16));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
            page.setRotation(position * 20);
        });
        viewPager.setPageTransformer(transformer);

        // Dots indicator
        DotsIndicator dotsIndicator = view.findViewById(R.id.dots_indicator);
        dotsIndicator.setViewPager2(viewPager);
    }
    private class Top5JobsAdapter extends RecyclerView.Adapter<Top5JobsAdapter.ViewHolder> {

        private List<JobUpdate> jobs;

        public Top5JobsAdapter(List<JobUpdate> jobs) {
            this.jobs = jobs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.top5_job_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JobUpdate job = jobs.get(position);
            holder.postTitle.setText(job.getTitle());
            holder.itemView.setTag("card_" + position); // Tag for identifying the view
            holder.titleView.setText("Top " + (position + 1));
            holder.postValue.setText(job.getPostName() != null ? job.getPostName() : "N/A");
            holder.educationValue.setText(job.getEducationRequirementText());
            holder.ageValue.setText(job.getAgeRequirement() != null ? job.getAgeRequirement() : "N/A");
            holder.jobPlaceValue.setText(job.getJobPlace() != null ? job.getJobPlace() : "N/A");
            holder.feesValue.setText(job.getFormattedApplicationFees() != null ? job.getFormattedApplicationFees() : "N/A");
            holder.lastDateValue.setText(job.getFormattedLastDate() != null ? job.getFormattedLastDate() : "N/A");
            Glide.with(getContext()).load(job.getIconUrl()).placeholder(R.drawable.image).into(holder.iconView);

            // Set background dynamically
            int mod = position % 3;
            switch (mod) {
                case 0:
                    holder.cardMain.setBackgroundResource(R.drawable.gradient_rectangle_blue);
                    break;
                case 1:
                    holder.cardMain.setBackgroundResource(R.drawable.gradient_rectangle_green);
                    break;
                case 2:
                    holder.cardMain.setBackgroundResource(R.drawable.gradient_rectangle_purple);
                    break;
            }

            // Set click listeners for buttons
            holder.selectionPdfButton.setOnClickListener(v -> {
                String pdfUrl = job.getSelectionPdfLink();
                if (pdfUrl != null && !pdfUrl.isEmpty()) {
                    com.newsproject.oneroadmap.Utils.PdfViewerHelper.openPdfInApp(Top5JobsFragment.this, pdfUrl);
                } else {
                    Toast.makeText(getContext(), "सिलेक्शन PDF उपलब्ध नाही", Toast.LENGTH_SHORT).show();
                }
            });

            holder.applicationLinkButtonCard.setOnClickListener(v -> {
                String link = job.getApplicationLink();
                if (link != null && !link.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                } else {
                    Toast.makeText(getContext(), "अर्जाची लिंक उपलब्ध नाही", Toast.LENGTH_SHORT).show();
                }
            });

            holder.notificationPdfButtonCard.setOnClickListener(v -> {
                String pdfUrl = job.getNotificationPdfLink();
                if (pdfUrl != null && !pdfUrl.isEmpty()) {
                    com.newsproject.oneroadmap.Utils.PdfViewerHelper.openPdfInApp(Top5JobsFragment.this, pdfUrl);
                } else {
                    Toast.makeText(getContext(), "नोटिफिकेशन PDF उपलब्ध नाही", Toast.LENGTH_SHORT).show();
                }
            });
        }



        @Override
        public int getItemCount() {
            return jobs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleView, postTitle, postValue, educationValue, ageValue, jobPlaceValue, feesValue, lastDateValue;
            ImageView iconView;
            TextView selectionPdfButton;
            CardView applicationLinkButtonCard, notificationPdfButtonCard;
            LinearLayout cardMain;

            ViewHolder(View view) {
                super(view);
                cardMain = view.findViewById(R.id.card_main);
                titleView = view.findViewById(R.id.textView5);
                titleView.setSelected(true);
                postTitle = view.findViewById(R.id.post_title);
                postValue = view.findViewById(R.id.post_value);
                postValue.setSelected(true);
                educationValue = view.findViewById(R.id.education_requirement_value);
                educationValue.setSelected(true);
                ageValue = view.findViewById(R.id.age_requirement_value);
                ageValue.setSelected(true);
                jobPlaceValue = view.findViewById(R.id.job_place_value);
                jobPlaceValue.setSelected(true);
                feesValue = view.findViewById(R.id.application_fees_value);
                feesValue.setSelected(true);
                lastDateValue = view.findViewById(R.id.last_date_value);
                lastDateValue.setSelected(true);
                iconView = view.findViewById(R.id.imageView7);
                iconView.setClipToOutline(true);
                selectionPdfButton = view.findViewById(R.id.textView48);
                selectionPdfButton.setSelected(true);
                applicationLinkButtonCard = view.findViewById(R.id.application_link_card);
                notificationPdfButtonCard = view.findViewById(R.id.notification_pdf_card);
            }
        }
    }
}