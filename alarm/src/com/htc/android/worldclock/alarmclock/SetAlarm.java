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

import java.util.ArrayList;
import java.util.Calendar;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.AlarmUtils.DaysOfWeek;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.ClockListItem;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcSkinUtils;
import com.htc.android.worldclock.utils.HtcStorageChecker;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.SDMConst;
import com.htc.android.worldclock.utils.ToastMaster;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.ActionBarUtil;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.cc.widget.HtcEditText;
import com.htc.lib1.cc.widget.HtcListItem;
import com.htc.lib1.cc.widget.HtcTimePicker;
import com.htc.lib1.theme.ThemeType;

/**
 * Manages each alarm
 */
public class SetAlarm extends Activity implements AlarmUtils.AlarmSettings {
    private static final String TAG = "WorldClock.SetAlarm";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private final int UI_MSG_UPDATE_ALARM_SOUND_NAME = 0x0001;
    private final int UI_MSG_UPDATE_ALARM_REPEAT = 0x0002;
    private final int UI_MSG_UPDATE_ALARM_VIBRATE = 0x0003;
    private final int UI_MSG_UPDATE_ALARM_OFFALARM = 0x0004;
    private final int NONUI_MSG_LOAD_DATA = 0x0100;
    private static final String SDMSHOWDEFAULT = "SDMShowDefault";

    private Looper mNonUILooper = null;
    private Handler mNonUIHandler = null;

    public static final String HOUR = "hour";
    public static final String MINUTES = "minutes";
    public static final String DESCRIPTION = "description";
    public static final String VIBRATE = "vibrate";
    public static final String TEMPREPEAT = "tempRepeat";
    public static final String REPEAT = "repeat";

    static final int REQUEST_ALARM_SOUND = 0;
    static final int REQUEST_REPEAT = 1;
    static final int INTERNAL_STORAGE_FULL = 2;
    static final int REQUEST_ADD = 3;
    static final int REQUEST_PERMISSION_GRANTED = 4;
    static final int REQUEST_CUSTOM = 5;

    static final int SCROLLVIEW_TRAVERSE_LEVEL = 1;

    private HtcTimePicker mHtcTimePicker;
    private HtcEditText mDescriptionText;

    private int mId;
    private int mHour;
    private int mMinutes;
    private String mDescription;
    private boolean mVibrate;
    private boolean mOffAlarm;
    private int mRepeatType;
    private AlarmUtils.DaysOfWeek mDaysOfWeek = new AlarmUtils.DaysOfWeek();
    private AlarmUtils.DaysOfWeek mTempDaysOfWeek = new AlarmUtils.DaysOfWeek();

    private InputMethodManager mInputMethodManager;

    private MediaReceiver mMediaReceiver = null;
    private SetAlarmResUtils mSetAlarmResUtils;

    private String mAlarmSoundTitle;
    private String mAlarmSoundUriString;

    private HtcListItem mDescriptionInputText;
    private ClockListItem mRepeatLayout;
    private ClockListItem mRingToneLayout;
    private ClockListItem mVibrateLayout;

    private CheckBox mVibrateCheckBox;
    private ClockListItem mOffAlarmLayout;
    private CheckBox mOffAlarmCheckBox;

    private LinearLayout mSetAlarmList;

    private int mStartWeekDay = 1; // 1 -- Sunday; 2 -- Monday
    private boolean isCTSLaunchAlarmAndMaxAlarm = false;
    private boolean mShouldShowRequestPermissionRationale = false;
    private boolean mRingtoneClicked = false;
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
    
    protected static enum SetAlarmEnum {
        INIT, NORMAL, PAUSE, END
    }

    // state control
    private SetAlarmState mSetAlarmState = new SetAlarmState(SetAlarmEnum.INIT);

    private class SetAlarmState {
        private SetAlarmEnum mPrevState;

        SetAlarmState(SetAlarmEnum initState) {
            changeState(initState);
        }

        public SetAlarmEnum getState() {
            return mPrevState;
        }

        public void changeState(SetAlarmEnum currentState) {
            if (DEBUG_FLAG) Log.d(TAG, "SetAlarmState.changeState: " + this.mPrevState + " -> " + currentState.toString());
            switch (currentState) {
                case INIT:
                    break;
                case NORMAL:
                    break;
                case PAUSE:
                    break;
                case END:
                    break;
                default:
                    if (DEBUG_FLAG) Log.d(TAG, "SetAlarmState.changeState: No support state = " + currentState.toString());
            }
            this.mPrevState = currentState;
        }
    }

    public static enum RepeatTypeEnum {
        MON2FRI, DONOTREPEAT, EVERYDAY, SKIPHOLIDAY, CUSTOM
    }

