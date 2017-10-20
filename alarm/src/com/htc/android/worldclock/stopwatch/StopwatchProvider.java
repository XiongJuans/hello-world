package com.htc.android.worldclock.stopwatch;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class StopwatchProvider extends ContentProvider {
    private static final String TAG = "WorldClock.StopwatchProvider";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    // Data base name
    private static final String DATABASE_NAME = "stopwatch.db";
    // Data base version
    private static final int DATABASE_VERSION = 1;
    // Table name
    private static final String STOPWATCH_TABLE_NAME = "stopwatch";
    // Table column primary id
    public static final String _ID = "_id";

    private static final String AUTHORITY = "com.htc.android.worldclock.StopwatchProvider";
    static final String LAP_ID = "lap_id";
    static final String LAP_TOTAL_TIME = "lap_total_time";
    static final String LAP_TIME = "lap_time";

    public static String[] sStopwatchKeys = new String[] {
        LAP_ID, LAP_TOTAL_TIME, LAP_TIME
    };

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/setting");

    private DbHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long id = db.insert(STOPWATCH_TABLE_NAME, null, initialValues);
        Uri itemUri = id > -1 ? ContentUris.withAppendedId(CONTENT_URI, id)
            : null;
        getContext().getContentResolver().notifyChange(itemUri, null);
        return itemUri;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        count = db.delete(STOPWATCH_TABLE_NAME, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor c = db.query(STOPWATCH_TABLE_NAME, projection, selection,
            selectionArgs, null, null, sortOrder);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        count = db.update(STOPWATCH_TABLE_NAME, values, where, whereArgs);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        if (DEBUG_FLAG) Log.d(TAG, "getType");
        return null;
    }

    /**
     * The data base helper for launcher settings
     */
    private static class DbHelper extends SQLiteOpenHelper {
        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (DEBUG_FLAG) Log.d(TAG, "DbHelper.onCreate: create Stopwatch provider default database");

            String sql = "CREATE TABLE " + STOPWATCH_TABLE_NAME + " (" + _ID
                + " INTEGER PRIMARY KEY,";
            StringBuffer sb = new StringBuffer(sql);
            for (int i = 0; i < sStopwatchKeys.length; i++) {
                if (i == (sStopwatchKeys.length - 1)) {
                    sb.append(sStopwatchKeys[i])
                        .append(" INTERGER");
                } else {
                    sb.append(sStopwatchKeys[i])
                    .append(" INTERGER,");
                }
            }
            sb.append(");");
            if (DEBUG_FLAG) Log.d(TAG, "DbHelper.onCreate: sql>" + sb.toString());

            db.execSQL(sb.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (DEBUG_FLAG) Log.d(TAG, "DbHelper.onUpgrade: upgrade LauncherSetting provider");
            db.execSQL("DROP TABLE IF EXISTS settings");
            onCreate(db);
        }
    }
}
