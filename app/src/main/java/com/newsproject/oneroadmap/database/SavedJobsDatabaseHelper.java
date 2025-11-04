// File: app/src/main/java/com/newsproject/oneroadmap/Database/SavedJobsDatabaseHelper.java
package com.newsproject.oneroadmap.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.gson.Gson;
import com.newsproject.oneroadmap.Models.JobUpdate;
import java.util.ArrayList;
import java.util.List;

public class SavedJobsDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SavedJobs.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "saved_jobs";
    private static final String COLUMN_ID = "document_id";
    private static final String COLUMN_DATA = "job_data";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_DATA + " TEXT NOT NULL)";

    private final Gson gson = new Gson();

    public SavedJobsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveJob(JobUpdate job) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, job.getDocumentId());
        cv.put(COLUMN_DATA, gson.toJson(job));
        db.replace(TABLE_NAME, null, cv); // replaces if exists
        db.close();
    }

    public List<JobUpdate> getAllSavedJobs() {
        List<JobUpdate> jobs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, "ROWID DESC");

        if (cursor.moveToFirst()) {
            do {
                String json = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATA));
                JobUpdate job = gson.fromJson(json, JobUpdate.class);
                jobs.add(job);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return jobs;
    }

    public void deleteJob(String documentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{documentId});
        db.close();
    }

    public boolean isJobSaved(String documentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_ID}, COLUMN_ID + "=?",
                new String[]{documentId}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}