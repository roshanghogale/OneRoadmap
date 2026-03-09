package com.newsproject.oneroadmap.Utils;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.newsproject.oneroadmap.R;

public class CoinAccessController {
    private static final String TAG = "CoinAccessController";
    private AlertDialog confirmDialog;
    private final Fragment fragment;
    private final String userId;
    private final CoinManager coinManager;
    private String pendingUrl;
    private final ShareHelper shareHelper;
    private RewardedAd rewardedAd;
    private boolean isLoadingAd = false;

    public CoinAccessController(Fragment fragment,
                                String userId,
                                ShareHelper shareHelper) {
        this.fragment = fragment;
        this.userId = userId;
        this.shareHelper = shareHelper;
        this.coinManager = new CoinManager(fragment.requireContext(), userId);
        loadRewardedAd();
    }

    private void loadRewardedAd() {
        if (isLoadingAd || rewardedAd != null) return;

        isLoadingAd = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        // Use test ID for now: ca-app-pub-3940256099942544/5224354917
        RewardedAd.load(fragment.requireContext(), "ca-app-pub-3940256099942544/5224354917",
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.toString());
                        rewardedAd = null;
                        isLoadingAd = false;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        isLoadingAd = false;
                        Log.d(TAG, "Ad was loaded.");
                    }
                });
    }

    public void requestPdfAccess(String url, Runnable onAccessGranted) {
        int required = CoinManager.getCoinsPerDownload();
        int current = coinManager.getCoins();

        if (current >= required) {
            showConfirmDialog(url, required, onAccessGranted);
        } else {
            pendingUrl = url;
            showNotEnoughDialog();
        }
    }

    private void showConfirmDialog(String url,
                                   int requiredCoins,
                                   Runnable onAccessGranted){

        int currentCoins = coinManager.getCoins();

        View view = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_confirm_pdf_coin, null);

        TextView message = view.findViewById(R.id.tv_message);
        View open = view.findViewById(R.id.btn_open);
        TextView cancel = view.findViewById(R.id.btn_cancel);

        message.setText(
                "Required Coins: " + requiredCoins +
                        "\nYour Coins: " + currentCoins +
                        "\n\nDo you want to continue?"
        );

        confirmDialog = new AlertDialog.Builder(fragment.requireContext())
                .setView(view)
                .setCancelable(false)
                .create();

        confirmDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        open.setOnClickListener(v -> {
            int before = coinManager.getCoins();
            if (before >= requiredCoins) {
                confirmDialog.dismiss();
                confirmDialog = null;

                coinManager.deductCoinsForDownload(newCoins -> {
                    showCoinIncrementAnimation(
                            before,
                            newCoins,
                            true,
                            () -> {
                                if (onAccessGranted != null) {
                                    onAccessGranted.run();
                                }
                                WebViewHelper.openUrlInApp(fragment, url);
                            }
                    );
                });
            }
        });

        cancel.setOnClickListener(v -> {
            confirmDialog.dismiss();
            confirmDialog = null;
        });

        confirmDialog.show();
    }

    private void showNotEnoughDialog() {
        int currentCoins = coinManager.getCoins();

        View view = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_not_enough_coin, null);

        TextView tvCoin = view.findViewById(R.id.tv_coin);
        View btnWatch = view.findViewById(R.id.btn_watch);
        View btnShare = view.findViewById(R.id.btn_share);

        tvCoin.setText(String.valueOf(currentCoins));

        AlertDialog dialog = new AlertDialog.Builder(fragment.requireContext())
                .setView(view)
                .setCancelable(true)
                .create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnWatch.setOnClickListener(v -> {
            dialog.dismiss();
            showVideoAd();
        });

        btnShare.setOnClickListener(v -> {
            dialog.dismiss();
            triggerShare();
        });

        dialog.show();
    }

    private void showVideoAd() {
        if (rewardedAd != null) {
            rewardedAd.show(fragment.requireActivity(), new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    Log.d(TAG, "The user earned the reward.");
                    onVideoRewarded();
                }
            });
            rewardedAd = null;
            loadRewardedAd();
        } else {
            Toast.makeText(fragment.getContext(), "Ad is loading, please try again in a moment.", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }

    private void onVideoRewarded() {
        int before = coinManager.getCoins();
        coinManager.addCoinsForVideo(newCoins -> {
            showCoinIncrementAnimation(
                    before,
                    newCoins,
                    false,
                    null
            );
        });
    }

    private void triggerShare() {
        shareHelper.sharePost("", "");
    }

    public void onShareCompleted() {
        int before = coinManager.getCoins();
        coinManager.addCoinsForShare(newCoins -> {
            showCoinIncrementAnimation(
                    before,
                    newCoins,
                    false,
                    null
            );
            pendingUrl = null;
        });
    }

    private void showCoinIncrementAnimation(
            int start,
            int end,
            boolean autoClose,
            Runnable onFinished
    ) {

        View view = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.coin_dialog_layout, null);

        TextView count = view.findViewById(R.id.coin_count);
        CardView closeBtn = view.findViewById(R.id.btn_close);

        count.setText(String.valueOf(start));

        AlertDialog dialog = new AlertDialog.Builder(fragment.requireContext())
                .setView(view)
                .setCancelable(false)
                .create();

        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        dialog.show();

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        Handler handler = new Handler();
        final int[] current = {start};
        int delay = 25;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (start < end) {
                    if (current[0] < end) {
                        current[0]++;
                        count.setText(String.valueOf(current[0]));
                        handler.postDelayed(this, delay);
                    } else finish();
                } else {
                    if (current[0] > end) {
                        current[0]--;
                        count.setText(String.valueOf(current[0]));
                        handler.postDelayed(this, delay);
                    } else finish();
                }
            }

            private void finish() {
                handler.removeCallbacks(this);

                if (autoClose) {
                    new Handler().postDelayed(() -> {
                        dialog.dismiss();
                        if (onFinished != null) onFinished.run();
                    }, 400);
                } else {
                    if (onFinished != null) onFinished.run();
                }
            }
        };

        handler.post(runnable);
    }
}