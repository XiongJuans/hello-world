package com.htc.android.worldclock.utils;

import java.util.ArrayList;
import java.util.Date;

import com.htc.android.worldclock.alarmclock.AlarmItem;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.alarmclock.AlarmUtils.DaysOfWeek;
import com.htc.android.worldclock.timer.Timer;
import com.htc.lib0.customization.HtcWrapCustomizationManager;
import com.htc.lib0.customization.HtcWrapCustomizationReader;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

public class ADWAgentReceiver extends BroadcastReceiver implements AlarmUtils.AlarmSettings{
    private static final String TAG = "WorldClock.ADWAgentReceiver";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    // broadcast intent for private data, is different between each application
    private static final String BROADCAST_INTENT_QUERY_DATA_CLOCK = "com.htc.devicewipe.action.QUERY_DATA_CLOCK";

    // for private data (result extras)
    private static final String KEY_QUERY = "QUERY";
    private static final String KEY_RESULT = "RESULT";

    private static final String ACTIVE_ALARM_NUMBER = "ALARM=NR";
    private static final String ACTIVE_ALARM_DATA = "ALARM=";
    private static final String ACTIVE_TIMER_NUMBER = "TIMER=NR";
    private static final String ACTIVE_TIMER_DATA = "TIMER=";

    private ArrayList<AlarmItem> mActiveAlarmList;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onReceive: action = " + action);
        if (action.equals(BROADCAST_INTENT_QUERY_DATA_CLOCK)) {
            // check flag is enable ADW or not
            HtcWrapCustomizationManager manager = new HtcWrapCustomizationManager();
            final String readerName = "Device_Wipe";
            HtcWrapCustomizationReader reader = manager.getCustomizationReader(readerName, HtcWrapCustomizationManager.READER_TYPE_XML, false);
            if (reader != null) {
                boolean enableADW = reader.readBoolean("Enabled_ADW", false);
                if (DEBUG_FLAG) Log.d(TAG, "isSupportAccFunction: Acc flag name \"Enabled_ADW\" = " + enableADW);
                if (!enableADW) {
                    return;
                }
            } else {
                Log.w(TAG, "onReceive: Can't get ACC reader");
                return;
            }

            String result = "";
            /* 1. Get demand from data extras to know what data should be returned */
            final String queryCommand = intent.getStringExtra(KEY_QUERY);
            if (DEBUG_FLAG) Log.d(TAG, "onReceive: queryCommand = " + queryCommand);
            if (ACTIVE_ALARM_NUMBER.equals(queryCommand)) {
                int activeAlarmCount = 0;
                mActiveAlarmList = new ArrayList<AlarmItem>();
                ContentResolver contentResolver = context.getContentResolver();
                AlarmUtils.getAlarms(contentResolver, this);
                activeAlarmCount = mActiveAlarmList.size();
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: active alarm count = " + activeAlarmCount);
                result = Integer.toString(activeAlarmCount);
            } else if (queryCommand.startsWith(ACTIVE_ALARM_DATA)) {
                String alarmIdString = queryCommand.substring(ACTIVE_ALARM_DATA.length());
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: alarmIdString = " + alarmIdString);
                int alarmIndex = Integer.parseInt(alarmIdString);
                int activeAlarmCount = 0;
                int hour = 0, minutes = 0;
                
                mActiveAlarmList = new ArrayList<AlarmItem>();
                ContentResolver contentResolver = context.getContentResolver();
                AlarmUtils.getAlarms(contentResolver, this);
                activeAlarmCount = mActiveAlarmList.size();
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: active alarm count = " + activeAlarmCount);
                if (alarmIndex < activeAlarmCount) {
                    hour = mActiveAlarmList.get(alarmIndex).aHour;
                    minutes = mActiveAlarmList.get(alarmIndex).aMinutes;
                }
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: hour = " + hour);
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: minutes = " + minutes);
                String resultString = String.format("%02d:%02d", hour, minutes);
                result = resultString;
            } else if (ACTIVE_TIMER_NUMBER.equals(queryCommand)) {
                int timerState = PreferencesUtil.getTimerState(context);
                if (Timer.TimerEnum.PLAY.ordinal() == timerState) {
                    result = "1";
                } else {
                    result = "0";
                }
            } else if (queryCommand.startsWith(ACTIVE_TIMER_DATA)) {
                result = "0";
                int timerState = PreferencesUtil.getTimerState(context);
                String timerIdString = queryCommand.substring(ACTIVE_ALARM_DATA.length());
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: timerIdString = " + timerIdString);
                int timerIndex = Integer.parseInt(timerIdString);
                if (timerIndex == 0) {
                    if (Timer.TimerEnum.PLAY.ordinal() == timerState) {
                        long expireTime = PreferencesUtil.getTimerExpireTime(context);
                        long timeLeft = (expireTime - SystemClock.elapsedRealtime()) % Timer.MAX_TIME;
                        // no ceil due to can't reach zero when alert ring.
                        int second = (int) (timeLeft / 1000);
                        result = Long.toString(second);
                    }
                }
            }
            if (DEBUG_FLAG) Log.d(TAG, "onReceive: result = " + result);
            
            /* 2. Set the result back into result extras */
            Bundle bundle = new Bundle();
            bundle.putString(KEY_RESULT, result);
            this.setResultExtras(bundle);
        }
    }

    @Override
    public void reportAlarm(int idx, boolean enabled, int hour, int minutes, long alarmtime, DaysOfWeek daysOfWeek, boolean vibrate, String message, String alert, boolean snoozed, boolean offalarm, int repeat_type) {
        if (DEBUG_FLAG) {
            Log.d(TAG, "reportAlarm: idx = " + idx + ", hour = " + hour + ", minutes = " +
                minutes + ", vibrate = " + vibrate + ", message = \"" + message + "\", alert = \"" + alert +
                "\", snoozed = " + snoozed + ", offalarm = " + offalarm + ", mask = " + Integer.toBinaryString(daysOfWeek.getCoded()) +
                ", enabled = " + enabled + ", repeat_type = " + repeat_type + ", time = " + alarmtime + "(" + new Date(alarmtime) + ")");
        }

        if (enabled) {
            AlarmItem item = new AlarmItem();
            item.aHour = hour;
            item.aMinutes = minutes;
            mActiveAlarmList.add(item);
        }
    }
}

