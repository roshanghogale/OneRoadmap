package com.newsproject.oneroadmap.Utils;

import android.content.Context;
import com.newsproject.oneroadmap.Utils.ApiClient;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CoinManager {
    private static final int COINS_PER_SHARE = 50; // Updated to match dialog (+50 Coin)
    private static final int COINS_PER_DOWNLOAD = 25;
    private static final int COINS_PER_VIDEO = 150;
    
    private final DatabaseHelper dbHelper;
    private final ApiClient apiClient;
    private final String userId;
    
    public CoinManager(Context context, String userId) {
        this.dbHelper = new DatabaseHelper(context);
        this.apiClient = ApiClient.getInstance();
        this.userId = userId;
    }
    
    public int getCoins() {
        return dbHelper.getUserCoins(userId);
    }
    
    public boolean hasEnoughCoins(int required) {
        return getCoins() >= required;
    }
    
    public void addCoinsForShare(OnCoinsUpdatedListener listener) {
        int current = getCoins();
        int newCoins = current + COINS_PER_SHARE;
        updateCoins(newCoins, listener);
    }

    public void addCoinsForVideo(OnCoinsUpdatedListener listener) {
        int current = getCoins();
        int newCoins = current + COINS_PER_VIDEO;
        updateCoins(newCoins, listener);
    }
    
    public boolean deductCoinsForDownload(OnCoinsUpdatedListener listener) {
        int current = getCoins();
        if (current < COINS_PER_DOWNLOAD) {
            return false;
        }
        int newCoins = current - COINS_PER_DOWNLOAD;
        updateCoins(newCoins, listener);
        return true;
    }
    
    private void updateCoins(int newCoins, OnCoinsUpdatedListener listener) {
        android.util.Log.d("CoinManager", "Updating coins for userId: " + userId + ", newCoins: " + newCoins);
        // First save to local database
        dbHelper.updateUserCoins(userId, newCoins);
        android.util.Log.d("CoinManager", "Coins saved to local database");
        // Then sync to server
        saveCoinsToServer(newCoins);
        // Notify listener
        if (listener != null) {
            listener.onCoinsUpdated(newCoins);
        }
    }
    
    private void saveCoinsToServer(int coins) {
        if (userId != null && !userId.isEmpty()) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            map.put("coins", coins);
            String json = new Gson().toJson(map);
            
            android.util.Log.d("CoinManager", "Saving coins to server: " + json);
            apiClient.saveUser(json, new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    android.util.Log.e("CoinManager", "Failed to save coins to server: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    android.util.Log.d("CoinManager", "Coins saved to server successfully, response code: " + response.code());
                    response.close();
                }
            });
        } else {
            android.util.Log.w("CoinManager", "userId is null or empty, cannot save to server");
        }
    }
    
    public interface OnCoinsUpdatedListener {
        void onCoinsUpdated(int newCoins);
    }
    
    public static int getCoinsPerShare() {
        return COINS_PER_SHARE;
    }
    
    public static int getCoinsPerDownload() {
        return COINS_PER_DOWNLOAD;
    }

    public static int getCoinsPerVideo() {
        return COINS_PER_VIDEO;
    }
}
