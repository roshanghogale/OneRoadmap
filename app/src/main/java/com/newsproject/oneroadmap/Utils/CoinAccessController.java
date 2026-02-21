package com.newsproject.oneroadmap.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.newsproject.oneroadmap.R;

public class CoinAccessController {
    private AlertDialog confirmDialog;
    private final Fragment fragment;
    private final String userId;
    private final CoinManager coinManager;
    private String pendingUrl;
    private final ShareHelper shareHelper;

    public CoinAccessController(Fragment fragment,
                                String userId,
                                ShareHelper shareHelper) {
        this.fragment = fragment;
        this.userId = userId;
        this.shareHelper = shareHelper;
        this.coinManager = new CoinManager(fragment.requireContext(), userId);
    }

    public void requestPdfAccess(String url, Runnable onAccessGranted) {

        int required = CoinManager.getCoinsPerDownload();
        int current = coinManager.getCoins();

        if (current >= required) {

            showConfirmDialog(url, required, onAccessGranted);

        } else {

            pendingUrl = url;
            showNotEnoughDialog(required);
        }
    }

    private void showConfirmDialog(String url,
                                   int requiredCoins,
                                   Runnable onAccessGranted){

        int currentCoins = coinManager.getCoins();

        View view = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_confirm_pdf_coin, null);

        TextView message = view.findViewById(R.id.tv_message);
        Button open = view.findViewById(R.id.btn_open);
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

                // ✅ CLOSE CONFIRM DIALOG FIRST
                confirmDialog.dismiss();
                confirmDialog = null;

                // Deduct coins in DB
                coinManager.deductCoinsForDownload(newCoins -> {

                    // Show animation
                    showCoinIncrementAnimation(
                            before,
                            newCoins,
                            true,
                            () -> {

                                // 🔥 Close Student Dialog FIRST
                                if (onAccessGranted != null) {
                                    onAccessGranted.run();
                                }

                                // Then open PDF
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

    private void showNotEnoughDialog(int requiredCoins) {

        int currentCoins = coinManager.getCoins();

        View view = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_not_enough_coin, null);

        TextView message = view.findViewById(R.id.tv_message);
        Button share = view.findViewById(R.id.btn_share);
        TextView cancel = view.findViewById(R.id.btn_cancel);

        message.setText(
                "Required Coins: " + requiredCoins +
                        "\nYour Coins: " + currentCoins +
                        "\n\nYou don't have enough coins.\nShare & earn coins first."
        );

        AlertDialog dialog = new AlertDialog.Builder(fragment.requireContext())
                .setView(view)
                .setCancelable(false)
                .create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        share.setOnClickListener(v -> {
            dialog.dismiss();
            triggerShare();
        });

        cancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void triggerShare() {
        shareHelper.sharePost("", "");
    }

    public void onShareCompleted() {

        int before = coinManager.getCoins();

        coinManager.addCoinsForShare(newCoins -> {

            // 🔹 Animation should STAY (no auto close)
            showCoinIncrementAnimation(
                    before,
                    newCoins,
                    false,  // do NOT auto close
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