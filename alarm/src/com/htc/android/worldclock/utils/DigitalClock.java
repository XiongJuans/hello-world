/*
 * Copyright (C) 2008 Google Inc.
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
//import android.pim.DateFormat;
//for cupcake
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.TimerTask;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.AlarmItem;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.worldclock.CityTime;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

/**
 * Displays the time
 */
public class DigitalClock extends RelativeLayout {
    private static final String TAG = "WorldClock.DigitalClock";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private final static String M12 = "hh:mm";
    private final static String M24 = "kk:mm";
    private final double DIGITAL_SHADOWLAY=0.1;
    private final static String TALKBACK_M12 = "h:mm a";
    private final static String TALKBACK_M24 = "hh:mm";
    private Calendar mCalendar;
    private String mFormat;
    private String mTalkbackFormat;
    private java.text.DateFormat mTimeFormat;
    private TextView mTimeDisplay;
    private TextView mDayDisplay;
    private AmPm mAmPm;
    private boolean mLive = true;
    private boolean mColorChanging = true;
    private boolean mAttached;
    private CharSequence[] mDaysOfWeek;
    private CharSequence[] mWholeDaysOfWeek;
    private boolean mIsAlarmAlert = false;
    private boolean mSingleView = false;
    boolean m24HourMode = false;
    protected IntentReceiver mIntentReceiver = null;
    
