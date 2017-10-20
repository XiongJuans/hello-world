/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.htc.android.worldclock.alarmclock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.support.v4.os.BuildCompat;
import android.text.format.DateFormat;
import android.util.Log;
import java.util.Collections;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.aiservice.AiUtils;
import com.htc.android.worldclock.aiservice.AlarmSorter;
import com.htc.android.worldclock.alarmclock.SetAlarm.RepeatTypeEnum;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.SettingsActivity;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.HtcCalendarFramework.util.calendar.holidays.ChinaHolidayUtil;
import com.htc.lib1.cc.util.res.HtcResUtil;

/**
 * The AlarmUtils provider supplies info about Alarm Clock settings
 */
public class AlarmUtils {
    private static final String TAG = "WorldClock.AlarmUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    public final static String ACTION_ALARM_ALERT = "com.htc.worldclock.ALARM_ALERT";
    public final static String ACTION_ALARM_NOTICE = "com.htc.worldclock.ALARM_NOTICE";

    public final static String ID = "alarm_id";
    public final static String TIME = "alarm_time";
    public final static String DESCRIPTION = "alarm_description";
    //for put alarm clock ring sounds uri strings key
    public final static String ALERT = "alarm_alert";

    public final static String ADD = "add";
    public final static String EDIT = "edit";

    public final static String M12 = "h:mm aa";
    public final static String M24 = "k:mm";

    public final static int INVALID_ALARMID = -1;
    public final static long INVALID_ALARMTIME = -1;
    public final static String NEXT_ALARM_TIME = "next_alarm_time";
    public static final int SCREEN_OFF_DELAY_TIME = 500;

    // This intent is sent from the notification when the user cancels the
    // snooze alert.
    public static final String ACTION_CANCEL_SNOOZE = "com.htc.android.worldclock.intent.action.cancel_snooze";
    public static final String SNOOZE = "snooze";

    private static final Uri ALARMS_ENABLE_BULK_URI =
        Uri.parse("content://" + "com.htc.android.alarmclock" + "/alarm_enable_bulk");

    private static final Uri ALARMS_SNOOZE_BULK_URI =
        Uri.parse("content://" + "com.htc.android.alarmclock" + "/alarm_snooze_bulk");

    public static class DaysOfWeek {
        // Number if days in the week.
        public static final int DAYS_IN_A_WEEK = 7;
        int mDays;

        /**
         * Days of week coded as single int, convenient for DB
         * storage:
         * 
         * 0x00: no day
         * 0x01: Monday
         * 0x02: Tuesday
         * 0x04: Wednesday
         * 0x08: Thursday
         * 0x10: Friday
         * 0x20: Saturday
         * 0x40: Sunday
         */
        public DaysOfWeek() {
            this(0);

        }

        public DaysOfWeek(int days) {
            mDays = days;
        }

        public String toString(Context context, boolean showNever, int startWeekDay) {
            StringBuilder ret = new StringBuilder();

            /* no days */
            if (mDays == 0) {
                return showNever ? context.getText(
                    R.string.never).toString() : "";
            }

            /* every day */
            if (mDays == 0x7f) {
                return context.getText(R.string.every_day).toString();
            }

            /* count selected days */
            int dayCount = 0, days = mDays;
            while (days > 0) {
                if ((days & 1) == 1) {
                    dayCount++;
                }
                days >>= 1;
            }

            /* short or long form? */
            CharSequence[] strings = context.getResources().getTextArray((dayCount > 1) ? R.array.days_of_week_short : R.array.days_of_week);
            CharSequence[] newWeekArray = new CharSequence[7];
            for (int i = 0; i < 7; i++) {
                newWeekArray[(((1 - (startWeekDay - 1)) + 7) + i) % 7] = strings[i];
            }

            int rotateLeftBits = ((1 - (startWeekDay - 1)) + 7) % 7; // reserves
            int newDays = (mDays << rotateLeftBits) & 0x7f | (mDays >> (7 - rotateLeftBits)) & 0x7f;
            /* selected days */
            for (int i = 0; i < 7; i++) {
                if ((newDays & (1 << i)) != 0) {
                   ret.append(HtcResUtil.toUpperCase(context, newWeekArray[i].toString()));
                    dayCount -= 1;
                    if (dayCount > 0) {
                        ret.append(context.getText(R.string.day_concat));
                    }
                }
            }
            return ret.toString();
        }

        /***
         *Set TextView Content Description String:MONDAY--SUNDAY
         * @param context
         * @param showNever
         * @param startWeekDay
         * @return
         */
        public String toContentDescriptionString(Context context, boolean showNever, int startWeekDay) {
            StringBuilder ret = new StringBuilder();

            /* no days */
            if (mDays == 0) {
                return showNever ? context.getText(
                        R.string.never).toString() : "";
            }

            /* every day */
            if (mDays == 0x7f) {
                return context.getText(R.string.every_day).toString();
            }

            /* count selected days */
            int dayCount = 0, days = mDays;
            while (days > 0) {
                if ((days & 1) == 1) {
                    dayCount++;
                }
                days >>= 1;
            }

            /* short or long form? */
            CharSequence[] strings = context.getResources().getTextArray(R.array.days_of_week);
            CharSequence[] newWeekArray = new CharSequence[7];
            for (int i = 0; i < 7; i++) {
                newWeekArray[(((1 - (startWeekDay - 1)) + 7) + i) % 7] = strings[i];
            }

            int rotateLeftBits = ((1 - (startWeekDay - 1)) + 7) % 7; // reserves
            int newDays = (mDays << rotateLeftBits) & 0x7f | (mDays >> (7 - rotateLeftBits)) & 0x7f;
            /* selected days */
            for (int i = 0; i < 7; i++) {
                if ((newDays & (1 << i)) != 0) {
                    ret.append(HtcResUtil.toUpperCase(context, newWeekArray[i].toString()));
                    dayCount -= 1;
                    if (dayCount > 0) {
                        ret.append(context.getText(R.string.day_concat));
                    }
                }
            }
            return ret.toString();
        }


        /**
         * @param day
         *            Mon=0 ... Sun=6
         * @return true if given day is set
         */
        public boolean isSet(int day) {
            return ((mDays & (1 << day)) > 0);
        }

        public void set(int day, boolean set) {
            if (set) {
                mDays |= (1 << day);
            } else {
                mDays &= ~(1 << day);
            }
        }

        public void set(DaysOfWeek dow) {
            mDays = dow.mDays;
        }

        /**
         * Need to have monday start at index 0 to be backwards compatible. This converts
         * Calendar.DAY_OF_WEEK constants to our internal bit structure.
         */
        private static int convertDayToBitIndex(int day) {
            return (day + 5) % DAYS_IN_A_WEEK;
        }
        
        public void setDaysOfWeek(boolean value, int ... daysOfWeek) {
            for (int day : daysOfWeek) {
                set(convertDayToBitIndex(day), value);
            }
        }
        
        public int getCoded() {
            return mDays;
        }

        public boolean equals(DaysOfWeek dow) {
            return mDays == dow.mDays;
        }

        // Returns days of week encoded in an array of booleans.
        public boolean[] getBooleanArray() {
            boolean[] ret = new boolean[7];
            for (int i = 0; i < 7; i++) {
                ret[i] = isSet(i);
            }
            return ret;
        }

        public void setCoded(int days) {
            mDays = days;
        }

        /**
         * @return true if alarm is set to repeat
         */
        public boolean isRepeatSet() {
            return mDays != 0;
        }

        /**
         * @return true if alarm is set to repeat every day
         */
        public boolean isEveryDaySet() {
            return mDays == 0x7f;
        }

        /**
         * returns number of days from today until next alarm
         * 
         * @param c
         *            must be set to today
         */
        public int getNextAlarm(Calendar c) {
            if (mDays == 0) {
                return -1;
            }
            int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

            int day, dayCount;
            for (dayCount = 0; dayCount < 7; dayCount++) {
                day = (today + dayCount) % 7;
                if ((mDays & (1 << day)) > 0) {
                    break;
                }
            }
            return dayCount;
        }
    }

