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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.alarmclock.AlarmUtils.AlarmColumns;
import com.htc.android.worldclock.timer.Timer;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.SettingsActivity;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.theme.ThemeType;

public class HandleApiCalls extends Activity {
    private static final String TAG = "WorldClock.HandleApiCalls";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private static final int REQUEST_ADD = 1;
    public static final String CTS_TEST_ALARM_STRING = "Start Alarm Test";
    public static final String CTS_TEST_TIMER_STRING = "Start Timer Test";
    public static final long TIMER_MIN_LENGTH = 1000;
    public static final long TIMER_MAX_LENGTH = 24 * 60 * 60 * 1000;
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
        try {
            HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
            // For Theme Change
            HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
            HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
            super.onCreate(icicle);
            Intent intent = getIntent();
            if (intent != null) {
                if (AlarmClock.ACTION_SET_ALARM.equals(intent.getAction())) {
                    if (DEBUG_FLAG) Log.d(TAG, "onCreate: handleSetAlarm case");
                    handleSetAlarm(intent);
                } else if (AlarmClock.ACTION_SHOW_ALARMS.equals(intent.getAction())) {
                    if (DEBUG_FLAG) Log.d(TAG, "onCreate: handleShowAlarms case");
                    handleShowAlarms();
                    finish();
                } else if (AlarmClock.ACTION_SNOOZE_ALARM.equals(intent.getAction())) {
                    if (DEBUG_FLAG) Log.d(TAG, "onCreate: handleSnoozeAlarm case");
                    handleSnoozeAlarm();
                    finish();
                }  else if (AlarmClock.ACTION_DISMISS_ALARM.equals(intent.getAction())) {
                    if (DEBUG_FLAG) Log.d(TAG, "onCreate: handleDismissAlarm case");
                    handleDismissAlarm();
                    finish();
                } else if (AlarmClock.ACTION_SET_TIMER.equals(intent.getAction())) {
                    if (DEBUG_FLAG) Log.d(TAG, "onCreate: handleSetTimer case");
                    handleSetTimer(intent);
                    finish();
                } else if (AlertUtils.ACTION_DISALBE_ALARM.equals(intent.getAction())) {
                    if (DEBUG_FLAG) Log.d(TAG, "onCreate: handleDisableAlarm case");
                    handleDisableAlarm();
                    finish();
                }
            }
        } finally {
//            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(HandleApiCalls.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
    }

    @Override
    protected void onDestroy() {
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onDestroy();
    }

    /***
     * Processes the SET_ALARM intent
     * @param intent
     */
    private void handleSetAlarm(Intent intent) {
        // If not provided or invalid, show UI
        final int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1);

