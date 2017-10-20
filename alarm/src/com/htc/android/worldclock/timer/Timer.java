package com.htc.android.worldclock.timer;

import java.util.ArrayList;
import java.util.Date;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.AdapterView.OnItemClickListener;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.CarouselTab.MyTabAdapter;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.SDMConst;
import com.htc.android.worldclock.utils.ToastMaster;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcListItem1LineCenteredText;
import com.htc.lib1.cc.widget.HtcTimePicker;
import com.htc.lib1.cc.widget.ListPopupBubbleWindow;

public class Timer extends Fragment {
    private static final String TAG = "WorldClock.Timer";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private static final String SDMSHOWDEFAULT = "SDMShowDefault";
    private final int UI_MSG_UPDATE_SELF_MSG = 0x0001;
    private final int NONUI_MSG_LOAD_DATA = 0x0100;

    private final int DELAY_TIME_MILLIS = 1000;
    public final static String ACTION_TIMER_ALERT = "com.htc.worldclock.TIMER_ALERT";
    private static final int REQUEST_TIMER_SOUND = 0;
    public static final String SILENT_SOUND_STRING = "Silent";
    
    private static final int REQUEST_PERMISSION_GRANTED = 0;

    public static final long MAX_TIME = 13 * 60 * 60 * 1000; // 13 hours

    private TimerResUtils mTimerResUtils;

    private Looper mNonUILooper = null;
    private Handler mNonUIHandler = null;

    private Handler mMainHandler;

    private View.OnClickListener mResetBtnClickListener;
    private BroadcastReceiver mMediaReceiver;
    private View.OnClickListener mStartBtnClickListener;

    // default timer count down value
    public static final int DEFAULT_COUNTDOWN_VALUE = 1 * 60 * 1000; // 1 minute, unit is ms

    private HtcTimePicker mHtcTimePicker;
    private int mCurrentHour;
    private int mCurrentMinute;
    private int mCurrentSecond;

    private long mUserChoiceTime;
    private long mStartTime;
    private long mExpireTime;
    private long mPauseTime;

    private boolean mTabTimer = false;
    private int mStateOrdinal;

    private Activity mActivity;
    private View mMainView;
    private ListPopupBubbleWindow mFooterPopUpWindow;
    private ArrayList<String> mFooterList;
    private FooterAdapter mFooterAdapter;
    private HtcAlertDialog mHtcAlartDialog;
    private boolean mShouldShowRequestPermissionRationale = false;

    private HtcTimePicker.OnTimeSetListener mOnTimeSetListener;

    public static enum TimerEnum {
        INIT, NORMAL, PLAY, STOP, PAUSE, END, ALERT
    }

    // state control
    private TimerState mTimerState;
    private RelativeLayout mTimerView;

    private class TimerState {
        private TimerEnum mPrevState;
        private TimerEnum mRestoreState;

        TimerState(TimerEnum initState) {
            changeState(initState);
        }

        public TimerEnum getState() {
            return mPrevState;
        }

        public void restoreState() {
            if (mPrevState == TimerEnum.PAUSE) {
                mPrevState = mRestoreState;
            }
            if (DEBUG_FLAG) Log.d(TAG, "mTimerState.restoreState: " + mPrevState.toString());
        }

        public void changeState(TimerEnum currentState) {
            if (DEBUG_FLAG) Log.d(TAG, "TimerState.changeState: " + this.mPrevState + " -> " + currentState.toString());
            switch (currentState) {
                case INIT:
                    break;
                case NORMAL:
                    break;
                case PLAY:
                    break;
                case STOP:
                    break;
                case PAUSE:
                    if (mPrevState != currentState) {
                        mRestoreState = mPrevState;
                    }
                    break;
                case END:
                    break;
                case ALERT:
                    break;
                default:
                    Log.w(TAG, "TimerState.changeState: No support state = " + currentState.toString());
            }
            this.mPrevState = currentState;
        }

