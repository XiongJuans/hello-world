package com.htc.android.worldclock.utils;

import java.io.IOException;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import com.htc.lib0.HDKLib0Util.HDKException;
import com.htc.lib0.customization.HtcWrapCustomizationManager;
import com.htc.lib0.customization.HtcWrapCustomizationReader;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib3.wrap.intent.HtcIntents;

public class Global {
    public static final boolean SECURITY_FLAG = HtcWrapHtcDebugFlag.Htc_SECURITY_DEBUG_flag;
    private static final String TAG = "WorldClock.Global";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    public static final String PERFORMANCE_TAG = "AutoTest";
    public static final boolean PERFORMANCE_FLAG = false;
    public static final String PERMISSION_APP_DEFAULT = "com.htc.permission.APP_DEFAULT";
    public static final String PERMISSION_APP_HSP = "com.htc.sense.permission.APP_HSP";
    public static final String PERMISSION_APP_WORLDCLOCK_ALERT = "com.htc.sense.permission.worldclock.alert";
    public static final String HTC_SOUND_PICKER_PACKAGENAME = "com.htc.sdm";
    public static final String HTC_SOUND_PICKER_ACTION_NAME = "com.htc.soundpicker.SoundPicker";

    public static final String ACC_IS_SUPPORT_ALARM_VOLUMEKEY_IN_SILENT_MODE = "isSupportAlarmVolumeKeyInSilentMode";
    public static final String ACC_IS_SUPPORT_CMCC_CUSTOMIZATION = "isSupportCMCCCustomization";
    public static final String ACC_IS_SUPPORT_CT_CUSTOMIZATION = "isSupportCTCustomization";
    public static final String ACC_IS_SUPPORT_KDDI_CUSTOMIZATION = "isSupportKDDICustomization";

    public static final int ANDROID_PLATFORM_L = 21;
    public static final int ANDROID_PLATFORM_M = 23;

    public static final int ANDROID_PLATFORM_O = 26;

    public static final double HTC_SENSE_VERSION_8 = 8.0;
    
    // By Region ID
    public static final int ACC_REGION_ID_CHINA = 3;
    
    // Base ACC function
    public static boolean isSupportAccByFunction(String flagName) {
        boolean retValue = false;
        HtcWrapCustomizationManager manager = new HtcWrapCustomizationManager();
        HtcWrapCustomizationReader reader = manager.getCustomizationReader("WorldClock", HtcWrapCustomizationManager.READER_TYPE_XML, false);
        if (reader != null) {
            retValue = reader.readBoolean(flagName, false);
        } else {
            Log.w(TAG, "isSupportAccByFunction: Can't get ACC reader");
        }
        return retValue;
    }
    
    public static boolean isSupportAccByRegionId(int regionId) {
        boolean retValue = false;
        HtcWrapCustomizationManager manager = new HtcWrapCustomizationManager();
        HtcWrapCustomizationReader reader = manager.getCustomizationReader("System", HtcWrapCustomizationManager.READER_TYPE_XML, false);
        if (reader != null) {
            int retInteger = reader.readInteger("region", -1);
            if (retInteger == regionId) {
                retValue = true;
            }
        } else {
            Log.w(TAG, "isSupportAccByRegionId: Can't get ACC reader");
        }
        return retValue;
    }
    
    public static boolean isSupportAccChinaSense() {
        boolean retValue = false;
        HtcWrapCustomizationManager manager = new HtcWrapCustomizationManager();
        HtcWrapCustomizationReader reader = manager.getCustomizationReader("System", HtcWrapCustomizationManager.READER_TYPE_XML, false);
        if (reader != null) {
            retValue = reader.readBoolean("support_china_sense_feature", false);
        } else {
            Log.w(TAG, "isSupportAccChinaSense: Can't get ACC reader");
        }
        if (DEBUG_FLAG) Log.d(TAG, "isSupportAccChinaSense: " + retValue);
        return retValue;
    }
    
    public static double getAccBySenseVersion() {
        String retValue = "";
        double retDouble = 0;
        try {
            HtcWrapCustomizationManager manager = new HtcWrapCustomizationManager();
            HtcWrapCustomizationReader reader = manager.getCustomizationReader("System", HtcWrapCustomizationManager.READER_TYPE_XML, false);
            if (reader != null) {
                retValue = reader.readString("sense_version", "");
            } else {
                Log.w(TAG, "isSupportAccBySkuId: Can't get ACC reader");
            }
            retDouble = Double.valueOf(retValue);
        } catch (Exception e) {
            Log.w(TAG, "getAccBySenseVersion: e = " + e.toString());
        }
        if (DEBUG_FLAG) Log.d(TAG, "getAccBySenseVersion sense version = " + retDouble);
        return retDouble;
    }
    