    private java.util.Timer mMinuteTimer;
    private Context mContext;
    /* called by system on minute ticks */
    private final Handler mHandler = new Handler();
    private final Handler mUIHandler = new UIHandler();

    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message m) {
            
            updateTime();
        }
    }

    private class MinuteTask extends TimerTask {
        @Override
        public void run() {
            
            mUIHandler.sendEmptyMessage(0);
        }
    }

    protected class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            
            if (mLive
                    && intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                mCalendar = Calendar.getInstance();
            }

            if (mLive) {
                if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)
                        || intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
                    resetTimer();
                }
            }
            updateTime();
        }
    }

    static class AmPm {
        public int mColorWhite, mColorDark;

        private TextView mAm_Pm;
        public boolean mIsMorning;

        AmPm(View parent) {

            mAm_Pm = (TextView) parent.findViewById(R.id.am_pm);

            Resources r = parent.getResources();
            mColorWhite = r.getColor(R.color.light_primaryfont_color); // color night color
            mColorDark = r.getColor(R.color.dark_primaryfont_color); // color day color
        }

        void setShowAmPm(boolean show) {
            
            mAm_Pm.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }

        void setShowAmPmForAlert(boolean show) {
            
            mAm_Pm.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        void setIsMorning(boolean isMorning) {
            mAm_Pm.setText(isMorning ? R.string.am : R.string.pm);
            //TalkBack set AM_PM TextView Accessibility no
            mAm_Pm.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void setIsNight(Context context, boolean isNight) {
            mAm_Pm.setTextColor(isNight ? mColorDark : mColorWhite);
        }
    }

    public DigitalClock(Context context) {
        this(context, null);
        
    }

    public DigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mDaysOfWeek = context.getResources().getTextArray(
                    R.array.days_of_week_capital);
        mWholeDaysOfWeek = context.getResources().getTextArray(
                R.array.days_of_week_whole_capital);
    }

    @Override
    protected void onFinishInflate() {
        
        super.onFinishInflate();
        mTimeDisplay = (TextView) findViewById(R.id.timeDisplay);
        mDayDisplay = (TextView) findViewById(R.id.day);
        mAmPm = new AmPm(this);
        mCalendar = Calendar.getInstance();
        setDateFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        
        super.onAttachedToWindow();
        if(DEBUG_FLAG) Log.d(TAG, "onAttachedToWindow: " + this);

        if (mAttached) {
            return;
        }
        mAttached = true;

        if (mLive) {
            /* monitor time ticks, time changed, timezone */
            if(mIntentReceiver == null) {
                mIntentReceiver = new IntentReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_TIME_CHANGED);
                filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                getContext().registerReceiver(mIntentReceiver, filter, Global.PERMISSION_APP_DEFAULT, mHandler);
            }
            mMinuteTimer = new java.util.Timer();
            mMinuteTimer.schedule(new MinuteTask(), 60000 - System.currentTimeMillis() % 60000, 60000); // 1000
                                                                                                        // =
                                                                                                        // 1
                                                                                                        // sec
        }

        if (mSingleView) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mTimeFormat = DateFormat.getTimeFormat(getContext());
                    mUIHandler.sendEmptyMessage(0);
                }
            }).start();
        } else {
//            updateTime();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        
        super.onDetachedFromWindow();

        if (!mAttached) {
            return;
        }
        mAttached = false;

        if (mLive) {
            if(mIntentReceiver != null) {
                getContext().unregisterReceiver(mIntentReceiver);
                mIntentReceiver = null;
            }
            
            if (mMinuteTimer != null) {
                mMinuteTimer.cancel();
                mMinuteTimer = null;
            }
        }
    }

    public void setShowDay(boolean show) {
        mDayDisplay.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    public void setSingleViewEnabled(boolean bSingleView) {
        
        mSingleView = bSingleView;
        setDateFormat();
    }

    public void updateTime(Calendar c) {
        
        mCalendar = c;
        updateTime();
    }

    public void updateTime(Calendar c, CityTime ct) {
        mCalendar = c;
        updateTime(ct);
    }

    private void updateTime() {
        
        if (mLive) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }

        if (mSingleView && (mTimeFormat != null)) {
            mTimeDisplay.setText(mTimeFormat.format(mCalendar.getTime()));
        } else {
            CharSequence newTime = DateFormat.format(mFormat, mCalendar);
            mTimeDisplay.setText(newTime);
        }
        //TalkBcak set TimeDisplay Format String
        mTimeDisplay.setContentDescription(getTimeDisplayTalkBack());
        mDayDisplay.setText(mDaysOfWeek[mCalendar.get(Calendar.DAY_OF_WEEK)]);
        mDayDisplay.setContentDescription(mWholeDaysOfWeek[mCalendar.get(Calendar.DAY_OF_WEEK)]);
        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);

        if (mColorChanging) {
            /* update text color and background color */
            if ((mCalendar.get(Calendar.HOUR_OF_DAY) < 6) || (mCalendar.get(Calendar.HOUR_OF_DAY) >= 18)) {
                this.setBackgroundResource(R.drawable.common_b_button_rest);
                mTimeDisplay.setTextColor(mAmPm.mColorDark);
                mDayDisplay.setTextColor(mAmPm.mColorDark);
                mAmPm.setIsNight(getContext(), true);
            } else {
                this.setBackgroundResource(R.drawable.common_button_rest);
                mTimeDisplay.setTextColor(mAmPm.mColorWhite);
                mDayDisplay.setTextColor(mAmPm.mColorWhite);
                mAmPm.setIsNight(getContext(), false);
            }
        }
    }

    private void updateTime(CityTime ct) {

        CharSequence newTime = null;
        int newTimeInt = 0;

        if (mLive) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }

        if (mSingleView && (mTimeFormat != null)) {
            mTimeDisplay.setText(mTimeFormat.format(mCalendar.getTime()));
        } else {
            newTime = DateFormat.format(mFormat, mCalendar);
            // set to UI of digit time
            mTimeDisplay.setText(newTime);
           // calculate digital time format to integer value
            CharSequence m24NewTime = DateFormat.format(M24, mCalendar);
            newTimeInt = CityTime.TimeFormatStringToInt(m24NewTime.toString());
        }
        //TalkBcak set TimeDisplay Format String
        mTimeDisplay.setContentDescription(getTimeDisplayTalkBack());
        mDayDisplay.setText(mDaysOfWeek[mCalendar.get(Calendar.DAY_OF_WEEK)]);
        mDayDisplay.setContentDescription(mWholeDaysOfWeek[mCalendar.get(Calendar.DAY_OF_WEEK)]);
        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);

        if (mColorChanging) {
            boolean isDay = false;
            if (ct != null) {
                int sunRise = ct.getWeatherDayTime();
                int sunSet = ct.getWeatherNightTime();
                if (sunRise < sunSet) { // normal case
                    if ((newTimeInt >= sunRise) && (newTimeInt < sunSet)) {
                        isDay = true;
                    }
                } else if (sunSet < sunRise) { // sun set at midnight
                    if ((newTimeInt < sunSet) || (newTimeInt >= sunRise)) {
                        isDay = true;
                    }
                } else if (sunRise == sunSet) {
                    isDay = true; // always day
                }
            }
            /* update text color and background color */
            if (isDay) {
                this.setBackgroundResource(R.drawable.common_button_rest);
                mTimeDisplay.setTextColor(mAmPm.mColorWhite);
                mDayDisplay.setTextColor(mAmPm.mColorWhite);
                mAmPm.setIsNight(getContext(), false);
            } else {
                this.setBackgroundResource(R.drawable.common_b_button_rest);
                mTimeDisplay.setTextColor(mAmPm.mColorDark);
                mDayDisplay.setTextColor(mAmPm.mColorDark);
                mAmPm.setIsNight(getContext(), true);
            }
        }
    }

    /***
     * TalkBack :get timeDisplay format String
     * eg:m24HourMode is true show 6:00  pronounce six am ;18:00 pronounce six pm
     *    m24HourMode is false show 6:00 am  pronounce six am ;6:00 pm pronounce six pm
     * @return :format String
     */
    private String getTimeDisplayTalkBack() {
        CharSequence talkbackText = DateFormat.format(mTalkbackFormat, mCalendar);
        if (m24HourMode && talkbackText != null) {
            if (mCalendar.get(Calendar.AM_PM) == 0 ) {
                talkbackText = talkbackText + getResources().getString(R.string.am);
            } else {
                talkbackText = talkbackText + getResources().getString(R.string.pm);
            }
        }
        return (talkbackText != null) ? talkbackText.toString() : "";
    }
    private void setDateFormat() {
        
        if (mSingleView) {
            mAmPm.setShowAmPmForAlert(false);
            return;
        }

        mFormat = m24HourMode ? M24 : M12;
        mTalkbackFormat = m24HourMode ? TALKBACK_M24 : TALKBACK_M12;
        if (mIsAlarmAlert) {
            mAmPm.setShowAmPmForAlert(!m24HourMode);
        } else {
            mAmPm.setShowAmPm(!m24HourMode);
        }
    }

    public void setLive(boolean live) {
        
        mLive = live;
    }

    public void set24HourMode(boolean is24Hour) {
        
        m24HourMode = is24Hour;
        setDateFormat();
    }

    public void setColorChanging(boolean change) {
        
        mColorChanging = change;
    }

    void setIsAlarmAlert(boolean alert) {
        
        mIsAlarmAlert = alert;
        setDateFormat();
    }

    private void resetTimer() {
        
        if (mLive) {
            if (mMinuteTimer != null) {
                mMinuteTimer.cancel();
                mMinuteTimer = null;
            }
            mMinuteTimer = new java.util.Timer();
            mMinuteTimer.schedule(new MinuteTask(), 60000 - System.currentTimeMillis() % 60000, 60000); // 1000
                                                                                                        // =
                                                                                                        // 1
                                                                                                        // sec
        }
    }

    public static int getWorldClockMaxTimeDisplay(Context context, ArrayList<CityTime> myList) {
        int maxTimeLength = ViewGroup.LayoutParams.WRAP_CONTENT;
        String format;
        CharSequence measuredTime;
        TextView measuredTimeDisplay;
        measuredTimeDisplay = new TextView(context);
        ViewGroup.LayoutParams lp_time = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        measuredTimeDisplay.setLayoutParams(lp_time);
        measuredTimeDisplay.setTextAppearance(context, R.style.fixed_world_clock_04);
        measuredTimeDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDefaultFontSize(context,R.dimen.time_display_size));
        
        if (myList != null) {
            for (int i = 0; i < myList.size(); i++) {
                CityTime ct = myList.get(i);
                TimeZone tz = ct.getTimeZone();
                Calendar c = Calendar.getInstance(tz);
                format = AlarmUtils.get24HourMode(context) ? M24 : M12;
                measuredTime = DateFormat.format(format, c);
                measuredTimeDisplay.setText(measuredTime);
                
                int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                measuredTimeDisplay.measure(widthMeasureSpec, heightMeasureSpec);
                int measuredWidth = measuredTimeDisplay.getMeasuredWidth();
                if (measuredWidth > maxTimeLength) {
                    maxTimeLength = measuredWidth;
                }
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "getWorldClockMaxTimeDisplay: maxTimeLength = " +  maxTimeLength);
        return maxTimeLength;
    }
    
    public static int getAlarmMaxTimeDisplay(Context context, ArrayList<AlarmItem> myList) {
        int maxTimeLength = ViewGroup.LayoutParams.WRAP_CONTENT;
        String format;
        CharSequence measuredTime;
        TextView measuredTimeDisplay;
        measuredTimeDisplay = new TextView(context);
        ViewGroup.LayoutParams lp_time = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        measuredTimeDisplay.setLayoutParams(lp_time);
        measuredTimeDisplay.setTextAppearance(context, R.style.fixed_world_clock_04);
        measuredTimeDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDefaultFontSize(context,R.dimen.time_display_size));
        
        if (myList != null) {
            for (int i = 0; i < myList.size(); i++) {
                AlarmItem ai = myList.get(i);
                /* set the alarm text */
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, ai.aHour);
                c.set(Calendar.MINUTE, ai.aMinutes);
                format = AlarmUtils.get24HourMode(context) ? M24 : M12;
                measuredTime = DateFormat.format(format, c);
                measuredTimeDisplay.setText(measuredTime);
                
                int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                measuredTimeDisplay.measure(widthMeasureSpec, heightMeasureSpec);
                int measuredWidth = measuredTimeDisplay.getMeasuredWidth();
                if (measuredWidth > maxTimeLength) {
                    maxTimeLength = measuredWidth;
                }
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "getAlarmMaxTimeDisplay: maxTimeLength = " +  maxTimeLength);
        return maxTimeLength;
    }
    
    public static int getMaxAMPMDisplay(Context context) {
        int maxAmPmLength = ViewGroup.LayoutParams.WRAP_CONTENT;
        TextView measuredAmPmDisplay;
        measuredAmPmDisplay = new TextView(context);
        ViewGroup.LayoutParams lp_ampm = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        measuredAmPmDisplay.setLayoutParams(lp_ampm);
        measuredAmPmDisplay.setTextAppearance(context, R.style.fixed_world_clock_05);
        measuredAmPmDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX,ResUtils.getDefaultFontSize(context,R.dimen.am_pm_size));
        measuredAmPmDisplay.setText(R.string.am);
        
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        measuredAmPmDisplay.measure(widthMeasureSpec, heightMeasureSpec);
        int measuredAMWidth = measuredAmPmDisplay.getMeasuredWidth();
        measuredAmPmDisplay.setText(R.string.pm);
        
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        measuredAmPmDisplay.measure(widthMeasureSpec, heightMeasureSpec);
        int measuredPMWidth = measuredAmPmDisplay.getMeasuredWidth();
        if (measuredAMWidth > measuredPMWidth) {
            maxAmPmLength = measuredAMWidth;
        } else {
            maxAmPmLength = measuredPMWidth;
        }
        if (DEBUG_FLAG) Log.d(TAG, "getMaxAMPMDisplay: maxAmPmLength = " +  maxAmPmLength);
        return maxAmPmLength;
    }
    
    public static int getWorldClockMaxDayDisplay(Context context, ArrayList<CityTime> myList) {
        int maxDayLength = ViewGroup.LayoutParams.WRAP_CONTENT;
        CharSequence[] daysOfWeek;
        CharSequence[] wholeDaysOfWeek;
        TextView measuredDayDisplay;
        measuredDayDisplay = new TextView(context);
        ViewGroup.LayoutParams lp_day = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        measuredDayDisplay.setLayoutParams(lp_day);
        measuredDayDisplay.setTextAppearance(context, R.style.fixed_world_clock_06);
        measuredDayDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDefaultFontSize(context,R.dimen.day_size));
        
        if (myList != null) {
            for (int i = 0; i < myList.size(); i++) {
                CityTime ct = myList.get(i);
                TimeZone tz = ct.getTimeZone();
                Calendar c = Calendar.getInstance(tz);
                daysOfWeek = context.getResources().getTextArray(R.array.days_of_week_capital);
                measuredDayDisplay.setText(daysOfWeek[c.get(Calendar.DAY_OF_WEEK)]);
                wholeDaysOfWeek = context.getResources().getTextArray(R.array.days_of_week_whole_capital);
                //Override setContentDescription method to solve pronounce format week.
                measuredDayDisplay.setContentDescription(wholeDaysOfWeek[c.get(Calendar.DAY_OF_WEEK)]);
                
                int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                measuredDayDisplay.measure(widthMeasureSpec, heightMeasureSpec);
                int measuredWidth = measuredDayDisplay.getMeasuredWidth();
                if (measuredWidth > maxDayLength) {
                    maxDayLength = measuredWidth;
                }
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "getWorldClockMaxDayDisplay: maxDayLength = " +  maxDayLength);
        return maxDayLength;
    }
    
    public static int getMaxDigitalDisplay(Context context, int timeLength, int ampmLength) {
        int maxDigitalLength = ViewGroup.LayoutParams.WRAP_CONTENT;
        int m4 = ResUtils.getDimensionPixelSize(context, R.dimen.common_dimen_m4);
        int m6 = ResUtils.getDimensionPixelSize(context, R.dimen.common_dimen_m6);
        int max_digital = ResUtils.getDimensionPixelSize(context, R.dimen.digital_clock_max_width);
        maxDigitalLength = m4 + timeLength + m6 + ampmLength + m4;
        if (maxDigitalLength > max_digital) {
            maxDigitalLength = max_digital;
        } else {
            maxDigitalLength = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        if (DEBUG_FLAG) Log.d(TAG, "getMaxDigitalDisplay: maxDigitalLength = " +  maxDigitalLength);
        return maxDigitalLength;
    }

    public void setDigitalClockAiColor() {
        this.setBackgroundResource(R.color.ai_tip_background);
        mTimeDisplay.setTextColor(mAmPm.mColorDark);
        mDayDisplay.setTextColor(mAmPm.mColorDark);
        mAmPm.setIsNight(getContext(), true);
    }
}
