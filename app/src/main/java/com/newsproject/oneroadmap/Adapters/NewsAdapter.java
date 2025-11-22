package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.newsproject.oneroadmap.Fragments.NewsFragment;
import com.newsproject.oneroadmap.Models.News;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.BuildConfig;
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;

import java.lang.reflect.Type;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {
    private final List<News> newsList;
    private final Context context;

    public NewsAdapter(List<News> newsList, Context context) {
        this.newsList = newsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.news_card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        News news = newsList.get(position);

        // Set title
        holder.titleTextView.setText(news.getTitle());

        // Set type badge
        if (news.getType() != null && !news.getType().isEmpty()) {
            holder.typeTextView.setText(news.getType());
            holder.typeCardView.setVisibility(View.VISIBLE);
        } else {
            holder.typeCardView.setVisibility(View.GONE);
        }

        // Set time ago using TimeAgoUtil (same as JobUpdate)
        String timeAgo = getTimeAgo(news);
        holder.timeAgoTextView.setText(timeAgo);

        // Load image - clear previous image first to avoid showing stale images
        Glide.with(context).clear(holder.bannerImageView);
        
        String imageUrl = news.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // URL should already be built in loadNews(), but build it again as safety measure
            String fullUrl = buildFullUrl(imageUrl);
            if (fullUrl != null) {
                // Use signature with news ID to ensure unique cache key per news item
                Glide.with(context)
                        .load(fullUrl)
                        .placeholder(R.drawable.student_update_1)
                        .error(R.drawable.student_update_1)
                        .signature(new com.bumptech.glide.signature.ObjectKey("news_" + news.getId()))
                        .into(holder.bannerImageView);
            } else {
                holder.bannerImageView.setImageResource(R.drawable.student_update_1);
            }
        } else {
            // Set placeholder if no image URL
            holder.bannerImageView.setImageResource(R.drawable.student_update_1);
        }

        // Set click listener to navigate to NewsFragment
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) context;
                // Serialize news list to JSON
                Gson gson = new Gson();
                String newsListJson = gson.toJson(newsList);
                
                // Navigate to NewsFragment with full list and clicked position
                NewsFragment fragment = NewsFragment.newInstance(newsListJson, position);
                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    private String getTimeAgo(News news) {
        // First try created_at Date object (preferred)
        if (news.getCreatedAt() != null) {
            return TimeAgoUtil.getTimeAgo(news.getCreatedAt());
        }
        
        // If created_at is null, try the date field (String format: "yyyy-MM-dd")
        if (news.getDate() != null && !news.getDate().isEmpty()) {
            return TimeAgoUtil.getTimeAgo(news.getDate());
        }
        
        // Fallback
        return "Unknown";
    }

    private String buildFullUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        String url = filePath.startsWith("http") ? filePath : BuildConfig.BASE_URL + filePath;
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        return url;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImageView;
        TextView titleTextView;
        TextView timeAgoTextView;
        TextView typeTextView;
        View typeCardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImageView = itemView.findViewById(R.id.news_banner);
            titleTextView = itemView.findViewById(R.id.news_title);
            timeAgoTextView = itemView.findViewById(R.id.news_time_ago);
            typeTextView = itemView.findViewById(R.id.textView11);
            typeCardView = itemView.findViewById(R.id.cardView11);
        }
    }
}

