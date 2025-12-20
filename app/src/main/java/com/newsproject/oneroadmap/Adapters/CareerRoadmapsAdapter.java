package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.newsproject.oneroadmap.Models.CareerRoadmaps;
import com.newsproject.oneroadmap.R;

import java.util.List;

public class CareerRoadmapsAdapter extends RecyclerView.Adapter<CareerRoadmapsAdapter.CareerRoadmapsViewHolder> {
    private final List<CareerRoadmaps> careerRoadmapsList;
    private final Context context;
    private static final String TAG = "CareerRoadmapsAdapter";

    public CareerRoadmapsAdapter(List<CareerRoadmaps> careerRoadmapsList, Context context) {
        this.careerRoadmapsList = careerRoadmapsList;
        this.context = context;
    }

    @NonNull
    @Override
    public CareerRoadmapsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.career_roadmaps_item, parent, false);
        return new CareerRoadmapsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CareerRoadmapsViewHolder holder, int position) {
        CareerRoadmaps item = careerRoadmapsList.get(position);
        Log.d(TAG, "Binding item: Position=" + position + ", Title=" + item.getTitle() + ", ImageUrl=" + item.getImageUrl());

        if (holder.careerBanner != null) {
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("http://")) {
                    imageUrl = imageUrl.replace("http://", "https://");
                }
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(holder.careerBanner);
            } else {
                Log.w(TAG, "Image URL is null or empty for item: " + item.getTitle());
                Glide.with(context)
                        .load(R.drawable.placeholder_image)
                        .into(holder.careerBanner);
            }
        } else {
            Log.e(TAG, "career_banner ImageView is null");
        }

        holder.itemView.setOnClickListener(v -> {
            String pdfUrl = item.getPdfUrl();
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                if (pdfUrl.startsWith("http://")) {
                    pdfUrl = pdfUrl.replace("http://", "https://");
                }
                Log.d(TAG, "Opening PDF: " + pdfUrl);
                com.newsproject.oneroadmap.Utils.PdfViewerHelper.openPdfInApp(context, pdfUrl);
            } else {
                Log.w(TAG, "PDF URL is null or empty for item: " + item.getTitle());
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "Item count: " + careerRoadmapsList.size());
        return careerRoadmapsList.size();
    }

    static class CareerRoadmapsViewHolder extends RecyclerView.ViewHolder {
        ImageView careerBanner;

        public CareerRoadmapsViewHolder(@NonNull View itemView) {
            super(itemView);
            careerBanner = itemView.findViewById(R.id.career_banner);
            if (careerBanner == null) {
                Log.e(TAG, "career_banner ImageView not found");
            }
        }
    }
}