/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.aiservice.AiUtils;
import com.htc.android.worldclock.alarmclock.SetAlarm.RepeatTypeEnum;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

/**
 * Manages alarms and vibe. Singleton, so it can be initiated in
 * AlarmReceiver and shut down in the AlarmAlert activity
 */
class AlarmKlaxon implements AlarmUtils.AlarmSettings {
    private static final String TAG = "WorldClock.AlarmKlaxon";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private boolean mEnableStopCallback = true;

    interface KillerCallback {
        public void onKilled();
    }

    // + Kun, to stop service
    interface StopCallback {
        public void onStopped();
    }

    // -Kun
    /** Play alarm up to 10 minutes before silencing */
    final static int ALARM_TIMEOUT_SECONDS = 10 * 60;
    final static String ICICLE_PLAYING = "IciclePlaying";
    final static String ICICLE_ALARMID = "IcicleAlarmId";
    private static final int VOLUME_INCREASE_INTERVAL = 3 * 1000; // mini second

    private static long[] sVibratePattern = new long[] { 500, 500 };

    private static AlarmKlaxon sInstance;

    private String mAlertSoundUriString;
    private AlarmUtils.DaysOfWeek mDaysOfWeek;
    private boolean mVibrate;

    private boolean mPlaying = false;
    private int mLastAlarmId;

    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;

    private Thread mTimeout;
    private KillerCallback mKillerCallback;
    private StopCallback mStopCallback;
    private static Context mContext;
    private AudioManager mAudioManager;
    private int mVolLevel;
    private Thread mVolIncreaseTimeout;
    private int mRepeatType;

    static synchronized AlarmKlaxon getInstance(Context context) {
        if (sInstance == null) {
            mContext = context;
            sInstance = new AlarmKlaxon();
        }
        return sInstance;
    }

    private AlarmKlaxon() {
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void reportAlarm(
        int idx, boolean enabled, int hour, int minutes, long alarmtime, AlarmUtils.DaysOfWeek daysOfWeek,
        boolean vibrate, String message, String alert, boolean snoozed, boolean offalarm, int repeat_type) {
        Log.i(TAG, "Alarm inform: alarm id = " + idx + ", hour = " + hour + ", minutes = " + minutes +
            ", vibrate = " + vibrate + ", alert = \"" + alert + "\", message = \"" + message +
            "\", mask = " + Integer.toBinaryString(daysOfWeek.getCoded()) + ", repeat_type = " + repeat_type);
        mAlertSoundUriString = alert;
        if (DEBUG_FLAG) Log.d(TAG, "reportAlarm: mAlertSoundUriString = " + mAlertSoundUriString);
        mDaysOfWeek = daysOfWeek;
        mVibrate = vibrate;
        mRepeatType= repeat_type;
    }

    synchronized void play(Context context, int alarmId, long alarmTime, String description, int alarmType) {
        if (DEBUG_FLAG) Log.d(TAG, "play: alarmId = " + alarmId + ", mPlaying = " + mPlaying);
        if (mPlaying) {
            mEnableStopCallback = false;
            stop(context, mLastAlarmId, false);
            mEnableStopCallback = true;
        }

        /* this will call reportAlarm() callback */
        ContentResolver contentResolver = context.getContentResolver();
        AlarmUtils.getAlarm(contentResolver, this, alarmId);

        mAudioManager =  (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mVolLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            Log.i(TAG, "Alarm sound volume level = " + mVolLevel);
        }

        try {
            if (AiUtils.getInstance(context).checkIsEarlyAlarmId(alarmId)) {
                //ai alarm use system default alert sound
                mAlertSoundUriString = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
                Log.i(AiUtils.AI_TAG, "early alarm sound uri" + mAlertSoundUriString);
                mVibrate = true;
            }
            Uri alertSoundUri;
            if (DEBUG_FLAG) Log.d(TAG, "play: mAlertSoundUriString = " + mAlertSoundUriString);
            if (("".equals(mAlertSoundUriString)) || (Settings.System.DEFAULT_ALARM_ALERT_URI.toString().equals(mAlertSoundUriString))) {
                // default case, get audio alert from Setting
                alertSoundUri = AlertUtils.getAlarmDefaultAlertUri(context);
            } else if (mAlertSoundUriString == null) {
                // silent case
                alertSoundUri = null;
            } else {
                // general media provider case
                alertSoundUri = Uri.parse(mAlertSoundUriString);
            }
            if (DEBUG_FLAG) Log.d(TAG, "play: alertSoundUri = " + alertSoundUri);

            /* check alert is exist or not */
            if ((null != alertSoundUri) && (!AlertUtils.isRingToneExist(context, alertSoundUri))) {
                /* get audio alert from Setting */
                alertSoundUri = AlertUtils.getAlarmDefaultAlertUri(context);
            }
            
            String alertSoundTitle = "";
            // get alert sound title
            if (alertSoundUri == null) {
                alertSoundTitle = context.getString(R.string.st_silent);
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(context, alertSoundUri);
                if (ringtone != null) {
                    ringtone.setStreamType(RingtoneManager.TYPE_ALARM);
                    alertSoundTitle = ringtone.getTitle(context);
                }
                if (DEBUG_FLAG) Log.d(TAG, "play: startPlayAlert begin");
                mMediaPlayer = AlertUtils.startPlayAlert(context, alertSoundUri);
            }
            Log.i(TAG, "Alert sound title = " + alertSoundTitle);
        } catch (Exception e) {
            Log.w(TAG, "play: Error playing alert e = " + e.toString());
        }

        if (mVibrate && (AlertUtils.isCallStateIdle(context))) {
            if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
                AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
                mVibrator.vibrate(sVibratePattern, 0, VIBRATION_ATTRIBUTES);
            } else {
                mVibrator.vibrate(sVibratePattern, 0);
            }
        } else {
            mVibrator.cancel();
        }

        enableKiller(context, alarmId, alarmTime, description, alarmType);
        mPlaying = true;
        mLastAlarmId = alarmId;
    }

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    synchronized void stop(Context context, int alarmId, boolean snoozed) {
        Log.i(TAG, "stop alarm sound, id = " + alarmId + ", snoozed = " + snoozed  + ", mRepeatType = " + mRepeatType);
        if (mPlaying) {
            mPlaying = false;

            releasePlayer();

            // Stop vibrator
            mVibrator.cancel();
            
            /* disable alarm only if it is not set to repeat */
            if ((RepeatTypeEnum.SKIPHOLIDAY.ordinal() != mRepeatType) && !snoozed && (((mDaysOfWeek == null) || !mDaysOfWeek.isRepeatSet()))) {
                AlarmUtils.enableAlarm(context, alarmId, false);
            }
            mDaysOfWeek = null;
            if (mEnableStopCallback) {
                if (mStopCallback != null) {
                    mStopCallback.onStopped();
                    mStopCallback = null;
                }
            }
        }
        disableKiller(context);
    }

