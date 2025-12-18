package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.newsproject.oneroadmap.Fragments.HomeFragment;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.Models.JobViewModel;
import com.newsproject.oneroadmap.Models.Story;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ShareHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.StoryViewHolder> {

    private final Context context;
    private final List<Story> stories;
    private final StoryAdapter storyAdapter;
    private final RecyclerView storiesPlayer;
    private final SharedPreferences storyPrefs;
    private final Map<String, JobUpdate> jobUpdateCache;

    // Track currently visible story
    private int currentVisiblePosition = 0;

    public StoriesAdapter(Context context, List<Story> stories, StoryAdapter storyAdapter, RecyclerView storiesPlayer) {
        this.context = context;
        this.stories = stories;
        this.storyAdapter = storyAdapter;
        this.storiesPlayer = storiesPlayer;
        this.storyPrefs = context.getSharedPreferences("StoryPrefs", Context.MODE_PRIVATE);
        this.jobUpdateCache = new HashMap<>();

        // Preload job updates for stories with type="post"
        for (Story story : stories) {
            if ("post".equalsIgnoreCase(story.getType())) {
                String id = story.getPostDocumentId();
                if (id == null || id.trim().isEmpty()) {
                    id = story.getDocumentId();
                }
                if (id != null && !id.trim().isEmpty()) {
                    JobViewModel.fetchJobUpdate(id, jobUpdateCache, context, null);
                }
            }
        }
    }

    public int getCurrentVisiblePosition() {
        return currentVisiblePosition;
    }

    public void setCurrentVisiblePosition(int position) {
        this.currentVisiblePosition = position;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = stories.get(position);

        // Bind UI
        if (holder.title != null) {
            holder.title.setText(story.getTitle() != null ? story.getTitle() : "");
        }
        if (holder.timeAgo != null) {
            holder.timeAgo.setText(story.getRelativeTime() != null && !story.getRelativeTime().isEmpty() ? story.getRelativeTime() : "");
        }

        // Load images
        if (holder.bannerImage != null && story.getImageUrl() != null && !story.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(story.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.bannerImage);
        } else if (holder.bannerImage != null) {
            holder.bannerImage.setImageResource(R.drawable.image);
        }

        if (holder.iconImage != null && story.getIconUrl() != null && !story.getIconUrl().isEmpty()) {
            Glide.with(context)
                    .load(story.getIconUrl())
                    .placeholder(R.drawable.hdfc_bank)
                    .error(R.drawable.hdfc_bank)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.iconImage);
        } else if (holder.iconImage != null) {
            holder.iconImage.setImageResource(R.drawable.hdfc_bank);
        }

        // === HIDE OPEN BUTTON IF TYPE IS "news" ===
        String type = story.getType() != null ? story.getType().trim().toLowerCase() : "";
        if ("news".equals(type)) {
            holder.itemView.findViewById(R.id.cardView4).setVisibility(View.GONE);
        } else {
            holder.itemView.findViewById(R.id.cardView4).setVisibility(View.VISIBLE);
        }

        // === LOG BANNER IMAGE SIZE AFTER LAYOUT ===
        holder.bannerImage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove listener to prevent multiple calls
                holder.bannerImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int width = holder.bannerImage.getWidth();
                int height = holder.bannerImage.getHeight();

                Log.d("sizeOfBanner", "Banner Image Size: " + width + " x " + height + " at position: " + position);
            }
        });

        // DO NOT reset progress here
        holder.cancelViewTask();

        // Only schedule task if this is the currently visible story
        if (position == currentVisiblePosition && storiesPlayer.getVisibility() == View.VISIBLE) {
            holder.scheduleViewTask(story, position);
        }
    }

    @Override
    public void onViewRecycled(@NonNull StoryViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cancelViewTask();
    }

    @Override
    public int getItemCount() {
        return stories != null ? stories.size() : 0;
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage, closeButton;
        CircleImageView iconImage;
        TextView title, timeAgo, openPostButton, shareButton;
        ProgressBar progressBar;
        private final Handler handler;
        private Runnable viewRunnable;
        private Runnable progressRunnable;
        private int progress = 0;
        private final StoriesAdapter adapter;

        public StoryViewHolder(@NonNull View itemView, StoriesAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            bannerImage = itemView.findViewById(R.id.banner_image);
            iconImage = itemView.findViewById(R.id.icon);
            closeButton = itemView.findViewById(R.id.close_button);
            title = itemView.findViewById(R.id.title);
            timeAgo = itemView.findViewById(R.id.timeAgo);
            openPostButton = itemView.findViewById(R.id.textView12);
            shareButton = itemView.findViewById(R.id.textView23);
            progressBar = itemView.findViewById(R.id.progressBar3);
            handler = new Handler(Looper.getMainLooper());

            // Open post
            openPostButton.setOnClickListener(v -> {
                Story story = adapter.stories.get(getAdapterPosition());
                if (story == null) return;
                String type = story.getType() != null ? story.getType() : "";

                if ("news".equals(type)) {
                    return;
                }

                if ("post".equalsIgnoreCase(type)) {
                    String postId = story.getPostDocumentId();
                    if (postId == null || postId.trim().isEmpty()) {
                        postId = story.getDocumentId();
                    }
                    if (postId == null || postId.trim().isEmpty()) return;

                    final String finalId = postId.trim();
                    android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(adapter.context);
                    progressDialog.setMessage("Loading...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    JobUpdate job = adapter.jobUpdateCache.get(finalId);
                    if (job != null) {
                        HomeFragment.stopStory(adapter.context);
                        JobViewModel.navigateToJobDetails(job, adapter.context, progressDialog);
                    } else {
                        JobViewModel.fetchJobUpdate(finalId, adapter.jobUpdateCache, adapter.context, () -> {
                            HomeFragment.stopStory(adapter.context);
                            JobViewModel.navigateToJobDetails(adapter.jobUpdateCache.get(finalId), adapter.context, progressDialog);
                        });
                    }
                } else if (story.getWebUrl() != null && !story.getWebUrl().isEmpty()) {
                    HomeFragment.stopStory(adapter.context);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(story.getWebUrl()));
                    adapter.context.startActivity(intent);
                }
            });

            // Share
            shareButton.setOnClickListener(v -> {
                Story story = adapter.stories.get(getAdapterPosition());
                ShareHelper shareHelper = new ShareHelper(adapter.context);
                shareHelper.sharePost("Check out this story: " + story.getTitle(), null);
            });

            // Close
            closeButton.setOnClickListener(v -> {
                cancelViewTask();
                if (adapter.storiesPlayer != null) {
                    adapter.storiesPlayer.setVisibility(View.GONE);
                }
                HomeFragment.stopStory(adapter.context);
            });

            // PAUSE ON HOLD
            itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!adapter.storiesPlayer.isComputingLayout()) {
                            cancelViewTask();
                            Log.d("StoriesAdapter", "PAUSED at progress: " + progress);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && pos == adapter.getCurrentVisiblePosition()
                                && adapter.storiesPlayer.getVisibility() == View.VISIBLE) {
                            Story story = adapter.stories.get(pos);
                            scheduleViewTask(story, pos);
                            Log.d("StoriesAdapter", "RESUMED at progress: " + progress);
                        }
                        return true;
                }
                return false;
            });
        }

        public void scheduleViewTask(Story story, int position) {
            cancelViewTask(); // Prevent duplicates

            viewRunnable = () -> {
                if (adapter.storiesPlayer != null && adapter.storiesPlayer.getVisibility() == View.VISIBLE) {
                    if (!story.isViewed() && story.getDocumentId() != null) {
                        story.setViewed(true);
                        adapter.storyPrefs.edit().putBoolean("viewed_" + story.getDocumentId(), true).apply();
                    }
                    adapter.notifyItemChanged(position);
                    HomeFragment.updateAdapter(adapter.context, position, adapter.storyAdapter);

                    if (position < adapter.stories.size() - 1) {
                        adapter.storiesPlayer.smoothScrollToPosition(position + 1);
                    } else {
                        HomeFragment.stopStory(adapter.context);
                    }
                }
            };

            // Progress bar
            if (adapter.storiesPlayer != null && adapter.storiesPlayer.getVisibility() == View.VISIBLE && progressBar != null) {
                progressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (adapter.storiesPlayer.getVisibility() == View.VISIBLE && progress < 100) {
                            progress++;
                            progressBar.setProgress(progress);
                            handler.postDelayed(this, 50);
                        }
                    }
                };
                handler.post(progressRunnable);
            }

            // Auto-advance
            if (progress < 100) {
                long remainingMs = (100 - progress) * 50L;
                handler.postDelayed(viewRunnable, remainingMs);
            }
        }

        public void cancelViewTask() {
            if (viewRunnable != null) {
                handler.removeCallbacks(viewRunnable);
                viewRunnable = null;
            }
            if (progressRunnable != null) {
                handler.removeCallbacks(progressRunnable);
                progressRunnable = null;
            }
            // DO NOT reset progress
        }
    }
}