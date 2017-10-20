package com.htc.android.worldclock.alarmclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager;
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
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.voiceutils.VoiceManager;
import com.htc.android.worldclock.aiservice.AiUtils;
import com.htc.android.worldclock.alarmclock.AlarmUtils.DaysOfWeek;
import com.htc.android.worldclock.alarmclock.SetAlarm.RepeatTypeEnum;
import com.htc.android.worldclock.stopwatch.Stopwatch;
import com.htc.android.worldclock.stopwatch.StopwatchUtils;
import com.htc.android.worldclock.timer.Timer;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcPhoneSensorFunctions;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.SettingsActivity;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.lockscreen.reminder.HtcReminderViewMode;

public class AlarmService extends Service {
    private static final String TAG = "WorldClock.AlarmService";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    public static final String ACTION_ALARM_TIME = "com.htc.intent.action.alarm_time";
    private static final String VIDEOPLAYER_CLASSNAME = "com.htc.video.VideoPlayerActivity";
    private static final String VIDEO_SKIP_AUDIOFOCUS = "htc.video.skip.audiofocus";

    private final static String OFFALARM_BOOT_REASON = "ro.boot.bootreason";
    private final static String OFFALARM_ID = "ro.boot.alarmid";
    private final static String OFFALARM_TIME = "ro.boot.alarmtime";
    private final static String RTC_ALARM = "rtc_alarm";
    
    private static final int UI_MSG_SHOW_SNOOZE_TOAST = 0x0001;
    private static final int UI_MSG_GET_CALL_STATE = 0x0002;
    private AlarmKlaxon mKlaxon = null;

    private int mId;
    private long mTime;
    private String mDescription;
    private boolean mLockScreen;
    private String mAlertSoundUriString;
    private int mAlarmType = AlertUtils.ALERT_DIALOG_NORMAL;
    private String mAction;
    private AlarmUtils.DaysOfWeek mDaysOfWeek = null;
    private TelephonyManager mTelephonyManager;

    private AudioManager mAudioManager;

    private IntentReceiver mIntentReceiver;
    private boolean bAlarmAlertRunning = false;
    private boolean registerPhoneListenerReady = false;
    private boolean registerSlot1PhoneListenerReady = false;
    private boolean registerSlot2PhoneListenerReady = false;
    boolean mOffAlarmTriggered = false;
    private boolean mOffAlarmRunning = false;
    private final int DELAY_TIME_MILLIS = 2000;
    private int mPrevCallState = TelephonyManager.CALL_STATE_IDLE;
    private int mRepeatType;

    private AlarmAlertReminderManager mReminderManager = null;

    private HtcPhoneSensorFunctions mSensorFunctions;

    private static class SnoozedInfo {
        int id;
        boolean snoozed;
        long snoozedTime;
    }
    private ArrayList<SnoozedInfo> mSnoozedInfoList;

    // Voice command
    private VoiceManager mVoiceManager;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        registerIntentReceiver();
        mAction = intent.getAction();
        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: mAction = " + mAction);

        // check args
        if ((intent == null) || TextUtils.isEmpty(mAction)) {
            stopService();
            return START_NOT_STICKY;
        }

