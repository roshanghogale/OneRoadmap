package com.newsproject.oneroadmap.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.newsproject.oneroadmap.Adapters.ResultAdapter;
import com.newsproject.oneroadmap.Models.JobViewModel;
import com.newsproject.oneroadmap.Models.ResultItem;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.FilterUtils;
import com.newsproject.oneroadmap.databinding.DialogEducationFilterBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Result_HallTitcket extends Fragment implements ResultAdapter.OnItemClickListener {
    private static final String PREFS_NAME = "UserPrefs";
    private static final int PAGE_SIZE = 100;
    private RecyclerView recyclerView;
    private ResultAdapter adapter;
    private List<ResultItem> resultList;
    private List<ResultItem> allItems;
    private LinearLayout hallticketChip;
    private LinearLayout resultChip;
    private LinearLayout educationChip;
    private ImageView backButton;
    private String selectedType = null;
    private boolean selectedFilter = false; // Track if education filter is active
    private AlertDialog educationDialog;
    private DialogEducationFilterBinding dialogBinding;
    private Filter filter = new Filter("", "", "");
    private int currentPage = 1;
    private boolean hasMoreItems = true;
    private Call currentCall;

    public static class Filter {
        private final String category;
        private final String degree;
        private final String postGrad;

        public Filter(String category, String degree, String postGrad) {
            this.category = category;
            this.degree = degree;
            this.postGrad = postGrad;
        }

        public String getCategory() {
            return category;
        }

        public String getDegree() {
            return degree;
        }

        public String getPostGrad() {
            return postGrad;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result__hall_titcket, container, false);

        recyclerView = view.findViewById(R.id.result_hallticket_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);

        hallticketChip = view.findViewById(R.id.chip_hallticket);
        resultChip = view.findViewById(R.id.chip_result);
        educationChip = view.findViewById(R.id.chip_education);
        backButton = view.findViewById(R.id.back_button2);

        resultList = new ArrayList<>();
        allItems = new ArrayList<>();
        adapter = new ResultAdapter(resultList, getContext(), this);
        recyclerView.setAdapter(adapter);

        setupListeners();
        loadResultAndHallticketData();
        restoreEducationFilter();

        return view;
    }

    private void setupListeners() {
        hallticketChip.setOnClickListener(v -> {
            if ("hallticket".equals(selectedType)) {
                selectedType = null;
            } else {
                selectedType = "hallticket";
                clearEducationFilter(); // Clear education filter
            }
            selectedFilter = false;
            updateChipColors();
            filterItems();
        });

        resultChip.setOnClickListener(v -> {
            if ("result".equals(selectedType)) {
                selectedType = null;
            } else {
                selectedType = "result";
                clearEducationFilter(); // Clear education filter
            }
            selectedFilter = false;
            updateChipColors();
            filterItems();
        });

        educationChip.setOnClickListener(v -> showEducationDialog());

        backButton.setOnClickListener(v -> {
            selectedType = null;
            selectedFilter = false;
            clearEducationFilter();
            updateChipColors();
            filterItems();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void updateChipColors() {
        hallticketChip.setBackgroundResource("hallticket".equals(selectedType) ? R.drawable.rectangle_blue_grey : R.drawable.rectangle_with_stroke);
        TextView hallticketText = hallticketChip.findViewById(R.id.text_hallticket);
        hallticketText.setTextColor(ContextCompat.getColor(requireContext(), "hallticket".equals(selectedType) ? R.color.white : R.color.text_primary));

        resultChip.setBackgroundResource("result".equals(selectedType) ? R.drawable.rectangle_blue_grey : R.drawable.rectangle_with_stroke);
        TextView resultText = resultChip.findViewById(R.id.text_result);
        resultText.setTextColor(ContextCompat.getColor(requireContext(), "result".equals(selectedType) ? R.color.white : R.color.text_primary));

        educationChip.setBackgroundResource(!selectedFilter ? R.drawable.rectangle_white : R.drawable.rectangle_black);
        TextView educationText = educationChip.findViewById(R.id.text_education);
        ImageView educationArrow = educationChip.findViewById(R.id.imageView13);
        educationText.setTextColor(ContextCompat.getColor(requireContext(), !selectedFilter ? R.color.black : R.color.white));
        educationArrow.setImageTintList(ContextCompat.getColorStateList(requireContext(), !selectedFilter ? R.color.black : R.color.white));
    }

    private void loadResultAndHallticketData() {
        allItems.clear();
        resultList.clear();
        adapter.notifyDataSetChanged();
        currentPage = 1;
        hasMoreItems = true;
        loadMoreItems();
    }

    private void loadMoreItems() {
        if (!hasMoreItems) {
            Log.d("Result_HallTitcket", "No more items to load");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String url = "https://test.gangainstitute.in/api/result-halltickets/?page=" + currentPage + "&limit=" + PAGE_SIZE;
        Request request = new Request.Builder().url(url).build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                if (call.isCanceled()) {
                    Log.d("Result_HallTitcket", "Request canceled");
                    return;
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws java.io.IOException {
                if (call.isCanceled()) {
                    Log.d("Result_HallTitcket", "Response canceled");
                    return;
                }
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray array = json.getJSONArray("resultHalltickets");
                        List<ResultItem> tempList = new ArrayList<>();
                        Log.d("Result_HallTitcket", "Page " + currentPage + ": Received " + array.length() + " items");

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);

                            String id = obj.getString("id");
                            String title = obj.getString("title");
                            String examName = obj.optString("exam_name", "");
                            String category = obj.optString("category", "");
                            String type = obj.optString("type", "");
                            String examDateStr = obj.optString("exam_date", null);
                            String createdAtStr = obj.getString("created_at");
                            String iconUrl = obj.optString("icon_url", null);
                            String imageUrl = obj.optString("image_url", null);

                            if (iconUrl != null) {
                                iconUrl = iconUrl.replace("http://", "https://");
                            }
                            if (imageUrl != null) {
                                imageUrl = imageUrl.replace("http://", "https://");
                            }

                            Date createdAt = null;
                            SimpleDateFormat sdfCreated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                            sdfCreated.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            try {
                                createdAt = sdfCreated.parse(createdAtStr);
                            } catch (ParseException e) {
                                Log.e("DateParse", "Error parsing created_at: " + createdAtStr, e);
                            }

                            Date examDate = null;
                            String lastDate = "N/A";
                            if (examDateStr != null && !examDateStr.isEmpty()) {
                                SimpleDateFormat sdfExam = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                try {
                                    examDate = sdfExam.parse(examDateStr);
                                    lastDate = formatDate(examDate);
                                } catch (ParseException e) {
                                    Log.e("DateParse", "Error parsing exam_date: " + examDateStr, e);
                                    lastDate = examDateStr;
                                }
                            }

                            JSONObject descJson = obj.optJSONObject("description");
                            String desc1 = descJson != null ? descJson.optString("paragraph1", "") : "";
                            String desc2 = descJson != null ? descJson.optString("paragraph2", "") : "";

                            JSONArray urlsArray = obj.optJSONArray("website_urls");
                            List<Map<String, String>> websiteUrls = new ArrayList<>();
                            if (urlsArray != null) {
                                for (int j = 0; j < urlsArray.length(); j++) {
                                    JSONObject urlObj = urlsArray.getJSONObject(j);
                                    String urlValue = urlObj.optString("url", "");
                                    String titleValue = urlObj.optString("title", "Download");
                                    Log.d("URLParse", "Parsed URL " + j + ": title='" + titleValue + "', url='" + urlValue + "'");
                                    if (!urlValue.trim().isEmpty()) {
                                        Map<String, String> map = new HashMap<>();
                                        map.put("title", titleValue);
                                        map.put("url", urlValue);
                                        websiteUrls.add(map);
                                    } else {
                                        Log.w("URLParse", "Skipping invalid URL for title: " + titleValue);
                                    }
                                }
                            }

                            String educationRequirement = obj.opt("education_requirement").toString();
                            ResultItem item = new ResultItem(id, title, category, type, createdAt, lastDate, iconUrl, imageUrl, examName, desc1, desc2, educationRequirement, websiteUrls);
                            item.setExamDate(examDate);
                            tempList.add(item);
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                allItems.addAll(tempList);
                                Log.d("Result_HallTitcket", "Total items loaded: " + allItems.size());
                                if (tempList.size() < PAGE_SIZE) {
                                    hasMoreItems = false;
                                    Log.d("Result_HallTitcket", "No more pages to load");
                                } else {
                                    currentPage++;
                                    loadMoreItems(); // Fetch next page
                                }
                                filterItems();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("JSONParse", "Error parsing JSON", e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show());
                        }
                    }
                } else {
                    Log.e("Result_HallTitcket", "Unsuccessful response: " + response.code());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: Server returned " + response.code(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void filterItems() {
        List<ResultItem> filteredList = new ArrayList<>();
        boolean hasEducationFilter = selectedFilter && !filter.getCategory().isEmpty() && !filter.getCategory().equals("Select Education Category");

        for (ResultItem item : allItems) {
            boolean typeMatch = selectedType == null || item.getType().equalsIgnoreCase(selectedType);
            boolean educationMatch = true;

            if (hasEducationFilter) {
                String requirement = item.getEducationRequirement().toLowerCase();
                String selectedEducation = (filter.getCategory() + " " + filter.getDegree() +
                        (filter.getPostGrad().equals("None") || filter.getPostGrad().equals("Select Post Graduation") ? "" : " " + filter.getPostGrad())).trim().toLowerCase();
                educationMatch = requirement.contains(filter.getCategory().toLowerCase()) ||
                        requirement.contains(filter.getDegree().toLowerCase()) ||
                        (filter.getPostGrad() != null && !filter.getPostGrad().equals("None") &&
                                !filter.getPostGrad().equals("Select Post Graduation") &&
                                requirement.contains(filter.getPostGrad().toLowerCase()));
            }

            if (typeMatch && educationMatch) {
                filteredList.add(item);
            }
        }

        Log.d("Result_HallTitcket", "Filtered items: " + filteredList.size() + ", selectedType: " + selectedType + ", hasEducationFilter: " + hasEducationFilter);
        resultList.clear();
        resultList.addAll(filteredList);
        adapter.notifyDataSetChanged();
        updateChipColors();
    }

    private void saveEducationFilter() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("education", filter.getCategory());
        editor.putString("degree", filter.getDegree());
        editor.putString("postGrad", filter.getPostGrad());
        editor.apply();
        Log.d("Result_HallTitcket", "Saved education filter: " + filter.getCategory() + ", " + filter.getDegree() + ", " + filter.getPostGrad());
    }

    private void clearEducationFilter() {
        filter = new Filter("", "", "");
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("education");
        editor.remove("degree");
        editor.remove("postGrad");
        editor.apply();
        Log.d("Result_HallTitcket", "Cleared education filter");
    }

    private void restoreEducationFilter() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String category = prefs.getString("education", "");
        String degree = prefs.getString("degree", "");
        String postGrad = prefs.getString("postGrad", "");
        filter = new Filter(category, degree, postGrad);
        selectedFilter = false; // Do not apply filter on initial load
        filterItems();
        Log.d("Result_HallTitcket", "Restored education filter: " + category + ", " + degree + ", " + postGrad);
    }

    private void showEducationDialog() {
        if (!isAdded()) return;

        if (educationDialog == null) {
            dialogBinding = DialogEducationFilterBinding.inflate(LayoutInflater.from(requireContext()));
            educationDialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogBinding.getRoot())
                    .create();

            if (educationDialog.getWindow() != null) {
                educationDialog.getWindow().setBackgroundDrawableResource(R.drawable.rectangle_white);
                educationDialog.getWindow().setElevation(16f);
                educationDialog.getWindow().getDecorView().setPadding(40, 40, 40, 40);
            }

            ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, FilterUtils.getEducationOptions());
            educationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dialogBinding.spinnerEducation.setAdapter(educationAdapter);

            ArrayAdapter<String> defaultDegreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, Arrays.asList("Select Degree"));
            ArrayAdapter<String> defaultPostGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, Arrays.asList("Select Post Graduation"));
            dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
            dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);

            dialogBinding.spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String category = FilterUtils.getEducationOptions().get(position);
                    if (!category.equals("Select Education Category")) {
                        List<String> degrees = FilterUtils.getDegreeMap().getOrDefault(category, Arrays.asList("Select Degree", "Other"));
                        ArrayAdapter<String> degreeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, degrees);
                        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerDegree.setAdapter(degreeAdapter);

                        List<String> postGrads = FilterUtils.getPostGradMap().getOrDefault(category, Arrays.asList("Select Post Graduation", "None"));
                        ArrayAdapter<String> postGradAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, postGrads);
                        postGradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dialogBinding.spinnerPostGrad.setAdapter(postGradAdapter);
                    } else {
                        dialogBinding.spinnerDegree.setAdapter(defaultDegreeAdapter);
                        dialogBinding.spinnerPostGrad.setAdapter(defaultPostGradAdapter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            dialogBinding.btnCancel.setOnClickListener(v -> educationDialog.dismiss());
            dialogBinding.btnSave.setOnClickListener(v -> {
                String education = dialogBinding.spinnerEducation.getSelectedItem().toString();
                String degree = dialogBinding.spinnerDegree.getSelectedItem().toString();
                String postGrad = dialogBinding.spinnerPostGrad.getSelectedItem().toString();

                if (education.equals("Select Education Category") || degree.equals("Select Degree")) {
                    Toast.makeText(requireContext(), "Please select education category and degree!", Toast.LENGTH_SHORT).show();
                    return;
                }

                filter = new Filter(education, degree, postGrad);
                selectedFilter = true; // Enable education filter
                saveEducationFilter();
                filterItems();
                educationDialog.dismiss();
            });

            educationDialog.setOnShowListener(d -> {
                View overlay = getView().findViewById(R.id.overlay);
                if (overlay != null) overlay.setVisibility(View.VISIBLE);
            });
            educationDialog.setOnDismissListener(d -> {
                View overlay = getView().findViewById(R.id.overlay);
                if (overlay != null) overlay.setVisibility(View.GONE);
            });
        }

        JobViewModel.Filter currentFilter = new JobViewModel.Filter(filter.getCategory(), filter.getDegree(), filter.getPostGrad());
        if (!currentFilter.getCategory().isEmpty() && !currentFilter.getCategory().equals("Select Education Category")) {
            ArrayAdapter<String> educationAdapter = (ArrayAdapter<String>) dialogBinding.spinnerEducation.getAdapter();
            int position = educationAdapter.getPosition(currentFilter.getCategory());
            if (position != -1) {
                dialogBinding.spinnerEducation.setSelection(position);
                dialogBinding.spinnerEducation.getOnItemSelectedListener().onItemSelected(dialogBinding.spinnerEducation, null, position, 0);
                ArrayAdapter<String> degreeAdapter = (ArrayAdapter<String>) dialogBinding.spinnerDegree.getAdapter();
                if (degreeAdapter != null && !currentFilter.getDegree().isEmpty()) {
                    int degreePosition = degreeAdapter.getPosition(currentFilter.getDegree());
                    if (degreePosition != -1) {
                        dialogBinding.spinnerDegree.setSelection(degreePosition);
                    }
                }
                ArrayAdapter<String> postGradAdapter = (ArrayAdapter<String>) dialogBinding.spinnerPostGrad.getAdapter();
                if (postGradAdapter != null && !currentFilter.getPostGrad().isEmpty()) {
                    int postGradPosition = postGradAdapter.getPosition(currentFilter.getPostGrad());
                    if (postGradPosition != -1) {
                        dialogBinding.spinnerPostGrad.setSelection(postGradPosition);
                    }
                }
            }
        }

        educationDialog.show();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public void onItemClick(ResultItem item) {
        showDetailsDialog(item);
    }

    private void showDetailsDialog(ResultItem item) {
        Dialog dialog = new Dialog(getContext(), R.style.BlurDialogTheme);
        dialog.setContentView(R.layout.result_hallticket_card);

        // Set dialog to full-screen
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.dimAmount = 0.6f; // Adjust dimming intensity (0.0f = no dim, 1.0f = fully dark)
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        dialog.getWindow().setStatusBarColor(Color.TRANSPARENT);

        // Populate content
        ImageView image = dialog.findViewById(R.id.imageView6);
        Glide.with(getContext())
                .load(item.getIconUrl())
                .placeholder(R.drawable.image)
                .error(R.drawable.image)
                .into(image);

        TextView titleTv = dialog.findViewById(R.id.textView4);
        titleTv.setText(item.getTitle());

        TextView descTv = dialog.findViewById(R.id.textView18);
        String desc = item.getDescription1();
        if (!item.getDescription2().isEmpty()) {
            desc += "\n\n" + item.getDescription2();
        }
        descTv.setText(desc);

        // Setup URL buttons
        setupUrlButtons(dialog, item.getWebsiteUrls());

        // Close button
        ImageView close = dialog.findViewById(R.id.imageView17);
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupUrlButtons(Dialog dialog, List<Map<String, String>> urls) {
        CardView card1 = dialog.findViewById(R.id.cardView7);
        CardView card2 = dialog.findViewById(R.id.cardView8);
        CardView card3 = dialog.findViewById(R.id.cardView9);

        TextView btn1 = dialog.findViewById(R.id.textView35);
        TextView btn2 = dialog.findViewById(R.id.textView36);
        TextView btn3 = dialog.findViewById(R.id.textView37);

        card1.setVisibility(View.GONE);
        card2.setVisibility(View.GONE);
        card3.setVisibility(View.GONE);

        if (urls != null && urls.size() > 0) {
            String url1 = urls.get(0).get("url");
            if (isValidUrl(url1)) {
                card1.setVisibility(View.VISIBLE);
                btn1.setText(urls.get(0).get("title"));
                card1.setOnClickListener(v -> openUrl(url1));
            }
        }
        if (urls != null && urls.size() > 1) {
            String url2 = urls.get(1).get("url");
            if (isValidUrl(url2)) {
                card2.setVisibility(View.VISIBLE);
                btn2.setText(urls.get(1).get("title"));
                card2.setOnClickListener(v -> openUrl(url2));
            }
        }
        if (urls != null && urls.size() > 2) {
            String url3 = urls.get(2).get("url");
            if (isValidUrl(url3)) {
                card3.setVisibility(View.VISIBLE);
                btn3.setText(urls.get(2).get("title"));
                card3.setOnClickListener(v -> openUrl(url3));
            }
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.trim().isEmpty() && url.length() > 5;
    }

    private void openUrl(String url) {
        if (!isValidUrl(url)) {
            Log.w("OpenUrl", "Invalid URL provided: " + (url != null ? url : "null"));
            Toast.makeText(getContext(), "Invalid link. Cannot open.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e("OpenUrl", "Error opening URL: " + url, e);
            Toast.makeText(getContext(), "Cannot open link. Please check your browser.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (educationDialog != null && educationDialog.isShowing()) {
            educationDialog.dismiss();
        }
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        educationDialog = null;
        dialogBinding = null;
        if (currentCall != null) {
            currentCall.cancel();
        }
    }
}