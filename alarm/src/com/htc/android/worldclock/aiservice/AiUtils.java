package com.htc.android.worldclock.aiservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.AlarmItem;
import com.htc.android.worldclock.alarmclock.AlarmUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class AiUtils {
    public static final String AI_TAG = "WorldClock.AiLog";
    public static final String AI_ALARM = "ai_alarm";
    public static final String AI_ALARM_SKIP_ALARM_LIST = "skip_alarm_list";
    public static final String AI_ALARM_EARLY_EVENT_LIST = "early_event_list";
    private static final String AI_ALARM_SKIP_START_TIME = "skip_start_time";
    private static final String AI_ALARM_SKIP_END_TIME = "skip_end_time";
    private static final String TEMPORARY_ALARM_ID = "last_event_alarm_id";

    //check enable is true
    private static final int INT_CHECK_ENABLE = 1;

    //check before noon first hour is 0
    private static final int INT_CHECK_FIRST_HOUR = 0;

    //check before noon last hour is 12
    private static final int INT_CHECK_LAST_HOUR = 12;

    //check repeat type only once
    private static final int REPEAT_TYPE = 1;

    private static AiUtils sInstance;
    //app context
    private final Context mContext;

    private AiUtils(Context context) {
        mContext = context;
    }

    public static synchronized AiUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AiUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    /***
     * set array list to jsonObject
     * @param arrayList list data
     * @param  key match key
     * @return JSONObject
     */
    private static JSONObject arrayListToJSON(ArrayList<AlarmItem> arrayList, String key) {
        if(arrayList == null || arrayList.isEmpty()) {
            return null;
        }

        JSONObject aiAlarmList = new JSONObject();
        JSONArray array = new JSONArray();
        try {
            for (int i = 0; i < arrayList.size(); i++) {
                JSONObject aiAlarm = new JSONObject();
                aiAlarm.put(AlarmUtils.AlarmColumns._ID, arrayList.get(i).aId);
                aiAlarm.put(AlarmUtils.AlarmColumns.HOUR, arrayList.get(i).aHour);
                aiAlarm.put(AlarmUtils.AlarmColumns.MINUTES, arrayList.get(i).aMinutes);
                aiAlarm.put(AlarmUtils.AlarmColumns.DAYS_OF_WEEK, arrayList.get(i).aDaysOfWeek);
                aiAlarm.put(AlarmUtils.AlarmColumns.REPEAT_TYPE, arrayList.get(i).aRepeatType);
                aiAlarm.put(AlarmUtils.AlarmColumns.ALERT, arrayList.get(i).aAlert);
                aiAlarm.put(AlarmUtils.AlarmColumns.ENABLED, arrayList.get(i).aEnabled);
                aiAlarm.put(AlarmUtils.AlarmColumns.VIBRATE, arrayList.get(i).aVibrate);
                aiAlarm.put(AlarmUtils.AlarmColumns.ALARM_TIME, arrayList.get(i).aAlertTime);
                aiAlarm.put(AlarmUtils.AlarmColumns.MESSAGE, arrayList.get(i).aDescription);
                aiAlarm.put(AlarmUtils.AlarmColumns.SNOOZED, arrayList.get(i).aSnoozed);
                aiAlarm.put(AlarmUtils.AlarmColumns.OFFALARM, arrayList.get(i).aOffAlarm);
                array.put(i, aiAlarm);
            }
            aiAlarmList.put(key, array);
        } catch (JSONException e) {
            Log.w(AI_TAG, "setArrayListToJSON JSONException :" + e);
            e.printStackTrace();
        }
        return aiAlarmList;
    }

    /**
     * parse jsonObject Data to Array list
     *
     * @param data parse data
     * @param key  match key
     * @return list
     */
    private static ArrayList<AlarmItem> jsonObjectToArray(String data, String key) {
        if(TextUtils.isEmpty(data)) {
            return null;
        }

        ArrayList<AlarmItem> alarmItemArrayList;
        try {
            JSONObject json = new JSONObject(data);
            alarmItemArrayList = new ArrayList<>();
            JSONArray arrayList = json.getJSONArray(key);
            for (int i = 0; i < arrayList.length(); i++) {
                AlarmItem aiAlarm = new AlarmItem();
                JSONObject jsonObject = arrayList.getJSONObject(i);
                int id = jsonObject.getInt(AlarmUtils.AlarmColumns._ID);
                int hour = jsonObject.getInt(AlarmUtils.AlarmColumns.HOUR);
                int minutes = jsonObject.getInt(AlarmUtils.AlarmColumns.MINUTES);
                int daysOfWeek = jsonObject.getInt(AlarmUtils.AlarmColumns.DAYS_OF_WEEK);
                boolean enable = jsonObject.getBoolean(AlarmUtils.AlarmColumns.ENABLED);
                int repeatType = jsonObject.getInt(AlarmUtils.AlarmColumns.REPEAT_TYPE);
                String alert = jsonObject.getString(AlarmUtils.AlarmColumns.ALERT);
                boolean vibrate = jsonObject.getBoolean(AlarmUtils.AlarmColumns.VIBRATE);
                long alertTime = jsonObject.getLong(AlarmUtils.AlarmColumns.ALARM_TIME);
                String message = jsonObject.getString(AlarmUtils.AlarmColumns.MESSAGE);
                boolean snooze = jsonObject.getBoolean(AlarmUtils.AlarmColumns.SNOOZED);
                boolean offAlarm = jsonObject.getBoolean(AlarmUtils.AlarmColumns.OFFALARM);
                aiAlarm.aAlertTime = alertTime;
                aiAlarm.aId = id;
                aiAlarm.aHour = hour;
                aiAlarm.aMinutes = minutes;
                aiAlarm.aDaysOfWeek = daysOfWeek;
                aiAlarm.aEnabled = enable;
                aiAlarm.aAlert = alert;
                aiAlarm.aRepeatType = repeatType;
                aiAlarm.aVibrate = vibrate;
                aiAlarm.aDescription = message;
                aiAlarm.aSnoozed = snooze;
                aiAlarm.aOffAlarm = offAlarm;
                alarmItemArrayList.add(aiAlarm);
            }
        } catch (JSONException e) {
            Log.w(AI_TAG, "setJsonObjectToArray JSONException:" + e);
            alarmItemArrayList = null;
            e.printStackTrace();
        }
        return alarmItemArrayList;
    }

    public long getSkipStartTime() {
        long value;
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        if (settings.contains(AI_ALARM_SKIP_START_TIME)) {
            value = settings.getLong(AI_ALARM_SKIP_START_TIME, -1);
        } else {
            value = -1;
        }
        return value;
    }

    public long getSkipEndTime() {
        long value;
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        if (settings.contains(AI_ALARM_SKIP_END_TIME)) {
            value = settings.getLong(AI_ALARM_SKIP_END_TIME, -1);
        } else {
            value = -1;
        }
        return value;
    }

    /**
     * check id is exist in ai alarms
     *
     * @param AiAlarms ai alarm data
     * @param id check id
     * @return true is exist false :not
     */
    public static boolean checkIsAiAlarmId(ArrayList<AlarmItem> AiAlarms, int id) {
        boolean isAiAlarm = false;
        if (AiAlarms != null && AiAlarms.size() > 0) {
            for (AlarmItem alarm : AiAlarms) {
                if (alarm.aId == id) {
                    isAiAlarm = true;
                    break;
                }
            }
        }
        return isAiAlarm;
    }

    /**
     * get skip alarm resume time
     * @return format String value
     */
    public String getAiSkipResumeTxt() {
        long endTime = getSkipEndTime();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(endTime);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        return mContext.getString(R.string.ai_skip_alarm_resume, month, day);
    }

    /**
     * set early event list in sharePreference
     * @param earlyEvents earlyEventList
     */
    public void setEarlyEventAlarms(ArrayList<AlarmItem> earlyEvents) {
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        if(earlyEvents == null || earlyEvents.isEmpty()) {
            //clear early events
            Log.i(AI_TAG, "clear early events");
            editor.remove(AI_ALARM_EARLY_EVENT_LIST);
            editor.remove(TEMPORARY_ALARM_ID);
        } else {
            //update early events
            String aiAlarms = arrayListToJSON(earlyEvents, AI_ALARM_EARLY_EVENT_LIST).toString();
            Log.i(AI_TAG, "update early events: " + aiAlarms);
            editor.putString(AI_ALARM_EARLY_EVENT_LIST, aiAlarms);
        }

        editor.apply();
    }

    public void setSkipAlarmsTime(ArrayList<AlarmItem> skipAlarms, long startTime, long endTime) {
        for (AlarmItem alarmItem : skipAlarms) {
            long skipTime = calculateSkipAlarm(alarmItem.aHour, alarmItem.aMinutes, new AlarmUtils.DaysOfWeek(alarmItem.aDaysOfWeek), startTime, endTime).getTimeInMillis();
            alarmItem.aAlertTime = skipTime;
        }
    }

    /**
     * @param context
     * @param currentTime
     */
    public void resumeSkipAlarm(Context context, long currentTime) {
        long resumeTime = getInstance(context).getSkipEndTime();
        if (currentTime > resumeTime) {
            getInstance(context).setSkipAlarms(null, -1, -1);
        }
    }

    public static void clearSkipAlarm(Context context, long nextRingTime) {
        ArrayList<AlarmItem> skipAlarms = getInstance(context).getSkipAlarms();
        if (skipAlarms != null) {
            Iterator<AlarmItem> iter = skipAlarms.iterator();
            while (iter.hasNext()) {
                AlarmItem alarmItem = iter.next();
                long alertTime = alarmItem.aAlertTime;
                if (nextRingTime > alertTime) {
                    iter.remove();
                }
            }
            if (skipAlarms.size() == 0) {
                getInstance(context).setSkipAlarms(null, -1 , -1);
            } else {
                getInstance(context).updateSkipAlarms(skipAlarms);
            }
        }
    }

    public void setSkipAlarms(ArrayList<AlarmItem> skipAlarms, long startTime, long endTime) {
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        if (skipAlarms == null || skipAlarms.isEmpty()) {
            //clear skip alarm list
            Log.i(AI_TAG, "clear skip alarm list");
            editor.remove(AI_ALARM_SKIP_ALARM_LIST);
        } else {
            //add skip alarm list
            String aiAlarms = arrayListToJSON(skipAlarms, AI_ALARM_SKIP_ALARM_LIST).toString();
            Log.i(AI_TAG, "set skip alarms:" + aiAlarms);
            if (!TextUtils.isEmpty(aiAlarms)) {
                editor.putString(AI_ALARM_SKIP_ALARM_LIST, aiAlarms);
            }
        }

        //clear start time
        if (startTime < 0) {
            editor.remove(AI_ALARM_SKIP_START_TIME);
        } else {
            editor.putLong(AI_ALARM_SKIP_START_TIME, startTime);
        }

        //clear end time
        if (endTime < 0) {
            editor.remove(AI_ALARM_SKIP_END_TIME);
        } else {
            editor.putLong(AI_ALARM_SKIP_END_TIME, endTime);
        }

        editor.apply();
    }

    /**
     * update skip alarm list
     * @param skipAlarms skipAlarmList
     */
    public void updateSkipAlarms(ArrayList<AlarmItem> skipAlarms) {
        String aiAlarms = arrayListToJSON(skipAlarms, AI_ALARM_SKIP_ALARM_LIST).toString();
        Log.i(AI_TAG, "update skip alarms:" + aiAlarms);
        if (!TextUtils.isEmpty(aiAlarms)) {
            SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(AI_ALARM_SKIP_ALARM_LIST, aiAlarms);
            editor.apply();
        }
    }

    public ArrayList<AlarmItem> getSkipAlarms() {
        String value = null;
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        if (settings.contains(AI_ALARM_SKIP_ALARM_LIST)) {
            value = settings.getString(AI_ALARM_SKIP_ALARM_LIST, null);
        }
        return jsonObjectToArray(value, AI_ALARM_SKIP_ALARM_LIST);
    }

    public ArrayList<AlarmItem> getEarlyEventAlarms() {
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        String jsonList = null;
        if (settings.contains(AI_ALARM_EARLY_EVENT_LIST)) {
            jsonList = settings.getString(AI_ALARM_EARLY_EVENT_LIST, null);
        }

        return jsonObjectToArray(jsonList, AI_ALARM_EARLY_EVENT_LIST);
    }

    public int getEarlyEventAlarmId() {
        int value;
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        if (settings.contains(TEMPORARY_ALARM_ID)) {
            value = settings.getInt(TEMPORARY_ALARM_ID, Integer.MIN_VALUE);
        } else {
            value = Integer.MIN_VALUE;
        }
        return value;
    }

    public void setEarlyEventAlarmId(int id) {
        SharedPreferences settings = mContext.getSharedPreferences(AI_ALARM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit().putInt(TEMPORARY_ALARM_ID, id);
        editor.apply();
    }

    /**
     * clear skip alarm list item by check id
     * @param id check id
     */
    public synchronized void clearSkipAlarmById(int id) {
        Log.d(AI_TAG, "clearSkipAlarmById: " + "id = " + id);
        ArrayList<AlarmItem> lists = getSkipAlarms();
        boolean removed = false;
        if (lists != null && lists.size() > 0) {
            for (AlarmItem item : lists) {
                if (id == item.aId) {
                    lists.remove(item);
                    removed = true;
                    break;
                }
            }
        }

        if (lists == null || lists.size() == 0) {
            //clear skip alarms
            setSkipAlarms(null, -1 , -1);
        } else if (removed) {
            updateSkipAlarms(lists);
        }
    }

    /**
     * clear early meeting by id or clear skip alarm
     * @param id check id
     */
    public synchronized void clearEarlyEventById(int id) {
        Log.d(AI_TAG, "clearEarlyEventAlarmById: " + "id = " + id);
        ArrayList<AlarmItem> earlyAlarms = getEarlyEventAlarms();
        if (earlyAlarms != null) {
            boolean removed = false;
            for (AlarmItem item : earlyAlarms) {
                if (item.aId == id) {
                    earlyAlarms.remove(item);
                    removed = true;
                    break;
                }
            }
            //save new data
            if (earlyAlarms.size() <= 0) {
                setEarlyEventAlarms(null);
            } else if (removed) {
                setEarlyEventAlarms(earlyAlarms);
            }
        }
    }

    /**
     * check id is exist in skip alarms
     * @param id check id
     * @return true is exist false :not
     */
    public boolean checkIsSkipAlarmId(int id) {
        ArrayList<AlarmItem> skipAlarms = getSkipAlarms();
        return checkIsAiAlarmId(skipAlarms, id);
    }

    /**
     * check id is exist in early event alarms
     * @param id check id
     * @return true is exist false :not
     */
    public boolean checkIsEarlyAlarmId(int id) {
        ArrayList<AlarmItem> earlyAlarms = getEarlyEventAlarms();
        return checkIsAiAlarmId(earlyAlarms, id);
    }

    /**
     * calculate early event time
     * @param context context
     * @param alarmSettings alarmSettings
     */
    public synchronized static void calculateEarlyEvent(Context context, AlarmUtils.AlarmSettings alarmSettings) {
        //get early meeting alarms be first
        ArrayList<AlarmItem> earlyEventAlarms = getInstance(context).getEarlyEventAlarms();
        if (earlyEventAlarms != null) {
            for (AlarmItem alarmItem : earlyEventAlarms) {
                Log.i(AI_TAG, "calculate early event " + "id:" + alarmItem.aId + "alertTime: " +
                        alarmItem.aAlertTime + "Date: " + new Date(alarmItem.aAlertTime));
                alarmSettings.reportAlarm(
                        alarmItem.aId, alarmItem.aEnabled, alarmItem.aHour, alarmItem.aMinutes,
                        alarmItem.aAlertTime, new AlarmUtils.DaysOfWeek(alarmItem.aDaysOfWeek),
                        alarmItem.aVibrate, alarmItem.aDescription, alarmItem.aAlert,
                        alarmItem.aSnoozed, alarmItem.aOffAlarm, alarmItem.aRepeatType);
            }
        }
    }

    public static Calendar calculateSkipAlarm(int hour, int minute, AlarmUtils.DaysOfWeek daysOfWeek,
                                               long startTime, long endTime) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if ((hour < nowHour) || ((hour == nowHour) && (minute <= nowMinute))) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c = calculateNextAiSkipAlarm(c, startTime, endTime);
        int addDays = daysOfWeek.getNextAlarm(c);
        if (addDays > 0) {
            c.add(Calendar.DAY_OF_WEEK, addDays);
        }
        return c;
    }


    private static Calendar calculateNextAiSkipAlarm(Calendar cal, long startTime, long endTime) {
        try {
            boolean isSkipAlarm = false;
            do {
                long time = cal.getTimeInMillis();
                Log.d(AiUtils.AI_TAG, "calculateNextAiAlarm: Checking day by day -> time = " + time + "(" + new Date(time) + ")");
                // start to check if it's skip alarm
                if (cal.getTimeInMillis() >= startTime && cal.getTimeInMillis() < endTime) {
                    isSkipAlarm = true;
                } else {
                    isSkipAlarm = false;
                }
                if (!isSkipAlarm) {
                    break;
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            } while(isSkipAlarm);
            long time = cal.getTimeInMillis();
            Log.d(AiUtils.AI_TAG, "calculateNextSkipAlarm: Next skip ai alarm : time = " + time + "(" + new Date(time) + ")");
        } catch (Exception e) {
            Log.w(AiUtils.AI_TAG, "calculateNextAiAlarm: e = " + e.toString());
        }

        return cal;
    }

    /**
     * get alarm list by check "Checked", Repeat type not only once, "Time Before Noon" alarms
     * @param context context
     * @param querySkipStartTime start time
     * @param querySkipEndTime end time
     * @return get all eligible alarm
     */
    public static ArrayList<AlarmItem> getAllSkipAlarms(Context context, long querySkipStartTime, long querySkipEndTime) {
        ArrayList<AlarmItem> alarmItems = null;
        Cursor cursor = null;
        //set query skip alarm condition from alarm DB
        String where = AlarmUtils.AlarmColumns.ENABLED + " = " + INT_CHECK_ENABLE
                + " and " + AlarmUtils.AlarmColumns.REPEAT_TYPE + " != " + REPEAT_TYPE
                + " and " + AlarmUtils.AlarmColumns.HOUR + " < " + INT_CHECK_LAST_HOUR
                + " and " + AlarmUtils.AlarmColumns.HOUR + " >= " + INT_CHECK_FIRST_HOUR;

        Log.i(AI_TAG, "querySkipStartTime: " + new Date(querySkipStartTime) + "querySkipEndTime: " + new Date(querySkipEndTime));

        //set query skip start time and end time accurate to milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(querySkipStartTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.MILLISECOND, 0);
        long skipStartTime = c.getTimeInMillis();

        c.setTimeInMillis(querySkipEndTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.MILLISECOND, 0);
        long skipEndTime = c.getTimeInMillis();

        Log.d(AI_TAG, "startDate = " + skipStartTime + "endTime = " + skipEndTime);
        try {
            cursor = context.getContentResolver().query(
                    AlarmUtils.AlarmColumns.CONTENT_URI, AlarmUtils.AlarmColumns.ALARM_QUERY_COLUMNS,
                    where, null, AlarmUtils.AlarmColumns.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                alarmItems = new ArrayList<>();
                if (cursor.moveToFirst()) {
                    do {
                        AlarmItem alarmItem = AlarmUtils.parseCursor(cursor);
                        long time = AlarmUtils.calculateAlarm(alarmItem.aHour, alarmItem.aMinutes,
                                new AlarmUtils.DaysOfWeek(alarmItem.aDaysOfWeek), alarmItem.aRepeatType).getTimeInMillis();
                        Log.d(AI_TAG, "getAlarmsDuringQueryDate time: " + time + "Date: " + new Date(time));
                        if (time >= skipStartTime && time < skipEndTime) {
                            alarmItems.add(alarmItem);
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.w(AI_TAG, "getAlarmsDuringQueryDate exception :" + e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return alarmItems;
    }

    /**
     * get alarm list by check "Checked", between queryStartTime and queryEndTime, alarm time
     * @param context context
     * @param queryStartTime start time
     * @param queryEndTime end time
     * @return get all eligible alarm
     */
    public static ArrayList<AlarmItem> getAllQueryAlarms(Context context, long queryStartTime, long queryEndTime) {
        ArrayList<AlarmItem> queryAlarms = new ArrayList<>();
        ArrayList<AlarmItem> alarmItems = AlarmUtils.getAlarmListData(context);
        for (AlarmItem alarmItem : alarmItems) {
            long time =  AlarmUtils.calculateAlarm(alarmItem.aHour, alarmItem.aMinutes,
                    new AlarmUtils.DaysOfWeek(alarmItem.aDaysOfWeek), alarmItem.aRepeatType).getTimeInMillis();
            if (alarmItem.aEnabled && time >= queryStartTime && time <= queryEndTime) {
                alarmItem.aAlertTime = time;
                queryAlarms.add(alarmItem);
            }
        }
        return queryAlarms;
    }

}
