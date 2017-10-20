/*
 * Copyright (C) 2008 HTC Inc.
 *
 */
package com.htc.android.worldclock;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.htc.android.worldclock.alarmclock.AlarmClock;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.alarmclock.HandleApiCalls;
import com.htc.android.worldclock.stopwatch.Stopwatch;
import com.htc.android.worldclock.timer.Timer;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.android.worldclock.utils.SettingsReader;
import com.htc.android.worldclock.worldclock.WorldClock;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.ActionBarUtil;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.view.viewpager.HtcPagerAdapter;
import com.htc.lib1.cc.view.viewpager.HtcPagerFragment;
import com.htc.lib1.cc.view.viewpager.HtcTabFragmentPagerAdapter;
import com.htc.lib1.cc.view.viewpager.HtcTabFragmentPagerAdapter.TabSpec;
import com.htc.lib1.cc.view.viewpager.HtcViewPager;
import com.htc.lib1.cc.widget.HtcOverlapLayout;

public class CarouselTab extends HtcPagerFragment {
    private static final String TAG = "WorldClock.CarouselTab";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    final static String AUTHORITY = "com.htc.android.worldclock.TabCarouselProvider";

    public static final String WORLDCLOCK_ACTION = "worldclock_action";
    public static final String EXTRA_LOCATION_SERVICE = "location_service";
    public static final String TAB_WORLDCLOCK = "1";
    public static final String TAB_ALARM = "2";
    public static final String TAB_STOPWATCH = "3";
    public static final String TAB_TIMER = "4";

    private static final String[] mClassName = {
        WorldClock.class.getName(),
        AlarmClock.class.getName(),
        Stopwatch.class.getName(),
        Timer.class.getName()
    };

    private static final int[] mResTitleId = {
        R.string.world_clock_caption
        , R.string.alarm_caption
        , R.string.stopwatch_caption
        , R.string.timer_caption
    };

    private static final boolean[] mRemoveable = {
        false,
        false,
        true,
        true
    };

    private Activity mActivity;

    private MyTabAdapter mAdapter;
    private HtcPagerFragment mHtcPagerFragment;
    private HtcViewPager mPager;
    private Drawable mTextureDrawable;
    private Drawable mColorDrawable;
    
    public static int getCarouselTabCount() {
        return mClassName.length;
    }

    public CarouselTab() {
        if (DEBUG_FLAG) Log.d(TAG, "CarouselTab");
        //custom title will force carousel hint to add padding for content
        if (DEBUG_FLAG) Log.d(TAG, "CarouselTab end");
    }