    synchronized void mute(Context context) {
        Log.i(TAG, "mute alarm sound");
        if (mPlaying) {
            releasePlayer();
            // Stop vibrator
            mVibrator.cancel();
        }
    }

    synchronized void releasePlayer() {
        try {
            // Stop audio playing
            if (mMediaPlayer != null) {
                if (DEBUG_FLAG) Log.d(TAG, "stop: mMediaPlayer.stop() begin");
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                if (DEBUG_FLAG) Log.d(TAG, "stop: mMediaPlayer done");
            }
        } catch (Exception e) {
            Log.w(TAG, "releasePlayer: ringtone stop e = " + e.toString());
        }
    }

    /**
     * This callback called when alarm killer times out unattended
     * alarm
     */
    void setKillerCallback(KillerCallback killerCallback) {
        mKillerCallback = killerCallback;
    }

    void setStopCallback(StopCallback stopCallback) {
        mStopCallback = stopCallback;
    }

    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     * 
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     */
    private void enableKiller(final Context context, final int alarmId, final long alarmTime, final String description, final int alarmType) {
        synchronized (AlarmKlaxon.this) {
            mTimeout = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000 * ALARM_TIMEOUT_SECONDS);
                        if (DEBUG_FLAG) Log.d(TAG, "enableKiller.run: Alarm killer triggered");
                        AlertUtils.alarmNotification(context, alarmId, alarmTime, description, AlertUtils.ALERT_DIALOG_TIMEOUT, false, true);
                        synchronized (mTimeout) {
                            if (mKillerCallback != null) {
                                mKillerCallback.onKilled();
                            }
                        }
                    } catch (Exception e) {
                        // this is normal exception case
                        if (DEBUG_FLAG) Log.d(TAG, "enableKiller: trigger mTimeout interrupt by disableKiller e = " + e.toString());
                    }
                }
            });
            mTimeout.start();
            if (PreferencesUtil.getVolumeIncrease(context)) {
                // for increase volume
                mVolIncreaseTimeout = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mVolLevel != 0) {
                                int startLevel;
                                if (mVolLevel == 1) {
                                    startLevel = 1;
                                } else {
                                    startLevel = mVolLevel / 2;
                                }
                                for (int i = startLevel; i <= mVolLevel; i++) {
                                    Log.i(TAG, "enableKiller: mVolLevel = " + i);
                                    if (mAudioManager != null) {
                                        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, i, 0);
                                        Thread.sleep(VOLUME_INCREASE_INTERVAL);
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            Log.w(TAG, "enableKiller: Alarm volume level increase killer triggered failed");
                        }
                    }
                });
                mVolIncreaseTimeout.start();
            }
        }
    }

    private void disableKiller(Context context) {
        synchronized (AlarmKlaxon.this) {
            if (mTimeout != null) {
                synchronized (mTimeout) {
                    mTimeout.interrupt(); // it will trigger Thread.run exception
                    mKillerCallback = null;
                }
                mTimeout = null;
            }
            if (PreferencesUtil.getVolumeIncrease(context)) {
                if (mVolIncreaseTimeout != null) {
                    synchronized (mVolIncreaseTimeout) {
                        mVolIncreaseTimeout.interrupt();
                        mVolIncreaseTimeout = null;
                        // restore original volume when volume != 0 (DND mode can't set volume)
                        if ((mAudioManager) != null && (mVolLevel != 0)) {
                            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mVolLevel, 0);
                        }
                    }
                    mVolIncreaseTimeout = null;
                }
            }
        }
    }
}
