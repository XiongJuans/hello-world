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

package com.htc.android.worldclock.timer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

/**
 * Manages timer. Singleton, so it can be initiated in
 * TimerReceiver and shut down in the TimerAlert activity
 */
class TimerKlaxon {
    private static final String TAG = "WorldClock.TimerKlaxon";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private boolean mEnableStopCallback = true;
    private MediaPlayer mMediaPlayer;

    interface KillerCallback {
        public void onKilled();
    }

    // + Kun, to stop service
    interface StopCallback {
        public void onStopped();
    }

    // -Kun
    /** Play alert sound up to 1 minute before silencing */
    final static int TIMER_TIMEOUT_SECONDS = 1 * 60;
    final static String ICICLE_PLAYING = "IciclePlaying";

    private static TimerKlaxon sInstance;

    private boolean mPlaying = false;
    private Thread mTimeout;
    private KillerCallback mKillerCallback;
    private StopCallback mStopCallback;

    static synchronized TimerKlaxon getInstance() {
        if (sInstance == null) {
            sInstance = new TimerKlaxon();
        }
        return sInstance;
    }

    private TimerKlaxon() {
    }

    synchronized void play(Context context) {
        if (mPlaying) {
            mEnableStopCallback = false;
            stop(context);
            mEnableStopCallback = true;
        }

        AudioManager am =  (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            Log.i(TAG, "Alarm sound volume level = " + am.getStreamVolume(AudioManager.STREAM_ALARM));
        }

        /* play audio alert */
        String alertSoundUriString = PreferencesUtil.getTimerSoundUri(context);
        if (Timer.SILENT_SOUND_STRING.equals(alertSoundUriString)) {
            alertSoundUriString = null;
        }
        
        try {
            Uri alertSoundUri;
            if (DEBUG_FLAG) Log.d(TAG, "play: alertSoundUriString = " + alertSoundUriString);
            if (("".equals(alertSoundUriString)) || (Settings.System.DEFAULT_ALARM_ALERT_URI.toString().equals(alertSoundUriString))) {
                // default case, get audio alert from Setting
                alertSoundUri = AlertUtils.getTimerDefaultAlertUri(context);
            } else if (alertSoundUriString == null) {
                // silent case
                alertSoundUri = null;
            } else {
                // general media provider case
                alertSoundUri = Uri.parse(alertSoundUriString);
            }
            if (DEBUG_FLAG) Log.d(TAG, "play: alertSoundUri = " + alertSoundUri);

            /* check alert is exist or not */
            if ((null != alertSoundUri) && (!AlertUtils.isRingToneExist(context, alertSoundUri))) {
                /* get audio alert from Setting */
                alertSoundUri = AlertUtils.getTimerDefaultAlertUri(context);
            }
            
            String alertSoundTitle = "";
            // get alert sound title
            if (alertSoundUri == null) {
                alertSoundTitle = context.getString(R.string.st_silent);
            } else {
                Uri defaultAlarmUri = AlertUtils.getAlarmDefaultAlertUri(context);
                Ringtone defaultAlarmRingtone = RingtoneManager.getRingtone(context, defaultAlarmUri);
                Ringtone ringtone = RingtoneManager.getRingtone(context, alertSoundUri);
                if (ringtone != null) {
                    // check ringtone is exist or not
                    if ((defaultAlarmRingtone != null) && (defaultAlarmRingtone.getTitle(context).equals(ringtone.getTitle(context))) && (!alertSoundUri.equals(defaultAlarmUri))) {
                        // back to default
                        ringtone = RingtoneManager.getRingtone(context, AlertUtils.getTimerDefaultAlertUri(context));
                    }
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

        enableKiller(context);
        mPlaying = true;
    }

    /**
     * Stops alert sound and disables timer
     */
    synchronized void stop(Context context) {
        if (DEBUG_FLAG) Log.d(TAG, "stop");
        if (mPlaying) {
            mPlaying = false;

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
                Log.w(TAG, "releasePlayer: mMediaPlayer stop e = " + e.toString());
            }

            if (mEnableStopCallback) {
                if (mStopCallback != null) {
                    mStopCallback.onStopped();
                    mStopCallback = null;
                }
            }
        }
        disableKiller();
    }

    void setKillerCallback(KillerCallback killerCallback) {
        mKillerCallback = killerCallback;
    }

    void setStopCallback(StopCallback stopCallback) {
        mStopCallback = stopCallback;
    }

    /**
     * Kills alert sound after TIMER_TIMEOUT_SECONDS, so the alert sound
     * won't run all day.
     * 
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the timer expired.
     */
    private void enableKiller(final Context context) {
        mTimeout = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000 * TIMER_TIMEOUT_SECONDS);
                    if (DEBUG_FLAG) Log.d(TAG, "enableKiller: Timer killer triggered");
                    AlertUtils.timerNotification(context, true, true);
                    if (mKillerCallback != null) {
                        mKillerCallback.onKilled();
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "enableKiller: Timer killer triggered failed");
                }
            }
        });
        mTimeout.start();
    }

    private void disableKiller() {
        if (mTimeout != null) {
            mTimeout.interrupt();
            mTimeout = null;
            mKillerCallback = null;
        }
    }
}
