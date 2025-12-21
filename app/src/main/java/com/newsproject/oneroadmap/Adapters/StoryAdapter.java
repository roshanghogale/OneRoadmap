package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.circularstatusview.CircularStatusView;
import com.newsproject.oneroadmap.Fragments.HomeFragment;
import com.newsproject.oneroadmap.Models.Story;
import com.newsproject.oneroadmap.R;

import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final List<Story> storyList;
    private final Context context;

    public StoryAdapter(Context context, List<Story> storyList) {
        this.context = context;
        this.storyList = storyList;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.story_item, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = storyList.get(position);
        holder.titleTextView.setText(story.getTitle());

        // Prefer server-provided relative time; fallback to uploadTime if present
        if (story.getRelativeTime() != null && !story.getRelativeTime().isEmpty()) {
            holder.uploadTimeTextView.setText(story.getRelativeTime());
        } else if (story.getUploadTime() != null) {
            long timeInMillis = story.getUploadTime().getSeconds() * 1000;  // Convert to milliseconds
            String relativeTime = DateUtils.getRelativeTimeSpanString(
                    timeInMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            ).toString();
            holder.uploadTimeTextView.setText(relativeTime);
        } else {
            holder.uploadTimeTextView.setText("");
        }

        // Check if the story has been viewed or not and set the status color accordingly

        if (!story.isViewed()) {
            if (story.isMainStory()){
                holder.statusView.setPortionsColor(
                        ContextCompat.getColor(context, R.color.green) // Use ContextCompat to get the color value
                );
            }else{
                holder.statusView.setPortionsColor(
                        ContextCompat.getColor(context, R.color.purple) // Use ContextCompat to get the color value
                );
            }

        } else {
            holder.statusView.setPortionsColor(
                    ContextCompat.getColor(context, R.color.gray) // Use ContextCompat to get the color value
            );
        }

        // Load the image into the ImageView using Glide
        Glide.with(holder.imageView.getContext())
                .load(story.getIconUrl())
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            // Call the method to play the story
            HomeFragment.playStory(context, position, storyList, this);
        });
    }

    @Override
    public int getItemCount() {
        return storyList.size();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView uploadTimeTextView;
        ImageView imageView;
        CircularStatusView statusView;

        public StoryViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.story_title);
            titleTextView.setSelected(true);
            uploadTimeTextView = itemView.findViewById(R.id.story_upload_time);
            imageView = itemView.findViewById(R.id.story_image_view);
            statusView = itemView.findViewById(R.id.story_status_view);
        }
    }
}

