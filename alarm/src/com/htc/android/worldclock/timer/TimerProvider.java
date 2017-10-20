package com.htc.android.worldclock.timer;

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

public class TimerProvider extends ContentProvider {
    private static final String TAG = "WorldClock.TimerProvider";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    // Data base name
    private static final String DATABASE_NAME = "timer.db";
    // Data base version
    private static final int DATABASE_VERSION = 1;
    // Table name
    private static final String TIMER_TABLE_NAME = "timer";
    // Table column primary id
    public static final String _ID = "_id";

    private static final String AUTHORITY = "com.htc.android.worldclock.TimerProvider";
    static final String STATE = "state";
    static final String EXPIRED_TIME = "expired_time";

    private static String[] sTimerKeys = new String[] {
        STATE, EXPIRED_TIME
    };

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/setting");

    private DbHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if (DEBUG_FLAG) Log.d(TAG, "delete");
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        count = db.delete(TIMER_TABLE_NAME, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        if (DEBUG_FLAG) Log.d(TAG, "getType");
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (DEBUG_FLAG) Log.d(TAG, "insert");
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long id = db.insert(TIMER_TABLE_NAME, null, initialValues);
        Uri itemUri = id > -1 ? ContentUris.withAppendedId(CONTENT_URI, id) : null;
        getContext().getContentResolver().notifyChange(itemUri, null);
        return itemUri;
    }

    @Override
    public boolean onCreate() {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        mOpenHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {
        if (DEBUG_FLAG) Log.d(TAG, "query");

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor c = db.query(TIMER_TABLE_NAME, projection, selection,
            selectionArgs, null, null, sortOrder);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (DEBUG_FLAG) Log.d(TAG, "update");
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        count = db.update(TIMER_TABLE_NAME, values, where, whereArgs);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
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
            if (DEBUG_FLAG) Log.d(TAG, "DbHelper.onCreate: create Timer provider default database");

            String sql = "CREATE TABLE " + TIMER_TABLE_NAME + " (" + _ID
                + " INTEGER PRIMARY KEY,";
            StringBuffer sb = new StringBuffer(sql);
            for (int i = 0; i < sTimerKeys.length; i++) {
                if (i == (sTimerKeys.length - 1)) {
                    sb.append(sTimerKeys[i])
                    .append(" INTERGER");
                } else {
                    sb.append(sTimerKeys[i])
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