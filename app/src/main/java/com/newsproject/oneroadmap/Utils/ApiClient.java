// File: com/newsproject/oneroadmap/Utils/ApiClient.java
package com.newsproject.oneroadmap.Utils;

import okhttp3.*;
import java.io.IOException;

public class ApiClient {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static ApiClient instance;
    public final OkHttpClient client;

    private ApiClient() {
        client = new OkHttpClient.Builder()
                .build();
    }

    // --------------------- Sliders (new) ---------------------
    public void getAllSliders(Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.SLIDERS)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) instance = new ApiClient();
        return instance;
    }

    // --------------------- Users (existing) ---------------------
    public void saveUser(String json, Callback callback) {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.USERS_SAVE)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getUser(String identifier, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.USERS + identifier)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteUser(String id, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.USERS + id)
                .delete()
                .build();
        client.newCall(request).enqueue(callback);
    }

    // --------------------- Queries (new) ---------------------
    public void getQueries(Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.QUERIES)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getQueriesByUser(String userId, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.QUERIES_USER + userId)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getQuery(String id, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.QUERIES + "/" + id)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void createQuery(String json, Callback callback) {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.QUERIES)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    // New unified save-or-update (POST /api/queries) using userId as unique key
    public void saveOrUpdateQuery(String json, Callback callback) {
        createQuery(json, callback);
    }

    public void updateQuery(String id, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.QUERIES + "/" + id)
                .put(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteQuery(String id, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + BuildConfig.QUERIES + "/" + id)
                .delete()
                .build();
        client.newCall(request).enqueue(callback);
    }
}