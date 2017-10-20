package com.htc.android.worldclock.alarmclock;

import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.DigitalClock;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib1.cc.support.widget.HtcTintManager;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcDeleteButton;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;

public class AlarmClockResUtils extends ResUtils {
    private HtcFooter mHtcFooter;

    public AlarmClockResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        if (Global.isSupportAccChinaSense()) {
            mHtcFooter = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
            mHtcFooter.setDividerEnabled(false);
            setHtcFooterButtonResource();
        }
    }

    public void setHtcFooterButtonResource() {
        HtcFooterButton addButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn3);
        addButton.setEnabled(true);
        addButton.setImageResource(R.drawable.icon_btn_add_light);
        addButton.setText(R.string.va_add);
        HtcFooterButton deleteButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn2);
        deleteButton.setEnabled(true);
        deleteButton.setImageResource(R.drawable.icon_btn_delete_light);
        deleteButton.setText(R.string.delete);
    }

    public View getCheckBox(View view) {
        /* checkbox of setting alarm */
        HtcDeleteButton db = (HtcDeleteButton) view.findViewById(R.id.function_delete);
        db.setVisibility(View.GONE);
        CheckBox cb = (CheckBox) view.findViewById(R.id.function_select);
        cb.setVisibility(View.VISIBLE);
        HtcTintManager htcTintManager = HtcTintManager.get(mActivity);
        htcTintManager.tintThemeColor(cb);
        return cb;
    }


    public View setCheckBox(View view, boolean enabled) {
        /* checkbox of setting alarm */
        HtcDeleteButton db = (HtcDeleteButton) view.findViewById(R.id.function_delete);
        db.setVisibility(View.GONE);
        CheckBox cb = (CheckBox) view.findViewById(R.id.function_select);
        cb.setVisibility(View.VISIBLE);
        cb.setChecked(enabled);
        HtcTintManager htcTintManager = HtcTintManager.get(mActivity);
        htcTintManager.tintThemeColor(cb);
        return cb;
    }

    // use for AlarmClock and DeleteAlarm
    public static void setDigitalClock(Context context, View view, int hour, int minutes) {
        DigitalClock digitalClock = (DigitalClock) view.findViewById(R.id.common_digital_clock_btn);
        digitalClock.setLive(false);
        digitalClock.setShowDay(false);
        digitalClock.set24HourMode(AlarmUtils.get24HourMode(context));
        /* set the alarm text */
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minutes);
        digitalClock.updateTime(c);
    }

    // use for AlarmClock and DeleteAlarm
    public static void setDescription(Context context, View view, String description) {
        TextView descriptionView = (TextView) view.findViewById(R.id.description);
        if ((description == null) || (description.length() == 0)) {
            descriptionView.setText(R.string.alarm_description);
            descriptionView.setAlpha(0.5f);
        } else {
            descriptionView.setText(description);
            descriptionView.setAlpha(1.0f);
        }
    }

    // use for AlarmClock and DeleteAlarm
    public static TextView[] getContainerPosition(TextView[] containerPosition, int startWeekDay, View mLayout) {
        // re-order container position "sun", "mon", "tue", "wed", "thu", "fri" and "sat" by startWeekDay.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = ResUtils.getDimensionPixelSize(mLayout.getContext(), R.dimen.day_of_week_margin);

        containerPosition[(startWeekDay + 5) % 7] = (TextView) mLayout.findViewById(R.id.sun); // 1st pos
        containerPosition[(startWeekDay + 5) % 7].setLayoutParams(params);
        containerPosition[(startWeekDay + 6) % 7] = (TextView) mLayout.findViewById(R.id.mon); // 2nd pos
        containerPosition[(startWeekDay + 6) % 7].setLayoutParams(params);
        containerPosition[(startWeekDay + 0) % 7] = (TextView) mLayout.findViewById(R.id.tue); // 3rd pos
        containerPosition[(startWeekDay + 0) % 7].setLayoutParams(params);
        containerPosition[(startWeekDay + 1) % 7] = (TextView) mLayout.findViewById(R.id.wed); // 4th pos
        containerPosition[(startWeekDay + 1) % 7].setLayoutParams(params);
        containerPosition[(startWeekDay + 2) % 7] = (TextView) mLayout.findViewById(R.id.thu); // 5th pos
        containerPosition[(startWeekDay + 2) % 7].setLayoutParams(params);
        containerPosition[(startWeekDay + 3) % 7] = (TextView) mLayout.findViewById(R.id.fri); // 6th pos
        containerPosition[(startWeekDay + 3) % 7].setLayoutParams(params);
        containerPosition[(startWeekDay + 4) % 7] = (TextView) mLayout.findViewById(R.id.sat); // 7th pos
        return containerPosition;
    }

    // use for AlarmClock and DeleteAlarm
    public static void setDaysOfWeek(Context context, View view, int startWeekDay, AlarmUtils.DaysOfWeek daysOfWeek) {
        /* set days of week */
        TextView[] containPosition = new TextView[7];
        CharSequence[] daysAbbr = context.getResources().getTextArray(R.array.days_of_week_abbr);
        CharSequence[] wholeDaysAbbr = context.getResources().getTextArray(R.array.days_of_week);
        containPosition = getContainerPosition(containPosition, startWeekDay, view);
        if (containPosition != null && daysAbbr != null && wholeDaysAbbr != null) {
            boolean[] mIsSet = daysOfWeek.getBooleanArray();
            for (int i = 0; i <= 6; i++) {
                containPosition[i].setText(daysAbbr[i]); // DaysAbbr is (M, TU, W, Th, F, Sa, Su) words
                containPosition[i].setContentDescription(wholeDaysAbbr[i]);
                if (mIsSet[i]) {
                    containPosition[i].setTextAppearance(context, R.style.fixed_world_clock_10);
                    containPosition[i].setTextSize(TypedValue.COMPLEX_UNIT_PX,ResUtils.getDefaultFontSize(context, R.dimen.days_of_week_size));
                    containPosition[i].setAlpha(1.0f);
                } else {
                    containPosition[i].setTextAppearance(context, R.style.fixed_list_secondary_xxs);
                    containPosition[i].setTextSize(TypedValue.COMPLEX_UNIT_PX,ResUtils.getDefaultFontSize(context, R.dimen.days_of_week_size));
                    containPosition[i].setAlpha(0.5f);
                }
            }
        }
    }

    public void resetLayout() {
    }
}
