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

import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "WorldClock.AlarmReceiver";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final int ALARM_JOB_ID = 1001;


    /**
     * When running on N devices, we're interested in the boot completed event that is sent while
     * the user is still locked, so that we can schedule alarms.
     */
    @SuppressLint("InlinedApi")
    public static final String ACTION_BOOT_COMPLETED = AlarmUtils.isNOrLater()
            ? Intent.ACTION_LOCKED_BOOT_COMPLETED : Intent.ACTION_BOOT_COMPLETED;
    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onReceive: receive action = " + action);
        if (AlarmUtils.ACTION_ALARM_ALERT.equals(action) || ACTION_BOOT_COMPLETED.equals(action) || Global.getHtcQuickBootPowerOnActionString(context).equals(action)) {
            // check current user first
            if (!AlertUtils.reflectIsCurrentUser()) {
                return;
            }
            /*
             * wake device, prevent lock screen wake up too late, cause can't wake up and ring
             * due to device suspend again, so wake lock can't do after broadcast to service
             */
            AlarmAlertWakeLock wakeLock = AlarmAlertWakeLock.getInstance();
            if (wakeLock != null) {
                wakeLock.acquirePartial(context);
            }
        }
        Intent service = new Intent(context, AlarmService.class);
        service.setAction(action);
        service.putExtra(AlarmUtils.ID, intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID));
        service.putExtra(AlarmUtils.TIME, intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME));
        service.putExtra(AlarmUtils.DESCRIPTION, intent.getStringExtra(AlarmUtils.DESCRIPTION));
        service.putExtra(AlarmUtils.ALERT, intent.getStringExtra(AlarmUtils.ALERT));
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            if (AlarmUtils.ACTION_ALARM_ALERT.equals(action)) {
                context.startForegroundService(service);
            } else {
                AlarmJobIntentService.enqueueWork(context, AlarmJobIntentService.class, ALARM_JOB_ID, service);
            }
        } else {
            context.startService(service);
        }

    }
}
