package com.htc.android.worldclock.voiceutils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Switch;

import com.htc.lib1.cc.support.widget.HtcTintManager;


public class VoiceSwitchButton extends Switch {

    private void init() {
        HtcTintManager.get(getContext()).tintThemeColor(this);
    }

    /**
     * Create a new MySwitchButton.
     *
     * @param context the application environment.
     */
    public VoiceSwitchButton(Context context) {
        super(context);
        init();
    }

    /**
     * Create a new MySwitchButton.
     *
     * @param context the application environment.
     * @param attrs   attributeSet.
     */
    public VoiceSwitchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

}
