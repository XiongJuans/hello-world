package com.htc.android.worldclock.alarmclock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.MyScrollView;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.support.widget.HtcTintManager;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.ActionBarContainer;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.ActionBarText;
import com.htc.lib1.cc.widget.HtcEditText;
import com.htc.lib1.cc.widget.HtcListItem2LineText;
import com.htc.lib1.cc.widget.HtcListItemLabeledLayout;
import com.htc.lib1.cc.widget.HtcOverlapLayout;
import com.htc.lib1.cc.widget.HtcTimePicker;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;

public class SetAlarmResUtils extends ResUtils {

    private static final String TAG = "WorldClock.SetAlarmResUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private ActionBarExt mActionBarExt = null;
    private ActionBarContainer mActionBarContainer = null;
    private ActionBarText mActionBarText = null;
    private HtcTimePicker mHtcTimePicker;
    private RelativeLayout mHtcTimePickerLayout;
    private InputMethodManager mInputMethodManager;
    private HtcEditText mDescriptionText;
    private HtcListItemLabeledLayout mDescriptionView;
    private HtcListItem2LineText mRingToneView;
    private HtcListItem2LineText mRepeatView;
    private HtcListItem2LineText mVibrateView;
    private HtcListItem2LineText mOffAlarmView;
    private LinearLayout mScrollView;
    private MyScrollView mScrollLayout;
    private CheckBox mVibrateCheckBox;
    private CheckBox mOffAlarmCheckBox;
    private LinearLayout mSetAlarmList;
    private ImageView mDivider;
    private final int SHADOW_COLOR_1ST = 0xff3a3a3a;
    private final int SHADOW_COLOR_2ND = 0x00000000;
    private HtcOverlapLayout mSetAlarmView;

    public SetAlarmResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        initActionBar();
        ResUtils.enableStatusBarTheme(mActivity);
        setBackgroundTheme(mActivity, mActionBarExt);
        mInputMethodManager = (InputMethodManager) mActivity.getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mSetAlarmView = (HtcOverlapLayout) findViewById(R.id.set_alarm_view);
        mHtcTimePickerLayout = (RelativeLayout) findViewById(R.id.timerPicker);
        mScrollView = (LinearLayout) findViewById(R.id.setAlarm_scrollContent);
        mScrollLayout = (MyScrollView) findViewById(R.id.myscrollview);
        mSetAlarmList = (LinearLayout) findViewById(R.id.setAlarm_list);
        mDescriptionView = (HtcListItemLabeledLayout) findViewById(R.id.labeled_layout);
        mDescriptionText = (HtcEditText) findViewById(R.id.edit_description);
        mRingToneView = (HtcListItem2LineText) findViewById(R.id.alarm_ringtone);
        mRepeatView = (HtcListItem2LineText) findViewById(R.id.alarm_repeat);
        mVibrateView = (HtcListItem2LineText) findViewById(R.id.vibrate_title);
        mVibrateCheckBox = (CheckBox) findViewById(R.id.vibrate);
        HtcTintManager htcTintManager = HtcTintManager.get(mActivity);
        htcTintManager.tintThemeColor(mVibrateCheckBox);
        mOffAlarmView = (HtcListItem2LineText) findViewById(R.id.offalarm_title);
        mOffAlarmCheckBox = (CheckBox) findViewById(R.id.offalarm);
        htcTintManager.tintThemeColor(mOffAlarmCheckBox);
        mDivider = (ImageView) findViewById(R.id.set_alarm_divider);

        mScrollView.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        mVibrateView.setSecondaryTextVisibility(View.GONE);
        mOffAlarmView.setSecondaryTextVisibility(View.GONE);

        ((HtcFooter) findViewById(R.id.btn)).ReverseLandScapeSequence(true);
        ((HtcFooterButton) findViewById(R.id.cmd_bar_btn_1)).setText(R.string.done);
        ((HtcFooterButton) findViewById(R.id.cmd_bar_btn_2)).setText(R.string.cancel);

        if (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mScrollView.setOrientation(LinearLayout.VERTICAL);
            mScrollLayout.setVerticalScrollBarEnabled(true);
        }else {
            mScrollView.setOrientation(LinearLayout.HORIZONTAL);
            mScrollLayout.setVerticalScrollBarEnabled(false);
        }

