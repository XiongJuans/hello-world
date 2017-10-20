package com.htc.android.worldclock.voiceutils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.hfm.Speech;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.hfmclient.HfmClient;

import java.util.Locale;
import java.util.Map;

/**
 * Manager voice state of current affairs.
 * add for AMT_MASD_LI_YU 2017.02.20.
 */
public class VoiceManager {

    protected static final String TAG = "VoiceManager";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final int TIME_OUT = 0;
    private static final int DISMISS_COMMAND_FLAG = 1;
    private static final int SNOOZED_COMMAND_FLAG = 2;
    private static final int SERVICE_NOT_START = -100;
    private Context mContext;
    private static VoiceManager sVoiceManager;
    private BluetoothReceiver mBluetoothReceiver;
    private boolean mIsSupportForVoiceCommands;
    private VoiceManagerUICallBack mVoiceManagerDialogCallBack;
    private VoiceManagerUICallBack mVoiceManagerLockScreenCallBack;
    private VoiceManagerServiceCallBack mVoiceManagerServiceCallBack;
    private HfmClient mHFMClient;
    private boolean mHFMServiceReady = false;
    private String mAlertSoundUriString;
    private ContentResolver mContentResolver;
    private ContentObserver mContentObserver;
    private TelephonyManager mTelephonyManager;
    private BluetoothDevice mDevice;
    private String mBlueToothAction;
    private String[] mStrCommandsForSnooze;
    private String[] mStrCommandsForDismiss;
    private int mStartServiceResult = SERVICE_NOT_START;


    public boolean getIsSupportForVoiceCommands() {
        return mIsSupportForVoiceCommands;
    }

    public void setAlertUri(String alertUri) {
        this.mAlertSoundUriString = alertUri;
    }

    private HfmClient.Callback mCallBack = new HfmClient.Callback() {
        //for HfmClient service ready
        @Override
        public void onReserveServiceComplete(int statusCode) {

            if (statusCode == HfmClient.SUCCESS_SERVICE_READY) {
                //Voice command
                mHFMServiceReady = true;
                if (mHFMClient != null) {
                    mHFMClient.setNotificationSoundEnabled(false);
                    mHFMClient.setDefaultRetryEnabled(false);
                }
            } else {
                mHFMServiceReady = false;
                mIsSupportForVoiceCommands = false;
            }
            Log.d(TAG, "HfmCallback onReserveServiceComplete: " + statusCode
                    + ", mbHFMServiceReady:" + mHFMServiceReady + "mIsSupportForVoiceCommands :"
                    + mIsSupportForVoiceCommands );
            if (mHFMServiceReady) {
                voiceCommand();
            }
            super.onReserveServiceComplete(statusCode);
        }


        /**
         * callback select commands for alarm clock
         */
        @Override
        public void onSelectCommandComplete(int statusCode, String command) {
            Log.d(TAG, "HfmCallback onSelectCommandComplete: " + statusCode + ",command:" + command);
            if (command != null) {
                for (String s : mStrCommandsForDismiss) {
                    if (command.equals(s)) {
                        triggerVoiceCommand(DISMISS_COMMAND_FLAG);
                        break;
                    }
                }
                for (String s : mStrCommandsForSnooze) {
                    if (command.equals(s)) {
                        triggerVoiceCommand(SNOOZED_COMMAND_FLAG);
                        break;
                    }
                }
            } else if (statusCode == HfmClient.ERROR_CANNOT_IDENTIFY_COMMAND) {
                Log.d(TAG, "HfmCallback request again");
            }
            super.onSelectCommandComplete(statusCode, command);
        }
    };


