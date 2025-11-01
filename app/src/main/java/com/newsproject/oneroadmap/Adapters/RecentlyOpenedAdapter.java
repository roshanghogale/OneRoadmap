package com.newsproject.oneroadmap.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.newsproject.oneroadmap.Models.RecentlyOpenedItem;
import com.newsproject.oneroadmap.R;

import java.util.List;

public class RecentlyOpenedAdapter extends RecyclerView.Adapter<RecentlyOpenedAdapter.ViewHolder> {

    private final List<RecentlyOpenedItem> itemList;

    public RecentlyOpenedAdapter(List<RecentlyOpenedItem> itemList) {
        this.itemList = itemList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView lastDate, type, title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.job_update_logo);
            lastDate = itemView.findViewById(R.id.job_update_last_date);
            type = itemView.findViewById(R.id.job_update_type);
            title = itemView.findViewById(R.id.job_update_title);
        }
    }

    @NonNull
    @Override
    public RecentlyOpenedAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recently_opened, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentlyOpenedAdapter.ViewHolder holder, int position) {
        RecentlyOpenedItem item = itemList.get(position);
        holder.logo.setImageResource(item.getLogoResId());
        holder.lastDate.setText(item.getLastDate());
        holder.type.setText(item.getType());
        holder.title.setText(item.getTitle());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}

