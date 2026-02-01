package com.newsproject.oneroadmap.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.newsproject.oneroadmap.Models.OnboardingModel;
import com.newsproject.oneroadmap.R;

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class OnboardingAdapter
        extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

    private final List<OnboardingModel> list;
    private final ViewPager2 viewPager;

    public OnboardingAdapter(List<OnboardingModel> list, ViewPager2 viewPager) {
        this.list = list;
        this.viewPager = viewPager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        OnboardingModel item = list.get(position);

        // Images
        holder.center.setImageResource(item.centerImage);
        holder.icon1.setImageResource(item.icon1);
        holder.icon2.setImageResource(item.icon2);
        holder.icon3.setImageResource(item.icon3);
        holder.icon4.setImageResource(item.icon4);

        // Text
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);

        // 🔥 Indicator update
        holder.updateIndicator(position);

        // Next button
        holder.next.setOnClickListener(v -> {
            if (position < list.size() - 1) {
                viewPager.setCurrentItem(position + 1, true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView center, icon1, icon2, icon3, icon4;
        TextView title, subtitle;
        CardView next;

        View dot1, dot2, dot3;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            center = itemView.findViewById(R.id.img_center);
            icon1 = itemView.findViewById(R.id.icon_1);
            icon2 = itemView.findViewById(R.id.icon_2);
            icon3 = itemView.findViewById(R.id.icon_3);
            icon4 = itemView.findViewById(R.id.icon_4);

            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
            next = itemView.findViewById(R.id.btn_next);

            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        void updateIndicator(int position) {
            dot1.setBackgroundColor(0x66FFFFFF);
            dot2.setBackgroundColor(0x66FFFFFF);
            dot3.setBackgroundColor(0x66FFFFFF);

            if (position == 0) dot1.setBackgroundColor(0xFFFFFFFF);
            else if (position == 1) dot2.setBackgroundColor(0xFFFFFFFF);
            else if (position == 2) dot3.setBackgroundColor(0xFFFFFFFF);
        }
    }
}