    private VoiceManager(Context context) {
        Log.d(TAG, "getInstance");
        this.mContext = context.getApplicationContext();
        if (!isVoiceCaptureSupported()){
            return;
        }

        Map<Integer, String[]> stringsCommands = HfmClient.getCommands(mContext, HfmClient.VOICE_COMMAND_GROUP_ALARM);
        if (stringsCommands != null) {
            mStrCommandsForDismiss = stringsCommands.get(HfmClient.COMMAND_TYPE_DISMISS);
            mStrCommandsForSnooze = stringsCommands.get(HfmClient.COMMAND_TYPE_SNOOZE);
        }

        //for bluetooth
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mBluetoothReceiver = new BluetoothReceiver();
        mContext.registerReceiver(mBluetoothReceiver, intentFilter);

        //for settings
        mContentResolver = mContext.getContentResolver();
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                triggerHfmServiceAndUpdateUI();
            }
        };
        mContentResolver.registerContentObserver(Settings.Secure.getUriFor(AlarmVoiceUtils.VOICE_ALARM_CONTROL_ENABLE), false, mContentObserver);

        //for phone
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

    }

    public static synchronized VoiceManager getInstance(Context context) {
        Log.d(TAG, "VoiceManager");
        if (sVoiceManager == null) {
            sVoiceManager = new VoiceManager(context);
        }
        return sVoiceManager;
    }

    public synchronized void releaseVoiceManager() {
        Log.d(TAG, "releaseVoiceManager");
        try {
            if (mContext != null && mBluetoothReceiver != null) {
                mContext.unregisterReceiver(mBluetoothReceiver);
                mBluetoothReceiver = null;
            }
            if (mContentResolver != null) {
                mContentResolver.unregisterContentObserver(mContentObserver);
            }
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "unregisterReceiver fail  " + e.toString());
        }

        if (mVoiceManagerServiceCallBack != null) {
            mVoiceManagerServiceCallBack = null;
        }
        if (mVoiceManagerDialogCallBack != null) {
            mVoiceManagerDialogCallBack = null;
        }
        if (mVoiceManagerLockScreenCallBack != null) {
            mVoiceManagerLockScreenCallBack = null;
        }
        sVoiceManager = null;
        mContext = null;
    }

    public void setDialogCallback(VoiceManagerUICallBack callback) {
        mVoiceManagerDialogCallBack = callback;
    }

    public void setServiceCallBack(VoiceManagerServiceCallBack voiceManagerServiceCallBack) {
        mVoiceManagerServiceCallBack = voiceManagerServiceCallBack;
    }

    public void setLockScreenCallBack(VoiceManagerUICallBack callBack) {
        mVoiceManagerLockScreenCallBack = callBack;
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBlueToothAction = intent.getAction();
            Log.d(TAG, "action =" + mBlueToothAction);
            mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (mBlueToothAction.equals(BluetoothDevice.ACTION_ACL_CONNECTED)
                    || mBlueToothAction.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                    && getBluetoothDevice() == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
                triggerHfmServiceAndUpdateUI();
                if (DEBUG_FLAG) {
                    Log.d(TAG, "bluetooth headset " + mBlueToothAction);
                }
            }
        }
    }

    private int getBluetoothDevice() {
        int bluetoothDevice = -1;
        if (mDevice != null) {
            BluetoothClass bluetoothClass = mDevice.getBluetoothClass();
            if (bluetoothClass != null) {
                Log.d(TAG, "device.getBluetoothClass().getDeviceClass() =" + bluetoothClass.getDeviceClass());
                return bluetoothClass.getDeviceClass();
            }
        }
        return bluetoothDevice;
    }

    //for phone state listener
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                Log.d(TAG, "onCallStateChanged CALL_STATE_IDLE");
                triggerHfmServiceAndUpdateUI();
            }
        }
    };

    /**
     * select commands for HfmClient
     */
    private void voiceCommand() {
        if (mHFMClient != null) {
            Speech[] commands = new Speech[mStrCommandsForSnooze.length + mStrCommandsForDismiss.length];
            for (int i = 0; i < mStrCommandsForSnooze.length; i++) {
                Log.d(TAG, "command:" + mStrCommandsForSnooze[i]);
                commands[i] = Speech.createSpeechFromText(mStrCommandsForSnooze[i]);
            }
            for (int i = 0; i < mStrCommandsForDismiss.length; i++) {
                Log.d(TAG, "command:" + mStrCommandsForDismiss[i]);
                commands[i + mStrCommandsForSnooze.length] = Speech.createSpeechFromText(mStrCommandsForDismiss[i]);
            }
            mHFMClient.selectCommand(commands, HfmClient.VOICE_COMMAND_GROUP_ALARM);

        } else {
            Log.d(TAG, "Hfm voiceCommand() mHFMClient is null");
        }
    }

    /**
     * trigger commands for alarm dismiss or snooze
     *
     * @param flagDismissOrSnooze flag for dismiss or snooze
     */
    private void triggerVoiceCommand(int flagDismissOrSnooze) {
        Log.d(TAG, "triggerVoiceCommand");
        if (flagDismissOrSnooze == DISMISS_COMMAND_FLAG) {
            if (mVoiceManagerServiceCallBack != null) {
                mVoiceManagerServiceCallBack.dismissAlarm();
            }
        } else if (flagDismissOrSnooze == SNOOZED_COMMAND_FLAG) {
            if (mVoiceManagerServiceCallBack != null) {
                mVoiceManagerServiceCallBack.snoozeAlarm();
            }
        }
    }


    /**
     * Determines whether the current conditions to meet the requirements.
     *
     * @return true (BT not connected  and telephone is idle and sounds not silent and voice control is open).
     * false （is not support anyone）.
     */
    private boolean isVoiceCaptureSupported() {
        return false;
       /* boolean isSupported = true;
        if (mContext == null) {
            return false;
        }
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int headset = adapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        if (mDevice != null && BluetoothDevice.ACTION_ACL_CONNECTED.equals(mBlueToothAction)
                && (getBluetoothDevice() == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET)) {
            isSupported = false;
            Log.d(TAG, "connecting or connected for headset for BroadcastReceiver ");
        } else if (!AlarmVoiceUtils.getVoiceAlarmControlEnable(mContext)) {
            isSupported = false;
            Log.d(TAG, "settings don't open");
        } else if (headset == BluetoothProfile.STATE_CONNECTED || headset == BluetoothProfile.STATE_CONNECTING) {
            Log.d(TAG, "connecting or connected for headset ");
            isSupported = false;
        } else if (mTelephonyManager != null && mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
            Log.d(TAG, "telephone is CALL_STATE_OFFHOOK ");
            isSupported = false;
        } else if (isAlarmSilent()) {
            Log.d(TAG, "alarm songs is silent ");
            isSupported = false;
        }

        if (isSupported) {
            Locale locale = Locale.getDefault();
            int localeResult = HfmClient.isSupportedLocaleEx(mContext, locale, HfmClient.VOICE_COMMAND_GROUP_ALARM);
            Log.d(TAG, "mLocaleResult:" + localeResult);
            isSupported = localeResult == HfmClient.RESULT_SUPPORT;
        }
        Log.d(TAG, "isSupported = " + isSupported);
        return isSupported;*/
    }

    private void newHfmClient() {
        if (mHFMClient == null) {
            Log.d(TAG, "newHfmClient");
            Bundle appInfo = new Bundle();
            appInfo.putInt(HfmClient.BUNDLE_KEY_GROUP, HfmClient.VOICE_COMMAND_GROUP_ALARM);
            String packageName = mContext.getPackageName();
            mHFMClient = new HfmClient(mCallBack, mContext, appInfo, packageName, packageName
                    , TIME_OUT, HfmClient.PRIORITY_LEVEL_4, false);
        }
    }

    // Determines whether the alarm is mute
    private boolean isAlarmSilent() {
        boolean silent = false;
        Uri alertSoundUri;
        Log.d(TAG, "mAlertSoundUriString =" + mAlertSoundUriString);
        if (("".equals(mAlertSoundUriString)) || (Settings.System.DEFAULT_ALARM_ALERT_URI.toString().equals(mAlertSoundUriString))) {
            Log.d(TAG, "mAlertSoundUriString =" + mAlertSoundUriString);
            alertSoundUri = AlertUtils.getAlarmDefaultAlertUri(mContext);
        } else if (mAlertSoundUriString == null) {
            // silent case
            alertSoundUri = null;
        } else {
            // general media provider case
            alertSoundUri = Uri.parse(mAlertSoundUriString);
        }
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int volLevel = 100;
        if (audioManager != null) {
            volLevel = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            Log.i(TAG, "Alarm sound volume level = " + volLevel);
        }
        if (alertSoundUri == null || (volLevel == 0)) {
            silent = true;
        }

        Log.d(TAG, "alarmSongIsSilent =" + silent + " alertSoundUri = " + alertSoundUri);
        return silent;
    }

    private void triggerHfmServiceAndUpdateUI() {
        if (isVoiceCaptureSupported()) {
            startHfmService();
            if (mVoiceManagerDialogCallBack != null) {
                mVoiceManagerDialogCallBack.showTips(true);
            }
            if (mVoiceManagerLockScreenCallBack != null) {
                mVoiceManagerLockScreenCallBack.showTips(true);
            }
        } else {
            releaseHfmClient();
            if (mVoiceManagerDialogCallBack != null) {
                mVoiceManagerDialogCallBack.showTips(false);
            }
            if (mVoiceManagerLockScreenCallBack != null) {
                mVoiceManagerLockScreenCallBack.showTips(false);
            }
        }
    }


    public void startHfmService() {
        Log.d(TAG, "startHfmService");
        if (isVoiceCaptureSupported()) {
            mIsSupportForVoiceCommands = true;
            newHfmClient();
            Log.d(TAG, "startHfmService flagForVoiceCommands = " + mIsSupportForVoiceCommands);
            if (mHFMClient != null && mStartServiceResult != HfmClient.SUCCESS) {
                mStartServiceResult = mHFMClient.reserveService(false, true);
                Log.d(TAG, "start client" + " , reserveResult=" + mStartServiceResult);
            }
        } else {
            mIsSupportForVoiceCommands = false;
        }

    }

    public void releaseHfmClient() {
        if (mHFMClient != null) {
            Log.d(TAG, "releaseHfmClient");
            mHFMClient.abort();
            int releaseService = mHFMClient.releaseService();
            Log.d(TAG, "releaseService： " + releaseService);
            mHFMClient = null;
            mStartServiceResult = SERVICE_NOT_START;
            mIsSupportForVoiceCommands = false;
        }
    }

    /**
     * callback for UI update.
     */
    public interface VoiceManagerUICallBack {
        /**
         * show tips UI update.
         *
         * @param show true show tips ui
         *             false not show tips ui
         */
        void showTips(boolean show);
    }

    /**
     * callback for Service update.
     */
    public interface VoiceManagerServiceCallBack {
        /**
         * call back dismiss alarm.
         */
        void dismissAlarm();

        /**
         * call back snooze alarm.
         */
        void snoozeAlarm();
    }

}
