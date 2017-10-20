package com.htc.android.worldclock.stopwatch;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class StopwatchUtils {
    private static final String TAG = "WorldClock.StopwatchUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String AUTHORITY = "com.htc.android.worldclock.StopwatchProvider";

    public static class StopwatchLapData {
        int lap_id = 0;
        long lap_total_time = 0;
        long lap_time = 0;
        String lap_total_time_str = "";
        String lap_time_str = "";

        public StopwatchLapData(int lap_id, long lap_total_time, long lap_time) {
            this.lap_id = lap_id;
            this.lap_total_time = lap_total_time;
            this.lap_time = lap_time;

            long hourData = lap_total_time / 600;
            long minuteData = (lap_total_time % 600) / 10;
            long secondData = lap_total_time % 600 % 10;
            lap_total_time_str = String.format("%1d%1d:%1d%1d.%1d", hourData / 10, hourData % 10, minuteData / 10, minuteData % 10, secondData);
            hourData = lap_time / 600;
            minuteData = (lap_time % 600) / 10;
            secondData = lap_time % 600 % 10;
            lap_time_str = String.format("%1d%1d:%1d%1d.%1d", hourData / 10, hourData % 10, minuteData / 10, minuteData % 10, secondData);
        }
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/setting");

    public static void AddStopwatchLapData(Context context, StopwatchLapData data) {
        ContentResolver contentResolver = context.getContentResolver();

        // check args
        if (data == null) {
            if (DEBUG_FLAG) Log.d(TAG, "AddStopwatchLapData: No data can be inserted");
            return;
        }

        // insert
        ContentValues values = new ContentValues();
        values.put(StopwatchProvider.LAP_ID, data.lap_id);
        values.put(StopwatchProvider.LAP_TOTAL_TIME, data.lap_total_time);
        values.put(StopwatchProvider.LAP_TIME, data.lap_time);
        if (contentResolver != null) {
            try {
                contentResolver.insert(Uri.withAppendedPath(CONTENT_URI, ""), values);
            } catch (Exception e) {
                Log.w(TAG, "AddStopwatchLapData: e = " + e.toString());
            }
        }
    }

    public static void DeleteStopwatchLapData(Context context) {
        ContentResolver contentResolver = context.getContentResolver();

        if (contentResolver != null) {
            try {
                contentResolver.delete(Uri.withAppendedPath(CONTENT_URI, ""), null, null);

            } catch (Exception e) {
                Log.w(TAG, "DeleteStopwatchLapData: e = " + e.toString());
            }
        }
    }

    public static ArrayList<StopwatchLapData> LoadStopwatchLapData(Context context) {
        Cursor cursor = null;
        ArrayList<StopwatchLapData> rets = new ArrayList<StopwatchLapData>();

        try {
            ContentResolver contentResolver = context.getContentResolver();

            cursor = contentResolver.query(Uri.withAppendedPath(CONTENT_URI,
                ""), StopwatchProvider.sStopwatchKeys, null, null, null);

            if ((cursor != null) && (cursor.getCount() > 0) && cursor.moveToFirst()) {
                do {
                    StopwatchLapData stopwatchLapInfo = cursorToStopwatchInfo(context, cursor);
                    if (stopwatchLapInfo != null) {
                        rets.add(stopwatchLapInfo);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.w(TAG, "LoadStopwatchLapData: e = " + e.toString());
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }
        return rets;
    }

    private static StopwatchLapData cursorToStopwatchInfo(Context context, Cursor stopwatchCursor) {
        StopwatchLapData data = null;
        int lap_id = 0;
        long lap_total_time = 0;
        long lap_time = 0;

        if (stopwatchCursor != null) {
            try {
                lap_id = stopwatchCursor.getInt(stopwatchCursor.getColumnIndex(StopwatchProvider.LAP_ID));
                lap_total_time = stopwatchCursor.getLong(stopwatchCursor.getColumnIndex(StopwatchProvider.LAP_TOTAL_TIME));
                lap_time = stopwatchCursor.getLong(stopwatchCursor.getColumnIndex(StopwatchProvider.LAP_TIME));
            } catch (NumberFormatException e) {
                Log.w(TAG, "cursorToStopwatchInfo: e = " + e.toString());
            }
            data = new StopwatchLapData(lap_id, lap_total_time, lap_time);
        }
        return data;
    }
}
