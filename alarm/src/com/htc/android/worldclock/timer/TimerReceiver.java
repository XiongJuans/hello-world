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

package com.htc.android.worldclock.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class TimerReceiver extends BroadcastReceiver {
    private static final String TAG = "WorldClock.TimerReceiver";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final int TIMER_JOB_ID = 1002;
    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onReceive: receive action = " + action);
        if (Timer.ACTION_TIMER_ALERT.equals(action)) {
            // check current user first
            if (!AlertUtils.reflectIsCurrentUser()) {
                return;
            }
            /*
             * wake device, prevent lock screen wake up too late, cause can't wake up and ring
             * due to device suspend again, so wake lock can't do after broadcast to service
             */
            TimerAlertWakeLock wakeLock = TimerAlertWakeLock.getInstance();
            if (wakeLock != null) {
                wakeLock.acquirePartial(context);
            }
        }
        Intent service = new Intent(context, TimerService.class);
        service.setAction(action);
        service.putExtra(AlarmUtils.TIME, intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME));
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            if (Timer.ACTION_TIMER_ALERT.equals(action)) {
                context.startForegroundService(service);
            } else {
                TimerJobIntentService.enqueueWork(context, TimerJobIntentService.class, TIMER_JOB_ID, service);
            }
        } else {
            context.startService(service);
        }
    }
}
