package com.htc.android.worldclock.worldclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcSkinUtils;
import com.htc.android.worldclock.utils.HtcStorageChecker;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcDeleteButton;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcListItem2LineText;
import com.htc.lib1.cc.widget.HtcListView.DeleteAnimationListener;
import com.htc.lib1.cc.widget.HtcOverlapLayout;
import com.htc.lib1.cc.widget.HtcReorderListView;
import com.htc.lib1.theme.ThemeType;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherUtility;

public class RearrangeTimeZone extends Activity {
    private static final String TAG = "WorldClock.RearrangeTimeZone";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private final String DELETE_LOCATION_CHANGED = "com.htc.Weather.delete_location_changed";
    private final String REARRANGE_LIST_CHANGED = "com.htc.Weather.rearrange_list_changed";

    protected RearrangeAdapter mRearrangeAdapter;
    // thread control
    private Handler mNonUIHandler = null;
    protected Looper mNonUILooper = null;
    // UI Message event
    private final int UI_MSG_INIT = 0x0001;
    // none UI Message event
    private final int NONUI_MSG_INIT = 0x0100;

    public int mListSize;
    public ArrayList<CityTime> myList = null;
    protected int mCurrentDay;
    protected TimeZone mTimeZone;

    // TouchInterceptor mTouchInterceptor;
    private HtcReorderListView mRearrangeTimeZoneList;

    // for Delete
    protected boolean[] mDeletedIndex;
    private int mDeleteNumber;
    private HtcFooterButton mDeleteButton;
    private String mDeleteString;
    private RearrangeTimeZoneResUtils mRearrangeTimeZoneResUtils;
    private WeatherLocation[] mRearrangeWeatherLoc = null;
    private String[] mDeleteItemCityCode = null;
    private boolean mIsRearranged = false;
    // Htc font scale
    private boolean mHtcFontscale = false;
    
    private HtcOverlapLayout mRearrangeView;
    // Htc Theme
    private boolean mIsThemeChanged = false;

    HtcCommonUtil.ThemeChangeObserver mThemeChangeObserver = new HtcCommonUtil.ThemeChangeObserver() {
        @Override
        public void onThemeChange(int type) {
                if (type == ThemeType.HTC_THEME_FULL || type == ThemeType.HTC_THEME_CC) {
                    mIsThemeChanged = true;
                }
        }
    };
    
    @Override
    public void onCreate(Bundle icicle) {
        mHtcFontscale = HtcSkinUtils.initHtcFontScale(this);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(icicle);
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][RearrangeTimeZone][START]");
        setContentView(R.layout.main_rearrange_clock);

        mRearrangeView = (HtcOverlapLayout) findViewById(R.id.rearrange_view);
        ResUtils.enableStatusBarTheme(this);
        mRearrangeTimeZoneResUtils = new RearrangeTimeZoneResUtils(this, null);
        mRearrangeTimeZoneResUtils.initResources();

        mRearrangeTimeZoneList = (HtcReorderListView) findViewById(R.id.touch_interceptor);
        mRearrangeTimeZoneList.setDraggerId(R.id.rearrage_icon);

        // for Delete
        mDeleteNumber = 0;
        mDeleteButton = (HtcFooterButton) findViewById(R.id.cmd_bar_btn_1);
        mDeleteString = getResources().getString(R.string.delete);

        mDeleteButton.setText(R.string.done);
        mDeleteButton.setEnabled(true);

        mRearrangeTimeZoneList.setDeleteAnimationListener(new DeleteAnimationListener() {

            @Override
            public void onAnimationEnd() {
                RearrangeTimeZone.this.setResult(RESULT_OK);
                RearrangeTimeZone.this.finish();
            }

            @Override
            public void onAnimationStart() {

            }

			@Override
			public void onAnimationUpdate() {
				mRearrangeAdapter.notifyDataSetChanged();
			}

        });