    @Override
    public void onCreate(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        super.onCreate(sis);
        // ATS Log
        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][press_widget][turning_on]");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreateView");
        return super.onCreateView(inflater, container, sis);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (DEBUG_FLAG) Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        HtcOverlapLayout content = (HtcOverlapLayout)getView();
        if (getActivity().getActionBar() !=null && getActivity().getActionBar().isShowing())
            content.setInsetActionbarTop(true);
        else
            content.setInsetActionbarTop(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (DEBUG_FLAG) Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
        mHtcPagerFragment = this;
        mAdapter = (MyTabAdapter) getAdapter();
        mPager = getPager();

        if (!PreferencesUtil.getLoadSettings(mActivity)) {
            if(DEBUG_FLAG) Log.d(TAG, "onActivityCreated: first launch worldclock");
            loadSettings();
            // first time to check device encryption for off-alarm
            boolean isDeviceEncryptionEnabled = AlertUtils.reflectIsDeviceEncryptionEnabled();
            Log.i(TAG, "isDeviceEncryptionEnabled = " + isDeviceEncryptionEnabled);
            PreferencesUtil.setDeviceEncryption(mActivity, isDeviceEncryptionEnabled);
            AlarmUtils.saveSupportOffAlarmInPreference(mActivity);
        }
        addAllTabs();
        setLaunchTab();
        ((WorldClockTabControl) mActivity).setCarouselTab(this);
        WorldClockTabControlResUtils worldclockTabControlResUtils = ((WorldClockTabControl) mActivity).getResUtilsInstance();
        worldclockTabControlResUtils.initFooter(this);
        if (Global.isSupportAccChinaSense()) {
            getTabBar().setBarHeight(ActionBarUtil.getActionBarHeight(getActivity(), false));
        }
        if (Global.isSupportAccChinaSense()) {
            mTextureDrawable = HtcCommonUtil.getCommonThemeTexture(getActivity(), com.htc.lib1.cc.R.styleable.CommonTexture_android_headerBackground);
        } else {
            mTextureDrawable = HtcCommonUtil.getCommonThemeTexture(getActivity(), com.htc.lib1.cc.R.styleable.CommonTexture_android_panelBackground);
        }
        mColorDrawable = new ColorDrawable(HtcCommonUtil.getCommonThemeColor(getActivity(), com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color));
        switchTabBarbkg(getActivity().getResources().getConfiguration().orientation);
        if (DEBUG_FLAG) Log.d(TAG, "onActivityCreated end");
    }

    @Override
    public void onStart() {
        if (DEBUG_FLAG) Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG_FLAG) Log.d(TAG, "onResume");
        super.onResume();
        if (mAdapter != null) {
            if (DEBUG_FLAG) Log.d(TAG, "onResume: Current Tab = " + mAdapter.getCurrentTabTag());
        }
    }

    @Override
    public void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG_FLAG) Log.d(TAG, "onStop");
        try {
            PreferencesUtil.setDefaultTab(mActivity, Integer.parseInt(mAdapter.getCurrentTabTag()));
        } catch(Exception e) {
            if(DEBUG_FLAG) Log.d(TAG,"onStop: e = " + e.toString());
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private void setLaunchTab() {
        Boolean isLocationEnabled = mActivity.getIntent().getBooleanExtra(CarouselTab.EXTRA_LOCATION_SERVICE, true);
        if (DEBUG_FLAG) Log.d(TAG, "setLaunchTab: isLocationEnabled = " + isLocationEnabled);
        String lastTab = mActivity.getIntent().getStringExtra(WORLDCLOCK_ACTION);
        if (DEBUG_FLAG) Log.d(TAG, "setLaunchTab: lastTab = " + lastTab);
        if (!isLocationEnabled) {
            lastTab = CarouselTab.TAB_WORLDCLOCK;
            mActivity.showDialog(WorldClockTabControl.LOCATION_SERVICE);
        }
        
        if (lastTab != null) {
            int tabInt = Integer.parseInt(lastTab);
            if ((tabInt >= 1) && (tabInt <= mClassName.length)) {
                mPager.setCurrentItem(mAdapter.getPagePosition(lastTab));
                setCurrentTabTag(lastTab);
                mAdapter.setCurrentTabTag(lastTab);
            }
        } else {
            lastTab = String.valueOf(PreferencesUtil.getDefaultTab(mActivity));
            mPager.setCurrentItem(mAdapter.getPagePosition(lastTab));
            mAdapter.setCurrentTabTag(lastTab);
        }
    }

    public void addAllTabs() {
        String seqTab = PreferencesUtil.getSequenceTab(mActivity);
        for (int i = 0; i < mClassName.length; i++) {
            int id = Integer.parseInt(seqTab.substring(i, i + 1));
            int index = id - 1;//remove deskclock
            if (id != 0) {
                TabSpec TabSpec = new TabSpec(mActivity.getResources().getString(mResTitleId[index]));
                TabSpec.setRemovable(mRemoveable[index]);
                mAdapter.addTab(Integer.toString(id), TabSpec);
                if(DEBUG_FLAG) Log.d(TAG, "addAllTabs: add tabs = "+ mClassName[index]);
            }
        }
    }

    private void loadSettings() {
        SettingsReader sr = new SettingsReader(mActivity.getContentResolver());
        String weekday = sr.getStartWeekDay();
        if(DEBUG_FLAG) Log.d(TAG, "loadSettings: start weekday = " + weekday);
        if (weekday != null) {
            try {
                PreferencesUtil.setStartWeekDay(mActivity, Integer.parseInt(weekday));
            } catch (Exception e) {
                Log.w(TAG, "loadSettings: e = " + e.toString());
            }
        }
        PreferencesUtil.setDefaultTab(mActivity, sr.getTabDefault());
        PreferencesUtil.setSequenceTab(mActivity, sr.getTabSequence());
        PreferencesUtil.setLoadSettings(mActivity, true);
        if(DEBUG_FLAG) Log.d(TAG, "loadSettings: set preferences completly");
    }

    public void setCurrentTabTag(String tab) {
        // this.setCurrentTabTag(mActivity.getResources().getString(mResTitleId[Integer.parseInt(tab)]));
        mAdapter.setCurrentTabTag(tab);
    }

    public void enterCarouselEditMode() {
        this.startEditing();
    }

    @Override
    protected HtcPagerAdapter onCreateAdapter(Context arg0) {
        return new MyTabAdapter(this);
    }
    
    public class MyTabAdapter extends HtcTabFragmentPagerAdapter {
        private String currentTab;
        
        public MyTabAdapter(Fragment host) {
            super(host);
        }
                
        public String getCurrentTabTag() {
            return currentTab;
        }
        
        public void setCurrentTabTag(String tag) {
            currentTab = tag;
        }

        @Override
        public Fragment getItem(String tag) {
            if (tag.equals(TAB_WORLDCLOCK)) {
                return new WorldClock();
            } else if (tag.equals(TAB_ALARM)) {
                return new AlarmClock();
            } else if (tag.equals(TAB_STOPWATCH)) {
                return new Stopwatch();
            } else if(tag.equals(TAB_TIMER)) {
                return new Timer();
            }
            return null;
        }
     
        @Override
        public void onTabChanged(String previousTag, String currentTag) {
            currentTab = currentTag;
            WorldClockTabControlResUtils worldclockTabControlResUtils = ((WorldClockTabControl) mActivity).getResUtilsInstance();
            if(DEBUG_FLAG) Log.d(TAG, "onTabChanged: currentTag = " + currentTag);
            if (TAB_WORLDCLOCK.equals(currentTag)) {
                if (!Global.isSupportAccChinaSense()) {
                    mHtcPagerFragment.hideFooter();
                } else {
                    ((WorldClock)(mHtcPagerFragment.getChildFragmentManager().findFragmentByTag(currentTag))).setFooter();
                }
                worldclockTabControlResUtils.setActionBarDropDownSecondaryText("");
            } else if (TAB_ALARM.equals(currentTag)) {
                if (!Global.isSupportAccChinaSense()) {
                    mHtcPagerFragment.hideFooter();
                } else {
                    ((AlarmClock)(mHtcPagerFragment.getChildFragmentManager().findFragmentByTag(currentTag))).setFooter();
                }
                worldclockTabControlResUtils.setActionBarDropDownSecondaryText("");
            } else if (TAB_STOPWATCH.equals(currentTag)) {
                mHtcPagerFragment.showFooter();
                ((Stopwatch)(mHtcPagerFragment.getChildFragmentManager().findFragmentByTag(currentTag))).setFooter();
                worldclockTabControlResUtils.setActionBarDropDownSecondaryText("");
            } else if (TAB_TIMER.equals(currentTag)) {
                mHtcPagerFragment.showFooter();
                ((Timer)(mHtcPagerFragment.getChildFragmentManager().findFragmentByTag(currentTag))).setFooter();
                worldclockTabControlResUtils.setActionBarDropDownSecondaryText(HandleApiCalls.CTS_TEST_TIMER_STRING);
            }
        }

        @Override
        public boolean isCNMode() {
            if (Global.isSupportAccChinaSense()) {
                return true;
            } else {
                return super.isCNMode();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        switchTabBarbkg(newConfig.orientation);
        super.onConfigurationChanged(newConfig);
    }
    
    private void switchTabBarbkg(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT && ResUtils.isDefaultDensity(getActivity())) {
            getTabBar().setBackground(mTextureDrawable);
        } else {
            getTabBar().setBackground(mColorDrawable);
        }
    }

    public void updateTabBarBg(int orientation){
        if (orientation == Configuration.ORIENTATION_PORTRAIT && ResUtils.isDefaultDensity(getActivity())) {
            mTextureDrawable = HtcCommonUtil.getCommonThemeTexture(getActivity(), com.htc.lib1.cc.R.styleable.CommonTexture_android_panelBackground);
            getTabBar().setBackground(mTextureDrawable);
        } else {
            mColorDrawable = new ColorDrawable(HtcCommonUtil.getCommonThemeColor(getActivity(), com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color));
            getTabBar().setBackground(mColorDrawable);
        }
    }
}
