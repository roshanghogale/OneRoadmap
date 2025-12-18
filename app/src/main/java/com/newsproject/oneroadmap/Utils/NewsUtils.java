package com.newsproject.oneroadmap.Utils;

import com.newsproject.oneroadmap.Utils.ShareHelper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.newsproject.oneroadmap.Models.News;
import java.io.IOException;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.app.Dialog;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.newsproject.oneroadmap.R;

public class NewsUtils {
    private static final OkHttpClient client = new OkHttpClient();
    private static final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final String BASE_URL = "https://test.gangainstitute.in/api/news/";

    public static void fetchNews(String id, Map<String, News> newsCache, Context context, Runnable onComplete) {
        String url = BASE_URL + id;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();
        Log.d("NewsUtils", "Fetching news: id=" + id + ", url=" + url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NewsUtils", "Failed to fetch news: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(context, "Failed to load news", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("NewsUtils", "Non-200 for news: code=" + response.code());
                    mainHandler.post(() -> {
                        Toast.makeText(context, "Failed to load news", Toast.LENGTH_SHORT).show();
                        if (onComplete != null) onComplete.run();
                    });
                    return;
                }
                String body = response.body().string();
                processResponse(body, id, newsCache, context, onComplete);
            }
        });
    }

    public static void processResponse(String body, String id, Map<String, News> newsCache, Context context, Runnable onComplete) {
        try {
            JsonObject root = new Gson().fromJson(body, JsonObject.class);
            JsonObject newsJson = null;
            if (root != null) {
                if (root.has("news") && root.get("news").isJsonObject()) {
                    newsJson = root.getAsJsonObject("news");
                } else if (root.has("data") && root.get("data").isJsonObject()) {
                    newsJson = root.getAsJsonObject("data");
                } else if (root.has("id")) {
                    newsJson = root;
                }
            }
            if (newsJson != null) {
                News news = new Gson().fromJson(newsJson, News.class);
                newsCache.put(id, news);
                mainHandler.post(() -> {
                    if (onComplete != null) onComplete.run();
                });
            } else {
                Log.e("NewsUtils", "Unexpected news response shape: " + body);
                mainHandler.post(() -> {
                    Toast.makeText(context, "Failed to load news", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                });
            }
        } catch (Exception e) {
            Log.e("NewsUtils", "Exception parsing news response: " + e.getMessage(), e);
            mainHandler.post(() -> {
                Toast.makeText(context, "Failed to load news", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
            });
        }
    }

    public static void showNewsDialog(News news, Context context, android.app.ProgressDialog progressDialog) {
        if (news == null) {
            mainHandler.post(() -> {
                Toast.makeText(context, "Failed to load news", Toast.LENGTH_SHORT).show();
                if (progressDialog != null) progressDialog.dismiss();
            });
            return;
        }

        Dialog dialog = new Dialog(context, R.style.BlurDialogTheme);
        dialog.setContentView(R.layout.goverment_news_card);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.dimAmount = 0.6f;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        dialog.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        ImageView imageView = dialog.findViewById(R.id.imageView6);
        TextView titleView = dialog.findViewById(R.id.textView4);
        TextView descriptionView = dialog.findViewById(R.id.textView18);

        Glide.with(context)
                .load(news.getImageUrl())
                .placeholder(R.drawable.ic_news_banner)
                .into(imageView);
        titleView.setText(news.getTitle());
        StringBuilder descriptionText = new StringBuilder();
        if (news.getDescription().getParagraph1() != null) {
            descriptionText.append(news.getDescription().getParagraph1());
        }
        if (news.getDescription().getParagraph2() != null) {
            if (descriptionText.length() > 0) descriptionText.append("\n\n");
            descriptionText.append(news.getDescription().getParagraph2());
        }
        if (news.getDescription().getParagraph3() != null) {
            if (descriptionText.length() > 0) descriptionText.append("\n\n");
            descriptionText.append(news.getDescription().getParagraph3());
        }
        if (news.getDescription().getParagraph4() != null) {
            if (descriptionText.length() > 0) descriptionText.append("\n\n");
            descriptionText.append(news.getDescription().getParagraph4());
        }
        descriptionView.setText(descriptionText.toString().trim());

        dialog.findViewById(R.id.cardView7).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.imageView17).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.cardView).setOnClickListener(v -> {
            ShareHelper shareHelper = new ShareHelper(context);
            shareHelper.sharePost(news.getTitle(), news.getWebUrl());
        });

        mainHandler.post(() -> {
            dialog.show();
            if (progressDialog != null) progressDialog.dismiss();
        });
    }
}