package com.htc.android.worldclock.utils;

import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;
import android.util.Log;

import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.stopwatch.Stopwatch;
import com.htc.android.worldclock.timer.Timer;
import com.htc.android.worldclock.worldclock.CityTime;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.weather.WeatherLocation;

public class PreferencesUtil {
    private static final String TAG = "WorldClock.PreferencesUtil";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    // tab : DeskClock
    public static final String DESKCLOCK = "deskclock";
    public static final String NEXT_ALARM = "next_alarm";
    public static final String NEXT_ALARM_ID = "next_alarm_id";
    public static final String NEXT_ALARM_TIME = "next_alarm_time";
    public static final String NEXT_ALARM_DESCRIPTION = "next_alarm_description";
    public static final String LONG_DATEFORMAT = "long_dateformat";
    public static final String SHORT_DATEFORMAT = "short_dateformat";
    public static final String TEMPERATURE = "temperature";
    public static final String HIGH_TEMPERATURE = "high_temperature";
    public static final String LOW_TEMPERATURE = "low_temperature";
    public static final String LOCATION = "location";
    public static final String INFORMATION = "information";
    public static final String CONDITION_ID = "condition_id";
    public static final String PLUGGED_IN = "plugged_in";
    public static final String BATTERY_LEVEL = "battery_level";
    public static final String HOUR_MODE = "hour_mode";

    // tab : Stopwatch
    public static final String STOPWATCH = "stopwatch";
    public static final String STARTTIME = "start_time";
    public static final String LAPCOUNT = "lap_count";
    public static final String STOPWATCH_STATE = "stopwatch_state";
    public static final String PAUSETIME = "pause_time";
    public static final String RUNNINGTOTALTIME = "running_total_time";
    public static final String LASTRUNNINGTOTALTIME = "last_running_total_time";

    // tab : Timer
    public static final String TIMER = "timer";
    public static final String TIMER_SOUND_URI = "timer_sound_uri";
    public static final String TIMER_STATE = "timer_state";
    public static final String TIMER_USER_CHOICE_TIME = "timer_user_choice_time";
    public static final String TIMER_START_TIME = "timer_start_time";
    public static final String TIMER_EXPIRE_TIME = "timer_expire_time";
    public static final String TIMER_PAUSE_TIME = "timer_pause_time";
    public static final String TIMER_LABEL = "timer_label";

    // tab : Alarm
    public static final String ALARM = "alarm";
    public static final String IS_SYNC_ALARM_CLOCK_DB = "is_sync_alarmclock_db";
    public static final String START_WEEKDAY = "start_weekday"; // 1 -- Sunday; 2 -- Monday
    public static final String IS_BACKUP_ALARM_DB = "is_backup_alarm_db";
    public static final String ALARM_SOUND_TITLE = "alarm_sound_title";
    public static final String PREVIOUS_SYSTEM_TIMEZONE = "previous_system_timezone";
    public static final String IS_DEVICE_ENCRYPTION = "is_device_encryption";
    public static final String IS_CLOCKD_EXIST = "is_clockd_exist";
    public static final String CURRENT_FIRING_ALARM = "current_firing_alarm";
    public static final String IS_FIRING_TIMER = "is_firing_timer";
    public static final String IS_VOLUME_INCREASE = "is_volume_increase";
    public static final String NEXT_OFFALARM_ID = "next_offalarm_id";
    public static final String NEXT_OFFALARM_TIME = "next_offalarm_time";
    public static final String NEXT_OFFALARM_DESCRIPTION = "next_offalarm_description";

    // tab : Tab Control
    public static final String TAB = "tab";
    public static final String LAST_TAB = "last_tab";
    public static final String DEFAULT_TAB = "default_tab";
    public static final String SEQUENCE_TAB = "sequence_tab";
    public static final String LOAD_SETTINGS = "load_settings";

    // tab: WorldClock:
    public static final String WORLDCLOCK = "worldclock";
    public static final String IS_SYNC_WORLD_CLOCK_DB = "is_sync_worldclock_db";
    public static final String IS_DUP_HOME_DEFAULT_CITY = "is_dup_home_default_city";
    public static final String CITY_TIME_CURRENT = "city_time_current";
    public static final String CITY_TIME_HOME = "city_time_HOME";
    public static final String CITY_TIME_CITY_SIZE = "city_time_size";
    public static final String CITY_TIME_CITY_INDEX = "city_time_index_";
    public static final String CITY_TIME_SPLIT_SYM = "___";
    public static final String IS_HOME_FROM_GEO_CODER = "is_home_from_geo_coder";
    public static final int CITY_TIME_DATA_LEN = 9;

