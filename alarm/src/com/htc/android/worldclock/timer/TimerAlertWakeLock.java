package com.htc.android.worldclock.timer;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class TimerAlertWakeLock {
    private static final String TAG = "WorldClock.TimerAlertWakeLock";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String WAKELOCK_TAG = "TimerAlertWakeLock_60";
    private WakeLock mTimerAlertPartialWakeLock = null;
    private PowerManager.WakeLock mTimerAlertFullWakeLock = null;
    private final Object mWakeLockObject = new Object();
    private long timeout = 1 * 60 * 1000; // unit is ms
    private static TimerAlertWakeLock mInstance;

    public static synchronized TimerAlertWakeLock getInstance() {
        if (mInstance == null) {
            mInstance = new TimerAlertWakeLock();
        }
        return mInstance;
    }
    
    public void acquirePartial(Context context) {
        // acquire partial wake lock workaround for google framework full wake lock can't keep CPU always on
        synchronized (mWakeLockObject) {
            WakeLock oldTimerAlertPartialWakeLock = mTimerAlertPartialWakeLock;
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mTimerAlertPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
            // check WakeLock is acquired or not
            if ((mTimerAlertPartialWakeLock != null) && !mTimerAlertPartialWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "acquire partial wakelock");
                mTimerAlertPartialWakeLock.acquire(timeout);
            }
            if (oldTimerAlertPartialWakeLock != null) {
                if (oldTimerAlertPartialWakeLock.isHeld()) {
                    if (DEBUG_FLAG) Log.d(TAG, "release old partial wakelock");
                    oldTimerAlertPartialWakeLock.release();
                    oldTimerAlertPartialWakeLock = null;
                }
            }
        }
    }

    public void releasePartial() {
        synchronized (mWakeLockObject) {
            if ((mTimerAlertPartialWakeLock != null) && mTimerAlertPartialWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "release partial wakelock");
                mTimerAlertPartialWakeLock.release();
                mTimerAlertPartialWakeLock = null;
            }
        }
    }
    
    public void acquireFullScreenOn(Context context) {
        synchronized (mWakeLockObject) {
            WakeLock oldTimerAlertFullWakeLock = mTimerAlertFullWakeLock;
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mTimerAlertFullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, WAKELOCK_TAG);
            // check WakeLock is acquired or not
            if ((mTimerAlertFullWakeLock != null) && !mTimerAlertFullWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "acquire full screen on wakelock");
                mTimerAlertFullWakeLock.acquire(timeout);
            }
            if (oldTimerAlertFullWakeLock != null) {
                if (oldTimerAlertFullWakeLock.isHeld()) {
                    if (DEBUG_FLAG) Log.d(TAG, "release old full screen on wakelock");
                    oldTimerAlertFullWakeLock.release();
                    oldTimerAlertFullWakeLock = null;
                }
            }
        }
    }

    public void releaseFullScreenOn() {
        synchronized (mWakeLockObject) {
            if ((mTimerAlertFullWakeLock != null) && mTimerAlertFullWakeLock.isHeld()) {
                if (DEBUG_FLAG) Log.d(TAG, "release full screen on wakelock");
                mTimerAlertFullWakeLock.release();
                mTimerAlertFullWakeLock = null;
            }
        }
    }
}
