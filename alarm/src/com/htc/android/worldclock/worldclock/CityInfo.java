package com.htc.android.worldclock.worldclock;

import java.util.ArrayList;

import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class CityInfo {
    private static final String TAG = "WorldClock.CityInfo";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    
    public static class CityColumns implements BaseColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://com.htc.android.alarmclock/worldclock");

        public static final String _ID = "_id";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "seq ASC";

        /**
         * City sequence
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String CITY_SEQ = "seq";

        /**
         * City id
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CITY_ID = "cityId";

        /**
         * City name (+ state, country)
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CITY_NAME = "cityName";

        /**
         * City name (only city)
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CITY_NAME_SHORT = "cityNameShort";

        static final String[] CITY_QUERY_COLUMNS = {
            _ID, CITY_SEQ, CITY_ID, CITY_NAME, CITY_NAME_SHORT };

        /**
         * These save calls to cursor.getColumnIndexOrThrow()
         * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
         */
        public static final int CITY_INDEX = 0;
        public static final int CITY_SEQ_INDEX = 1;
        public static final int CITY_ID_INDEX = 2;
        public static final int CITY_NAME_INDEX = 3;
        public static final int CITY_NAME_SHORT_INDEX = 4;
    }

    public synchronized static Uri insertCity(Context context, int seq, String id, String name, String shortName) {
        
        ContentValues values = new ContentValues(4);
        ContentResolver resolver = context.getContentResolver();
        values.put(CityColumns.CITY_SEQ, seq);
        values.put(CityColumns.CITY_ID, id);
        values.put(CityColumns.CITY_NAME, name);
        values.put(CityColumns.CITY_NAME_SHORT, shortName);
        Uri uri = resolver.insert(CityColumns.CONTENT_URI, values);
        return uri;
    }

    public synchronized static Cursor getCityCursor(
        ContentResolver contentResolver) {
        
        return contentResolver.query(
            CityColumns.CONTENT_URI, CityColumns.CITY_QUERY_COLUMNS,
            null, null, CityColumns.DEFAULT_SORT_ORDER);
    }

    public synchronized static int updateCitySeq(Context context, ArrayList<CityTime> myList) {
        
        String cityName;
        int id;
        int seq;
        int size = myList.size();
        boolean updated = false;
        ContentResolver resolver = context.getContentResolver();
        Cursor cur = getCityCursor(resolver);
        if (cur != null) {
            if (cur.moveToFirst()) {
                do {
                    updated = false;
                    id = cur.getInt(CityColumns.CITY_INDEX);
                    seq = cur.getInt(CityColumns.CITY_SEQ_INDEX);
                    cityName = cur.getString(CityColumns.CITY_NAME_INDEX);
                    for (int i = 0; i < size; i++) {
                        if (myList.get(i).getCityName().equals(cityName)) {
                            if (i != seq) { // need to update
                                ContentValues values = new ContentValues(1);
                                values.put(CityColumns.CITY_SEQ, i);
                                resolver.update(ContentUris.withAppendedId(CityColumns.CONTENT_URI, id),
                                    values, null, null);
                            }
                            updated = true;
                            break;
                        }
                    }
                    if (updated == false) {
                        if (Global.SECURITY_FLAG) Log.w(TAG, "updateCitySeq: Error: cannot find city " + cityName);
                    }
                } while (cur.moveToNext());
            }
            if(cur.isClosed() == false) {
                cur.close();
            }
            return 0;
        } else {
            return -1;
        }
    }

    public synchronized static int deleteCity(Context context, String where, String[] whereArgs) {
        
        ContentResolver resolver = context.getContentResolver();
        return resolver.delete(CityColumns.CONTENT_URI, where, whereArgs);
    }

    public synchronized static int queryCount(Context context, String where, String[] whereArgs) {
        
        int count = -1;
        ContentResolver resolver = context.getContentResolver();
        Cursor cur = resolver.query(CityColumns.CONTENT_URI, new String[] { "count(*)" }, where,
            whereArgs, null);
        if (cur != null) {
            if (cur.moveToNext()) {
                count = cur.getInt(0);
            }
            if(cur.isClosed() == false) {
                cur.close();
            }
        }
        return count;

    }

    public static class LocationColumns implements BaseColumns {

        public static final String DEFAULT_SORT_ORDER = "name ASC";

        /**
         * These save calls to cursor.getColumnIndexOrThrow()
         * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://com.htc.android.alarmclock/timezone");

        public static final String TIMEZONEID = "timezoneId";
        public static final String EN = "en";
        public static final String FS = "fs";
        public static final String DE = "de";
        public static final String ES = "es";
        public static final String FR = "fr";
        public static final String IT = "it";
        public static final String JA = "ja";
        public static final String KO = "ko";
        public static final String NL = "nl";
        public static final String NO = "no";
        public static final String PL = "pl";
        public static final String RU = "ru";
        public static final String ZH = "zh";
        public static final String ZHTW = "zhTW";
        public static final String CODE = "code";

    }
}
