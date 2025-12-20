package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;

import java.util.Map;

public class CardPagerAdapter extends RecyclerView.Adapter<CardPagerAdapter.ViewHolder> {
    private static final String TAG = "CardPagerAdapter";
    private final LayoutInflater inflater;
    private final int[] layouts = {
            R.layout.card_view_19,
            R.layout.card_view_10,
            R.layout.card_view_20
    };
    private JobUpdate jobUpdate;

    public CardPagerAdapter(Context context, JobUpdate jobUpdate) {
        this.inflater = LayoutInflater.from(context);
        this.jobUpdate = jobUpdate;
        Log.d(TAG, "CardPagerAdapter initialized, jobUpdate: " + (jobUpdate != null));
    }

    public void setJobUpdate(JobUpdate jobUpdate) {
        this.jobUpdate = jobUpdate;
        Log.d(TAG, "setJobUpdate called, jobUpdate: " + (jobUpdate != null));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Inflating layout for position " + viewType + ": " + layouts[viewType]);
        View view = inflater.inflate(layouts[viewType], parent, false);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        view.setLayoutParams(params);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "Binding view for position " + position + ", jobUpdate: " + (jobUpdate != null));
        if (jobUpdate != null && position < 2) {
            if (position == 0) {
                TextView postNameValue = holder.itemView.findViewById(R.id.post_value);
                TextView educationRequirementValue = holder.itemView.findViewById(R.id.education_requirement_value);
                TextView ageRequirementValue = holder.itemView.findViewById(R.id.age_requirement_value);
                TextView jobPlaceValue = holder.itemView.findViewById(R.id.job_place_value);
                TextView applicationFeesValue = holder.itemView.findViewById(R.id.application_fees_value);
                TextView lastDateValue = holder.itemView.findViewById(R.id.last_date_value);

                postNameValue.setText(jobUpdate.getPostName());

                // Handle education requirement display logic
                Object education = jobUpdate.getEducationRequirement();
                String educationText;
                if (education instanceof String) {
                    String category = (String) education;
                    if (category.equalsIgnoreCase("all")) {
                        educationText = "All";
                    } else if (category.equalsIgnoreCase("10th_12th")) {
                        educationText = "10th and 12th";
                    } else {
                        // For other categories, display category followed by specific degrees
                        StringBuilder educationBuilder = new StringBuilder();
                        if (category.equalsIgnoreCase("engineering")) {
                            educationBuilder.append("Engineering\n");
                            educationBuilder.append("Bachelors: B.Tech, B.E\n");
                            educationBuilder.append("Masters: M.Tech, M.E");
                        } else if (category.equalsIgnoreCase("computer science/it")) {
                            educationBuilder.append("Computer Science/IT\n");
                            educationBuilder.append("Bachelors: B.Tech, B.E\n");
                            educationBuilder.append("Masters: M.Tech, M.E");
                        } else {
                            educationBuilder.append(category); // Fallback for unknown categories
                        }
                        educationText = educationBuilder.toString();
                    }
                } else if (education instanceof Map) {
                    Map<?, ?> educationMap = (Map<?, ?>) education;
                    StringBuilder educationBuilder = new StringBuilder();
                    // Handle categories
                    Object categoriesObj = educationMap.get("categories");
                    if (categoriesObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> categories = (java.util.List<String>) categoriesObj;
                        if (!categories.isEmpty()) {
                            educationBuilder.append(String.join(", ", categories));
                            educationBuilder.append("\n");
                        }
                    }
                    // Handle bachelors
                    Object bachelorsObj = educationMap.get("bachelors");
                    if (bachelorsObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> bachelors = (java.util.List<String>) bachelorsObj;
                        if (!bachelors.isEmpty()) {
                            educationBuilder.append("Bachelors: ").append(String.join(", ", bachelors)).append("\n");
                        }
                    }
                    // Handle masters
                    Object mastersObj = educationMap.get("masters");
                    if (mastersObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> masters = (java.util.List<String>) mastersObj;
                        if (!masters.isEmpty()) {
                            educationBuilder.append("Masters: ").append(String.join(", ", masters));
                        }
                    }
                    educationText = educationBuilder.toString().trim();
                    if (educationText.isEmpty()) educationText = "N/A";
                } else {
                    educationText = "N/A";
                }
                educationRequirementValue.setText(educationText);

                ageRequirementValue.setText(jobUpdate.getAgeRequirement());
                jobPlaceValue.setText(jobUpdate.getJobPlace());
                applicationFeesValue.setText(jobUpdate.getFormattedApplicationFees());
                lastDateValue.setText(jobUpdate.getFormattedLastDate());
            } else if (position == 1) {
                TextView textView45 = holder.itemView.findViewById(R.id.textView45);
                TextView textView34 = holder.itemView.findViewById(R.id.textView34);
                TextView textView48 = holder.itemView.findViewById(R.id.textView48);
                TextView textView44 = holder.itemView.findViewById(R.id.textView44);
                TextView textView3 = holder.itemView.findViewById(R.id.textView3);

                textView45.setText(jobUpdate.getApplicationLink() != null && !jobUpdate.getApplicationLink().isEmpty() ? "अर्जाची लिंक" : "अर्जाची लिंक अनुपलब्ध");
                textView34.setText(jobUpdate.getNotificationPdfLink() != null && !jobUpdate.getNotificationPdfLink().isEmpty() ? "नोटिफिकेशन PDF" : "नोटिफिकेशन PDF अनुपलब्ध");
                textView48.setText(jobUpdate.getSelectionPdfLink() != null && !jobUpdate.getSelectionPdfLink().isEmpty() ? "सिलेक्शन PDF" : "सिलेक्शन PDF अनुपलब्ध");
                textView44.setText(jobUpdate.getSyllabusPdf() != null && !jobUpdate.getSyllabusPdf().isEmpty() ? "अभ्यासक्रम PDF" : "अभ्यासक्रम PDF अनुपलब्ध");
                textView3.setText(jobUpdate.getNote() != null && !jobUpdate.getNote().isEmpty() ? jobUpdate.getNote() : "No note available");

                setLinkClickListener(holder.itemView, R.id.textView45, jobUpdate.getApplicationLink());
                setLinkClickListener(holder.itemView, R.id.textView34, jobUpdate.getNotificationPdfLink());
                setLinkClickListener(holder.itemView, R.id.textView48, jobUpdate.getSelectionPdfLink());
                setLinkClickListener(holder.itemView, R.id.textView44, jobUpdate.getSyllabusPdf());
            }
        }
        holder.itemView.requestLayout();
    }

    private void setLinkClickListener(View view, int textViewId, String url) {
        TextView textView = view.findViewById(textViewId);
        if (url != null && !url.isEmpty()) {
            textView.setOnClickListener(v -> {
                // Check if it's a PDF URL and open in app viewer, otherwise open externally
                if (com.newsproject.oneroadmap.Utils.PdfViewerHelper.isPdfUrl(url)) {
                    com.newsproject.oneroadmap.Utils.PdfViewerHelper.openPdfInApp(view.getContext(), url);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                }
            });
        } else {
            textView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "Item count: " + layouts.length);
        return layouts.length;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}