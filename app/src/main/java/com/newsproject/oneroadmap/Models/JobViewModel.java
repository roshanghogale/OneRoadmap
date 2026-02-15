package com.newsproject.oneroadmap.Models;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import com.newsproject.oneroadmap.Fragments.JobUpdateDetails;
import com.newsproject.oneroadmap.R;
import com.newsproject.oneroadmap.Utils.BuildConfig;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JobViewModel extends ViewModel {
    private static final OkHttpClient client = new OkHttpClient();
    private static final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private static final String TAG = "JobViewModel";
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static final String UPLOADS_PATH = BuildConfig.UPLOADS_PATH;
    private static final String PREFS_NAME = "UserPrefs";
    private static final int PAGE_SIZE = 147;
    private final MutableLiveData<List<JobUpdate>> jobs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> activeTab = new MutableLiveData<>("all");
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Filter> filter = new MutableLiveData<>(new Filter("", "", ""));
    private final MutableLiveData<String> selectedChip = new MutableLiveData<>(null);
    private final MutableLiveData<String> currentJobType = new MutableLiveData<>(null);
    private final List<JobUpdate> allJobs = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Call currentCall;
    private int currentPage = 1;
    private boolean hasMoreJobs = true;

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

    public LiveData<List<JobUpdate>> getJobs() {
        return jobs;
    }

    public LiveData<String> getActiveTab() {
        return activeTab;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Filter> getFilter() {
        return filter;
    }

    public LiveData<String> getSelectedChip() {
        return selectedChip;
    }

    public void setCurrentJobType(String type) {
        //Log.d(TAG, "setCurrentJobType: " + type);
        currentJobType.setValue(type);
    }

    public static void fetchJobUpdate(String id, Map<String, JobUpdate> jobUpdateCache, Context context, Runnable onComplete) {
        String base = BuildConfig.JOB_UPDATES;
        String effectiveBase = base != null ? base : "";
        if (!effectiveBase.startsWith("http")) {
            String root = BuildConfig.BASE_URL;
            if (root == null) root = "";
            if (root.endsWith("/") && effectiveBase.startsWith("/")) {
                effectiveBase = root + effectiveBase.substring(1);
            } else if (!root.endsWith("/") && !effectiveBase.startsWith("/")) {
                effectiveBase = root + "/" + effectiveBase;
            } else {
                effectiveBase = root + effectiveBase;
            }
        }
        if (effectiveBase.startsWith("http://")) {
            effectiveBase = effectiveBase.replace("http://", "https://");
        }
        String url = effectiveBase.endsWith("/") ? (effectiveBase + id) : (effectiveBase + "/" + id);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();
        Log.d("JobUpdateUtils", "Fetching job update: id=" + id + ", url=" + url);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("JobUpdateUtils", "Failed to fetch job update: " + e.getMessage());
                mainHandler.post(() -> {
                    //Toast.makeText(context, "Failed to load post", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("JobUpdateUtils", "Non-200 for job update: code=" + response.code());
                    mainHandler.post(() -> {
                        //Toast.makeText(context, "Failed to load post", Toast.LENGTH_SHORT).show();
                        if (onComplete != null) onComplete.run();
                    });
                    return;
                }
                String body = response.body().string();
                processResponse(body, id, jobUpdateCache, context, onComplete);
            }
        });
    }

    public static void processResponse(String body, String id, Map<String, JobUpdate> jobUpdateCache, Context context, Runnable onComplete) {
        try {
            JsonObject root = new Gson().fromJson(body, JsonObject.class);
            JsonObject jobUpdateJson = null;
            if (root != null) {
                if (root.has("jobUpdate") && root.get("jobUpdate").isJsonObject()) {
                    jobUpdateJson = root.getAsJsonObject("jobUpdate");
                } else if (root.has("job_update") && root.get("job_update").isJsonObject()) {
                    jobUpdateJson = root.getAsJsonObject("job_update");
                } else if (root.has("data") && root.get("data").isJsonObject()) {
                    jobUpdateJson = root.getAsJsonObject("data");
                } else if (root.has("id")) {
                    jobUpdateJson = root;
                }
            }
            if (jobUpdateJson != null) {
                JobUpdate job = new Gson().fromJson(jobUpdateJson, JobUpdate.class);
                jobUpdateCache.put(id, job);
                mainHandler.post(() -> {
                    if (onComplete != null) onComplete.run();
                });
            } else {
                Log.e("JobUpdateUtils", "Unexpected job update response shape: " + body);
                mainHandler.post(() -> {
                    //Toast.makeText(context, "Failed to load post", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                });
            }
        } catch (Exception e) {
            Log.e("JobUpdateUtils", "Exception parsing job update response: " + e.getMessage(), e);
            mainHandler.post(() -> {
                //Toast.makeText(context, "Failed to load post", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
            });
        }
    }

    public static void navigateToJobDetails(JobUpdate job, Context context, android.app.ProgressDialog progressDialog) {
        if (job != null) {
            FragmentActivity activity = (FragmentActivity) context;
            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            JobUpdateDetails fragment = JobUpdateDetails.newInstance(job);
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            Toast.makeText(context, "Failed to open post", Toast.LENGTH_SHORT).show();
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public void handleTypeChipClick(String type, String subType, Context context) {
        String key = type + (subType != null ? "-" + subType : "");
        String current = selectedChip.getValue();
        if (key.equals(current)) {
            //Log.d(TAG, "handleTypeChipClick: Unselecting " + key + ", showing all jobs for type: " + type);
            selectedChip.setValue(null);
            resetEducationFilter();
            filterJobs(type, context);
        } else {
            //Log.d(TAG, "handleTypeChipClick: Selecting " + key);
            selectedChip.setValue(key);
            resetEducationFilter();
            filterJobs(type, subType, context);
        }
    }

    public void handleMainTypeChipClick(String type, Context context) {
        String current = selectedChip.getValue();
        if (type.equals(current)) {
            //Log.d(TAG, "handleMainTypeChipClick: Unselecting " + type + ", showing all jobs");
            resetSelectedChip();
            resetEducationFilter();
            showAllJobs();
        } else {
            //Log.d(TAG, "handleMainTypeChipClick: Selecting " + type);
            selectedChip.setValue(type);
            resetEducationFilter();
            filterJobs(type, context);
        }
    }

    private String getCurrentJobType() {
        return currentJobType.getValue();
    }

    public boolean isLoading() {
        return loadingState.getValue() != null && loadingState.getValue();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void resetSelectedChip() {
        //Log.d(TAG, "resetSelectedChip: Resetting selected chip to null");
        selectedChip.setValue(null);
    }

    public void setActiveTab(String tab, Context context) {
        if (!tab.equals(activeTab.getValue())) {
            //Log.d(TAG, "setActiveTab: Setting active tab to: " + tab);
            activeTab.setValue(tab);
            resetEducationFilter();
            resetSelectedChip();
            executor.execute(() -> {
                if ("forYou".equals(tab)) {
                    //Log.d(TAG, "setActiveTab: Filtering for 'forYou' tab");
                    filterJobsByUserEducation(context);
                } else {
                    //Log.d(TAG, "setActiveTab: Showing all jobs for 'all' tab");
                    showAllJobs();
                }
            });
        }
    }

    public void setFilter(Context context, String category, String degree, String postGrad) {
        //Log.d(TAG, "setFilter: Setting education filter - category: " + category + ", degree: " + degree + ", postGrad: " + postGrad);
        filter.setValue(new Filter(category, degree, postGrad));
        selectedChip.setValue("education");
        saveFilter(context);
        filterJobsByEducation();
    }

    public void restoreFilter(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String category = prefs.getString("education", "");
        String degree = prefs.getString("degree", "");
        String postGrad = prefs.getString("postGrad", "");
        //Log.d(TAG, "restoreFilter: Restoring filter - category: " + category + ", degree: " + degree + ", postGrad: " + postGrad);
        filter.setValue(new Filter(category, degree, postGrad));
        if (!category.isEmpty() && !category.equals("Select Education Category") && "forYou".equals(activeTab.getValue())) {
            //Log.d(TAG, "restoreFilter: Applying education filter for 'forYou' tab");
            selectedChip.setValue("education");
            filterJobsByEducation();
        } else {
            //Log.d(TAG, "restoreFilter: Showing all jobs");
            showAllJobs();
        }
    }

    private void saveFilter(Context context) {
        Filter f = filter.getValue();
        if (f != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("education", f.getCategory());
            editor.putString("degree", f.getDegree());
            editor.putString("postGrad", f.getPostGrad());
            editor.apply();
            //Log.d(TAG, "saveFilter: Saved filter - category: " + f.getCategory() + ", degree: " + f.getDegree() + ", postGrad: " + f.getPostGrad());
        }
    }

    public void loadJobs(OkHttpClient client, String endpoint, Context context) {
        if (!allJobs.isEmpty()) {
            executor.execute(() -> {
                //Log.d(TAG, "loadJobs: Jobs already loaded, showing all jobs");
                showAllJobs();
            });
            return;
        }
        currentPage = 1;
        hasMoreJobs = true;
        allJobs.clear();
        jobs.postValue(new ArrayList<>());
        loadingState.postValue(true);
        //Log.d(TAG, "loadJobs: Clearing jobs and loading page 1");
        loadMoreJobs(client, endpoint, context);
    }

    // Convenience methods for specific job types
    public void loadAllGovernmentJobs(OkHttpClient client, String endpoint, Context context) {
        //Log.d(TAG, "loadAllGovernmentJobs: Starting to load government jobs");
        loadAllOfType(client, BuildConfig.JOB_UPDATES_GOVT, "government", context);
    }

    public void loadAllPrivateJobs(OkHttpClient client, String endpoint, Context context) {
        //Log.d(TAG, "loadAllPrivateJobs: Starting to load private jobs");
        loadAllOfType(client, BuildConfig.JOB_UPDATES_PRIVATE, "private", context);
    }

    public void loadAllBankingJobs(OkHttpClient client, String endpoint, Context context) {
        //Log.d(TAG, "loadAllBankingJobs: Starting to load banking jobs");
        loadAllOfType(client, BuildConfig.JOB_UPDATES_BANKING, "banking", context);
    }

    // Method to reload jobs (useful for retry functionality)
    public void reloadJobs(OkHttpClient client, String endpoint, String jobType, Context context) {
        //Log.d(TAG, "reloadJobs: Reloading all jobs for type: " + jobType);
        String reloadEndpoint = BuildConfig.JOB_UPDATES + "/" + jobType;
        if (jobType.equalsIgnoreCase("government")) reloadEndpoint = BuildConfig.JOB_UPDATES_GOVT;
        else if (jobType.equalsIgnoreCase("private")) reloadEndpoint = BuildConfig.JOB_UPDATES_PRIVATE;
        else if (jobType.equalsIgnoreCase("banking")) reloadEndpoint = BuildConfig.JOB_UPDATES_BANKING;
        
        loadAllOfType(client, reloadEndpoint, jobType, context);
    }

    // Debug method to check current job counts
    public void debugJobCounts() {
        //Log.d(TAG, "debugJobCounts: Total jobs in allJobs: " + allJobs.size());
        int bankingCount = 0, governmentCount = 0, privateCount = 0, otherCount = 0;
        for (JobUpdate job : allJobs) {
            String jobType = job.getType() != null ? job.getType() : "null";
            if ("banking".equalsIgnoreCase(jobType)) {
                bankingCount++;
            } else if ("government".equalsIgnoreCase(jobType)) {
                governmentCount++;
            } else if ("private".equalsIgnoreCase(jobType)) {
                privateCount++;
            } else {
                otherCount++;
            }
        }
        //Log.d(TAG, "debugJobCounts: Banking: " + bankingCount + ", Government: " + governmentCount + ", Private: " + privateCount + ", Other: " + otherCount);
    }

    // Load all jobs of a specific type using the corresponding endpoint
    public void loadAllOfType(OkHttpClient client, String endpoint, String jobType, Context context) {
        //Log.d(TAG, "loadAllOfType: Loading all jobs of type " + jobType + " from endpoint " + endpoint);
        allJobs.clear();
        jobs.postValue(new ArrayList<>());
        loadingState.postValue(true);
        setCurrentJobType(jobType);

        // Clear OkHttp cache
        try {
            if (client.cache() != null) {
                client.cache().evictAll();
                //Log.d(TAG, "loadAllOfType: Cleared OkHttp cache");
            }
        } catch (IOException e) {
            Log.e(TAG, "loadAllOfType: Failed to clear cache: " + e.getMessage());
        }

        String url = endpoint + "?page=1&limit=10000";
        //Log.d(TAG, "loadAllOfType: Fetching from URL: " + BASE_URL + url);
        Request request = new Request.Builder()
                .url(BASE_URL + url)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    //Log.d(TAG, "loadAllOfType: Request canceled");
                    return;
                }
                Log.e(TAG, "loadAllOfType: Failed to fetch jobs: " + e.getMessage());
                loadingState.postValue(false);
                errorMessage.postValue("Failed to fetch jobs: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) {
                    //Log.d(TAG, "loadAllOfType: Response canceled");
                    return;
                }
                if (!response.isSuccessful()) {
                    Log.e(TAG, "loadAllOfType: Unexpected response code: " + response.code());
                    loadingState.postValue(false);
                    errorMessage.postValue("Unexpected response: " + response.code());
                    return;
                }

                String responseData = response.body().string();
                //Log.d(TAG, "loadAllOfType: Raw response length: " + responseData.length());
                executor.execute(() -> {
                    try {
                        JsonObject jsonObject = new Gson().fromJson(responseData, JsonObject.class);
                        List<JobUpdate> jobList;
                        if (jsonObject.has("data")) {
                            jobList = new Gson().fromJson(jsonObject.getAsJsonArray("data"),
                                    new TypeToken<List<JobUpdate>>(){}.getType());
                        } else if (jsonObject.has("jobUpdates")) {
                            jobList = new Gson().fromJson(jsonObject.getAsJsonArray("jobUpdates"),
                                    new TypeToken<List<JobUpdate>>(){}.getType());
                        } else {
                            Log.e(TAG, "loadAllOfType: No valid job data found in response");
                            errorMessage.postValue("No valid job data found");
                            loadingState.postValue(false);
                            return;
                        }

                        if (jobList == null || jobList.isEmpty()) {
                            //Log.d(TAG, "loadAllOfType: No jobs returned");
                            loadingState.postValue(false);
                            errorMessage.postValue("No jobs available");
                            return;
                        }

                        List<JobUpdate> typeJobs = new ArrayList<>();
                        for (JobUpdate job : jobList) {
                            job.setImageUrl(buildFullUrl(job.getImageUrl()));
                            job.setIconUrl(buildFullUrl(job.getIconUrl()));
                            typeJobs.add(job);
                            //Log.d(TAG, "loadAllOfType: Added job - ID: " + job.getDocumentId() + ", Type: " + job.getType() + ", SubType: " + job.getSubType());
                        }

                        allJobs.addAll(typeJobs);
                        //Log.d(TAG, "loadAllOfType: Loaded " + typeJobs.size() + " jobs for type " + jobType);
                        loadingState.postValue(false);
                        filterJobs(jobType, context);
                    } catch (Exception e) {
                        Log.e(TAG, "loadAllOfType: Failed to parse jobs: " + e.getMessage());
                        errorMessage.postValue("Failed to parse jobs: " + e.getMessage());
                        loadingState.postValue(false);
                    }
                });
            }
        });
    }

    public void loadMoreJobs(OkHttpClient client, String endpoint, Context context) {
        if (!hasMoreJobs || isLoading()) {
            //Log.d(TAG, "loadMoreJobs: Skipping, hasMoreJobs: " + hasMoreJobs + ", isLoading: " + isLoading());
            return;
        }
        String url = endpoint + "?page=" + currentPage + "&limit=" + PAGE_SIZE;
        //Log.d(TAG, "loadMoreJobs: Fetching jobs from URL: " + BASE_URL + url);
        Request request = new Request.Builder()
                .url(BASE_URL + url)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    //Log.d(TAG, "loadMoreJobs: Request canceled");
                    return;
                }
                Log.e(TAG, "loadMoreJobs: Failed to fetch jobs: " + e.getMessage());
                loadingState.postValue(false);
                errorMessage.postValue("Failed to fetch jobs: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) {
                    //Log.d(TAG, "loadMoreJobs: Response canceled");
                    return;
                }
                if (!response.isSuccessful()) {
                    Log.e(TAG, "loadMoreJobs: Unexpected response code: " + response.code());
                    loadingState.postValue(false);
                    errorMessage.postValue("Unexpected response: " + response.code());
                    return;
                }

                String responseData = response.body().string();
                //Log.d(TAG, "loadMoreJobs: Raw response length: " + responseData.length());
                executor.execute(() -> {
                    try {
                        JsonObject jsonObject = new Gson().fromJson(responseData, JsonObject.class);
                        List<JobUpdate> jobList;
                        if (jsonObject.has("data")) {
                            jobList = new Gson().fromJson(jsonObject.getAsJsonArray("data"),
                                    new TypeToken<List<JobUpdate>>(){}.getType());
                            //Log.d(TAG, "loadMoreJobs: Parsed jobs from 'data' array, count: " + (jobList != null ? jobList.size() : 0));
                        } else if (jsonObject.has("jobUpdates")) {
                            jobList = new Gson().fromJson(jsonObject.getAsJsonArray("jobUpdates"),
                                    new TypeToken<List<JobUpdate>>(){}.getType());
                            //Log.d(TAG, "loadMoreJobs: Parsed jobs from 'jobUpdates' array, count: " + (jobList != null ? jobList.size() : 0));
                        } else {
                            Log.e(TAG, "loadMoreJobs: No valid job data found in response");
                            errorMessage.postValue("No valid job data found");
                            loadingState.postValue(false);
                            return;
                        }

                        if (jobList == null || jobList.isEmpty()) {
                            //Log.d(TAG, "loadMoreJobs: No jobs returned, setting hasMoreJobs to false");
                            hasMoreJobs = false;
                            if (allJobs.isEmpty()) {
                                errorMessage.postValue("No jobs available");
                            }
                            loadingState.postValue(false);
                            return;
                        }

                        List<JobUpdate> newJobs = new ArrayList<>();
                        for (JobUpdate job : jobList) {
                            job.setImageUrl(buildFullUrl(job.getImageUrl()));
                            job.setIconUrl(buildFullUrl(job.getIconUrl()));
                            if (!isJobInList(job.getDocumentId(), allJobs)) {
                                //Log.d(TAG, "loadMoreJobs: Adding job - ID: " + job.getDocumentId() + ", Type: " + job.getType() + ", SubType: " + job.getSubType());
                                newJobs.add(job);
                            } else {
                                //Log.d(TAG, "loadMoreJobs: Skipping duplicate job - ID: " + job.getDocumentId());
                            }
                        }

                        if (!newJobs.isEmpty()) {
                            allJobs.addAll(newJobs);
                            //Log.d(TAG, "loadMoreJobs: Added " + newJobs.size() + " new jobs, total: " + allJobs.size());
                        } else {
                            //Log.d(TAG, "loadMoreJobs: No new jobs added");
                        }

                        if (jobList.size() >= PAGE_SIZE) {
                            currentPage++;
                            //Log.d(TAG, "loadMoreJobs: Continuing to page " + currentPage);
                        } else {
                            hasMoreJobs = false;
                            //Log.d(TAG, "loadMoreJobs: Last page reached");
                        }

                        loadingState.postValue(false);
                        String sel = selectedChip.getValue();
                        if (sel == null) {
                            showAllJobs();
                        } else if (sel.equals("education")) {
                            filterJobsByEducation();
                        } else {
                            String[] parts = sel.split("-");
                            String type = parts[0];
                            String sub = parts.length > 1 ? sel.substring(type.length() + 1) : null;
                            filterJobs(type, sub, context);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadMoreJobs: Failed to parse jobs: " + e.getMessage());
                        errorMessage.postValue("Failed to parse jobs: " + e.getMessage());
                        loadingState.postValue(false);
                    }
                });
            }
        });
    }

    private boolean isJobInList(String jobId, List<JobUpdate> jobs) {
        if (jobId == null) return false;
        for (JobUpdate job : jobs) {
            if (job.getDocumentId() != null && job.getDocumentId().equals(jobId)) {
                return true;
            }
        }
        return false;
    }

    private String buildFullUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        String url = filePath.startsWith("http") ? filePath : BASE_URL + UPLOADS_PATH + filePath;
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        //Log.d(TAG, "buildFullUrl: Built URL: " + url);
        return url;
    }

    public void showAllJobs() {
        executor.execute(() -> {
            //Log.d(TAG, "showAllJobs: Showing all jobs, size: " + allJobs.size());
            for (JobUpdate job : allJobs) {
                //Log.d(TAG, "showAllJobs: Job ID: " + job.getDocumentId() + ", Type: " + job.getType() + ", SubType: " + job.getSubType());
            }
            List<JobUpdate> displayJobs = new ArrayList<>(allJobs);
            jobs.postValue(displayJobs);
            selectedChip.postValue(null);
        });
    }

    private void filterJobs(String type, Context context) {
        executor.execute(() -> filterJobs(type, null, context));
    }

    public void filterJobs(String type, String subType, Context context) {
        executor.execute(() -> {
            List<JobUpdate> displayJobs = new ArrayList<>();
            //Log.d(TAG, "filterJobs: Filtering jobs for type: " + type + ", subType: " + subType + ", total jobs: " + allJobs.size());
            String requestedSub = null;
            if (subType != null) {
                if ("banking".equalsIgnoreCase(type)) {
                    requestedSub = normalizeBankingSubType(subType);
                } else if ("private".equalsIgnoreCase(type)) {
                    requestedSub = normalizePrivateSubType(subType);
                } else if ("government".equalsIgnoreCase(type)) {
                    requestedSub = normalizeGovernmentSubType(subType);
                } else {
                    requestedSub = subType;
                }
            }
            for (JobUpdate job : allJobs) {
                String jobType = job.getType() != null ? job.getType().toLowerCase() : "";
                if (!type.toLowerCase().equals(jobType)) {
                    continue;
                }
                boolean subtypeMatches = true;
                if (subType != null) {
                    String jobSubTypeRaw = job.getSubType() != null ? job.getSubType() : "";
                    String jobSubTypeNorm;
                    if ("banking".equalsIgnoreCase(type)) {
                        jobSubTypeNorm = normalizeBankingSubType(jobSubTypeRaw);
                    } else if ("private".equalsIgnoreCase(type)) {
                        jobSubTypeNorm = normalizePrivateSubType(jobSubTypeRaw);
                    } else if ("government".equalsIgnoreCase(type)) {
                        jobSubTypeNorm = normalizeGovernmentSubType(jobSubTypeRaw);
                    } else {
                        jobSubTypeNorm = jobSubTypeRaw.toLowerCase();
                    }
                    subtypeMatches = jobSubTypeNorm.equals(requestedSub);
                }
                if (subtypeMatches) {
                    displayJobs.add(job);
                    //Log.d(TAG, "filterJobs: Including job - ID: " + job.getDocumentId() + ", Type: " + job.getType() + ", SubType: " + job.getSubType());
                }
            }
            //Log.d(TAG, "filterJobs: Filtered " + displayJobs.size() + " jobs");
            // Sort by newest first
            Collections.sort(displayJobs, (a, b) -> Long.compare(getJobMillis(b), getJobMillis(a)));
            jobs.postValue(displayJobs);
        });
    }

    private long getJobMillis(JobUpdate job) {
        try {
            if (job.getTimestamp() != null) {
                return job.getTimestamp().toDate().getTime();
            }
            String created = job.getCreatedAtString();
            if (created != null && !created.isEmpty()) {
                // New format: "28/10/2025, 4:09:31 pm"
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, h:mm:ss a", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // IST
                Date date = sdf.parse(created);
                if (date != null) {
                    return date.getTime();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse created_at: " + job.getCreatedAtString(), e);
        }
        return 0L; // fallback
    }

    private String normalizeBankingSubType(String value) {
        if (value == null) return "";
        String v = value.trim().toLowerCase();
        if (v.contains("gov")) return "govt-bank";
        if (v.contains("private")) return "private-bank";
        return v;
    }

    private String normalizePrivateSubType(String value) {
        if (value == null) return "";
        String v = value.trim().toLowerCase();
        if (v.contains("work") || v.contains("home")) return "work-home";
        if (v.contains("regular")) return "regular-job";
        return v;
    }

    private String normalizeGovernmentSubType(String value) {
        if (value == null) return "";
        String v = value.trim().toLowerCase();
        if (v.startsWith("maha")) return "Maha";
        if (v.contains("central")) return "Central";
        return value;
    }

    private void filterJobsByEducation() {
        executor.execute(() -> {
            Filter f = filter.getValue();
            if (f == null || f.getCategory().isEmpty() || f.getCategory().equals("Select Education Category")) {
                //Log.d(TAG, "filterJobsByEducation: No valid education filter, showing all jobs");
                showAllJobs();
                return;
            }

            String currentType = getCurrentJobType();
            List<JobUpdate> displayJobs = new ArrayList<>();
            for (JobUpdate job : allJobs) {
                if (currentType != null && !currentType.equalsIgnoreCase(job.getType())) {
                    continue;
                }
                Map<String, List<String>> educationReq = job.getEducationRequirement();
                if (educationReq != null) {
                    List<String> categories = educationReq.get("categories");
                    List<String> bachelors = educationReq.get("bachelors");
                    List<String> postGrads = educationReq.get("masters");
                    boolean categoryMatch = categories != null && categories.contains(f.getCategory());
                    boolean degreeMatch = bachelors == null || bachelors.contains(f.getDegree());
                    boolean postGradMatch = postGrads == null || f.getPostGrad().equals("Select Post Graduation") || f.getPostGrad().equals("None") || postGrads.contains(f.getPostGrad());
                    if (categoryMatch && degreeMatch && postGradMatch) {
                        displayJobs.add(job);
                    }
                }
            }
            //Log.d(TAG, "filterJobsByEducation: Filtered jobs by education, size: " + displayJobs.size());
            jobs.postValue(displayJobs);
        });
    }

    public void filterJobsByUserEducation(Context context) {
        executor.execute(() -> {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String category = prefs.getString("education", "");
            String degree = prefs.getString("degree", "");
            String postGrad = prefs.getString("postGrad", "");
            //Log.d(TAG, "filterJobsByUserEducation: Filtering for user education - category: " + category + ", degree: " + degree + ", postGrad: " + postGrad);

            if (category.isEmpty() || category.equals("Select Education Category")) {
                //Log.d(TAG, "filterJobsByUserEducation: No user education filter, showing all jobs");
                showAllJobs();
                return;
            }

            String currentType = getCurrentJobType();
            List<JobUpdate> displayJobs = new ArrayList<>();
            for (JobUpdate job : allJobs) {
                if (currentType != null && !currentType.equalsIgnoreCase(job.getType())) {
                    continue;
                }
                Map<String, List<String>> educationReq = job.getEducationRequirement();
                if (educationReq != null) {
                    List<String> categories = educationReq.get("categories");
                    List<String> bachelors = educationReq.get("bachelors");
                    List<String> postGrads = educationReq.get("masters");
                    boolean categoryMatch = categories != null && categories.contains(category);
                    boolean degreeMatch = bachelors == null || bachelors.contains(degree);
                    boolean postGradMatch = postGrads == null || postGrad.equals("Select Post Graduation") || postGrad.equals("None") || postGrads.contains(postGrad);
                    if (categoryMatch && degreeMatch && postGradMatch) {
                        displayJobs.add(job);
                    }
                }
            }
            //Log.d(TAG, "filterJobsByUserEducation: Filtered jobs size for 'forYou': " + displayJobs.size());
            jobs.postValue(displayJobs);
        });
    }

    public void resetEducationFilter() {
        //Log.d(TAG, "resetEducationFilter: Resetting education filter");
        filter.setValue(new Filter("", "", ""));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        //Log.d(TAG, "onCleared: Cleaning up ViewModel resources");
        if (currentCall != null) {
            currentCall.cancel();
        }
        executor.shutdown();
    }
}