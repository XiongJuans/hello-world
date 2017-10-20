/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.provider.AlarmClock;
import android.util.Log;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.AlarmUtils.AlarmCalculator;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

/**
 * Returns a list of alarms that are specified by the intent
 * processed by HandleDeskClockApiCalls
 * if there are more than 1 matching alarms and the SEARCH_MODE is not ALL
 * we show a picker UI dialog
 */
class FetchMatchingAlarmsAction implements Runnable {
    private static final String TAG = "WorldClock.FetchMatchingAlarmsAction";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private final Context mContext;
    private final List<AlarmItem> mAlarms;
    private final Intent mIntent;
    private final List<AlarmItem> mMatchingAlarms = new ArrayList<>();
    private final Activity mActivity;

    public FetchMatchingAlarmsAction(Context context, List<AlarmItem> alarms, Intent intent,
                                     Activity activity) {
        mContext = context;
        // only enabled alarms are passed
        mAlarms = alarms;
        mIntent = intent;
        mActivity = activity;
    }

    @Override
    public void run() {
        // only allow on background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Must be called on a background thread");
        }

        final String searchMode = mIntent.getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE);
        if (DEBUG_FLAG) Log.d(TAG, "searchMode = " + searchMode);
        // if search mode isn't specified show all alarms in the UI picker
        if (searchMode == null) {
            mMatchingAlarms.addAll(mAlarms);
            return;
        }

        switch (searchMode) {
            case AlarmClock.ALARM_SEARCH_MODE_TIME:
                // at least one of these has to be specified in this search mode.
                final int hour = mIntent.getIntExtra(AlarmClock.EXTRA_HOUR, -1);
                // if minutes weren't specified default to 0
                final int minutes = mIntent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
                final Boolean isPm = (Boolean) mIntent.getExtras().get(AlarmClock.EXTRA_IS_PM);
                boolean badInput = isPm != null && hour > 12 && isPm;
                badInput |= hour < 0 || hour > 23;
                badInput |= minutes < 0 || minutes > 59;

                if (badInput) {
                    final String[] ampm = new DateFormatSymbols().getAmPmStrings();
                    final String amPm = isPm == null ? "" : (isPm ? ampm[1] : ampm[0]);
                    final String reason = mContext.getString(R.string.invalid_time, hour, minutes,
                            amPm);
                    notifyFailureAndLog(reason, mActivity);
                    return;
                }

                final int hour24 = Boolean.TRUE.equals(isPm) && hour < 12 ? (hour + 12) : hour;

                // there might me multiple alarms at the same time
                for (AlarmItem alarm : mAlarms) {
                    if (alarm.aHour == hour24 && alarm.aMinutes == minutes) {
                        mMatchingAlarms.add(alarm);
                    }
                }
                if (mMatchingAlarms.isEmpty()) {
                    final String reason = mContext.getString(R.string.no_alarm_at, hour24, minutes);
                    notifyFailureAndLog(reason, mActivity);
                    return;
                }
                break;
            case AlarmClock.ALARM_SEARCH_MODE_NEXT:
                int alarmId = PreferencesUtil.getCurrentFiringAlarm(mContext);
                if (AlarmUtils.INVALID_ALARMID != alarmId) {
                    AlertUtils.sendAlarmDismissIntent(mContext, alarmId);
                    Voice.notifySuccess(mActivity, mContext.getString(R.string.pick_alarm_to_dismiss));
                } else {
                    AlarmCalculator ac = AlarmUtils.calculateNextAlert(mContext);
                    int id = ac.getIndex();
                    if (DEBUG_FLAG) Log.d(TAG, "id = " + id);
                    for (AlarmItem alarm : mAlarms) {
                        if (alarm.aId == id) {
                            mMatchingAlarms.add(alarm);
                        }
                    }
                }
                break;
            case AlarmClock.ALARM_SEARCH_MODE_ALL:
                mMatchingAlarms.addAll(mAlarms);
                break;
            case AlarmClock.ALARM_SEARCH_MODE_LABEL:
                // EXTRA_MESSAGE has to be set in this mode
                final String label = mIntent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
                if (label == null) {
                    final String reason = mContext.getString(R.string.no_label_specified);
                    notifyFailureAndLog(reason, mActivity);
                    return;
                }

                // there might me multiple alarms with this label
                for (AlarmItem alarm : mAlarms) {
                    if (alarm.aDescription.contains(label)) {
                        mMatchingAlarms.add(alarm);
                    }
                }

                if (mMatchingAlarms.isEmpty()) {
                    final String reason = mContext.getString(R.string.no_alarms_with_label);
                    notifyFailureAndLog(reason, mActivity);
                    return;
                }
                break;
        }
    }

    public List<AlarmItem> getMatchingAlarms() {
        return mMatchingAlarms;
    }

    private void notifyFailureAndLog(String reason, Activity activity) {
        Log.w(TAG, "notifyFailureAndLog: reason = " + reason);
        Voice.notifyFailure(activity, reason);
    }
}