        initHandler();
        mNonUIHandler.sendEmptyMessage(NONUI_MSG_INIT);
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
                        if (DEBUG_FLAG) Log.d(TAG, "initHandler: NONUI_MSG_INIT");
                        initCityList();
                        mMainHandler.sendEmptyMessage(UI_MSG_INIT);
                        break;
                    default:
                        Log.w(TAG, "initHandler: no support message");
                        break;
                }
            }
        };
    }

    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_MSG_INIT:
                    if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_INIT");
                    initUI();
                    if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][RearrangeTimeZone][DATA_READY]");
                    break;
                default:
                    Log.w(TAG, "mMainHandler.handleMessage: no support message");
                    break;
            }
        }
    };

    private void initUI() {
        // for Delete

        if (myList == null) {
            return;
        }

        mDeletedIndex = new boolean[myList.size()];

        for (int i = 0; i < mDeletedIndex.length; i++) {
            mDeletedIndex[i] = false;
        }

        mListSize = myList.size();

        mRearrangeTimeZoneList.setVerticalScrollBarEnabled(false);

        HtcFooterButton btn1 = (HtcFooterButton) findViewById(R.id.cmd_bar_btn_1);
        btn1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myList == null) {
                    return;
                }

                //for rearrange preset
                ArrayList<CityTime> cachedList = new ArrayList<CityTime>(myList);
                int cacheSize = cachedList.size();
                if (cacheSize > 0) {
                    mRearrangeWeatherLoc = new WeatherLocation[cacheSize];
                    for (int i = 0; i < cacheSize; i++) {
                        CityTime ct = cachedList.get(i);
                        mRearrangeWeatherLoc[i] = ct.getWeatherLocation();
                    }
                }

                // for Delete preset
                LinkedList<String> aCode = new LinkedList<String>();
                ArrayList<Integer> delItemList = new ArrayList<Integer>();

                for (int i = mDeletedIndex.length - 1; i >= 0; i--) {
                    if (mDeletedIndex[i] == true) {
                        aCode.add(cachedList.get(i).getWeatherLocation().getCode());
                        delItemList.add(i);
                        mRearrangeAdapter.removeItem(i);
                    }
                }

                int aCodeSize = aCode.size();
                if (aCodeSize > 0) {
                    mDeleteItemCityCode = new String[aCode.size()];
                    for (int i = 0; i < aCodeSize; i++) {
                        mDeleteItemCityCode[i] = aCode.get(i);
                    }
                }

                //write db for rearrange and delete
                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                        	Intent intent = null;
                            try {
                                
                                if (mRearrangeWeatherLoc != null && mIsRearranged) {
                                    WeatherUtility.saveLocations(getContentResolver(),
                                        WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY, mRearrangeWeatherLoc);
                                    PreferencesUtil.setSyncWorldClockDB(RearrangeTimeZone.this, false);
                                    intent = new Intent(REARRANGE_LIST_CHANGED);
                                }
                                if (mDeleteItemCityCode != null) {
                                    WeatherUtility.deleteLocation(getContentResolver(), WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY, mDeleteItemCityCode);
                                    PreferencesUtil.setSyncWorldClockDB(RearrangeTimeZone.this, false);
                                    intent = new Intent(REARRANGE_LIST_CHANGED);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "initUI.Thread.run: e = " + e.toString());
                            }
                            
                            if(intent != null) {
                            	sendBroadcast(intent, Global.PERMISSION_APP_HSP);
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    Log.w(TAG, "initUI.btn1.onClick: deleteLocation e = " + e.toString());
                }

                for (int i = 0; i < mDeletedIndex.length; i++) {
                    mDeletedIndex[i] = false;
                }

                mRearrangeTimeZoneList.disableTouchEventInAnim();
                if (delItemList.size() != 0) {
                    mRearrangeTimeZoneList.setDelPositionsList(delItemList);
                }
                else {
                    RearrangeTimeZone.this.setResult(RESULT_OK);
                    RearrangeTimeZone.this.finish();
                }
            }
        });

        HtcFooterButton btn2 = (HtcFooterButton) findViewById(R.id.cmd_bar_btn_2);
        btn2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RearrangeTimeZone.this.setResult(RESULT_CANCELED);
                RearrangeTimeZone.this.finish();
            }
        });

        mRearrangeAdapter = new RearrangeAdapter(myList);
        mRearrangeTimeZoneList.setAdapter(mRearrangeAdapter);
        mRearrangeTimeZoneList.setItemsCanFocus(false);
        mRearrangeTimeZoneList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                if (mDeletedIndex == null) {
                    return;
                }

                mDeletedIndex[position] = !mDeletedIndex[position];
                HtcDeleteButton cb = (HtcDeleteButton) v.findViewById(R.id.delete_icon);
                cb.setChecked(mDeletedIndex[position]);
                handleDeleteCount(mDeletedIndex[position]);
            }
        });

        if (mListSize > 1) {
            (mRearrangeTimeZoneList).setDropListener(mDropListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // create customize menu from inflater or runtime add from code
        getMenuInflater().inflate(R.menu.rearrangetimezone_menuitems, menu);
        if (DEBUG_FLAG) Log.d(TAG, "onCreatOptionMenu:inflate menu item complete");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (menu == null) {
            Log.w(TAG, "onPrepareOptionsMenu: menu = null");
            return false;
        }

        if(!WorldClockTabControl.isShowMeInstall(this)) {
            menu.findItem(R.id.tips).setVisible(false);
        } else {
            menu.findItem(R.id.tips).setVisible(true);
        }

        if (mDeleteNumber == 0) {
            // Disabled "Deselect all" if nothing is selected
            menu.findItem(R.id.select_all).setEnabled(true);
            menu.findItem(R.id.deselect_all).setEnabled(false);
        }
        else if (mDeleteNumber == mListSize) {
            // Disabled "Select all" if everything is selected.
            menu.findItem(R.id.select_all).setEnabled(false);
            menu.findItem(R.id.deselect_all).setEnabled(true);
        }
        else {
            menu.findItem(R.id.select_all).setEnabled(true);
            menu.findItem(R.id.deselect_all).setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_all:
                handleDeleteAllCount(true);
                break;
            case R.id.deselect_all:
                handleDeleteAllCount(false);
                break;
            case R.id.tips:
                if(DEBUG_FLAG) Log.d(TAG,"onOptionsItemSelected: tips & help");
                WorldClockTabControl.launchShowme(this);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initCityList() {

        if (myList != null) {
            myList.clear();
        } else {
            myList = new ArrayList<CityTime>();
        }

        WeatherLocation[] w = WeatherUtility.loadLocations(getContentResolver()
            , WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY);
        if (w != null) {
            int length = w.length;

            String id = null;
            String name = null;
            String code = null;
            TimeZone tz;
            Set<String> codeSet = new HashSet<String>();
            for (int i = 0; i < length; i++) {
                code = w[i].getCode();
                if (!codeSet.contains(code)) {
                    id = w[i].getTimezoneId();
                    name = w[i].getName();
                    tz = TimeZone.getTimeZone(id);
                    addCityName(tz, id, name, w[i]);
                    codeSet.add(code);
                }
            }
        }
    }

    private void addCityName(TimeZone tz, String id, String name, WeatherLocation w) {

        CityTime ct = new CityTime();
        ct.setTimeZone(tz);
        ct.setCityId(id);
        ct.setCityName(name);
        ct.setWeatherLocation(w);
        if (myList != null) {
            myList.add(ct);
        }
    }

    class RearrangeAdapter extends BaseAdapter {
        protected ArrayList<CityTime> mItems = null;
        protected LayoutInflater mInflater;
        protected View mLayout;

        public RearrangeAdapter(ArrayList<CityTime> list) {

            mItems = list;
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void removeItem(int position) {
            mItems.remove(position);
            notifyDataSetChanged();
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

            // super.getView(position, convertView, parent);
            if (convertView != null) {
                mLayout = convertView;
            } else {
                mLayout = mInflater.inflate(R.layout.common_rearrange_item, null);
            }

            CityTime ct;
            try {
                ct = mItems.get(position);
            } catch (IndexOutOfBoundsException e) {
                Log.w(TAG, "RearrangeAdapter.getView: e = " + e.toString());
                return null;
            }

            String cityName = ct.getCityName();
            TimeZone tz = ct.getTimeZone();

            Calendar c = Calendar.getInstance(tz);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

            HtcListItem2LineText text_city_name = (HtcListItem2LineText) mLayout.findViewById(R.id.text_city_name);
            HtcDeleteButton cb = (HtcDeleteButton) mLayout.findViewById(R.id.delete_icon);
            text_city_name.setPrimaryText(cityName);

            // today, yesterday, or tomorrow
            long diff = tz.getOffset(c.getTimeInMillis()) - mTimeZone.getOffset(c.getTimeInMillis());

            if ((diff > 0) && (dayOfWeek != mCurrentDay)) {
                text_city_name.setSecondaryText(WorldClock.TOMORROW);
            } else if ((diff < 0) && (dayOfWeek != mCurrentDay)) {
                text_city_name.setSecondaryText(WorldClock.YESTERDAY);
            } else {
                text_city_name.setSecondaryText(WorldClock.TODAY);
            }
            // for Delete
            final int selectedPosition = position;
            try {
                cb.setChecked(mDeletedIndex[selectedPosition]);
            } catch (IndexOutOfBoundsException e) {
                Log.w(TAG, "RearrangeAdapter.getView: e = " + e.toString());
                return mLayout;
            }

            cb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DEBUG_FLAG) Log.d(TAG, "RearrangeAdapter.getView.onClick");
                    if (mDeletedIndex == null) {
                        return;
                    }

                    boolean isChecked = ((HtcDeleteButton) v).isChecked();

                    handleDeleteCount(isChecked);
                    mDeletedIndex[selectedPosition] = isChecked;
                }
            });

            convertView = mLayout;

            return convertView;
        }

        public void onDestroy() {

            if (mItems != null) {
                mItems.clear();
                mItems = null;
            }
            mLayout = null;
        }

    }

    private void handleDeleteCount(boolean isChecked) {

        if (DEBUG_FLAG) Log.d(TAG, "handleDeleteCount: mDeleteNumber = " + mDeleteNumber);

        if (isChecked) {
            mDeleteNumber++;
            if (mDeleteNumber == 1) {
                if (DEBUG_FLAG) Log.d(TAG, "handleDeleteCount: isChecked mDeleteNumber == 1");
            }
        } else {
            mDeleteNumber--;
            if (mDeleteNumber == 0) {
                if (DEBUG_FLAG) Log.d(TAG, "handleDeleteCount: notChecked mDeleteNumber == 0");
                mDeleteButton.setText(R.string.done);
                return;
            }
        }
        mDeleteButton.setText(mDeleteString + " (" + mDeleteNumber + ")");
    }

    private void handleDeleteAllCount(boolean isChecked) {

        if ((mDeletedIndex != null) && (mDeletedIndex.length > 0)) {
            for (int i = 0; i < mDeletedIndex.length; i++) {
                if (mDeletedIndex[i] != isChecked) {
                    mDeletedIndex[i] = isChecked;
                    handleDeleteCount(isChecked);
                }
            }
            mRearrangeAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        HtcStorageChecker.checkStorageFull(this);
        currentSetting();
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(RearrangeTimeZone.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
    }

    @Override
    protected void onDestroy() {

        mNonUIHandler.removeMessages(NONUI_MSG_INIT);
        mMainHandler.removeMessages(UI_MSG_INIT);

        if (mNonUILooper != null) {
            mNonUILooper.quit();
        }

        if (mRearrangeAdapter != null) {
            mRearrangeAdapter.onDestroy();
        }

        if (myList != null) {
            myList.clear();
        }

        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onDestroy();
    }

    /**
     * get day of week and timezone of current area
     */
    private void currentSetting() {

        Calendar c = Calendar.getInstance();
        mCurrentDay = c.get(Calendar.DAY_OF_WEEK);
        mTimeZone = c.getTimeZone();
    }

    private HtcReorderListView.DropListener mDropListener = new HtcReorderListView.DropListener() {
        @Override
        public void drop(int from, int to) {

            boolean tempChecked;
            CityTime ct = new CityTime();
            if ((to >= 0) && (to <= (mListSize - 1)) && (from != to)) {
                if (from > to) {
                    for (int i = from; i > to; i--) {
                        ct = myList.get(i - 1);
                        myList.set(i - 1, myList.get(i));
                        myList.set(i, ct);
                        if (mDeletedIndex != null) {
                            // swap two check status for rearrange
                            tempChecked = mDeletedIndex[i - 1];
                            mDeletedIndex[i - 1] = mDeletedIndex[i];
                            mDeletedIndex[i] = tempChecked;
                        }
                    }
                } else if (from < to) {
                    for (int i = from; i < to; i++) {
                        ct = myList.get(i + 1);
                        myList.set(i + 1, myList.get(i));
                        myList.set(i, ct);
                        if (mDeletedIndex != null) {
                            // swap two check status for rearrange
                            tempChecked = mDeletedIndex[i + 1];
                            mDeletedIndex[i + 1] = mDeletedIndex[i];
                            mDeletedIndex[i] = tempChecked;
                        }
                    }
                }
            }
            if(from != to) {
            	mIsRearranged = true;
            }
            ((RearrangeAdapter) mRearrangeTimeZoneList.getAdapter()).notifyDataSetChanged();
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        HtcSkinUtils.initHtcFontScale(this);
        mRearrangeTimeZoneResUtils.switchTheme(newConfig);
        if (mRearrangeTimeZoneList != null) {
            return;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU) && event.isLongPress()) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
