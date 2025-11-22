package com.newsproject.oneroadmap.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.newsproject.oneroadmap.Models.Reply;
import com.newsproject.oneroadmap.R;

import java.util.ArrayList;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private final ArrayList<Reply> replyList;

    public ReplyAdapter(ArrayList<Reply> replyList) {
        this.replyList = replyList;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Reply reply = replyList.get(position);

        holder.replyName.setText(reply.getName());
        holder.replyTitle.setText(reply.getTitle());



        // TODO: Load image (if URL provided) using Glide/Picasso.
        holder.replyIcon.setImageResource(R.drawable.app_logo);
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView replyName, replyTitle;
        ImageView replyIcon;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            replyName = itemView.findViewById(R.id.reply_name);
            replyTitle = itemView.findViewById(R.id.reply_title);
            replyIcon = itemView.findViewById(R.id.imageView8);
        }
    }

    // Add a method to update the list of replies dynamically
    public void addReply(Reply reply) {
        this.replyList.add(reply);
        notifyDataSetChanged();
    }
}
