/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.EXTRA_HOUR;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static android.provider.AlarmClock.EXTRA_MINUTES;

import java.util.Calendar;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.theme.ThemeType;

public class HandleSetAlarm extends Activity {
    private static final String TAG = "WorldClock.HandleSetAlarm";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    public static final String EXTRA_ALARM_RESULT = "com.htc.htcspeaker.extra.alarm.result";

    private int mHour;
    private int mMinute;
    private String mMessage;
    private HtcAlertDialog mHtcAlartDialog = null;

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
    
    // state control
    private HandleSetAlarmState mHandleSetAlarmState = new HandleSetAlarmState(HandleSetAlarmEnum.INIT);

    protected static enum HandleSetAlarmEnum {
        INIT, VOICESEARCH, HTCSPEAK, SETFAIL;
    }

    private class HandleSetAlarmState {
        private HandleSetAlarmEnum mState;

        HandleSetAlarmState(HandleSetAlarmEnum state) {
            this.mState = state;
            changeState(mState);
        }

        public HandleSetAlarmEnum getState() {
            return mState;
        }

        public void changeState(HandleSetAlarmEnum state) {
            if (DEBUG_FLAG) Log.d(TAG, "HandleSetAlarmState.changeState: from " + this.mState + " to " + state.toString());
            switch (state) {
                case INIT:
                    break;
                case VOICESEARCH:
                    if (mState == HandleSetAlarmEnum.SETFAIL) {
                        showUIDialog();
                    } else {
                        VoiceSearchSetAlarm();
                    }
                    break;
                case SETFAIL:
                    break;
                default:
                    if (DEBUG_FLAG) Log.d(TAG, "HandleSetAlarmState.changeState: NONE support state = " + state.toString());
            }
            this.mState = state;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][HandleSetAlarm][START]");
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(icicle);
        Intent intent = getIntent();
        if (DEBUG_FLAG) Log.d(TAG, "onCreate: intent.getAction() = " + intent.getAction());
        if ((intent == null) || !ACTION_SET_ALARM.equals(intent.getAction())) {
            finish();
            return;
        } else if (!intent.hasExtra(EXTRA_HOUR)) {
            Intent worldClockIntent = new Intent(this, WorldClockTabControl.class);
            worldClockIntent.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_ALARM);
            startActivity(worldClockIntent);
            finish();
            return;
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        mHour = intent.getIntExtra(EXTRA_HOUR,
            calendar.get(Calendar.HOUR_OF_DAY));
        mMinute = intent.getIntExtra(EXTRA_MINUTES,
            calendar.get(Calendar.MINUTE));
        mMessage = intent.getStringExtra(EXTRA_MESSAGE);

        if (DEBUG_FLAG) Log.d(TAG, "onCreate: receive hour = " + mHour);
        if (DEBUG_FLAG) Log.d(TAG, "onCreate: receive minutes = " + mMinute);
        if (DEBUG_FLAG) Log.d(TAG, "onCreate: receive message = " + mMessage);

        // check max alarm
        CheckMaxAlarm();
        boolean bSetAlarmFail = (mHandleSetAlarmState.getState() == HandleSetAlarmEnum.SETFAIL);

        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][HandleSetAlarm][DATA_READY]");
        mHandleSetAlarmState.changeState(HandleSetAlarmEnum.VOICESEARCH);
        // check activity left or not
        if (!bSetAlarmFail) {
            finish();
        }
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
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(HandleSetAlarm.this, HtcCommonUtil.TYPE_THEME);
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
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onDestroy();
    }

    private void VoiceSearchSetAlarm() {
        AlarmUtils.DaysOfWeek mDaysOfWeek = new AlarmUtils.DaysOfWeek();
        int mId;

        Uri uri = AlarmUtils.addAlarm(this, getContentResolver());
        if (uri != null) {
            String segment = uri.getPathSegments().get(1);
            mId = Integer.parseInt(segment);
            if (mId == AlarmUtils.INVALID_ALARMID) {
                return;
            }
        } else { // no alarm data
            return;
        }

        /* get audio alert from Setting */
        Uri alert = AlertUtils.getAlarmDefaultAlertUri(this);

        SetAlarm.saveAlarm(this, mId, true, mHour, mMinute,
            mDaysOfWeek, true, alert.toString(), mMessage, false, false, SetAlarm.RepeatTypeEnum.MON2FRI.ordinal(), true);
    }

    private void dismissHtcAlartDialog() {
        if (mHtcAlartDialog != null) {
            mHtcAlartDialog.dismiss();
            mHtcAlartDialog = null;
        }
    }

    private void CheckMaxAlarm() {
        Cursor cursor = AlarmUtils.getAlarmsCursor(getContentResolver());
        if ((cursor != null) && (cursor.getCount() >= AlertUtils.MAX_ALARM_COUNT)) {
            // set state to setalarm fail
            mHandleSetAlarmState.changeState(HandleSetAlarmEnum.SETFAIL);
        }

        if (cursor != null) {
            if (cursor.isClosed() == false) {
                cursor.close();
            }
        }
    }

    private void showUIDialog() {
        HtcAlertDialog.Builder alertDialogView = new HtcAlertDialog.Builder(this);
        alertDialogView.setTitle(getString(R.string.error));
        alertDialogView.setMessage(getString(R.string.add_alarm_error));
        alertDialogView.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });

        dismissHtcAlartDialog();
        mHtcAlartDialog = alertDialogView.create();
        mHtcAlartDialog.show();
    }
}
