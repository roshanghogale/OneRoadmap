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
import com.newsproject.oneroadmap.Models.Query;
import com.newsproject.oneroadmap.Models.Reply;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.ApiClient;
import com.newsproject.oneroadmap.Utils.TimeAgoUtil;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final Context context;
    private final ArrayList<Query> chatList;
    private final String userId; // User ID to check for likes
    private OnLikeToggleListener onLikeToggleListener; // Callback for like toggles

    public interface OnLikeToggleListener {
        void onLikeToggled(Query query);
    }

    public ChatAdapter(ArrayList<Query> chatList, Context context, String userId) {
        this.chatList = chatList;
        this.context = context;
        this.userId = userId; // Current user's ID
    }
    
    public void setOnLikeToggleListener(OnLikeToggleListener listener) {
        this.onLikeToggleListener = listener;
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
        holder.queryType.setText(query.getType());
        holder.queryTitle.setText(query.getTitle());
        holder.queryTime.setText(TimeAgoUtil.getTimeAgo(query.getUploadTime()));

        // Initialize like state and count (deduplicate to avoid stale duplicates)
        List<String> likedByUsers = query.getLikedByUsers() != null ? query.getLikedByUsers() : new ArrayList<>();
        if (likedByUsers.size() > 1) {
            java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>(likedByUsers);
            if (set.size() != likedByUsers.size()) {
                likedByUsers = new ArrayList<>(set);
                query.setLikedByUsers(likedByUsers);
            }
        }
        boolean isLiked = likedByUsers.contains(userId);
        Glide.with(context)
                .load(isLiked ? R.drawable.like_active : R.drawable.like_inactive)
                .placeholder(R.drawable.like_inactive)
                .into(holder.likeButton);
        holder.likeCount.setText(String.valueOf(likedByUsers.size()));
        // Initialize expand label/icon (fallback to first TextView/ImageView inside expandButton)
        TextView expandLabel = findFirstTextView(holder.expandButton);
        ImageView expandIcon = findFirstImageView(holder.expandButton);
        if (expandLabel != null) expandLabel.setText("Show answer");
        if (expandIcon != null) expandIcon.setRotation(0f);

        // Long press: allow owner to Edit/Delete
        holder.itemView.setOnLongClickListener(v -> {
            if (query.getUserId() != null && query.getUserId().equals(userId)) {
                CharSequence[] options = new CharSequence[]{"Edit", "Delete"};
                new AlertDialog.Builder(context)
                        .setTitle("Manage Query")
                        .setItems(options, (dlg, which) -> {
                            if (which == 0) {
                                showEditDialog(query, holder.getAdapterPosition());
                            } else if (which == 1) {
                                confirmDelete(query, holder.getAdapterPosition());
                            }
                        })
                        .show();
                return true;
            }
            return false;
        });

        // like button click

        // Handle like button click
        holder.likeButton.setOnClickListener(v -> toggleLike(holder, query, position));

        // Load avatar by drawable name stored in imageUrl (userRs)
        int resId = context.getResources().getIdentifier(query.getImageUrl(), "drawable", context.getPackageName());
        if (resId == 0) resId = R.drawable.career_images;
        Glide.with(context)
                .load(resId)
                .placeholder(R.drawable.career_images)
                .into(holder.queryIcon);

        // Handle reply section visibility and population
        holder.expandButton.setOnClickListener(v -> {
            Reply reply = query.getReply();
            String replyText = reply != null ? reply.getTitle() : null;

            if (replyText.equals("") && replyText == null) {
                Toast.makeText(context, "Reply is not present yet " + reply.getTitle(), Toast.LENGTH_SHORT).show();
                return;
            }

            boolean willShow = holder.replyList.getVisibility() == View.GONE;
            if (willShow) {
                holder.replyList.setVisibility(View.VISIBLE);
                holder.replyList.removeAllViews(); // Clear previous replies

                LayoutInflater inflater = LayoutInflater.from(context);
                View replyView = inflater.inflate(R.layout.item_reply, holder.replyList, false);

                TextView titleTextView = replyView.findViewById(R.id.reply_title);
                TextView nameTextView = replyView.findViewById(R.id.reply_name);

                titleTextView.setText(replyText);
                nameTextView.setText(reply.getName());

                holder.replyList.addView(replyView);
                holder.linearHorizontal.setVisibility(View.VISIBLE);

                TextView lbl = findFirstTextView(holder.expandButton);
                ImageView icon = findFirstImageView(holder.expandButton);
                if (lbl != null) lbl.setText("Hide answer");
                if (icon != null) icon.setRotation(180f); // simulate arrow up
            } else {
                holder.replyList.setVisibility(View.GONE);
                holder.linearHorizontal.setVisibility(View.GONE);

                TextView lbl = findFirstTextView(holder.expandButton);
                ImageView icon = findFirstImageView(holder.expandButton);
                if (lbl != null) lbl.setText("Show answer");
                if (icon != null) icon.setRotation(0f); // arrow down
            }
        });
    }

    private void toggleLike(ChatViewHolder holder, Query query, int position) {
        List<String> current = query.getLikedByUsers() != null ? new ArrayList<>(query.getLikedByUsers())
                : new ArrayList<>();
        boolean currentlyLiked = current.contains(userId);

        // Optimistic update: toggle locally
        if (currentlyLiked) {
            // Remove all occurrences to ensure clean state
            while (current.remove(userId)) { /* keep removing */ }
        } else {
            if (!current.contains(userId)) current.add(userId);
        }
        query.setLikedByUsers(current);

        // Update UI immediately
        boolean newLikedState = !currentlyLiked;
        Glide.with(context)
                .load(newLikedState ? R.drawable.like_active : R.drawable.like_inactive)
                .placeholder(R.drawable.like_inactive)
                .into(holder.likeButton);
        holder.likeCount.setText(String.valueOf(current.size()));
        
        // Notify listener to update allChats and re-apply filters
        if (onLikeToggleListener != null) {
            onLikeToggleListener.onLikeToggled(query);
        }

        // Send to server using save/update by userId
        try {
            JSONObject body = new JSONObject();
            JSONArray liked = new JSONArray();
            for (String u : current) liked.put(u);
            body.put("userId", query.getUserId());     // identify the query by its author's userId
            body.put("likedByUsers", liked);           // full array

            ApiClient.getInstance().saveOrUpdateQuery(body.toString(), new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    Log.e("QueriesAPI", "Error updating like status", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    int code = response.code();
                    response.close();
                    if (code == 400 || code == 404) {
                        // Fallback: try PUT /api/queries/{id} if server requires id-based update
                        String id = query.getDocumentId();
                        if (id != null && !id.isEmpty()) {
                            try {
                                JSONObject putBody = new JSONObject();
                                JSONArray putLiked = new JSONArray();
                                for (String u : current) putLiked.put(u);
                                putBody.put("likedByUsers", putLiked);
                                ApiClient.getInstance().updateQuery(id, putBody.toString(), new Callback() {
                                    @Override
                                    public void onFailure(Call call, java.io.IOException e) {
                                        Log.e("QueriesAPI", "PUT like update failed", e);
                                    }

                                    @Override
                                    public void onResponse(Call call, Response resp) throws java.io.IOException {
                                        // Just log; UI already updated optimistically
                                        resp.close();
                                        if (!resp.isSuccessful()) {
                                            Log.e("QueriesAPI", "PUT like update non-2xx: " + resp.code());
                                        }
                                    }
                                });
                            } catch (JSONException je) {
                                Log.e("QueriesAPI", "PUT JSON build error", je);
                            }
                        } else {
                            Log.e("QueriesAPI", "No documentId to fallback PUT like update");
                        }
                    } else if (! (code >= 200 && code < 300)) {
                        Log.e("QueriesAPI", "Update like failed: " + code);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e("QueriesAPI", "JSON build error", e);
        }
    }

    private void showEditDialog(Query query, int position) {
        final EditText input = new EditText(context);
        input.setHint("Update title");
        input.setText(query.getTitle());
        new AlertDialog.Builder(context)
                .setTitle("Edit Query")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newTitle = input.getText().toString().trim();
                    if (newTitle.isEmpty()) return;
                    try {
                        // Use POST /api/queries with userId and changed field(s)
                        JSONObject body = new JSONObject();
                        body.put("userId", query.getUserId());
                        body.put("title", newTitle);
                        ApiClient.getInstance().saveOrUpdateQuery(body.toString(), new Callback() {
                            @Override
                            public void onFailure(Call call, java.io.IOException e) {
                                Log.e("QueriesAPI", "Update failed", e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws java.io.IOException {
                                response.close();
                                if (response.isSuccessful()) {
                                    ((android.app.Activity) context).runOnUiThread(() -> {
                                        // Preserve the "(time ago)" suffix already appended in title
                                        String current = query.getTitle();
                                        String suffix = "";
                                        int idx = current.indexOf(" (");
                                        if (idx >= 0) suffix = current.substring(idx);
                                        Query updated = new Query(
                                                query.getUserId(),
                                                query.getName(),
                                                query.getEducation(),
                                                query.getType(),
                                                newTitle + suffix,
                                                query.getUploadTime(),
                                                query.getImageUrl(),
                                                query.getReply(),
                                                query.getLike(),
                                                query.getLikeCount(),
                                                query.getDocumentId(),
                                                new ArrayList<>(query.getLikedByUsers())
                                        );
                                        chatList.set(position, updated);
                                        notifyItemChanged(position);
                                    });
                                } else {
                                    Log.e("QueriesAPI", "Update error: " + response.code());
                                }
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("QueriesAPI", "JSON error", e);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(Query query, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Query")
                .setMessage("Are you sure you want to delete this query?")
                .setPositiveButton("Delete", (d, w) -> {
                    ApiClient.getInstance().deleteQuery(query.getDocumentId(), new Callback() {
                        @Override
                        public void onFailure(Call call, java.io.IOException e) {
                            Log.e("QueriesAPI", "Delete failed", e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws java.io.IOException {
                            response.close();
                            if (response.isSuccessful()) {
                                ((android.app.Activity) context).runOnUiThread(() -> {
                                    chatList.remove(position);
                                    notifyItemRemoved(position);
                                });
                            } else {
                                Log.e("QueriesAPI", "Delete error: " + response.code());
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView queryName, queryType, queryTime, queryTitle, likeCount;
        LinearLayout expandButton, replyList, linearHorizontal;
        ImageView queryIcon, likeButton;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            queryName = itemView.findViewById(R.id.query_name);
            queryType = itemView.findViewById(R.id.query_type);
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

    private TextView findFirstTextView(ViewGroup group) {
        if (group == null) return null;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) return (TextView) child;
            if (child instanceof ViewGroup) {
                TextView nested = findFirstTextView((ViewGroup) child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private ImageView findFirstImageView(ViewGroup group) {
        if (group == null) return null;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ImageView) return (ImageView) child;
            if (child instanceof ViewGroup) {
                ImageView nested = findFirstImageView((ViewGroup) child);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
