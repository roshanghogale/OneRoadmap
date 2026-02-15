package com.newsproject.oneroadmap.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.newsproject.oneroadmap.Models.User;

import java.util.HashSet;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user_db";
    private static final String TABLE_NAME = "users";

    // Columns
    private static final String COL_USER_ID          = "userId";
    private static final String COL_USER_AVATAR      = "userAvatar";
    private static final String COL_NAME             = "name";
    private static final String COL_GENDER           = "gender";

    // NEW study-material columns
    private static final String COL_STUDY_GOV        = "study_Government";
    private static final String COL_STUDY_POLICE     = "study_Police___Defence";
    private static final String COL_STUDY_BANK       = "study_Banking";
    private static final String COL_STUDY_SELF       = "study_Self_Improvement";

    private static final String COL_DEGREE           = "degree";
    private static final String COL_TWELFTH          = "twelfth";
    private static final String COL_POST_GRADUATION  = "postGraduation";
    private static final String COL_DISTRICT         = "district";
    private static final String COL_TALUKA           = "taluka";
    private static final String COL_CURRENT_AFFAIRS  = "currentAffairs";
    private static final String COL_JOBS             = "jobs";
    private static final String COL_AGE_GROUP        = "ageGroup";
    private static final String COL_COINS            = "coins";
    private static final String COL_EDUCATION        = "education";

    private static final int DATABASE_VERSION = 7;   // <-- bump

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " +
                COL_USER_AVATAR + " TEXT, " +
                COL_NAME + " TEXT, " +
                COL_GENDER + " TEXT, " +

                COL_STUDY_GOV + " INTEGER, " +
                COL_STUDY_POLICE + " INTEGER, " +
                COL_STUDY_BANK + " INTEGER, " +
                COL_STUDY_SELF + " INTEGER, " +

                COL_DEGREE + " TEXT, " +
                COL_TWELFTH + " TEXT, " +
                COL_POST_GRADUATION + " TEXT, " +
                COL_DISTRICT + " TEXT, " +
                COL_TALUKA + " TEXT, " +
                COL_CURRENT_AFFAIRS + " INTEGER, " +
                COL_JOBS + " INTEGER, " +
                COL_AGE_GROUP + " TEXT, " +
                COL_COINS + " INTEGER DEFAULT 0, " +
                COL_EDUCATION + " TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Keep previous upgrades (coins, education, etc.) …
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_COINS + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            // add missing columns safely
            addMissingColumns(db);
        }
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_EDUCATION + " TEXT"); }
            catch (Exception ignored) {}
        }
        if (oldVersion < 5) {
            // old upsc/mpsc handling – remove them if they exist
            try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN upsc INTEGER"); }
            catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN mpsc INTEGER"); }
            catch (Exception ignored) {}
        }
        if (oldVersion < 6) {
            // convert currentAffairs / jobs to INTEGER (same as before)
            convertBooleanColumnsToInt(db);
        }

        // ---- NEW VERSION 7: add the 4 study columns & drop old upsc/mpsc ----
        if (oldVersion < 7) {
            // Add new columns if missing
            String[] newCols = {COL_STUDY_GOV, COL_STUDY_POLICE, COL_STUDY_BANK, COL_STUDY_SELF};
            for (String col : newCols) {
                try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + col + " INTEGER"); }
                catch (Exception ignored) {}
            }

            // Remove old upsc/mpsc if they exist
            try { db.execSQL("ALTER TABLE " + TABLE_NAME + " DROP COLUMN upsc"); }
            catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_NAME + " DROP COLUMN mpsc"); }
            catch (Exception ignored) {}
        }
    }

    private void addMissingColumns(SQLiteDatabase db) {
        String[] expected = {
                COL_USER_ID, COL_USER_AVATAR, COL_NAME, COL_GENDER,
                COL_STUDY_GOV, COL_STUDY_POLICE, COL_STUDY_BANK, COL_STUDY_SELF,
                COL_DEGREE, COL_TWELFTH, COL_POST_GRADUATION,
                COL_DISTRICT, COL_TALUKA, COL_CURRENT_AFFAIRS,
                COL_JOBS, COL_AGE_GROUP, COL_COINS, COL_EDUCATION
        };
        Set<String> existing = getExistingColumns(db);
        for (String col : expected) {
            if (!existing.contains(col)) {
                String type = (col.contains("study_") || col.equals(COL_CURRENT_AFFAIRS) ||
                        col.equals(COL_JOBS) || col.equals(COL_COINS)) ? "INTEGER" : "TEXT";
                try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + col + " " + type); }
                catch (Exception ignored) {}
            }
        }
    }

    private void convertBooleanColumnsToInt(SQLiteDatabase db) {
        // Same logic you already had for version 6
        try {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO temp_users");
            onCreate(db);
            db.execSQL("INSERT INTO " + TABLE_NAME + " SELECT *, " +
                    "CASE WHEN currentAffairs='हो' THEN 1 ELSE 0 END, " +
                    "CASE WHEN jobs='हो' THEN 1 ELSE 0 END " +
                    "FROM temp_users");
            db.execSQL("DROP TABLE temp_users");
        } catch (Exception ignored) {}
    }

    private Set<String> getExistingColumns(SQLiteDatabase db) {
        Set<String> set = new HashSet<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + TABLE_NAME + ")", null)) {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIdx != -1) set.add(c.getString(nameIdx));
            }
        } catch (Exception ignored) {}
        return set;
    }

    /* ---------------------------------------------------- */
    public User getUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = null;
        User u = null;
        try {
            cur = db.query(TABLE_NAME,
                    new String[]{COL_USER_ID, COL_USER_AVATAR, COL_NAME, COL_GENDER,
                            COL_STUDY_GOV, COL_STUDY_POLICE, COL_STUDY_BANK, COL_STUDY_SELF,
                            COL_DEGREE, COL_TWELFTH, COL_POST_GRADUATION,
                            COL_DISTRICT, COL_TALUKA, COL_CURRENT_AFFAIRS,
                            COL_JOBS, COL_AGE_GROUP, COL_COINS, COL_EDUCATION},
                    COL_USER_ID + "=?", new String[]{userId}, null, null, null);

            if (cur != null && cur.moveToFirst()) {
                u = new User(
                        cur.getString(cur.getColumnIndexOrThrow(COL_USER_ID)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_NAME)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_GENDER)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_USER_AVATAR)),

                        cur.getInt(cur.getColumnIndexOrThrow(COL_STUDY_GOV)) == 1,
                        cur.getInt(cur.getColumnIndexOrThrow(COL_STUDY_POLICE)) == 1,
                        cur.getInt(cur.getColumnIndexOrThrow(COL_STUDY_BANK)) == 1,

                        cur.getString(cur.getColumnIndexOrThrow(COL_DEGREE)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_POST_GRADUATION)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_DISTRICT)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_TALUKA)),
                        cur.getInt(cur.getColumnIndexOrThrow(COL_CURRENT_AFFAIRS)) == 1,
                        cur.getInt(cur.getColumnIndexOrThrow(COL_JOBS)) == 1,
                        cur.getString(cur.getColumnIndexOrThrow(COL_AGE_GROUP)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_EDUCATION)),
                        cur.getString(cur.getColumnIndexOrThrow(COL_TWELFTH))
                );
            }
        } catch (Exception e) {
            Log.e("DB", "getUser error: " + e.getMessage());
        } finally {
            if (cur != null) cur.close();
            db.close();
        }
        return u;
    }

    public void insertUser(User u) throws Exception {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_ID, u.getUserId());
        cv.put(COL_USER_AVATAR, u.getAvatar());
        cv.put(COL_NAME, u.getName());
        cv.put(COL_GENDER, u.getGender());

        cv.put(COL_STUDY_GOV, u.isStudyGovernment() ? 1 : 0);
        cv.put(COL_STUDY_POLICE, u.isStudyPoliceDefence() ? 1 : 0);
        cv.put(COL_STUDY_BANK, u.isStudyBanking() ? 1 : 0);
        cv.put(COL_STUDY_SELF, u.isStudySelfImprovement() ? 1 : 0);

        cv.put(COL_DEGREE, u.getDegree());
        cv.put(COL_TWELFTH, u.getTwelfth());
        cv.put(COL_POST_GRADUATION, u.getPostGraduation());
        cv.put(COL_DISTRICT, u.getDistrict());
        cv.put(COL_TALUKA, u.getTaluka());
        cv.put(COL_CURRENT_AFFAIRS, u.isCurrentAffairs() ? 1 : 0);
        cv.put(COL_JOBS, u.isJobs() ? 1 : 0);
        cv.put(COL_AGE_GROUP, u.getAgeGroup());
        cv.put(COL_COINS, 0);
        cv.put(COL_EDUCATION, u.getEducation());

        long r = db.insertOrThrow(TABLE_NAME, null, cv);
        db.close();
        if (r == -1) throw new Exception("Insert failed");
    }

    public void updateUser(User u) throws Exception {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_AVATAR, u.getAvatar());
        cv.put(COL_NAME, u.getName());
        cv.put(COL_GENDER, u.getGender());

        cv.put(COL_STUDY_GOV, u.isStudyGovernment() ? 1 : 0);
        cv.put(COL_STUDY_POLICE, u.isStudyPoliceDefence() ? 1 : 0);
        cv.put(COL_STUDY_BANK, u.isStudyBanking() ? 1 : 0);
        cv.put(COL_STUDY_SELF, u.isStudySelfImprovement() ? 1 : 0);

        cv.put(COL_DEGREE, u.getDegree());
        cv.put(COL_TWELFTH, u.getTwelfth());
        cv.put(COL_POST_GRADUATION, u.getPostGraduation());
        cv.put(COL_DISTRICT, u.getDistrict());
        cv.put(COL_TALUKA, u.getTaluka());
        cv.put(COL_CURRENT_AFFAIRS, u.isCurrentAffairs() ? 1 : 0);
        cv.put(COL_JOBS, u.isJobs() ? 1 : 0);
        cv.put(COL_AGE_GROUP, u.getAgeGroup());
        cv.put(COL_EDUCATION, u.getEducation());

        int rows = db.update(TABLE_NAME, cv, COL_USER_ID + "=?", new String[]{u.getUserId()});
        db.close();
        if (rows == 0) throw new Exception("User not found for update");
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