package com.htc.android.worldclock.timer;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcTimePicker;

public class TimerResUtils extends ResUtils {

    private final int SHADOW_COLOR_1ST = 0xff3a3a3a;
    private final int SHADOW_COLOR_2ND = 0x00000000;
    private HtcFooterButton mStartButton;
    private HtcFooterButton mResetButton;
    private HtcFooter mHtcFooter;
    private HtcTimePicker mHtcTimePicker;

    public TimerResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        mActivity.getResources().getDrawable(android.R.drawable.ic_delete).setDither(false);

        mHtcFooter = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
        mHtcFooter.setDividerEnabled(false);
        setHtcFooterButtonResource();
        setStartButtonView(true);
        setResetButtonEnabled(false);
    }

    public void setHtcFooterButtonResource() {
        mStartButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn2);
        mStartButton.setEnabled(true);
        mStartButton.setImageResource(R.drawable.icon_btn_timer_light);
        mStartButton.setText(R.string.start);
        mResetButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn3);
        mResetButton.setEnabled(true);
        mResetButton.setImageResource(R.drawable.icon_btn_retry_light);
        mResetButton.setText(R.string.reset);
    }
    
    public void setStartButtonView(boolean bStart) {

        if (bStart) {
            mStartButton.setText(R.string.start);
        } else {
            mStartButton.setText(R.string.stop);
        }
    }

    public void setStartButtonImage() {
        mStartButton.setImageResource(R.drawable.icon_btn_timer_light);
    }

    public void setResetButtonEnabled(boolean enabled) {
        mResetButton.setEnabled(enabled);
    }

    public void resetLayout() {

        setLayout(R.id.timerPicker
            , R.dimen.timerPicker_width, 0
            , 0, R.dimen.timerPickerAll_marginTop);
    }
}
