package com.newsproject.oneroadmap.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.Timestamp;
import com.newsproject.oneroadmap.Fragments.JobUpdateDetails;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.R;

import java.util.List;

public class JobUpdateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "JobUpdateAdapter";
    private static final int VIEW_TYPE_JOB = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private final FragmentManager fragmentManager;
    private final AsyncListDiffer<JobUpdate> differ;
    private boolean isLoading = false;

    public JobUpdateAdapter(List<JobUpdate> initialList, FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
        this.differ = new AsyncListDiffer<>(this, new JobDiffCallback());
        this.differ.submitList(initialList);
    }

    public void updateJobs(List<JobUpdate> newJobs) {
        differ.submitList(newJobs);
    }

    public void setLoading(boolean loading) {
        if (isLoading != loading) {
            isLoading = loading;
            notifyItemChanged(differ.getCurrentList().size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (position == differ.getCurrentList().size() && isLoading) ? VIEW_TYPE_LOADING : VIEW_TYPE_JOB;
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size() + (isLoading ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_JOB) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.job_update_item, parent, false);
            return new JobUpdateViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof JobUpdateViewHolder) {
            JobUpdate item = differ.getCurrentList().get(position);
            ((JobUpdateViewHolder) holder).bind(item);
        }
        // No binding needed for LoadingViewHolder
    }

    class JobUpdateViewHolder extends RecyclerView.ViewHolder {
        TextView title, lastDate, timeAgo;
        ImageView icon;

        public JobUpdateViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            lastDate = itemView.findViewById(R.id.txt_lastDate);
            timeAgo = itemView.findViewById(R.id.txt_time_ago);
            icon = itemView.findViewById(R.id.imageView18);

            itemView.setOnClickListener(v -> {
                JobUpdate item = differ.getCurrentList().get(getAdapterPosition());
                Log.d(TAG, "Clicked on item with ID: " + item.getDocumentId());
                JobUpdateDetails fragment = JobUpdateDetails.newInstance(item);
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            });
        }

        public void bind(JobUpdate item) {
            title.setText(item.getTitle());
            lastDate.setText(item.getFormattedLastDateMarathi());
            timeAgo.setText(item.getTimeAgo());

            // === DEBUG: LOG LAST DATES ===
            String rawString = item.getLastDateString();
            Timestamp rawTs = item.getLastDateTimestamp();
            String english = item.getFormattedLastDate();
            String marathi = item.getFormattedLastDateMarathi();

            Log.d("LAST_DATES_DEBUG", "=== JOB: " + item.getTitle() + " ===");
            Log.d("LAST_DATES_DEBUG", "Raw String: " + rawString);
            Log.d("LAST_DATES_DEBUG", "Raw Timestamp: " + (rawTs != null ? rawTs.toDate() + " (UTC)" : "null"));
            Log.d("LAST_DATES_DEBUG", "English: " + english);
            Log.d("LAST_DATES_DEBUG", "Marathi: " + marathi);
            Log.d("LAST_DATES_DEBUG", "-------------------------------------");

            // Load icon using Glide
            String iconUrl = item.getIconUrl();
            Log.d(TAG, "Loading icon for job " + item.getTitle() + ": " + iconUrl);
            if (iconUrl != null && !iconUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(iconUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.maharashtra_poilice3x)
                                .error(R.drawable.maharashtra_poilice3x)
                                .diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(icon);
            } else {
                Log.w(TAG, "Icon URL is null or empty for job: " + item.getTitle());
                icon.setImageResource(R.drawable.maharashtra_poilice3x);
            }
        }
    }

    class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class JobDiffCallback extends DiffUtil.ItemCallback<JobUpdate> {
        @Override
        public boolean areItemsTheSame(@NonNull JobUpdate oldItem, @NonNull JobUpdate newItem) {
            return oldItem.getDocumentId().equals(newItem.getDocumentId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull JobUpdate oldItem, @NonNull JobUpdate newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getFormattedSalary().equals(newItem.getFormattedSalary()) &&
                    oldItem.getFormattedLastDateMarathi().equals(newItem.getFormattedLastDateMarathi()) &&  // BOTH MARATHI
                    oldItem.getTimeAgo().equals(newItem.getTimeAgo()) &&
                    (oldItem.getIconUrl() != null ? oldItem.getIconUrl().equals(newItem.getIconUrl()) : newItem.getIconUrl() == null);
        }
    }
}