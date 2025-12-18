package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.newsproject.oneroadmap.Models.StudyMaterial;
import com.newsproject.oneroadmap.R;

import java.util.List;

public class StudyMaterialAdapter extends RecyclerView.Adapter<StudyMaterialAdapter.ViewHolder> {

    private List<StudyMaterial> studyMaterials;
    private Context context;
    private OnPdfClickListener onPdfClickListener;
    
    public interface OnPdfClickListener {
        void onPdfClick(String pdfUrl);
    }

    public StudyMaterialAdapter(List<StudyMaterial> studyMaterials, Context context, OnPdfClickListener listener) {
        this.studyMaterials = studyMaterials;
        this.context = context;
        this.onPdfClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_study_materiel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudyMaterial material = studyMaterials.get(position);
        Glide.with(context)
                .load(material.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .skipMemoryCache(true)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            String pdfUrl = material.getPdfUrl();
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                if (onPdfClickListener != null) {
                    onPdfClickListener.onPdfClick(pdfUrl);
                } else {
                    Toast.makeText(context, "PDF not available", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "PDF not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return studyMaterials.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.student_update_image);
        }
    }
}