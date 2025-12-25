package com.newsproject.oneroadmap.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.newsproject.oneroadmap.Models.Reel;
import com.newsproject.oneroadmap.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoFragment extends Fragment {

    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;

    private ImageView backButton, iconImage;
    private TextView title, description, uploadDate, timeAgo, openPostButton;

    private ExecutorService executorService;
    private boolean isFragmentAttached = false;

    private String documentId;
    private Reel reel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        // UI refs
        playerView = view.findViewById(R.id.playerView);
        progressBar = view.findViewById(R.id.progressBar);

        backButton = view.findViewById(R.id.back_button6);
        iconImage = view.findViewById(R.id.icon);
        title = view.findViewById(R.id.title);
        description = view.findViewById(R.id.discription);
        uploadDate = view.findViewById(R.id.uploadDate);
        timeAgo = view.findViewById(R.id.timeAgo);
        openPostButton = view.findViewById(R.id.textView28);

        executorService = Executors.newSingleThreadExecutor();
        isFragmentAttached = true;

        if (getArguments() != null) {
            documentId = getArguments().getString("documentId");
        }

        if (documentId != null) {
            loadReel(documentId);
        }

        backButton.setOnClickListener(v -> {
            stopVideo();
            requireActivity().onBackPressed();
        });

        return view;
    }

    private void loadReel(String docId) {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("reels").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || !isFragmentAttached) return;

                    reel = doc.toObject(Reel.class);
                    if (reel == null) return;

                    reel.setDocumentId(doc.getId());

                    if (reel.getCreatedAt() != null) {
                        Date date = reel.getCreatedAt().toDate();
                        reel.setTimeAgo(calculateTimeAgo(date));
                        reel.setUploadDate(formatUploadDate(date));
                    }

                    bindReelInfo(reel);
                    executorService.execute(() -> downloadVideo(reel));
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("VideoFragment", "Error loading reel", e);
                });
    }

    private void bindReelInfo(Reel reel) {
        title.setText(reel.getTitle());
        description.setText(reel.getDescription());
        uploadDate.setText(reel.getUploadDate());
        timeAgo.setText(reel.getTimeAgo());

        Glide.with(this)
                .load(reel.getIconUrl())
                .placeholder(R.drawable.maharashtra_poilice)
                .into(iconImage);

        openPostButton.setOnClickListener(v -> {
            stopVideo();
            if (reel.getDocumentIdWebUrl() != null) {
                com.newsproject.oneroadmap.Utils.WebViewHelper.openUrlInApp(this, reel.getDocumentIdWebUrl());
            }
        });
    }

    private void downloadVideo(Reel reel) {
        try {
            StorageReference ref = firebaseStorage.getReferenceFromUrl(reel.getVideoUrl());
            File dir = new File(requireContext().getCacheDir(), "videos");
            if (!dir.exists()) dir.mkdirs();

            File localFile = new File(dir, ref.getName());

            if (localFile.exists()) {
                reel.setVideoPath(localFile.getAbsolutePath());
                playVideo(reel);
            } else {
                ref.getFile(localFile).addOnSuccessListener(task -> {
                    reel.setVideoPath(localFile.getAbsolutePath());
                    playVideo(reel);
                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("VideoFragment", "Video download failed", e);
                });
            }
        } catch (Exception e) {
            Log.e("VideoFragment", "Error downloading", e);
        }
    }

    private void playVideo(Reel reel) {
        if (!isFragmentAttached) return;

        requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);

            if (player == null) {
                player = new ExoPlayer.Builder(requireContext()).build();
                playerView.setPlayer(player);
            }

            MediaItem item = MediaItem.fromUri(reel.getVideoPath());
            player.setMediaItem(item);
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
            player.prepare();
            player.setPlayWhenReady(true);
        });
    }

    private String calculateTimeAgo(Date createdAt) {
        long diff = System.currentTimeMillis() - createdAt.getTime();
        long mins = diff / (1000 * 60);
        if (mins < 60) return mins + "m ago";
        long hours = mins / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    private String formatUploadDate(Date date) {
        return new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date);
    }

    private void stopVideo() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.stop();
            player.release();
            player = null;
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        isFragmentAttached = false;
        if (player != null) {
            player.release();
            player = null;
        }
        if (executorService != null) executorService.shutdownNow();
    }
}