        if (Intent.ACTION_TIME_CHANGED.equals(mAction) || Intent.ACTION_TIMEZONE_CHANGED.equals(mAction)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (Intent.ACTION_TIMEZONE_CHANGED.equals(mAction)) {
                        // get system timezone information
                        Calendar calNow = Calendar.getInstance();
                        long offset = calNow.getTimeZone().getOffset(calNow.getTimeInMillis());
                        if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: offset = " + offset);
                        if (offset != PreferencesUtil.getPrevSysTimezone(AlarmService.this)) {
                            if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: different timezone offset");
                            updateAlarms(false);
                        }
                        PreferencesUtil.setPrevSysTimezone(AlarmService.this, offset);
                    } else {
                        updateAlarms(false);
                    }
                    stopService();
                }
            }).start();
            return START_NOT_STICKY;
        } else if (AlarmUtils.ACTION_CANCEL_SNOOZE.equals(mAction)) {
            // This intent is sent from the notification when the user cancels the snooze alert.
            Log.i(TAG, "User cancels snooze alarm from status bar");
            mId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
            if (DEBUG_FLAG) Log.d(TAG, "AlarmUtils.CANCEL_SNOOZE: mId = " + mId);
            cancelSnooze();
            stopService();
            return START_NOT_STICKY;
        } else if (AlertUtils.RESHOW_ALERT_DIALOG.equals(mAction)) {
            // This intent is sent from the notification when the user taps alarm alert notification.
            Log.i(TAG, "User taps alarm alert from status bar");
            mId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
            mTime = intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME);
            mDescription = intent.getStringExtra(AlarmUtils.DESCRIPTION);
            mAlarmType = intent.getIntExtra(AlertUtils.EXTRA_ALERT_TYPE, AlertUtils.ALERT_DIALOG_NORMAL);
            boolean isShowSnooze = intent.getBooleanExtra(AlertUtils.EXTRA_SHOW_SNOOZE, true);
            if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: isShowSnooze = " + isShowSnooze);
            if (DEBUG_FLAG) Log.d(TAG, "onStartCommand: mAlarmType = " + mAlarmType);
            // Reshow alert dialog
            AlertUtils.alarmAlert(AlarmService.this, mId, mTime, mDescription, mAlarmType, AlertUtils.getLockScreenMode(AlarmService.this));
            // keep alarm ring and notify alarm to status bar
            //AlertUtils.alarmNotification(this, mId, mTime, mDescription, mAlarmType, isShowSnooze);
            return START_NOT_STICKY;
        } else if (AlertUtils.ALERT_SNOOZE.equals(mAction)) {
            // This intent is sent from the notification when the user presses the snooze action.
            Log.i(TAG, "User presses snooze alarm from status bar");
            mId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
            snooze(AlarmService.this, mId, mDescription);
            return START_NOT_STICKY;
        } else if (AlertUtils.ALERT_DISMISS.equals(mAction)) {
            // This intent is sent from the notification when the user presses the dismiss action.
            Log.i(TAG, "User presses dismiss alarm from status bar");
            mId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
            dismiss(AlarmService.this, mId);
            return START_NOT_STICKY;
        } else if (AlarmReceiver.ACTION_BOOT_COMPLETED.equals(mAction) || Global.getHtcQuickBootPowerOnActionString(this).equals(mAction)) {
            // check device encryption for off-alarm
            boolean isDeviceEncryptionEnabled = AlertUtils.reflectIsDeviceEncryptionEnabled();
            Log.i(TAG, "isDeviceEncryptionEnabled = " + isDeviceEncryptionEnabled);
            PreferencesUtil.setDeviceEncryption(this, isDeviceEncryptionEnabled);
            checkReShowSnoozedNotifications();
            final boolean isOffAlarm = isOffAlarm();
            /* allow next alarm to trigger while device is restart */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AiUtils aiUtils = AiUtils.getInstance(AlarmService.this);
                    ArrayList<AlarmItem> earlyEvent = aiUtils.getEarlyEventAlarms();
                    if (earlyEvent != null && earlyEvent.size() > 0) {
                        for (AlarmItem alarmItem : earlyEvent) {
                            //if AI alert time is smaller than current time , clear this AI alarm
                            if (alarmItem.aAlertTime < System.currentTimeMillis()) {
                                aiUtils.clearEarlyEventById(alarmItem.aId);
                            }
                        }
                    }
                    if (!isOffAlarm) {
                        updateAlarms(true);
                    }
                    resetStopwatchToDefault();
                    resetTimerToDefault(AlarmService.this);
                }
            }).start();
            
            // offalarm part
            AlarmUtils.saveSupportOffAlarmInPreference(this);
            if (isDeviceEncryptionEnabled) {
                AlarmUtils.writeOffAlarmData(this, AlarmUtils.INVALID_ALARMID, AlarmUtils.INVALID_ALARMTIME, isDeviceEncryptionEnabled);
            }
            if (isOffAlarm) {
                mOffAlarmTriggered = true;
                doAlarmService();
            } else {
                stopService();
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(mAction)) {
            // avoid service execute long time
            // when get ACTION_BOOT_COMPLETED broadcast after ACTION_LOCKED_BOOT_COMPLETED broadcast at Least N
            stopService();
        } else if (AlarmUtils.ACTION_ALARM_ALERT.equals(mAction)) {
            if (!isValidAlarm(intent)) {
                if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
                    Log.i(TAG, "onStartCommand: start foreground with a notification then stop it");
                    startAlarmForeground();
                    stopForeground(true);
                }
                stopService();
                return START_NOT_STICKY;
            }
            doAlarmService();
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
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
        AlarmAlertWakeLock wakeLock = AlarmAlertWakeLock.getInstance();
        if (wakeLock != null) {
            wakeLock.releaseFullScreenOn();
            wakeLock.releasePartial();
            wakeLock = null;
        }
        
        if (mReminderManager != null) {
            mReminderManager.cleanUp();
        }
        
        super.onDestroy();
    }

    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_MSG_SHOW_SNOOZE_TOAST:
                    makeSnoozeToast(AlarmService.this);
                    break;
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
        //fix not show snoozed toast issue
        //because of alarm service onDestroy before handle message
        //mMainHandler.removeMessages(UI_MSG_SHOW_SNOOZE_TOAST);
        mMainHandler.removeMessages(UI_MSG_GET_CALL_STATE);
    }
    
    private void resetStopwatchToDefault() {
        Log.i(TAG, "resetStopwatchToDefault");
        // reset preference settings
        PreferencesUtil.setStopwatchState(this, Stopwatch.StopwatchEnum.INIT.ordinal());
        PreferencesUtil.setStartTime(this, 0);
        PreferencesUtil.setPauseTime(this, 0);
        PreferencesUtil.setRunningTotalTime(this, 0);
        PreferencesUtil.setLastRunningTotalTime(this, 0);
        PreferencesUtil.setLapCount(this, 1);
        StopwatchUtils.DeleteStopwatchLapData(getBaseContext());
    }
    
    private void resetTimerToDefault(Context context) {
        Log.i(TAG, "resetTimerToDefault");
        // reset preference settings
        PreferencesUtil.setTimerState(context, Timer.TimerEnum.INIT.ordinal());
        PreferencesUtil.setTimerUserChoiceTime(context, Timer.DEFAULT_COUNTDOWN_VALUE);
    }
    
    private boolean isValidAlarm(Intent intent) {
        mId = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
        mTime = intent.getLongExtra(AlarmUtils.TIME, AlarmUtils.INVALID_ALARMTIME);
        mAlarmType = intent.getIntExtra(AlertUtils.EXTRA_ALERT_TYPE, AlertUtils.ALERT_DIALOG_NORMAL);
        mDescription = intent.getStringExtra(AlarmUtils.DESCRIPTION);
        mAlertSoundUriString = intent.getStringExtra(AlarmUtils.ALERT);
        if (DEBUG_FLAG) {
            Log.d(TAG, "onStartCommand: id = " + mId +
                    ", alarm time = " + mTime + "(" + new Date(mTime) + ")" +
                    ", mAlarmType = " + mAlarmType + ", mDescription = \"" + mDescription +
                    ", mAlertSoundUriString = \"" + mAlertSoundUriString + "\"");
        }

        // google's check
        long now = System.currentTimeMillis();
        if (now > (mTime + AlertUtils.STALE_WINDOW)) {
            if (DEBUG_FLAG) {
                Log.d(TAG, "onStartCommand: ignoring alarm intent id = " + mId +
                    ", alarm time = " + mTime + "(" + new Date(mTime) + ")" +
                    ", now = " + now + "(" + new Date(now) + ")");
            }
            calculateAndTriggerNextAlarm();
            return false;
        }

        return true;
    }

    private void stopService() {
        if (DEBUG_FLAG) Log.d(TAG, "stopService");
        if (DEBUG_FLAG) Log.d(TAG, "stopService: bAlarmAlertRunning = " + bAlarmAlertRunning);
        // don't stopSelf for register lock screen listener if check alarm is active
        if (!bAlarmAlertRunning) {
            if (DEBUG_FLAG) Log.d(TAG, "stopService: do stopSelf");
            stopSelf();
        }
    }

    private void doAlarmService() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
        // check phone state
        if (mTelephonyManager != null) {
            mPrevCallState = mTelephonyManager.getCallState();
        }
        if (DEBUG_FLAG) Log.d(TAG, "doAlarmService: mPrevCallState = " + mPrevCallState);
        
        if (TelephonyManager.CALL_STATE_RINGING == mPrevCallState) {
            Log.i(TAG, "Phone call is ringing, alarm is auto snoozed");
            // TelephonyManager.CALL_STATE_RINGING -> A new call arrived and is ringing or waiting
            //only start alarm foreground with show notification in sdk 26
            startAlarmForeground();
            AutoSnooze();
            stopService();
            return;
        } else {
            if ((TelephonyManager.CALL_STATE_OFFHOOK == mPrevCallState) && (Global.isSupportAutoSnoozeInCallState())) {
                Log.i(TAG, "In call state, CMCC sku doesn't pop UI and sound");
                // no any UI and alert when phone isn't idle for CMCC
                startAlarmForeground();
                AutoSnooze();
                stopService();
                return;
            }

            initAlarm();
            if (isNoNeedPopupAlarmAlertUI()) {
                // play Klaxon but don't show dialog
                startAlarmForeground();
                noUIAlarmAlertPlay();
            } else {
                Log.i(TAG, "Popup UI and play sound");
                mVoiceManager = VoiceManager.getInstance(this);
                mVoiceManager.setAlertUri(mAlertSoundUriString);
                mVoiceManager.startHfmService();
                mVoiceManager.setServiceCallBack(new VoiceManagerCallback());
                startAlarmProcess();
            }
        }
    }

    /**
     * android-O startForegroundService did call startForeground with notification
     */
    private void startAlarmForeground() {
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            Notification notification = AlertUtils.alarmNotification(this, mId, mTime, mDescription, mAlarmType, true, false);
            startForeground(AlertUtils.ALARMALERT_NOTIFICATION_ID, notification);
        }
    }

    private void initAlarm() {
        // set alarm alert running status
        bAlarmAlertRunning = true;
        boolean isDualSIM = AlertUtils.reflectIsMultiSimEnabled(this);
        if (DEBUG_FLAG) Log.d(TAG, "initAlarm: isDualSIM = " + isDualSIM);
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
        // Broadcast intent that alarm is alert
        informAlarmAlert(this, mId, mDescription, mTime);
        
        calculateAndTriggerNextAlarm();
    }
    
    private void checkReShowSnoozedNotifications() {
        mSnoozedInfoList = new ArrayList<SnoozedInfo>();
        if (mSnoozedInfoList != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //check if there is early event snoozed first
                    AiUtils.calculateEarlyEvent(AlarmService.this, mSettings);
                    AlarmUtils.getAlarms(AlarmService.this.getContentResolver(), mSettings);
                    for (int i = 0; i < mSnoozedInfoList.size(); i++) {
                        if (DEBUG_FLAG) Log.d(TAG, "checkReShowSnoozed: id = " + mSnoozedInfoList.get(i).id + ", snoozed = " + mSnoozedInfoList.get(i).snoozed + ", snoozedTime = " + mSnoozedInfoList.get(i).snoozedTime);
                        if (mSnoozedInfoList.get(i).snoozed) {
                            AlertUtils.alarmSnoozedNotification(AlarmService.this, mSnoozedInfoList.get(i).id, mSnoozedInfoList.get(i).snoozedTime);
                        }
                    }
                }
            }).start();
        }
    }
    
    private void startAlarmProcess() {
        PreferencesUtil.setCurrentFiringAlarm(AlarmService.this, mId);
        mLockScreen = AlertUtils.getLockScreenMode(this);
        if (DEBUG_FLAG) Log.d(TAG, "startAlarm: mLockScreen = " + mLockScreen);
        if (mLockScreen) {
            mReminderManager = AlarmAlertReminderManager.getInstance();
            mReminderManager.init(this);
            int currentViewMode = mReminderManager.getActiveViewMode();
            if (DEBUG_FLAG) Log.d(TAG, "startAlarmProcess: currentViewMode = " + currentViewMode);
            if (HtcReminderViewMode.ALARM_MODE == currentViewMode) {
                // second alarm case
                AlertUtils.alarmAlert(this, mId, mTime, mDescription, mAlarmType, mLockScreen);
            } else {
                // Control View State
                mReminderManager.initRegisterViewMode(this, HtcReminderViewMode.ALARM_MODE, mId, mTime, mDescription, AlertUtils.ALERT_DIALOG_NORMAL, mLockScreen);
            }
        } else {
            // Alarm alert dialog UI
            AlertUtils.alarmAlert(this, mId, mTime, mDescription, AlertUtils.ALERT_DIALOG_NORMAL, mLockScreen);
            
        }
        startAlarmSound();
    }
    
    private void startAlarmSound() {
        // acquire full screen on wakelock
        AlarmAlertWakeLock wakeLock = AlarmAlertWakeLock.getInstance();
        if (wakeLock != null) {
            wakeLock.acquireFullScreenOn(this);
        }
        
        // cancel alarm snoozed first for the same alarm id
        AlertUtils.cancelAlarmSnoozedNotification(AlarmService.this, mId);
        // keep alarm ring and notify alarm to status bar
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            startAlarmForeground();
        } else {
            //show notification
            AlertUtils.alarmNotification(this, mId, mTime, mDescription, mAlarmType, true, true);
        }
        // check DND setting value
        if (mOffAlarmRunning || !AlertUtils.isDoNotDisturbEnabled(this)) {
            playAlarm();
        }
        mOffAlarmRunning = false;
    }

    private void noUIAlarmAlertPlay() {
     // check DND setting value
        if (mOffAlarmRunning || !AlertUtils.isDoNotDisturbEnabled(this)) {
            playAlarm();
        }
    }

    private void cancelSnooze() {
        // Inform BT accessory that alarm is dismissed
        AlertUtils.sendAlarmSnoozeOrDismissIntent(this, AlertUtils.ALERT_DISMISS);
        AiUtils aiManager = AiUtils.getInstance(this);
        if (aiManager.checkIsEarlyAlarmId(mId)) {
            Log.d(AiUtils.AI_TAG, "cancel snooze clear early event alarm: " + mId);
            aiManager.clearEarlyEventById(mId);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                AlarmUtils.getAlarm(AlarmService.this.getContentResolver(), mSettings, mId);
                if ((RepeatTypeEnum.SKIPHOLIDAY.ordinal() != mRepeatType) && ((mDaysOfWeek == null) || (!mDaysOfWeek.isRepeatSet()))) {
                    // No repeat case:
                    AlarmUtils.enableAlarm(AlarmService.this, mId, false);
                } else {
                    // Repeat case:
                    AlertUtils.disableAlertAndSetNextAlert(AlarmService.this, mId);
                }
                stopService();
            }
        }).start();
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
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (DEBUG_FLAG) Log.d(TAG, "receive action = " + action);
            if (AlertUtils.ACTION_SNOOZE_DISMISS_RECEIVE.equals(action)) {
                final String recvAction = intent.getStringExtra(AlertUtils.ACTION_ALARM_ACTION);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (AlertUtils.ALERT_SNOOZE.equals(recvAction)) {
                            Log.i(TAG, "Alarm is snoozed from dialog, reminder or BT");
                            String description = intent.getStringExtra(AlarmUtils.DESCRIPTION);
                            int id = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
                            snooze(AlarmService.this, id, description);
                            stopService();
                        } else if (AlertUtils.ALERT_DISMISS.equals(recvAction)) {
                            Log.i(TAG, "Alarm is dismissed from dialog, reminder or BT");
                            int id = intent.getIntExtra(AlarmUtils.ID, AlarmUtils.INVALID_ALARMID);
                            dismiss(AlarmService.this, id);
                            stopService();
                        }
                    }
                }).start();
            }
        }
    }

    private void calculateAndTriggerNextAlarm() {
        /* allow next alarm to trigger while this activity is active */
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_FLAG) Log.d(TAG, "calculateAndTriggerNextAlarm: mId = " + mId);
                AlertUtils.disableAlertAndSetNextAlert(AlarmService.this, mId);
            }
        }).start();
    }
    
    private void informAlarmAlert(Context context, int id, String description, long time) {
        Intent btIntent = new Intent();
        btIntent.setAction(AlertUtils.ACTION_ALARMALERT_INFROM);
        btIntent.putExtra(AlarmUtils.ID, id);
        btIntent.putExtra(AlarmUtils.TIME, time);
        btIntent.putExtra(AlarmUtils.DESCRIPTION, description);
        context.sendBroadcast(btIntent, Global.PERMISSION_APP_DEFAULT);
        if (DEBUG_FLAG) Log.d(TAG, "informAlarmAlert: send action = " + AlertUtils.ACTION_ALARMALERT_INFROM);
    }

    private void playAlarm() {
        //register sensor to flip alarm
        //add support sensor feature judge and flip action is not do nothing
        boolean isDoNothingFlipAction = AlarmUtils.getAlarmFlipAction(this) == AlarmUtils.FlipAction.Action_None;
        boolean isSupportSensorFeature = HtcPhoneSensorFunctions.isSupportSensorFeature(this);
        if (isSupportSensorFeature && !isDoNothingFlipAction) {
            mSensorFunctions = HtcPhoneSensorFunctions.getInstances(this);
            mSensorFunctions.setCallBack(new SensorFunctionFlipAlarm());
        }
        // request audio focus
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }

        mKlaxon = AlarmKlaxon.getInstance(this);
        mKlaxon.setStopCallback(mStopCallback); // to stop callback
        mKlaxon.setKillerCallback(mKillerCalback); // need to set this callback due to no dialog case(background play)
        // put audio at the latest
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (DEBUG_FLAG) Log.d(TAG, "playAlarm: alarm_sound start");
                // ATS log
                if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][alarm_sound][start]");
                mKlaxon.play(AlarmService.this, mId, mTime, mDescription, mAlarmType);
            }
        }).start();
    }


    private class SensorFunctionFlipAlarm implements HtcPhoneSensorFunctions.FlipActionsCallBack {
        @Override
        public void flipOptions() {
            doActionFlipPhone();
        }
    }

    private void doActionFlipPhone() {
        AlarmUtils.FlipAction flipAction = AlarmUtils.getAlarmFlipAction(this);
        Log.i(TAG, "Flip phone behavior = " + flipAction);
        switch (flipAction) {
            case Action_None:
                break;
            case Action_Snooze:
                snooze(this, mId ,mDescription);
                break;
            case Action_Dismiss:
                dismiss(this,mId);
                break;
        }
    }

    // use Ood Tsen function
    private boolean isNoNeedPopupAlarmAlertUI() {
        if (Global.isSupportNoNeedPopupAlarmAlertUI()) {
            ActivityManager myActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            // 1 means top 1 activity
            List<ActivityManager.RunningTaskInfo> list = myActivityManager.getRunningTasks(1);
    
            if ((list != null) && !list.isEmpty()) {
                String topActivity = null;
                if (list.get(0).topActivity != null) {
                    topActivity = list.get(0).topActivity.getClassName();
                    if (DEBUG_FLAG) Log.d(TAG, "isNoNeedPopupAlarmAlertUI: topActivity =" + topActivity);
    
                    if (topActivity.compareTo(VIDEOPLAYER_CLASSNAME) == 0) {
                        // set value for streaming to skip audio focus
                        android.provider.Settings.System.putInt(getContentResolver(), VIDEO_SKIP_AUDIOFOCUS, 1);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private AlarmKlaxon.KillerCallback mKillerCalback = new AlarmKlaxon.KillerCallback() {
        @Override
        public void onKilled() {
            Log.i(TAG, "Alarm rings time out");
            //release flip alarm case phone sensor
            releaseHtcPhoneSensor();
            AlertUtils.cancelAlarmSnoozedNotification(AlarmService.this, mId);
            //clear early event alarm for time out
            AiUtils aiUtils = AiUtils.getInstance(AlarmService.this);
            if (aiUtils.checkIsEarlyAlarmId(mId)) {
                aiUtils.clearEarlyEventById(mId);
            }
            // Stop reminder UI and cover view but dialog
            AlertUtils.sendAlarmTimeoutIntent(AlarmService.this);
            AlertUtils.alarmAlert(AlarmService.this, mId, mTime, mDescription, AlertUtils.ALERT_DIALOG_TIMEOUT, false);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    // put audio to stop
                    if (mKlaxon != null) {
                        // set alarm alert running status
                        bAlarmAlertRunning = false;
                        mKlaxon.stop(AlarmService.this, mId, false);
                        mKlaxon = null;
                    }
                }
            }).start();
        }
    };

    private AlarmKlaxon.StopCallback mStopCallback = new AlarmKlaxon.StopCallback() {
        @Override
        public void onStopped() {
            Log.i(TAG, "Stop alert sound");
            // set alarm alert running status
            bAlarmAlertRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // check CTS Test
                    if ( (!TextUtils.isEmpty(mDescription)) && (mDescription.contains(HandleApiCalls.CTS_TEST_ALARM_STRING))) {
                        if (DEBUG_FLAG) Log.d(TAG, "This alarm is for CTS test");
                        AlarmUtils.deleteAlarm(AlarmService.this, mId);
                    }
                }
            }).start();

            stopService();
        }
    };

    public void snooze(Context context, int id, String description) {
        if (DEBUG_FLAG) Log.d(TAG, "snooze: id = " + id + ", description = " + description);
        if (mVoiceManager != null) {
            mVoiceManager.releaseHfmClient();
            mVoiceManager.releaseVoiceManager();
        }
        // stop alert sound
        AlarmKlaxon klaxon = AlarmKlaxon.getInstance(context);
        if (klaxon != null) {
            // ATS
            if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][alarm_sound][stop]");
            klaxon.stop(context, id, true);
            klaxon = null;
        }
        releaseHtcPhoneSensor();
        String snoozeMinutesString = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_ALARM_SNOOZE, AlertUtils.DEFAULT_SNOOZE);
        int snoozeMinutes = Integer.parseInt(snoozeMinutesString);
        if (DEBUG_FLAG) Log.d(TAG, "snoozeMinutes = " + snoozeMinutes);
        long snoozeTarget = System.currentTimeMillis() + (1000 * 60 * snoozeMinutes);
        AlarmUtils.saveSnoozeAlert(context, id, description, snoozeTarget);
        AlarmUtils.setNextAlert(context);
        AlertUtils.alarmSnoozedNotification(context, id, snoozeTarget);
        AlertUtils.cancelAlarmNotification(context, id);
        AlertUtils.sendCancelAlertIntent(context, id);
        AlertUtils.setAlarmSnoozeOrDismissByUser(true);
        // Inform BT accessory that alarm is snoozed
        AlertUtils.sendAlarmSnoozeOrDismissIntent(context, AlertUtils.ALERT_SNOOZE);
        PreferencesUtil.setCurrentFiringAlarm(AlarmService.this, AlarmUtils.INVALID_ALARMID);
        mMainHandler.sendEmptyMessage(UI_MSG_SHOW_SNOOZE_TOAST);
        // show off alarm dialog
        if (DEBUG_FLAG) Log.d(TAG, "snooze: mOffAlarmTriggered = " + mOffAlarmTriggered);
        if (mOffAlarmTriggered) {
            mOffAlarmTriggered = false;
            showOffAlarmDialog(this);
        }
    }

    public void dismiss(Context context, int id) {
        if (DEBUG_FLAG) Log.d(TAG, "dismiss: alarm_sound stop");
        AiUtils aiManager = AiUtils.getInstance(context);
        if (aiManager.checkIsEarlyAlarmId(id)) {
            aiManager.clearEarlyEventById(id);
            AlarmUtils.setNextAlert(context);
            Log.d(AiUtils.AI_TAG, "dismiss clearEarlyMeetingById: " + id);
        }
        if (mVoiceManager != null) {
            mVoiceManager.releaseHfmClient();
            mVoiceManager.releaseVoiceManager();
        }
        // stop alert sound
        AlarmKlaxon klaxon = AlarmKlaxon.getInstance(context);
        if (klaxon != null) {
            // ATS
            if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][alarm_sound][stop]");
            klaxon.stop(context, id, false);
            klaxon = null;
        }
        releaseHtcPhoneSensor();
        AlertUtils.cancelAlarmSnoozedNotification(context, id);
        AlertUtils.cancelAlarmNotification(context, id);
        AlertUtils.sendCancelAlertIntent(context, id);
        AlertUtils.setAlarmSnoozeOrDismissByUser(true);
        // Inform BT accessory that alarm is dismissed
        AlertUtils.sendAlarmSnoozeOrDismissIntent(context, AlertUtils.ALERT_DISMISS);
        PreferencesUtil.setCurrentFiringAlarm(AlarmService.this, AlarmUtils.INVALID_ALARMID);
        // show off alarm dialog
        if (DEBUG_FLAG) Log.d(TAG, "snooze: mOffAlarmTriggered = " + mOffAlarmTriggered);
        if (mOffAlarmTriggered) {
            mOffAlarmTriggered = false;
            showOffAlarmDialog(this);
        }
    }

    private void releaseHtcPhoneSensor() {
        Log.i(TAG, "releaseHtcPhoneSensor");
        if (mSensorFunctions != null) {
            mSensorFunctions.unregisterOrientationSensor();
            mSensorFunctions.setCallBack(null);
            HtcPhoneSensorFunctions.releaseInstances();
        }
    }

    private void doPhoneStateChangeAction(int phoneState) {
        if (DEBUG_FLAG) Log.d(TAG, "doPhoneStateChangeAction: phoneState = " + phoneState);
        if (Global.isSupportAutoSnoozeInCallState()) {
            if (phoneState != TelephonyManager.CALL_STATE_IDLE) {
                // no any UI and alert when phone isn't idle for CMCC
                AutoSnooze();
                stopService();
            } else {
                if (!isNoNeedPopupAlarmAlertUI()) {
                    startAlarmSound();
                }
            }
        } else {
            if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
                Log.i(TAG, "A new phone call arrived, alarm is auto snoozed");
                // TelephonyManager.CALL_STATE_RINGING -> A new call arrived and is ringing or waiting
                AutoSnooze();
                stopService();
            } else if (phoneState == TelephonyManager.CALL_STATE_IDLE) {
                Log.i(TAG, "Phone call from in-call to idle, alarm rings again");
                // Reshow alert dialog if Phone state change from in-call to non in-call;
                AlertUtils.alarmAlert(AlarmService.this, mId, mTime, mDescription, mAlarmType, AlertUtils.getLockScreenMode(AlarmService.this));
            }
        }
    }

    private void showOffAlarmDialog(Context context) {
        try {
            Intent offAlarmDialog = new Intent();
            offAlarmDialog.setClassName("com.android.settings", "com.android.settings.OffAlarmDialog");
            offAlarmDialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(offAlarmDialog);
        } catch (Exception e) {
            Log.w(TAG, "showOffAlarmDialog: can't find settings activity fail e = " + e.toString());
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
    
    private void makeSnoozeToast(Context context) {
        if (Global.isSupportAutoSnoozeInCallState() && !AlertUtils.isCallStateIdle(context)) {
            return;
        } else {
            String snoozeMinutesString = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_ALARM_SNOOZE, AlertUtils.DEFAULT_SNOOZE);
            int snoozeMinutes = Integer.parseInt(snoozeMinutesString);
            Toast.makeText(context, context.getString(R.string.alarm_alert_snooze_set, snoozeMinutes), Toast.LENGTH_LONG).show();
        }
    }

    private void AutoSnooze() {
        // set alarm alert running status
        bAlarmAlertRunning = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                snooze(AlarmService.this, mId, mDescription);
            }
        }).start();
    }

    private void updateAlarms(boolean bootCompleted) {
        if (bootCompleted) {
            try {
                AlarmUtils.disableExpiredAlarms(this);
            } catch (Exception e) {
                Log.w(TAG, "updateAlarms: AlarmUtils.disableExpiredAlarms fail e = " + e.toString());
            }
        }

        try {
            AlarmUtils.setNextAlert(this);
        } catch (Exception e) {
            Log.w(TAG, "updateAlarms: AlarmUtils.setNextAlert fail e = " + e.toString());
        }
    }

    private boolean isOffAlarm() {
        boolean retValue = false;
        String bootReason = AlertUtils.reflectSystemPropertyGet(OFFALARM_BOOT_REASON, "");
        if (DEBUG_FLAG) Log.d(TAG, "isOffAlarm: bootReason = " + bootReason);
        if (RTC_ALARM.equals(bootReason)) {
            mId = PreferencesUtil.getNextOffAlarmId(this);
            mTime = PreferencesUtil.getNextOffAlarmTime(this);
            mDescription = PreferencesUtil.getNextOffAlarmDescription(this);
            if (DEBUG_FLAG) Log.d(TAG, "isOffAlarm: mId = " + mId + ", mTime = " + mTime  + "(" + new Date(mTime) + ")" + ", mDescription = " + mDescription);
            retValue = true;
        }
        return retValue;
    }
    
    AlarmUtils.AlarmSettings mSettings = new AlarmUtils.AlarmSettings() {
        @Override
        public void reportAlarm(
            int idx, boolean enabled, int hour, int minutes, long alarmtime,
            DaysOfWeek daysOfWeek, boolean vibrate, String message,
            String alert, boolean snoozed, boolean offalarm, int repeat_type) {

            mDaysOfWeek = daysOfWeek;
            mRepeatType= repeat_type;
            if (snoozed) {
                SnoozedInfo snoozedInfo = new SnoozedInfo();
                snoozedInfo.id = idx;
                snoozedInfo.snoozed = snoozed;
                snoozedInfo.snoozedTime = alarmtime;
                if (mSnoozedInfoList != null) {
                    mSnoozedInfoList.add(snoozedInfo);
                }
            }
        };
    };

    /**
     * for voice manager callback for service update.
     */
    class VoiceManagerCallback implements VoiceManager.VoiceManagerServiceCallBack {
        @Override
        public void dismissAlarm() {
            dismiss(AlarmService.this, mId);
        }

        @Override
        public void snoozeAlarm() {
           snooze(AlarmService.this, mId, mDescription);
        }
    }

}
