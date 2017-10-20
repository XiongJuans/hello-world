package com.htc.android.worldclock.timer;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.ReminderActivity;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.lockscreen.reminder.HtcReminderViewMode;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

public class TimerAlertReminder extends ReminderActivity {
    private static final String TAG = "WorldClock.TimerAlertReminder";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private static final int ICON_DISMISS = 0;
    private static final int ICON_SETTINGS = 1;
    TimerAlertReminderView mReminderView;
    TimerAlertReminderManager mReminderManager;
    int mViewMode;
    IntentReceiver mIntentReceiver;

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
        setContentView(R.layout.specific_timer_alert_reminder);
        
        mReminderManager = TimerAlertReminderManager.getInstance();
        
        mReminderView = (TimerAlertReminderView) this.findViewById(R.id.alert_reminder_view);
        if (mReminderView != null) {
            this.setReminderView(mReminderView);
            mViewMode = mReminderView.getViewMode();
            mReminderView.setCallback(new TimerAlertReminderView.Callback() {
                @Override
                public void onTileDropEnd() {
                    AlertUtils.sendTimerDismissIntent(TimerAlertReminder.this);
                    unlockAndLaunchTimer();
                }
                @Override
                public void onButtonDrop(int index) {
                    if (DEBUG_FLAG) Log.d(TAG, "onButtonDrop: index = " + index);
                    if (index == ICON_DISMISS) {
                        AlertUtils.sendTimerDismissIntent(TimerAlertReminder.this);
                        if (mReminderManager != null) {
                            mReminderManager.requestShowIdleScreen();
                        }
                        finishActivity();
                    } else if (index == ICON_SETTINGS) {
                        AlertUtils.sendTimerDismissIntent(TimerAlertReminder.this);
                        unlockAndLaunchTimer();
                    }
                }
            });
        }

        registerIntentReceiver();
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
            filter.addAction(AlertUtils.ACTION_TIMER_TIMEOUT);
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
            if (DEBUG_FLAG) Log.d(TAG, "IntentReceiver onReceive: action = " + action);
            if (AlertUtils.LOCAL_ACTION_CANCEL_ALERT.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive AlertUtils.LOCAL_ACTION_CANCEL_ALERT");
                finishActivity();
            } else if (AlertUtils.ACTION_TIMER_TIMEOUT.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive AlertUtils.ACTION_TIMER_TIMEOUT");
                finishActivity();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive Intent.ACTION_SCREEN_OFF");
                if (AlertUtils.isTopActivity(context) && AlertUtils.isCallStateIdle(context)) {
                    Log.i(TAG, "onReceive: user press power key");
                    AlertUtils.sendTimerDismissIntent(context);
                    finishActivity();
                }
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    Log.i(TAG, "User press back key");
                    break;
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    Log.i(TAG, "User press voulme key");
                    break;
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
            // Timer alert reminder UI
            mReminderManager = TimerAlertReminderManager.getInstance();
            if (mReminderManager != null) {
                mReminderManager.init(this);
                mReminderManager.initRegisterViewMode(this, HtcReminderViewMode.TIMER_MODE, true);
            }
        }
    }

    private void finishActivity() {
        // You must unregister ViewMode when view disappear.
        if (mReminderManager != null) {
            mReminderManager.unregisterViewMode(mViewMode);
            mReminderManager = null;
        }
        finish();
    }

    private PendingIntent getTimerActivityPendingIntent(Context context) {
        Intent intent = new Intent();
        if (intent != null && context != null) {
            intent.setClassName(context.getPackageName(), WorldClockTabControl.LAUNCH_AP_ACTIVITY_NAME);
            intent.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_TIMER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            Log.w(TAG, "getTimerActivityPendingIntent: intent or context = null");
        }
        return PendingIntent.getActivity(context, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    
    private void unlockAndLaunchTimer() {
        // Dismiss Keyguard
        if (mReminderManager != null) {
            if (DEBUG_FLAG) Log.d(TAG, "mReminderManager.unlock");
            mReminderManager.requestUnlockAndFinish(TimerAlertReminder.this, getTimerActivityPendingIntent(TimerAlertReminder.this));
        }
    }
}
