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

import java.util.Date;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.voiceutils.VoiceManager;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcPhoneSensorFunctions;
import com.htc.android.worldclock.utils.HtcSkinUtils;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.theme.ThemeType;
import java.util.Timer;
import java.util.TimerTask;
/**
 * Alarm Clock alarm alert: pops visible indicator
 */
public class AlarmAlert extends Activity {
    private static final String TAG = "WorldClock.AlarmAlert";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private int mAlarmId;
    private long mAlarmTime;
    private String mAlarmDescription;

    private int mAlertType = AlertUtils.ALERT_DIALOG_NORMAL;
    private HtcAlertDialog mHtcAlartDialog;
    private IntentReceiver mIntentReceiver;
    
    // Htc font scale
    private boolean mHtcFontscale = false;
    // Htc Theme
    private boolean mIsThemeChanged = false;
    private HtcPhoneSensorFunctions mSensorFunctions;
    //for voice commands
    private VoiceManager mVoiceManager;
    private Timer mTimer;
    HtcCommonUtil.ThemeChangeObserver mThemeChangeObserver = new HtcCommonUtil.ThemeChangeObserver() {
        @Override
        public void onThemeChange(int type) {
                if (type == ThemeType.HTC_THEME_FULL || type == ThemeType.HTC_THEME_CC) {
                    mIsThemeChanged = true;
                }
        }
    };
    
