package com.htc.android.worldclock.voiceutils;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.hfmclient.HfmClient;

import java.util.Locale;

/**
 * get settings voice control state and check this phone is support AEC or not.
 * create by AMT_MASD_LI_YU 2017.02.20.
 */
public class AlarmVoiceUtils {

    /**
     * for settings voice control name key.
     **/
    public static final String VOICE_ALARM_CONTROL_ENABLE = "htc_voicecontrol_alarm_enabled";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String TAG = "AlarmVoiceUtils";
    /**
     * voice control open.
     */
    private static final int VOICE_CONTROL_SWITCH = 1;

    /**
     *if didn't set default voice control status then set it close.
     */
    private static final int DEFAULT_VOICE_CONTROL = 0;


    /**
     * set settings voice control open and close.
     *
     * @param context context for getContentResolver.
     * @param enable  set switch open or close.
     */
    public static void setVoiceAlarmControlEnable(Context context, int enable) {
        try {
            boolean result = Settings.Secure.putInt(context.getContentResolver(), VOICE_ALARM_CONTROL_ENABLE, enable);
            if (DEBUG_FLAG) {
                Log.d(TAG, "setVoiceAlarmControlEnable: reseult = " + result);
            }
        } catch (Exception e) {
            Log.w(TAG, "setVoiceAlarmControlEnable: setVoiceAlarmControlEnable fail e = " + e.toString());
        }
    }

    /**
     * get settings voice control status.
     *
     * @param context context for getContentResolver.
     * @return voice control status.
     */
    public static boolean getVoiceAlarmControlEnable(Context context) {
        boolean ret = false;
        try {
            int voiceControl = Settings.Secure.getInt(context.getContentResolver(),
                    VOICE_ALARM_CONTROL_ENABLE, DEFAULT_VOICE_CONTROL);
            if (DEBUG_FLAG) {
                Log.d(TAG, "getVoiceAlarmControlEnable: htc_voicecontrol_alarm_enabled = " + voiceControl);
            }
            if (voiceControl == VOICE_CONTROL_SWITCH) {
                ret = true;
            }
        } catch (Exception e) {
            Log.w(TAG, "getVoiceAlarmControlEnable: getVoiceAlarmControlEnable fail e = " + e.toString());
        }
        Log.d(TAG, "getVoiceAlarmControlEnable: ret = " + ret);
        return ret;
    }

    /**
     * check this phone is support AEC or not.
     *
     * @param context for HfmClient param.
     * @return localeResult.
     */
    public static int isSupportAEC(Context context) {
        Locale locale = Locale.getDefault();
        int localeResult = HfmClient.isSupportedLocaleEx(context, locale, HfmClient.VOICE_COMMAND_GROUP_ALARM);
        Log.d(TAG, "isSupportAEC localeResult =" + localeResult);
        return localeResult;
    }

}
