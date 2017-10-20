package com.htc.android.worldclock.timer;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.HandleApiCalls;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcSkinUtils;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.theme.ThemeType;

/**
 * Timer alert: pops visible indicator
 */
public class TimerAlert extends Activity {
    private static final String TAG = "WorldClock.TimerAlert";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private HtcAlertDialog mHtcAlartDialog;
    private IntentReceiver mIntentReceiver;
    private int mAlertType = AlertUtils.ALERT_DIALOG_NORMAL;
    // Htc font scale
    private boolean mHtcFontscale = false;
    // Htc Theme
    private boolean mIsThemeChanged = false;

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
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][TimerAlert][START]");
        
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

        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][TimerAlert][DATA_READY]");
        showDialogView(mAlertType);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

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
        AlertUtils.setTimerDismissByUser(false);
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(TimerAlert.this, HtcCommonUtil.TYPE_THEME);
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
        super.onDestroy();
    }

    private void registerIntentReceiver() {
        if (mIntentReceiver == null) {
            mIntentReceiver = new IntentReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(AlertUtils.LOCAL_ACTION_CANCEL_ALERT);
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
                finish();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (DEBUG_FLAG) Log.d(TAG, "onReceive: Receive Intent.ACTION_SCREEN_OFF");
                if ((AlertUtils.ALERT_DIALOG_NORMAL == mAlertType) && AlertUtils.isTopActivity(context) && AlertUtils.isCallStateIdle(context)) {
                    Log.i(TAG, "onReceive: user press power key");
                    AlertUtils.sendTimerDismissIntent(context);
                    finish();
                }
            }
        }
    }

    private void initFunction(Intent intent) {
        if (intent != null) {
            mAlertType = intent.getIntExtra(AlertUtils.EXTRA_ALERT_TYPE, AlertUtils.ALERT_DIALOG_NORMAL);
            if (DEBUG_FLAG) Log.d(TAG, "initFunction: mAlertType = " + mAlertType);
        }
    }
    
    private void dismissHtcAlartDialog() {
        if (mHtcAlartDialog != null) {
            mHtcAlartDialog.dismiss();
            mHtcAlartDialog = null;
        }
    }

    private void showDialogView(int id) {
        HtcAlertDialog.Builder alertDialogView = new HtcAlertDialog.Builder(this);
        alertDialogView.setTitle(this.getString(R.string.timer_caption));
        if (HandleApiCalls.CTS_TEST_TIMER_STRING.equals(PreferencesUtil.getTimerLabel(this))) {
            alertDialogView.setMessage(HandleApiCalls.CTS_TEST_TIMER_STRING);
        } else {
            alertDialogView.setMessage(R.string.time_up);
        }
        alertDialogView.setOnKeyListener(mOnKeyListener);
        alertDialogView.setCancelable(false); // set touch outside of dialog to disable for ICS
        alertDialogView.setNegativeButton(R.string.alarm_alert_dismiss_text, mDismissButtonListener);

        dismissHtcAlartDialog();
        mHtcAlartDialog = alertDialogView.create();
        mHtcAlartDialog.show();
    };

    private Dialog.OnKeyListener mOnKeyListener = new Dialog.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface arg0, int arg1, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                int keyCode = event.getKeyCode();
                if (DEBUG_FLAG) Log.d(TAG, "Dialog.onkey: code = " + keyCode);
                switch (keyCode) {
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

    private Dialog.OnClickListener mDismissButtonListener = new Dialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (DEBUG_FLAG) Log.d(TAG, "onClick: User press dismiss button");
            if (AlertUtils.ALERT_DIALOG_TIMEOUT == mAlertType) {
                AlertUtils.cancelTimerNotification(TimerAlert.this);
            } else {
                AlertUtils.sendTimerDismissIntent(TimerAlert.this);
            }
            finish();
        }
    };
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        HtcSkinUtils.initHtcFontScale(this);
    }
}
