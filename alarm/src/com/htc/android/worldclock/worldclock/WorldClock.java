/*
 * design by sky - 20080818
 * and tiffanie
 */

package com.htc.android.worldclock.worldclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.TimeZonePicker;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.CarouselTab.MyTabAdapter;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.utils.DigitalClock;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.android.worldclock.utils.ToastMaster;
import com.htc.android.worldclock.utils.WeatherUtils;
import com.htc.android.worldclock.worldclock.CityInfo.LocationColumns;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.res.HtcResUtil;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcListItem;
import com.htc.lib1.cc.widget.HtcListItem1LineCenteredText;
import com.htc.lib1.cc.widget.HtcListItem2LineText;
import com.htc.lib1.cc.widget.HtcListView;
import com.htc.lib1.cc.widget.ListPopupBubbleWindow;
import com.htc.lib2.weather.WeatherConsts;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherUtility;

@SuppressWarnings("deprecation")
public class WorldClock extends Fragment {
    private static final String TAG = "WorldClock.WorldClock";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    public static final String DB_APP_NAME_WORLD_CLOCK_CITY = "com.htc.elroy.Weather";
    public static final String DB_APP_NAME_WORLD_CLOCK_CITY_HOME = "com.htc.android.worldclock.home";

    public static final int HOURS_1 = 60 * 60 * 1000; // milliseconds of 1 hour
    public static final int MINUTE_1 = 60000;

    private static final int LAUNCH_ADD_TIMEZONE = 0;
    private static final int LAUNCH_LOCALTIME_SETTING = 1;
    private static final int LAUNCH_HOME_SETTING = 2;
    private static final int LAUNCH_REARRANGE_TIMEZONE = 3;

    protected static final int DELAY_TIME_CITY_UPDATE = 500;
    protected static final int DELAY_TIME_WATI_ANNIMATION_END = 100;

    public static final String BEIJING_TIMEZONE_CODE = "ASI|CN|CH002|BEIJING";
    public static final String BEIJING_TIMEZONE_CHINA_CODE = "*01010101";
    public static final String HONG_KONG_TIMEZONE_CODE = "ASI|HK|HK---|HONG KONG";
    public static final String HONG_KONG_TIMEZONE_CHINA_CODE = "*01013101";

    // get timezone from network
    public static final String APP_LOCATION_SERVICE = "com.htc.htclocationservice";

    private static final String GMT = "GMT";
    public static final String SEARCH_INTENTION = "search_intention";
    public static final String DELETED_INDEX = "deleted_index";
    public static final int SEARCH_FOR_ADD = 1;
    public static final int SEARCH_FOR_HOME_SETTINGS = 2;

    protected static String CURRENT;
    protected static String HOME;
    protected static String TODAY;
    protected static String TOMORROW;
    protected static String YESTERDAY;
    protected static CharSequence[] MONTH;
    protected static CharSequence[] DAYOFWEEK;

    protected static final int ADD_CITY_ID = 1;
    protected static final int CURRENT_SETTINGS_ID = 2;
    protected static final int HOME_SETTINGS_ID = 3;
    protected static final int REARRANGE_ID = 4;
    protected static final int DELETE_ID = 5;

    public final static int MAX_CITY_COUNT = 15;
    protected int mStationarySize = 0;

    public ArrayList<CityTime> myList;
    public ArrayList<CityTime> tempList;
    protected WorldClockAdapter mWorldClockAdapter;
    private ListPopupBubbleWindow mFooterPopUpWindow;
    private ArrayList<String> mFooterList;
    private FooterAdapter mFooterAdapter;
    protected HtcListView mListView;
    protected int mCurrentDay;
    protected TimeZone mTimeZone;
    protected CityChangeObserver mCityChangeObserver;
    protected FormatChangeObserver mFormatChangeObserver;
    protected ContentResolver mContentResolver;

    protected View mHeaderView;

    protected boolean mIsRegistered = false;
    protected IntentReceiver mIntentReceiver = null;
    protected Handler mMainHandler;
    // thread control
    private Handler mNonUIHandler = null;
    private Looper mNonUILooper = null;
    // UI message
    private final int UI_MSG_CITY = 0x0001;
    private final int UI_MSG_TIMETICK = 0x0002;
    protected final int UI_MSG_SCROLL_TO_POS = 0x0004;
    private final int UI_MSG_DIGITAL_BAKCGROUND_UPDATE = 0x0007;
    // none UI message
    private final int NONUI_MSG_INIT = 0x0100;
    private final int NONUI_MSG_CITY_UPDATE = 0x0200;
    private final int NONUI_MSG_TIMETICK = 0x0300;
    private final int NONUI_MSG_WEATHER_UPDATE = 0x0400;

    private final int WEATHER_UPDATE_DELAYTIME = 2000;
    private final int WEATHER_UPDATE_RETRY_MAX = 3;
    private int mWeatherUpdateCount = 0;
    private boolean mTimeChanged = false;
    private boolean mCityChanged = false;
    private java.util.Timer mMinuteTimer;
    private MinuteTask mMinuteTask;
    private int mSelectedPos = 0;
    private Menu mMenuView;

    private boolean m24HourMode = false;

    WorldClockResUtils mWorldClockResUtils;
    private View.OnClickListener mAddBtnClickListener;
    private View.OnClickListener mEditBtnClickListener;

    private Activity mActivity;
    private View mMainView;
    
    // state control
    private WorldClockState mWorldClockState;

    private CityTime mHomeInDB = null;
    private int mMaxTimeDisplay;
    private int mMaxAmPmDisplay;
    private int mMaxDayDisplay;
    private int mMaxDigitalDisplay;
    
    protected static enum WorldClockEnum {
        INIT, NORMAL_SCREEN_MODE, PAUSE, END;
    }

    private class WorldClockState {
        private WorldClockEnum mState;
        private WorldClockEnum mRestoreState;

        WorldClockState(WorldClockEnum state) {
            this.mState = state;
            changeState(mState);
        }

        public WorldClockEnum getState() {
            return mState;
        }

        public void restoreState() {
            if (mState == WorldClockEnum.PAUSE) {
                mState = mRestoreState;
            }
            if (DEBUG_FLAG) Log.d(TAG, "WorldClockState.restoreState: " + mState.toString());
        }

        public void changeState(WorldClockEnum state) {
            if (DEBUG_FLAG) Log.d(TAG, "WorldClockState.changeState: next State = " + state.toString());
            switch (state) {
                case INIT:
                    break;
                case NORMAL_SCREEN_MODE:
                    mActivity.invalidateOptionsMenu();
                    mWorldClockResUtils.setActionBarDropDownClickAble(false);
                    ((CarouselTab)getParentFragment()).showTabBar();
                    break;
                case PAUSE:
                    if (mState != state) {
                        mRestoreState = mState;
                    }
                case END:
                    break;
                default:
                    Log.w(TAG, "WorldClockState.changeState: NONE support state = " + state.toString());
            }
            this.mState = state;
        }
    }

    protected static enum LoadDataEnum {
        NO_ANIMATION, ADD_ANIMATION, RUNNING_ANIMATION
    }

    // state control
    private LoadDataState mLoadDataState;

    private class LoadDataState {
        private LoadDataEnum mPrevState;
        private LoadDataEnum mRestoreState;

        LoadDataState(LoadDataEnum initState) {
            changeState(initState);
        }

        public LoadDataEnum getState() {
            return mPrevState;
        }

        @SuppressWarnings("unused")
        public LoadDataEnum getRestoreState() {
            return mRestoreState;
        }

