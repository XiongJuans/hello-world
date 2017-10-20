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

import java.util.Date;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.alarmclock.HandleApiCalls;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.lockscreen.reminder.HtcReminderViewMode;

/**
 * Glue class: connects TimerAlert IntentReceiver to TimerAlert activity.
 */
public class TimerService extends Service {
    private static final String TAG = "WorldClock.TimerService";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    public static final String ACTION_TIMER_TIME = "com.htc.intent.action.timer_time";
    private static final String EXTRA_LAUNCH_TIMER= "extra_launch_timer";

    private static final int UI_MSG_GET_CALL_STATE = 0x0001;
    
    private AudioManager mAudioManager;
    private TimerKlaxon mKlaxon = null;
    private TelephonyManager mTelephonyManager;

    private IntentReceiver mIntentReceiver;

    private boolean mLockScreen;
    private boolean bTimerAlertRunning = false;

    private boolean registerPhoneListenerReady = false;
    private boolean registerSlot1PhoneListenerReady = false;
    private boolean registerSlot2PhoneListenerReady = false;
    private TimerAlertReminderManager mReminderManager = null;
    
    private final int DELAY_TIME_MILLIS = 2000;
    private int mPrevCallState = TelephonyManager.CALL_STATE_IDLE;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        registerIntentReceiver();
        String action = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: receive action = " + action);

        // check args
        if ((intent == null) || TextUtils.isEmpty(action)) {
            stopService();
            return START_NOT_STICKY;
        }

