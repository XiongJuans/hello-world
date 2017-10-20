package com.htc.android.worldclock.alarmclock;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class AlarmAlertWakeLock {
    private static final String TAG = "WorldClock.AlarmAlertWakeLock";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String WAKELOCK_TAG = "AlarmAlertWakeLock_600";
    private WakeLock mAlarmAlertPartialWakeLock = null;
    private WakeLock mAlarmAlertFullWakeLock = null;
    private final Object mWakeLockObject = new Object();
    private long timeout = 10 * 60 * 1000; // unit is ms
    private static AlarmAlertWakeLock mInstance;

    public static synchronized AlarmAlertWakeLock getInstance() {
        if (mInstance == null) {
            mInstance = new AlarmAlertWakeLock();
        }
        return mInstance;
    }

    public void acquirePartial(Context context) {
        // acquire partial wake lock workaround for google framework full wake lock can't keep CPU always on
        synchronized (mWakeLockObject) {
            WakeLock oldAlarmAlertPartialWakeLock = mAlarmAlertPartialWakeLock;
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mAlarmAlertPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
            // check WakeLock is acquired or not
            if ((mAlarmAlertPartialWakeLock != null) && !mAlarmAlertPartialWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "acquire partial wakelock");
                mAlarmAlertPartialWakeLock.acquire(timeout);
            }
            if (oldAlarmAlertPartialWakeLock != null) {
                if (oldAlarmAlertPartialWakeLock.isHeld()) {
                    if (DEBUG_FLAG) Log.d(TAG, "release old partial wakelock");
                    oldAlarmAlertPartialWakeLock.release();
                    oldAlarmAlertPartialWakeLock = null;
                }
            }
        }
    }

    public void releasePartial() {
        synchronized (mWakeLockObject) {
            if ((mAlarmAlertPartialWakeLock != null) && mAlarmAlertPartialWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "release partial wakelock");
                mAlarmAlertPartialWakeLock.release();
                mAlarmAlertPartialWakeLock = null;
            }
        }
    }
    
    public void acquireFullScreenOn(Context context) {
        synchronized (mWakeLockObject) {
            WakeLock oldAlarmAlertFullWakeLock = mAlarmAlertFullWakeLock;
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mAlarmAlertFullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, WAKELOCK_TAG);
            // check WakeLock is acquired or not
            if ((mAlarmAlertFullWakeLock != null) && !mAlarmAlertFullWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "acquire full screen on wakelock");
                mAlarmAlertFullWakeLock.acquire(timeout);
            }
            if (oldAlarmAlertFullWakeLock != null) {
                if (oldAlarmAlertFullWakeLock.isHeld()) {
                    if (DEBUG_FLAG) Log.d(TAG, "release old full screen on wakelock");
                    oldAlarmAlertFullWakeLock.release();
                    oldAlarmAlertFullWakeLock = null;
                }
            }
        }
    }

    public void releaseFullScreenOn() {
        synchronized (mWakeLockObject) {
            if ((mAlarmAlertFullWakeLock != null) && mAlarmAlertFullWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "release full screen on wakelock");
                mAlarmAlertFullWakeLock.release();
                mAlarmAlertFullWakeLock = null;
            }
        }
    }
}
