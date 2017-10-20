package com.htc.android.worldclock.timer;

import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

import java.util.Date;

public class TimerJobIntentService extends JobIntentService {
    private static final String TAG = "WorldClock.TimerJobIntentService";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (DEBUG_FLAG) Log.d(TAG, "onHandleWork: action = " + action);
        } else {
            if (DEBUG_FLAG) Log.d(TAG, "onHandleWork: action = null");
            return;
        }
        if (Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            HandleTimeChange();
            stopService();
        }
    }

    private void stopService() {
        if (DEBUG_FLAG) Log.d(TAG, "stopService");
        // don't stopSelf for register lock screen listener if check alarm is active
        if (!PreferencesUtil.getIsFiringTimer(this)) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "stopService");
        super.onDestroy();
    }

    private void HandleTimeChange() {
        if (PreferencesUtil.getTimerState(this) == Timer.TimerEnum.PLAY.ordinal()) {
            long currentTimeMillis = System.currentTimeMillis();
            long timeChangeElapsedRealtime = SystemClock.elapsedRealtime();
            long timeLeft = PreferencesUtil.getTimerExpireTime(this) - timeChangeElapsedRealtime;
            long newAlertTime = currentTimeMillis + timeLeft;
            Timer.disableAlert(this);
            Timer.enableAlert(this, newAlertTime);

            String timeFormat = String.format("%02d:%02d:%02d", timeLeft / 1000 / 60 / 60, (timeLeft / 1000 / 60) % 60, (timeLeft / 1000) % 60);
            Log.d(TAG, "HandleTimeChange: timeLeft = " + timeFormat);
            Log.d(TAG, "HandleTimeChange: currentTimeMillis of time changed = " + currentTimeMillis + "(" + new Date(currentTimeMillis) + ")");
            Log.d(TAG, "HandleTimeChange: newAlertTime of time changed = " + newAlertTime + "(" + new Date(newAlertTime) + ")");
        }
    }
}
