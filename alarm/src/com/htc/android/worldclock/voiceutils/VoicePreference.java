package com.htc.android.worldclock.voiceutils;

import android.content.Context;
import android.content.Intent;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.htc.android.worldclock.R;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.hfmclient.HfmClient;

/**
 * UI for alarm settings voice control
 */
public class VoicePreference extends SwitchPreference {

    private static final String TAG = "VoicePreference";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String SETTINGS_ACTION = "com.htc.intent.action.settings.VOICE_CONTROL_LIST";
    private static final int SETTINGS_VOICE_ALARM_TYPE = 1;
    private static final int SETTINGS_VOICE_ALARM_ENABLE = 1;
    private static final int SETTINGS_VOICE_ALARM_NOT_ENABLE = 0;
    private final Context mContext;
    private LinearLayout mSwitch;
    private VoiceSwitchButton mPreferenceSwitch;


    public VoicePreference(Context context) {
        this(context, null);
    }

    public VoicePreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public VoicePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        if (DEBUG_FLAG) Log.e(TAG, "VoicePreference");
        setLayoutResource(R.layout.htc_preference_voice_alarm_switch_item);
    }

    //for init UI
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSwitch = (LinearLayout) view.findViewById(R.id.switch_pref);
        mPreferenceSwitch = (VoiceSwitchButton) view.findViewById(R.id.switch_btn);
        mPreferenceSwitch.setChecked(AlarmVoiceUtils.getVoiceAlarmControlEnable(mContext));
        mPreferenceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    int isSupportAEC = AlarmVoiceUtils.isSupportAEC(mContext);
                    if (DEBUG_FLAG) Log.d(TAG, "isSupportAEC = " + isSupportAEC);
                    if (isSupportAEC == HfmClient.RESULT_SUPPORT) {
                        AlarmVoiceUtils.setVoiceAlarmControlEnable(mContext, SETTINGS_VOICE_ALARM_ENABLE);
                   } else {
                        Intent intent = new Intent(SETTINGS_ACTION);
                        intent.putExtra("type",SETTINGS_VOICE_ALARM_TYPE);
                        mContext.startActivity(intent);
                        mPreferenceSwitch.setChecked(false);
                    }
                } else {
                    AlarmVoiceUtils.setVoiceAlarmControlEnable(mContext, SETTINGS_VOICE_ALARM_NOT_ENABLE);
                }
            }
        });
        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SETTINGS_ACTION);
                intent.putExtra("type",SETTINGS_VOICE_ALARM_TYPE);
                mContext.startActivity(intent);
            }
        });
    }
}
