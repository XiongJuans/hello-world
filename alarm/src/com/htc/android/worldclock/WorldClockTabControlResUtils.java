package com.htc.android.worldclock;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.htc.android.worldclock.alarmclock.HandleApiCalls;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.view.viewpager.HtcPagerFragment;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.ActionBarContainer;
import com.htc.lib1.cc.widget.ActionBarDropDown;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.theme.ThemeFileUtil;
import com.htc.lib1.theme.ThemeType;

public class WorldClockTabControlResUtils extends ResUtils {
    private static final String TAG = "WorldClock.WorldClockTabControlResUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private ActionBarExt mActionBarExt = null;
    private ActionBarContainer mActionBarContainer = null;
    private ActionBarDropDown mActionBarDropDown = null;

    private HtcFooter mCarouselFooter;

    private ThemeFileUtil.FileCallback mThemeCallBack = new ThemeFileUtil.FileCallback() {
        @Override
        public void onCompleted(Context context, ThemeFileUtil.ThemeFileTaskInfo result) {
            super.onCompleted(context, result);
            //update status bar and actionbar
            setBackgroundTheme(context,mActionBarExt);
            //update tab bar bg
            if(mActivity instanceof WorldClockTabControl){
                CarouselTab tabFragment = ((WorldClockTabControl)mActivity).getCarouselTab();
                if(tabFragment != null){
                    tabFragment.updateTabBarBg(context.getResources().getConfiguration().orientation);
                }
            }

        }
    };

    public WorldClockTabControlResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        initActionBar();
        setBackgroundTheme(mActivity, mActionBarExt);
    }

    public void initTheme() {
        boolean themeChanged = ThemeFileUtil.isAppliedThemeChanged(mActivity,
                ThemeType.HTC_THEME_CT);
        if (themeChanged) {
            //force recreate theme resources when them changed
            HtcCommonUtil.initTheme(mActivity, HtcCommonUtil.CATEGORYTWO, 0, true, mThemeCallBack);
            //save theme info manually because theme info is not set correctly by common control
            ThemeFileUtil.saveAppliedThemeInfo(mActivity, ThemeType.HTC_THEME_CT);
        } else {
            //not force recreate resource by default
            HtcCommonUtil.initTheme(mActivity, HtcCommonUtil.CATEGORYTWO, mThemeCallBack);
        }
    }

    public void initActionBar() {
        // create and enable htc style action bar
        if (mActionBarExt == null) {
            mActionBarExt = new ActionBarExt(mActivity, mActivity.getActionBar());
        }
        // runtime create and generate module container
        mActionBarContainer = mActionBarExt.getCustomContainer();

        initActionBarDropDown();

        if (mActionBarDropDown != null) {
            mActionBarContainer.addCenterView(mActionBarDropDown);
        }
    }

    public HtcFooter initFooter(HtcPagerFragment host) {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCarouselFooter = (HtcFooter) inflater.inflate(R.layout.common_carousel_footer, null);
        if (mCarouselFooter != null) {
            mCarouselFooter.ReverseLandScapeSequence(true);
            host.setFooter(mCarouselFooter);
            if (Global.isSupportAccChinaSense()) {
                mCarouselFooter.setBackgroundStyleMode(HtcFooter.STYLE_MODE_PURELIGHT);
                if (mResource.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    mCarouselFooter.enableThumbMode(true);
                } else {
                     mCarouselFooter.enableThumbMode(false);
                }
            }
            HtcFooterButton moreButton = (HtcFooterButton) mCarouselFooter.findViewById(R.id.footer_btn1);
            if (moreButton != null) {
                moreButton.setImageResource(R.drawable.icon_btn_menu_light);
                moreButton.setText(R.string.st_more);
            }
        }
        return mCarouselFooter;
    }

    public void initActionBarDropDown() {
        if (mActionBarDropDown == null) {
            mActionBarDropDown = new ActionBarDropDown(mActivity);
            mActionBarDropDown.setArrowEnabled(false);
            mActionBarDropDown.setPrimaryText(R.string.htc_private_app_clock);
        }
    }

    public ActionBarDropDown getActionBarDropDown() {
        return mActionBarDropDown;
    }

    public void setActionBarDropDownSecondaryText(String msg) {
        String label = PreferencesUtil.getTimerLabel(mActivity);
        if (DEBUG_FLAG) Log.d(TAG, "setActionBarDropDownSecondaryText: label = " + label);
        if (msg.isEmpty()) {
            mActionBarDropDown.setSecondaryVisibility(View.GONE);
        } else {
            if (HandleApiCalls.CTS_TEST_TIMER_STRING.equals(label)) {
                mActionBarDropDown.setSecondaryText(msg);
            }
        }
    }

    public ActionBarExt getActionBarExt() {
        return mActionBarExt;
    }

    public ActionBarContainer getActionBarContainer() {
        return mActionBarContainer;
    }

    public void setActionBarAppTitle(int resId) {
        mActionBarDropDown.setPrimaryText(R.string.htc_private_app_clock);
        mActionBarDropDown.setSecondaryText(resId);
    }

    public HtcFooter getCarouselFooter() {
        return mCarouselFooter;
    }

    public void switchTheme(Configuration newConfig) {
        HtcCommonUtil.updateCommonResConfiguration(mActivity);
        switchStatusBarActionBarBkg(newConfig.orientation, mActionBarExt);
    }
}