    // WorldClock function list
    public static boolean isSupportAlarmColorLed() {
        boolean retValue = isSupportAccByFunction(ACC_IS_SUPPORT_KDDI_CUSTOMIZATION);
        if (DEBUG_FLAG) Log.d(TAG, "isSupportAlarmColorLed: " + retValue);
        return retValue;
    }
    
    public static boolean isSupportAutoSnoozeInCallState() {
        boolean retValue = isSupportAccByFunction(ACC_IS_SUPPORT_CMCC_CUSTOMIZATION);
        if (DEBUG_FLAG) Log.d(TAG, "isSupportAutoSnoozeInCallState: " + retValue);
        return retValue;
    }
    
    public static boolean isSupportStopwatchPauseResumeButton() {
        boolean retValue = isSupportAccByFunction(ACC_IS_SUPPORT_CMCC_CUSTOMIZATION);
        if (DEBUG_FLAG) Log.d(TAG, "isSupportStopwatchPauseResumeButton: " + retValue);
        return retValue;
    }
    
    public static boolean isCMCCSku() {
        boolean retValue = isSupportAccByFunction(ACC_IS_SUPPORT_CMCC_CUSTOMIZATION);
        if (DEBUG_FLAG) Log.d(TAG, "isCMCCSku: " + retValue);
        return retValue;
    }
    
    public static boolean isSupportAlarmVolumeKeyInSilentMode() {
        boolean retValue = isSupportAccByFunction(ACC_IS_SUPPORT_ALARM_VOLUMEKEY_IN_SILENT_MODE);
        if (DEBUG_FLAG) Log.d(TAG, "isSupportAlarmVolumeKeyInSilentMode: " + retValue);
        return retValue;
    }
    
    public static boolean isSupportExchangeCurrentHomePosition() {
        boolean retValue = isSupportAccByFunction(ACC_IS_SUPPORT_CT_CUSTOMIZATION);
        if (DEBUG_FLAG) Log.d(TAG, "isSupportExchangeCurrentHomePosition: " + retValue);
        return retValue;
    }
    
    public static boolean isSupportNoNeedPopupAlarmAlertUI() {
        boolean retValue = isSupportAccByRegionId(ACC_REGION_ID_CHINA);
        if (DEBUG_FLAG) Log.d(TAG, "isSupportNoNeedPopupAlarmAlertUI: " + retValue);
        return retValue;
    }
    
    public static boolean isSupportBeijingDefaultCityCode() {
        boolean retValue = isSupportAccByRegionId(ACC_REGION_ID_CHINA);
        if (DEBUG_FLAG) Log.d(TAG, "isSupportBeijingDefaultCityCode: " + retValue);
        return retValue;
    }
    
    public static String getHtcQuickBootPowerOnActionString(Context context) {
        String quickBootPowerOn = "";
        HtcIntents intent = new HtcIntents();
        try {
            quickBootPowerOn = intent.getString(HtcIntents.FieldName.ACTION_QUICKBOOT_POWERON);
            if (DEBUG_FLAG) Log.d(TAG, "getHtcQuickBootPowerOnActionString: quickBootPowerOn = " + quickBootPowerOn);
        } catch (HDKException e) {
            Log.w(TAG, "getHtcQuickBootPowerOnActionString: No such field fail e = " + e.toString());
        }
        return quickBootPowerOn;
    }
    
    public static int getAndroidSdkPlatform() {
        int sdkInt = android.os.Build.VERSION.SDK_INT;
        Log.i(TAG, "getAndroidSdkPlatform = " + sdkInt);
        return sdkInt;
    }
    
    public static boolean isHEPDevice(Context context) {
        boolean isHep = com.htc.lib0.HDKLib0Util.isHEPDevice(context);
        Log.i(TAG, "isHEPDevice = " + isHep);
        return isHep;
    }
    
    public static boolean isClockdExist() {
        boolean isExist = false;
        String CLOCKD_CLIENT_SOCKET_PATH = "/dev/socket/clockd";
        LocalSocket requestSocket = null;
        
        try {
            // creating a socket to connect to the server
            requestSocket = new LocalSocket();
            LocalSocketAddress localSocketAddr = new LocalSocketAddress(CLOCKD_CLIENT_SOCKET_PATH, LocalSocketAddress.Namespace.FILESYSTEM);
            requestSocket.connect(localSocketAddr);
            isExist = true;
        } catch (IOException ioe) {
            Log.w(TAG, "isClockdExist: IOException ioe = " + ioe.toString());
        } catch (Exception e) {
            Log.w(TAG, "isClockdExist: Exception e = " + e.toString());
        } finally {
            try {
                requestSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "isClockdExist: close exception e = " + e.toString());
            }
        }
        Log.d(TAG, "isClockdExist: is clockd exist = " + isExist);
        return isExist;
    }
}
