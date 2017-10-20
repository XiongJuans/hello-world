package com.htc.android.worldclock.stopwatch;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.cc.widget.HtcListItemSeparator;
public class StopwatchResUtils extends ResUtils {
    private static final String TAG = "WorldClock.StopwatchResUtils";
    private final int MAX_SEC = 60000; // 100 minutes

    private int[] mResId = {
        R.drawable.clock_stopwatch_digit_0,
        R.drawable.clock_stopwatch_digit_1,
        R.drawable.clock_stopwatch_digit_2,
        R.drawable.clock_stopwatch_digit_3,
        R.drawable.clock_stopwatch_digit_4,
        R.drawable.clock_stopwatch_digit_5,
        R.drawable.clock_stopwatch_digit_6,
        R.drawable.clock_stopwatch_digit_7,
        R.drawable.clock_stopwatch_digit_8,
        R.drawable.clock_stopwatch_digit_9 };

    private ImageView mImageMinute10;
    private ImageView mImageMinute;
    private ImageView mImageSecond10;
    private ImageView mImageSecond;
    private ImageView mImageMilliSecond;
    private ImageView mImageLapMinute10;
    private ImageView mImageLapMinute;
    private ImageView mImageLapSecond10;
    private ImageView mImageLapSecond;
    private ImageView mImageLapMilliSecond;

    private int mOldMinute10;
    private int mOldMinute;
    private int mOldSecond10;
    private int mOldSecond;
    private int mOldMilliSecond;
    private int mOldLapMinute10;
    private int mOldLapMinute;
    private int mOldLapSecond10;
    private int mOldLapSecond;
    private int mOldLapMilliSecond;
    private HtcAlertDialog mHtcAlertDialog;
    private HtcFooterButton mStartButton;
    private HtcFooterButton mLapButton;
    private HtcFooterButton mResetButton;
    private HtcFooter mHtcFooter;
    private HtcListItemSeparator mSeparator;
    private ImageView mDivider;

    public StopwatchResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {

        setImageViewImageResource(R.id.digit_colon, "stopwatch_digit_colon", R.drawable.clock_stopwatch_digit_colon);
        setImageViewImageResource(R.id.dot, "stopwatch_digit_dot", R.drawable.clock_stopwatch_digit_dot);

        setImageViewImageResource(R.id.digit_colon2, "digit_colon", R.drawable.clock_stopwatch_digit_colon);
        setImageViewImageResource(R.id.dot2, "stopwatch_dot", R.drawable.clock_stopwatch_digit_dot);

        setLapTitleView();

        mImageMinute10 = (ImageView) findViewById(R.id.minute_ten);
        mImageMinute = (ImageView) findViewById(R.id.minute_unit);
        mImageSecond10 = (ImageView) findViewById(R.id.second_ten);
        mImageSecond = (ImageView) findViewById(R.id.second_unit);
        mImageMilliSecond = (ImageView) findViewById(R.id.millisecond);
        mImageLapMinute10 = (ImageView) findViewById(R.id.minute_ten2);
        mImageLapMinute = (ImageView) findViewById(R.id.minute_unit2);
        mImageLapSecond10 = (ImageView) findViewById(R.id.second_ten2);
        mImageLapSecond = (ImageView) findViewById(R.id.second_unit2);
        mImageLapMilliSecond = (ImageView) findViewById(R.id.millisecond2);
        mDivider = (ImageView) findViewById(R.id.stopwatch_divider);
        
        if(mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        	mDivider.setVisibility(View.INVISIBLE);
        } else {
        	mDivider.setVisibility(View.VISIBLE);
         }

        mImageMinute10.setImageResource(mResId[0]);
        mImageMinute.setImageResource(mResId[0]);
        mImageSecond10.setImageResource(mResId[0]);
        mImageSecond.setImageResource(mResId[0]);
        mImageMilliSecond.setImageResource(mResId[0]);
        mImageLapMinute10.setImageResource(mResId[0]);
        mImageLapMinute.setImageResource(mResId[0]);
        mImageLapSecond10.setImageResource(mResId[0]);
        mImageLapSecond.setImageResource(mResId[0]);
        mImageLapMilliSecond.setImageResource(mResId[0]);

        mHtcFooter = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
        mHtcFooter.setDividerEnabled(false);
        setHtcFooterButtonResource();
        setStartButtonView(true, R.string.start);
        setLapButtonEnabled(false);
        setResetButtonEnabled(false);
        resetLayout();
    }

