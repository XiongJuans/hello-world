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

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SkipHolidayReceiver extends BroadcastReceiver {
    private static final String TAG = "WorldClock.SkipHolidayReceiver";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String ACTION_HOLIDAY_CHANGED = "com.htc.intent.action.ACTION_HOLIDAY_CHANGED";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onReceive: receive action = " + action);
        if (ACTION_HOLIDAY_CHANGED.equals(action)) {
            Log.i(TAG, "Server data changed of skip holiday");
            /* allow next alarm to trigger while this activity is active */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AlarmUtils.setNextAlert(context);
                }
            }).start();
            return;
        } 
    }
}
