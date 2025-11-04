package com.newsproject.oneroadmap.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;
import java.util.List;

public class RecentlyOpenedAdapter extends RecyclerView.Adapter<RecentlyOpenedAdapter.ViewHolder> {

    private final List<JobUpdate> jobs;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(JobUpdate job);
    }

    public RecentlyOpenedAdapter(List<JobUpdate> jobs, OnItemClickListener listener) {
        this.jobs = jobs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recently_opened, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JobUpdate job = jobs.get(position);
        holder.bind(job);
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView logo, shareBtn;
        TextView title, lastDate, shareCount, applyBtn;

        ViewHolder(View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.job_update_logo);
            title = itemView.findViewById(R.id.job_update_title);
            lastDate = itemView.findViewById(R.id.job_update_last_date);
            shareCount = itemView.findViewById(R.id.job_update_share_count);
            shareBtn = itemView.findViewById(R.id.job_update_share_button);
            applyBtn = itemView.findViewById(R.id.job_update_button);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(jobs.get(pos));
                }
            });
        }

        void bind(JobUpdate job) {
            title.setText(job.getTitle());
            lastDate.setText(job.getFormattedLastDateMarathi());
            shareCount.setText("Share Family");

            Glide.with(itemView.getContext())
                    .load(job.getIconUrl())
                    .placeholder(R.drawable.hdfc_bank3x)
                    .error(R.drawable.hdfc_bank3x)
                    .into(logo);

            applyBtn.setOnClickListener(v -> {
                // Optional: open application link
            });
        }
    }

    public void updateList(List<JobUpdate> newList) {
        this.jobs.clear();
        this.jobs.addAll(newList);
        notifyDataSetChanged();
    }
}