    public void setHtcFooterButtonResource() {
        mStartButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn2);
        mStartButton.setEnabled(true);
        mStartButton.setImageResource(R.drawable.icon_btn_stopwatch_light);
        mStartButton.setText(R.string.start);
        mLapButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn3);
        mLapButton.setEnabled(true);
        mLapButton.setImageResource(R.drawable.icon_btn_lap_light);
        mLapButton.setText(R.string.lap);
        mResetButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn4);
        mResetButton.setEnabled(true);
        mResetButton.setImageResource(R.drawable.icon_btn_retry_light);
        mResetButton.setText(R.string.reset);
    }
    
    private void setLapTitleView() {
        mSeparator = (HtcListItemSeparator) findViewById(R.id.separator);
        if (mSeparator != null) {
            mSeparator.setText(HtcListItemSeparator.TEXT_LEFT, R.string.lap_id);
            mSeparator.setText(HtcListItemSeparator.TEXT_MIDDLE, R.string.lap_total_time);
            mSeparator.setText(HtcListItemSeparator.TEXT_RIGHT, R.string.lap_time);
            mSeparator.setTextStyle(HtcListItemSeparator.TEXT_LEFT, R.style.fixed_separator_primary_m);
            mSeparator.setTextStyle(HtcListItemSeparator.TEXT_MIDDLE, R.style.fixed_separator_primary_m);
            mSeparator.setTextStyle(HtcListItemSeparator.TEXT_RIGHT, R.style.fixed_separator_primary_m);
        }
    }

    public void showLapTitleView(boolean isShow) {
        if (mSeparator != null) {
            if (isShow) {
                mSeparator.setVisibility(View.VISIBLE);
            } else {
                mSeparator.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setStartButtonView(boolean bStart, int stringId) {
        mStartButton.setText(stringId);
    }

    public void setStartButtonImage() {
        mStartButton.setImageResource(R.drawable.icon_btn_stopwatch_light);
    }

    public void setLapButtonEnabled(boolean enabled) {
        mLapButton.setEnabled(enabled);
    }

    public void setResetButtonEnabled(boolean enabled) {
        mResetButton.setEnabled(enabled);
    }

    public void updateImageSrc(long time) {
        if (time < 0) {
            time = 0;
            Log.w(TAG, "updateImageSrc: indexOutOfBound, reset to 0");
        } else if (time >= MAX_SEC) {
            time %= MAX_SEC;
            Log.w(TAG, "updateImageSrc: indexOutOfBound, mod MAX_SEC");
        }

        int minute = (int) time / 600;
        int second = (int) (time % 600) / 10;
        int millisecond = (int) time % 600 % 10;

        int minute10 = minute / 10;
        int minute1 = minute % 10;
        int second10 = second / 10;
        int second1 = second % 10;

        if (mOldMinute10 != minute10) {
            mImageMinute10.setImageResource(mResId[minute10]);
        }
        if (mOldMinute != (minute1)) {
            mImageMinute.setImageResource(mResId[minute1]);
        }
        if (mOldSecond10 != (second10)) {
            mImageSecond10.setImageResource(mResId[second10]);
        }
        if (mOldSecond != (second1)) {
            mImageSecond.setImageResource(mResId[second1]);
        }
        if (mOldMilliSecond != millisecond) {
            mImageMilliSecond.setImageResource(mResId[millisecond]);
        }

        mOldMinute10 = minute10;
        mOldMinute = minute1;
        mOldSecond10 = second10;
        mOldSecond = second1;
        mOldMilliSecond = millisecond;
    }

    public void updateLapImageSrc(long time) {
        if (time < 0) {
            time = 0;
            Log.w(TAG, "updateLapImageSrc: indexOutOfBound, reset to 0");
        } else if (time >= MAX_SEC) {
            time %= MAX_SEC;
            Log.w(TAG, "updateLapImageSrc: indexOutOfBound, mod MAX_SEC");
        }

        int minute = (int) time / 600;
        int second = (int) (time % 600) / 10;
        int millisecond = (int) time % 600 % 10;
        int minute10 = minute / 10;
        int minute1 = minute % 10;
        int second10 = second / 10;
        int second1 = second % 10;

        if (mOldLapMinute10 != minute10) {
            mImageLapMinute10.setImageResource(mResId[minute10]);
        }
        if (mOldLapMinute != minute1) {
            mImageLapMinute.setImageResource(mResId[minute1]);
        }
        if (mOldLapSecond10 != second10) {
            mImageLapSecond10.setImageResource(mResId[second10]);
        }
        if (mOldLapSecond != second1) {
            mImageLapSecond.setImageResource(mResId[second1]);
        }
        if (mOldLapMilliSecond != millisecond) {
            mImageLapMilliSecond.setImageResource(mResId[millisecond]);
        }

        mOldLapMinute10 = minute10;
        mOldLapMinute = minute1;
        mOldLapSecond10 = second10;
        mOldLapSecond = second1;
        mOldLapMilliSecond = millisecond;
    }

    private void dismissHtcAlertDialog() {
        if (mHtcAlertDialog != null) {
            mHtcAlertDialog.dismiss();
            mHtcAlertDialog = null;
        }
    }

    public void showMaxNumErrorAlertDialog() {
        HtcAlertDialog.Builder alertDialogView = new HtcAlertDialog.Builder(mActivity);
        alertDialogView.setTitle(mActivity.getString(R.string.lap_error));
        alertDialogView.setMessage(mActivity.getString(R.string.add_lap_error));
        alertDialogView.setNeutralButton(R.string.ok, null);

        dismissHtcAlertDialog();
        mHtcAlertDialog = alertDialogView.create();
        mHtcAlertDialog.show();
    }

    public static int getTextMeasureDisplay(Context context, int resId) {
        TextView measuredTextDisplay;
        measuredTextDisplay = new TextView(context);
        ViewGroup.LayoutParams lp_param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        measuredTextDisplay.setLayoutParams(lp_param);
        measuredTextDisplay.setTextAppearance(context, R.style.fixed_button_primary_s);
        measuredTextDisplay.setText(resId);
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        measuredTextDisplay.measure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = measuredTextDisplay.getMeasuredHeight();
        return measuredHeight;
    }
    
    public void resetTotalTextMarginTop() {
        int marginTop;
        int m6 = mResource.getDimensionPixelSize(R.dimen.common_dimen_m6);
        
        View pannelView = findViewById(R.id.led_pannel);
        if (pannelView != null) {
            ViewGroup.MarginLayoutParams lp_pannelView = (ViewGroup.MarginLayoutParams)pannelView.getLayoutParams();
            marginTop = lp_pannelView.topMargin - m6 - getTextMeasureDisplay(mActivity, R.string.total_title);
            View textView = findViewById(R.id.total_title);
            if (textView != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)textView.getLayoutParams();
                layoutParams.topMargin = marginTop;
                textView.setLayoutParams(layoutParams);
            }
        }
    }
    
    public void resetLapTextMarginTop() {
        int marginTop;
        int m6 = mResource.getDimensionPixelSize(R.dimen.common_dimen_m6);
        
        View pannelView = findViewById(R.id.led_pannel2);
        if (pannelView != null) {
            ViewGroup.MarginLayoutParams lp_pannelView = (ViewGroup.MarginLayoutParams)pannelView.getLayoutParams();
            marginTop = lp_pannelView.topMargin - m6 - getTextMeasureDisplay(mActivity, R.string.lap_title);    
            View textView = findViewById(R.id.lap_title);
            if (textView != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)textView.getLayoutParams();
                layoutParams.topMargin = marginTop;
                textView.setLayoutParams(layoutParams);
            }
        }
    }
    
    public void resetLayout() {
        if(ResUtils.hasNavigationBar(mActivity)) {
            setLayout(R.id.htclist, 0, 
            		0, R.dimen.stopwatch_list_marginLeft, R.dimen.stopwatch_list_marginTop);
            setLayout(R.id.separator
                    , 0, 0, R.dimen.stopwatch_listheader_marginLeft
                       , R.dimen.stopwatch_listheader_marginTop);
            setLayout(R.id.stopwatch_divider, 0, 0
                    , R.dimen.stopwatch_divider_marginLeft, 0);
            
            setLayout(R.id.led_pannel
                    , R.dimen.led_pannel_width, R.dimen.led_pannel_height
                    , R.dimen.led_pannel_marginLeft, R.dimen.led_pannel_marginTop);
            
            setLayout(R.id.led_pannel2
                    , R.dimen.led_pannel_width, R.dimen.led_pannel_height
                    , R.dimen.led_pannel2_marginLeft, R.dimen.led_pannel2_marginTop);
            
            setLayout(R.id.minute_ten
                    , R.dimen.stopwatch_digit_width, 0
                       , 0, 0);

            setLayout(R.id.minute_unit
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.digit_colon
                    , R.dimen.stopwatch_colon_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.second_ten
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.second_unit
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.dot
                    , R.dimen.stopwatch_dot_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.millisecond
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);
            
            setLayout(R.id.minute_ten2
                    , R.dimen.stopwatch_digit_width, 0
                    , 0, 0);

            setLayout(R.id.minute_unit2
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.digit_colon2
                    , R.dimen.stopwatch_colon_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.second_ten2
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.second_unit2
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.dot2
                    , R.dimen.stopwatch_dot_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);

            setLayout(R.id.millisecond2
                    , R.dimen.stopwatch_digit_width, 0
                    , R.dimen.stopwatch_digit_colon_marginLeft, 0);
        } else {
           setLayout(R.id.htclist, 0, 
            		0, R.dimen.stopwatch_list_no_nevigation_marginLeft, R.dimen.stopwatch_list_marginTop);
           setLayout(R.id.separator
                    , 0, 0, R.dimen.stopwatch_list_no_nevigation_marginLeft
                       , R.dimen.stopwatch_listheader_marginTop);
           setLayout(R.id.stopwatch_divider, 0, 0
                   , R.dimen.stopwatch_divider_no_nevigation_marginLeft, 0);

           setLayout(R.id.led_pannel
                   , R.dimen.led_pannel_no_nevigation_width, R.dimen.led_pannel_no_nevigation_height
                   , R.dimen.led_pannel_marginLeft, R.dimen.led_pannel_marginTop);
           
           setLayout(R.id.led_pannel2
                   , R.dimen.led_pannel_no_nevigation_width, R.dimen.led_pannel_no_nevigation_height
                   , R.dimen.led_pannel2_marginLeft, R.dimen.led_pannel2_marginTop);
           
           setLayout(R.id.minute_ten
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                      , 0, 0);

           setLayout(R.id.minute_unit
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.digit_colon
                   , R.dimen.stopwatch_colon_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.second_ten
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.second_unit
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.dot
                   , R.dimen.stopwatch_dot_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.millisecond
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);
           
           setLayout(R.id.minute_ten2
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                      , 0, 0);

           setLayout(R.id.minute_unit2
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.digit_colon2
                   , R.dimen.stopwatch_colon_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.second_ten2
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.second_unit2
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.dot2
                   , R.dimen.stopwatch_dot_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);

           setLayout(R.id.millisecond2
                   , R.dimen.stopwatch_digit_no_nevigation_width, 0
                   , R.dimen.stopwatch_digit_colon_no_negivation_marginLeft, 0);
         }
        
        resetTotalTextMarginTop();
        resetLapTextMarginTop();
        
        if(mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        	   mDivider.setVisibility(View.GONE);
        } else {
        	   mDivider.setVisibility(View.VISIBLE);
        }
    }
}
