package com.htc.android.worldclock.alarmclock;

import java.util.Date;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcPhoneSensorFunctions;
import com.htc.android.worldclock.utils.ReminderActivity;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.lockscreen.reminder.HtcReminderViewMode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmAlertReminder extends ReminderActivity {
    private static final String TAG = "WorldClock.AlarmAlertReminder";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    
    private static final int ICON_DISMISS = 0;
    private static final int ICON_SNOOZE = 1;
    AlarmAlertReminderView mReminderView;
    AlarmAlertReminderManager mReminderManager;
    int mViewMode;
    IntentReceiver mIntentReceiver;

    private int mAlarmId;
    private long mAlarmTime;
    private String mAlarmDescription;
    private int mAlarmType;

    private HtcPhoneSensorFunctions mSensorFunctions;
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle arg0) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        super.onCreate(arg0);
        // TODO: Window Flag:
        // FLAG_SHOW_WHEN_LOCKED: to let windows be shown when the screen is locked.
        // FLAG_TURN_SCREEN_ON: once the window has been shown then the system will poke the power manager's user activity to turn the screen on. 
        // FLAG_KEEP_SCREEN_ON: keep the device's screen turned on and bright. 
        // For more detail, please refer to 
        // http://developer.android.com/reference/android/view/WindowManager.LayoutParams.html
        // 
        // Because Google already deprecated FULL_WAKE_LOCK 
        // and we should use FLAG_KEEP_SCREEN_ON instead of this type of wake lock.
        // http://developer.android.com/reference/android/os/PowerManager.html

        // TODO: case1: one Tile with two Buttons.
        setContentView(R.layout.specific_alarm_alert_reminder);
        
        Intent intent = getIntent();
        mAlarmId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
        mAlarmTime = intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME);
        mAlarmDescription = intent.getStringExtra(AlarmUtils.DESCRIPTION);
        mAlarmType = intent.getIntExtra(AlertUtils.EXTRA_ALERT_TYPE, AlertUtils.ALERT_DIALOG_NORMAL);

        if (DEBUG_FLAG) Log.d(TAG, "onCreate: mAlarmId = " + mAlarmId);
        if (DEBUG_FLAG) Log.d(TAG, "onCreate: mAlarmTime = " + mAlarmTime + "(" + new Date(mAlarmTime) + ")");
        if (DEBUG_FLAG) Log.d(TAG, "onCreate: mAlarmDescription = " + mAlarmDescription);
        if (DEBUG_FLAG) Log.d(TAG, "onCreate: mAlarmType = " + mAlarmType);
        
        mReminderManager = AlarmAlertReminderManager.getInstance();
        
        mReminderView = (AlarmAlertReminderView) this.findViewById(R.id.alert_reminder_view);
        if (mReminderView != null) {
            this.setReminderView(mReminderView);
            mViewMode = mReminderView.getViewMode();
            mReminderView.updateUI(mAlarmId, mAlarmTime, mAlarmDescription);
            mReminderView.setCallback(new AlarmAlertReminderView.Callback() {
                @Override
                public void onTileDropEnd() {
                    AlertUtils.sendAlarmDismissIntent(AlarmAlertReminder.this, mAlarmId);
                    unlock();
                }
                @Override
                public void onButtonDrop(int index) {
                    if (DEBUG_FLAG) Log.d(TAG, "onButtonDrop: index = " + index);
                    if (index == ICON_DISMISS) {
                        AlertUtils.sendAlarmDismissIntent(AlarmAlertReminder.this, mAlarmId);
                        if (mReminderManager != null) {
                            mReminderManager.requestShowIdleScreen();
                        }
                        finishActivity();
                    } else if (index == ICON_SNOOZE) {
                        AlertUtils.sendAlarmSnoozeIntent(AlarmAlertReminder.this, mAlarmId, mAlarmDescription);
                        if (mReminderManager != null) {
                            mReminderManager.requestShowIdleScreen();
                        }
                        finishActivity();
                    }
                }
            });
        }

        registerIntentReceiver();
        boolean isDoNothingFlipAction = AlarmUtils.getAlarmFlipAction(this) == AlarmUtils.FlipAction.Action_None;
        boolean isSupportSensorFeature = HtcPhoneSensorFunctions.isSupportSensorFeature(this);
        if (isSupportSensorFeature && !isDoNothingFlipAction) {
            mSensorFunctions = HtcPhoneSensorFunctions.getInstances(this);
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG_FLAG) Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        
        mAlarmId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
        mAlarmTime = intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME);
        mAlarmDescription = intent.getStringExtra(AlarmUtils.DESCRIPTION);
        mAlarmType = intent.getIntExtra(AlertUtils.EXTRA_ALERT_TYPE, AlertUtils.ALERT_DIALOG_NORMAL);

        if (DEBUG_FLAG) Log.d(TAG, "onAlarmIntent: mAlarmId = " + mAlarmId);
        if (DEBUG_FLAG) Log.d(TAG, "onAlarmIntent: mAlarmTime = " + mAlarmTime + "(" + new Date(mAlarmTime) + ")");
        if (DEBUG_FLAG) Log.d(TAG, "onAlarmIntent: mAlarmDescription = " + mAlarmDescription);
        if (DEBUG_FLAG) Log.d(TAG, "onAlarmIntent: mAlarmType = " + mAlarmType);

        mReminderView.updateUI(mAlarmId, mAlarmTime, mAlarmDescription);
    }

    @Override
    protected void onStart() {
        if (DEBUG_FLAG) Log.d(TAG, "onStart");
        super.onStart();
    }
    
    @Override
    protected void onResume() {
        if (DEBUG_FLAG) Log.d(TAG, "onResume");
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (DEBUG_FLAG) Log.d(TAG, "onStop");
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy");
        unregisterIntentReceiver();
        super.onDestroy();
    }
    
    private void registerIntentReceiver() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new IntentReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(AlertUtils.LOCAL_ACTION_CANCEL_ALERT);
            filter.addAction(AlertUtils.ACTION_ALARM_TIMEOUT);
            filter.addAction(AlertUtils.ACTION_VOLUMEKEY_EVENT);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            if (Global.isHEPDevice(this)) {
                registerReceiver(mIntentReceiver, filter, Global.PERMISSION_APP_DEFAULT, null);
            } else {
                registerReceiver(mIntentReceiver, filter, Global.PERMISSION_APP_WORLDCLOCK_ALERT, null);
            }
        }
    }

    private void unregisterIntentReceiver() {
        if (mIntentReceiver != null) {
            unregisterReceiver(mIntentReceiver);
            mIntentReceiver = null;
        }
    }

    private class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG_FLAG) Log.d(TAG, "onReceive: action = " + action);
            if (AlertUtils.LOCAL_ACTION_CANCEL_ALERT.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive AlertUtils.LOCAL_ACTION_CANCEL_ALERT");
                int id = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
                // check alarm id is match or not
                if ((AlarmUtils.INVALID_ALARMID != id) && (mAlarmId == id)) {
                    finishActivity();
                }
            } else if (AlertUtils.ACTION_ALARM_TIMEOUT.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive AlertUtils.ACTION_ALARM_TIMEOUT");
                finishActivity();
            } else if (AlertUtils.ACTION_VOLUMEKEY_EVENT.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive AlertUtils.ACTION_VOLUMEKEY_EVENT");
                if (mAlarmType == AlertUtils.ALERT_DIALOG_NORMAL) {
                    doActionVolumeKey();
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                //To sleep 500ms in new thread if Htc Phone Sensor Functions enabled
                if (mSensorFunctions != null) {
                    mTimer = new Timer();
                    TimerTask task = new TimerTask() {
                        public void run() {
                            boolean isFlipAlarmSuccessful = mSensorFunctions.getIsFlipAlarmSuccessful();
                            Log.i(TAG, "isFlipAlarmSuccessful: " + isFlipAlarmSuccessful);
                            if (!isFlipAlarmSuccessful) {
                                doScreenOffAction(AlarmAlertReminder.this);
                            }
                            mTimer.cancel();
                            mTimer = null;
                        }
                    };
                    mTimer.schedule(task, AlarmUtils.SCREEN_OFF_DELAY_TIME);
                } else {
                    doScreenOffAction(AlarmAlertReminder.this);
                }
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    Log.i(TAG, "User press back key");
                    break;
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Log.i(TAG, "User press voulme key");
                    if (mAlarmType == AlertUtils.ALERT_DIALOG_NORMAL) {
                        doActionVolumeKey();
                    }
                    return true;
                default:
                    Log.i(TAG, "User press undefine key");
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (DEBUG_FLAG) Log.d(TAG, "onWindowFocusChanged: " + hasFocus);
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            // When user press Home Key, please unregister ViewMode 
            // to avoid the other lower priority view can't show.

            // You must unregister ViewMode when view disappear.
            if (mReminderManager != null) {
                mReminderManager.unregisterViewMode(mViewMode);
                mReminderManager.cleanUp();
                mReminderManager = null;
            }
        } else {
            // Control View State
            mReminderManager = AlarmAlertReminderManager.getInstance();
            if (mReminderManager != null) {
                mReminderManager.init(this);
                mReminderManager.initRegisterViewMode(this, HtcReminderViewMode.ALARM_MODE, mAlarmId, mAlarmTime, mAlarmDescription, AlertUtils.ALERT_DIALOG_NORMAL, true);
            }
        }
    }

    /**
     * user press power key or close phone cover
     * call snooze alarm action
     * @param context context
     */
    private void doScreenOffAction(Context context) {
        if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive Intent.ACTION_SCREEN_OFF");
        if (AlertUtils.isTopActivity(context) && AlertUtils.isCallStateIdle(context)) {
            Log.i(TAG, "onReceive: user press power key");
            AlertUtils.sendAlarmSnoozeIntent(context, mAlarmId, mAlarmDescription);
            finish();
        }
    }


    private void doActionVolumeKey() {
        AlarmUtils.VolumeBehavior volumeBehavior = AlarmUtils.getAlarmVolumeSideButtonBehavior(this);
        Log.i(TAG, "Side button behavior = " + volumeBehavior);
        switch (volumeBehavior) {
            case Vol_None:
                break;
            case Vol_Snooze:
                AlertUtils.sendAlarmSnoozeIntent(AlarmAlertReminder.this, mAlarmId, mAlarmDescription);
                finishActivity();
                break;
            case Vol_Dismiss:
                AlertUtils.sendAlarmDismissIntent(AlarmAlertReminder.this, mAlarmId);
                finishActivity();
                break;
            case Vol_Silent:
                MuteAlarm();
                break;
        }
    }
    
    private void MuteAlarm() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AlarmKlaxon klaxon = AlarmKlaxon.getInstance(AlarmAlertReminder.this);
                if (klaxon != null) {
                    klaxon.mute(AlarmAlertReminder.this);
                    klaxon = null;
                }
            }
        }).start();
    }
    
    private void finishActivity() {
        // You must unregister ViewMode when view disappear.
        if (mReminderManager != null) {
            mReminderManager.unregisterViewMode(mViewMode);
            mReminderManager = null;
        }
        finish();
    }

    private void unlock() {
        // Dismiss Keyguard
        if (mReminderManager != null) {
            if (DEBUG_FLAG) Log.d(TAG, "mReminderManager.unlock");
            mReminderManager.requestUnlockAndFinish(AlarmAlertReminder.this, null);
        }
    }
}