        public TimerEnum findEnumById(int id) {
            for (TimerEnum state : TimerEnum.values()) {
                if (id == state.ordinal()) {
                    return state;
                }
            }
            return null;
        }
    }

    @Override
    public void onCreate(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][Timer][START]");
        super.onCreate(sis);
        setHasOptionsMenu(true);
        initMember();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreateView");
        mMainView = inflater.inflate(R.layout.main_timer, container, false);
        mTimerView = (RelativeLayout) mMainView.findViewById(R.id.timer_view);
        return mMainView;
    }

    @Override
    public void onActivityCreated(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(sis);
        mActivity = getActivity();
        mTimerResUtils = new TimerResUtils(mActivity, mMainView);
        mTimerResUtils.initResources();
        initNonUIHandlerThread();
        initUIFuntion();
    }

    @Override
    public void onStart() {
        if (DEBUG_FLAG) Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG_FLAG) Log.d(TAG, "onResume");
        super.onResume();
        mTimerState.restoreState();
        updateLayout();
        setFooter();

        if(mTimerView.hasFocus()) {
        	mTimerView.setFocusable(false);
        	mTimerView.clearFocus();
        	mHtcTimePicker.requestFocus();
         }
       
    }

    @Override
    public void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        // get timer data from variables
        mStateOrdinal = mTimerState.getState().ordinal();
        mCurrentHour = mHtcTimePicker.getCurrentHour();
        mCurrentMinute = mHtcTimePicker.getCurrentMinute();
        mCurrentSecond = mHtcTimePicker.getCurrentSecond();
        if (mTimerState.getState() == TimerEnum.PLAY)
            mHtcTimePicker.setCountDownMode(false);
        saveDataToPreference();
        ToastMaster.cancelToast();

        mTimerState.changeState(TimerEnum.PAUSE);
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG_FLAG) Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy");
        dismissHtcAlartDialog();
        removeAllHandlerMessages();
        unInitRegister(); // to prevent in low memory,onStop never called.
        mTimerState.changeState(TimerEnum.END);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!Global.isSupportAccChinaSense()) {
            // create customize menu from inflater or runtime add from code
            inflater.inflate(R.menu.timer_menuitems, menu);
            if (DEBUG_FLAG) Log.d(TAG, "onCreatOptionMenu:inflate menu item complete");
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (!Global.isSupportAccChinaSense()) {
            if (menu == null) {
                Log.w(TAG, "onPrepareOptionsMenu: menu = null");
                return;
            }
    
            // If data is not ready, we don't want to show menu panel.
            if (mTimerState.getState() == TimerEnum.INIT) {
                return;
            }
    
            try {
                if (mTimerState.getState() == TimerEnum.PLAY) {
                    menu.findItem(R.id.alert_sound).setEnabled(false);
                } else {
                    menu.findItem(R.id.alert_sound).setEnabled(true);
                }
                if (!WorldClockTabControl.isShowMeInstall(mActivity)) {
                    menu.findItem(R.id.tips).setVisible(false);
                } else {
                    menu.findItem(R.id.tips).setVisible(true);
                }
            } catch (Exception e) {
                Log.w(TAG, "onPrepareOptionsMenu: menu find null view, Exception e = " + e.toString());
                mActivity.invalidateOptionsMenu();
                Handler handler = new Handler();
                handler.post(new Runnable() {
    
                    @Override
                    public void run() {
                        mActivity.openOptionsMenu();
                    }
                });
                return;
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!Global.isSupportAccChinaSense()) {
            switch (item.getItemId()) {
                case R.id.alert_sound:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: alert_sound");
                    checkStoragePermission();
                    break;
                case R.id.edit_tabs:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: edit_tabs");
                    ((CarouselTab)getParentFragment()).enterCarouselEditMode();
                    break;
                case R.id.tips:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: tips & help");
                    WorldClockTabControl.launchShowme(mActivity);
                    break;
                default:
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void dismissHtcAlartDialog() {
        if (mHtcAlartDialog != null) {
            mHtcAlartDialog.dismiss();
            mHtcAlartDialog = null;
        }
    }
    
    private void showHtcPermissionDialogView(int id) {
        HtcAlertDialog.Builder alertDialogView = new HtcAlertDialog.Builder(mActivity);
        alertDialogView.setTitle(getString(R.string.clock_permission_dialog_title));
//        alertDialogView.setOnKeyListener(mOnKeyListener);
        alertDialogView.setCancelable(false); // set touch outside of dialog to disable for ICS
        alertDialogView.setMessage(R.string.clock_permission_dialog_content);

        switch (id) {
            case REQUEST_PERMISSION_GRANTED:
                alertDialogView.setPositiveButton(R.string.clock_permission_dialog_btn_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // go to setting page
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + mActivity.getPackageName()));
                        startActivity(intent);
                        dismissHtcAlartDialog();
                    }
                });
                alertDialogView.setNegativeButton(R.string.clock_permission_dialog_btn_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dismissHtcAlartDialog();
                    }
                });
                break;
        }

        dismissHtcAlartDialog();
        mHtcAlartDialog = alertDialogView.create();
        mHtcAlartDialog.show();
    }
    
    private void checkStoragePermission() {
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_M) {
            int storagePermission = mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (DEBUG_FLAG) Log.d(TAG, "checkStoragePermission: storagePermission = " + storagePermission);
            if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                mShouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
                // show system permission dialog
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AlertUtils.REQUEST_PERMISSION_ACCESS);
                return;
            } else {
                optionAlertSound();
            }
        } else {
            optionAlertSound();
        }
    }
    
    private void optionAlertSound() {
        String timerSoundUriString = PreferencesUtil.getTimerSoundUri(mActivity);
        if (SILENT_SOUND_STRING.equals(timerSoundUriString)) {
            timerSoundUriString = null;
        }
        if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: alert_sound, timerSoundUriString = " + timerSoundUriString);
        if (Settings.System.DEFAULT_ALARM_ALERT_URI.toString().equals(timerSoundUriString)) {
            // default case, get audio alert from Setting
            timerSoundUriString = AlertUtils.getTimerDefaultAlertUri(mActivity).toString();
            if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: get timer default alert_sound uri, timerSoundUriString = " + timerSoundUriString);
        }
        
        try {
            if (AlertUtils.isHtcSoundPickerExist(mActivity)) {
                Bundle bundle = new Bundle();
                bundle.putInt(SDMConst.SDMPICKERTYPE, SDMConst.ID_ALARM);
                bundle.putString(SDMConst.SDMPICKERTITLE, getResources().getTextArray(R.array.sound_selection_setting_entries)[0].toString());
                bundle.putString(SDMConst.SDMDEFSTRURI, timerSoundUriString);
                bundle.putBoolean(SDMSHOWDEFAULT, false);
                Intent intent = new Intent();
                intent.setAction(Global.HTC_SOUND_PICKER_ACTION_NAME);
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_TIMER_SOUND);
            } else {
                final Intent googleIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                googleIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(timerSoundUriString));
                if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: googleIntent, timerSoundUriString = " + timerSoundUriString);
                googleIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                googleIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
                startActivityForResult(googleIntent, REQUEST_TIMER_SOUND);
            }
        } catch (Exception e) {
            Log.w(TAG, "onOptionsItemSelected:Picker not found: e = " + e.toString());
        }
        mTimerState.changeState(TimerEnum.ALERT);
    }
    
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
                        break;
                }
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mTimerResUtils != null) {
            mTimerResUtils.resetLayout();
        }
        if (Global.isSupportAccChinaSense() && mFooterPopUpWindow != null) {
            if (mFooterPopUpWindow.isShowing()) {
                mFooterPopUpWindow.dismissWithoutAnimation();
            }
            if (mTimerResUtils != null) {
                mTimerResUtils.setPopUpWindowExpand(newConfig, mFooterPopUpWindow);
            }
        }
  }

    private void initMember() {
        mTimerState = new TimerState(TimerEnum.INIT);

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UI_MSG_UPDATE_SELF_MSG:
                        if (mTimerState.getState() == TimerEnum.PLAY) {
                            updateSlideTime();
                            long now = SystemClock.elapsedRealtime();
                            long timePass = ((now - mStartTime) % MAX_TIME);
                            long nextDelayTime = DELAY_TIME_MILLIS - (timePass % DELAY_TIME_MILLIS);
                            mMainHandler.removeMessages(UI_MSG_UPDATE_SELF_MSG);
                            mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(UI_MSG_UPDATE_SELF_MSG), nextDelayTime);
                        }
                        break;
                }
            }
        };

        mResetBtnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimerState.changeState(TimerEnum.NORMAL);
                enableAlarmInternal(mActivity, false, 0, 0, 0);
                int second = (int) (mUserChoiceTime / 1000);
                transferTimeToHourMinuteSecond(second);
                setCurrentTime();
                setInitStateStateView();
                mTimerResUtils.setResetButtonEnabled(false);
                mHtcTimePicker.setCountDownMode(false);
            }
        };

        mOnTimeSetListener = new HtcTimePicker.OnTimeSetListener() {
            @Override
            public void onTimeSet(HtcTimePicker arg0, int arg1, int arg2, int arg3) {
                MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
                if (adapter != null && adapter.getCurrentTabTag().equals(CarouselTab.TAB_TIMER)) {
                    mTimerResUtils.setResetButtonEnabled(true);
                }
            }
        };

        mMediaReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DEBUG_FLAG) Log.d(TAG, "mMediaReceiver.onReceive: action = " + action);
                if ((mTimerState.getState() == TimerEnum.PAUSE) ||
                    (mTimerState.getState() == TimerEnum.END)) {
                    if (DEBUG_FLAG) Log.d(TAG, "mMediaReceiver.onReceive: mMediaReceiver meet PAUSE or END state");
                    return;
                }
            }
        };

        mStartBtnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG_FLAG) Log.d(TAG, "mStartBtnClickListener.onClick");
                // check Timer is launched by other AP
                if (mTabTimer) {
                    // stop Timer page and return to other AP
                    mActivity.finish();
                }

                switch (mTimerState.getState()) {
                    case NORMAL:
                    case STOP:
                        mTimerState.changeState(TimerEnum.PLAY);
                        mStartTime = SystemClock.elapsedRealtime();
                        mCurrentHour = mHtcTimePicker.getCurrentHour();
                        mCurrentMinute = mHtcTimePicker.getCurrentMinute();
                        mCurrentSecond = mHtcTimePicker.getCurrentSecond();
                        Log.i(TAG, "Start timer time, " + String.format("%02d:%02d:%02d", mCurrentHour, mCurrentMinute, mCurrentSecond));
                        enableAlarmInternal(mActivity, true, mCurrentHour, mCurrentMinute, mCurrentSecond);
                        mMainHandler.removeMessages(UI_MSG_UPDATE_SELF_MSG);
                        mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(UI_MSG_UPDATE_SELF_MSG), DELAY_TIME_MILLIS);

                        mExpireTime = calculateExpire(mCurrentHour, mCurrentMinute, mCurrentSecond);
                        mUserChoiceTime = (mCurrentHour * 60 * 60 * 1000) + (mCurrentMinute * 60 * 1000) + (mCurrentSecond * 1000);
                        setPlayStateStateView();
                        mTimerResUtils.setResetButtonEnabled(true);
                        mHtcTimePicker.setCountDownMode(true);
                        stopSlideHtcTimePicker();
                        break;
                    case PLAY:
                        mTimerState.changeState(TimerEnum.STOP);
                        enableAlarmInternal(mActivity, false, 0, 0, 0);
                        mPauseTime = SystemClock.elapsedRealtime();
                        setInitStateStateView();
                        mTimerResUtils.setResetButtonEnabled(true);
                        mHtcTimePicker.setCountDownMode(false);
                        break;
                }
            }
        };
    }

    private void initUIFuntion() {
        initHtcTimePicker();

        // check Timer is launched by other AP
        mTabTimer = mActivity.getIntent().getBooleanExtra(CarouselTab.TAB_TIMER, false);

        initRegister();
        initKeyListener();
        if (Global.isSupportAccChinaSense()) {
            initFooterMoreList();
        }

        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][Timer][DATA_READY]");
        mTimerState.changeState(TimerEnum.NORMAL);
    }

    private void initFooterMoreList() {
        mFooterList = new ArrayList<String>();
        mFooterList.add(getString(R.string.alert_sound));
        mFooterList.add(getString(R.string.edit_tabs_menu_item));
        if (WorldClockTabControl.isShowMeInstall(mActivity)) {
            mFooterList.add(getString(R.string.tips));
        }
        mFooterAdapter = new FooterAdapter(mFooterList);
        mFooterPopUpWindow = new ListPopupBubbleWindow(mActivity);
        if (mFooterPopUpWindow != null) {
            mFooterPopUpWindow.setAdapter(mFooterAdapter);
            HtcFooterButton moreButton = (HtcFooterButton) mActivity.findViewById(R.id.footer_btn1);
            mFooterPopUpWindow.setAnchorView(moreButton);
            mFooterPopUpWindow.setFocusable(true);
            mFooterPopUpWindow.setOutsideTouchable(true);
            Configuration configuration = mActivity.getResources().getConfiguration();
            mTimerResUtils.setPopUpWindowExpand(configuration, mFooterPopUpWindow);
        }
    }
    
    public void setFooter() {
        if (mActivity == null)
            return;

        MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
        if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_TIMER)) {
            return;
        }

        mTimerResUtils.setHtcFooterButtonResource();
        // set button click listener
        HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
        if (footer != null) {
            footer.findViewById(R.id.footer_btn2).setOnClickListener(mStartBtnClickListener);
            footer.findViewById(R.id.footer_btn3).setOnClickListener(mResetBtnClickListener);
            footer.findViewById(R.id.footer_btn4).setVisibility(View.GONE);
            if (!Global.isSupportAccChinaSense()) {
                footer.findViewById(R.id.footer_btn1).setVisibility(View.GONE);
            }
        }
        if (Global.isSupportAccChinaSense()) {
            updateFooterMoreList();
        }
        updateHtcTimePicker();
        mTimerResUtils.setStartButtonImage();
    }

    private void updateFooterMoreList() {
        if (mFooterPopUpWindow != null) {
            HtcFooterButton moreButton = (HtcFooterButton) mActivity.findViewById(R.id.footer_btn1);
            moreButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFooterPopUpWindow.show();
                }
            });
            mFooterPopUpWindow.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mFooterPopUpWindow.dismiss();
                    switch (position) {
                        case 0:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: alert_sound");
                            checkStoragePermission();
                            break;
                        case 1:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: edit_tabs");
                            ((CarouselTab)getParentFragment()).enterCarouselEditMode();
                            break;
                        case 2:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: tips & help");
                            WorldClockTabControl.launchShowme(mActivity);
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }
    
    private void initHtcTimePicker() {
        // initial HtcTimePicker
        mHtcTimePicker = (HtcTimePicker) mMainView.findViewById(R.id.timerPicker);
        mHtcTimePicker.setSecondPickerEnable(true);
        mHtcTimePicker.init(0, 0, 0, mOnTimeSetListener);
        mHtcTimePicker.setRepeatEnable(true);
        mHtcTimePicker.setEnabled(true);
        mHtcTimePicker.setHourRange(0, 12);
        mHtcTimePicker.setMinuteRange(0, 59, true);
        mHtcTimePicker.setSecondRange(0, 59, true);
        mHtcTimePicker.dispatchOnScrollIdleStateListener(mHtcTimePicker);
        mHtcTimePicker.setFocusable(true);
    }

    private void updateLayout() {
        if (mTimerState.getState() != TimerEnum.ALERT) {
            loadDataFromPreference();
            reLoadData();
            mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);
            if (mTimerState.getState() == TimerEnum.PLAY) {
                mHtcTimePicker.setCountDownMode(true);
            }
        } else {
            mTimerState.changeState(TimerEnum.NORMAL);
        }
    }

    private void updateHtcTimePicker() {
        // for HtcTimePicker
        if (mTimerState.getState() == TimerEnum.PLAY) {
            mHtcTimePicker.setEnabled(false);
        }
        setCurrentTime();

        // for start button and alert layout
        if (mTimerState.getState() == TimerEnum.PLAY) {
            mMainHandler.removeMessages(UI_MSG_UPDATE_SELF_MSG);
            mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(UI_MSG_UPDATE_SELF_MSG), DELAY_TIME_MILLIS);
            setPlayStateStateView();
            mTimerResUtils.setResetButtonEnabled(true);
        } else if (mTimerState.getState() == TimerEnum.STOP) {
            setInitStateStateView();
            mTimerResUtils.setResetButtonEnabled(true);
        } else {
            setInitStateStateView();
            mTimerResUtils.setResetButtonEnabled(false);
        }
    }

    private void removeAllHandlerMessages() {
        if (mNonUILooper != null) {
            mNonUILooper.quit();
        }

        mMainHandler.removeMessages(UI_MSG_UPDATE_SELF_MSG);
        mNonUIHandler.removeMessages(NONUI_MSG_LOAD_DATA);
    }

    private void loadDataFromPreference() {
        // reset device state will be change state to INIT state
        TimerEnum prefState = mTimerState.findEnumById(PreferencesUtil.getTimerState(mActivity));
        if ((prefState != null) && (prefState != TimerEnum.INIT)) {
            mTimerState.changeState(prefState);
        }
        if (DEBUG_FLAG) Log.d(TAG, "loadDataFromPreference: mTimerState.getState() = " + mTimerState.getState());

        mUserChoiceTime = PreferencesUtil.getTimerUserChoiceTime(mActivity);
        mStartTime = PreferencesUtil.getTimerStartTime(mActivity);
        mExpireTime = PreferencesUtil.getTimerExpireTime(mActivity);
        mPauseTime = PreferencesUtil.getTimerPauseTime(mActivity);
    }

    private void saveDataToPreference() {
        PreferencesUtil.setTimerState(mActivity, mStateOrdinal);
        PreferencesUtil.setTimerUserChoiceTime(mActivity, mUserChoiceTime);
        PreferencesUtil.setTimerStartTime(mActivity, mStartTime);
        PreferencesUtil.setTimerExpireTime(mActivity, mExpireTime);
        PreferencesUtil.setTimerPauseTime(mActivity, mPauseTime);
    }

    private void reLoadData() {
        if (mTimerState.getState() == TimerEnum.NORMAL) {
            int second = (int) (mUserChoiceTime / 1000);
            if (DEBUG_FLAG) Log.d(TAG, "reLoadData: NORMAL state, mUserChoiceTime = " + mUserChoiceTime + ", second = " + second);
            transferTimeToHourMinuteSecond(second);
        } else if (mTimerState.getState() == TimerEnum.PLAY) {
            long timeLeft = (mExpireTime - SystemClock.elapsedRealtime()) % MAX_TIME;
            // no ceil due to can't reach zero when alert ring.
            int second = (int) (timeLeft / 1000);
            if (DEBUG_FLAG) Log.d(TAG, "reLoadData: PLAY state, time left = " + timeLeft + ", second = " + second);
            if (second < 0) {
                second = (int) (mUserChoiceTime / 1000);
                mTimerState.changeState(TimerEnum.NORMAL);
                PreferencesUtil.setTimerState(mActivity, Timer.TimerEnum.NORMAL.ordinal());
            }
            transferTimeToHourMinuteSecond(second);
        } else {
            long timeLeft = (mExpireTime - mPauseTime) % MAX_TIME;
            int second = (int) Math.ceil((float) timeLeft / 1000);
            if (DEBUG_FLAG) Log.d(TAG, "reLoadData: Else case, timeLeft = " + timeLeft + ", second = " + second);
            transferTimeToHourMinuteSecond(second);
        }

        if (DEBUG_FLAG) {
            Log.d(TAG, "reLoadData: mCurrentHour = " + mCurrentHour + ", mCurrentMinute = " + mCurrentMinute + ", mCurrentSecond = " + mCurrentSecond);
        }
    }

    private void initRegister() {
        /* Register for SD card changed */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme(ContentResolver.SCHEME_FILE);
        mActivity.registerReceiver(mMediaReceiver, filter, Global.PERMISSION_APP_DEFAULT, null);
    }

    private void unInitRegister() {
        try {
            mActivity.unregisterReceiver(mMediaReceiver);
            mMediaReceiver = null;
        } catch (Exception e) {
            Log.w(TAG, "unInitRegister: mMediaReceiver never registered e = " + e.toString());
        }
    }

    private void setInitStateStateView() {
        mHtcTimePicker.setEnabled(true);
        mTimerResUtils.setStartButtonView(true);
    }

    private void setPlayStateStateView() {
        mHtcTimePicker.setEnabled(false);
        mTimerResUtils.setStartButtonView(false);
    }

    private void setCurrentTime() {
        if (mHtcTimePicker != null) {
            mHtcTimePicker.setCurrentHour(mCurrentHour);
            mHtcTimePicker.setCurrentMinute(mCurrentMinute);
            mHtcTimePicker.setCurrentSecond(mCurrentSecond);
        }
    }

    private void updateSlideTime() {
        if (!((mCurrentSecond == 0) && (mCurrentMinute == 0) && (mCurrentHour == 0))) {
            slideOneSecond();
        } else {
            mTimerState.changeState(TimerEnum.NORMAL);
            PreferencesUtil.setTimerState(mActivity, Timer.TimerEnum.NORMAL.ordinal());
        }
    }

    private void slideOneSecond() {
        boolean isHourChanged = false;
        boolean isMinuteChanged = false;
        boolean isSecondChanged = true;
        mCurrentSecond -= 1;
        if (mCurrentSecond == -1) {
            mCurrentSecond = 59;
            mCurrentMinute -= 1;
            isMinuteChanged = true;
            if (mCurrentMinute == -1) {
                mCurrentMinute = 59;
                mCurrentHour -= 1;
                isHourChanged = true;
                if (mCurrentHour == -1) {
                    mCurrentSecond = 0;
                    mCurrentMinute = 0;
                    mCurrentHour = 0;
                }
            }
        }

        // error handling
        if (mHtcTimePicker == null) {
            Log.w(TAG, "slideOneSecond: mHtcTimePicker == null");
            return;
        }

        int slideOffset = mHtcTimePicker.getTableViewSlideOffset();
        if (isHourChanged) {
            mHtcTimePicker.slideHourWithOffset(slideOffset);
        }
        if (isMinuteChanged) {
            if (mCurrentMinute == 59) {
                mHtcTimePicker.setCurrentMinute(mCurrentMinute);
            } else {
                mHtcTimePicker.slideMinuteWithOffset(slideOffset);
            }
        }
        if (isSecondChanged) {
            if (mCurrentSecond == 59) {
                mHtcTimePicker.setCurrentSecond(mCurrentSecond);
            } else {
                mHtcTimePicker.slideSecondWithOffset(slideOffset);
            }
        }
    }

    private void stopSlideHtcTimePicker() {
        if (mHtcTimePicker != null) {
            mHtcTimePicker.slideHourWithOffset(0);
            mHtcTimePicker.slideMinuteWithOffset(0);
            mHtcTimePicker.slideSecondWithOffset(0);
        }
    }

    public synchronized static void enableAlarmInternal(
        final Context context, boolean enabled,
        int mCurrentHour, int mCurrentMinute, int mCurrentSecond) {

        if (enabled) {
            long time = calculateAlarm(mCurrentHour, mCurrentMinute, mCurrentSecond);
            enableAlert(context, time);
        } else {
            disableAlert(context);
        }
    }

    private static long calculateAlarm(int mCurrentHour, int mCurrentMinute, int mCurrentSecond) {
        return System.currentTimeMillis() +
            (mCurrentHour * 60 * 60 * 1000) + (mCurrentMinute * 60 * 1000)
            + (mCurrentSecond * 1000);
    }

    private long calculateExpire(int mCurrentHour, int mCurrentMinute, int mCurrentSecond) {
        return mStartTime +
            (mCurrentHour * 60 * 60 * 1000) + (mCurrentMinute * 60 * 1000)
            + (mCurrentSecond * 1000);
    }

    public static void enableAlert(Context context, long atTimeInMillis) {
        Intent intent = new Intent(ACTION_TIMER_ALERT);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setClass(context, TimerReceiver.class);
        intent.putExtra(AlarmUtils.TIME, atTimeInMillis);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        }
        Log.i(TAG, "Set timer to alarm manager: time = " + atTimeInMillis + "(" + new Date(atTimeInMillis) + ")");
    }

    public static void disableAlert(Context context) {
        Intent intent = new Intent(ACTION_TIMER_ALERT);
        intent.setClass(context, TimerReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
        Log.i(TAG, "Cancel any timer from alarm manager");
    }

    private void transferTimeToHourMinuteSecond(int value) {
        if (DEBUG_FLAG) Log.d(TAG, "transferTimeToHourMinuteSecond: value = " + value);
        mCurrentHour = value / 60 / 60;
        mCurrentMinute = (value / 60) % 60;
        mCurrentSecond = value % 60;
    }

    private void initKeyListener() {
        mTimerView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        getView().setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if ((keyCode == KeyEvent.KEYCODE_MENU) && event.isLongPress()) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TIMER_SOUND:
                if ((data != null) && (resultCode == Activity.RESULT_OK)) {
                    String timerSoundUriString = "";
                    if (Global.isHEPDevice(mActivity)) {
                        timerSoundUriString = data.getDataString();
                    } else {
                        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (DEBUG_FLAG) Log.d(TAG, "onActivityResult: uri = " + uri);
                        if (uri != null) {
                            timerSoundUriString = uri.toString();
                        }
                    }
                    if (DEBUG_FLAG) Log.d(TAG, "onActivityResult: timerSoundUriString = " + timerSoundUriString);
                    // need save it here immediately for timer sound ring
                    // SharedPreferences set value "null" will remove string with this key
                    if (null == timerSoundUriString) {
                        PreferencesUtil.setTimerSoundUri(mActivity, SILENT_SOUND_STRING);
                    } else {
                        PreferencesUtil.setTimerSoundUri(mActivity, timerSoundUriString);
                    }
                }
                break;
        }
    }
    
    private class FooterAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        protected ArrayList<String> mItems = null;
        protected View mLayout;

        public FooterAdapter(ArrayList<String> list) {
            if (list != null) {
                mItems = new ArrayList<String>(list);
            }
            mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if (mItems == null) {
                return 0;
            }
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                mLayout = convertView;
            } else {
                mLayout = mInflater.inflate(R.layout.common_footer_list_item, null);
            }
            HtcListItem1LineCenteredText cityNameView;
            cityNameView = (HtcListItem1LineCenteredText)mLayout.findViewById(R.id.footer_list_name);
            mLayout.setBackground(null);
            cityNameView.setText(mItems.get(position));
            return mLayout;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AlertUtils.REQUEST_PERMISSION_ACCESS: {
                if (grantResults.length != 0) {
                    if (DEBUG_FLAG) Log.d(TAG, "onRequestPermissionsResult: grantResults[0] = " + grantResults[0]);
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        optionAlertSound();
                    } else {
                        if ((!mShouldShowRequestPermissionRationale) && (!mActivity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))) {
                            // show HTC permission dialog
                            showHtcPermissionDialogView(REQUEST_PERMISSION_GRANTED);
                        }
                    }
                }
                return;
            }
        }
    }
}
