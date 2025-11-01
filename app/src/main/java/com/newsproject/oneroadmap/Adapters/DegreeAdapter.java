package com.newsproject.oneroadmap.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.newsproject.oneroadmap.R;
import java.util.ArrayList;
import java.util.List;

public class DegreeAdapter extends RecyclerView.Adapter<DegreeAdapter.DegreeViewHolder> {
    private List<String> degrees;
    private List<String> selectedDegrees;

    public DegreeAdapter(List<String> degrees, List<String> selectedDegrees) {
        this.degrees = degrees;
        this.selectedDegrees = selectedDegrees;
    }

    @NonNull
    @Override
    public DegreeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_degree, parent, false);
        return new DegreeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DegreeViewHolder holder, int position) {
        String degree = degrees.get(position);
        holder.checkBox.setText(degree);
        holder.checkBox.setChecked(selectedDegrees.contains(degree));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedDegrees.contains(degree)) {
                    selectedDegrees.add(degree);
                }
            } else {
                selectedDegrees.remove(degree);
            }
        });
    }

    @Override
    public int getItemCount() {
        return degrees.size();
    }

    static class DegreeViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        DegreeViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_degree);
        }
    }
}