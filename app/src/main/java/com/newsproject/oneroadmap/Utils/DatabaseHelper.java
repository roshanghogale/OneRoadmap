package com.newsproject.oneroadmap.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.newsproject.oneroadmap.Models.User;

import java.util.HashSet;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "user_db";
    private static final String TABLE_NAME = "users";
    private static final String COL_USER_ID = "userId";
    private static final String COL_USER_AVATAR = "userAvatar";
    private static final String COL_NAME = "name";
    private static final String COL_GENDER = "gender";
    private static final String COL_UPSC = "upsc";
    private static final String COL_MPSC = "mpsc";
    private static final String COL_DEGREE = "degree";
    private static final String COL_TWELFTH = "twelfth";
    private static final String COL_POST_GRADUATION = "postGraduation";
    private static final String COL_DISTRICT = "district";
    private static final String COL_TALUKA = "taluka";
    private static final String COL_CURRENT_AFFAIRS = "currentAffairs";
    private static final String COL_JOBS = "jobs";
    private static final String COL_AGE_GROUP = "ageGroup";
    private static final String COL_COINS = "coins";
    private static final String COL_EDUCATION = "education";

    private static final int DATABASE_VERSION = 6; // Incremented due to schema changes

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " +
                COL_USER_AVATAR + " TEXT, " +
                COL_NAME + " TEXT, " +
                COL_GENDER + " TEXT, " +
                COL_UPSC + " INTEGER, " +
                COL_MPSC + " INTEGER, " +
                COL_DEGREE + " TEXT, " +
                COL_TWELFTH + " TEXT, " +
                COL_POST_GRADUATION + " TEXT, " +
                COL_DISTRICT + " TEXT, " +
                COL_TALUKA + " TEXT, " +
                COL_CURRENT_AFFAIRS + " INTEGER, " + // Changed to INTEGER
                COL_JOBS + " INTEGER, " +            // Changed to INTEGER
                COL_AGE_GROUP + " TEXT, " +
                COL_COINS + " INTEGER DEFAULT 0, " +
                COL_EDUCATION + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_COINS + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            String[] expectedColumns = {
                    COL_USER_ID, COL_USER_AVATAR, COL_NAME, COL_GENDER, COL_UPSC, COL_MPSC,
                    COL_DEGREE, COL_TWELFTH, COL_POST_GRADUATION, COL_DISTRICT, COL_TALUKA,
                    COL_CURRENT_AFFAIRS, COL_JOBS, COL_AGE_GROUP, COL_COINS, COL_EDUCATION
            };
            Set<String> existingColumns = getExistingColumns(db);
            for (String column : expectedColumns) {
                if (!existingColumns.contains(column)) {
                    String columnType = column.equals(COL_COINS) || column.equals(COL_UPSC) ||
                            column.equals(COL_MPSC) || column.equals(COL_CURRENT_AFFAIRS) ||
                            column.equals(COL_JOBS) ? "INTEGER" : "TEXT";
                    try {
                        db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + column + " " + columnType);
                    } catch (SQLiteException e) {
                        // Log error but continue
                    }
                }
            }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_EDUCATION + " TEXT");
            } catch (SQLiteException e) {
                // Log error but continue
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_UPSC + " INTEGER");
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_MPSC + " INTEGER");
                db.execSQL("ALTER TABLE " + TABLE_NAME + " DROP COLUMN studyMaterial");
            } catch (SQLiteException e) {
                // Log error but continue
            }
        }
        if (oldVersion < 6) {
            try {
                // Convert currentAffairs and jobs to INTEGER
                db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO temp_users");
                onCreate(db); // Create new table with updated schema
                db.execSQL("INSERT INTO " + TABLE_NAME + " (" +
                        COL_USER_ID + "," + COL_USER_AVATAR + "," + COL_NAME + "," + COL_GENDER + "," +
                        COL_UPSC + "," + COL_MPSC + "," + COL_DEGREE + "," + COL_TWELFTH + "," +
                        COL_POST_GRADUATION + "," + COL_DISTRICT + "," + COL_TALUKA + "," +
                        COL_CURRENT_AFFAIRS + "," + COL_JOBS + "," + COL_AGE_GROUP + "," +
                        COL_COINS + "," + COL_EDUCATION + ") " +
                        "SELECT " +
                        COL_USER_ID + "," + COL_USER_AVATAR + "," + COL_NAME + "," + COL_GENDER + "," +
                        COL_UPSC + "," + COL_MPSC + "," + COL_DEGREE + "," + COL_TWELFTH + "," +
                        COL_POST_GRADUATION + "," + COL_DISTRICT + "," + COL_TALUKA + "," +
                        "CASE WHEN " + COL_CURRENT_AFFAIRS + " = 'हो' THEN 1 ELSE 0 END," +
                        "CASE WHEN " + COL_JOBS + " = 'हो' THEN 1 ELSE 0 END," +
                        COL_AGE_GROUP + "," + COL_COINS + "," + COL_EDUCATION + " " +
                        "FROM temp_users");
                db.execSQL("DROP TABLE temp_users");
            } catch (SQLiteException e) {
                // Log error but continue
            }
        }
    }

    private Set<String> getExistingColumns(SQLiteDatabase db) {
        Set<String> columns = new HashSet<>();
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_NAME + ")", null)) {
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1) {
                        columns.add(cursor.getString(nameIndex));
                    }
                }
            }
        } catch (Exception e) {
            // Handle error silently
        }
        return columns;
    }

    public User getUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        User user = null;

        try {
            cursor = db.query(TABLE_NAME,
                    new String[]{COL_USER_ID, COL_USER_AVATAR, COL_NAME, COL_GENDER, COL_UPSC, COL_MPSC,
                            COL_DEGREE, COL_TWELFTH, COL_POST_GRADUATION, COL_DISTRICT,
                            COL_TALUKA, COL_CURRENT_AFFAIRS, COL_JOBS, COL_AGE_GROUP,
                            COL_COINS, COL_EDUCATION},
                    COL_USER_ID + "=?", new String[]{userId},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                user = new User(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_AVATAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_UPSC)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_MPSC)) == 1,
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DEGREE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_GRADUATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DISTRICT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TALUKA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CURRENT_AFFAIRS)) == 1,
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_JOBS)) == 1,
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_AGE_GROUP)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EDUCATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TWELFTH))
                );
            }
        } catch (SQLiteException e) {
            Log.e("DatabaseHelper", "Error retrieving user: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return user;
    }

    public void insertUser(User user) throws Exception {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_USER_ID, user.getUserId());
            values.put(COL_USER_AVATAR, user.getAvatar());
            values.put(COL_NAME, user.getName());
            values.put(COL_GENDER, user.getGender());
            values.put(COL_UPSC, user.isUpsc() ? 1 : 0);
            values.put(COL_MPSC, user.isMpsc() ? 1 : 0);
            values.put(COL_DEGREE, user.getDegree());
            values.put(COL_TWELFTH, user.getTwelfth());
            values.put(COL_POST_GRADUATION, user.getPostGraduation());
            values.put(COL_DISTRICT, user.getDistrict());
            values.put(COL_TALUKA, user.getTaluka());
            values.put(COL_CURRENT_AFFAIRS, user.isCurrentAffairs() ? 1 : 0);
            values.put(COL_JOBS, user.isJobs() ? 1 : 0);
            values.put(COL_AGE_GROUP, user.getAgeGroup());
            values.put(COL_COINS, 0);
            values.put(COL_EDUCATION, user.getEducation());

            String query = "INSERT INTO " + TABLE_NAME + " (" +
                    COL_USER_ID + "," + COL_USER_AVATAR + "," + COL_NAME + "," + COL_GENDER + "," +
                    COL_UPSC + "," + COL_MPSC + "," + COL_DEGREE + "," + COL_TWELFTH + "," +
                    COL_POST_GRADUATION + "," + COL_DISTRICT + "," + COL_TALUKA + "," +
                    COL_CURRENT_AFFAIRS + "," + COL_JOBS + "," + COL_AGE_GROUP + "," +
                    COL_COINS + "," + COL_EDUCATION + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            String params = "Parameters: [" +
                    user.getUserId() + ", " + user.getAvatar() + ", " + user.getName() + ", " +
                    user.getGender() + ", " + (user.isUpsc() ? 1 : 0) + ", " + (user.isMpsc() ? 1 : 0) + ", " +
                    user.getDegree() + ", " + user.getTwelfth() + ", " + user.getPostGraduation() + ", " +
                    user.getDistrict() + ", " + user.getTaluka() + ", " + (user.isCurrentAffairs() ? 1 : 0) + ", " +
                    (user.isJobs() ? 1 : 0) + ", " + user.getAgeGroup() + ", 0, " + user.getEducation() + "]";

            long result = db.insertOrThrow(TABLE_NAME, null, values);
            if (result == -1) {
                throw new SQLiteException("Failed to insert user: Unknown database error\nQuery: " + query + "\n" + params);
            }
        } catch (SQLiteException e) {
            throw new Exception("SQLite error: " + e.getMessage() + "\nStackTrace: " + getStackTraceAsString(e));
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    public void updateUser(User user) throws Exception {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_USER_AVATAR, user.getAvatar());
            values.put(COL_NAME, user.getName());
            values.put(COL_GENDER, user.getGender());
            values.put(COL_UPSC, user.isUpsc() ? 1 : 0);
            values.put(COL_MPSC, user.isMpsc() ? 1 : 0);
            values.put(COL_DEGREE, user.getDegree());
            values.put(COL_TWELFTH, user.getTwelfth());
            values.put(COL_POST_GRADUATION, user.getPostGraduation());
            values.put(COL_DISTRICT, user.getDistrict());
            values.put(COL_TALUKA, user.getTaluka());
            values.put(COL_CURRENT_AFFAIRS, user.isCurrentAffairs() ? 1 : 0);
            values.put(COL_JOBS, user.isJobs() ? 1 : 0);
            values.put(COL_AGE_GROUP, user.getAgeGroup());
            values.put(COL_EDUCATION, user.getEducation());

            String query = "UPDATE " + TABLE_NAME + " SET " +
                    COL_USER_AVATAR + "=?, " + COL_NAME + "=?, " + COL_GENDER + "=?, " +
                    COL_UPSC + "=?, " + COL_MPSC + "=?, " + COL_DEGREE + "=?, " +
                    COL_TWELFTH + "=?, " + COL_POST_GRADUATION + "=?, " + COL_DISTRICT + "=?, " +
                    COL_TALUKA + "=?, " + COL_CURRENT_AFFAIRS + "=?, " + COL_JOBS + "=?, " +
                    COL_AGE_GROUP + "=?, " + COL_EDUCATION + "=? WHERE " + COL_USER_ID + "=?";
            String params = "Parameters: [" +
                    user.getAvatar() + ", " + user.getName() + ", " + user.getGender() + ", " +
                    (user.isUpsc() ? 1 : 0) + ", " + (user.isMpsc() ? 1 : 0) + ", " +
                    user.getDegree() + ", " + user.getTwelfth() + ", " + user.getPostGraduation() + ", " +
                    user.getDistrict() + ", " + user.getTaluka() + ", " + (user.isCurrentAffairs() ? 1 : 0) + ", " +
                    (user.isJobs() ? 1 : 0) + ", " + user.getAgeGroup() + ", " + user.getEducation() + ", " +
                    user.getUserId() + "]";

            int rowsAffected = db.update(TABLE_NAME, values, COL_USER_ID + "=?", new String[]{user.getUserId()});
            if (rowsAffected == 0) {
                throw new SQLiteException("User not found for update: " + user.getUserId() + "\nQuery: " + query + "\n" + params);
            }
        } catch (SQLiteException e) {
            throw new Exception("SQLite update error: " + e.getMessage() + "\nStackTrace: " + getStackTraceAsString(e));
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    public void updateUserCoins(String userId, int coins) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_COINS, coins);

        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_USER_ID},
                COL_USER_ID + "=?", new String[]{userId}, null, null, null);
        if (cursor.getCount() > 0) {
            db.update(TABLE_NAME, values, COL_USER_ID + "=?", new String[]{userId});
        } else {
            values.put(COL_USER_ID, userId);
            db.insert(TABLE_NAME, null, values);
        }
        cursor.close();
        db.close();
    }

    public int getUserCoins(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_COINS},
                COL_USER_ID + "=?", new String[]{userId}, null, null, null);
        int coins = 0;
        if (cursor.moveToFirst()) {
            coins = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COINS));
        } else {
            updateUserCoins(userId, 0);
        }
        cursor.close();
        db.close();
        return coins;
    }

    public boolean deleteUser(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_NAME, COL_USER_ID + "=?", new String[]{userId});
        db.close();
        return rowsAffected > 0;
    }

    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}