    @Override
    protected void onCreate(Bundle icicle) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][AlarmAlert][START]");
        mHtcFontscale = HtcSkinUtils.initHtcFontScale(this);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        getTheme().applyStyle(R.style.ThemeDialog, true);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(icicle);
        setContentView(R.layout.specific_alert_background);
        ResUtils.enableStatusBarTheme(this);

        initFunction(getIntent());
        registerIntentReceiver();
        mVoiceManager = VoiceManager.getInstance(this);
        mVoiceManager.setDialogCallback(new UIVoiceManagerCallBack());
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][AlarmAlert][DATA_READY]");
        showDialogView(mAlertType);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        boolean isDoNothingFlipAction = AlarmUtils.getAlarmFlipAction(this) == AlarmUtils.FlipAction.Action_None;
        boolean isSupportSensorFeature = HtcPhoneSensorFunctions.isSupportSensorFeature(this);
        if (isSupportSensorFeature && !isDoNothingFlipAction) {
            mSensorFunctions = HtcPhoneSensorFunctions.getInstances(this);
        }
    }

    /**
     * this is called when a second alarm is triggered while a
     * previous alert window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG_FLAG) Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        initFunction(intent);
        showDialogView(mAlertType);
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
        AlertUtils.setAlarmSnoozeOrDismissByUser(false);
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(AlarmAlert.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
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
        dismissHtcAlartDialog();
        unregisterIntentReceiver();
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        if (mVoiceManager != null) {
            mVoiceManager.setDialogCallback(null);
        }
        super.onDestroy();
    }

    private void registerIntentReceiver() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new IntentReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(AlertUtils.LOCAL_ACTION_CANCEL_ALERT);
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
            if (DEBUG_FLAG) Log.d(TAG, "IntentReceiver onReceive: action = " + action);
            if (AlertUtils.LOCAL_ACTION_CANCEL_ALERT.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive AlertUtils.LOCAL_ACTION_CANCEL_ALERT");
                int id = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
                // check alarm id is match or not
                if ((AlarmUtils.INVALID_ALARMID != id) && (mAlarmId == id)) {
                    finish();
                }
            } else if (AlertUtils.ACTION_VOLUMEKEY_EVENT.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive AlertUtils.ACTION_VOLUMEKEY_EVENT");
                if (AlertUtils.ALERT_DIALOG_NORMAL == mAlertType) {
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
                                doScreenOffAction(AlarmAlert.this);
                            }
                            mTimer.cancel();
                            mTimer = null;
                        }
                    };
                    mTimer.schedule(task, AlarmUtils.SCREEN_OFF_DELAY_TIME);
                } else {
                    doScreenOffAction(AlarmAlert.this);
                }
            }
        }
    }
    
    private void initFunction(Intent intent) {
        if (intent != null) {
            mAlarmId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
            mAlarmTime = intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME);
            mAlarmDescription = intent.getStringExtra(AlarmUtils.DESCRIPTION);
            mAlertType = intent.getIntExtra(AlertUtils.EXTRA_ALERT_TYPE, AlertUtils.ALERT_DIALOG_NORMAL);

            if (DEBUG_FLAG) Log.d(TAG, "initFunction: mAlarmId = " + mAlarmId);
            if (DEBUG_FLAG) Log.d(TAG, "initFunction: mAlarmTime = " + mAlarmTime + "(" + new Date(mAlarmTime) + ")");
            if (DEBUG_FLAG) Log.d(TAG, "initFunction: mAlarmDescription = " + mAlarmDescription);
            if (DEBUG_FLAG) Log.d(TAG, "initFunction: mAlertType = " + mAlertType);
        }
    }
    
    private void showDialogView(int id) {
        HtcAlertDialog.Builder alertDialogView = new HtcAlertDialog.Builder(this);
        alertDialogView.setTitle(getString(R.string.alarm));
        alertDialogView.setOnKeyListener(mOnKeyListener);
        alertDialogView.setCancelable(false); // set touch outside of dialog to disable for ICS
        alertDialogView.setMessage("");

        switch (id) {
            case AlertUtils.ALERT_DIALOG_NORMAL:
                alertDialogView.setPositiveButton(R.string.alarm_alert_snooze_text, mSnoozeButtonListener);
                alertDialogView.setNegativeButton(R.string.alarm_alert_dismiss_text, mDismissButtonListener);
                break;
            case AlertUtils.ALERT_DIALOG_TIMEOUT:
                alertDialogView.setPositiveButton(R.string.alarm_alert_snooze_text, null);
                alertDialogView.setPositiveButtonDisabled(true);
                alertDialogView.setNegativeButton(R.string.alarm_alert_dismiss_text, mDismissButtonListener);
                break;
        }

        dismissHtcAlartDialog();
        mHtcAlartDialog = alertDialogView.create();
        mHtcAlartDialog.show();
        updateDialogContent();
    }

    private void updateDialogContent() {
        java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(this);
        if (DEBUG_FLAG) Log.d(TAG, "showDialogView: getTime = " + mTimeFormat.format(mAlarmTime));

        StringBuffer sb = new StringBuffer(mTimeFormat.format(mAlarmTime));
        if (!TextUtils.isEmpty(mAlarmDescription)) {
            sb.append("\n" + mAlarmDescription);
        }
        boolean isShowTips = mVoiceManager.getIsSupportForVoiceCommands();
        if (DEBUG_FLAG) Log.d(TAG, "canShowTips =" + isShowTips);
        if (isShowTips) {
            sb.append("\n\n" + getResources().getString(R.string.alarm_in_voice_mode_tips));
        }
        if (mHtcAlartDialog != null) {
            mHtcAlartDialog.setMessage(sb);
        }
    }

    private Dialog.OnKeyListener mOnKeyListener = new Dialog.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface arg0, int arg1, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                int keyCode = event.getKeyCode();
                if (DEBUG_FLAG) Log.d(TAG, "Dialog.onkey: code = " + keyCode);
                switch (keyCode) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        Log.i(TAG, "User press voulme key");
                        if (AlertUtils.ALERT_DIALOG_NORMAL == mAlertType) {
                            doActionVolumeKey();
                        }
                        break;
                    case KeyEvent.KEYCODE_BACK:
                        Log.i(TAG, "User press back key");
                        finish();
                        break;
                    default:
                        Log.i(TAG, "User press undefine key, keyCode = " + keyCode);
                        break;
                }
            }
            return true;
        }
    };

    private Dialog.OnClickListener mSnoozeButtonListener = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (DEBUG_FLAG) Log.d(TAG, "onClick: User press snooze button");
            AlertUtils.sendAlarmSnoozeIntent(AlarmAlert.this, mAlarmId, mAlarmDescription);
            finish();
        }
    };

    private Dialog.OnClickListener mDismissButtonListener = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (DEBUG_FLAG) Log.d(TAG, "onClick: User press dismiss button");
            AlertUtils.sendAlarmDismissIntent(AlarmAlert.this, mAlarmId);
            if (AlertUtils.ALERT_DIALOG_TIMEOUT == mAlertType) {
                AlertUtils.cancelAlarmNotification(AlarmAlert.this, mAlarmId);
            }
            finish();
        }
    };

    private void dismissHtcAlartDialog() {
        if (mHtcAlartDialog != null) {
            mHtcAlartDialog.dismiss();
            mHtcAlartDialog = null;
        }
    }

    private void MuteAlarm() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AlarmKlaxon klaxon = AlarmKlaxon.getInstance(AlarmAlert.this);
                if (klaxon != null) {
                    klaxon.mute(AlarmAlert.this);
                    klaxon = null;
                }
            }
        }).start();
    }

    /**
     * user press power key or close phone cover
     * call snooze alarm action
     * @param context context
     */
    private void doScreenOffAction(Context context) {
        if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive Intent.ACTION_SCREEN_OFF");
        if ((AlertUtils.ALERT_DIALOG_NORMAL == mAlertType) && AlertUtils.isTopActivity(context) && AlertUtils.isCallStateIdle(context)) {
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
                dismissHtcAlartDialog();
                AlertUtils.sendAlarmSnoozeIntent(AlarmAlert.this, mAlarmId, mAlarmDescription);
                finish();
                break;
            case Vol_Dismiss:
                dismissHtcAlartDialog();
                AlertUtils.sendAlarmDismissIntent(AlarmAlert.this, mAlarmId);
                finish();
                break;
            case Vol_Silent:
                MuteAlarm();
                break;
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        HtcSkinUtils.initHtcFontScale(this);
    }

    /**
     * for voice manager callback for UI update.
     */
    private class UIVoiceManagerCallBack implements VoiceManager.VoiceManagerUICallBack {

        @Override
        public void showTips(boolean show) {
            updateDialogContent();
        }
    }

}
