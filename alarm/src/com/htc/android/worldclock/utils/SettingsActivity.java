/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.htc.android.worldclock.utils;

import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.voiceutils.AlarmVoiceUtils;
import com.htc.android.worldclock.voiceutils.VoicePreference;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.hfmclient.HfmClient;
import com.htc.lib1.theme.ThemeType;

/**
 * Settings for the Alarm Clock.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "WorldClock.SettingsActivity";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    public static final int ALARM_STREAM_TYPE_BIT = 1 << AudioManager.STREAM_ALARM;
    private static final String KEY_ALARM_IN_VOLUME_INCREASE_MODE = "alarm_in_volume_increase_mode";
    private static final String KEY_ALARM_IN_ALARM_VOLUME = "alarm_in_alarm_volume";
    private static final String KEY_ALARM_IN_VOICE_ALARM = "alarm_in_voice_mode";
    private CheckBoxPreference mAlarmInVolumeIncreaseModePref;
    private VoicePreference mVoicePreference;
    private SettingsResUtils mSettingsResUtils;
    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_VOLUME_BEHAVIOR = "volume_button_setting";
    public static final String KEY_FLIP_BEHAVIOR = "flip_to_setting";

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

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mHtcFontscale = HtcSkinUtils.initHtcFontScale(this);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(savedInstanceState);
        if (Global.isSupportAlarmVolumeKeyInSilentMode()) {
            addPreferencesFromResource(R.xml.common_sprint_settings);
        } else {
            addPreferencesFromResource(R.xml.common_settings);
        }

        ResUtils.enablePreferenceStatusBarTheme(this);
        mAlarmInVolumeIncreaseModePref = (CheckBoxPreference) findPreference(KEY_ALARM_IN_VOLUME_INCREASE_MODE);
        mVoicePreference = (VoicePreference) findPreference(KEY_ALARM_IN_VOICE_ALARM);
        //if not support AEC this VoicePreference would be remove.
        int localResult = AlarmVoiceUtils.isSupportAEC(this);
        if ( localResult == HfmClient.RESULT_NOT_SUPPORT || localResult == HfmClient.RESULT_NOT_INSTALL_ENGINE) {
            getPreferenceScreen().removePreference(mVoicePreference);
            mVoicePreference = null;
        }
        //add judge whether remove flip pref in setting list by isSupportSensorFeature
        if (!HtcPhoneSensorFunctions.isSupportSensorFeature(this)) {
            LongSummaryListPreference flipPref = (LongSummaryListPreference) findPreference(KEY_FLIP_BEHAVIOR);
            getPreferenceScreen().removePreference(flipPref);
        }
        mSettingsResUtils = new SettingsResUtils(this, null);
        mSettingsResUtils.initResources();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(SettingsActivity.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onStop() {
        super.onStop();
        ((ClockVolumePreference)getPreferenceManager().findPreference(KEY_ALARM_IN_ALARM_VOLUME)).stopSample();
    }

    @Override
    protected void onDestroy() {
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAlarmInVolumeIncreaseModePref) {
            if (mAlarmInVolumeIncreaseModePref.isChecked()) {
                PreferencesUtil.setVolumeIncrease(this, true);
            } else {
            	PreferencesUtil.setVolumeIncrease(this, false);
            }
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        final ListPreference listPref = (ListPreference) pref;
        final int idx = listPref.findIndexOfValue((String) newValue);
        listPref.setSummary(listPref.getEntries()[idx]);
        return true;
    }

    @SuppressWarnings("deprecation")
    private void refresh() {
        mAlarmInVolumeIncreaseModePref.setChecked(PreferencesUtil.getVolumeIncrease(this));
        if (mVoicePreference != null) {
            mVoicePreference.setChecked(AlarmVoiceUtils.getVoiceAlarmControlEnable(this));
        }
        ListPreference snooze = (ListPreference) findPreference(KEY_ALARM_SNOOZE);
        snooze.setSummary(snooze.getEntry());
        snooze.setOnPreferenceChangeListener(this);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        HtcSkinUtils.initHtcFontScale(this);
        mSettingsResUtils.switchTheme(newConfig);
    }
}
