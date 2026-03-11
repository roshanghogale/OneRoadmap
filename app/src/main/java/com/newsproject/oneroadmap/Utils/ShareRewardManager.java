package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShareRewardManager {
    private static final String PREFS_NAME = "ShareRewardPrefs";
    private static final String KEY_DAILY_COUNT = "daily_share_count";
    private static final String KEY_LAST_DATE = "last_share_date";
    private static final String KEY_LAST_REWARD_TIME = "last_reward_time";

    private static final long MIN_SHARE_DURATION_MS = 4000; // 4 seconds
    private static final long COOLDOWN_MS = 10000; // 10 seconds
    private static final int MAX_DAILY_SHARES = 10;

    private final Context context;
    private final CoinManager coinManager;
    private final SharedPreferences prefs;
    
    private long shareStartTime = 0;
    private boolean isShareInProgress = false;

    public interface OnRewardGrantedListener {
        void onRewardGranted(int newTotalCoins);
    }

    public ShareRewardManager(Context context, String userId) {
        this.context = context;
        this.coinManager = new CoinManager(context, userId);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Call this right before launching the share intent
     */
    public void startShare() {
        if (isCooldownActive()) {
            Toast.makeText(context, "Please wait before sharing again", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isDailyLimitReached()) {
            Toast.makeText(context, "Daily share limit reached", Toast.LENGTH_SHORT).show();
            return;
        }

        shareStartTime = System.currentTimeMillis();
        isShareInProgress = true;
    }

    /**
     * Call this when the user returns to the app from the share intent
     */
    public void onShareReturned(OnRewardGrantedListener listener) {
        if (!isShareInProgress) return;
        
        long duration = System.currentTimeMillis() - shareStartTime;
        isShareInProgress = false;

        if (duration >= MIN_SHARE_DURATION_MS) {
            giveShareReward(listener);
        } else {
            Toast.makeText(context, "Share duration too short for reward", Toast.LENGTH_SHORT).show();
        }
    }

    private void giveShareReward(OnRewardGrantedListener listener) {
        if (isDailyLimitReached()) return;

        coinManager.addCoinsForShare(newTotalCoins -> {
            incrementDailyCount();
            updateLastRewardTime();
            if (listener != null) {
                listener.onRewardGranted(newTotalCoins);
            } else {
                Toast.makeText(context, "Reward added!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isCooldownActive() {
        long lastRewardTime = prefs.getLong(KEY_LAST_REWARD_TIME, 0);
        return (System.currentTimeMillis() - lastRewardTime) < COOLDOWN_MS;
    }

    private boolean isDailyLimitReached() {
        checkAndResetDailyCount();
        return prefs.getInt(KEY_DAILY_COUNT, 0) >= MAX_DAILY_SHARES;
    }

    private void checkAndResetDailyCount() {
        String lastDate = prefs.getString(KEY_LAST_DATE, "");
        String currentDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        if (!currentDate.equals(lastDate)) {
            prefs.edit()
                    .putString(KEY_LAST_DATE, currentDate)
                    .putInt(KEY_DAILY_COUNT, 0)
                    .apply();
        }
    }

    private void incrementDailyCount() {
        int count = prefs.getInt(KEY_DAILY_COUNT, 0);
        prefs.edit().putInt(KEY_DAILY_COUNT, count + 1).apply();
    }

    private void updateLastRewardTime() {
        prefs.edit().putLong(KEY_LAST_REWARD_TIME, System.currentTimeMillis()).apply();
    }
}