        setItemPrimaryText();
        resetLayout();
    }

    @SuppressLint("NewApi")
    public void initActionBar() {
        // create and enable htc style action bar
        if (mActionBarExt == null) {
            mActionBarExt = new ActionBarExt(mActivity, mActivity.getActionBar());
        }
        
        // runtime create and generate module container
        mActionBarContainer = mActionBarExt.getCustomContainer();

        if (mActionBarText == null) {
            mActionBarText = new ActionBarText(mActivity);
            mActionBarText.setPrimaryText(R.string.htc_private_app_clock);
            mActionBarText.setSecondaryText(R.string.set_alarm_caption);
            mActionBarContainer.addCenterView(mActionBarText);
        }
    }

    public void setItemPrimaryText() {
        ((TextView)(mDescriptionView.getChildAt(0))).setTextAppearance(mActivity, R.style.fixed_separator_primary_l);
        mRingToneView.setPrimaryText(R.string.alarm_ringtone);
        mRingToneView.setPrimaryTextStyle(R.style.HtcListItem2LineTextStyle);
        mRepeatView.setPrimaryText(R.string.alarm_repeat);
        mRepeatView.setPrimaryTextStyle(R.style.HtcListItem2LineTextStyle);
        mVibrateView.setPrimaryText(R.string.alarm_vibrate);
        mVibrateView.setPrimaryTextStyle(R.style.HtcListItem2LineTextStyle);
        mOffAlarmView.setPrimaryText(R.string.alarm_offalarm);
        mOffAlarmView.setPrimaryTextStyle(R.style.HtcListItem2LineTextStyle);
    }

    public void setAlarmDescription(String name) {
        mDescriptionText.setText(name);
    }

    public void setAlarmSoundName(String name) {
        mRingToneView.setSecondaryText(name);
    }

    public void setAlarmRepeat(String name) {
        mRepeatView.setSecondaryText(name);
    }

    public void setAlarmRepeatDescription(String name) {
        if (mRepeatView.getSecondaryTextView() != null) {
            mRepeatView.getSecondaryTextView().setContentDescription(name);
        }
    }

    public void setAlarmVibrateCheckBox(boolean vibrate) {
        mVibrateCheckBox.setChecked(vibrate);
    }

    public void setAlarmOffAlarmCheckBox(boolean offalarm) {
        mOffAlarmCheckBox.setChecked(offalarm);
    }

    @SuppressLint("NewApi")
    public void resetLayout() {
        //if setAlarm do not initial list view complete (get view) , the edit_description may be null.
        if (mDescriptionText != null) {
            mInputMethodManager.hideSoftInputFromWindow(mDescriptionText.getWindowToken(), 0);
        } else {
            if (DEBUG_FLAG) Log.d(TAG, "resetLayout: list view is not created");
        }

        mHtcTimePickerLayout.requestFocus();


        if (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mScrollView.setOrientation(LinearLayout.VERTICAL);
            mScrollLayout.setVerticalScrollBarEnabled(true);
            mDivider.setVisibility(View.GONE);
        }else {
            mScrollView.setOrientation(LinearLayout.HORIZONTAL);
            mScrollLayout.setVerticalScrollBarEnabled(false);
            mDivider.setVisibility(View.VISIBLE);
        }

        setLayout(R.id.timerPicker
            , R.dimen.alarm_timerPicker_width, R.dimen.alarm_timerPicker_layout_height
            , 0, 0);

        if(ResUtils.hasNavigationBar(mActivity)) {
	        setLayout(R.id.timePickerContainer
	                , R.dimen.timePickerContainer_width, R.dimen.timePickerContainer_height
	                , 0, 0);
	        setLayout(R.id.timerPicker
	        		, R.dimen.alarm_timerPicker_width,0
	        		, 0, 0);
	        setLayout(R.id.setAlarm_list
	                , R.dimen.setAlarm_list_width, 0
	                , 0, 0);
        } else {
	        setLayout(R.id.timePickerContainer
	                , R.dimen.timePickerContainer_no_nevigation_width, R.dimen.timePickerContainer_height
	                , 0, 0);
	        setLayout(R.id.timerPicker
	        		, R.dimen.alarm_timerPicker_no_nevigation_width,0
	        		, 0, 0);
	        setLayout(R.id.setAlarm_list
	                , R.dimen.setAlarm_list_no_nevigation_width, 0
	                , 0, 0);
        }
    }
    
    public void switchTheme(Configuration newConfig) {
        HtcCommonUtil.updateCommonResConfiguration(mActivity);
        switchStatusBarActionBarBkg(newConfig.orientation, mActionBarExt);
    }
}