        if (Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            HandleTimeChange();
            stopService();
            return START_NOT_STICKY;
        } else if (AlertUtils.ALERT_DISMISS.equals(action)) {
            // This intent is sent from the notification when the user presses the dismiss action.
            Log.i(TAG, "User presses dismiss timer from status bar");
            dismiss(TimerService.this);
            return START_NOT_STICKY;
        } else if (Timer.ACTION_TIMER_ALERT.equals(action)) {
            PreferencesUtil.setTimerState(this, Timer.TimerEnum.NORMAL.ordinal());
            long now = System.currentTimeMillis();
            long time = intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME);
            if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: timer time = " + time + "(" + new Date(time) + ")");
            if (now > (time + AlertUtils.STALE_WINDOW)) {
                if (DEBUG_FLAG) {
                    Log.d(TAG, "onStartCommand: ignoring timer intent timer time = " + time +
                        "(" + new Date(time) + ")" + ", now = " + now + "(" + new Date(now) + ")");
                }
                if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
                    Log.i(TAG, "onStartCommand: start foreground with a notification then stop it");
                    startTimerForeground();
                    stopForeground(true);
                }
                stopService();
                return START_NOT_STICKY;
            }
            doTimerService();
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        if (DEBUG_FLAG) Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy");
        stopForeground(true);
        removeAllHandlerMessages();
        // stop to listen phone event
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }

        unRegisterIntentReceiver();

        // release wakelock
        TimerAlertWakeLock wakeLock = TimerAlertWakeLock.getInstance();
        if (wakeLock != null) {
            wakeLock.releaseFullScreenOn();
            wakeLock.releasePartial();
            wakeLock = null;
        }

        if (mReminderManager != null) {
            mReminderManager.cleanUp();
        }
        
        super.onDestroy();
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy END");
    }

    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_MSG_GET_CALL_STATE:
                    int callState = TelephonyManager.CALL_STATE_IDLE;
                    if (mTelephonyManager != null) {
                        callState = mTelephonyManager.getCallState();
                    }
                    if (mPrevCallState != callState) {
                        if (DEBUG_FLAG) Log.d(TAG, "UI_MSG_GET_CALL_STATE: Now callState = " + callState + ", mPrevCallState = " + mPrevCallState);
                        doPhoneStateChangeAction(callState);
                        mPrevCallState = callState;
                    }
                    mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(UI_MSG_GET_CALL_STATE), DELAY_TIME_MILLIS);
                    break;
            }
        }
    };
    
    private void removeAllHandlerMessages() {
        mMainHandler.removeMessages(UI_MSG_GET_CALL_STATE);
    }
    
    private void stopService() {
        if (DEBUG_FLAG) Log.d(TAG, "stopService");
        // don't stopSelf for register lock screen listener if check alarm is active
        if (!bTimerAlertRunning) {
            if (DEBUG_FLAG) Log.d(TAG, "stopService: do stopSelf");
            stopSelf();
        }
    }

    private void doTimerService() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
        // check phone state
        if (mTelephonyManager != null) {
            mPrevCallState = mTelephonyManager.getCallState();
        }
        if (DEBUG_FLAG) Log.d(TAG, "doTimerService: mPrevCallState = " + mPrevCallState);
        
        if (TelephonyManager.CALL_STATE_RINGING == mPrevCallState) {
            Log.i(TAG, "Phone call is ringing, timer is auto dismissed");
            // TelephonyManager.CALL_STATE_RINGING -> A new call arrived and is ringing or waiting
            // show status bar
            startTimerForeground();
            dismiss(TimerService.this);
            stopService();
        } else {
            initTimer();
            Log.i(TAG, "Popup UI and play sound");
            startTimerProcess();
        }
    }

    /**
     * android-O startForegroundService did call startForeground with notification
     */
    private void startTimerForeground() {
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            Notification notification = AlertUtils.timerNotification(this, false, false);
            startForeground(AlertUtils.TIMERALERT_NOTIFICATION_ID, notification);
        }
    }

    private void initTimer() {
        // set timer alert running status
        bTimerAlertRunning = true;
        boolean isDualSIM = AlertUtils.reflectIsMultiSimEnabled(this);
        if (DEBUG_FLAG) Log.d(TAG, "initTimer: isDualSIM = " + isDualSIM);
        if (isDualSIM) {
            if (mMainHandler != null) {
                mMainHandler.sendMessage(mMainHandler.obtainMessage(UI_MSG_GET_CALL_STATE));
            }
        } else {
            // listen phone event
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
        // Broadcast intent that timer is alert
        informTimerAlert(this);
    }
    
    private String getStartTimerActivityIntentString(Context context) {
        Intent intent = new Intent();
        if (intent != null && context != null) {
            intent.setClassName(context.getPackageName(), WorldClockTabControl.LAUNCH_AP_ACTIVITY_NAME);
            intent.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_TIMER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            Log.w(TAG, "getStartTimerActivityIntentString: intent or context = null");
        }
        return (intent != null) ? intent.toUri(0) : "";
    }
    
    private void informTimerAlert(Context context) {
        Intent informIntent = new Intent();
        informIntent.setAction(AlertUtils.ACTION_TIMERALERT_INFROM);
        informIntent.putExtra(EXTRA_LAUNCH_TIMER, getStartTimerActivityIntentString(context));
        context.sendBroadcast(informIntent, Global.PERMISSION_APP_DEFAULT);
        if (DEBUG_FLAG) Log.d(TAG, "informTimerAlert: send action = " + AlertUtils.ACTION_TIMERALERT_INFROM);
    }
    
    private void startTimerProcess() {
        PreferencesUtil.setIsFiringTimer(this, true);
        mLockScreen = AlertUtils.getLockScreenMode(this);
        if (DEBUG_FLAG) Log.d(TAG, "doTimerService: mLockScreen = " + mLockScreen);
        if (mLockScreen) {
            // Timer alert reminder UI
            mReminderManager = TimerAlertReminderManager.getInstance();
            mReminderManager.init(this);
            mReminderManager.initRegisterViewMode(this, HtcReminderViewMode.TIMER_MODE, mLockScreen);
        } else {
            // Timer alert dialog UI
            AlertUtils.timerAlert(TimerService.this, AlertUtils.ALERT_DIALOG_NORMAL, mLockScreen);
        }
        startTimerSound();
    }
    
    private void startTimerSound() {
        // acquire full screen on wakelock
        TimerAlertWakeLock wakeLock = TimerAlertWakeLock.getInstance();
        if (wakeLock != null) {
            wakeLock.acquireFullScreenOn(this);
        }

        // keep timer ring and notify timer to status bar
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            startTimerForeground();
        } else {
            AlertUtils.timerNotification(this, false, true);
        }
        // check DND setting value
        if (!AlertUtils.isDoNotDisturbEnabled(this)) {
            playAlarm();
        }
    }

    private void registerIntentReceiver() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new IntentReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(AlertUtils.ACTION_SNOOZE_DISMISS_RECEIVE); // for BT, dialog and reminder
            if (Global.isHEPDevice(this)) {
                registerReceiver(mIntentReceiver, filter, Global.PERMISSION_APP_DEFAULT, null);
            } else {
                registerReceiver(mIntentReceiver, filter, Global.PERMISSION_APP_WORLDCLOCK_ALERT, null);
            }
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
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG_FLAG) Log.d(TAG, "receive action = " + action);
            if (AlertUtils.ACTION_SNOOZE_DISMISS_RECEIVE.equals(action)) {
                final String recvAction = intent.getStringExtra(AlertUtils.ACTION_TIMER_ACTION);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (AlertUtils.ALERT_DISMISS.equals(recvAction)) {
                            Log.i(TAG, "Timer is dismissed from dialog, reminder or BT");
                            dismiss(TimerService.this);
                            stopService();
                        }
                    }
                }).start();
            }
        }
    }

    private void playAlarm() {
        // request audio focus
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }

        // play audio at the last
        mKlaxon = TimerKlaxon.getInstance();
        mKlaxon.setStopCallback(mStopCallback); // to stop callback
        mKlaxon.setKillerCallback(mKillerCalback);
        // put audio at the latest
        new Thread(new Runnable() {
            @Override
            public void run() {
                mKlaxon.play(TimerService.this);
            }
        }).start();
    }

    private void resetTimer(Context context) {
        // reset preference settings
        PreferencesUtil.setTimerState(context, Timer.TimerEnum.INIT.ordinal());
        PreferencesUtil.setTimerUserChoiceTime(context, Timer.DEFAULT_COUNTDOWN_VALUE);
    }

    private TimerKlaxon.StopCallback mStopCallback = new TimerKlaxon.StopCallback() {
        @Override
        public void onStopped() {
            Log.i(TAG, "Stop alert sound");
            // set timer alert running status
            bTimerAlertRunning = false;
            if (HandleApiCalls.CTS_TEST_TIMER_STRING.equals(PreferencesUtil.getTimerLabel(TimerService.this))) {
                PreferencesUtil.setTimerLabel(TimerService.this, "");
                resetTimer(TimerService.this);
            }
            stopService();
        }
    };

    private TimerKlaxon.KillerCallback mKillerCalback = new TimerKlaxon.KillerCallback() {
        @Override
        public void onKilled() { // non-UI thread
            Log.i(TAG, "Timer rings time out");
            // set timer alert running status
            bTimerAlertRunning = false;
            // Stop reminder UI and cover view but dialog
            AlertUtils.sendTimerTimeoutIntent(TimerService.this);
            AlertUtils.timerAlert(TimerService.this, AlertUtils.ALERT_DIALOG_TIMEOUT, false);
            
            // to prevent audio lock
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mKlaxon != null) {
                        mKlaxon.stop(TimerService.this);
                        mKlaxon = null;
                    }
                }
            }).start();
        }
    };
    
    public void dismiss(Context context) {
        if (DEBUG_FLAG) Log.d(TAG, "dismiss: dismiss");
        // stop alert sound
        TimerKlaxon klaxon = TimerKlaxon.getInstance();
        if (klaxon != null) {
            klaxon.stop(context);
            klaxon = null;
        }
        AlertUtils.cancelTimerNotification(context);
        AlertUtils.setTimerDismissByUser(true);
        AlertUtils.sendCancelAlertIntent(context, AlarmUtils.INVALID_ALARMID);
        // Inform intent that timer is dismissed
        AlertUtils.sendTimerDismissIntent(context, AlertUtils.ALERT_DISMISS);
        PreferencesUtil.setIsFiringTimer(context, false);
    }

    private void doPhoneStateChangeAction(int phoneState) {
        if (DEBUG_FLAG) Log.d(TAG, "doPhoneStateChangeAction: phoneState = " + phoneState);
        if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
            Log.i(TAG, "A new phone call arrived, timer is auto dismissed");
            // TelephonyManager.CALL_STATE_RINGING -> A new call arrived and is ringing or waiting
            PreferencesUtil.setTimerState(TimerService.this, Timer.TimerEnum.NORMAL.ordinal());
            dismiss(TimerService.this);
            stopService();
        } else if (phoneState == TelephonyManager.CALL_STATE_IDLE) {
            Log.i(TAG, "Phone call from in-call to idle, timer rings again");
            // Reshow Alert dialog if Phone state change from in-call to non in-call;
            AlertUtils.timerAlert(TimerService.this, AlertUtils.ALERT_DIALOG_NORMAL, AlertUtils.getLockScreenMode(TimerService.this));
        }
    }

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DEBUG_FLAG) Log.d(TAG, "onCallStateChanged: PhoneState = " + state);
            if (DEBUG_FLAG) Log.d(TAG, "onCallStateChanged: registerPhoneListenerReady = " + registerPhoneListenerReady);
            if (!registerPhoneListenerReady) {
                Log.i(TAG, "onCallStateChanged: Single SIM and register call state change not ready, do nothing");
                registerPhoneListenerReady = true;
            } else {
                Log.i(TAG, "onCallStateChanged: Single SIM and register call state change is ready");
                doPhoneStateChangeAction(state);
            }
        }
    };
    
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