        public void changeState(LoadDataEnum currentState) {
            if (DEBUG_FLAG) Log.d(TAG, "LocalDataState.changeState: " + this.mPrevState + " -> " + currentState.toString());
            switch (currentState) {
                case NO_ANIMATION:
                    break;
                case ADD_ANIMATION:
                    break;
                case RUNNING_ANIMATION:
                    break;
                default:
                    Log.w(TAG, "LocalDataState.changeState: No support state = " + currentState.toString());
            }
            this.mPrevState = currentState;
        }
    }

    @Override
    public void onCreate(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][WorldClock][START]");
        super.onCreate(sis);
        setHasOptionsMenu(true);
        initMember();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreateView");
        mMainView = inflater.inflate(R.layout.main_worldclock, container, false);
        return mMainView;
    }

    @Override
    public void onActivityCreated(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(sis);
        mActivity = getActivity();

        mWorldClockResUtils = new WorldClockResUtils(mActivity, mMainView);
        mWorldClockResUtils.initResources();

        Resources res = getResources();
        /* should be final string, but has to read from string.xml */
        CURRENT = res.getString(R.string.current);
        HOME = res.getString(R.string.home);
        TODAY = HtcResUtil.toUpperCase(mActivity, res.getString(R.string.today));
        TOMORROW = HtcResUtil.toUpperCase(mActivity, res.getString(R.string.tomorrow));
        YESTERDAY = HtcResUtil.toUpperCase(mActivity, res.getString(R.string.yesterday));
        MONTH = res.getTextArray(R.array.month_of_year);
        DAYOFWEEK = new CharSequence[8];
        CharSequence[] tempDay = res.getTextArray(R.array.days_of_week);
        for (int i = 0; i <= 5; i++) {
            DAYOFWEEK[i + 2] = tempDay[i];
        }
        DAYOFWEEK[1] = tempDay[6];

        // get system timezone information
        Calendar calNow = Calendar.getInstance();
        mTimeZone = calNow.getTimeZone();

        mContentResolver = mActivity.getContentResolver();

        initHandler();

        initRegister();

        initListener();
        if (Global.isSupportAccChinaSense()) {
            initFooterMoreList();
        }
    }

    private void initFooterMoreList() {
        mFooterList = new ArrayList<String>();
        mFooterList.add(getString(R.string.current_settings));
        mFooterList.add(getString(R.string.home_settings));
        mFooterList.add(getString(R.string.edit_tabs_menu_item));
        if (WorldClockTabControl.isShowMeInstall(mActivity)) {
            mFooterList.add(getString(R.string.tips));
        }
        mFooterAdapter = new FooterAdapter(mFooterList);
        mFooterPopUpWindow = new ListPopupBubbleWindow(mActivity);
        if(mFooterPopUpWindow != null) {
            mFooterPopUpWindow.setAdapter(mFooterAdapter);
            HtcFooterButton moreButton = (HtcFooterButton) mActivity.findViewById(R.id.footer_btn1);
            mFooterPopUpWindow.setAnchorView(moreButton);
            mFooterPopUpWindow.setFocusable(true);
            mFooterPopUpWindow.setOutsideTouchable(true);
            Configuration configuration = mActivity.getResources().getConfiguration();
            mWorldClockResUtils.setPopUpWindowExpand(configuration, mFooterPopUpWindow);
        }
    }
    
    public void setFooter() {
        if (mActivity == null)
            return;

        MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
        if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_WORLDCLOCK)) {
            return;
        }

        mWorldClockResUtils.setHtcFooterButtonResource();
        // set button click listener
        HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
        if (footer != null) {
            footer.findViewById(R.id.footer_btn3).setOnClickListener(mAddBtnClickListener);
            footer.findViewById(R.id.footer_btn2).setOnClickListener(mEditBtnClickListener);
            footer.findViewById(R.id.footer_btn4).setVisibility(View.GONE);
            if (!Global.isSupportAccChinaSense()) {
                footer.findViewById(R.id.footer_btn1).setVisibility(View.GONE);
            }
        }
        if (Global.isSupportAccChinaSense()) {
            updateFooterMoreList();
            if (footer != null) {
                if ((myList == null) || (getDefaultCityCount() <= 0)) {
                    footer.findViewById(R.id.footer_btn2).setEnabled(false);
                } else {
                    footer.findViewById(R.id.footer_btn2).setEnabled(true);
                }
            }
        }
    }

    private void updateFooterMoreList() {
        if (mFooterPopUpWindow != null) {
            HtcFooterButton moreButton = (HtcFooterButton) mActivity.findViewById(R.id.footer_btn1);
            moreButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFooterPopUpWindow.show();
                }
            });
            mFooterPopUpWindow.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mFooterPopUpWindow.dismiss();
                    switch (position) {
                        case 0:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: current_settings");
                            try {
                                Intent intent = new Intent();
                                intent.setAction("android.settings.DATE_SETTINGS");
                                startActivityForResult(intent, LAUNCH_LOCALTIME_SETTING);
                            } catch (Exception e) {
                                Log.w(TAG, "updateFooterMoreList: CURRENT_SETTINGS_ID fail e = " + e.toString());
                            }
                            break;
                        case 1:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: home_settings");
                            Intent intent = new Intent();
                            intent.setClass(mActivity, TimeZonePicker.class);
                            intent.putExtra(SEARCH_INTENTION, SEARCH_FOR_HOME_SETTINGS);
                            startActivityForResult(intent, LAUNCH_HOME_SETTING);
                            break;
                        case 2:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: edit_tabs");
                            ((CarouselTab)getParentFragment()).enterCarouselEditMode();
                            break;
                        case 3:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: tips & help");
                            WorldClockTabControl.launchShowme(mActivity);
                            break;
                        default:
                            break;
                    }
                }
            });
        }
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
        /* Register for time ticks and other reasons for time change */
        if (mIntentReceiver == null) {
            mIntentReceiver = new IntentReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            mActivity.registerReceiver(mIntentReceiver, filter, Global.PERMISSION_APP_DEFAULT, null);
        }

        try {
            mMinuteTimer = new java.util.Timer();
            mMinuteTask = new MinuteTask();
            mMinuteTimer.schedule(mMinuteTask, MINUTE_1 - (System.currentTimeMillis() % MINUTE_1), MINUTE_1);
        } catch (Exception e) {
            Log.w(TAG, "onResume: Initialiaze timer fail e = " + e.toString());
        }

        mTimeChanged = true; // must update time
        mWorldClockState.restoreState();

        if (mWorldClockState.getState() == WorldClockEnum.INIT) {
            setCurrentDay();
            if (((WorldClockTabControl) mActivity).isWorldClockCachedDataReady()) {
                if (DEBUG_FLAG) Log.d(TAG, "onResume: use preference to load city data");
                if (myList == null) {
                    myList = new ArrayList<CityTime>();
                } else {
                    myList.clear();
                }

                mStationarySize = 0;
                CityTime currentCityTime = ((WorldClockTabControl) mActivity).getCachedCityTimeCurrent();
                CityTime homeCityTime = ((WorldClockTabControl) mActivity).getCachedCityTimeHome();
                if (currentCityTime != null) {
                    myList.add(currentCityTime);
                    mStationarySize++;
                }
                if (homeCityTime != null) {
                    myList.add(homeCityTime);
                    mStationarySize++;
                }
                if (Global.isSupportExchangeCurrentHomePosition()) {
                    exchangeCurrentHomeTime();
                }
                CityTime[] cityTimeCities = ((WorldClockTabControl) mActivity).getCachedCityTimeCities();
                int size = cityTimeCities.length;
                for (int i = 0; i < size; i++) {
                    myList.add(cityTimeCities[i]);
                }
                if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][WorldClock][DATA_READY]");
                initList();
                mWorldClockState.changeState(WorldClockEnum.NORMAL_SCREEN_MODE);
            } else {
                initList();
                mNonUIHandler.sendEmptyMessage(NONUI_MSG_INIT);
            }
        } else {
            checkCityAndTimeChange();
        }
        if (Global.isSupportAccChinaSense()) {
            setFooter();
        }
    }

    private void initList() {
        mListView = (HtcListView) mMainView.findViewById(android.R.id.list);
        mWorldClockAdapter = new WorldClockAdapter(myList);

        mListView.setAdapter(mWorldClockAdapter);
        mListView.setChoiceMode(HtcListView.CHOICE_MODE_SINGLE);
        mListView.setItemChecked(0, true);
        mListView.setSelector(android.R.color.transparent);
    }

    @Override
    public void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        mWorldClockState.changeState(WorldClockEnum.PAUSE);
        mLoadDataState.changeState(LoadDataEnum.NO_ANIMATION);

        ToastMaster.cancelToast();

        if (mIntentReceiver != null) {
            mActivity.unregisterReceiver(mIntentReceiver);
            mIntentReceiver = null;
        }

        try {
            mMinuteTask.cancel();
        } catch (Exception e) {
        }
        mMinuteTask = null;

        try {
            mMinuteTimer.cancel();
        } catch (Exception e) {
        }
        mMinuteTimer = null;
        mActivity.closeOptionsMenu();

        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG_FLAG) Log.d(TAG, "onStop");
        mWorldClockState.changeState(WorldClockEnum.PAUSE);
        mNonUIHandler.removeMessages(NONUI_MSG_WEATHER_UPDATE);
        unInitRegister();
        mCityChanged = true; // for next run while resume
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
        mWorldClockState.changeState(WorldClockEnum.END);
        mMainHandler.removeMessages(UI_MSG_CITY);
        mMainHandler.removeMessages(UI_MSG_TIMETICK);
        mMainHandler.removeMessages(UI_MSG_SCROLL_TO_POS);

        mNonUIHandler.removeMessages(NONUI_MSG_INIT);
        mNonUIHandler.removeMessages(NONUI_MSG_CITY_UPDATE);
        mNonUIHandler.removeMessages(NONUI_MSG_TIMETICK);
        mNonUIHandler.removeMessages(NONUI_MSG_WEATHER_UPDATE);
        if (mNonUILooper != null) {
            mNonUILooper.quit();

            Thread theThread = mNonUILooper.getThread();
            if (theThread != null) {
                try {
                    theThread.interrupt();
                    theThread.join();
                    theThread = null;
                } catch (Exception e) {
                    Log.w(TAG, "onDestroy: theThread Exception e = " + e.toString());
                }
            }
        }

        unInitRegister(); // to prevent in low memory,onStop never called.

        mContentResolver = null;
        mCityChangeObserver = null;
        mFormatChangeObserver = null;

        if (mWorldClockAdapter != null) {
            mWorldClockAdapter.onDestroy();
        }
        mTimeZone = null;
        if (mListView != null) {
            mListView.setAdapter(null);
        }
        if (myList != null) {
            myList.clear();
        }

        super.onDestroy();
    }

    private void initHandler() {

        HandlerThread theHandlerThread = new HandlerThread("WorldClock", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        theHandlerThread.start();
        mNonUILooper = theHandlerThread.getLooper();
        mNonUIHandler = new Handler(mNonUILooper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case NONUI_MSG_INIT:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_INIT");
                        getAllDisplayTimeZone();
                        mMainHandler.sendEmptyMessage(UI_MSG_CITY);
                        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][WorldClock][DATA_READY]");
                        mActivity.invalidateOptionsMenu();
                        break;
                    case NONUI_MSG_TIMETICK:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_TIMETICK");
                        updateTimeTick();
                        break;
                    case NONUI_MSG_CITY_UPDATE:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_CITY_UPDATE");
                        onCityUpdate();
                        mActivity.invalidateOptionsMenu();
                        break;
                    case NONUI_MSG_WEATHER_UPDATE:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_WEATHER_UPDATE");
                        updateWeatherData();
                        break;
                }
            }
        };
    }

    private void initRegister() {

        // register city changed
        mCityChangeObserver = new CityChangeObserver(mNonUIHandler);
        if ((mContentResolver != null) && (mCityChangeObserver != null)) {
            mContentResolver.registerContentObserver(WeatherConsts.CONTENT_URI,
                true, mCityChangeObserver);
            mContentResolver.registerContentObserver(Settings.System.CONTENT_URI,
                true, mCityChangeObserver);
            // register time zone changed from network
            Uri uri = Uri.withAppendedPath(WeatherConsts.CONTENT_URI, WeatherConsts.LOCATION_PATH);
            uri = Uri.withAppendedPath(uri, APP_LOCATION_SERVICE);
            mContentResolver.registerContentObserver(uri,
                true, mCityChangeObserver);
        }
        /* monitor 12/24-hour display preference */
        mFormatChangeObserver = new FormatChangeObserver();

        if ((mContentResolver != null) && (mFormatChangeObserver != null)) {
            mContentResolver.registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }
        mIsRegistered = true;
    }

    private void onCityUpdate() {

        if (mWorldClockState.getState() == WorldClockEnum.PAUSE) {
            mCityChanged = true;
            return;
        }

        // get time zone data
        getAllDisplayTimeZone();
        setCurrentDay();
        mMainHandler.sendEmptyMessage(UI_MSG_CITY);
    }

    private void updateTimeTick() {

        if (mWorldClockState.getState() == WorldClockEnum.PAUSE) {
            mTimeChanged = true;
            return;
        }

        setCurrentDay();

        mMainHandler.sendEmptyMessage(UI_MSG_TIMETICK);
    }

    private void initMember() {
        mWorldClockState = new WorldClockState(WorldClockEnum.INIT);
        mLoadDataState = new LoadDataState(LoadDataEnum.NO_ANIMATION);
        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case UI_MSG_DIGITAL_BAKCGROUND_UPDATE:
                        if(mWorldClockAdapter != null) {
                            if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_DIGITAL_BAKCGROUND_UPDATE");
                            mWorldClockAdapter.changeList(myList);
                            mWorldClockAdapter.notifyDataSetChanged();
                        }
                        break;
                    case UI_MSG_CITY:
                        if (mWorldClockState.getState() == WorldClockEnum.INIT) {
                            mWorldClockState.changeState(WorldClockEnum.NORMAL_SCREEN_MODE);
                        }
                        if (mWorldClockState.getState() == WorldClockEnum.PAUSE) {
                            mCityChanged = true; // for next turn;
                            mLoadDataState.changeState(LoadDataEnum.NO_ANIMATION);
                            return;
                        }

                        if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_CITY");
                        if (mWorldClockAdapter != null) {

                            if ((mLoadDataState.getState() == LoadDataEnum.ADD_ANIMATION)
                                && (mListView.getCount() == 0)) {
                                //raymond: no city item
                                if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_CITY: No City Item case");
                                int position = myList.size() - 1;
                                ArrayList<Integer> addedItemList = new ArrayList<Integer>();
                                addedItemList.add(position);
                                mWorldClockAdapter.changeList(myList);
                                mWorldClockAdapter.notifyDataSetChanged();
                                mListView.setItemChecked(mSelectedPos, true);
                                mLoadDataState.changeState(LoadDataEnum.NO_ANIMATION);
                            } else if ((mLoadDataState.getState() == LoadDataEnum.ADD_ANIMATION)
                                && (myList.size() != mListView.getCount())) {
                                if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_CITY: Add City");
                                mSelectedPos = myList.size() - 1;
                                mWorldClockAdapter.changeList(myList);
                                mWorldClockAdapter.notifyDataSetChanged();
                                mListView.setItemChecked(mSelectedPos, true);
                                mListView.smoothScrollToPosition(mSelectedPos);
                                mLoadDataState.changeState(LoadDataEnum.NO_ANIMATION);
                            } else if (mLoadDataState.getState() == LoadDataEnum.RUNNING_ANIMATION) {
                                if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_CITY: Run Animation");
                                // update city will trigger messeage update
                                mNonUIHandler.sendEmptyMessageDelayed(UI_MSG_CITY, DELAY_TIME_WATI_ANNIMATION_END);
                            }
                        }
                        break;
                    case UI_MSG_TIMETICK:

                        if (mWorldClockState.getState() == WorldClockEnum.PAUSE) {
                            mLoadDataState.changeState(LoadDataEnum.NO_ANIMATION);
                            mTimeChanged = true; // for next turn;
                            return;
                        }

                        if (mLoadDataState.getState() == LoadDataEnum.RUNNING_ANIMATION) {
                            // update time will trigger messeage update
                            mNonUIHandler.sendEmptyMessageDelayed(UI_MSG_TIMETICK,
                                DELAY_TIME_WATI_ANNIMATION_END);
                        }

                        if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_TIMETICK");
                        if (mWorldClockAdapter != null) {
                            calculateDigitalMaxDisplay();
                            mWorldClockAdapter.notifyDataSetChanged();
                        }
                        break;
                    case UI_MSG_SCROLL_TO_POS:
                        if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_SCROLL_TO_POS");
                        int count = mWorldClockAdapter.getCount();
                        for (int i = 0; i < count; i++) {
                            CityTime ct = (CityTime) mWorldClockAdapter.getItem(i);
                        }
                        break;
                }

            }
        };
        
        if (Global.isSupportAccChinaSense()) {
            mAddBtnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ((myList == null) || (getDefaultCityCount() >= MAX_CITY_COUNT)) {
                        mActivity.showDialog(WorldClockTabControl.WORLDCLOCK_REQUEST_ADD);
                    } else {
                        Intent intent = new Intent();
                        intent.setClass(mActivity, TimeZonePicker.class);
                        intent.putExtra(SEARCH_INTENTION, SEARCH_FOR_ADD);
                        startActivityForResult(intent, LAUNCH_ADD_TIMEZONE);
                    }
                }
            };
            
           mEditBtnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClass(mActivity, RearrangeTimeZone.class);
                    startActivityForResult(intent, LAUNCH_REARRANGE_TIMEZONE);
                }
            };
        }
    }

    public void updateCityAdapter() {

        if (myList == null) {
            return;
        }

        if (DEBUG_FLAG) Log.i(TAG, "update action bar city adapter");

        ArrayList<String> cityNameList = new ArrayList<String>();
        for (int i = 0; i < myList.size(); i++) {
            CityTime ct = myList.get(i);

            if(ct != null) {
                String cityName =  (ct.getCityName().equals(CURRENT) || ct.getCityName().equals(HOME)) ? ct.getCityId() : ct.getCityName();
                cityNameList.add(cityName);
            }
        }

        setActionCityName(mListView.getCheckedItemPosition());

    }

    private void setActionCityName(int checkedIndex) {

        if (checkedIndex == ListView.INVALID_POSITION) {
            checkedIndex = 0;
        }

        CityTime ct = (CityTime) mWorldClockAdapter.getItem(checkedIndex);
        String checkedCityName;
        if (ct != null) {
            if (ct.getCityName().equals(CURRENT)) {
                mWorldClockResUtils.addLocIcon();
                checkedCityName = ct.getCityId();
            } else if (ct.getCityName().equals(HOME)) {
                mWorldClockResUtils.removeIcon();
                checkedCityName = ct.getCityId();
            } else {
                mWorldClockResUtils.removeIcon();
                checkedCityName = ct.getCityName();
            }
        }
    }

    /**
     * Load all display information from db (WeatherProvider)
     */
    protected void getAllDisplayTimeZone() {
        /* clear previous city list */
        if (myList != null) {
            tempList = (ArrayList<CityTime>) myList.clone();
            myList.clear();
        } else {
            myList = new ArrayList<CityTime>();
        }

        setCurrentHomeTime();

        if (Global.isSupportExchangeCurrentHomePosition()) {
            exchangeCurrentHomeTime();
        }

        WeatherLocation[] w = WeatherUtility.loadLocations(mActivity.getContentResolver(), WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY);
        if (w != null) {
            int length = w.length;
            boolean needSaveLocationsFlag = false;

            String id = null;
            String name = null;
            String code = null;
            TimeZone tz;
            Set<String> codeSet = new HashSet<String>();
            CityTime CityTimeHome = mHomeInDB;
            String homeCityCode = "";
            if (CityTimeHome != null) {
                homeCityCode = CityTimeHome.getLocCode();
            }

            PreferencesUtil.setDupHomeDefaultCity(mActivity, false);
            for (int i = 0; i < length; i++) {
                code = w[i].getCode();
                if (!codeSet.contains(code)) {
                    if (homeCityCode.equals(code) != true) {
                        id = w[i].getTimezoneId();
                        String longitude = w[i].getLongitude();
                        String latitude = w[i].getLatitude();
                        if ("".equals(longitude) || "".equals(latitude)) {
                            if (DEBUG_FLAG) Log.d(TAG, "getAllDisplayTimeZone: add latitude and longitude to DB");
                            WeatherUtils.setLongLatitude(mActivity, w[i]);
                            if (longitude.equals("") || latitude.equals("")) {
                                if (Global.SECURITY_FLAG) Log.w(TAG, "getAllDisplayTimeZone: dont find city info, city code = " + code);
                            } else {
                                needSaveLocationsFlag = true;
                            }
                        }
                        name = w[i].getName();
                        tz = TimeZone.getTimeZone(id);
                        addCityName(tz, id, name, w[i]);
                        codeSet.add(code);
                    } else {
                        PreferencesUtil.setDupHomeDefaultCity(mActivity, true);
                    }
                }
            }
            if (needSaveLocationsFlag) {
                ContentResolver cr = mActivity.getContentResolver();
                WeatherUtility.saveLocations(cr, WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY, w);
            }
        }

        if (myList != null) {
            int size = myList.size();
            if (DEBUG_FLAG) Log.d(TAG, "getAllDisplayTimeZone: mylist.size = " + size);
            CityTime[] ctArray = new CityTime[size - mStationarySize];
            for (int i = mStationarySize; i < size; i++) {
                ctArray[i - mStationarySize] = myList.get(i);
            }
            int listLen = myList.size();
            //for rearrange cities case, reset position if city position changed
            mSelectedPos = 0;
        }
        
        mWeatherUpdateCount = 0;
        mNonUIHandler.sendMessage(mNonUIHandler.obtainMessage(NONUI_MSG_WEATHER_UPDATE));
    }

    private void exchangeCurrentHomeTime() {
        if (myList != null) {
            if ((mStationarySize == 2) && (myList.size() == 2)) {
                CityTime currentCityTime = myList.remove(0);
                CityTime homeCityTime = myList.remove(0);
                myList.add(homeCityTime);
                myList.add(currentCityTime);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateWeatherData() {
        boolean needUpdateFlag = true;
        if (mWeatherUpdateCount < WEATHER_UPDATE_RETRY_MAX) {
            if (myList == null) {
                return;
            }

            int size = myList.size();
            String timeZone;
            boolean isGetCurrent = false;

            for (int i = 0; i < size; i++) {
                if (myList == null) {
                    return;
                }
                CityTime ct = myList.get(i);
                boolean isGetWeatherData = false;
                timeZone = ct.getWeatherLocation().getTimezoneId();
                if (Global.SECURITY_FLAG) Log.d(TAG, "updateWeatherData: CityName = " + ct.getCityName());
                if (!ct.getCityName().equals(CURRENT) && !ct.getLocCode().equals("")) {
                    isGetWeatherData = WeatherUtils.getWeatherDataByLocationCode(mActivity, ct.getLocCode()
                        , timeZone);
                } else if (ct.getCityName().equals(CURRENT)) {
                    isGetWeatherData = WeatherUtils.getWeatherDataByCurrentLocation(mActivity, timeZone);
                    isGetCurrent = true;
                }
                else {
                    isGetWeatherData = WeatherUtils.getWeatherDataByLongLatitude(mActivity, Float.toString(ct.getLongitude())
                        , Float.toString(ct.getLatitude()), timeZone);
                }

                if (isGetWeatherData) {
                    ct.setWeatherDayNightInfo(WeatherUtils.mWeatherDayTime, WeatherUtils.mWeatherNightTime);
                } else {
                    needUpdateFlag = false;
                }
            }

            if(needUpdateFlag == true){

                if(isGetCurrent) {
                    PreferencesUtil.setCityTimeCurrent(mActivity, myList.get(0));
                }
                if(mStationarySize == 2 || (!isGetCurrent && mStationarySize == 1)) {
                    PreferencesUtil.setCityTimeHome(mActivity, myList.get(mStationarySize-1));
                }

                CityTime[] ctArray = new CityTime[size - mStationarySize];
                for (int i = mStationarySize; i < size; i++) {
                    ctArray[i - mStationarySize] = myList.get(i);
                }
                PreferencesUtil.setCityTimeCities(mActivity, ctArray);
                PreferencesUtil.setSyncWorldClockDB(mActivity, true);
            }

            mWeatherUpdateCount++;
        }

        mMainHandler.sendEmptyMessage(UI_MSG_DIGITAL_BAKCGROUND_UPDATE);
        
        if ((needUpdateFlag == false) && (mWeatherUpdateCount < WEATHER_UPDATE_RETRY_MAX)) {
            //update again
            if (DEBUG_FLAG) Log.d(TAG, "updateWeatherData: mWeatherUpdateCount = " + mWeatherUpdateCount);
            mNonUIHandler.sendMessageDelayed(Message.obtain(mNonUIHandler, NONUI_MSG_WEATHER_UPDATE), WEATHER_UPDATE_DELAYTIME * (mWeatherUpdateCount + 1));
        } else {
            mWeatherUpdateCount = 0;
        }
    }

    /**
     * Set current and home time zone
     */
    protected void setCurrentHomeTime() {

        // get system timezone information
        Calendar calNow = Calendar.getInstance();
        mTimeZone = calNow.getTimeZone();
        mCurrentDay = calNow.get(Calendar.DAY_OF_WEEK);
        String systemTimeZoneId = mTimeZone.getID();
        String systemTimeZoneCityName = queryTimeZoneName(mActivity.getBaseContext(), systemTimeZoneId);

        // to check current city
        WeatherLocation[] currentLoc = WeatherUtility.loadLocations(mActivity.getContentResolver(), APP_LOCATION_SERVICE);
        String currentCityName = "";
        String currentTimeZoneId = "";

        if (isUseWirelessNetworks(mActivity.getBaseContext())) {
            if (DEBUG_FLAG) Log.d(TAG, "setCurrentHomeTime: isUseWirelessNetworks = true");
            if ((currentLoc != null) && (currentLoc.length != 0)) {
                // to check city name: google maybe return null city name
                currentCityName = currentLoc[0].getName();
                if (!TextUtils.isEmpty(currentCityName)) {
                    // if google return city name is not exist in weather DB, WeatherLocation will
                    // return timezone id = null
                    // for this case, we will use systemTimeZoneId
                    currentTimeZoneId = currentLoc[0].getTimezoneId();
                    if (TextUtils.isEmpty(currentTimeZoneId)) {
                        if (Global.SECURITY_FLAG) Log.d(TAG, "setCurrentHomeTime: cityName is not exist in DB, currentCityName = " + currentCityName);
                        currentTimeZoneId = systemTimeZoneId;
                    }
                }
            }

            // for this case, location is enable but don't get currentCityName from WeatherUtility
            // use system timezone to set current city name
            if (TextUtils.isEmpty(currentCityName)) {
                if (DEBUG_FLAG) Log.d(TAG, "setCurrentHomeTime: no current data");
                currentTimeZoneId = systemTimeZoneId;
                currentCityName = systemTimeZoneCityName;
            }
        }

        // to check home city
        WeatherLocation[] homeLoc = WeatherUtility.loadLocations(mActivity.getContentResolver(), WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY_HOME);
        String homeCityName = "";
        String homeTimeZoneId = "";

        if ((homeLoc != null) && (homeLoc.length > 0)) {
            homeTimeZoneId = homeLoc[0].getTimezoneId();
            homeCityName = homeLoc[0].getName();

            //FOTA case: Home city is time zone(no latitude and longitude)  or city before sense4.5.
            if (homeLoc[0].getLatitude().equals("") || homeLoc[0].getLongitude().equals("")) {
                if (homeLoc[0].getCode().equals("")) {
                    Log.w(TAG, "setCurrentHomeTime: dont find city code and reset default city code");
                    String cityCode = queryTimeZoneCityCode(mActivity, homeTimeZoneId);
                    if (TextUtils.isEmpty(cityCode) == true) {
                        homeTimeZoneId = "Asia/Taipei";
                        cityCode = queryTimeZoneCityCode(mActivity, homeTimeZoneId);
                    }

                    homeLoc[0].setCode(cityCode);
                }
                WeatherUtils.setLongLatitude(mActivity.getBaseContext(), homeLoc[0]);
            }
        }

        // set current city and home city to listview
        if (Global.SECURITY_FLAG) Log.d(TAG, "setCurrentHomeTime: currentTimeZoneId = " + currentTimeZoneId);
        if (Global.SECURITY_FLAG) Log.d(TAG, "setCurrentHomeTime: currentCityName = " + currentCityName);
        if (DEBUG_FLAG) Log.d(TAG, "setCurrentHomeTime: homeTimeZoneId = " + homeTimeZoneId);
        if (DEBUG_FLAG) Log.d(TAG, "setCurrentHomeTime: homeCityName = " + homeCityName);

        mStationarySize = 0;

        if ((currentLoc != null) && (currentLoc.length > 0) && !TextUtils.isEmpty(currentCityName)) {
            addCityName(TimeZone.getTimeZone(currentTimeZoneId),
                currentCityName, CURRENT, currentLoc[0]);
            mStationarySize++;
        }

        if ((homeLoc != null) && (homeLoc.length > 0) && !TextUtils.isEmpty(homeCityName)) {
            setHomeTime(TimeZone.getTimeZone(homeTimeZoneId), homeCityName,
                homeLoc[0]);
            mStationarySize++;
        }
    }

    private String getTimeZoneName(TimeZone tz) {

        boolean daylight = tz.inDaylightTime(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append(formatOffset(tz.getRawOffset()
            + (daylight ? tz.getDSTSavings() : 0)));
        return sb.toString();
    }

    private char[] formatOffset(int off) {

        off = off / 1000 / 60;

        char[] buf = new char[9];
        buf[0] = 'G';
        buf[1] = 'M';
        buf[2] = 'T';

        if (off < 0) {
            buf[3] = '-';
            off = -off;
        } else {
            buf[3] = '+';
        }

        int hours = off / 60;
        int minutes = off % 60;

        buf[4] = (char) ('0' + (hours / 10));
        buf[5] = (char) ('0' + (hours % 10));

        buf[6] = ':';

        buf[7] = (char) ('0' + (minutes / 10));
        buf[8] = (char) ('0' + (minutes % 10));

        return buf;
    }

    /**
     * Query display name for current time zone based on Locale
     *
     * @param timezoneId
     * @return
     */
    public static String queryTimeZoneName(Context context, String timezoneId) {

        String where = LocationColumns.TIMEZONEID + "='" + timezoneId + "'";
        Locale systemLocale = context.getResources().getConfiguration().locale;
        String systemLang = systemLocale.getLanguage();
        String systemCountry = systemLocale.getCountry();
        if ("zh".equals(systemLang)) {
            if ("CN".equals(systemCountry)) {
                systemLang = "zh";
            } else if ("TW".equals(systemCountry)) {
                systemLang = "zhTW";
            } else if ("HK".equals(systemCountry)) {
                systemLang = "zhTW";
            } else if ("SG".equals(systemCountry)) {
                systemLang = "zh";
            }
        }

        Cursor c = null;

        try {
            c = context.getContentResolver().query(LocationColumns.CONTENT_URI, null,
                where, null, null);
        } catch (Exception e) {
            Log.w(TAG, "queryTimeZoneName: fail e = " + e.toString());
        }

        String timeZoneName = null;
        if (c != null) {
            if ((c.getCount() > 0) && c.moveToFirst()) {
                int index = c.getColumnIndex(systemLang);
                index = (index < 0) ? c.getColumnIndex(LocationColumns.EN) : index;

                if (index >= 0) {
                    timeZoneName = c.getString(index);
                } else {

                }
            }

            if (c.isClosed() == false) {
                c.close();
            }
        }

        // for special case, timezoneId = GMT or timezoneId wasn't exist in DB(ex NITZ case)
        if (timeZoneName == null) {
            TimeZone tz = TimeZone.getTimeZone(timezoneId);
            timeZoneName = tz.getDisplayName();
        }

        return timeZoneName;
    }

    public static String queryTimeZoneCityCode(Context context, String timezoneId) {
        if (Global.SECURITY_FLAG) Log.d(TAG, "queryTimeZoneCityCode: timezoneId = " + timezoneId);
        String where = LocationColumns.TIMEZONEID + "='" + timezoneId + "'";
        Cursor cursor = null;
        String timeZoneCityCode = "";
        try {
            cursor = context.getContentResolver().query(LocationColumns.CONTENT_URI, null, where, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    int index = cursor.getColumnIndex(LocationColumns.CODE);

                    if (index >= 0) {
                        timeZoneCityCode = cursor.getString(index);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "queryTimeZoneCityCode: fail e = " + e.toString());
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }
        
        if (Global.isSupportBeijingDefaultCityCode()) {
            if (timeZoneCityCode.equals(WorldClock.BEIJING_TIMEZONE_CODE)) {
                timeZoneCityCode = WorldClock.BEIJING_TIMEZONE_CHINA_CODE;
            } else if (timeZoneCityCode.equals(WorldClock.HONG_KONG_TIMEZONE_CODE)) {
                timeZoneCityCode = WorldClock.HONG_KONG_TIMEZONE_CHINA_CODE;
            }
            if (Global.SECURITY_FLAG) Log.d(TAG, "queryTimeZoneCityCode: timeZoneCityCode = " + timeZoneCityCode);
         }
        
        return timeZoneCityCode;
    }

    /**
     *
     * @param tz
     * @param name
     */
    protected void setHomeTime(TimeZone tz, String cityName, WeatherLocation w) {

        if (cityName.equals(GMT)) {
            // show Settings' time zone
            cityName = getTimeZoneName(tz);
        }

        addCityName(tz, cityName, HOME, w);
    }

    /**
     * Add a city to be shown on the list
     *
     * @param c
     * @param tz
     * @param id
     * @param w
     * @param position
     */
    private void addCityName(TimeZone tz, String id, String name, WeatherLocation w) {

        CityTime ct = new CityTime();
        ct.setTimeZone(tz);
        ct.setCityId(id);
        ct.setCityName(name);
        ct.setWeatherLocation(w);

        if(tempList != null) {
            for(int i = 0 ; i < tempList.size() ; i++) {
                CityTime tempCity = tempList.get(i);
                if(tempCity.getCityName().equals(name)) {
                    ct.setWeatherDayNightInt(tempCity.getWeatherDayTime(), tempCity.getWeatherNightTime());
                }
            }
        }
        if (myList != null) {
            myList.add(ct);
        }
        
        if(name == HOME) {
        	mHomeInDB = ct;
        }
    }

    private void calculateDigitalMaxDisplay() {
        mMaxTimeDisplay = DigitalClock.getWorldClockMaxTimeDisplay(mActivity, myList);
        mMaxAmPmDisplay = DigitalClock.getMaxAMPMDisplay(mActivity);
        mMaxDayDisplay = DigitalClock.getWorldClockMaxDayDisplay(mActivity, myList);
        mMaxDigitalDisplay = DigitalClock.getMaxDigitalDisplay(mActivity, mMaxTimeDisplay, mMaxAmPmDisplay);
    }
    
    class WorldClockAdapter extends BaseAdapter {
        protected ArrayList<CityTime> mItems = null;
        protected LayoutInflater mInflater;
        protected LinearLayout mLayout;
        protected TextView mZoneInfo;

        public WorldClockAdapter(ArrayList<CityTime> list) {
            if (list != null) {
                mItems = new ArrayList<CityTime>(list);
            }
            mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            calculateDigitalMaxDisplay();
            if (Global.isSupportAccChinaSense()) {
                MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
                if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_WORLDCLOCK)) {
                    return;
                }
                HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
                if ((mItems == null) || (getDefaultCityCount() <= 0)) {
                    footer.findViewById(R.id.footer_btn2).setEnabled(false);
                } else {
                    footer.findViewById(R.id.footer_btn2).setEnabled(true);
                }
            }
        }

        public void changeList(ArrayList<CityTime> list) {
            if (list != null) {
                mItems = new ArrayList<CityTime>(list);
            }
            calculateDigitalMaxDisplay();
            if (Global.isSupportAccChinaSense()) {
                MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
                if (adapter != null) {
                    if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_WORLDCLOCK)) {
                        return;
                    }
                    HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
                    if (footer != null) {
                        if ((mItems == null) || (getDefaultCityCount() <= 0)) {
                            footer.findViewById(R.id.footer_btn2).setEnabled(false);
                        } else {
                            footer.findViewById(R.id.footer_btn2).setEnabled(true);
                        }
                    }
                }
            }
        }

        @Override
        public int getCount() {
            if (mItems == null) {
                return 0;
            }
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            if (mItems == null) {
                return null;
            }
            if (mItems.size() <= position) {
                return null;
            }

            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                mLayout = (LinearLayout) convertView;
            } else {
                mLayout = (LinearLayout) mInflater.inflate(R.layout.specific_worldclock_item, null);
            }

            CityTime ct;
            try {
                ct = mItems.get(position);
            } catch (IndexOutOfBoundsException e) {
                Log.w(TAG, "WorldClockAdapter.getView: e = " + e.toString());
                return mLayout;
            }

            if (ct == null) {
                return mLayout;
            }

            String id = ct.getCityId();
            String cityName = ct.getCityName();
            TimeZone tz = ct.getTimeZone();

            Calendar c = Calendar.getInstance(tz);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

            // city name
            HtcListItem2LineText zoneInfo = (HtcListItem2LineText) mLayout.findViewById(R.id.zone_information);
            HtcListItem infoItem = (HtcListItem) mLayout.findViewById(R.id.info_item);
            ImageView cityIcon = (ImageView) mLayout.findViewById(R.id.city_icon);
            ResUtils itemResUtils = new ResUtils(mActivity, mLayout);

            if (cityName.equals(CURRENT)) {
                cityIcon.setVisibility(View.VISIBLE);
                cityIcon.setImageResource(R.drawable.icon_indicator_location_light_l);
                zoneInfo.setPrimaryText(id);
                zoneInfo.setPrimaryTextStyle(R.style.HtcListItem2LineTextStyle);
            } else if (cityName.equals(HOME)) {
                cityIcon.setVisibility(View.VISIBLE);
                cityIcon.setImageResource(R.drawable.icon_indicator_home_l);
                zoneInfo.setPrimaryText(id);
                zoneInfo.setPrimaryTextStyle(R.style.HtcListItem2LineTextStyle);
            } else {
                cityIcon.setVisibility(View.GONE);
                zoneInfo.setPrimaryText(cityName);
                zoneInfo.setPrimaryTextStyle(R.style.HtcListItem2LineTextStyle);
            }

            if ((position == 0) && (cityName.equals(CURRENT) || cityName.equals(HOME))) {
                CharSequence format = getDateFormat(mActivity);
                if (format != null) {
                    zoneInfo.setSecondaryText(HtcResUtil.toUpperCase(mActivity, DateFormat.format(format, c).toString()));
                }
            } else {
                // today, yesterday, or tomorrow
                long diff = tz.getOffset(c.getTimeInMillis()) - mTimeZone.getOffset(c.getTimeInMillis());

                if ((diff > 0) && (dayOfWeek != mCurrentDay)) {
                    zoneInfo.setSecondaryText(TOMORROW);
                } else if ((diff < 0) && (dayOfWeek != mCurrentDay)) {
                    zoneInfo.setSecondaryText(YESTERDAY);
                } else {
                    zoneInfo.setSecondaryText(TODAY);
                }
            }

            // time
            DigitalClock digitalClock = (DigitalClock) mLayout.findViewById(R.id.common_digital_clock_btn);
            digitalClock.setLive(false);
            digitalClock.set24HourMode(m24HourMode);
            
            // calculate all displays for digital clock
            TextView timeDisplay = (TextView) mLayout.findViewById(R.id.timeDisplay);
            timeDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDefaultFontSize(getActivity(), R.dimen.time_display_size));
            TextView ampmDisplay = (TextView) mLayout.findViewById(R.id.am_pm);
            ampmDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDefaultFontSize(getActivity(), R.dimen.am_pm_size));
            TextView dayDisplay = (TextView) mLayout.findViewById(R.id.day);
            dayDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDefaultFontSize(getActivity(), R.dimen.day_size));
            timeDisplay.setWidth(mMaxTimeDisplay);
            if (mMaxAmPmDisplay > mMaxDayDisplay) {
                ampmDisplay.setWidth(mMaxAmPmDisplay);
                dayDisplay.setWidth(mMaxAmPmDisplay);
            } else {
                ampmDisplay.setWidth(mMaxDayDisplay);
                dayDisplay.setWidth(mMaxDayDisplay);
            }
            
            timeDisplay.setMaxLines(1);
            timeDisplay.setEllipsize(TruncateAt.MARQUEE);
            timeDisplay.setHorizontalFadingEdgeEnabled(true);
            ampmDisplay.setMaxLines(1);
            ampmDisplay.setEllipsize(TruncateAt.MARQUEE);
            ampmDisplay.setHorizontalFadingEdgeEnabled(true);
            dayDisplay.setMaxLines(1);
            dayDisplay.setEllipsize(TruncateAt.MARQUEE);
            dayDisplay.setHorizontalFadingEdgeEnabled(true);

            ViewGroup.LayoutParams lp_digital = (ViewGroup.LayoutParams)digitalClock.getLayoutParams();
            if (mMaxDigitalDisplay != lp_digital.width) {
                if (mMaxDigitalDisplay == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    lp_digital.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    lp_digital.width = mMaxDigitalDisplay;
                    // set right word fade out when truncate
                    int m4 = getResources().getDimensionPixelSize(R.dimen.common_dimen_m4);
                    if (m4 + mMaxTimeDisplay > mMaxDigitalDisplay) {
                        timeDisplay.setGravity(Gravity.LEFT);
                    }
                    ampmDisplay.setGravity(Gravity.LEFT);
                    dayDisplay.setGravity(Gravity.LEFT);
                }
                digitalClock.setLayoutParams(lp_digital);
            }
            // end of calculate all displays for digital clock
            
            if(ct.getWeatherDayTime() != 0) {
                digitalClock.updateTime(c, ct);
            } else {
                digitalClock.updateTime(c);
            }

            return mLayout;
        }

        public void onDestroy() {

            if (mItems != null) {
                mItems.clear();
                mItems = null;
            }
        }
    }

    private class FooterAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        protected ArrayList<String> mItems = null;
        protected View mLayout;

        public FooterAdapter(ArrayList<String> list) {
            if (list != null) {
                mItems = new ArrayList<String>(list);
            }
            mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if (mItems == null) {
                return 0;
            }
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                mLayout = convertView;
            } else {
                mLayout = mInflater.inflate(R.layout.common_footer_list_item, null);
            }
            HtcListItem1LineCenteredText cityNameView;
            cityNameView = (HtcListItem1LineCenteredText)mLayout.findViewById(R.id.footer_list_name);
            mLayout.setBackground(null);
            cityNameView.setText(mItems.get(position));
            return mLayout;
        }
    }
    
    private static String getDateFormat(Context context) {
        String datFormat;
        datFormat = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);
        Log.i(TAG, "getDateFormat: settings's datFormat = " + datFormat);
        if (TextUtils.isEmpty(datFormat)) {
            datFormat = "EE, MMM d, yyyy"; // htc_default_date_format
        }

        Log.i(TAG, "getDateFormat: datFormat = " + datFormat);
        return datFormat;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!Global.isSupportAccChinaSense()) {
            // create customize menu from inflater or runtime add from code
            inflater.inflate(R.menu.worldclock_menuitems, menu);
            if (DEBUG_FLAG) Log.d(TAG, "onCreatOptionMenu:inflate menu item complete");
            mMenuView = menu;
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (!Global.isSupportAccChinaSense()) {
            if (menu == null) {
                Log.w(TAG, "onPrepareOptionsMenu: menu = null");
                return;
            }
    
            // If data is not ready, we don't want to show menu panel.
            if (mWorldClockState.getState() == WorldClockEnum.INIT) {
                return;
            }
    
            if (DEBUG_FLAG) Log.d(TAG, "onPrepareOptionsMenu: mStationarySize = " + mStationarySize);
            if (myList != null) {
                if (DEBUG_FLAG) Log.d(TAG, "onPrepareOptionsMenu: myList.size() = " + myList.size());
            }
    
            try {
                if ((myList == null) || (getDefaultCityCount() <= 0)) {
                    menu.findItem(R.id.edit).setEnabled(false);
                } else {
                    menu.findItem(R.id.edit).setEnabled(true);
                }
    
                if(!WorldClockTabControl.isShowMeInstall(mActivity)) {
                    menu.findItem(R.id.tips).setVisible(false);
                } else {
                    menu.findItem(R.id.tips).setVisible(true);
                }
            } catch (Exception e) {
                Log.w(TAG, "onPrepareOptionsMenu: menu find null view, Exception = " + e.toString());
                mActivity.invalidateOptionsMenu();
                Handler handler = new Handler();
                handler.post(new Runnable() {
    
                    @Override
                    public void run() {
                        mActivity.openOptionsMenu();
                    }
                });
                return;
            }
            try {
                mWorldClockResUtils.setActionDropDownPrimaryText(null, WorldClockEnum.NORMAL_SCREEN_MODE);
                setMenuItemVisibity(true);
            } catch (Exception e) {
    
                //when time change, it will call UI_MSG_CITY event to re-add city to adpater.
                //If this happen between main thread update city list and clear city list. the adpater city list size will
                //become zero
                Log.w(TAG, "onPrepareOptionsMenu: adapter list size is 0, Exception = " + e.toString());
                mNonUIHandler.sendEmptyMessage(NONUI_MSG_CITY_UPDATE);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!Global.isSupportAccChinaSense()) {
            Intent intent;
            intent = new Intent();
            switch (item.getItemId()) {
                case R.id.add_city:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: add_city");
                    if ((myList == null) || (getDefaultCityCount() >= MAX_CITY_COUNT)) {
                        mActivity.showDialog(WorldClockTabControl.WORLDCLOCK_REQUEST_ADD);
                    } else {
                        intent.setClass(mActivity, TimeZonePicker.class);
                        intent.putExtra(SEARCH_INTENTION, SEARCH_FOR_ADD);
                        startActivityForResult(intent, LAUNCH_ADD_TIMEZONE);
                    }
                    break;
                case R.id.current_settings:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: current_settings");
                    try {
                        intent.setAction("android.settings.DATE_SETTINGS");
                        startActivityForResult(intent, LAUNCH_LOCALTIME_SETTING);
                    } catch (Exception e) {
                        Log.w(TAG, "onOptionsItemSelected: CURRENT_SETTINGS_ID fail e = " + e.toString());
                    }
                    break;
                case R.id.home_settings:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: home_settings");
                    intent.setClass(mActivity, TimeZonePicker.class);
                    intent.putExtra(SEARCH_INTENTION, SEARCH_FOR_HOME_SETTINGS);
                    startActivityForResult(intent, LAUNCH_HOME_SETTING);
                    break;
                case R.id.edit:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: edit");
                    intent.setClass(mActivity, RearrangeTimeZone.class);
                    startActivityForResult(intent, LAUNCH_REARRANGE_TIMEZONE);
                    break;
                case R.id.edit_tabs:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: edit_tabs");
                    ((CarouselTab) getParentFragment()).enterCarouselEditMode();
                    break;
                case R.id.tips:
                    if(DEBUG_FLAG) Log.d(TAG,"onOptionsItemSelected: tips & help");
                    WorldClockTabControl.launchShowme(mActivity);
                    break;
                default:
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setMenuItemVisibity(boolean visible) {
        if (DEBUG_FLAG) Log.d(TAG, "setMenuItemVisibity: value = " + visible);
        mMenuView.findItem(R.id.edit_tabs).setVisible(visible);
        mMenuView.findItem(R.id.edit).setVisible(visible);
        mMenuView.findItem(R.id.home_settings).setVisible(visible);
        mMenuView.findItem(R.id.current_settings).setVisible(visible);
        mMenuView.findItem(R.id.add_city).setVisible(visible);
        if(WorldClockTabControl.isShowMeInstall(mActivity)) {
            mMenuView.findItem(R.id.tips).setVisible(visible);
        }
    }

    private void checkCityAndTimeChange() {

        if (DEBUG_FLAG) Log.d(TAG, "checkCityAndTimeChange: mCityChanged = " + mCityChanged + ", mTimeChanged = " + mTimeChanged);

        if (mCityChanged) {
            mCityChanged = false;
            mTimeChanged = false;

            if (mWorldClockAdapter != null) {
                mNonUIHandler.sendEmptyMessage(NONUI_MSG_CITY_UPDATE);
            }
        } else if (mTimeChanged) {
            mTimeChanged = false;
            if (mWorldClockAdapter != null) {
                mNonUIHandler.sendEmptyMessage(NONUI_MSG_TIMETICK);
            }
        }
    }

    private void unInitRegister() {

        if (mIsRegistered) {

            if (mIntentReceiver != null) {
                mActivity.unregisterReceiver(mIntentReceiver);
                mIntentReceiver = null;
            }
            // unregister city change observer
            if ((mContentResolver != null) && (mCityChangeObserver != null)) {
                mContentResolver.unregisterContentObserver(mCityChangeObserver);
            }
            // unregister time format (12/24H) change observer
            if ((mContentResolver != null) && (mFormatChangeObserver != null)) {
                mContentResolver.unregisterContentObserver(mFormatChangeObserver);
            }
            mIsRegistered = false;
        }
    }

    private int getDefaultCityCount() {
        int ret = 0;
        if (myList != null) {
            boolean isDup = PreferencesUtil.getDupHomeDefaultCity(mActivity);
            ret = myList.size() - mStationarySize;
            if (isDup) {
                ret++;
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "getDefaultCityCount: ret = " + ret);
        return ret;
    }

    protected void setCurrentDay() {

        Calendar calNow = Calendar.getInstance();
        mCurrentDay = calNow.get(Calendar.DAY_OF_WEEK);
        m24HourMode = AlarmUtils.get24HourMode(mActivity); // update date format
    }

    // TIMETICK_CHANGED
    protected class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            refreshTime();
        }
    }

    private void refreshTime() {

        if (mWorldClockState.getState() == WorldClockEnum.PAUSE) {
            mTimeChanged = true;
            return;
        }
        if (DEBUG_FLAG) Log.d(TAG, "refreshTime");
        if (mWorldClockAdapter != null) {
            mNonUIHandler.sendEmptyMessage(NONUI_MSG_TIMETICK);
        }
    }

    private class MinuteTask extends TimerTask {
        @Override
        public void run() {

            refreshTime();
        }
    }

    private class CityChangeObserver extends ContentObserver {
        public CityChangeObserver(Handler h) {
            super(h);

        }

        @Override
        public void onChange(boolean selfChange) {

            if (mWorldClockState.getState() == WorldClockEnum.PAUSE) {
                mCityChanged = true;
                return;
            }
            if (DEBUG_FLAG) Log.d(TAG, "CityChangeObserver.onChange");
            if (mWorldClockAdapter != null) {
                mNonUIHandler.sendEmptyMessageDelayed(NONUI_MSG_CITY_UPDATE, DELAY_TIME_CITY_UPDATE);
            }
        }
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());

        }

        @Override
        public void onChange(boolean selfChange) {

            if (mWorldClockState.getState() == WorldClockEnum.PAUSE) {
                mTimeChanged = true;
                return;
            }
            if (DEBUG_FLAG) Log.d(TAG, "FormatChangeObserver.onChange");
            if (mWorldClockAdapter != null) {
                mNonUIHandler.sendEmptyMessage(NONUI_MSG_TIMETICK);
            }
        }
    }

    private void initListener() {
        getView().setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {

		        if (event.getAction() == KeyEvent.ACTION_DOWN) {
		            if ((keyCode == KeyEvent.KEYCODE_MENU) && event.isLongPress()) {
		                return true;
		            }
		        }
				return false;
			}
        	
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (DEBUG_FLAG) Log.d(TAG, "onActivityResult: resultCode = " + resultCode);
        switch (requestCode) {
            case LAUNCH_ADD_TIMEZONE:
                if (resultCode == Activity.RESULT_OK) {
                    mLoadDataState.changeState(LoadDataEnum.ADD_ANIMATION);
                }
                break;
        }
    }

    public ArrayList<CityTime> getCityList() {
        return myList;
    }

    public static boolean isUseWirelessNetworks(Context context) {

        return Settings.Secure.isLocationProviderEnabled(
            context.getContentResolver(), LocationManager.NETWORK_PROVIDER);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Global.isSupportAccChinaSense() && mFooterPopUpWindow != null) {
            if (mFooterPopUpWindow.isShowing()) {
                mFooterPopUpWindow.dismissWithoutAnimation();
            }
            if (mWorldClockResUtils != null) {
                mWorldClockResUtils.setPopUpWindowExpand(newConfig, mFooterPopUpWindow);
            }
        }
    }
}
