package com.newsproject.oneroadmap.Adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;
import com.newsproject.oneroadmap.Fragments.HomeFragment;
import com.newsproject.oneroadmap.Models.JobUpdate;
import com.newsproject.oneroadmap.Models.JobViewModel;
import com.newsproject.oneroadmap.Models.Story;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ShareHelper;
import com.newsproject.oneroadmap.Utils.WebViewHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.StoryViewHolder> {

    private final Context context;
    private final List<Story> stories;
    private final StoryAdapter storyAdapter;
    public static RecyclerView storiesPlayer = null;
    private final SharedPreferences storyPrefs;
    private final Map<String, JobUpdate> jobUpdateCache;
    private static SimpleCache videoCache;

    // Track currently visible story
    private int currentVisiblePosition = 0;

    private static SimpleCache getVideoCache(Context context) {
        if (videoCache == null) {
            try {
                // Use app's cache directory (no permissions needed)
                File cacheDir = new File(context.getCacheDir(), "exoplayer_video_cache");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                long maxCacheSize = 50 * 1024 * 1024; // 50 MB
                videoCache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(maxCacheSize));
            } catch (Exception e) {
                Log.e("StoriesAdapter", "Failed to initialize video cache: " + e.getMessage());
            }
        }
        return videoCache;
    }

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

        // Preload images and videos for faster loading
        preloadMedia();
    }

    private void preloadMedia() {
        // Preload images and videos for all stories
        for (Story story : stories) {
            String mediaType = story.getMediaType() != null ? story.getMediaType().toLowerCase() : "image";
            boolean isVideo = "video".equals(mediaType) && story.getVideoUrl() != null && !story.getVideoUrl().isEmpty();
            
            if (isVideo) {
                // Preload video thumbnail (banner image) for faster display
                String bannerUrl = story.getBannerUrl() != null && !story.getBannerUrl().isEmpty() 
                        ? story.getBannerUrl() : story.getImageUrl();
                if (bannerUrl != null && !bannerUrl.isEmpty()) {
                    Glide.with(context)
                            .load(bannerUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload();
                }
            } else {
                // Preload banner images
                String imageUrl = story.getBannerUrl() != null && !story.getBannerUrl().isEmpty() 
                        ? story.getBannerUrl() : story.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload();
                }
            }
            
            // Preload icon images
            if (story.getIconUrl() != null && !story.getIconUrl().isEmpty()) {
                Glide.with(context)
                        .load(story.getIconUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .preload();
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

        // Reset progress to 0
        holder.progress = 0;
        if (holder.progressBar != null) {
            holder.progressBar.setProgress(0);
        }

        // Show loading indicator
        if (holder.loadingProgress != null) {
            holder.loadingProgress.setVisibility(View.VISIBLE);
        }

        // Bind UI
        if (holder.title != null) {
            holder.title.setText(story.getTitle() != null ? story.getTitle() : "");
        }
        
        // For video stories, we'll update timeAgo when video is loaded
        String mediaType = story.getMediaType() != null ? story.getMediaType().toLowerCase() : "image";
        boolean isVideo = "video".equals(mediaType) && story.getVideoUrl() != null && !story.getVideoUrl().isEmpty();
        
        if (!isVideo && holder.timeAgo != null) {
            holder.timeAgo.setText(story.getRelativeTime() != null && !story.getRelativeTime().isEmpty() ? story.getRelativeTime() : "");
        } else if (isVideo && holder.timeAgo != null) {
            holder.timeAgo.setText(""); // Will be updated when video loads
        }

        // Check media type and load accordingly (already set above)
        
        if (isVideo) {
            // Show banner image as placeholder while video loads
            String bannerUrl = story.getBannerUrl() != null && !story.getBannerUrl().isEmpty() 
                    ? story.getBannerUrl() : story.getImageUrl();
            if (holder.bannerImage != null && bannerUrl != null && !bannerUrl.isEmpty()) {
                holder.bannerImage.setVisibility(View.VISIBLE);
                // Show loading indicator while banner loads
                if (holder.loadingProgress != null) {
                    holder.loadingProgress.setVisibility(View.VISIBLE);
                }
                Glide.with(context)
                        .load(bannerUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                // Keep loading indicator visible for video loading
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                // Keep loading indicator visible for video loading
                                return false;
                            }
                        })
                        .into(holder.bannerImage);
            }
            
            // Show video player and load video (loading indicator will be hidden when video is ready)
            if (holder.videoPlayerView != null) {
                holder.videoPlayerView.setVisibility(View.VISIBLE);
                holder.loadVideo(story.getVideoUrl(), bannerUrl);
            } else {
                if (holder.loadingProgress != null) {
                    holder.loadingProgress.setVisibility(View.GONE);
                }
            }
        } else {
            // Show image, hide video player
            if (holder.videoPlayerView != null) {
                holder.videoPlayerView.setVisibility(View.GONE);
                holder.releaseVideo();
            }
            if (holder.bannerImage != null) {
                holder.bannerImage.setVisibility(View.VISIBLE);
                String imageUrl = story.getBannerUrl() != null && !story.getBannerUrl().isEmpty() 
                        ? story.getBannerUrl() : story.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .thumbnail(0.1f) // Show thumbnail immediately while full image loads
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    if (holder.loadingProgress != null) {
                                        holder.loadingProgress.setVisibility(View.GONE);
                                    }
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    if (holder.loadingProgress != null) {
                                        holder.loadingProgress.setVisibility(View.GONE);
                                    }
                                    return false;
                                }
                            })
                            .into(holder.bannerImage);
                } else {
                    holder.bannerImage.setImageResource(R.drawable.image);
                    if (holder.loadingProgress != null) {
                        holder.loadingProgress.setVisibility(View.GONE);
                    }
                }
            } else {
                if (holder.loadingProgress != null) {
                    holder.loadingProgress.setVisibility(View.GONE);
                }
            }
        }

        if (holder.iconImage != null && story.getIconUrl() != null && !story.getIconUrl().isEmpty()) {
            Glide.with(context)
                    .load(story.getIconUrl())
                    .placeholder(R.drawable.hdfc_bank)
                    .error(R.drawable.hdfc_bank)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .thumbnail(0.1f) // Show thumbnail immediately
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

        // Preload adjacent stories for faster loading
        preloadAdjacentStories(position);
        
        // Only schedule task if this is the currently visible story
        if (position == currentVisiblePosition && storiesPlayer.getVisibility() == View.VISIBLE) {
            holder.scheduleViewTask(story, position);
            // Start video if it's a video story
            if (isVideo) {
                holder.resumeVideo();
            }
        } else {
            // Pause video if not visible
            holder.pauseVideo();
        }
    }

    private void preloadAdjacentStories(int currentPosition) {
        // Preload next story
        if (currentPosition + 1 < stories.size()) {
            Story nextStory = stories.get(currentPosition + 1);
            preloadStoryMedia(nextStory);
        }
        
        // Preload previous story
        if (currentPosition - 1 >= 0) {
            Story prevStory = stories.get(currentPosition - 1);
            preloadStoryMedia(prevStory);
        }
    }

    private void preloadStoryMedia(Story story) {
        String mediaType = story.getMediaType() != null ? story.getMediaType().toLowerCase() : "image";
        boolean isVideo = "video".equals(mediaType) && story.getVideoUrl() != null && !story.getVideoUrl().isEmpty();
        
        if (isVideo) {
            // Preload video thumbnail
            String bannerUrl = story.getBannerUrl() != null && !story.getBannerUrl().isEmpty() 
                    ? story.getBannerUrl() : story.getImageUrl();
            if (bannerUrl != null && !bannerUrl.isEmpty()) {
                Glide.with(context)
                        .load(bannerUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .preload();
            }
        } else {
            // Preload banner image
            String imageUrl = story.getBannerUrl() != null && !story.getBannerUrl().isEmpty() 
                    ? story.getBannerUrl() : story.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .preload();
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull StoryViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cancelViewTask();
        holder.releaseVideo();
    }

    @Override
    public int getItemCount() {
        return stories != null ? stories.size() : 0;
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage, closeButton;
        CircleImageView iconImage;
        TextView title, timeAgo, openPostButton, shareButton;
        ProgressBar progressBar, loadingProgress;
        static PlayerView videoPlayerView;
        private static ExoPlayer exoPlayer;
        private static Player.Listener videoListener;
        private static Handler handler = null;
        private static Runnable viewRunnable;
        private static Runnable progressRunnable;
        private int progress = 0;
        private final StoriesAdapter adapter;

        public StoryViewHolder(@NonNull View itemView, StoriesAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            bannerImage = itemView.findViewById(R.id.banner_image);
            videoPlayerView = itemView.findViewById(R.id.video_player);
            iconImage = itemView.findViewById(R.id.icon);
            closeButton = itemView.findViewById(R.id.close_button);
            title = itemView.findViewById(R.id.title);
            timeAgo = itemView.findViewById(R.id.timeAgo);
            openPostButton = itemView.findViewById(R.id.textView12);
            shareButton = itemView.findViewById(R.id.textView23);
            progressBar = itemView.findViewById(R.id.progressBar3);
            loadingProgress = itemView.findViewById(R.id.loading_progress);
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
                    ProgressDialog progressDialog = new ProgressDialog(adapter.context);
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
                    WebViewHelper.openUrlInApp(adapter.context, story.getWebUrl());
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
                releaseVideo();
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
                            pauseVideo();
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
                            resumeVideo();
                            Log.d("StoriesAdapter", "RESUMED at progress: " + progress);
                        }
                        return true;
                }
                return false;
            });
        }

        private void loadVideo(String videoUrl, String bannerUrl) {
            if (videoUrl == null || videoUrl.isEmpty()) {
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(View.GONE);
                }
                return;
            }

            releaseVideo(); // Release any existing player

            try {
                // Create ExoPlayer with optimized settings for faster startup
                exoPlayer = new ExoPlayer.Builder(adapter.context)
                        .setSeekBackIncrementMs(10000)
                        .setSeekForwardIncrementMs(10000)
                        .build();
                
                // Optimize for faster loading
                exoPlayer.setPlayWhenReady(false); // Don't auto-play, wait for user
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF); // Don't loop - advance to next story when finished
                
                videoPlayerView.setPlayer(exoPlayer);
                
                // Create MediaItem with optimized settings
                MediaItem mediaItem = new MediaItem.Builder()
                        .setUri(videoUrl)
                        .build();
                
                exoPlayer.setMediaItem(mediaItem);
                
                // Prepare video (this will start buffering)
                exoPlayer.prepare();
                
                // Initial listener for video ready state
                exoPlayer.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == Player.STATE_READY) {
                            // Video is ready, hide banner image and loading indicator
                            if (bannerImage != null && bannerUrl != null && !bannerUrl.isEmpty()) {
                                bannerImage.setVisibility(View.GONE);
                            }
                            if (loadingProgress != null) {
                                loadingProgress.setVisibility(View.GONE);
                            }
                            
                            // Update timeAgo with video duration
                            if (timeAgo != null && exoPlayer.getDuration() > 0) {
                                long durationSeconds = exoPlayer.getDuration() / 1000;
                                String durationText = formatVideoDuration(durationSeconds);
                                timeAgo.setText(durationText);
                            }
                            
                            // Start playing if this is the visible story
                            int pos = getAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION && pos == adapter.getCurrentVisiblePosition()
                                    && adapter.storiesPlayer.getVisibility() == View.VISIBLE) {
                                exoPlayer.setPlayWhenReady(true);
                                exoPlayer.play();
                            }
                        } else if (playbackState == Player.STATE_BUFFERING) {
                            // Show loading indicator while buffering
                            if (loadingProgress != null) {
                                loadingProgress.setVisibility(View.VISIBLE);
                            }
                        } else if (playbackState == Player.STATE_ENDED) {
                            // Video finished - this will be handled by scheduleViewTask listener
                            // But we can hide loading here
                            if (loadingProgress != null) {
                                loadingProgress.setVisibility(View.GONE);
                            }
                        }
                    }
                });

                // Video listener will be added in scheduleViewTask
            } catch (Exception e) {
                Log.e("StoriesAdapter", "Error loading video: " + e.getMessage());
                // Show banner image if video fails to load
                if (bannerImage != null && bannerUrl != null && !bannerUrl.isEmpty()) {
                    bannerImage.setVisibility(View.VISIBLE);
                }
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(View.GONE);
                }
            }
        }

        private String formatVideoDuration(long seconds) {
            if (seconds < 60) {
                return seconds + "s";
            } else if (seconds < 3600) {
                long minutes = seconds / 60;
                long secs = seconds % 60;
                return minutes + "m " + secs + "s";
            } else {
                long hours = seconds / 3600;
                long minutes = (seconds % 3600) / 60;
                long secs = seconds % 60;
                return hours + "h " + minutes + "m " + secs + "s";
            }
        }

        private static void pauseVideo() {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.pause();
            }
        }

        private void resumeVideo() {
            if (exoPlayer != null) {
                if (exoPlayer.getPlaybackState() == Player.STATE_READY) {
                    // Video is ready, hide banner and play
                    if (bannerImage != null) {
                        bannerImage.setVisibility(View.GONE);
                    }
                    exoPlayer.setPlayWhenReady(true);
                    exoPlayer.play();
                } else if (exoPlayer.getPlaybackState() == Player.STATE_BUFFERING) {
                    // Still buffering, keep banner visible
                    exoPlayer.setPlayWhenReady(true);
                }
            }
        }

        public static void releaseVideo() {
            if (exoPlayer != null) {
                if (videoListener != null) {
                    exoPlayer.removeListener(videoListener);
                    videoListener = null;
                }
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
            }
            if (videoPlayerView != null) {
                videoPlayerView.setPlayer(null);
            }
        }

        public void scheduleViewTask(Story story, int position) {
            cancelViewTask(); // Prevent duplicates

            viewRunnable = () -> {
                if (adapter.storiesPlayer != null && adapter.storiesPlayer.getVisibility() == View.VISIBLE) {
                    // Mark story as viewed
                    if (!story.isViewed() && story.getDocumentId() != null) {
                        story.setViewed(true);
                        adapter.storyPrefs.edit().putBoolean("viewed_" + story.getDocumentId(), true).apply();
                    }
                    // Notify adapter to update UI (grey border)
                    adapter.notifyItemChanged(position);
                    HomeFragment.updateAdapter(adapter.context, position, adapter.storyAdapter);

                    // Advance to next story
                    if (position < adapter.stories.size() - 1) {
                        adapter.storiesPlayer.smoothScrollToPosition(position + 1);
                    } else {
                        HomeFragment.stopStory(adapter.context);
                    }
                }
            };

            // Check if it's a video story
            String mediaType = story.getMediaType() != null ? story.getMediaType().toLowerCase() : "image";
            boolean isVideo = "video".equals(mediaType) && story.getVideoUrl() != null && !story.getVideoUrl().isEmpty();

            if (isVideo && exoPlayer != null) {
                // Remove old listener if exists
                if (videoListener != null) {
                    exoPlayer.removeListener(videoListener);
                }
                
                // For videos, use video duration and update progress based on playback position
                videoListener = new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == Player.STATE_ENDED) {
                            // Video finished, advance to next story
                            handler.post(viewRunnable);
                        } else if (playbackState == Player.STATE_READY && progressBar != null) {
                            // Start updating progress when video is ready
                            updateVideoProgress();
                        }
                    }

                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        if (isPlaying && progressBar != null) {
                            // Update progress bar based on video position
                            updateVideoProgress();
                        }
                    }
                };
                exoPlayer.addListener(videoListener);
            } else {
                // For images, use the standard progress bar timer
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

                // Auto-advance for images
                if (progress < 100) {
                    long remainingMs = (100 - progress) * 50L;
                    handler.postDelayed(viewRunnable, remainingMs);
                }
            }
        }

        private void updateVideoProgress() {
            if (exoPlayer != null && progressBar != null && exoPlayer.getDuration() > 0) {
                long currentPosition = exoPlayer.getCurrentPosition();
                long duration = exoPlayer.getDuration();
                int newProgress = (int) ((currentPosition * 100) / duration);
                if (newProgress != progress) {
                    progress = newProgress;
                    progressBar.setProgress(progress);
                }
                if (exoPlayer.isPlaying() && progress < 100) {
                    handler.postDelayed(this::updateVideoProgress, 100);
                }
            }
        }

        public static void cancelViewTask() {
            if (viewRunnable != null) {
                handler.removeCallbacks(viewRunnable);
                viewRunnable = null;
            }
            if (progressRunnable != null) {
                handler.removeCallbacks(progressRunnable);
                progressRunnable = null;
            }
            pauseVideo();
            // DO NOT reset progress
        }
    }
}