package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.CoinAccessController;

import java.util.List;
import java.util.Map;

public class CardPagerAdapter extends RecyclerView.Adapter<CardPagerAdapter.ViewHolder> {

    private static final String TAG = "CardPagerAdapter";

    private final LayoutInflater inflater;
    private final CoinAccessController coinAccessController;

    private final int[] layouts = {
            R.layout.card_view_19,
            R.layout.card_view_10,
            R.layout.card_view_20
    };

    private JobUpdate jobUpdate;

    // ✅ UPDATED CONSTRUCTOR (Now requires CoinAccessController)
    public CardPagerAdapter(Context context,
                            JobUpdate jobUpdate,
                            CoinAccessController coinAccessController) {

        this.inflater = LayoutInflater.from(context);
        this.jobUpdate = jobUpdate;
        this.coinAccessController = coinAccessController;

        Log.d(TAG, "Adapter initialized. jobUpdate: " + (jobUpdate != null));
    }

    public void setJobUpdate(JobUpdate jobUpdate) {
        this.jobUpdate = jobUpdate;
        notifyDataSetChanged();
        Log.d(TAG, "setJobUpdate called.");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

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

        if (jobUpdate == null) return;

        if (position == 0) {
            bindDetailsCard(holder);
        } else if (position == 1) {
            bindLinksCard(holder);
        }
    }

    // =========================
    // FIRST CARD (DETAILS)
    // =========================
    private void bindDetailsCard(ViewHolder holder) {

        TextView postNameValue = holder.itemView.findViewById(R.id.post_value);
        TextView educationRequirementValue = holder.itemView.findViewById(R.id.education_requirement_value);
        TextView ageRequirementValue = holder.itemView.findViewById(R.id.age_requirement_value);
        TextView jobPlaceValue = holder.itemView.findViewById(R.id.job_place_value);
        TextView applicationFeesValue = holder.itemView.findViewById(R.id.application_fees_value);
        TextView lastDateValue = holder.itemView.findViewById(R.id.last_date_value);

        postNameValue.setText(jobUpdate.getPostName());
        educationRequirementValue.setText(formatEducation(jobUpdate.getEducationRequirement()));
        ageRequirementValue.setText(jobUpdate.getAgeRequirement());
        jobPlaceValue.setText(jobUpdate.getJobPlace());
        applicationFeesValue.setText(jobUpdate.getFormattedApplicationFees());
        lastDateValue.setText(jobUpdate.getFormattedLastDate());
    }

    // =========================
    // SECOND CARD (LINKS)
    // =========================
    private void bindLinksCard(ViewHolder holder) {

        TextView link = holder.itemView.findViewById(R.id.textView45);
        TextView notification = holder.itemView.findViewById(R.id.textView34);
        TextView selection = holder.itemView.findViewById(R.id.textView48);
        TextView syllabus = holder.itemView.findViewById(R.id.textView44);
        TextView note = holder.itemView.findViewById(R.id.textView3);

        link.setText(isAvailable(jobUpdate.getApplicationLink(), "अर्जाची लिंक"));
        notification.setText(isAvailable(jobUpdate.getNotificationPdfLink(), "नोटिफिकेशन PDF"));
        selection.setText(isAvailable(jobUpdate.getSelectionPdfLink(), "सिलेक्शन PDF"));
        syllabus.setText(isAvailable(jobUpdate.getSyllabusPdf(), "अभ्यासक्रम PDF"));
        note.setText(jobUpdate.getNote() != null && !jobUpdate.getNote().isEmpty()
                ? jobUpdate.getNote()
                : "No note available");

        setLinkClickListener(link, jobUpdate.getApplicationLink());
        setLinkClickListener(notification, jobUpdate.getNotificationPdfLink());
        setLinkClickListener(selection, jobUpdate.getSelectionPdfLink());
        setLinkClickListener(syllabus, jobUpdate.getSyllabusPdf());
    }

    // =========================
    // HELPER METHODS
    // =========================

    private void setLinkClickListener(TextView textView, String url) {

        if (url != null && !url.isEmpty()) {

            textView.setOnClickListener(v -> {
                if (coinAccessController != null) {
                    // ✅ NEW METHOD SIGNATURE
                    coinAccessController.requestPdfAccess(url, null);
                }
            });

        } else {
            textView.setOnClickListener(null);
        }
    }

    private String isAvailable(String url, String label) {
        return (url != null && !url.isEmpty())
                ? label
                : label + " अनुपलब्ध";
    }

    private String formatEducation(Object education) {

        if (education instanceof String) {

            String category = (String) education;

            if (category.equalsIgnoreCase("all")) return "All";
            if (category.equalsIgnoreCase("10th_12th")) return "10th and 12th";

            return category;

        } else if (education instanceof Map) {

            Map<?, ?> map = (Map<?, ?>) education;
            StringBuilder builder = new StringBuilder();

            appendList(builder, map.get("categories"));
            appendList(builder, map.get("bachelors"));
            appendList(builder, map.get("masters"));

            return builder.length() == 0 ? "N/A" : builder.toString().trim();
        }

        return "N/A";
    }

    @SuppressWarnings("unchecked")
    private void appendList(StringBuilder builder, Object obj) {

        if (obj instanceof List) {

            List<String> list = (List<String>) obj;

            if (!list.isEmpty()) {
                builder.append(String.join(", ", list)).append("\n");
            }
        }
    }

    @Override
    public int getItemCount() {
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