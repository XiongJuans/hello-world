package com.htc.android.worldclock.utils;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;

import com.htc.android.worldclock.R;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.weather.WeatherConsts;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherRequest;
import com.htc.lib2.weather.WeatherUtility;

public class WeatherUtils {
    private static final String TAG = "WorldClock.WeatherUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private final static long ONE_DAY_DURATION = 86400000; // 1000*60*60*24

    public static String mWeatherDayTime;
    public static String mWeatherNightTime;

    public static boolean isOverdue(Context context, long lastUpdateTime) {

        long interval = 10800000;
        boolean bAutoSync = WeatherUtility.isSyncAutomatically(context);

        if (bAutoSync) {
            interval = safe_parseInt(Settings.System.getString(context
                .getContentResolver(),
                WeatherConsts.SETTING_KEY_AUTO_SYNC_FREQUENCY));
        }

        long current_time = System.currentTimeMillis();
        if ((current_time >= lastUpdateTime)
            && ((current_time - lastUpdateTime) < interval)) {
            return false;
        } else {
            return true;
        }
    }

    private static int safe_parseInt(String intStr) {
        int iRet = 0;
        try {
            iRet = Integer.parseInt(intStr);
        } catch (Exception e) {
            Log.w(TAG, "safe_parseInt: e = " + e.toString());
            iRet = 0;
        }
        return iRet;
    }

    public static int getDayDiff(String startDate, String timezoneId) {
        Time tmNow = getToday(timezoneId);
        Time tmStart = getDate(startDate, timezoneId);
        return dayDiff(tmNow, tmStart);
    }

    private static Time getToday(String timeZoneId) {
        Time now = new Time();
        now.setToNow();
        if (timeZoneId != null) {
            now.switchTimezone(timeZoneId);
        }
        now.hour = 0;
        now.minute = 0;
        now.second = 0;
        return now;
    }

    public static Time getDate(String date, String timezoneId) {
        Time tm = new Time();
        int y = 1900;
        int m = 1;
        int d = 1;
        ArrayList<String> keywords = new ArrayList<String>();
        for (String k : date.split("/")) {
            if (!k.equals("")) {
                keywords.add(k);
            }
        }
        try {
            if (keywords.size() > 0) {
                m = Integer.parseInt(keywords.get(0));
            }
            if (keywords.size() > 1) {
                d = Integer.parseInt(keywords.get(1));
            }
            if (keywords.size() > 2) {
                y = Integer.parseInt(keywords.get(2));
            }
        } catch (Exception e) {
            Log.w(TAG, "getDate: e = " + e.toString());
        }
        keywords.clear();
        if (timezoneId != null) {
            tm.switchTimezone(timezoneId);
        }
        tm.set(d, m - 1, y);
        return tm;
    }

    public static int dayDiff(Time from, Time to) {
        long tTo = to.toMillis(false);
        long tFrom = from.toMillis(false);
        int daysBetween = (int) ((tFrom - tTo) / (ONE_DAY_DURATION));
        return daysBetween;
    }

    public static void setLongLatitude(Context context, WeatherLocation w) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = WeatherUtility.getLocationListByCode(cr, w.getCode());
            if ((cursor != null) && (cursor.getCount() > 0) && cursor.moveToFirst()) {
                WeatherLocation newloc = WeatherUtility.CursorToWeatherLocation(cursor);
                w.setLongitude(newloc.getLongitude());
                w.setLatitude(newloc.getLatitude());
            }
        } catch (Exception e) {
            Log.w(TAG, "setLongLatitude: e = " + e.toString());
            w.setLongitude("0");
            w.setLatitude("0");
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }
    }

    public static boolean getWeatherDataByLongLatitude(Context context, String longitude, String latitude, String timeZone) {

        // initial need to update ..
        WeatherRequest req = WeatherRequest.generateWeatherRequestForLatitude(latitude, longitude);
        Bundle data = null;

        try {
            data = WeatherRequest.request(context, req);
            if(data == null) {
            	Log.w(TAG, "getWeatherDataByLongLatitude: data = " + null);
            	return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "getWeatherDataByLongLatitude: e = " + e.toString());
            return false;
        }

        Bundle bundle = data.getParcelable(WeatherConsts.KEY_OUT_CURWEATHER_DATA);
        if ((bundle == null) || (bundle.getInt(WeatherConsts.KEY_OUT_CURR_COND_ID, 0) == 0)) {
            return false;
        }

        updateWeatherData(context, bundle, timeZone);

        return true;
    }

    public static boolean getWeatherDataByCurrentLocation(Context context, String timeZone) {

        // initial need to update ..
    	WeatherRequest req = WeatherRequest.generateWeatherRequestForCurrentLocation();
        Bundle data = null;

        try {
            data = WeatherRequest.request(context, req);
            if(data == null) {
            	Log.w(TAG, "getWeatherDataByLongLatitude: data = " + null);
            	return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "getWeatherDataByCurrentLocation: e = " + e.toString());
            return false;
        }
        
        Bundle bundle = data.getParcelable(WeatherConsts.KEY_OUT_CURWEATHER_DATA);

        if ((bundle == null) || (bundle.getInt(WeatherConsts.KEY_OUT_CURR_COND_ID, 0) == 0)) {
            return false;
        }

        updateWeatherData(context, bundle, timeZone);

        return true;
    }

    public static boolean getWeatherDataByLocationCode(Context context, String locCode, String timeZone) {

        // initial need to update ..
    	 WeatherRequest req = WeatherRequest.generateWeatherRequestForLocCode(locCode);
        Bundle data = null;

        try {
            data = WeatherRequest.request(context, req);
            if(data == null) {
            	Log.w(TAG, "getWeatherDataByLongLatitude: data = " + null);
            	return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "getWeatherDataByLocationCode: e = " + e.toString());
            return false;
        }
        
        Bundle bundle = data.getParcelable(WeatherConsts.KEY_OUT_CURWEATHER_DATA);

        if ((bundle == null) || (bundle.getInt(WeatherConsts.KEY_OUT_CURR_COND_ID, 0) == 0)) {
            return false;
        }

        updateWeatherData(context, bundle, timeZone);

        return true;
    }

    private static void updateWeatherData(Context context, Bundle data, String timeZone) {
        mWeatherDayTime = data.getString(WeatherConsts.KEY_OUT_SUNRISE, "");
        mWeatherNightTime = data.getString(WeatherConsts.KEY_OUT_SUNSET, "");
    }
}