    /**
     * Parse cursor
     */
    public static AlarmItem parseCursor(Cursor cur) {
        AlarmItem item = new AlarmItem();
        item.aId = cur.getInt(AlarmColumns.ALARM_ID_INDEX);
        item.aHour = cur.getInt(AlarmColumns.ALARM_HOUR_INDEX);
        item.aMinutes = cur.getInt(AlarmColumns.ALARM_MINUTES_INDEX);
        item.aDaysOfWeek = cur.getInt(AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX);
        item.aEnabled = cur.getInt(AlarmColumns.ALARM_ENABLED_INDEX) == 1 ? true : false;
        item.aVibrate = cur.getInt(AlarmColumns.ALARM_VIBRATE_INDEX) == 1 ? true : false;
        item.aDescription = cur.getString(AlarmColumns.ALARM_MESSAGE_INDEX);
        item.aAlert = cur.getString(AlarmColumns.ALARM_ALERT_INDEX);
        item.aSnoozed = cur.getInt(AlarmColumns.ALARM_SNOOZED_INDEX) == 1 ? true : false;
        item.aOffAlarm = cur.getInt(AlarmColumns.ALARM_OFFALARM_INDEX) == 1 ? true : false;
        item.aRepeatType = cur.getInt(AlarmColumns.ALARM_REPEAT_TYPE_INDEX);

        return item;
    }

    public static class AlarmColumns implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://com.htc.android.alarmclock/alarm");

        public static final String _ID = "_id";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        /**
         * Hour in 24-hour localtime 0 - 23.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String MINUTES = "minutes";

        /**
         * Days of week coded as integer
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DAYS_OF_WEEK = "daysofweek";

        /**
         * Alarm time in UTC milliseconds from the epoch.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String ALARM_TIME = "alarmtime";

        /**
         * True if alarm is active
         * <P>
         * Type: BOOLEAN
         * </P>
         */
        public static final String ENABLED = "enabled";

        /**
         * True if alarm should vibrate
         * <P>
         * Type: BOOLEAN
         * </P>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Message to show when alarm triggers
         * Note: not currently used
         * <P>
         * Type: STRING
         * </P>
         */
        public static final String MESSAGE = "message";

        /**
         * Audio alert to play when alarm triggers
         * <P>
         * Type: STRING
         * </P>
         */
        public static final String ALERT = "alert";

        /**
         * True if alarm is snoozed
         * <P>
         * Type: BOOLEAN
         * </P>
         */
        public static final String SNOOZED = "snoozed";

        /**
         * True if alarm is support off alarm
         * <P>
         * Type: BOOLEAN
         * </P>
         */
        public static final String OFFALARM = "offalarm";
        
