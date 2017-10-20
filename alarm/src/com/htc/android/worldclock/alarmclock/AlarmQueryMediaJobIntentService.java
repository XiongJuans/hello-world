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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.htc.android.worldclock.alarmclock.AlarmUtils.DaysOfWeek;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

import java.util.ArrayList;

/**
 * Glue class: connects TimerAlert IntentReceiver to TimerAlert activity.
 */
public class AlarmQueryMediaJobIntentService extends JobIntentService implements AlarmUtils.AlarmSettings {
    private static final String TAG = "WorldClock.AlarmQueryMediaJobIntentService";
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
    protected void onHandleWork(@NonNull Intent intent) {
        if (DEBUG_FLAG) Log.d(TAG, "onHandleWork intent = " + intent);
        // check args
        if (intent == null) {
            Log.w(TAG, "onHandleWork: intent = null");
            stopSelf();
            return;
        }

        registerIntentReceiver();
        String action = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onHandleWork: receive action = " + action);
        if (AlertUtils.ACTION_QUERY_SYNTAX.equals(action)) {
            mQueryVersion = intent.getIntExtra(AlertUtils.EXTRA_QUERY_VERSION, -1);
            ArrayList<Integer> alarmId = intent.getIntegerArrayListExtra(AlarmUtils.ID);
            ArrayList<String> alarmQuerySyntax = intent.getStringArrayListExtra(AlertUtils.EXTRA_QUERY_SYNTAX);
            if (DEBUG_FLAG) Log.d(TAG, "onHandleWork: mQueryVersion = " + mQueryVersion);

            if ((alarmId != null) && (alarmQuerySyntax != null)) {
                if (DEBUG_FLAG) Log.d(TAG, "onHandleWork: query count = " + alarmId.size());
                if (alarmId.size() == alarmQuerySyntax.size()) {
                    for (int i = 0; i < alarmId.size(); i++) {
                        QueryItem queryItem = new QueryItem();
                        queryItem.id = alarmId.get(i);
                        queryItem.querySyntax = alarmQuerySyntax.get(i);
                        if (DEBUG_FLAG) Log.d(TAG, "onHandleWork: alarmId = " + queryItem.id);
                        if (DEBUG_FLAG) Log.d(TAG, "onHandleWork: querySyntax = " + queryItem.querySyntax);
                        mQueryItemList.add(queryItem);
                    }
                } else {
                    Log.w(TAG, "onHandleWork: alarmId size != alarmQuerySyntax size");
                }
            } else {
                Log.w(TAG, "onHandleWork: alarmId or alarmQuerySyntax = null");
            }
        }
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
                    AlarmUtils.getAlarm(context.getContentResolver(), AlarmQueryMediaJobIntentService.this, mQueryItemList.get(i).id);
                }
                unRegisterIntentReceiver();
                stopSelf();
            }
        }
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
                restoreUri = AlertUtils.getAlarmRestoreAlertUriByTitle(AlarmQueryMediaJobIntentService.this, mQueryItemList.get(mQueryIndex).querySyntax);
                break;
            case 1002:
                restoreUri = AlertUtils.getAlarmRestoreAlertUriByQuertCondition(AlarmQueryMediaJobIntentService.this, mQueryItemList.get(mQueryIndex).querySyntax);
                break;
            default:
                Log.w(TAG, "restoreAlarms: no this version = " + mQueryVersion);
        }
        if (DEBUG_FLAG) Log.d(TAG, "reportAlarm: alarm " + idx + ", restoreUri = " + restoreUri);
        SetAlarm.saveAlarm(this, idx, enabled, hour, minutes, daysOfWeek, vibrate, restoreUri.toString(), message, snoozed, offalarm, repeat_type, false);
        mQueryIndex += 1;
    }
}
