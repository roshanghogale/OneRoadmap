package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.newsproject.oneroadmap.Fragments.HomeFragment;
import com.newsproject.oneroadmap.Models.Story;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ShareHelper;
import com.newsproject.oneroadmap.Utils.WebViewHelper;

import java.util.List;

public class StoriesAdapter
        extends RecyclerView.Adapter<StoriesAdapter.StoryViewHolder> {

    private final Context context;
    private final List<Story> stories;
    final StoryAdapter storyAdapter;
    private final RecyclerView storiesPlayer;

    private int currentVisiblePosition = 0;

    public StoriesAdapter(
            Context context,
            List<Story> stories,
            StoryAdapter storyAdapter,
            RecyclerView storiesPlayer
    ) {
        this.context = context;
        this.stories = stories;
        this.storyAdapter = storyAdapter;
        this.storiesPlayer = storiesPlayer;
    }

    public int getCurrentVisiblePosition() {
        return currentVisiblePosition;
    }

    public void setCurrentVisiblePosition(int pos) {
        currentVisiblePosition = pos;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(v, this);
    }

    @Override
    public void onBindViewHolder(
            @NonNull StoryViewHolder holder,
            int position
    ) {
        holder.bind(stories.get(position));
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    // =====================================================
    // VIEW HOLDER
    // =====================================================

    public static class StoryViewHolder extends RecyclerView.ViewHolder {

        private static final long IMAGE_DURATION_MS = 5000;

        private final ImageView banner;
        private final ImageView icon;
        private final ImageView closeButton;
        private final TextView openButton;
        private final TextView shareButton;
        private final PlayerView playerView;
        private final ProgressBar progressBar;
        private final ProgressBar loadingProgress;

        private final Handler handler = new Handler(Looper.getMainLooper());

        private ExoPlayer exoPlayer;
        private Runnable progressRunnable;

        private final StoriesAdapter adapter;
        private Story story;

        private long playSessionId = 0;

        public StoryViewHolder(
                @NonNull View itemView,
                StoriesAdapter adapter
        ) {
            super(itemView);
            this.adapter = adapter;

            banner = itemView.findViewById(R.id.banner_image);
            icon = itemView.findViewById(R.id.icon);
            closeButton = itemView.findViewById(R.id.close_button);
            openButton = itemView.findViewById(R.id.textView12);
            shareButton = itemView.findViewById(R.id.textView23);
            playerView = itemView.findViewById(R.id.video_player);
            progressBar = itemView.findViewById(R.id.progressBar3);
            loadingProgress = itemView.findViewById(R.id.loading_progress);

            // -------- Buttons --------
            closeButton.setOnClickListener(v ->
                    HomeFragment.stopStory(adapter.context)
            );

            openButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                Story s = adapter.stories.get(pos);
                if (s.getWebUrl() != null) {
                    HomeFragment.stopStory(adapter.context);
                    WebViewHelper.openUrlInApp(adapter.context, s.getWebUrl());
                }
            });

            shareButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                Story s = adapter.stories.get(pos);
                new ShareHelper(adapter.context)
                        .sharePost("Check this out: " + s.getTitle(), s.getWebUrl());
            });

            // -------- Pause on hold --------
            itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    pauseVideo();
                    pauseProgress();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    resumeVideo();
                    resumeProgress();
                    return true;
                }
                return false;
            });
        }

        void bind(Story s) {
            story = s;

            Glide.with(itemView)
                    .load(s.getIconUrl())
                    .placeholder(R.drawable.app_logo)
                    .into(icon);

            banner.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
            loadingProgress.setVisibility(View.GONE);
            progressBar.setProgress(0);
        }

        public void onBecameVisible() {
            reset();
            loadMedia();
        }

        public void onBecameInvisible() {
            reset();
        }

        // ================= MEDIA =================

        private void loadMedia() {
            loadingProgress.setVisibility(View.VISIBLE);

            boolean isVideo =
                    "video".equalsIgnoreCase(story.getMediaType())
                            && story.getVideoUrl() != null;

            if (isVideo) {
                loadVideo();
            } else {
                loadImage();
            }
        }

        // ================= IMAGE =================

        private void loadImage() {
            final long session = playSessionId;

            Glide.with(itemView)
                    .load(story.getBannerUrl())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onResourceReady(
                                Drawable resource,
                                Object model,
                                Target<Drawable> target,
                                DataSource dataSource,
                                boolean isFirstResource
                        ) {
                            if (session != playSessionId) return true;

                            loadingProgress.setVisibility(View.GONE);
                            startImageProgress(session);
                            return false;
                        }

                        @Override
                        public boolean onLoadFailed(
                                @Nullable GlideException e,
                                Object model,
                                Target<Drawable> target,
                                boolean isFirstResource
                        ) {
                            loadingProgress.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(banner);
        }

        private void startImageProgress(long session) {
            long start = System.currentTimeMillis();

            progressRunnable = () -> {
                if (session != playSessionId) return;

                long elapsed = System.currentTimeMillis() - start;
                int p = (int) (elapsed * 100 / IMAGE_DURATION_MS);
                progressBar.setProgress(Math.min(p, 100));

                if (elapsed >= IMAGE_DURATION_MS) {
                    goNext();
                } else {
                    handler.postDelayed(progressRunnable, 16);
                }
            };
            handler.post(progressRunnable);
        }

        // ================= VIDEO =================

        private void loadVideo() {
            final long session = playSessionId;

            banner.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);

            exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
            playerView.setPlayer(exoPlayer);

            exoPlayer.setMediaItem(MediaItem.fromUri(story.getVideoUrl()));
            exoPlayer.prepare();

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (session != playSessionId) return;

                    if (state == Player.STATE_READY) {
                        loadingProgress.setVisibility(View.GONE);
                        exoPlayer.play();
                        startVideoProgress(session);
                    }

                    if (state == Player.STATE_ENDED) {
                        goNext();
                    }
                }
            });
        }

        private void startVideoProgress(long session) {
            progressRunnable = () -> {
                if (session != playSessionId || exoPlayer == null) return;

                long pos = exoPlayer.getCurrentPosition();
                long dur = exoPlayer.getDuration();
                if (dur > 0) {
                    progressBar.setProgress((int) (pos * 100 / dur));
                }
                handler.postDelayed(progressRunnable, 100);
            };
            handler.post(progressRunnable);
        }

        // ================= CONTROL =================

        private void pauseVideo() {
            if (exoPlayer != null) exoPlayer.pause();
        }

        private void resumeVideo() {
            if (exoPlayer != null) exoPlayer.play();
        }

        private void pauseProgress() {
            if (progressRunnable != null) handler.removeCallbacks(progressRunnable);
        }

        private void resumeProgress() {
            if (progressRunnable != null) handler.post(progressRunnable);
        }

        private void goNext() {
            int pos = getAdapterPosition();
            if (pos != adapter.getCurrentVisiblePosition()) return;
            HomeFragment.updateAdapter(itemView.getContext(), pos, adapter.storyAdapter);
        }

        private void reset() {
            playSessionId++;

            if (progressRunnable != null) {
                handler.removeCallbacks(progressRunnable);
                progressRunnable = null;
            }

            loadingProgress.setVisibility(View.GONE);
            progressBar.setProgress(0);

            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }

            Glide.with(itemView).clear(banner);
        }
    }
}