        /**
         * Repeat in type 0 - 4
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String REPEAT_TYPE = "repeat_type";

        public static final String[] ALARM_QUERY_COLUMNS = {
            _ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME,
            ENABLED, VIBRATE, MESSAGE, ALERT, SNOOZED, OFFALARM, REPEAT_TYPE};

        /**
         * These save calls to cursor.getColumnIndexOrThrow()
         * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
         */
        public static final int ALARM_ID_INDEX = 0;
        public static final int ALARM_HOUR_INDEX = 1;
        public static final int ALARM_MINUTES_INDEX = 2;
        public static final int ALARM_DAYS_OF_WEEK_INDEX = 3;
        public static final int ALARM_TIME_INDEX = 4;
        public static final int ALARM_ENABLED_INDEX = 5;
        public static final int ALARM_VIBRATE_INDEX = 6;
        public static final int ALARM_MESSAGE_INDEX = 7;
        public static final int ALARM_ALERT_INDEX = 8;
        public static final int ALARM_SNOOZED_INDEX = 9;
        public static final int ALARM_OFFALARM_INDEX = 10;
        public static final int ALARM_REPEAT_TYPE_INDEX = 11;
    }

    public static class AlarmData {
        int enabled = -1;
        long time = -1;
    }

    /**
     * getAlarm and getAlarms call this interface to report alarms in
     * the database
     */
    public static interface AlarmSettings {
        void reportAlarm(
            int idx, boolean enabled, int hour, int minutes, long alarmtime,
            DaysOfWeek daysOfWeek, boolean vibrate, String message,
            String alert, boolean snoozed, boolean offalarm, int repeat_type);
    }

    /**
     * Creates a new Alarm.
     */
    public synchronized static Uri addAlarm(Context context, ContentResolver contentResolver) {
        if (DEBUG_FLAG) Log.d(TAG, "addAlarm: add new alarm to database");
        ContentValues values = new ContentValues();
        values.put(AlarmUtils.AlarmColumns.HOUR, 9);

        // check contentResolver
        if (contentResolver == null) {
            if (DEBUG_FLAG) Log.d(TAG, "contentResolver is null");
            return null;
        }
        try {
            Uri uri = contentResolver.insert(AlarmColumns.CONTENT_URI, values);
            BackupManager.dataChanged(context.getPackageName());
            PreferencesUtil.setBackupAlarmDB(context, false);
            PreferencesUtil.setSyncAlarmClockDB(context, false);
            return uri;
        } catch (Exception e) {
            Log.w(TAG, "addAlarm: e = " + e.toString());
            return null;
        }
    }

    /**
     * Removes an existing Alarm. If this alarm is snoozing, disables
     * snooze. Sets next alert.
     */
    public synchronized static int deleteAlarm(Context context, int alarmId) {
        if (DEBUG_FLAG) Log.d(TAG, "deleteAlarm");
        // ATS log
        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][alarm_schedule][delete]");
        ContentResolver contentResolver = context.getContentResolver();

        Uri uri = ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, alarmId);
        if (deleteAlarm(context, contentResolver, uri) == -1) {
            return -1;
        } else {
            // cancel notification
            AlertUtils.cancelAlarmSnoozedNotification(context, alarmId);
            setNextAlert(context);
            return 0;
        }
    }

    private synchronized static int deleteAlarm(Context context, ContentResolver contentResolver, Uri uri) {
        if (DEBUG_FLAG) Log.d(TAG, "deleteAlarm");
        // ATS log
        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][alarm_schedule][delete]");
        // check contentResolver
        if (contentResolver == null) {
            if (DEBUG_FLAG) Log.d(TAG, "contentResolver is null");
            return -1;
        }
        try {
            int result = contentResolver.delete(uri, "", null);
            BackupManager.dataChanged(context.getPackageName());
            PreferencesUtil.setBackupAlarmDB(context, false);
            PreferencesUtil.setSyncAlarmClockDB(context, false);
            return result;
        } catch (Exception e) {
            Log.w(TAG, "deleteAlarm: e = " + e.toString());
            return -1;
        }
    }

    /**
     * Queries all alarms
     * 
     * @return cursor over all alarms
     */
    public synchronized static Cursor getAlarmsCursor(ContentResolver contentResolver) {
        // check contentResolver
        if (contentResolver == null) {
            if (DEBUG_FLAG) Log.d(TAG, "contentResolver is null");
            return null;
        }
        try {
            return contentResolver.query(
                AlarmColumns.CONTENT_URI, AlarmColumns.ALARM_QUERY_COLUMNS,
                null, null, AlarmColumns.DEFAULT_SORT_ORDER);
        } catch (Exception e) {
            Log.w(TAG, "getAlarmsCursor: e = " + e.toString());
            return null;
        }
    }

    /**
     * Calls the AlarmSettings.reportAlarm interface on all alarms found in db.
     */
    public synchronized static void getAlarms(ContentResolver contentResolver, AlarmSettings alarmSettings) {
        Cursor cursor = getAlarmsCursor(contentResolver);
        if (cursor != null) {
            getAlarms(alarmSettings, cursor);
            if (cursor.isClosed() == false) {
                cursor.close();
            }
        }
    }

    private synchronized static void getAlarms(AlarmSettings alarmSettings, Cursor cur) {
        if (cur.moveToFirst()) {
            do {
                // Get the field values
                int id = cur.getInt(AlarmColumns.ALARM_ID_INDEX);
                int hour = cur.getInt(AlarmColumns.ALARM_HOUR_INDEX);
                int minutes = cur.getInt(AlarmColumns.ALARM_MINUTES_INDEX);
                long alarmtime = cur.getLong(AlarmColumns.ALARM_TIME_INDEX);
                int daysOfWeek = cur.getInt(AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX);
                boolean enabled = cur.getInt(AlarmColumns.ALARM_ENABLED_INDEX) == 1 ? true : false;
                boolean vibrate = cur.getInt(AlarmColumns.ALARM_VIBRATE_INDEX) == 1 ? true : false;
                String message = cur.getString(AlarmColumns.ALARM_MESSAGE_INDEX);
                String alert = cur.getString(AlarmColumns.ALARM_ALERT_INDEX);
                boolean snoozed = cur.getInt(AlarmColumns.ALARM_SNOOZED_INDEX) == 1 ? true : false;
                boolean offalarm = cur.getInt(AlarmColumns.ALARM_OFFALARM_INDEX) == 1 ? true : false;
                int repeat_type = cur.getInt(AlarmColumns.ALARM_REPEAT_TYPE_INDEX);
                alarmSettings.reportAlarm(
                    id, enabled, hour, minutes, alarmtime, new DaysOfWeek(daysOfWeek),
                    vibrate, message, alert, snoozed, offalarm, repeat_type);

            } while (cur.moveToNext());
        }
    }

    /**
     * Calls the AlarmSettings.reportAlarm interface on all alarms found in db.
     */
    public synchronized static void offAlarmGetAlarms(ContentResolver contentResolver, AlarmSettings alarmSettings) {
        Cursor cursor = getAlarmsCursor(contentResolver);
        if (cursor != null) {
            offAlarmGetAlarms(alarmSettings, cursor);
            if (cursor.isClosed() == false) {
                cursor.close();
            }
        }
    }

    private synchronized static void offAlarmGetAlarms(AlarmSettings alarmSettings, Cursor cur) {
        if (cur.moveToFirst()) {
            do {
                // Get the field values
                int id = cur.getInt(AlarmColumns.ALARM_ID_INDEX);
                int hour = cur.getInt(AlarmColumns.ALARM_HOUR_INDEX);
                int minutes = cur.getInt(AlarmColumns.ALARM_MINUTES_INDEX);
                long alarmtime = cur.getLong(AlarmColumns.ALARM_TIME_INDEX);
                int daysOfWeek = cur.getInt(AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX);
                boolean enabled = cur.getInt(AlarmColumns.ALARM_ENABLED_INDEX) == 1 ? true : false;
                boolean vibrate = cur.getInt(AlarmColumns.ALARM_VIBRATE_INDEX) == 1 ? true : false;
                String message = cur.getString(AlarmColumns.ALARM_MESSAGE_INDEX);
                String alert = cur.getString(AlarmColumns.ALARM_ALERT_INDEX);
                boolean snoozed = cur.getInt(AlarmColumns.ALARM_SNOOZED_INDEX) == 1 ? true : false;
                boolean offalarm = cur.getInt(AlarmColumns.ALARM_OFFALARM_INDEX) == 1 ? true : false;
                int repeat_type = cur.getInt(AlarmColumns.ALARM_REPEAT_TYPE_INDEX);
                if (true == offalarm) {
                    alarmSettings.reportAlarm(
                        id, enabled, hour, minutes, alarmtime, new DaysOfWeek(daysOfWeek),
                        vibrate, message, alert, snoozed, offalarm, repeat_type);
                }

            } while (cur.moveToNext());
        }
    }

    public synchronized static void getAlarmByAlarmIdx(ContentResolver contentResolver, AlarmSettings alarmSetting, int alarmIdx) {
        Cursor cur = null;
        int i;

        // check contentResolver
        if (contentResolver == null) {
            if (DEBUG_FLAG) Log.d(TAG, "contentResolver is null");
            return;
        }
        
        try {
            cur = AlarmUtils.getAlarmsCursor(contentResolver);
            if ((cur != null) && (cur.moveToFirst())) {
                for (i = 0; i < (alarmIdx - 1); i++) {
                    cur.moveToNext();
                }
                // Get the field values
                int id = cur.getInt(AlarmColumns.ALARM_ID_INDEX);
                int hour = cur.getInt(AlarmColumns.ALARM_HOUR_INDEX);
                int minutes = cur.getInt(AlarmColumns.ALARM_MINUTES_INDEX);
                long alarmtime = cur.getLong(AlarmColumns.ALARM_TIME_INDEX);
                int daysOfWeek = cur.getInt(AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX);
                boolean enabled = cur.getInt(AlarmColumns.ALARM_ENABLED_INDEX) == 1 ? true : false;
                boolean vibrate = cur.getInt(AlarmColumns.ALARM_VIBRATE_INDEX) == 1 ? true : false;
                String message = cur.getString(AlarmColumns.ALARM_MESSAGE_INDEX);
                String alert = cur.getString(AlarmColumns.ALARM_ALERT_INDEX);
                boolean snoozed = cur.getInt(AlarmColumns.ALARM_SNOOZED_INDEX) == 1 ? true : false;
                boolean offalarm = cur.getInt(AlarmColumns.ALARM_OFFALARM_INDEX) == 1 ? true : false;
                int repeat_type = cur.getInt(AlarmColumns.ALARM_REPEAT_TYPE_INDEX);
                alarmSetting.reportAlarm(
                    id, enabled, hour, minutes, alarmtime, new DaysOfWeek(daysOfWeek),
                    vibrate, message, alert, snoozed, offalarm, repeat_type);
            }

        } catch (Exception e) {
            Log.w(TAG, "getAlarmByAlarmIdx: e = " + e.toString());
        } finally {
            if (cur != null) {
                if (cur.isClosed() == false) {
                    cur.close();
                }
            }
        }
    }
    
    /**
     * Calls the AlarmSettings.reportAlarm interface on alarm with given
     * alarmId
     */
    public synchronized static void getAlarm(ContentResolver contentResolver, AlarmSettings alarmSetting, int alarmId) {
        Cursor cursor = null;

        // check contentResolver
        if (contentResolver == null) {
            if (DEBUG_FLAG) Log.d(TAG, "contentResolver is null");
            return;
        }
        try {
            cursor = contentResolver.query(
                ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, alarmId),
                AlarmColumns.ALARM_QUERY_COLUMNS,
                null, null, AlarmColumns.DEFAULT_SORT_ORDER);
            getAlarms(alarmSetting, cursor);
        } catch (Exception e) {
            Log.w(TAG, "getAlarm: e = " + e.toString());
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }
    }

    /**
     * A convenience method to set an alarm in the AlarmUtils
     * content provider.
     * 
     * @param id
     *            corresponds to the _id column
     * @param enabled
     *            corresponds to the ENABLED column
     * @param hour
     *            corresponds to the HOUR column
     * @param minutes
     *            corresponds to the MINUTES column
     * @param daysOfWeek
     *            corresponds to the DAYS_OF_WEEK column
     * @param time
     *            corresponds to the ALARM_TIME column
     * @param vibrate
     *            corresponds to the VIBRATE column
     * @param message
     *            corresponds to the MESSAGE column
     * @param alert
     *            corresponds to the ALERT column
     * @param snoozed
     *            corresponds to the SNOOZED column
     * @param offalarm
     *            corresponds to the OFFALARM column
     */
    public synchronized static void setAlarm(Context context, int id, boolean enabled, int hour, int minutes,
        DaysOfWeek daysOfWeek, boolean vibrate, String message, String alert, boolean snoozed, boolean offalarm, int repeat_type) {
        // ATS log
        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][set_time][complete]");
        ContentValues values = new ContentValues(9);
        ContentResolver resolver = context.getContentResolver();
        long time = calculateAlarm(hour, minutes, daysOfWeek, repeat_type).getTimeInMillis();

        if (DEBUG_FLAG) {
            Log.d(TAG, "setAlarm: idx = " + id + ", hour = " + hour + ", minutes = " + minutes +
                ", vibrate = " + vibrate + ", message = \"" + message + "\", alert = \"" + alert +
                "\", snoozed = " + snoozed + ", offalarm = " + offalarm + ", mask = " + Integer.toBinaryString(daysOfWeek.getCoded()) +
                ", enabled = " + enabled + ", repeat_type = " + repeat_type + ", time = " + time + "(" + new Date(time) + ")");
        }

        values.put(AlarmColumns.ENABLED, enabled ? 1 : 0);
        values.put(AlarmColumns.HOUR, hour);
        values.put(AlarmColumns.MINUTES, minutes);
        values.put(AlarmColumns.ALARM_TIME, time);
        values.put(AlarmColumns.DAYS_OF_WEEK, daysOfWeek.getCoded());
        values.put(AlarmColumns.VIBRATE, vibrate);
        values.put(AlarmColumns.MESSAGE, message);
        values.put(AlarmColumns.ALERT, alert);
        values.put(AlarmColumns.SNOOZED, snoozed);
        values.put(AlarmColumns.OFFALARM, offalarm);
        values.put(AlarmColumns.REPEAT_TYPE, repeat_type);
        int count = resolver.update(ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, id),
            values, null, null);
        BackupManager.dataChanged(context.getPackageName());
        PreferencesUtil.setBackupAlarmDB(context, false);
        PreferencesUtil.setSyncAlarmClockDB(context, false);

        if (count != -1) {

            if (snoozed) {
                disableSnoozeAlert(context, id);
            }

            // cancel notification
            AlertUtils.cancelAlarmSnoozedNotification(context, id);

            setNextAlert(context);
        } else {
            Log.w(TAG, "setAlarm: Alarm not set");
        }
    }

    /**
     * A convenience method to enable or disable an alarm.
     * 
     * @param id
     *            corresponds to the _id column
     * @param enabled
     *            corresponds to the ENABLED column
     */

    public synchronized static void enableAlarm(final Context context, final int id, boolean enabled) {
        disableSnoozeAlert(context, id);

        enableAlarmInternal(context, id, enabled);
        setNextAlert(context);
    }

    public synchronized static void enableAheadNotification(final Context context, final int id, boolean enabled) {
        Log.d("juan", "enableAheadNotification");
        ContentResolver resolver = context.getContentResolver();
        class EnableAlarm implements AlarmSettings {
            public int mHour;
            public int mMinutes;
            public DaysOfWeek mDaysOfWeek;
            public int mRepeatType;

            @Override
            public void reportAlarm(
                    int idx, boolean enabled, int hour, int minutes, long alarmtime,
                    DaysOfWeek daysOfWeek, boolean vibrate, String message,
                    String alert, boolean snoozed, boolean offalarm, int repeat_type) {
                mHour = hour;
                mMinutes = minutes;
                mDaysOfWeek = daysOfWeek;
                mRepeatType = repeat_type;
            }
        }

        if (enabled) {
            EnableAlarm enableAlarm = new EnableAlarm();
            getAlarm(resolver, enableAlarm, id);
            if (enableAlarm.mDaysOfWeek == null) {
                /*
                 * Under monkey, sometimes reportAlarm is never
                 * called
                 */
                Log.w(TAG, "enableAheadNotification: failed " + id + " h " +
                        enableAlarm.mHour + " m " + enableAlarm.mMinutes);
                return;
            }
            //for test
            Log.w("juan", "enableAheadNotification: failed " + id + " h " +
                    enableAlarm.mHour + " m " + enableAlarm.mMinutes);
            int aheadTime = enableAlarm.mMinutes -2;
            long time = calculateAlarm(enableAlarm.mHour, aheadTime,
                    enableAlarm.mDaysOfWeek, enableAlarm.mRepeatType).getTimeInMillis();
            Log.w("juan", "enableAheadNotification: failed " + id + " h " +
                    enableAlarm.mHour + " m " + aheadTime);
            //alarmManager
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(ACTION_ALARM_NOTICE);
            intent.setClass(context, AlarmReminderReceiver.class);
            intent.putExtra(ID, id);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);

        }
    }

    private synchronized static void _enableAlarmInternal(final Context context, final int id, boolean enabled, AlarmData data) {
        ContentResolver resolver = context.getContentResolver();

        class EnableAlarm implements AlarmSettings {
            public int mHour;
            public int mMinutes;
            public DaysOfWeek mDaysOfWeek;
            public int mRepeatType;

            @Override
            public void reportAlarm(
                int idx, boolean enabled, int hour, int minutes, long alarmtime,
                DaysOfWeek daysOfWeek, boolean vibrate, String message,
                String alert, boolean snoozed, boolean offalarm, int repeat_type) {
                mHour = hour;
                mMinutes = minutes;
                mDaysOfWeek = daysOfWeek;
                mRepeatType = repeat_type;
            }
        }

        data.enabled = enabled ? 1 : 0;

        /*
         * If we are enabling the alarm, load hour/minutes/daysOfWeek
         * from db, so we can calculate alarm time
         */
        if (enabled) {
            EnableAlarm enableAlarm = new EnableAlarm();
            getAlarm(resolver, enableAlarm, id);
            if (enableAlarm.mDaysOfWeek == null) {
                /*
                 * Under monkey, sometimes reportAlarm is never
                 * called
                 */
                Log.w(TAG, "_enableAlarmInternal: failed " + id + " h " +
                    enableAlarm.mHour + " m " + enableAlarm.mMinutes);
                return;
            }

            long time = calculateAlarm(enableAlarm.mHour, enableAlarm.mMinutes,
                enableAlarm.mDaysOfWeek, enableAlarm.mRepeatType).getTimeInMillis();

            data.time = time;
        }
    }

    private synchronized static void enableAlarmInternal(final Context context, final int id, boolean enabled) {
        ContentResolver resolver = context.getContentResolver();

        class EnableAlarm implements AlarmSettings {
            public int mHour;
            public int mMinutes;
            public DaysOfWeek mDaysOfWeek;
            public int mRepeatType;

            @Override
            public void reportAlarm(
                int idx, boolean enabled, int hour, int minutes, long alarmtime,
                DaysOfWeek daysOfWeek, boolean vibrate, String message,
                String alert, boolean snoozed, boolean offalarm, int repeat_type) {
                mHour = hour;
                mMinutes = minutes;
                mDaysOfWeek = daysOfWeek;
                mRepeatType = repeat_type;
            }
        }

        ContentValues values = new ContentValues(2);
        values.put(AlarmColumns.ENABLED, enabled ? 1 : 0);

        /*
         * If we are enabling the alarm, load hour/minutes/daysOfWeek
         * from db, so we can calculate alarm time
         */
        if (enabled) {
            EnableAlarm enableAlarm = new EnableAlarm();
            getAlarm(resolver, enableAlarm, id);
            if (enableAlarm.mDaysOfWeek == null) {
                /*
                 * Under monkey, sometimes reportAlarm is never
                 * called
                 */
                Log.w(TAG, "enableAlarmInternal: failed " + id + " h " +
                    enableAlarm.mHour + " m " + enableAlarm.mMinutes);
                return;
            }

            long time = calculateAlarm(enableAlarm.mHour, enableAlarm.mMinutes, enableAlarm.mDaysOfWeek, enableAlarm.mRepeatType).getTimeInMillis();
            values.put(AlarmColumns.ALARM_TIME, time);
        } else {
            // cancel notification
            AlertUtils.cancelAlarmSnoozedNotification(context, id);
        }

        try {
            resolver.update(ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, id), values, null, null);
            BackupManager.dataChanged(context.getPackageName());
            PreferencesUtil.setBackupAlarmDB(context, false);
            PreferencesUtil.setSyncAlarmClockDB(context, false);
        } catch (Exception e) {
            Log.w(TAG, "enableAlarmInternal: fail e = " + e.toString());
        }
    }

    /**
     * Calculates next scheduled alert
     */
    static class AlarmCalculator implements AlarmSettings {
        public long mMinAlert = Long.MAX_VALUE;
        public int mMinIdx = -1;
        private String mDescription;
        boolean mSnoozed = false;
        //for alarm clock ring sounds uri
        private String mAlertSoundsUri;
        private Context mContext;

        public AlarmCalculator(Context context) {
            mContext = context;
        }

        /**
         * returns next scheduled alert, MAX_VALUE if none
         */
        public long getAlert() {
            return mMinAlert;
        }

        public int getIndex() {
            return mMinIdx;
        }

        public String getDescription() {
            return mDescription;
        }

        public boolean getSnoozed() {
            return mSnoozed;
        }

        public String getAlertSoundsUri() {
            return mAlertSoundsUri;
        }

        @Override
        public void reportAlarm(int idx, boolean enabled, int hour, int minutes, long alarmtime,
            DaysOfWeek daysOfWeek, boolean vibrate, String message, String alert, boolean snoozed, boolean offalarm, int repeat_type) {
            if (enabled) {
                // s: update by Tiffanie
                long atTime = calculateAlarm(hour, minutes, daysOfWeek, repeat_type).getTimeInMillis();
                //calculate skip alarm alert
                AiUtils aiManager = AiUtils.getInstance(mContext);
                ArrayList<AlarmItem> skipAlarms = aiManager.getSkipAlarms();
                long startTime = aiManager.getSkipStartTime();
                long endTime = aiManager.getSkipEndTime();
                boolean isSkipAlarm = AiUtils.checkIsAiAlarmId(skipAlarms, idx);
                if (isSkipAlarm) {
                    long skipTime = AiUtils.calculateSkipAlarm(hour, minutes, daysOfWeek, startTime, endTime).getTimeInMillis();
                    if (atTime < skipTime) {
                        atTime = skipTime;
                    }
                }
                if (snoozed) {
                    long now = System.currentTimeMillis();
                    // normal snoozed alarm
                    if ((alarmtime > now) && (alarmtime < atTime)) {
                        atTime = alarmtime;
                    }
                }
                // e
                if (DEBUG_FLAG) {
                    Log.d(TAG, "reportAlarm: idx = " + idx + ", hour = " + hour + ", minutes = " +
                        minutes + ", vibrate = " + vibrate + ", message = \"" + message + "\", alert = \"" + alert +
                        "\", snoozed = " + snoozed + ", offalarm = " + offalarm + ", mask = " + Integer.toBinaryString(daysOfWeek.getCoded()) +
                        ", enabled = " + enabled + ", repeat_type = " + repeat_type + ", time = " + atTime + "(" + new Date(atTime) + ")");
                }

                if (atTime < mMinAlert) {
                    mMinIdx = idx;
                    mMinAlert = atTime;
                    mDescription = message;
                    mSnoozed = snoozed;
                    mAlertSoundsUri = alert;
                }

//               Arrays.sort(new long[]{mMinAlert});
//                if (atTime < mMinAlert){
//                    if (mMinIdx > 1) {
//                        mMinIdx = idx;
//                        mMinAlert = atTime;
//                        mDescription = message;
//                        mSnoozed = snoozed;
//                        mAlertSoundsUri = alert;
//                    }
//                }
            }
        }
    }

    static AlarmCalculator calculateNextAlert(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        AlarmCalculator alarmCalc = new AlarmCalculator(context);
        //check next alert early event
        AiUtils.calculateEarlyEvent(context,alarmCalc);
        getAlarms(resolver, alarmCalc);
        return alarmCalc;
    }

    static AlarmCalculator offAlarmCalculateNextAlert(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        AlarmCalculator alarmCalc = new AlarmCalculator(context);
        offAlarmGetAlarms(resolver, alarmCalc);
        return alarmCalc;
    }
    
    /**
     * Disables non-repeating alarms that have passed. Called at
     * boot.
     */
    public static void disableExpiredAlarms(final Context context) {
        Cursor cur = getAlarmsCursor(context.getContentResolver());
        long now = System.currentTimeMillis();

        if (cur == null) {
            Log.w(TAG, "disableExpiredAlarms: cur = null");
            return;
        }

        ArrayList<String> ids = new ArrayList<String>();
        ArrayList<String> alarm_times = new ArrayList<String>();
        ArrayList<String> enables = new ArrayList<String>();
        ArrayList<String> snoozeIds = new ArrayList<String>();
        if (cur.moveToFirst()) {
            do {
                // Get the field values
                int id = cur.getInt(AlarmColumns.ALARM_ID_INDEX);
                boolean enabled = cur.getInt(
                    AlarmColumns.ALARM_ENABLED_INDEX) == 1 ? true : false;
                DaysOfWeek daysOfWeek = new DaysOfWeek(
                    cur.getInt(AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX));
                long time = cur.getLong(AlarmColumns.ALARM_TIME_INDEX);
                boolean snoozed = cur.getInt(AlarmColumns.ALARM_SNOOZED_INDEX) == 1 ? true : false;

                if (enabled && (time < now)) {
                    if (DEBUG_FLAG) Log.d(TAG, "disableExpiredAlarms: DISABLE " + id + " now " + now + " set " + time);
                    if (!daysOfWeek.isRepeatSet()) {
                        AlarmData data = new AlarmData();
                        _enableAlarmInternal(context, id, false, data);
                        ids.add(Integer.toString(id));
                        alarm_times.add(Long.toString(data.time));
                        enables.add(Integer.toString(data.enabled));
                    }
                    if (snoozed) {
                        snoozeIds.add(Integer.toString(id));
                    }
                }
            } while (cur.moveToNext());
        }

        if (cur.isClosed() == false) {
            cur.close();
        }
        // enable alarm part
        ContentValues values = new ContentValues();
        int idsSize = ids.size();
        for (int i = 0; i < idsSize; i++) {
            values.put(AlarmColumns._ID, ids.get(i));
        }

        int alarmTimeSize = alarm_times.size();
        for (int i = 0; i < alarmTimeSize; i++) {
            values.put(AlarmColumns.ALARM_TIME, alarm_times.get(i));
        }

        int enablesSize = enables.size();
        for (int i = 0; i < enablesSize; i++) {
            values.put(AlarmColumns.ENABLED, enables.get(i));
        }

        ContentResolver cr = context.getContentResolver();
        cr.update(ALARMS_ENABLE_BULK_URI, values, null, null);
        BackupManager.dataChanged(context.getPackageName());
        PreferencesUtil.setBackupAlarmDB(context, false);
        PreferencesUtil.setSyncAlarmClockDB(context, false);

        // snooze part
        ContentValues snoozeValues = new ContentValues();

        int snoozeIdsSize = snoozeIds.size();
        for (int i = 0; i < snoozeIdsSize; i++) {
            snoozeValues.put(AlarmColumns._ID, snoozeIds.get(i));
        }

        ContentResolver snoozeCr = context.getContentResolver();
        snoozeCr.update(ALARMS_SNOOZE_BULK_URI, values, null, null);
        BackupManager.dataChanged(context.getPackageName());
        PreferencesUtil.setBackupAlarmDB(context, false);
        PreferencesUtil.setSyncAlarmClockDB(context, false);
    }

    /**
     * Called at system startup, on time/timezone change, and whenever
     * the user changes alarm settings. Activates snooze if set,
     * otherwise loads all alarms, activates next alert.
     */
    public static void setNextAlert(final Context context) {
        // 1. Must call initialize() once at first time to prepare holiday data before getting holiday data
        if (Global.isSupportAccChinaSense()) {
            ChinaHolidayUtil.getInstance().initialize(context);// blocking code to query db
        }
        AlarmCalculator ac = calculateNextAlert(context);
        int id = ac.getIndex();
        long atTime = ac.getAlert();
        String alert = ac.getAlertSoundsUri();
        Log.i(TAG, "Calculate next alarm: id = " + id + " time = " + atTime + "(" + new Date(atTime) + ")");
        Log.i(AiUtils.AI_TAG, "Calculate next alarm: id = " + id + " time = " + atTime + "(" + new Date(atTime) + ")");
        AiUtils.clearSkipAlarm(context, atTime);
        if (atTime < Long.MAX_VALUE) {
            enableAlert(context, id, ac.getDescription(), atTime, alert);
        } else {
            disableAlert(context, id);
        }
        
        setNextOffAlarm(context);
    }

    public static void setNextAlertNot(final Context context) {
        // 1. Must call initialize() once at first time to prepare holiday data before getting holiday data
        if (Global.isSupportAccChinaSense()) {
            ChinaHolidayUtil.getInstance().initialize(context);// blocking code to query db
        }
        AlarmCalculator ac = calculateNextAlert(context);
        int id = ac.getIndex();
        long atTime = ac.getAlert();
        String alert = ac.getAlertSoundsUri();
        Log.i(TAG, "Calculate next alarm: id = " + id + " time = " + atTime + "(" + new Date(atTime) + ")");
        Log.i(AiUtils.AI_TAG, "Calculate next alarm: id = " + id + " time = " + atTime + "(" + new Date(atTime) + ")");
        AiUtils.clearSkipAlarm(context, atTime);
        if (atTime < Long.MAX_VALUE) {
            enableAlert(context, id, ac.getDescription(), atTime, alert);
        } else {
            disableAlert(context, id);
        }

        setNextOffAlarm(context);
    }

    private static void setNextOffAlarm(final Context context) {
        AlarmCalculator offalarm_ac = offAlarmCalculateNextAlert(context);
        int offalarm_id = offalarm_ac.getIndex();
        long offalarm_atTime = offalarm_ac.getAlert();
        Log.i(TAG, "Calculate next off alarm: id = " + offalarm_id + " time = " + offalarm_atTime + "(" + new Date(offalarm_atTime) + ")");
        final boolean isDeviceEncryptionEnabled = PreferencesUtil.getDeviceEncryption(context);
        Log.i(TAG, "isDeviceEncryptionEnabled = " + isDeviceEncryptionEnabled);
        if (isDeviceEncryptionEnabled) {
            Settings.System.putLong(context.getContentResolver(), "offalarm", INVALID_ALARMID);
        } else {
            Settings.System.putLong(context.getContentResolver(), "offalarm", offalarm_atTime);
        }
        if (DEBUG_FLAG) Log.d(TAG, "setNextOffAlarm: offalarm_id = " + offalarm_id + ", offalarm_atTime = " + offalarm_atTime + "(" + new Date(offalarm_atTime) + ")" + ", offalarm_ac.getDescription() = " + offalarm_ac.getDescription());
        if (!isDeviceEncryptionEnabled) {
            PreferencesUtil.setNextOffAlarmId(context, offalarm_id);
            PreferencesUtil.setNextOffAlarmTime(context, offalarm_atTime);
            PreferencesUtil.setNextOffAlarmDescription(context, offalarm_ac.getDescription());
            writeOffAlarmData(context, offalarm_id, offalarm_atTime, isDeviceEncryptionEnabled);
        }
    }

    /**
     * Sets alert in AlarmManger and StatusBar. This is what will
     * actually launch the alert when the alarm triggers.
     * 
     * Note: In general, apps should call setNextAlert() instead of
     * this method. setAlert() is only used outside this class when
     * the alert is not to be driven by the state of the db. "Snooze"
     * uses this API, as we do not want to alter the alarm in the db
     * with each snooze.
     * 
     * @param id
     *            Alarm ID.
     * @param atTimeInMillis
     *            milliseconds since epoch
     */
    @TargetApi(21)
    static void enableAlert(Context context, int id, String description, long atTimeInMillis, String alert) {
        if (DEBUG_FLAG) Log.d(TAG, "enableAlert: id = " + id + " time = " + atTimeInMillis + "(" + new Date(atTimeInMillis) + ")");
        Intent intent = new Intent(ACTION_ALARM_ALERT);
        intent.setClass(context, AlarmReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(ID, id);
        if ((description == null) || description.equals("")) {
            intent.putExtra(DESCRIPTION, "");
        } else {
            intent.putExtra(DESCRIPTION, description);
        }
        //add alert
        intent.putExtra(ALERT,alert);
        intent.putExtra(TIME, atTimeInMillis);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
            // Create an intent that can be used to show or edit details of the next alarm.
            Intent launchAlarmClockIntent = new Intent(Intent.ACTION_MAIN);
            launchAlarmClockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchAlarmClockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchAlarmClockIntent.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_ALARM);
            launchAlarmClockIntent.setClassName(context.getPackageName(), WorldClockTabControl.LAUNCH_AP_ACTIVITY_NAME);
            PendingIntent viewIntent = PendingIntent.getActivity(context, 0, launchAlarmClockIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(atTimeInMillis, viewIntent);
            am.setAlarmClock(info, sender);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        }
        
        Log.i(AiUtils.AI_TAG, "Set alarm to alarm manager: id = " + id + " time = " + atTimeInMillis + "(" + new Date(atTimeInMillis) + ")");
        Log.i(TAG, "Set alarm to alarm manager: id = " + id + " time = " + atTimeInMillis + "(" + new Date(atTimeInMillis) + ")");
        setStatusBarIcon(context, true);

        // Modify by Andrew, Liu 2008/12/24 (HTC), +Kun, rollback code
        // don't convert to DateTime formate to store string into DB,
        // should keep long value of time.
        // Fixed for ITS#3733
        Calendar c = Calendar.getInstance();
        c.setTime(new java.util.Date(atTimeInMillis));
        String timeString = formatDayAndTime(context, c);
        // String timeString = Long.toString(atTimeInMillis);
        saveNextAlarm(context, id, description, timeString, atTimeInMillis);
        // End by Andrew, Liu (HTC)
    }

    /**
     * Disables alert in AlarmManger and StatusBar.
     * 
     * @param id
     *            Alarm ID.
     */
    public static void disableAlert(Context context, int id) {
        if (DEBUG_FLAG) Log.d(TAG, "disableAlert: id = " + id);
        Intent intent = new Intent(ACTION_ALARM_ALERT);
        intent.setClass(context, AlarmReceiver.class);
        intent.putExtra(ID, id);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
        Log.i(TAG, "Cancel any alarm from alarm manager");
        setStatusBarIcon(context, false);
        saveNextAlarm(context, id, "", "", INVALID_ALARMTIME);
    }

    // +Kun,support off-alarm
    public static void writeOffAlarmData(final Context context, final int alarmId, final long alarmTime, final boolean isDeviceEncryptionEnabled) {
        if (!Global.isSupportAccChinaSense()) {
            return;
        }
        
        Log.i(TAG, "writeOffAlarmData: isDeviceEncryptionEnabled = " + isDeviceEncryptionEnabled);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Long> list = new ArrayList<Long>();
                if ((isDeviceEncryptionEnabled) || (alarmId == AlarmUtils.INVALID_ALARMID)) {
                    Log.i(TAG, "writeOffAlarmData: clear offalarm's data");
                    list.add(0L);
                    list.add(0L);
                    list.add(0L);
                    list.add(0L);
                    list.add(0L);
                    list.add(0L);
                } else {
                    list.add(Long.valueOf(alarmId));
                    list.add(alarmTime / 1000);
                    list.add(0L);
                    list.add(1L);
                    list.add(0L);
                    list.add(0L);
                }
                DmdCmd dmdCmd = new DmdCmd();
                if (dmdCmd.Conn()) {
                    Long[] data = list.toArray(new Long[0]);
                    if (DEBUG_FLAG) Log.d(TAG, "writeOffAlarmData: alarm id = " + data[0]);
                    if (DEBUG_FLAG) Log.d(TAG, "writeOffAlarmData: alarm time = " + data[1] + "(" + new Date(data[1] * 1000) + ")");
                    if (DEBUG_FLAG) Log.d(TAG, "writeOffAlarmData: alarm mask = " + data[2]);
                    if (DEBUG_FLAG) Log.d(TAG, "writeOffAlarmData: alarm enabled = " + data[3]);
                    if (DEBUG_FLAG) Log.d(TAG, "writeOffAlarmData: alarm snooze flag = " + data[4]);
                    if (DEBUG_FLAG) Log.d(TAG, "writeOffAlarmData: alarm snooze time = " + data[5] + "(" + new Date(data[5] * 1000) + ")");
                    dmdCmd.writeData(convertLongToByte(data)); // convert time to seconds
                    dmdCmd.DisConn();
                }
            }
        });
        thread.start();
    }
    
    public static byte[] convertLongToByte(Long[] data) {
        int size = 4;
        byte[] out = new byte[data.length * size];
        long value;
        int i, j;

        for (i = 0; i < data.length; i++) {
            value = data[i].longValue();
            for (j = 0; j < size; j++) {
                out[(i * size) + j] = (byte) (value >> (8 * j));

            }
        }
        return out;
    }

    // -Kun

    static void saveSnoozeAlert(final Context context, int id, String description, long atTimeInMillis) {
        //update early event alarm snooze data
        if (AiUtils.getInstance(context).checkIsEarlyAlarmId(id) && atTimeInMillis != 0) {
            ArrayList<AlarmItem> arrayList = AiUtils.getInstance(context).getEarlyEventAlarms();
            Log.i(AiUtils.AI_TAG, "saveSnoozeAlert update early event alarms size: " + arrayList.size());
            for(int i = 0; i < arrayList.size(); i++) {
                if (arrayList.get(i).aId == id) {
                    arrayList.get(i).aAlertTime = atTimeInMillis;
                    arrayList.get(i).aSnoozed = true;
                }
            }
            AiUtils.getInstance(context).setEarlyEventAlarms(arrayList);
            return;
        }
        // s: added by Tiffanie
        ContentValues values = new ContentValues(2);
        if (atTimeInMillis > 0) {
            values.put(AlarmColumns.SNOOZED, 1);
            values.put(AlarmColumns.ALARM_TIME, atTimeInMillis);
        } else {
            values.put(AlarmColumns.SNOOZED, 0);
        }
        try {
            // to prevent update data fail
            context.getContentResolver().update(ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, id), values, null, null);
            BackupManager.dataChanged(context.getPackageName());
            PreferencesUtil.setBackupAlarmDB(context, false);
            PreferencesUtil.setSyncAlarmClockDB(context, false);
        } catch (Exception e) {
            Log.w(TAG, "saveSnoozeAlert: fail e = " + e.toString());
        }
        // e
    }

    /**
     * @return ID of alarm disabled
     */
    public static int disableSnoozeAlert(final Context context, int id) {
        if (id == -1) {
            return -1;
        }
        saveSnoozeAlert(context, id, null, 0);
        return id;
    }

    /**
     * Tells the StatusBar whether the alarm is enabled or disabled
     */
    private static void setStatusBarIcon(Context context, boolean enabled) {
        Intent alarmChanged = new Intent(reflectAlarmChangedField());
        alarmChanged.putExtra("alarmSet", enabled);
        context.sendBroadcast(alarmChanged);
    }

    private static String reflectAlarmChangedField() {
        // hide field from google framework
        final String CLASS_NAME = "android.content.Intent";
        String retValue = "";

        try {
            Class<?> c = null;
            c = Class.forName(CLASS_NAME);
            Field alarmChangedField = c.getDeclaredField("ACTION_ALARM_CHANGED");
            // this call allows private fields to be accessed via reflection
            alarmChangedField.setAccessible(true);
            retValue =  (String) alarmChangedField.get(retValue);
        } catch (Exception e) {
            Log.w(TAG, "reflectAlarmChangedField: e = " + e.toString());
        }
        Log.i(TAG, "broadcast \"" + retValue + "\" intent for alarm icon");
        return retValue;
    }
    
    private static Calendar calculateNextSkipHoliday(Calendar cal) {
        try {
            boolean isHoliday =false;
            do {
                long time = cal.getTimeInMillis();
                if (DEBUG_FLAG) Log.d(TAG, "calculateNextSkipHoliday: Checking day by day -> time = " + time + "(" + new Date(time) + ")");
                // start to check if it's holiday
                isHoliday = ChinaHolidayUtil.getInstance().isHoliday(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),cal.get(Calendar.DATE));
                if (DEBUG_FLAG) Log.d(TAG, "calculateNextSkipHoliday: isHoliday = " + isHoliday);
                // start to get display name
                String display_name = ChinaHolidayUtil.getInstance().getHoliday(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),cal.get(Calendar.DATE));
                if (DEBUG_FLAG) Log.d(TAG, "calculateNextSkipHoliday: holiday display name = '" + display_name + "'");
                if (!isHoliday) {
                    break;
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            } while(isHoliday);
            long time = cal.getTimeInMillis();
            if (DEBUG_FLAG) Log.d(TAG, "calculateNextSkipHoliday: Next skip holiday alarm : time = " + time + "(" + new Date(time) + ")");
        } catch (Exception e) {
            Log.w(TAG, "calculateNextSkipHoliday: e = " + e.toString());
        }
        
        return cal;
    }
    
    /**
     * Given an alarm in hours and minutes, return a time suitable for
     * setting in AlarmManager.
     * 
     * @param hour
     *            Always in 24 hour 0-23
     * @param minute
     *            0-59
     * @param daysOfWeek
     *            0-59
     */
    public static Calendar calculateAlarm(int hour, int minute, DaysOfWeek daysOfWeek, int repeat_type) {
        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if ((hour < nowHour) ||
            ((hour == nowHour) && (minute <= nowMinute))) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if ((Global.isSupportAccChinaSense()) && (RepeatTypeEnum.SKIPHOLIDAY.ordinal() == repeat_type)) {
            c = calculateNextSkipHoliday(c);
        } else {
            int addDays = daysOfWeek.getNextAlarm(c);
            /*
             * HtcLog.d("** TIMES * " + c.getTimeInMillis() + " hour " + hour +
             * " minute " + minute + " dow " + c.get(Calendar.DAY_OF_WEEK) + " from now " +
             * addDays);
             */
            if (addDays > 0) {
                c.add(Calendar.DAY_OF_WEEK, addDays);
            }
        }
        
        return c;
    }

    static String formatTime(final Context context, int hour, int minute, DaysOfWeek daysOfWeek, int repeat_type) {
        Calendar c = calculateAlarm(hour, minute, daysOfWeek, repeat_type);
        return formatTime(context, c);
    }

    /* used by AlarmAlert */
    public static String formatTime(final Context context, Calendar c) {
        return (c == null) ? "" : (String) DateFormat.getTimeFormat(context).format(c.getTime());
    }

    /**
     * used in Desk Clock
     */
    public static String formatDayAndTime(final Context context, long atTimeInMillis) {
        Calendar c = Calendar.getInstance();
        c.setTime(new java.util.Date(atTimeInMillis));
        return formatDayAndTime(context, c);
    }

    public static String formatTime(final Context context, long atTimeInMillis) {
        Calendar c = Calendar.getInstance();
        c.setTime(new java.util.Date(atTimeInMillis));
        return formatTime(context, c);
    }

    /**
     * Shows day and time -- used for pattern lock , ex: Wed 9:00 am
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        if (c == null) {
            return "";
        }
        String weekday = (String) DateFormat.format("E ", c);
        String timeString = weekday + DateFormat.getTimeFormat(context).format(c.getTime());
        return timeString;
    }

    /**
     * Save time of the next alarm, as a formatted string, into the system
     * settings so those who care can make use of it.
     */
    static void saveNextAlarm(final Context context, int id, String description, String timeString, long atTimeInMillis) {
        PreferencesUtil.setNextAlarmId(context, id);
        PreferencesUtil.setNextAlarmTime(context, atTimeInMillis);
        PreferencesUtil.setNextAlarmDescription(context, description);

        Settings.System.putLong(context.getContentResolver(),
            NEXT_ALARM_TIME,
            atTimeInMillis);
        Settings.System.putString(context.getContentResolver(),
            Settings.System.NEXT_ALARM_FORMATTED,
            timeString);
    }

    /**
     * upgrade next alarm string type, from long to date
     */
    static void upgradePatternLockNextAlarm(final Context context) {
        String longString = Settings.System.getString(context.getContentResolver(),
            Settings.System.NEXT_ALARM_FORMATTED);
        try {
            long atTimeInMillis = Long.parseLong(longString); // to prevent convert fail
            Calendar c = Calendar.getInstance();
            c.setTime(new java.util.Date(atTimeInMillis));
            String timeString = formatDayAndTime(context, c);
            Settings.System.putString(context.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED, timeString);
        } catch (Exception e) {
        }
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    public static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    /**
     * Extract data from cursor and put them to ArrayList
     */
    public static ArrayList<AlarmItem> getAlarmListData(Context context) {
        ArrayList<AlarmItem> alarmClockList = null;
        Cursor alarmCursor = null;
        try {
            alarmCursor = AlarmUtils.getAlarmsCursor(context.getContentResolver());
        } catch (Exception e) {
            Log.w(TAG, "getAlarmDataList: getAlarmsCursor: e = " + e.toString());
        }

        //add early meeting alarm data
        alarmClockList = AiUtils.getInstance(context).getEarlyEventAlarms();
        if (alarmCursor != null) {
            if (alarmClockList == null) {
                alarmClockList = new ArrayList<AlarmItem>();
            }
            if (alarmCursor.moveToFirst()) {
                boolean next = false;
                do {
                    AlarmItem ai = AlarmUtils.parseCursor(alarmCursor);
                    try {
                        if (alarmClockList != null) {
                            if (ai != null) {
                                if (DEBUG_FLAG) {
                                    Log.d(TAG, "getAlarmListData: idx = " + ai.aId + ", hour = " + ai.aHour + ", minutes = " +
                                        ai.aMinutes + ", vibrate = " + ai.aVibrate + ", message = \"" + ai.aDescription + "\", alert = \"" + ai.aAlert +
                                        "\", snoozed = " + ai.aSnoozed + ", offalarm = " + ai.aOffAlarm + ", mask = " + Integer.toBinaryString(ai.aDaysOfWeek) + ", repeat_type = " + ai.aRepeatType + ", enabled = " + ai.aEnabled);
                                }
                                alarmClockList.add(ai);
                            }
                            next = alarmCursor.moveToNext();
                        } else {
                            next = false;
                        }
                    } catch (Exception e) {
                        if (DEBUG_FLAG) Log.d(TAG, "getAlarmListData: e = " + e.toString());
                        next = false;
                    }
                } while (next);
            }

            if (alarmCursor != null) {
                if (alarmCursor.isClosed() == false) {
                    alarmCursor.close();
                }
                alarmCursor = null;
            }
        } else {
            if (DEBUG_FLAG) Log.d(TAG, "getAlarmListData: alarmCursor = null");
        }
        AiUtils aiManager = AiUtils.getInstance(context);

        ArrayList<AlarmItem> skipArrayList = aiManager.getSkipAlarms();
        ArrayList<AlarmItem> earlyArrayList = aiManager.getEarlyEventAlarms();

        AlarmSorter alarmSorter = AlarmSorter.getInstance(context);
        alarmSorter.convert(skipArrayList, earlyArrayList);
        Collections.sort(alarmClockList, alarmSorter);
        return alarmClockList;
    }

    public enum FlipAction {
        Action_None,
        Action_Snooze,
        Action_Dismiss,
    }

    public static FlipAction getAlarmFlipAction(Context context) {
        final String DEFAULT_FLIP_BEHAVIOR = "2";
        FlipAction mFlipAction = FlipAction.Action_Dismiss;

        // Get the flip phone behavior setting
        final String vol = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_FLIP_BEHAVIOR, DEFAULT_FLIP_BEHAVIOR);

        switch (Integer.parseInt(vol)) {
            case 0:
                mFlipAction = FlipAction.Action_None;
                break;
            case 1:
                mFlipAction = FlipAction.Action_Snooze;
                break;
            case 2:
                mFlipAction = FlipAction.Action_Dismiss;
                break;
        }
        return mFlipAction;
    }

    public enum VolumeBehavior {
        Vol_None,
        Vol_Snooze,
        Vol_Dismiss,
        Vol_Silent
    }
    
    public static VolumeBehavior getAlarmVolumeSideButtonBehavior(Context context) {
        final String DEFAULT_VOLUME_BEHAVIOR = "1";
        VolumeBehavior mVolumeBehavior = VolumeBehavior.Vol_None;
        
        // Get the volume button behavior setting
        final String vol = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_VOLUME_BEHAVIOR, DEFAULT_VOLUME_BEHAVIOR);

        switch (Integer.parseInt(vol)) {
            case 0:
                mVolumeBehavior = VolumeBehavior.Vol_None;
                break;
            case 1:
                mVolumeBehavior = VolumeBehavior.Vol_Snooze;
                break;
            case 2:
                mVolumeBehavior = VolumeBehavior.Vol_Dismiss;
                break;
            case 3:
                mVolumeBehavior = VolumeBehavior.Vol_Silent;
                break;
        }
        return mVolumeBehavior;
    }
    
    public static int getCalendarStartWeekday(Context context) {
        // 1 -- Sunday; 2 -- Monday
        int retVal = 1;
        retVal = PreferencesUtil.getStartWeekDay(context);
        if (DEBUG_FLAG) Log.d(TAG, "getCalendarStartWeekday: start weekday = " + retVal);
        return retVal;
    }
    
    public static void saveSupportOffAlarmInPreference(Context context) {
        boolean isSupport = false;
        if ((Global.isSupportAccChinaSense()) && (!Global.isCMCCSku())) {
            isSupport = Global.isClockdExist();
        }
        PreferencesUtil.setClockdExist(context, isSupport);
        Log.i(TAG, "saveSupportOffAlarmInPreference: isSupport = " + isSupport);
    }
    
    public static boolean isNotShowOffAlarmUI(Context context) {
        boolean isNotShowUI = false;
        boolean isDeviceEncryptionEnabled = PreferencesUtil.getDeviceEncryption(context);
        Log.i(TAG, "isNotShowOffAlarmUI: isDeviceEncryptionEnabled = " + isDeviceEncryptionEnabled);
        boolean isClockdExistInPreference = PreferencesUtil.getClockdExist(context);
        Log.i(TAG, "isNotShowOffAlarmUI: isClockdExistInPreference = " + isClockdExistInPreference);
        
        if ((isDeviceEncryptionEnabled) || (!isClockdExistInPreference) || (!AlertUtils.getCurrentUserIsOwner(context))) {
            isNotShowUI = true;
        }
        Log.i(TAG, "isNotShowOffAlarmUI: isNotShowUI = " + isNotShowUI);
        return isNotShowUI;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#N} or later
     */
    public static boolean isNOrLater() {
        return BuildCompat.isAtLeastN();
    }



}
