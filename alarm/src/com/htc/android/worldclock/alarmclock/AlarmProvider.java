/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.htc.android.worldclock.alarmclock;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class AlarmProvider extends ContentProvider {
    private static final String TAG = "WorldClock.AlarmProvider";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private SQLiteOpenHelper mOpenHelper;

    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final int TIMEZONES = 3;
    private static final int ALARMS_ENABLE_BULK = 4;
    private static final int ALARMS_SNOOZE_BULK = 5;
    private static final UriMatcher sURLMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);
    private SQLiteDatabase mTimeZonedb;

    static {
        sURLMatcher.addURI("com.htc.android.alarmclock", "alarm", ALARMS);
        sURLMatcher.addURI("com.htc.android.alarmclock", "alarm/#", ALARMS_ID);
        sURLMatcher.addURI("com.htc.android.alarmclock", "timezone", TIMEZONES);
        sURLMatcher.addURI("com.htc.android.alarmclock", "alarm_enable_bulk", ALARMS_ENABLE_BULK);
        sURLMatcher.addURI("com.htc.android.alarmclock", "alarm_snooze_bulk", ALARMS_SNOOZE_BULK);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "alarms.db";
        private static final int DATABASE_VERSION = 7;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            
            try {
                db.execSQL("CREATE TABLE alarms (" +
                        "_id INTEGER PRIMARY KEY," +
                        "hour INTEGER, " +
                        "minutes INTEGER, " +
                        "daysofweek INTEGER, " +
                        "alarmtime INTEGER, " +
                        "enabled INTEGER, " +
                        "vibrate INTEGER, " +
                        "message TEXT, " +
                        "alert TEXT, " +
                        "snoozed INTEGER, " +
                        "offalarm INTEGER, " +
                        "repeat_type INTEGER);");
            } catch (SQLException e) {
                Log.w(TAG, "onCreate: create table: e = " + e.toString());
            }
            // insert default alarms
            String insertMe = "INSERT INTO alarms " +
                    "(hour, minutes, daysofweek, alarmtime, enabled, vibrate, message, alert, snoozed, offalarm, repeat_type) " +
                    "VALUES ";
            try {
                db.execSQL(insertMe + "(6, 0, 31, 0, 0, 1, '', '" + Settings.System.DEFAULT_ALARM_ALERT_URI.toString() + "', 0, 0, 0);");
                db.execSQL(insertMe + "(7, 0, 31, 0, 0, 1, '', '" + Settings.System.DEFAULT_ALARM_ALERT_URI.toString() + "', 0, 0, 0);");
                db.execSQL(insertMe + "(8, 0, 31, 0, 0, 1, '', '" + Settings.System.DEFAULT_ALARM_ALERT_URI.toString() + "', 0, 0, 0);");
            } catch (SQLException e) {
                Log.w(TAG, "onCreate: insert data: e = " + e.toString());
            }

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            if(DEBUG_FLAG) Log.d(TAG, "DatabaseHelper.onUpgrade: from version " + oldVersion + " to " + currentVersion);
            try {
                if (oldVersion < 6) {
                    db.execSQL("ALTER TABLE alarms ADD offalarm INTEGER");
                    db.execSQL("UPDATE alarms SET offalarm='0'");
                }
                if (oldVersion < 7) {
                    db.execSQL("ALTER TABLE alarms ADD repeat_type INTEGER");
                    db.execSQL("UPDATE alarms SET repeat_type='0'");
                }
            } catch (SQLException e) {
                Log.w(TAG, "onUpgrade: add offalarm: e = " + e.toString());
            }
        }
        
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if(DEBUG_FLAG) Log.d(TAG, "DatabaseHelper.onDowngrade: from version " + oldVersion + " to " + newVersion);
        }
    }

    public AlarmProvider() {
        
    }

    @Override
    public boolean onCreate() {
        
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        SQLiteDatabase db;

        // Generate the body of the query
        int match = sURLMatcher.match(url);
        switch (match) {
        case ALARMS:
            db = mOpenHelper.getReadableDatabase();
            qb.setTables("alarms");
            break;
        case ALARMS_ID:
            db = mOpenHelper.getReadableDatabase();
            qb.setTables("alarms");
            qb.appendWhere("_id=");
            qb.appendWhere(url.getPathSegments().get(1));
            break;
        case TIMEZONES:
            if ((mTimeZonedb == null) || !mTimeZonedb.isOpen()) {
                mTimeZonedb = SQLiteDatabase.openDatabase("/system/etc/timezones.db", null, SQLiteDatabase.OPEN_READONLY);
            }
            if (mTimeZonedb == null) {
                return null;
            } else {
                return mTimeZonedb.query("timezone", projectionIn, selection, selectionArgs, null, null, sort);
            }
        default:
            throw new IllegalArgumentException("Unknown URL " + url);
        }

        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                              null, null, sort);

        if (ret == null) {
            Log.w(TAG, "query: failed");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), url);
        }

        return ret;
    }

    @Override
    public String getType(Uri url) {
        
        int match = sURLMatcher.match(url);
        switch (match) {
        case ALARMS:
            return "vnd.android.cursor.dir/alarms";
        case ALARMS_ID:
            return "vnd.android.cursor.item/alarms";
        case ALARMS_ENABLE_BULK:
            return "Bulk to enable alarms";
        case ALARMS_SNOOZE_BULK:
            return "Bulk to snooze alarms";
        default:
            throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        
        int count = -1;
        long rowId = 0;
        String tableName;
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = null;

        try {
            db = mOpenHelper.getWritableDatabase();
        } catch (SQLiteDatabaseCorruptException e) {
            e.printStackTrace();
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (db == null) {
            return -1;
        }

        switch (match) {
        case ALARMS_ID: {
            String segment = url.getPathSegments().get(1);
            rowId = Long.parseLong(segment);
            tableName = "alarms";
            try {
                count = db.update(tableName, values, "_id=" + rowId, null);
                if(DEBUG_FLAG) Log.d(TAG, "update rowId: " + rowId + " url " + url);
                getContext().getContentResolver().notifyChange(url, null);
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
            break;
        }

        case ALARMS_ENABLE_BULK: {
            tableName = "alarms";
            count = bulkEnableAlarms(url, values, where, tableName);
            break;
        }

        case ALARMS_SNOOZE_BULK: {
            tableName = "alarms";
            count = bulkSnoozeAlarms(url, values, where, tableName);
            break;
        }
        default: {
            tableName = null;
            try {
                count = db.update(tableName, values, "_id=" + rowId, null);
                if(DEBUG_FLAG) Log.d(TAG, "update rowId: " + rowId + " url " + url);
                getContext().getContentResolver().notifyChange(url, null);
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
        }
        return count;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private int bulkEnableAlarms(Uri uri, ContentValues values, String userWhere, String table) {
        
        ArrayList<String> enables = (ArrayList<String>)values.get(AlarmUtils.AlarmColumns.ENABLED);
        ArrayList<String> alarm_times = (ArrayList<String>)values.get(AlarmUtils.AlarmColumns.ALARM_TIME);
        ArrayList<String> ids = (ArrayList<String>)values.get(AlarmUtils.AlarmColumns._ID);

        int size = ids.size();

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        int sum = 0;

        try {
            ContentValues contentValues = new ContentValues();

            for (int i = 0; i < size; i++) {
                String id = ids.get(i);
                String enable = enables.get(i);
                String alarm_time = alarm_times.get(i);

                if (enable.equals("-1") == false) {
                    contentValues.put(AlarmUtils.AlarmColumns.ENABLED, Integer.parseInt(enable));
                }

                if (alarm_time.equals("-1") == false) {
                    contentValues.put(AlarmUtils.AlarmColumns.ALARM_TIME, Long.parseLong(alarm_time));
                }

                if(DEBUG_FLAG) Log.d(TAG, "bulkEnableAlarms: id  = " + id);
                if(DEBUG_FLAG) Log.d(TAG, "bulkEnableAlarms: enable  = " + enable);
                if(DEBUG_FLAG) Log.d(TAG, "bulkEnableAlarms: alarm_time  = " + alarm_time);

                int numUpdated = db.update(table, contentValues, "_id=" + id, null);

                if (numUpdated == 0) {
                    Log.w(TAG, "bulkEnableAlarms: update failed");

                } else {
                    sum += numUpdated;
                }

                // yield the lock if anyone else is trying to
                // perform a db operation here.
                db.yieldIfContended();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        
        if(DEBUG_FLAG) Log.d(TAG, "bulkEnableAlarms: " + sum + " entries updated");
        return sum;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private int bulkSnoozeAlarms(Uri uri, ContentValues values, String userWhere, String table) {
        
        ArrayList<String> ids = (ArrayList<String>)values.get(AlarmUtils.AlarmColumns._ID);
        ids.size();

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        int sum = 0;

        try {
            ContentValues contentValues = new ContentValues();

            // for (int i=0; i<size; i++) {
            // String id = ids.get(i);
            for (String id : ids) {
                contentValues.put(AlarmUtils.AlarmColumns.SNOOZED, 0);
                int numUpdated = db.update(table, contentValues, "_id=" + id, null);

                if (numUpdated == 0) {
                    Log.w(TAG, "bulkSnoozeAlarms: update failed");
                } else {
                    sum += numUpdated;
                }

                // yield the lock if anyone else is trying to
                // perform a db operation here.
                db.yieldIfContended();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        
        if(DEBUG_FLAG) Log.d(TAG, "bulkSnoozeAlarms: " + sum + " entries updated");
        return sum;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        
        int match = sURLMatcher.match(url);
        ContentValues values = null;
        String tableName;
        switch (match) {
        case ALARMS:
            if (initialValues != null) {
                values = new ContentValues(initialValues);
            } else {
                values = new ContentValues();
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.HOUR)) {
                values.put(AlarmUtils.AlarmColumns.HOUR, 0);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.MINUTES)) {
                values.put(AlarmUtils.AlarmColumns.MINUTES, 0);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.DAYS_OF_WEEK)) {
                values.put(AlarmUtils.AlarmColumns.DAYS_OF_WEEK, 0);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.ALARM_TIME)) {
                values.put(AlarmUtils.AlarmColumns.ALARM_TIME, 0);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.ENABLED)) {
                values.put(AlarmUtils.AlarmColumns.ENABLED, 0);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.VIBRATE)) {
                values.put(AlarmUtils.AlarmColumns.VIBRATE, 1);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.MESSAGE)) {
                values.put(AlarmUtils.AlarmColumns.MESSAGE, "");
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.ALERT)) {
                values.put(AlarmUtils.AlarmColumns.ALERT, "");
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.SNOOZED)) {
                values.put(AlarmUtils.AlarmColumns.SNOOZED, 0);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.OFFALARM)) {
                values.put(AlarmUtils.AlarmColumns.OFFALARM, 0);
            }

            if (!values.containsKey(AlarmUtils.AlarmColumns.REPEAT_TYPE)) {
                values.put(AlarmUtils.AlarmColumns.REPEAT_TYPE, 0);
            }

            tableName = "alarms";
            break;
        default:
            throw new IllegalArgumentException("Cannot insert into URL: " + url);
        }

        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
        } catch (SQLiteDatabaseCorruptException e) {
            e.printStackTrace();
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (db == null) {
            return null;
        }

        long rowId = -1;
        try {
            rowId = db.insert(tableName, AlarmUtils.AlarmColumns.MESSAGE, values);
        } catch (SQLiteFullException e) {
            e.printStackTrace();
            return null;
        } catch (SQLiteException e) {
            e.printStackTrace();
            return null;
        }

        if (rowId < 0) {
            // throw new SQLException("Failed to insert row into " + url);
            return null;
        }
        if(DEBUG_FLAG) Log.d(TAG, "insert: Added alarm rowId = " + rowId);
        Uri newUrl = null;
        if (match == ALARMS) {
            newUrl = ContentUris.withAppendedId(AlarmUtils.AlarmColumns.CONTENT_URI, rowId);
        }
        if (newUrl != null) {
            getContext().getContentResolver().notifyChange(newUrl, null);
        }

        return newUrl;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        
        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
        } catch (SQLiteDatabaseCorruptException e) {
            e.printStackTrace();
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (db == null) {
            return 0;
        }

        int count = -1;
        String tableName;
        String segment = null;

        switch (sURLMatcher.match(url)) {
        case ALARMS:
            tableName = "alarms";
            break;
        case ALARMS_ID:
            segment = url.getPathSegments().get(1);
            if (TextUtils.isEmpty(where)) {
                where = "_id=" + segment;
            } else {
                where = "_id=" + segment + " AND (" + where + ")";
            }
            tableName = "alarms";
            break;
        default:
            throw new IllegalArgumentException("Cannot delete from URL: " + url);
        }

        try {
            count = db.delete(tableName, where, whereArgs);
            getContext().getContentResolver().notifyChange(url, null);
        } catch (SQLiteException e) {
            return -1;
        }

        return count;
    }
}
