package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.newsproject.oneroadmap.R;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {
    private Context context;
    private List<String> avatarList;
    private OnAvatarClickListener listener;
    private int selectedPosition = -1; // Track selected item

    public interface OnAvatarClickListener {
        void onAvatarClick(String drawableName);
    }

    public AvatarAdapter(Context context, List<String> avatarList, OnAvatarClickListener listener) {
        this.context = context;
        this.avatarList = avatarList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.avatar_item, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        String drawableName = avatarList.get(position);
        int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        if (resId != 0) {
            holder.imageView.setImageResource(resId);
        }

        // Set background based on selection
        holder.avatarContainer.setBackgroundResource(
                position == selectedPosition ?
                        R.drawable.gradient_progress :
                        R.drawable.gray_background
        );

        holder.imageView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged(); // Refresh to update backgrounds
            listener.onAvatarClick(drawableName);
        });
    }

    @Override
    public int getItemCount() {
        return avatarList.size();
    }

    public void updateAvatars(List<String> newAvatarList) {
        this.avatarList = newAvatarList;
        selectedPosition = -1; // Reset selection
        notifyDataSetChanged();
    }

    public void setSelectedAvatar(String drawableName) {
        selectedPosition = avatarList.indexOf(drawableName);
        notifyDataSetChanged();
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageView;
        LinearLayout avatarContainer;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView32);
            avatarContainer = itemView.findViewById(R.id.avatar_container);
        }
    }
}