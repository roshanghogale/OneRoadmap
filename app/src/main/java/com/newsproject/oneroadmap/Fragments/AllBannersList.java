package com.newsproject.oneroadmap.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.newsproject.oneroadmap.Adapters.StudentUpdateAdapter;
import com.newsproject.oneroadmap.Models.StudentUpdateItem;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.BuildConfig;

import de.hdodenhof.circleimageview.CircleImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllBannersList extends Fragment {

    private ImageView backButton;


    public AllBannersList() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_all_banners_list, container, false);
        backButton = view.findViewById(R.id.back_button7);
        loadStudentUpdates(view);

        ConstraintLayout constraintLayout = view.findViewById(R.id.constraintLayout4);

        ViewGroup.LayoutParams params = constraintLayout.getLayoutParams();

        if (Build.VERSION.SDK_INT >= 35) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
            marginLayoutParams.setMargins(0, 100, 0, 0);
            constraintLayout.setLayoutParams(params);
        }

        backButton.setOnClickListener(v -> {
            Log.d("ReelsAdapter", "Back button pressed");
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                Log.w("ReelsAdapter", "No back stack entries to pop");
            }
        });

        return view;
    }

    private void loadStudentUpdates(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.all_banner_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        List<StudentUpdateItem> itemList = new ArrayList<>();
        StudentUpdateAdapter adapter = new StudentUpdateAdapter(itemList, getContext());
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            showStudentUpdateDialog(item);
        });

        OkHttpClient client = new OkHttpClient();
        String url = BuildConfig.BASE_URL + "/api/student-updates";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("AllBannersList", "Failed to fetch student updates: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to load student updates", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("AllBannersList", "Unexpected response: " + response.code());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Failed to load student updates", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }
                String body = response.body().string();
                try {
                    JsonObject root = new Gson().fromJson(body, JsonObject.class);
                    List<StudentUpdateItem> fetched = new ArrayList<>();
                    if (root != null && root.has("studentUpdates")) {
                        JsonArray arr = root.getAsJsonArray("studentUpdates");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject o = arr.get(i).getAsJsonObject();
                            int id = o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsInt() : 0;
                            String title = o.has("title") && !o.get("title").isJsonNull() ? o.get("title").getAsString() : "";
                            String education = o.has("education") && !o.get("education").isJsonNull() ? o.get("education").getAsString() : "";
                            String ageRestriction = o.has("age_restriction") && !o.get("age_restriction").isJsonNull() ? o.get("age_restriction").getAsString() : "";
                            String applicationMethod = o.has("application_method") && !o.get("application_method").isJsonNull() ? o.get("application_method").getAsString() : "";
                            String description = o.has("description") && !o.get("description").isJsonNull() ? o.get("description").getAsString() : "";
                            String applicationLink = o.has("application_link") && !o.get("application_link").isJsonNull() ? o.get("application_link").getAsString() : "";
                            String lastDate = o.has("last_date") && !o.get("last_date").isJsonNull() ? o.get("last_date").getAsString() : "";
                            String imageUrl = o.has("image_url") && !o.get("image_url").isJsonNull() ? o.get("image_url").getAsString() : "";
                            String iconUrl = o.has("icon_url") && !o.get("icon_url").isJsonNull() ? o.get("icon_url").getAsString() : "";
                            String notificationPdfUrl = o.has("notification_pdf_url") && !o.get("notification_pdf_url").isJsonNull() ? o.get("notification_pdf_url").getAsString() : "";
                            String selectionPdfUrl = o.has("selection_pdf_url") && !o.get("selection_pdf_url").isJsonNull() ? o.get("selection_pdf_url").getAsString() : "";
                            String createdAt = o.has("created_at") && !o.get("created_at").isJsonNull() ? o.get("created_at").getAsString() : "";

                            imageUrl = buildFullUrl(imageUrl);
                            iconUrl = buildFullUrl(iconUrl);
                            notificationPdfUrl = buildFullUrl(notificationPdfUrl);
                            selectionPdfUrl = buildFullUrl(selectionPdfUrl);
                            applicationLink = buildFullUrl(applicationLink);

                            if (imageUrl == null || imageUrl.isEmpty()) continue;

                            fetched.add(new StudentUpdateItem(id, title, education, ageRestriction, applicationMethod,
                                    description, applicationLink, lastDate, imageUrl, iconUrl, notificationPdfUrl, selectionPdfUrl, createdAt));
                        }
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            itemList.clear();
                            itemList.addAll(fetched);
                            adapter.notifyDataSetChanged();
                            Log.d("AllBannersList", "Student updates loaded, count: " + itemList.size());
                        });
                    }
                } catch (Exception e) {
                    Log.e("AllBannersList", "Failed to parse student updates: " + e.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Failed to parse student updates", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    public void showStudentUpdateDialog(@NonNull StudentUpdateItem item) {
        Context context = this.getContext();
        if (context == null) return;

        // Create Dialog with BlurDialogTheme
        Dialog dialog = new Dialog(context, R.style.BlurDialogTheme);
        dialog.setContentView(R.layout.student_update);

        // Set window attributes to match news dialog
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.dimAmount = 0.6f;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        dialog.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // Initialize views
        CircleImageView iconImageView = dialog.findViewById(R.id.circleImageView2);
        TextView titleText = dialog.findViewById(R.id.title_text);
        TextView educationValue = dialog.findViewById(R.id.education_requirement_value);
        TextView ageValue = dialog.findViewById(R.id.age_requirement_value);
        TextView descriptionText = dialog.findViewById(R.id.textView6);
        androidx.cardview.widget.CardView openLinkButton = dialog.findViewById(R.id.open_link_button);
        androidx.cardview.widget.CardView selectionPdfButton = dialog.findViewById(R.id.selection_pdf_button);

        // Load icon image
        if (item.getIconUrl() != null && !item.getIconUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getIconUrl())
                    .placeholder(R.drawable.app_logo)
                    .error(R.drawable.app_logo)
                    .into(iconImageView);
        } else {
            iconImageView.setImageResource(R.drawable.app_logo);
        }

        // Set text data
        titleText.setText(item.getTitle() != null ? item.getTitle() : "N/A");
        educationValue.setText(item.getEducation() != null ? item.getEducation() : "N/A");
        ageValue.setText(item.getAgeRestriction() != null ? item.getAgeRestriction() : "N/A");
        descriptionText.setText(item.getDescription() != null ? item.getDescription() : "N/A");

        // 🔗 Open Link Button Click
        openLinkButton.setOnClickListener(v -> {
            String link = item.getApplicationLink();
            if (link != null && !link.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "अर्जाची लिंक उपलब्ध नाही", Toast.LENGTH_SHORT).show();
            }
        });

        // 📄 Selection PDF Button Click
        selectionPdfButton.setOnClickListener(v -> {
            String pdfUrl = item.getSelectionPdfUrl();
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "सिलेक्शन PDF उपलब्ध नाही", Toast.LENGTH_SHORT).show();
            }
        });

        // Show dialog
        dialog.setCancelable(true);
        dialog.show();
    }

    private String buildFullUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        String url = filePath.startsWith("http") ? filePath : BuildConfig.BASE_URL + filePath;
        if (url.startsWith("http://")) url = url.replace("http://", "https://");
        return url;
    }
}