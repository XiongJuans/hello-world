package com.htc.android.worldclock.worldclock;

import java.util.TimeZone;

import android.util.Log;

import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.weather.WeatherLocation;

public class CityTime {
    private static final String TAG = "WorldClock.CityTime";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    
    private String mCityId;
    private String mCityName;
    private TimeZone mTimeZone;
    private WeatherLocation mLoc;
    private float mLongitude;
    private float mLatitude;
    private String mCityType;
    
    //weather inforamtion
    private int mWeatherConditionId = -1;
    private String mWeatherInformationString;
    private String mWeatherHighLowTemperatureString;
    private int mWeatherDayTimeInt;
    private int mWeatherNightTimeInt;

    public static int DEFAULT_DAY_TIME = 360;
    public static int DEFAULT_NIGHT_TIME = 1080;
    public static String DEFAULT_DAY_TIME_STRING = "360";
    public static String DEFAULT_NIGHT_TIME_STRING = "1080";

    public void setCityId(String id) {
        
        mCityId = id;
    }

    public String getCityId() {
        
        return mCityId;
    }

    public void setCityName(String city) {
        
        mCityName = city;
    }

    public String getCityName() {
        
        return mCityName;
    }

    public void setTimeZone(TimeZone tz) {
        
        mTimeZone = tz;
    }

    public TimeZone getTimeZone() {
        
        return mTimeZone;
    }

    public void setWeatherLocation(WeatherLocation loc) {
        
        mLoc = loc;
        String longStr = mLoc.getLongitude();
        String latStr = mLoc.getLatitude();
        
        if(longStr != null && longStr.equals("") == false) {
            mLongitude = Float.parseFloat(longStr);
        } else {
            mLongitude = 0;
            if (Global.SECURITY_FLAG) Log.w(TAG, "setWeatherLocation: citycode = " + loc.getCode() + " doesn't find longStr");
        }
        
        if(latStr != null && latStr.equals("") == false) {
            mLatitude = Float.parseFloat(latStr);
        } else {
            mLatitude = 0;
            if (Global.SECURITY_FLAG) Log.w(TAG, "setWeatherLocation: citycode = " + loc.getCode() + " doesn't find latStr = null");
        }
    }

    public void setWeatherConditionId(int id) {
        mWeatherConditionId = id;
    }
    
    public void setWeatherInformation(String info) {
        if(info != null) {
            mWeatherInformationString = info;
        } else {
            mWeatherInformationString = "";
        }
    }
    
    public void setWeatherHighLowTemperature(String HighLowTemp) {
        if(HighLowTemp != null) {
            mWeatherHighLowTemperatureString = HighLowTemp;
        } else {
            mWeatherHighLowTemperatureString = "";
        }
    }

    public void setWeatherDayNightInfo(String dayTimeString , String nightTimeString) {

        try {
            mWeatherDayTimeInt = TimeFormatStringToInt(dayTimeString);
        } catch (Exception e) {
            Log.w(TAG, "Exception e = " + e.toString());
            mWeatherDayTimeInt = DEFAULT_DAY_TIME;
        }
        try {
            mWeatherNightTimeInt = TimeFormatStringToInt(nightTimeString);
        } catch(Exception e) {
            Log.w(TAG, "Exception e = " + e.toString());
            mWeatherNightTimeInt = DEFAULT_NIGHT_TIME;
        }
    }

    public void setWeatherDayNightInt(int dayTimeInt , int nightTimeInt) {
        mWeatherDayTimeInt = dayTimeInt;
        mWeatherNightTimeInt = nightTimeInt;
    }

    public static int TimeFormatStringToInt(String timeString) {

        int dotIndex  = timeString.indexOf(":");
        int spaceIndex;
        int time;

        if(dotIndex == -1) {
            return Integer.parseInt(timeString);
        }

        String hourTime = timeString.substring(0, dotIndex);
        String minuteTime;

        if (DEBUG_FLAG) Log.d(TAG, "TimeFormatStringToInt: timeString = " + timeString);

        if(timeString.contains(" ")) {
            spaceIndex = timeString.indexOf(" ");
            minuteTime = timeString.substring(dotIndex+1, spaceIndex);
        } else {
            minuteTime = timeString.substring(dotIndex+1);
        }

        if ((timeString.contains("PM")) || (timeString.contains("pm")) || (timeString.contains("AM")) || (timeString.contains("am"))) {
            if (((timeString.contains("AM")) || (timeString.contains("am"))) && (hourTime.equals("12"))) {
                // 12:xx AM case, change hour time to 0:xx AM
                hourTime = "0";
            }

            if (((timeString.contains("PM")) || (timeString.contains("pm"))) && (!hourTime.equals("12"))) {
                // 1 - 11:xx PM case
                time = (Integer.parseInt(hourTime)+12)*60 + Integer.parseInt(minuteTime);
            } else {
                // 0 - 11:xx AM and 12:xx PM case
                time = Integer.parseInt(hourTime)*60 + Integer.parseInt(minuteTime);
            }
        } else {
            // 24 hour case
            time = Integer.parseInt(hourTime)*60 + Integer.parseInt(minuteTime);
        }

        return time;
    }

    public WeatherLocation getWeatherLocation() {
        
        return mLoc;
    }

    public float getLongitude() {
        
        return mLongitude;
    }
    
    public float getLatitude() {
        
        return mLatitude;
    }

    public void setCityType(String cityType) {
        
        mCityType = cityType;
    }

    public String getCityType() {
        
        return mCityType;
    }

    public String getLocCode() {
        
        if (mLoc == null) {
            return "";
        }

        return mLoc.getCode();
    }

    public int getWeatherConditionId() {
        return mWeatherConditionId;
    }
    
    
    public String getWeatherInformation() {
        return mWeatherInformationString;
    }
    
    public String getWeatherHighLowTemperature() {
        return mWeatherHighLowTemperatureString;
    }

    public int getWeatherDayTime() {
        return mWeatherDayTimeInt;
    }

    public int getWeatherNightTime() {
        return mWeatherNightTimeInt;
    }
    @Override
    public boolean equals(Object obj) {
        
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        return ((CityTime) obj).getCityName().equals(this.mCityName);
    }
}
