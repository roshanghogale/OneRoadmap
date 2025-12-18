package com.newsproject.oneroadmap.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.newsproject.oneroadmap.Models.CurrentAffairs;
import com.newsproject.oneroadmap.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CurrentAffairsAdapter extends RecyclerView.Adapter<CurrentAffairsAdapter.ViewHolder> {
    private final List<CurrentAffairs> currentAffairsList;
    private final Context context;
    private final OnPdfClickListener onPdfClickListener;
    private static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy", new Locale("mr", "IN"));
    
    public interface OnPdfClickListener {
        void onPdfClick(String pdfUrl);
    }

    public CurrentAffairsAdapter(List<CurrentAffairs> currentAffairsList, Context context, OnPdfClickListener listener) {
        this.currentAffairsList = currentAffairsList;
        this.context = context;
        this.onPdfClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.current_affairs_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CurrentAffairs item = currentAffairsList.get(position);

        // Format the date to Marathi (e.g., "२५ फेब्रुवारी २०२५")
        String formattedDate = formatDateToMarathi(item.getDate());
        holder.dateTextView.setText(formattedDate);
        Glide.with(context)
                .load(item.getImageUrl())
                .into(holder.bannerView);
        
        // Set click listener to open PDF in PDFViewerFragment
        holder.itemView.setOnClickListener(v -> {
            String pdfUrl = item.getPdfUrl();
            if (pdfUrl != null && !pdfUrl.isEmpty() && onPdfClickListener != null) {
                onPdfClickListener.onPdfClick(pdfUrl);
            }
        });
    }

    @Override
    public int getItemCount() {
        return currentAffairsList.size();
    }

    private String formatDateToMarathi(String dateStr) {
        // Check for null or empty date string
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "No Date"; // Fallback for missing date
        }

        try {
            // Parse the input date (assuming "yyyy-MM-dd" format)
            Date date = INPUT_DATE_FORMAT.parse(dateStr);
            if (date != null) {
                // Format to Marathi (e.g., "२५ फेब्रुवारी २०२५")
                return OUTPUT_DATE_FORMAT.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // Log the invalid date for debugging
            android.util.Log.e("CurrentAffairsAdapter", "Failed to parse date: " + dateStr);
        }
        return dateStr; // Return original string if formatting fails
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        ImageView bannerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.current_affair_date);
            bannerView = itemView.findViewById(R.id.current_affair_banner);
        }
    }
}