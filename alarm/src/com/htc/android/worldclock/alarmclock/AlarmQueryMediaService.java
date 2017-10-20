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

import java.util.ArrayList;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.htc.android.worldclock.alarmclock.AlarmUtils.DaysOfWeek;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

/**
 * Glue class: connects TimerAlert IntentReceiver to TimerAlert activity.
 */
public class AlarmQueryMediaService extends Service implements AlarmUtils.AlarmSettings {
    private static final String TAG = "WorldClock.AlarmQueryMediaService";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    
    private static final String RESTORE_COMPLETED = "com.htc.dnatransfer.action.RESTORE_COMPLETED";
    private IntentReceiver mIntentReceiver = null;
    private int mQueryIndex = 0;
    private int mQueryVersion;

    public class QueryItem {
        public int id;
        public String querySyntax;
    }

    private ArrayList<QueryItem> mQueryItemList = new ArrayList<QueryItem>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand, startId = " + startId + ", intent = " + intent);
        super.onStartCommand(intent, flags, startId);
        
        // check args
        if (intent == null) {
            Log.w(TAG, "onStartCommand: intent = null");
            stopSelf();
            return START_NOT_STICKY;
        }
        
        registerIntentReceiver();
        String action = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: receive action = " + action);
        if (AlertUtils.ACTION_QUERY_SYNTAX.equals(action)) {
            mQueryVersion = intent.getIntExtra(AlertUtils.EXTRA_QUERY_VERSION, -1);
            ArrayList<Integer> alarmId = intent.getIntegerArrayListExtra(AlarmUtils.ID);
            ArrayList<String> alarmQuerySyntax = intent.getStringArrayListExtra(AlertUtils.EXTRA_QUERY_SYNTAX);
            if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: mQueryVersion = " + mQueryVersion);

            if ((alarmId != null) && (alarmQuerySyntax != null)) {
                if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: query count = " + alarmId.size());
                if (alarmId.size() == alarmQuerySyntax.size()) {
                    for (int i = 0; i < alarmId.size(); i++) {
                        QueryItem queryItem = new QueryItem();
                        queryItem.id = alarmId.get(i);
                        queryItem.querySyntax = alarmQuerySyntax.get(i);
                        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: alarmId = " + queryItem.id);
                        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: querySyntax = " + queryItem.querySyntax);
                        mQueryItemList.add(queryItem);
                    }
                } else {
                    Log.w(TAG, "onStartCommand: alarmId size != alarmQuerySyntax size");
                }
            } else {
                Log.w(TAG, "onStartCommand: alarmId or alarmQuerySyntax = null");
            }
        }

        return START_REDELIVER_INTENT;
    }
    

    private void registerIntentReceiver() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new IntentReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(RESTORE_COMPLETED);
            registerReceiver(mIntentReceiver, filter, Global.PERMISSION_APP_DEFAULT, null);
        }
    }

    private void unRegisterIntentReceiver() {
        if (mIntentReceiver != null) {
            try {
                unregisterReceiver(mIntentReceiver);
            } catch (Exception e) {
                Log.w(TAG, "unRegisterIntentReceiver: unregisterReceiver fail e = " + e.toString());
            }
            mIntentReceiver = null;
        }
    }

    private class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (DEBUG_FLAG) Log.d(TAG, "receive action = " + action);
            if (RESTORE_COMPLETED.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: receive RESTORE_COMPLETED");
                for (int i = 0; i < mQueryItemList.size(); i++) {
                    AlarmUtils.getAlarm(context.getContentResolver(), AlarmQueryMediaService.this, mQueryItemList.get(i).id);
                }
                unRegisterIntentReceiver();
                stopSelf();
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        if (DEBUG_FLAG) Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void reportAlarm(int idx, boolean enabled, int hour, int minutes, long alarmtime, DaysOfWeek daysOfWeek, boolean vibrate, String message, String alert, boolean snoozed, boolean offalarm, int repeat_type) {
        Uri restoreUri = Uri.parse("");
        switch (mQueryVersion) {
            case 1001:
                restoreUri = AlertUtils.getAlarmRestoreAlertUriByTitle(AlarmQueryMediaService.this, mQueryItemList.get(mQueryIndex).querySyntax);
                break;
            case 1002:
                restoreUri = AlertUtils.getAlarmRestoreAlertUriByQuertCondition(AlarmQueryMediaService.this, mQueryItemList.get(mQueryIndex).querySyntax);
                break;
            default:
                Log.w(TAG, "restoreAlarms: no this version = " + mQueryVersion);
        }
        if (DEBUG_FLAG) Log.d(TAG, "reportAlarm: alarm " + idx + ", restoreUri = " + restoreUri);
        SetAlarm.saveAlarm(this, idx, enabled, hour, minutes, daysOfWeek, vibrate, restoreUri.toString(), message, snoozed, offalarm, repeat_type, false);
        mQueryIndex += 1;
    }
}
