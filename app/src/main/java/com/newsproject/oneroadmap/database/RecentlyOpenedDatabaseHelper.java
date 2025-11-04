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

public class RecentlyOpenedDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "RecentlyOpened.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "recently_opened";
    private static final String COL_ID = "document_id";
    private static final String COL_DATA = "job_data";
    private static final int MAX_ITEMS = 5;

    private final Gson gson = new Gson();

    public RecentlyOpenedDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_DATA + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int old, int n) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public void addOrUpdateJob(JobUpdate job) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_ID, job.getDocumentId());
            cv.put(COL_DATA, gson.toJson(job));
            db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

            // Keep only 5 latest
            Cursor cursor = db.rawQuery("SELECT " + COL_ID + " FROM " + TABLE + " ORDER BY rowid DESC LIMIT 100", null);
            List<String> ids = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    ids.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (ids.size() > MAX_ITEMS) {
                for (int i = MAX_ITEMS; i < ids.size(); i++) {
                    db.delete(TABLE, COL_ID + "=?", new String[]{ids.get(i)});
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public List<JobUpdate> getAllRecent() {
        List<JobUpdate> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_DATA + " FROM " + TABLE + " ORDER BY rowid DESC LIMIT " + MAX_ITEMS, null);
        if (cursor.moveToFirst()) {
            do {
                String json = cursor.getString(0);
                JobUpdate job = gson.fromJson(json, JobUpdate.class);
                list.add(job);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}