    /**
     * for deskclock
     */
    public static int getNextAlarmId(Context context) {
        int value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(NEXT_ALARM_ID)) {
            value = settings.getInt(NEXT_ALARM_ID, -1);
        } else {
            value = -1;
        }
        return value;
    }

    public static void setNextAlarmId(Context context, int nextAlarmId) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(NEXT_ALARM_ID, nextAlarmId);
        editor.apply();
    }

    public static long getNextAlarmTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(NEXT_ALARM_TIME)) {
            value = settings.getLong(NEXT_ALARM_TIME, -1);
        } else {
            value = -1;
        }
        return value;
    }

    public static void setNextAlarmTime(Context context, long nextAlarmTime) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(NEXT_ALARM_TIME, nextAlarmTime);
        editor.apply();
    }
    
    public static String getNextAlarmDescription(Context context) {
        String value = "";
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(NEXT_ALARM_DESCRIPTION)) {
            value = settings.getString(NEXT_ALARM_DESCRIPTION, "");
        }
        return value;
    }

    public static void setNextAlarmDescription(Context context, String description) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(NEXT_ALARM_DESCRIPTION, description);
        editor.apply();
    }

    public static String getLongDateFormat(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(LONG_DATEFORMAT)) {
            value = settings.getString(LONG_DATEFORMAT, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setLongDateFormat(Context context, String title) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(LONG_DATEFORMAT, title);
        editor.apply();
    }

    public static String getShortDateFormat(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(SHORT_DATEFORMAT)) {
            value = settings.getString(SHORT_DATEFORMAT, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setShortDateFormat(Context context, String title) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(SHORT_DATEFORMAT, title);
        editor.apply();
    }

    public static String getTemperature(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(TEMPERATURE)) {
            value = settings.getString(TEMPERATURE, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setTemperature(Context context, String title) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(TEMPERATURE, title);
        editor.apply();
    }

    public static String getHighTemperature(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(HIGH_TEMPERATURE)) {
            value = settings.getString(HIGH_TEMPERATURE, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setHighTemperature(Context context, String title) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(HIGH_TEMPERATURE, title);
        editor.apply();
    }

    public static String getLowTemperature(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(LOW_TEMPERATURE)) {
            value = settings.getString(LOW_TEMPERATURE, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setLowTemperature(Context context, String title) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(LOW_TEMPERATURE, title);
        editor.apply();
    }

    public static String getLocation(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(LOCATION)) {
            value = settings.getString(LOCATION, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setLocation(Context context, String title) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(LOCATION, title);
        editor.apply();
    }

    public static String getInformation(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(INFORMATION)) {
            value = settings.getString(INFORMATION, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setInformation(Context context, String title) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(INFORMATION, title);
        editor.apply();
    }

    public static int getConditionId(Context context) {
        int value = -1;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(CONDITION_ID)) {
            value = settings.getInt(CONDITION_ID, -1);
        }
        return value;
    }

    public void setConditionId(Context context, int id) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(CONDITION_ID, id);
        editor.apply();
    }

    public static boolean getPluggedIn(Context context) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        boolean value = false;
        if (settings.contains(PLUGGED_IN)) {
            value = settings.getBoolean(PLUGGED_IN, false);
        }
        return value;
    }

    public static void setPluggedIn(Context context, boolean pluggedIn) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(PLUGGED_IN, pluggedIn);
        editor.apply();
    }

    public static int getBatteryLevel(Context context) {
        int value = -1;
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(BATTERY_LEVEL)) {
            value = settings.getInt(BATTERY_LEVEL, -1);
        }
        return value;
    }

    public static void setBatteryLevel(Context context, int id) {
        SharedPreferences settings = context.getSharedPreferences(DESKCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(BATTERY_LEVEL, id);
        editor.apply();
    }

    public static long getNextAlarm(SharedPreferences settings) {
        long value;
        if (settings.contains(NEXT_ALARM)) {
            value = settings.getLong(NEXT_ALARM, -1);
        } else {
            value = -1;
        }
        return value;
    }

    public static void setNextAlarm(SharedPreferences settings, long nextAlarm) {
        Editor editor = settings.edit().putLong(NEXT_ALARM, nextAlarm);
        editor.apply();
    }

    public static String getLongDateFormat(SharedPreferences settings) {
        String value;
        if (settings.contains(LONG_DATEFORMAT)) {
            value = settings.getString(LONG_DATEFORMAT, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setLongDateFormat(SharedPreferences settings, String title) {
        Editor editor = settings.edit().putString(LONG_DATEFORMAT, title);
        editor.apply();
    }

    public static String getShortDateFormat(SharedPreferences settings) {
        String value;
        if (settings.contains(SHORT_DATEFORMAT)) {
            value = settings.getString(SHORT_DATEFORMAT, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setShortDateFormat(SharedPreferences settings, String title) {
        Editor editor = settings.edit().putString(SHORT_DATEFORMAT, title);
        editor.apply();
    }

    public static String getTemperature(SharedPreferences settings) {
        String value;
        if (settings.contains(TEMPERATURE)) {
            value = settings.getString(TEMPERATURE, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setTemperature(SharedPreferences settings, String title) {
        Editor editor = settings.edit().putString(TEMPERATURE, title);
        editor.apply();
    }

    public static String getHighTemperature(SharedPreferences settings) {
        String value;
        if (settings.contains(HIGH_TEMPERATURE)) {
            value = settings.getString(HIGH_TEMPERATURE, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setHighTemperature(SharedPreferences settings, String title) {
        Editor editor = settings.edit().putString(HIGH_TEMPERATURE, title);
        editor.apply();
    }

    public static String getLowTemperature(SharedPreferences settings) {
        String value;
        if (settings.contains(LOW_TEMPERATURE)) {
            value = settings.getString(LOW_TEMPERATURE, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setLowTemperature(SharedPreferences settings, String title) {
        Editor editor = settings.edit().putString(LOW_TEMPERATURE, title);
        editor.apply();
    }

    public static String getLocation(SharedPreferences settings) {
        String value;
        if (settings.contains(LOCATION)) {
            value = settings.getString(LOCATION, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setLocation(SharedPreferences settings, String title) {
        Editor editor = settings.edit().putString(LOCATION, title);
        editor.apply();
    }

    public static String getInformation(SharedPreferences settings) {
        String value;
        if (settings.contains(INFORMATION)) {
            value = settings.getString(INFORMATION, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setInformation(SharedPreferences settings, String title) {
        Editor editor = settings.edit().putString(INFORMATION, title);
        editor.apply();
    }

    public static int getConditionId(SharedPreferences settings) {
        int value = -1;
        if (settings.contains(CONDITION_ID)) {
            value = settings.getInt(CONDITION_ID, -1);
        }
        return value;
    }

    public static void setConditionId(SharedPreferences settings, int id) {
        Editor editor = settings.edit().putInt(CONDITION_ID, id);
        editor.apply();
    }

    public static boolean getPluggedIn(SharedPreferences settings) {
        boolean value = false;
        if (settings.contains(PLUGGED_IN)) {
            value = settings.getBoolean(PLUGGED_IN, false);
        }
        return value;
    }

    public static void setPluggedIn(SharedPreferences settings, boolean pluggedIn) {
        Editor editor = settings.edit().putBoolean(PLUGGED_IN, pluggedIn);
        editor.apply();
    }

    public static int getBatteryLevel(SharedPreferences settings) {
        int value = -1;
        if (settings.contains(BATTERY_LEVEL)) {
            value = settings.getInt(BATTERY_LEVEL, -1);
        }
        return value;
    }

    public static void setBatteryLevel(SharedPreferences settings, int level) {
        Editor editor = settings.edit().putInt(BATTERY_LEVEL, level);
        editor.apply();
    }
    
    /**
     * for stopwtach
     */
    public static int getLapCount(Context context) {
        int value;
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        if (settings.contains(LAPCOUNT)) {
            value = settings.getInt(LAPCOUNT, 1);
        } else {
            value = 1;
        }
        return value;
    }

    public static void setLapCount(Context context, int count) {
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(LAPCOUNT, count);
        editor.apply();
    }

    public static long getStartTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        if (settings.contains(STARTTIME)) {
            value = settings.getLong(STARTTIME, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setStartTime(Context context, long start) {
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(STARTTIME, start);
        editor.apply();
    }

    public static long getPauseTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        if (settings.contains(PAUSETIME)) {
            value = settings.getLong(PAUSETIME, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setPauseTime(Context context, long pause) {
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(PAUSETIME, pause);
        editor.apply();
    }

    public static int getStopwatchState(Context context) {
        int value;
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        if (settings.contains(STOPWATCH_STATE)) {
            value = settings.getInt(STOPWATCH_STATE, Stopwatch.StopwatchEnum.NORMAL.ordinal());
        } else {
            value = Stopwatch.StopwatchEnum.NORMAL.ordinal();
        }
        return value;
    }

    public static void setStopwatchState(Context context, int state) {
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(STOPWATCH_STATE, state);
        editor.apply();
    }

    public static long getRunningTotalTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        if (settings.contains(RUNNINGTOTALTIME)) {
            value = settings.getLong(RUNNINGTOTALTIME, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setRunningTotalTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(RUNNINGTOTALTIME, time);
        editor.apply();
    }

    public static long getLastRunningTotalTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        if (settings.contains(LASTRUNNINGTOTALTIME)) {
            value = settings.getLong(LASTRUNNINGTOTALTIME, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setLastRunningTotalTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(STOPWATCH, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(LASTRUNNINGTOTALTIME, time);
        editor.apply();
    }

    /**
     * for timer
     */
    public static String getTimerSoundUri(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        if (settings.contains(TIMER_SOUND_URI)) {
            value = settings.getString(TIMER_SOUND_URI, null);
        } else {
            value = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
        }
        return value;
    }

    public static void setTimerSoundUri(Context context, String uri) {
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(TIMER_SOUND_URI, uri);
        editor.apply();
    }

    public static void setTimerState(Context context, int state) {
        if (DEBUG_FLAG) Log.d(TAG, "setTimerState: state = " + state);
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(TIMER_STATE, state);
        editor.apply();
    }

    public static int getTimerState(Context context) {
        int value;
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        if (settings.contains(TIMER_STATE)) {
            value = settings.getInt(TIMER_STATE, Timer.TimerEnum.NORMAL.ordinal());
        } else {
            value = Timer.TimerEnum.NORMAL.ordinal();
        }
        return value;
    }

    public static long getTimerUserChoiceTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        if (settings.contains(TIMER_USER_CHOICE_TIME)) {
            value = settings.getLong(TIMER_USER_CHOICE_TIME, Timer.DEFAULT_COUNTDOWN_VALUE);
        } else {
            value = Timer.DEFAULT_COUNTDOWN_VALUE;
        }
        return value;
    }

    public static void setTimerUserChoiceTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(TIMER_USER_CHOICE_TIME, time);
        editor.apply();
    }

    public static long getTimerStartTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        if (settings.contains(TIMER_START_TIME)) {
            value = settings.getLong(TIMER_START_TIME, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setTimerStartTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(TIMER_START_TIME, time);
        editor.apply();
    }

    public static long getTimerExpireTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        if (settings.contains(TIMER_EXPIRE_TIME)) {
            value = settings.getLong(TIMER_EXPIRE_TIME, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setTimerExpireTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(TIMER_EXPIRE_TIME, time);
        editor.apply();
    }

    public static long getTimerPauseTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        if (settings.contains(TIMER_PAUSE_TIME)) {
            value = settings.getLong(TIMER_PAUSE_TIME, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setTimerPauseTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(TIMER_PAUSE_TIME, time);
        editor.apply();
    }

    public static String getTimerLabel(Context context) {
        String value = "";
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        if (settings.contains(TIMER_LABEL)) {
            value = settings.getString(TIMER_LABEL, "");
        }
        return value;
    }

    public static void setTimerLabel(Context context, String label) {
        SharedPreferences settings = context.getSharedPreferences(TIMER, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(TIMER_LABEL, label);
        editor.apply();
    }
    
    /**
     * for alarm
     */
    // 1 -- Sunday; 2 -- Monday
    public static int getStartWeekDay(Context context) {
        int value = 1;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(START_WEEKDAY)) {
            value = settings.getInt(START_WEEKDAY, 1);
        }
        return value;
    }

    public static void setStartWeekDay(Context context, int weekDay) {
        // check args
        if ((weekDay < 1) || (weekDay > 7)) {
            weekDay = 1; // reset to default(Sunday)
        }
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(START_WEEKDAY, weekDay);
        editor.apply();
    }
    
    public static boolean getBackupAlarmDB(Context context) {
        boolean value = false;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(IS_BACKUP_ALARM_DB)) {
            value = settings.getBoolean(IS_BACKUP_ALARM_DB, false);
        }
        return value;
    }

    public static void setBackupAlarmDB(Context context, boolean isBackup) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_BACKUP_ALARM_DB, isBackup);
        editor.apply();
    }

    public static boolean getSyncAlarmClockDB(Context context) {
        boolean value = false;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(IS_SYNC_ALARM_CLOCK_DB)) {
            value = settings.getBoolean(IS_SYNC_ALARM_CLOCK_DB, false);
        }
        return value;
    }

    public static void setSyncAlarmClockDB(Context context, boolean isSync) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_SYNC_ALARM_CLOCK_DB, isSync);
        editor.apply();
    }

    public static String getAlarmSoundCachedTitle(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(ALARM_SOUND_TITLE)) {
            value = settings.getString(ALARM_SOUND_TITLE, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setAlarmSoundCachedTitle(Context context, String uri) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(ALARM_SOUND_TITLE, uri);
        editor.apply();
    }

    public static long getPrevSysTimezone(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(PREVIOUS_SYSTEM_TIMEZONE)) {
            value = settings.getLong(PREVIOUS_SYSTEM_TIMEZONE, 0);
        } else {
            value = 0;
        }
        return value;
    }

    public static void setPrevSysTimezone(Context context, long offset) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(PREVIOUS_SYSTEM_TIMEZONE, offset);
        editor.apply();
    }

    public static void setClockdExist(Context context, boolean isExist) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_CLOCKD_EXIST, isExist);
        editor.apply();
    }
    
    public static boolean getClockdExist(Context context) {
        boolean value = false;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(IS_CLOCKD_EXIST)) {
            value = settings.getBoolean(IS_CLOCKD_EXIST, false);
        }
        return value;
    }

    public static boolean getDeviceEncryption(Context context) {
        boolean value = false;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(IS_DEVICE_ENCRYPTION)) {
            value = settings.getBoolean(IS_DEVICE_ENCRYPTION, false);
        }
        return value;
    }
    
    public static void setDeviceEncryption(Context context, boolean isEncryption) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_DEVICE_ENCRYPTION, isEncryption);
        editor.apply();
    }
    
    public static int getCurrentFiringAlarm(Context context) {
        int value;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(CURRENT_FIRING_ALARM)) {
            value = settings.getInt(CURRENT_FIRING_ALARM, AlarmUtils.INVALID_ALARMID);
        } else {
            value = AlarmUtils.INVALID_ALARMID;
        }
        return value;
    }

    public static void setCurrentFiringAlarm(Context context, int alarmId) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(CURRENT_FIRING_ALARM, alarmId);
        editor.apply();
    }

    public static boolean getIsFiringTimer(Context context) {
        boolean value;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(IS_FIRING_TIMER)) {
            value = settings.getBoolean(IS_FIRING_TIMER, false);
        } else {
            value = false;
        }
        return value;
    }

    public static void setIsFiringTimer(Context context, boolean isFiring) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_FIRING_TIMER, isFiring);
        editor.apply();
    }
    
    public static boolean getVolumeIncrease(Context context) {
        boolean value = true;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(IS_VOLUME_INCREASE)) {
            value = settings.getBoolean(IS_VOLUME_INCREASE, true);
        }
        return value;
    }
    
    public static void setVolumeIncrease(Context context, boolean isEncryption) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_VOLUME_INCREASE, isEncryption);
        editor.apply();
    }
    
    public static int getNextOffAlarmId(Context context) {
        int value;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(NEXT_OFFALARM_ID)) {
            value = settings.getInt(NEXT_OFFALARM_ID, -1);
        } else {
            value = -1;
        }
        return value;
    }

    public static void setNextOffAlarmId(Context context, int nextAlarmId) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(NEXT_OFFALARM_ID, nextAlarmId);
        editor.apply();
    }

    public static long getNextOffAlarmTime(Context context) {
        long value;
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(NEXT_OFFALARM_TIME)) {
            value = settings.getLong(NEXT_OFFALARM_TIME, -1);
        } else {
            value = -1;
        }
        return value;
    }

    public static void setNextOffAlarmTime(Context context, long nextAlarmTime) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putLong(NEXT_OFFALARM_TIME, nextAlarmTime);
        editor.apply();
    }
    
    public static String getNextOffAlarmDescription(Context context) {
        String value = "";
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        if (settings.contains(NEXT_OFFALARM_DESCRIPTION)) {
            value = settings.getString(NEXT_OFFALARM_DESCRIPTION, "");
        }
        return value;
    }

    public static void setNextOffAlarmDescription(Context context, String description) {
        SharedPreferences settings = context.getSharedPreferences(ALARM, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(NEXT_OFFALARM_DESCRIPTION, description);
        editor.apply();
    }
    
    /**
     * for worldclock tab control
     */
    public static String getLastTab(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        if (settings.contains(LAST_TAB)) {
            value = settings.getString(LAST_TAB, null);
        } else {
            value = null;
        }
        return value;
    }

    public static void setLastTab(Context context, String tab) {
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(LAST_TAB, tab);
        editor.apply();
    }

    public static int getDefaultTab(Context context) {
        int value;
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        if (settings.contains(DEFAULT_TAB)) {
            value = settings.getInt(DEFAULT_TAB, SettingsReader.TAB_DEFAULT);
        } else {
            value = SettingsReader.TAB_DEFAULT;
        }
        return value;
    }

    public static void setDefaultTab(Context context, int defaultTab) {
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putInt(DEFAULT_TAB, defaultTab);
        editor.apply();
    }

    public static String getSequenceTab(Context context) {
        String value;
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        if (settings.contains(SEQUENCE_TAB)) {
            value = settings.getString(SEQUENCE_TAB, SettingsReader.TAB_SEQUENCE);
        } else {
            value = SettingsReader.TAB_SEQUENCE;
        }
        return value;
    }

    public static void setSequenceTab(Context context, String sequenceTab) {
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putString(SEQUENCE_TAB, sequenceTab);
        editor.apply();
    }

    public static boolean getLoadSettings(Context context) {
        boolean value = false;
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        if (settings.contains(LOAD_SETTINGS)) {
            value = settings.getBoolean(LOAD_SETTINGS, false);
        }
        return value;
    }

    public static void setLoadSettings(Context context, boolean loadSettings) {
        SharedPreferences settings = context.getSharedPreferences(TAB, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(LOAD_SETTINGS, loadSettings);
        editor.apply();
    }
    
    /*
     * WorldClock:
     */
    public static void setSyncWorldClockDB(Context context, boolean isSync) {
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_SYNC_WORLD_CLOCK_DB, isSync);
        editor.apply();
    }

    public static boolean getSyncWorldClockDB(Context context) {
        boolean value = false;
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(IS_SYNC_WORLD_CLOCK_DB)) {
            value = settings.getBoolean(IS_SYNC_WORLD_CLOCK_DB, false);
        }
        return value;
    }

    public static void setDupHomeDefaultCity(Context context, boolean isDuplicate) {
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_DUP_HOME_DEFAULT_CITY, isDuplicate);
        editor.apply();
    }

    public static boolean getDupHomeDefaultCity(Context context) {
        boolean value = false;
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(IS_DUP_HOME_DEFAULT_CITY)) {
            value = settings.getBoolean(IS_DUP_HOME_DEFAULT_CITY, false);
        }
        return value;
    }

    public static void setCityTimeCurrent(Context context, CityTime ct) {
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);

        String str = cityTimeToPreferenceStr(ct);
        Editor editor = settings.edit().putString(CITY_TIME_CURRENT, str);
        editor.apply();
    }

    public static CityTime getCityTimeCurrent(Context context) {
        CityTime ct = null;
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);

        if (settings.contains(CITY_TIME_CURRENT)) {
            String value = settings.getString(CITY_TIME_CURRENT, null);
            if (value != null) {
                ct = preferenceStrToCityTime(value);
            }
        }
        return ct;
    }

    public static void setCityTimeHome(Context context, CityTime ct) {
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);

        String str = cityTimeToPreferenceStr(ct);
        Editor editor = settings.edit().putString(CITY_TIME_HOME, str);
        editor.apply();
    }

    public static CityTime getCityTimeHome(Context context) {
        CityTime ct = null;
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);

        if (settings.contains(CITY_TIME_HOME)) {
            String value = settings.getString(CITY_TIME_HOME, null);
            if (value != null) {
                ct = preferenceStrToCityTime(value);
            }
        }

        return ct;
    }

    public static void setCityTimeCities(Context context, CityTime[] ct) {
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        int size = ct.length;

        {
            Editor editor = settings.edit().putInt(CITY_TIME_CITY_SIZE, size);
            editor.apply();
        }

        for (int i = 0; i < size; i++) {
            String str = cityTimeToPreferenceStr(ct[i]);
            Editor editor = settings.edit().putString(CITY_TIME_CITY_INDEX + i, str);
            editor.apply();
        }
    }

    public static CityTime[] getCityTimeCities(Context context) {
        CityTime[] ct = null;
        int size = 0;
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(CITY_TIME_CITY_SIZE)) {
            size = settings.getInt(CITY_TIME_CITY_SIZE, 0);
        }
        ct = new CityTime[size];
        for (int i = 0; i < size; i++) {
            String label = CITY_TIME_CITY_INDEX + i;
            if (settings.contains(label)) {
                String value = settings.getString(label, "");
                ct[i] = preferenceStrToCityTime(value);
            }
        }
        return ct;
    }

    public static String cityTimeToPreferenceStr(CityTime ct) {
        WeatherLocation weatherLoc = ct.getWeatherLocation();
        return weatherLoc.getCode() + CITY_TIME_SPLIT_SYM
            + weatherLoc.getName() + CITY_TIME_SPLIT_SYM
            + weatherLoc.getLatitude() + CITY_TIME_SPLIT_SYM
            + weatherLoc.getLongitude() + CITY_TIME_SPLIT_SYM
            + weatherLoc.getTimezoneId() + CITY_TIME_SPLIT_SYM
            + ct.getCityId() + CITY_TIME_SPLIT_SYM
            + ct.getCityName() + CITY_TIME_SPLIT_SYM
            + ct.getWeatherDayTime() + CITY_TIME_SPLIT_SYM
            + ct.getWeatherNightTime();
    }

    public static CityTime preferenceStrToCityTime(String str) {
        String[] token = str.split(CITY_TIME_SPLIT_SYM);
        CityTime ct = new CityTime();

        WeatherLocation weatherLoc = new WeatherLocation();
        weatherLoc.setCode(token[0]);
        weatherLoc.setName(token[1]);
        weatherLoc.setLatitude(token[2]);
        weatherLoc.setLongitude(token[3]);
        weatherLoc.setTimezoneId(token[4]);
        ct.setWeatherLocation(weatherLoc);
        ct.setTimeZone(TimeZone.getTimeZone(weatherLoc.getTimezoneId()));
        ct.setCityId(token[5]);
        ct.setCityName(token[6]);

        try {
            ct.setWeatherDayNightInfo(token[7], token[8]);
        } catch (Exception e){
            Log.w(TAG, "Excpetion e = " + e.toString());
            ct.setWeatherDayNightInfo(CityTime.DEFAULT_DAY_TIME_STRING, CityTime.DEFAULT_NIGHT_TIME_STRING);
        }

        return ct;
    }
    
    public static boolean getNewHomeFromGeoCoder(Context context) {
        boolean value = true;
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        if (settings.contains(IS_HOME_FROM_GEO_CODER)) {
            value = settings.getBoolean(IS_HOME_FROM_GEO_CODER, true);
        }
        return value;
    }
    
    public static void setNewHomeFromGeoCoder(Context context, boolean isFromGeoCoder) {
        SharedPreferences settings = context.getSharedPreferences(WORLDCLOCK, Context.MODE_PRIVATE);
        Editor editor = settings.edit().putBoolean(IS_HOME_FROM_GEO_CODER, isFromGeoCoder);
        editor.apply();
    }
}
