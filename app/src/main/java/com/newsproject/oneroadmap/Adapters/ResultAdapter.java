package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.newsproject.oneroadmap.Models.ResultItem;
import com.newsproject.oneroadmap.R;

import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
    private final List<ResultItem> resultList;
    private final Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ResultItem item);
    }

    public ResultAdapter(List<ResultItem> resultList, Context context, OnItemClickListener listener) {
        this.resultList = resultList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.result_hallticket_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResultItem item = resultList.get(position);
        holder.title.setText(item.getTitle());
        holder.category.setText(item.getCategory());
        holder.type.setText(item.getType().equals("result") ? "निकाल" : "प्रवेशपत्र");
        holder.time.setText(item.getTime());
        holder.examDate.setText(item.getLastDate() != null ? item.getLastDate() : "N/A");

        // Load image from URL using Glide
        Glide.with(context)
                .load(item.getIconUrl())
                .placeholder(R.drawable.image) // Use your placeholder drawable
                .error(R.drawable.image) // Use your error drawable, same as placeholder for now
                .into(holder.image);

        // Set card background color based on category
        switch (item.getCategory()) {
            case "government":
                holder.cardView.setCardBackgroundColor(Color.parseColor("#C0BFFF"));
                break;
            case "banking":
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FF9436"));
                break;
            case "private":
                holder.cardView.setCardBackgroundColor(Color.parseColor("#75EE77"));
                break;
            default:
                holder.cardView.setCardBackgroundColor(Color.parseColor("#EE7575"));
                break;
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, type, time, examDate;
        ImageView image;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.result_hallticket_icon);
            title = itemView.findViewById(R.id.result_hallticket_title);
            category = itemView.findViewById(R.id.result_hallticket_category);
            type = itemView.findViewById(R.id.result_hallticket_type);
            time = itemView.findViewById(R.id.result_hallticket_time);
            examDate = itemView.findViewById(R.id.result_hallticket_lastdate);
            cardView = itemView.findViewById(R.id.cardView2);
        }
    }
}