    /**
     * Set an alarm. Requires an AlarmUtils.ID to be passed in as an
     * extra
     */
    @Override
    protected void onCreate(Bundle icicle) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][SetAlarm][START]");
        
        mHtcFontscale = HtcSkinUtils.initHtcFontScale(this);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(icicle);
        setContentView(R.layout.main_set_alarm);
        if (mSetAlarmState.getState() == SetAlarmEnum.INIT) {
            mSetAlarmResUtils = new SetAlarmResUtils(this, null);
            mSetAlarmResUtils.initResources();
            initNonUIHandlerThread();
            initUIFuntion();
        }
        checkStoragePermission(false);
    }

    @Override
    protected void onStart() {
        if (DEBUG_FLAG) Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG_FLAG) Log.d(TAG, "onResume");
        super.onResume();
        HtcStorageChecker.checkStorageFull(this);
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(SetAlarm.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
        mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);
    }

    @Override
    protected void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        if ((mInputMethodManager != null) && (mDescriptionText != null)) {
            mInputMethodManager.hideSoftInputFromWindow(mDescriptionText.getWindowToken(), 0);
        }
        mSetAlarmState.changeState(SetAlarmEnum.PAUSE);
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
        removeAllHandlerMessages();
        unInitRegister(); // to prevent in low memory,onStop never called.
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        mSetAlarmState.changeState(SetAlarmEnum.END);
        super.onDestroy();
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        int categoryColor = HtcCommonUtil.getCommonThemeColor(this, com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color);
        Drawable actionBarColorDrawable = new ColorDrawable(categoryColor);
       ActionBarUtil.setActionModeBackground(this, mode, actionBarColorDrawable);
    }

    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_MSG_UPDATE_ALARM_SOUND_NAME:
                    if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_UPDATE_ALARM_SOUND_NAME");
                    mSetAlarmResUtils.setAlarmSoundName(mAlarmSoundTitle);
                    break;
                case UI_MSG_UPDATE_ALARM_REPEAT:
                    if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_UPDATE_ALARM_REPEAT");
                    if ((Global.isSupportAccChinaSense()) && (RepeatTypeEnum.SKIPHOLIDAY.ordinal() == mRepeatType)) {
                        mSetAlarmResUtils.setAlarmRepeat(getResources().getTextArray(R.array.repeat_option)[RepeatTypeEnum.SKIPHOLIDAY.ordinal()].toString());
                    } else {
                        mSetAlarmResUtils.setAlarmRepeat(mDaysOfWeek.toString(SetAlarm.this, true, mStartWeekDay));
                        //Override setContentDescription method to solve pronounce format week.
                        mSetAlarmResUtils.setAlarmRepeatDescription(mDaysOfWeek.toContentDescriptionString(SetAlarm.this, true, mStartWeekDay));
                    }
                    break;
                case UI_MSG_UPDATE_ALARM_VIBRATE:
                    if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_UPDATE_ALARM_VIBRATE");
                    mSetAlarmResUtils.setAlarmVibrateCheckBox(mVibrate);
                    break;
                case UI_MSG_UPDATE_ALARM_OFFALARM:
                    if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_UPDATE_ALARM_OFFALARM");
                    mSetAlarmResUtils.setAlarmOffAlarmCheckBox(mOffAlarm);
                    break;
            }
        }
    };

    private final void initNonUIHandlerThread() {
        HandlerThread nonUIHandlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        nonUIHandlerThread.start();
        mNonUILooper = nonUIHandlerThread.getLooper();
        mNonUIHandler = new Handler(mNonUILooper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case NONUI_MSG_LOAD_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_LOAD_DATA");
                        getAlarmSoundByUriString();
                        break;
                }
            }
        };
    }

    private void initUIFuntion() {
        if (DEBUG_FLAG) Log.d(TAG, "initUIFuntion");
        initHtcTimePicker();

        mSetAlarmList = (LinearLayout) findViewById(R.id.setAlarm_list);

        mDescriptionText = (HtcEditText) findViewById(R.id.edit_description);
        mDescriptionInputText = (HtcListItem) findViewById(R.id.description_input_text);
        mRingToneLayout = (ClockListItem) findViewById(R.id.setAlarm_ringtone);
        mRepeatLayout = (ClockListItem) findViewById(R.id.setAlarm_repeat);
        mVibrateLayout = (ClockListItem) findViewById(R.id.setAlarm_vibrate);
        mVibrateCheckBox = (CheckBox) mVibrateLayout.findViewById(R.id.vibrate);
        mOffAlarmLayout = (ClockListItem) findViewById(R.id.setAlarm_offalarm);
        mOffAlarmCheckBox = (CheckBox) mOffAlarmLayout.findViewById(R.id.offalarm);
        
        if (AlarmUtils.isNotShowOffAlarmUI(this)) {
            ClockListItem offalarm = (ClockListItem) findViewById(R.id.setAlarm_offalarm);
            if (offalarm != null) {
                offalarm.setVisibility(View.GONE);
            }
            ImageView offalarm_divider = (ImageView) findViewById(R.id.setAlarm_offalarm_feature_divider);
            if (offalarm_divider != null) {
                offalarm_divider.setVisibility(View.GONE);
            }
        }

        TypedArray a = obtainStyledAttributes(R.style.HtcListView, R.styleable.list_selector);
        Drawable d1 = a.getDrawable(R.styleable.list_selector_android_listSelector);
        Drawable d2 = a.getDrawable(R.styleable.list_selector_android_listSelector);
        Drawable d3 = a.getDrawable(R.styleable.list_selector_android_listSelector);
        Drawable d4 = a.getDrawable(R.styleable.list_selector_android_listSelector);
        a.recycle();

        mRingToneLayout.setBackground(d1);
        mRepeatLayout.setBackground(d2);
        mVibrateLayout.setBackground(d3);
        mOffAlarmLayout.setBackground(d4);
        
        mRingToneLayout.setFocusable(true);
        mRepeatLayout.setFocusable(true);
        mVibrateLayout.setFocusable(true);
        mVibrateLayout.setLastComponentAlign(true);
        mOffAlarmLayout.setFocusable(true);
        mOffAlarmLayout.setLastComponentAlign(true);
        mRingToneLayout.setOnClickListener(mRingToneLayoutClickListener);
        mRepeatLayout.setOnClickListener(mRepeatLayoutClickListener);
        mVibrateLayout.setOnClickListener(mVibrateLayoutClickListener);
        mVibrateCheckBox.setOnClickListener(mCheckBoxClickListener);
        mOffAlarmLayout.setOnClickListener(mOffAlarmLayoutClickListener);
        mOffAlarmCheckBox.setOnClickListener(mOffAlarmCheckBoxClickListener);
        findViewById(R.id.cmd_bar_btn_1).setOnClickListener(mDoneBtnClickListener);
        findViewById(R.id.cmd_bar_btn_2).setOnClickListener(mCancelBtnClickListener);

        initRegister();

        loadDataFromDatabase();
        updateTime();
        mSetAlarmResUtils.setAlarmDescription(mDescription);
        if (mId == AlarmUtils.INVALID_ALARMID) {
            // set alert sound cached title of new alarm
            mAlarmSoundTitle = PreferencesUtil.getAlarmSoundCachedTitle(this);
            mSetAlarmResUtils.setAlarmSoundName(mAlarmSoundTitle);
        }
        mStartWeekDay = AlarmUtils.getCalendarStartWeekday(this);
        if ((Global.isSupportAccChinaSense()) && (RepeatTypeEnum.SKIPHOLIDAY.ordinal() == mRepeatType)) {
            mSetAlarmResUtils.setAlarmRepeat(getResources().getTextArray(R.array.repeat_option)[RepeatTypeEnum.SKIPHOLIDAY.ordinal()].toString());
        } else {
            mSetAlarmResUtils.setAlarmRepeat(mDaysOfWeek.toString(SetAlarm.this, true, mStartWeekDay));
            mSetAlarmResUtils.setAlarmRepeatDescription(mDaysOfWeek.toContentDescriptionString(SetAlarm.this, true, mStartWeekDay));
        }
        mSetAlarmResUtils.setAlarmVibrateCheckBox(mVibrate);
        mSetAlarmResUtils.setAlarmOffAlarmCheckBox(mOffAlarm);
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][SetAlarm][DATA_READY]");
        mSetAlarmState.changeState(SetAlarmEnum.NORMAL);
    }

    private void initHtcTimePicker() {
        // initial HtcTimePicker
        mHtcTimePicker = (HtcTimePicker) findViewById(R.id.timerPicker);
        mHtcTimePicker.setRepeatEnable(true);
        mHtcTimePicker.setEnabled(true);
        mHtcTimePicker.setMinuteRange(0, 59, true);

        if (!AlarmUtils.get24HourMode(this)) {
            mHtcTimePicker.setHourRange(1, 12);
            String[] ampm = new String[2];
            ampm[0] = getBaseContext().getString(R.string.am);
            ampm[1] = getBaseContext().getString(R.string.pm);
            mHtcTimePicker.setAmPmRange(0, 1, ampm);
        } else {
            mHtcTimePicker.setHourRange(0, 23);
        }
    }

    private void removeAllHandlerMessages() {
        if (mNonUILooper != null) {
            mNonUILooper.quit();
        }

        mMainHandler.removeMessages(UI_MSG_UPDATE_ALARM_SOUND_NAME);
        mMainHandler.removeMessages(UI_MSG_UPDATE_ALARM_REPEAT);
        mMainHandler.removeMessages(UI_MSG_UPDATE_ALARM_VIBRATE);
        mMainHandler.removeMessages(UI_MSG_UPDATE_ALARM_OFFALARM);
        mNonUIHandler.removeMessages(NONUI_MSG_LOAD_DATA);
    }

    private void loadDataFromDatabase() {
        mInputMethodManager = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        Intent i = getIntent();
        mId = i.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
        if (DEBUG_FLAG) Log.d(TAG, "loadDataFromDatabase: alarm id = " + mId);

        /* load alarm details from database */
        if (mId != AlarmUtils.INVALID_ALARMID) {
            AlarmUtils.getAlarm(getContentResolver(), SetAlarm.this, mId);
        } else {
            ArrayList<AlarmItem> alarmList = (ArrayList<AlarmItem>) AlarmUtils.getAlarmListData(SetAlarm.this);
            if ((alarmList != null) && (alarmList.size() >= AlertUtils.MAX_ALARM_COUNT)) {
                isCTSLaunchAlarmAndMaxAlarm = true;
            }
            Calendar calendar = Calendar.getInstance();
            reportAlarm(AlarmUtils.INVALID_ALARMID, false, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0, new DaysOfWeek(0), true, "", "", false, false, RepeatTypeEnum.DONOTREPEAT.ordinal());
        }
    }

    private void getAlarmSoundByUriString() {
        Uri alarmSoundUri;
        boolean isDefaultUri = false;
        if (DEBUG_FLAG) Log.d(TAG, "getAlarmSoundByUriString: mAlarmSoundUriString = " + mAlarmSoundUriString);
        if (Settings.System.DEFAULT_ALARM_ALERT_URI.toString().equals(mAlarmSoundUriString)) {
            // default case, get audio alert from Setting
            alarmSoundUri = AlertUtils.getAlarmDefaultAlertUri(this);
            isDefaultUri = true;
        } else if (mAlarmSoundUriString == null) {
            // silent case
            alarmSoundUri = null;
        } else {
            // general media provider case
            alarmSoundUri = Uri.parse(mAlarmSoundUriString);
        }
        if (DEBUG_FLAG) Log.d(TAG, "getAlarmSoundByUriString: alarmSoundUri = " + alarmSoundUri);

        // get alarm sound title
        if (alarmSoundUri == null) {
            mAlarmSoundTitle = getString(R.string.st_silent);
        } else {
            if (!AlertUtils.isRingToneExist(this, alarmSoundUri)) {
                alarmSoundUri = AlertUtils.getAlarmDefaultAlertUri(this);
            }
            Ringtone ringtone = RingtoneManager.getRingtone(this, alarmSoundUri);
            if (ringtone != null) {
                ringtone.setStreamType(RingtoneManager.TYPE_ALARM);
                mAlarmSoundTitle = ringtone.getTitle(this);
                ringtone.stop(); // fix alarm sound can't change issue
            } else {
                Log.w(TAG, "getAlarmSoundByUriString: ringtone = null");
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "getAlarmSoundByUriString: mAlarmSoundTitle = " + mAlarmSoundTitle);
        if (isDefaultUri) {
            mAlarmSoundTitle = getString(R.string.st_default) + " (" + mAlarmSoundTitle + ")";
        }
        mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_ALARM_SOUND_NAME);
    }
    
    private void checkStoragePermission(boolean launchAlarmSound) {
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_M) {
            int storagePermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (DEBUG_FLAG) Log.d(TAG, "checkStoragePermission: storagePermission = " + storagePermission);
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                mShouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
                // show system permission dialog
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AlertUtils.REQUEST_PERMISSION_ACCESS);
                return;
            } else {
                if (launchAlarmSound) {
                    launchAlarmSoundURI();
                }
            }
        } else {
            if (launchAlarmSound) {
                launchAlarmSoundURI();
            }
        }
    }
    
    private void launchAlarmSoundURI() {
    	if (DEBUG_FLAG) Log.d(TAG, "launchAlarmSoundURI: mAlarmSoundUriString = " + mAlarmSoundUriString);
        try {
            if (AlertUtils.isHtcSoundPickerExist(SetAlarm.this)) {
                Bundle bundle = new Bundle();
                bundle.putInt(SDMConst.SDMPICKERTYPE, SDMConst.ID_ALARM);
                bundle.putString(SDMConst.SDMPICKERTITLE, getResources().getTextArray(R.array.sound_selection_setting_entries)[0].toString());
                bundle.putString(SDMConst.SDMDEFSTRURI, mAlarmSoundUriString);
                bundle.putBoolean(SDMSHOWDEFAULT, true);
                Intent intent = new Intent();
                intent.setAction(Global.HTC_SOUND_PICKER_ACTION_NAME);
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_ALARM_SOUND);
            } else {
                final Intent googleIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                googleIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(mAlarmSoundUriString));
                if (DEBUG_FLAG) Log.d(TAG, "launchAlarmSoundURI: googleIntent, mAlarmSoundUriString = " + mAlarmSoundUriString);
                googleIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                googleIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
                startActivityForResult(googleIntent, REQUEST_ALARM_SOUND);
            }
        } catch (Exception e) {
            Log.w(TAG, "launchAlarmSoundURI: Picker not found: e = " + e.toString());
        }
    }
    
    private View.OnClickListener mRingToneLayoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mRingtoneClicked = true;
            checkStoragePermission(true);
        }
    };

    private View.OnClickListener mRepeatLayoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showDialog(REQUEST_REPEAT);
        }
    };

    private View.OnClickListener mVibrateLayoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mVibrate = !mVibrate;
            updateVibrateCheckBox();
        }
    };

    private View.OnClickListener mOffAlarmLayoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mOffAlarm = !mOffAlarm;
            updateOffAlarmCheckBox();
        }
    };

    private View.OnClickListener mDoneBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isCTSLaunchAlarmAndMaxAlarm) {
                showDialog(REQUEST_ADD);
                return;
            }
            
            if ((mHtcTimePicker == null) || (mDescriptionText == null)) {
                return;
            }
            mHour = mHtcTimePicker.getCurrentHour();
            mMinutes = mHtcTimePicker.getCurrentMinute();
            mDescription = mDescriptionText.getText().toString();

            if (mId == AlarmUtils.INVALID_ALARMID) {
                Uri uri = AlarmUtils.addAlarm(SetAlarm.this, getContentResolver());
                if (uri != null) {
                    String segment = uri.getPathSegments().get(1);
                    mId = Integer.parseInt(segment);
                    if (mId != AlarmUtils.INVALID_ALARMID) { // add alarm data is ok
                        // nothing
                    } else { // no alarm id
                        SetAlarm.this.setResult(RESULT_CANCELED);
                        SetAlarm.this.finish();
                        return;
                    }
                } else { // no alarm data
                    SetAlarm.this.setResult(RESULT_CANCELED);
                    SetAlarm.this.finish();
                    return;
                }
            }

            saveAlarm(true);
            SetAlarm.this.setResult(RESULT_OK);
            SetAlarm.this.finish();
        }
    };

    private View.OnClickListener mCancelBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SetAlarm.this.setResult(RESULT_CANCELED);
            SetAlarm.this.finish();
        }
    };

    private View.OnClickListener mCheckBoxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mVibrate = ((CheckBox) v).isChecked();
        }
    };

    private View.OnClickListener mOffAlarmCheckBoxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mOffAlarm = ((CheckBox) v).isChecked();
        }
    };
    
    private CharSequence[] getNewWeekNameSequence() {
        CharSequence[] originalWeekArray = this.getResources().getTextArray(R.array.days_of_week);
        CharSequence[] newWeekArray = new CharSequence[7];
        for (int i = 0; i < 7; i++) {
            newWeekArray[(((1 - (mStartWeekDay - 1)) + 7) + i) % 7] = originalWeekArray[i];
        }
        return newWeekArray;
    }

    private boolean[] getNewWeekCheckSequence() {
        boolean[] originalCheck = mDaysOfWeek.getBooleanArray();
        boolean[] newCheck = new boolean[7];
        for (int i = 0; i < 7; i++) {
            newCheck[((1 - (mStartWeekDay - 1)) + i + 7) % 7] = originalCheck[i];
        }
        return newCheck;
    }

    private CharSequence[] getNewRepeatOptionSequence() {
        CharSequence[] originalRepeatOptionArray = this.getResources().getTextArray(R.array.repeat_option);
        CharSequence[] newRepeatOptionArray;
        if (Global.isSupportAccChinaSense()) {
            newRepeatOptionArray = new CharSequence[originalRepeatOptionArray.length];
            newRepeatOptionArray[0] = originalRepeatOptionArray[1];
            newRepeatOptionArray[1] = originalRepeatOptionArray[0];
            for (int i = 2; i < newRepeatOptionArray.length; i++) {
                newRepeatOptionArray[i] = originalRepeatOptionArray[i];
            }
        } else {
            newRepeatOptionArray = new CharSequence[4];
            newRepeatOptionArray[0] = originalRepeatOptionArray[1];
            newRepeatOptionArray[1] = originalRepeatOptionArray[0];
            for (int i = 2; i < newRepeatOptionArray.length; i++) {
                if (i < RepeatTypeEnum.SKIPHOLIDAY.ordinal()) {
                    newRepeatOptionArray[i] = originalRepeatOptionArray[i];
                } else {
                    newRepeatOptionArray[i] = originalRepeatOptionArray[i+1];
                }
            }
        }
        return newRepeatOptionArray;
    }

    private void setRepeatTypeToDaysOfWeek(int repeatType) {
        mRepeatType = repeatType;
        switch(findRepeatTypeEnumById(mRepeatType)) {
            case MON2FRI:
                for (int i = 0; i < 7; i++) {
                    if (i < 5) {
                        mTempDaysOfWeek.set(i, true);
                    } else {
                        mTempDaysOfWeek.set(i, false);
                    }
                }
                break;
            case DONOTREPEAT:
            case SKIPHOLIDAY:
                for (int i = 0; i < 7; i++) {
                    mTempDaysOfWeek.set(i, false);
                }
                break;
            case EVERYDAY:
                for (int i = 0; i < 7; i++) {
                    mTempDaysOfWeek.set(i, true);
                }
                break;
            case CUSTOM:
                break;
        }
        mDaysOfWeek.set(mTempDaysOfWeek);
        updateRepeat();
    }

    public RepeatTypeEnum findRepeatTypeEnumById(int id) {
        for (RepeatTypeEnum state : RepeatTypeEnum.values()) {
            if (id == state.ordinal()) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case REQUEST_PERMISSION_GRANTED:
                return new HtcAlertDialog.Builder(this)
                .setTitle(R.string.clock_permission_dialog_title)
                .setMessage(R.string.clock_permission_dialog_content)
                .setNeutralButton(R.string.clock_permission_dialog_btn_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .setPositiveButton(R.string.clock_permission_dialog_btn_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // go to setting page
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        }
                    })
                .create();
            case REQUEST_ADD:
                int titleResId = R.string.error;
                int messageResId = R.string.add_alarm_error;
                return new HtcAlertDialog.Builder(this)
                .setTitle(getString(titleResId))
                .setMessage(getString(messageResId))
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                }).create();
            case REQUEST_REPEAT:
                int repeatItem;
                int daysOfWeek = mDaysOfWeek.getCoded();
                boolean isSkipHolidayItem = (Global.isSupportAccChinaSense()) && (RepeatTypeEnum.SKIPHOLIDAY.ordinal() == mRepeatType);
                if (DEBUG_FLAG) Log.d(TAG, "REQUEST_REPEAT: daysOfWeek = " + daysOfWeek);
                if (DEBUG_FLAG) Log.d(TAG, "REQUEST_REPEAT: mRepeatType = " + mRepeatType);
                if (daysOfWeek == 31) {
                    // Mon - Fri case
                    repeatItem = 1;
                } else if ((daysOfWeek == 0) && (!isSkipHolidayItem)) {
                    // Do not repeat case
                    repeatItem = 0;
                } else if (daysOfWeek == 127) {
                    // Every day case
                    repeatItem = 2;
                } else if (isSkipHolidayItem) {
                    // Skip Holiday case
                    repeatItem = RepeatTypeEnum.SKIPHOLIDAY.ordinal();
                } else {
                    // Custom case
                    if ((Global.isSupportAccChinaSense())) {
                        repeatItem = RepeatTypeEnum.CUSTOM.ordinal();
                    } else {
                        repeatItem = RepeatTypeEnum.CUSTOM.ordinal() - 1;
                    }
                }
                if (DEBUG_FLAG) Log.d(TAG, "REQUEST_REPEAT: repeatItem = " + repeatItem);
                return new HtcAlertDialog.Builder(SetAlarm.this)
                    .setTitle(R.string.alarm_repeat)
                    .setSingleChoiceItems(getNewRepeatOptionSequence(), repeatItem,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (DEBUG_FLAG) Log.d(TAG, "whichButton = " + whichButton);
                                // check repeat type
                                if (RepeatTypeEnum.MON2FRI.ordinal() == whichButton) {
                                    whichButton = RepeatTypeEnum.DONOTREPEAT.ordinal();
                                } else if (RepeatTypeEnum.DONOTREPEAT.ordinal() == whichButton) {
                                    whichButton = RepeatTypeEnum.MON2FRI.ordinal();
                                }
                                int userChooseType = whichButton;
                                if (!Global.isSupportAccChinaSense()) {
                                    if (whichButton >= RepeatTypeEnum.SKIPHOLIDAY.ordinal()) {
                                        userChooseType = whichButton +1;
                                    }
                                }
                                if (RepeatTypeEnum.CUSTOM == findRepeatTypeEnumById(userChooseType)) {
                                    showDialog(REQUEST_CUSTOM);
                                } else {
                                	setRepeatTypeToDaysOfWeek(userChooseType);
                                }
                                removeDialog(REQUEST_REPEAT);
                            }
                        })
                    .setOnKeyListener(new OnKeyListener() {
                        @SuppressWarnings("deprecation")
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode,
                            KeyEvent event) {
                            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                                removeDialog(REQUEST_REPEAT);
                                return true;
                            } else {
                                if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    })
                    .setCancelable(false) // set touch outside of dialog to disable for ICS
                    .create();
            case REQUEST_CUSTOM:
                return new HtcAlertDialog.Builder(SetAlarm.this)
                    .setTitle(getResources().getTextArray(R.array.repeat_option)[RepeatTypeEnum.CUSTOM.ordinal()].toString())
                    .setMultiChoiceItems(getNewWeekNameSequence(), getNewWeekCheckSequence(),
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                                if (DEBUG_FLAG) Log.d(TAG, "whichButton = " + whichButton);
                                if (DEBUG_FLAG) Log.d(TAG, "isChecked = " + isChecked);
                                int dbDay = ((whichButton + (mStartWeekDay - 1)) + 6) % 7;
                                if (DEBUG_FLAG) Log.d(TAG, "dbDay = " + dbDay);
                                mTempDaysOfWeek.set(dbDay, isChecked);
                            }
                        })
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	setRepeatTypeToDaysOfWeek(RepeatTypeEnum.CUSTOM.ordinal());
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @SuppressWarnings("deprecation")
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mTempDaysOfWeek.set(mDaysOfWeek);
                            removeDialog(REQUEST_CUSTOM);
                        }
                    })
                    .setOnKeyListener(new OnKeyListener() {
                        @SuppressWarnings("deprecation")
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode,
                            KeyEvent event) {
                            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                                mTempDaysOfWeek.set(mDaysOfWeek);
                                removeDialog(REQUEST_CUSTOM);
                                return true;
                            } else {
                                if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    })
                    .setCancelable(false) // set touch outside of dialog to disable for ICS
                    .create();
        }

        return null;
    }

    /**
     * AlarmUtils.AlarmSettings implementation. Database feeds current
     * settings in through this call
     */
    // non-UI function
    @Override
    public void reportAlarm(
        int idx, boolean enabled, int hour, int minutes, long alarmtime,
        AlarmUtils.DaysOfWeek daysOfWeek, boolean vibrate, String message,
        String alert, boolean snoozed, boolean offalarm, int repeat_type) {

        mHour = hour;
        mMinutes = minutes;
        mDaysOfWeek.set(daysOfWeek);
        mTempDaysOfWeek.set(mDaysOfWeek);
        mDescription = message;
        mVibrate = vibrate;
        mAlarmSoundUriString = alert;
        mOffAlarm = offalarm;
        mRepeatType = repeat_type;
        if ("".equals(alert)) {
            mAlarmSoundUriString = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
        }
        if (DEBUG_FLAG) Log.d(TAG, "reportAlarm: mAlarmSoundUriString = " + mAlarmSoundUriString);
    }

    private void updateTime() {
        mHtcTimePicker.setCurrentHour(mHour);
        mHtcTimePicker.setCurrentMinute(mMinutes);
    }

    private void updateRepeat() {
        mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_ALARM_REPEAT);
    }

    private void updateVibrateCheckBox() {
        mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_ALARM_VIBRATE);
    }

    private void updateOffAlarmCheckBox() {
        mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_ALARM_OFFALARM);
    }
    
    private void saveAlarm(boolean popToast) {
        saveAlarm(this, mId, true, mHour, mMinutes,
            mDaysOfWeek, mVibrate, mAlarmSoundUriString,
            mDescription, false, mOffAlarm, mRepeatType, popToast);
    }

    /**
     * Write alarm out to persistent store and pops toast if alarm
     * enabled
     */
    public static void saveAlarm(
        final Context context, final int id, final boolean enabled, final int hour, final int minute,
        final AlarmUtils.DaysOfWeek daysOfWeek, final boolean vibrate, final String alert,
        final String message, final boolean snoozed, final boolean offalarm, final int repeat_type, boolean popToast) {
        // fix slow operation for strict mode enabled
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Fix alert string first
                AlarmUtils.setAlarm(context, id, enabled, hour, minute, daysOfWeek, vibrate,
                    message, alert, snoozed, offalarm, repeat_type);
            }
        }).start();

        if (enabled && popToast) {
            popAlarmSetToast(context, hour, minute, daysOfWeek, repeat_type);
        }
    }

    /**
     * Display a toast that tells the user how long until the alarm
     * goes off. This helps prevent "am/pm" mistakes.
     */
    static void popAlarmSetToast(Context context, int hour, int minute, AlarmUtils.DaysOfWeek daysOfWeek, int repeat_type) {
        String toastText = formatToast(context, hour, minute, daysOfWeek, repeat_type);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from
     * now"
     */
    static String formatToast(Context context, int hour, int minute, AlarmUtils.DaysOfWeek daysOfWeek, int repeat_type) {
        long alarm = AlarmUtils.calculateAlarm(hour, minute,
            daysOfWeek, repeat_type).getTimeInMillis();
        long delta = alarm - System.currentTimeMillis();
        ;
        long hours = delta / (1000 * 60 * 60);
        long minutes = (delta / (1000 * 60)) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
            (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
            (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
            (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        String ret;
        if (!(dispDays || dispHour || dispMinute)) {
            ret = context.getString(R.string.subminute);
        } else {
            String parts[] = new String[5];
            parts[0] = daySeq;
            parts[1] = !dispDays ? "" :
                dispHour && dispMinute ? context.getString(R.string.space) :
                    !dispHour && !dispMinute ? "" :
                        context.getString(R.string.and);
            parts[2] = dispHour ? hourSeq : "";
            parts[3] = dispHour && dispMinute ? context.getString(R.string.and) : "";
            parts[4] = dispMinute ? minSeq : "";
            ret = context.getString(R.string.combiner, (Object[]) parts);
        }

        ret = context.getString(R.string.alarm_set, ret);
        return ret;
    }

    private void initRegister() {
        /* Register for SD card changed */
        if (mMediaReceiver == null) {
            mMediaReceiver = new MediaReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            filter.addDataScheme(ContentResolver.SCHEME_FILE);
            registerReceiver(mMediaReceiver, filter, Global.PERMISSION_APP_DEFAULT, null);
        }
    }

    private void unInitRegister() {
        if (mMediaReceiver != null) {
            unregisterReceiver(mMediaReceiver);
            mMediaReceiver = null;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if ((mHtcTimePicker == null) || (mDescriptionText == null)) {
            return super.dispatchKeyEvent(event);
        }

        switch (event.getKeyCode()) {
            case (KeyEvent.KEYCODE_DPAD_DOWN):
                if (mHtcTimePicker.hasFocus()) {
                    mDescriptionText.requestFocus();
                    return true;
                }
            case (KeyEvent.KEYCODE_DPAD_UP):
                if (mHtcTimePicker.hasFocus()) {
                    return true;
                }
            case (KeyEvent.KEYCODE_DPAD_RIGHT):
                if (mHtcTimePicker.hasFocus()) {
                    return true;
                }
            case (KeyEvent.KEYCODE_DPAD_LEFT):
                if (mHtcTimePicker.hasFocus()) {
                    return true;
                }
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private class MediaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG_FLAG) Log.d(TAG, "MediaReceiver.onReceive: action = " + action);
            if ((mSetAlarmState.getState() == SetAlarmEnum.PAUSE) ||
                (mSetAlarmState.getState() == SetAlarmEnum.END)) {
                if (DEBUG_FLAG) Log.d(TAG, "MediaReceiver.onReceive: mMediaReceiver meet PAUSE or END state");
                return;
            }
            getAlarmSoundByUriString();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DEBUG_FLAG) Log.d(TAG, "onConfigrationChanged");
        HtcSkinUtils.initHtcFontScale(this);
        mSetAlarmResUtils.resetLayout();
        mSetAlarmResUtils.switchTheme(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ALARM_SOUND:
                if ((data != null) && (resultCode == RESULT_OK)) {
                    if (Global.isHEPDevice(this)) {
                        mAlarmSoundUriString = data.getDataString();
                    } else {
                        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (DEBUG_FLAG) Log.d(TAG, "onActivityResult: uri = " + uri);
                        if (uri != null) {
                            mAlarmSoundUriString = uri.toString();
                        }
                    }
                    if (DEBUG_FLAG) Log.d(TAG, "onActivityResult: mAlarmSoundUriString = " + mAlarmSoundUriString);
                    getAlarmSoundByUriString();
                }
                break;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AlertUtils.REQUEST_PERMISSION_ACCESS: {
                if (grantResults.length != 0) {
                    if (DEBUG_FLAG) Log.d(TAG, "onRequestPermissionsResult: grantResults[0] = " + grantResults[0]);
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (mRingtoneClicked) {
                            launchAlarmSoundURI();
                            mRingtoneClicked = false;
                        }
                    } else {
                        if ((!mShouldShowRequestPermissionRationale) && (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))) {
                            // show HTC permission dialog
                            showDialog(REQUEST_PERMISSION_GRANTED);
                        }
                    }
                }
                return;
            }
        }
    }
}
