package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.newsproject.oneroadmap.Models.Query;
import com.newsproject.oneroadmap.Models.Reply;
import com.newsproject.oneroadmap.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final Context context;
    private final ArrayList<Query> chatList;
    private final String userId; // User ID to check for likes

    public ChatAdapter(ArrayList<Query> chatList, Context context, String userId) {
        this.chatList = chatList;
        this.context = context;
        this.userId = userId; // Current user's ID
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_query, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Query query = chatList.get(position);

        holder.queryName.setText(query.getName());
        holder.queryEducation.setText(query.getEducation());
        holder.queryTitle.setText(query.getTitle());

        // Get likedByUsers array
        List<String> likedByUsers = query.getLikedByUsers();
        boolean isLiked = likedByUsers.contains(userId); // Check if current user has liked it

        // Update like button icon
        Glide.with(context)
                .load(isLiked ? R.drawable.like_active : R.drawable.like_inactive)
                .placeholder(R.drawable.like_inactive)
                .into(holder.likeButton);

        // Update like count
        holder.likeCount.setText(String.valueOf(likedByUsers.size()));

        // Handle like button click
        holder.likeButton.setOnClickListener(v -> {
            updateLikeStatus(query, isLiked, position);
        });

        // Load query image using Glide
        Glide.with(context)
                .load(query.getImageUrl())
                .placeholder(R.drawable.career_images)
                .into(holder.queryIcon);

        // Handle reply section visibility and population
        holder.expandButton.setOnClickListener(v -> {
            if (holder.replyList.getVisibility() == View.GONE) {
                holder.replyList.setVisibility(View.VISIBLE);
                holder.replyList.removeAllViews(); // Clear previous replies

                Reply reply = query.getReply();
                if (reply != null) {
                    LayoutInflater inflater = LayoutInflater.from(context);
                    View replyView = inflater.inflate(R.layout.item_reply, holder.replyList, false);

                    TextView titleTextView = replyView.findViewById(R.id.reply_title);
                    TextView nameTextView = replyView.findViewById(R.id.reply_name);

                    titleTextView.setText(reply.getTitle());
                    nameTextView.setText(reply.getName());

                    holder.replyList.addView(replyView);
                    holder.linearHorizontal.setVisibility(View.VISIBLE);
                } else {
                    holder.replyList.setVisibility(View.GONE);
                    holder.linearHorizontal.setVisibility(View.GONE);
                }
            } else {
                holder.replyList.setVisibility(View.GONE);
                holder.linearHorizontal.setVisibility(View.GONE);
            }
        });
    }

    private void updateLikeStatus(Query query, boolean isLiked, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String documentId = query.getDocumentId(); // Ensure you have the Firestore document ID

        if (documentId == null) {
            Log.e("FirestoreError", "Document ID is null for query: " + query.getTitle());
            return;
        }

        List<String> likedByUsers = new ArrayList<>(query.getLikedByUsers());

        if (isLiked) {
            likedByUsers.remove(userId); // Remove user ID from array
        } else {
            likedByUsers.add(userId); // Add user ID to array
        }

        // Update Firestore document
        db.collection("chats").document(documentId)
                .update("likedByUsers", likedByUsers)
                .addOnSuccessListener(aVoid -> {
                    query.setLikedByUsers(likedByUsers); // Update local data
                    notifyItemChanged(position); // Refresh only the updated item
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating like status", e));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView queryName, queryEducation, queryTime, queryTitle, likeCount;
        LinearLayout expandButton, replyList, linearHorizontal;
        ImageView queryIcon, likeButton;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            queryName = itemView.findViewById(R.id.query_name);
            queryEducation = itemView.findViewById(R.id.query_education);
            queryTime = itemView.findViewById(R.id.query_time);
            queryTitle = itemView.findViewById(R.id.query_title);
            queryIcon = itemView.findViewById(R.id.query_icon);
            expandButton = itemView.findViewById(R.id.query_expand_button);
            replyList = itemView.findViewById(R.id.query_linear_for_reply);
            linearHorizontal = itemView.findViewById(R.id.linearHorizontal);
            likeButton = itemView.findViewById(R.id.query_like_button);
            likeCount = itemView.findViewById(R.id.query_like_count); // Assuming this exists in layout

            replyList.setVisibility(View.GONE);
            linearHorizontal.setVisibility(View.GONE);
        }
    }
}
