package com.htc.android.worldclock.timer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class TimerUtils {
    private static final String TAG = "WorldClock.TimerUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    static final String STATE = "state";
    static final String EXPIRED_TIME = "expired_time";

    private static final String AUTHORITY = "com.htc.android.worldclock.TimerProvider";

    private static String[] sTimerKeys = new String[] {
        STATE, EXPIRED_TIME
    };

    public static class TimerData {
        public int state = 0; // 0:IDLE_STATE, 1:PLAY_STATE, 2:PAUSE_STATE, 3:TIMEUP_STATE
        public long timeExpired = 0;
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/setting");

    public static void setTimerData(Context context, TimerData data) {
        ContentResolver contentResolver = context.getContentResolver();

        // insert
        ContentValues values = new ContentValues();
        values.put(STATE, data.state);
        values.put(EXPIRED_TIME, data.timeExpired);

        Cursor cursor = contentResolver.query(Uri.withAppendedPath(CONTENT_URI,
            ""), sTimerKeys, null, null, null);

        if (cursor != null) {
            int count = cursor.getCount();

            try {
                if (count > 1) {
                    contentResolver.delete(Uri.withAppendedPath(
                        CONTENT_URI, ""), null, null);
                    contentResolver.insert(Uri.withAppendedPath(
                        CONTENT_URI, ""), values);
                } else if (count == 0) {
                    contentResolver.insert(Uri.withAppendedPath(
                        CONTENT_URI, ""), values);
                } else if (count == 1) {
                    contentResolver.update(ContentUris.withAppendedId(
                        CONTENT_URI, 1), values, null, null);
                }
            } catch (Exception e) {
                Log.w(TAG, "setTimerData: e = " + e.toString());
            } finally {
                if(cursor != null) {
                    if(cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
            }
        }
    }

    public static void getTimerData(Context context, TimerData data) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(Uri.withAppendedPath(CONTENT_URI,
            ""), sTimerKeys, null, null, null);

        if (cursor != null) {
            if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                data.state = cursor.getInt(0);
                data.timeExpired = cursor.getLong(1);
            }
            if(cursor.isClosed() == false) {
                cursor.close();
            }
        }
    }
}