        // If not provided, use zero. If it is provided, make sure it's valid, otherwise, show UI
        final int minutes;
        if (intent.hasExtra(AlarmClock.EXTRA_MINUTES)) {
            minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1);
        } else {
            minutes = 0;
        }
        if (DEBUG_FLAG) Log.d(TAG, "handleSetAlarm: hour = " + hour);
        if (DEBUG_FLAG) Log.d(TAG, "handleSetAlarm: minutes = " + minutes);
        if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
            // Intent has no time or an invalid time, open the alarm creation UI
            Intent setAlarmIntent = new Intent(this, SetAlarm.class);
            setAlarmIntent.putExtra(AlarmUtils.ID, -1);
            startActivity(setAlarmIntent);
            Voice.notifyFailure(this, getString(R.string.bt_cancel_str));
            finish();
            return;
        }

        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (DEBUG_FLAG) Log.d(TAG, "handleSetAlarm: skipUi = " + skipUi);
        final boolean vibrate = intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, false);
        if (DEBUG_FLAG) Log.d(TAG, "handleSetAlarm: vibrate = " + vibrate);
        final String alert = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE);
        if (DEBUG_FLAG) Log.d(TAG, "handleSetAlarm: alert = " + alert);

        String message = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        message = (message == null) ? "" : message;
        if (DEBUG_FLAG) Log.d(TAG, "handleSetAlarm: message = " + message);

        int[] days = null;
        boolean isHasDaysExtra = intent.hasExtra(AlarmClock.EXTRA_DAYS);
        if (DEBUG_FLAG) Log.d(TAG, "handleSetAlarm: isHasDaysExtra = " + isHasDaysExtra);
        if (isHasDaysExtra) {
            ArrayList<Integer> arrayDays = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
            days = new int[arrayDays.size()];
            for (int i = 0; i < arrayDays.size(); i++) {
                days[i] = arrayDays.get(i);
            }
        }

        if(!isMaxAlarm()) {
            apiCallsSetAlarm(hour, minutes, message, days, alert, vibrate, skipUi);
            Voice.notifySuccess(this, getString(R.string.bt_done_str));
            finish();
        } else {
            showDialog(REQUEST_ADD);
            Voice.notifyFailure(this, getString(R.string.bt_cancel_str));
        }
    }

    private void apiCallsSetAlarm(int hour, int minutes, String message, int[] days, String alertString, boolean vibrate, boolean skipUi) {
        AlarmUtils.DaysOfWeek daysOfWeek = new AlarmUtils.DaysOfWeek();
        if (days != null) {
            daysOfWeek.setDaysOfWeek(true, days);
        }
        
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
        
        String alarmSoundUriString = null;
        if ("silent".equals(alertString)) {
            alarmSoundUriString = null;
        } else if (TextUtils.isEmpty(alertString)) {
            alarmSoundUriString  = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
        } else {
            /* get audio alert from Setting */
            Uri alert = AlertUtils.getAlarmDefaultAlertUri(this);
            if (alert != null) {
                alarmSoundUriString = alert.toString();
            }
        }

        if (DEBUG_FLAG) Log.d(TAG, "apiCallsSetAlarm: mask = " + Integer.toBinaryString(daysOfWeek.getCoded()));
        SetAlarm.saveAlarm(this, mId, true, hour, minutes, daysOfWeek, vibrate, alarmSoundUriString, message, false, false, SetAlarm.RepeatTypeEnum.MON2FRI.ordinal(), true);

        if (!skipUi) {
            handleShowAlarms();
        }
    }

    private boolean isMaxAlarm() {
        boolean retValue = false;
        Cursor cursor = AlarmUtils.getAlarmsCursor(getContentResolver());
        if (cursor != null) {
            if (DEBUG_FLAG) Log.d(TAG, "CheckMaxAlarm: cursor.getCount() = " + cursor.getCount());
        }
        if ((cursor != null) && (cursor.getCount() >= AlertUtils.MAX_ALARM_COUNT)) {
            retValue = true;
        }

        if (cursor != null) {
            if (cursor.isClosed() == false) {
                cursor.close();
            }
        }
        return retValue;
    }

    private void handleShowAlarms() {
        Intent alarmClockIntent = new Intent();
        if (alarmClockIntent != null) {
            alarmClockIntent.setClassName(getPackageName(), WorldClockTabControl.LAUNCH_AP_ACTIVITY_NAME);
            alarmClockIntent.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_ALARM);
            alarmClockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            alarmClockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(alarmClockIntent);
        }
    }

    /**
     * dismiss firing alert when alarm or time ring but only dismiss one alert when both alarm and timer ring.
     * @param context context
     * @param activity activity
     * @param isFiringTimer firing time
     * @param alarmId firing alarm id
     */
    private static void dismissFiringAlert(Context context, Activity activity, boolean isFiringTimer, int alarmId) {
        // only allow on background thread
        String reason = "";
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("dismissAlert must be called on a " + "background thread");
        }
        if (isFiringTimer) {
            AlertUtils.sendTimerDismissIntent(context);
            reason = context.getString(R.string.timer_alert_dismissed);
            if (DEBUG_FLAG) Log.d(TAG, "dismissTimer: reason = " + reason);
        } else {
            AlertUtils.sendAlarmDismissIntent(context, alarmId);
            reason = context.getString(R.string.alarm_alert_dismissed);
            if (DEBUG_FLAG) Log.d(TAG , "dismissAlarm: reason = " + reason);
        }
        Voice.notifySuccess(activity, reason);
    }

    private void handleDismissAlarm() {
        final Intent intent = getIntent();
        new DismissAlarmAsync(this, intent, this).execute();
    }

    private void handleDisableAlarm() {
        final Intent intent = getIntent();
        new DisableAlarmAsync(this, intent, this).execute();
    }

    private static class DisableAlarmAsync extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final Intent mIntent;
        private final Activity mActivity;

        public DisableAlarmAsync(Context context, Intent intent, Activity activity) {
            mContext = context;
            mIntent = intent;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int alarmId = mIntent.getIntExtra("alarm_id",  AlarmUtils.INVALID_ALARMID);
            if (AlarmUtils.INVALID_ALARMID == alarmId) {
                if (DEBUG_FLAG) Log.d(TAG, "disableAlarm: invalid alarmid");
                return null;
            }
            dismissAlarm(alarmId, mContext, mActivity);
            return null;
        }
    }

    public static void dismissAlarm(int alarmId, Context context, Activity activity) {
        // only allow on background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("dismissAlarm must be called on a " + "background thread");
        }

        AlarmUtils.enableAlarm(activity, alarmId, false);
        final String reason = context.getString(R.string.pick_alarm_to_dismiss);
        if (DEBUG_FLAG) Log.d(TAG, "dismissAlarm: reason = " + reason);
        Voice.notifySuccess(activity, reason);
    }

    private static class DismissAlarmAsync extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final Intent mIntent;
        private final Activity mActivity;

        public DismissAlarmAsync(Context context, Intent intent, Activity activity) {
            mContext = context;
            mIntent = intent;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            boolean isFiringTimer = PreferencesUtil.getIsFiringTimer(mContext);
            if (isFiringTimer) {
                dismissFiringAlert(mContext, mActivity, true, AlarmUtils.INVALID_ALARMID);
                return null;
            }
            final List<AlarmItem> alarms = getEnabledAlarms(mContext);
            if (alarms.isEmpty()) {
                final String reason = mContext.getString(R.string.no_scheduled_alarms);
                if (DEBUG_FLAG) Log.d(TAG, "alarms is empty: reason = " + reason);
                Voice.notifyFailure(mActivity, reason);
                return null;
            }

            // fetch the alarms that are specified by the intent
            final FetchMatchingAlarmsAction fmaa = new FetchMatchingAlarmsAction(mContext, alarms, mIntent, mActivity);
            fmaa.run();
            final List<AlarmItem> matchingAlarms = fmaa.getMatchingAlarms();

            if (matchingAlarms.isEmpty()) {
                final String reason = mContext.getString(R.string.no_scheduled_alarms);
                if (DEBUG_FLAG) Log.d(TAG, "matchingAlarms is empty: reason = " + reason);
                Voice.notifyFailure(mActivity, reason);
                return null;
            } else {
                int alarmId = PreferencesUtil.getCurrentFiringAlarm(mContext);
                if (AlarmUtils.INVALID_ALARMID == alarmId) {
                    final String reason = mContext.getString(R.string.no_firing_alarms);
                    if (DEBUG_FLAG) Log.d(TAG, "dismissAlarm: reason = " + reason);
                    Voice.notifyFailure(mActivity, reason);
                    return null;
                }
                dismissFiringAlert(mContext, mActivity, false, alarmId);
                // Apply the action to the matching alarms
                /*for (AlarmItem alarm : matchingAlarms) {
                    dismissAlarm(alarm.aId, mContext, mActivity);
                    if (DEBUG_FLAG) Log.d(TAG, "Alarm " + alarm.aId + " is dismissed");
                }*/
                return null;
            }
        }

        private static List<AlarmItem> getEnabledAlarms(Context context) {
            final String selection = String.format("%s=?", AlarmColumns.ENABLED);
            final String[] args = { "1" };
            
            Cursor cursor  = context.getContentResolver().query(AlarmColumns.CONTENT_URI, AlarmColumns.ALARM_QUERY_COLUMNS, selection, args, null);
            List<AlarmItem> result = new LinkedList<AlarmItem>();
            if (cursor == null) {
                return result;
            }

            try {
                if (cursor.moveToFirst()) {
                    do {
                        if (cursor != null) {
                            AlarmItem ai = AlarmUtils.parseCursor(cursor);
                            if (ai != null) {
                                result.add(ai);
                            }
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }

            return result;
        }
    }
    
    private void handleSnoozeAlarm() {
        new SnoozeAlarmAsync(this, this).execute();
    }

    private static class SnoozeAlarmAsync extends AsyncTask<Void, Void, Void> {

        private final Context mContext;
        private final Activity mActivity;

        public SnoozeAlarmAsync(Context context, Activity activity) {
            mContext = context;
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... parameters) {
            int alarmId = PreferencesUtil.getCurrentFiringAlarm(mContext);
            if (AlarmUtils.INVALID_ALARMID == alarmId) {
                final String reason = mContext.getString(R.string.no_firing_alarms);
                if (DEBUG_FLAG) Log.d(TAG, "snoozeAlarm: reason = " + reason);
                Voice.notifyFailure(mActivity, reason);
                return null;
            }

            snoozeAlarm(mContext, mActivity, alarmId);
            return null;
        }
    }

    static void snoozeAlarm(Context context, Activity activity, int alarmId) {
        // only allow on background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("snoozeAlarm must be called on a " + "background thread");
        }
        AlertUtils.sendAlarmSnoozeIntent(context, alarmId, "");
        String snoozeMinutesString = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_ALARM_SNOOZE, AlertUtils.DEFAULT_SNOOZE);
        int snoozeMinutes = Integer.parseInt(snoozeMinutesString);
        final String reason = context.getString(R.string.alarm_alert_snooze_set, snoozeMinutes);
        if (DEBUG_FLAG) Log.d(TAG, "snoozeAlarm: reason = " + reason);
        Voice.notifySuccess(activity, reason);
    }
    
    private void handleShowTimer() {
        Intent timerClockIntent = new Intent();
        if (timerClockIntent != null) {
            timerClockIntent.setClassName(getPackageName(), WorldClockTabControl.LAUNCH_AP_ACTIVITY_NAME);
            timerClockIntent.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_TIMER);
            timerClockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            timerClockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(timerClockIntent);
        }
    }
    
    private void handleSetTimer(Intent intent) {
        // If no length is supplied , show the timer setup view
        if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
            handleShowTimer();
            return;
        }

        final long length = 1000l * intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0);
        if (DEBUG_FLAG) Log.d(TAG, "handleSetTimer: length = " + length);
        if (length < TIMER_MIN_LENGTH || length > TIMER_MAX_LENGTH) {
            Log.w(TAG, "Invalid timer length requested: " + length);
            return;
        }

        String message = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        message = (message == null) ? "" : message;
        if (DEBUG_FLAG) Log.d(TAG, "handleSetTimer: message = " + message);

        long time = System.currentTimeMillis() + length;
        Timer.enableAlert(this, time);
        saveDataToPreference(length, message);
        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (DEBUG_FLAG) Log.d(TAG, "handleSetTimer: skipUi = " + skipUi);
        if (!skipUi) {
            handleShowTimer();
        }
    }

    private void saveDataToPreference(long userChoiceTime, String label) {
        long startTime = SystemClock.elapsedRealtime() + 1000; // 1000 for UI to show user choice time
        PreferencesUtil.setTimerState(this, Timer.TimerEnum.PLAY.ordinal());
        PreferencesUtil.setTimerUserChoiceTime(this, userChoiceTime);
        PreferencesUtil.setTimerStartTime(this, startTime);
        PreferencesUtil.setTimerExpireTime(this, startTime + userChoiceTime);
        PreferencesUtil.setTimerPauseTime(this, 0);
        PreferencesUtil.setTimerLabel(this, label);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case REQUEST_ADD:
                int titleResId = R.string.error;
                int messageResId = R.string.add_alarm_error;
                return new HtcAlertDialog.Builder(this)
                .setTitle(getString(titleResId))
                .setMessage(getString(messageResId))
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
        }
        return null;
    }
}
