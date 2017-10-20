package com.htc.android.worldclock.stopwatch;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
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
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.CarouselTab.MyTabAdapter;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.stopwatch.StopwatchUtils.StopwatchLapData;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcListItem1LineCenteredText;
import com.htc.lib1.cc.widget.HtcListView;
import com.htc.lib1.cc.widget.HtcProperty;
import com.htc.lib1.cc.widget.ListPopupBubbleWindow;

public class Stopwatch extends Fragment {
    private static final String TAG = "WorldClock.Stopwatch";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private final int UI_MSG_UPDATE_SELF_MSG = 0x0001;
    private final int UI_MSG_UPDATE_LIST_DATA = 0x0002;
    private final int NONUI_MSG_ADD_LAP_DATA = 0x0100;
    private final int NONUI_MSG_DELETE_LAP_DATA = 0x0200;
    private final int NONUI_MSG_LOAD_DATA = 0x0300;

    private final int DELAY_TIME_MILLIS = 100;
    private final int MAX_LAP_COUNT = 300;
    private final long MAX_TIME = 100 * 60 * 1000; // 100 minutes

    private StopwatchResUtils mStopwatchResUtils;

    private ArrayList<StopwatchLapData> mLapList;
    private HtcListView mListView;
    private StopwatchAdapter mStopwatchAdapter;
    private ListPopupBubbleWindow mFooterPopUpWindow;
    private ArrayList<String> mFooterList;
    private FooterAdapter mFooterAdapter;

    private Looper mNonUILooper = null;
    private Handler mNonUIHandler = null;
    private StopwatchLapData mLapData = null;
    private View.OnClickListener mResetBtnClickListener;
    private View.OnClickListener mLapBtnClickListener;
    private View.OnClickListener mStartBtnClickListener;
    private Handler mMainHandler;
    private int mLapCount;
    private long mStart = 0;
    private long mPauseStart = 0;
    private long mRunningTotalTime;
    private long mLastRunningTotalTime;

    private int mStateOrdinal;

    private Activity mActivity;
    private View mMainView;
    private FrameLayout mStopwatchView;
    private int htcListItemHeight;

    public static enum StopwatchEnum {
        INIT, NORMAL, PLAY, STOP, PAUSE, END
    }

    // state control
    private StopwatchState mStopwatchState;

    private class StopwatchState {
        private StopwatchEnum mPrevState;
        private StopwatchEnum mRestoreState;

        StopwatchState(StopwatchEnum initState) {
            changeState(initState);
        }

        public StopwatchEnum getState() {
            return mPrevState;
        }

        public void restoreState() {
            if (mPrevState == StopwatchEnum.PAUSE) {
                mPrevState = mRestoreState;
            }
            if (DEBUG_FLAG) Log.d(TAG, "mStopwatchState.restoreState: " + mPrevState.toString());
        }

        public void changeState(StopwatchEnum currentState) {
            if (DEBUG_FLAG) Log.d(TAG, "mStopwatchState.changeState: " + this.mPrevState + " -> " + currentState.toString());
            switch (currentState) {
                case INIT:
                    break;
                case NORMAL:
                    break;
                case PLAY:
                    break;
                case STOP:
                    break;
                case PAUSE:
                    if (mPrevState != currentState) {
                        mRestoreState = mPrevState;
                    }
                    break;
                case END:
                    break;
                default:
                    Log.w(TAG, "mStopwatchState.changeState: No support state = " + currentState.toString());
            }
            this.mPrevState = currentState;
        }

        public StopwatchEnum findEnumById(int id) {
            for (StopwatchEnum state : StopwatchEnum.values()) {
                if (id == state.ordinal()) {
                    return state;
                }
            }
            return null;
        }
    }

    @Override
    public void onCreate(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][Stopwatch][START]");
        super.onCreate(sis);
        setHasOptionsMenu(true);
        initMember();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreateView");
        mMainView = inflater.inflate(R.layout.main_stopwatch, container, false);
        mStopwatchView = (FrameLayout) mMainView.findViewById(R.id.stopwatch_view);
        return mMainView;
    }

    @Override
    public void onActivityCreated(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(sis);

        mActivity = getActivity();
        mStopwatchResUtils = new StopwatchResUtils(mActivity, mMainView);
        mStopwatchResUtils.initResources();
        initNonUIHandlerThread();
        initFuntion();
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
        mStopwatchState.restoreState();
        loadDataFromPreference();
        setFooter();

        if(mListView != null && mListView.getCount() == 0 && mStopwatchView.hasFocus()) {
           if(DEBUG_FLAG)Log.d(TAG, "mStopwatchView has focus");
           mStopwatchView.setFocusable(false);
           mStopwatchView.clearFocus();
         }
    }

    @Override
    public void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        // save stopwatch state into preference
        mStateOrdinal = mStopwatchState.getState().ordinal();
        saveDataToPreference();

        if (mStopwatchState.getState() == StopwatchEnum.PLAY) {
            mMainView.setKeepScreenOn(false);
        }
        mStopwatchState.changeState(StopwatchEnum.PAUSE);
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG_FLAG) Log.d(TAG, "onStop");
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
        removeAllHandlerMessages();
        mStopwatchState.changeState(StopwatchEnum.END);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!Global.isSupportAccChinaSense()) {
            // create customize menu from inflater or runtime add from code
            inflater.inflate(R.menu.stopwatch_menuitems, menu);
            if (DEBUG_FLAG) Log.d(TAG, "onCreatOptionMenu:inflate menu item complete");
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
    
            if (mStopwatchState.getState() == StopwatchEnum.INIT) {
                return;
            }
    
            try {
                if(!WorldClockTabControl.isShowMeInstall(mActivity)) {
                    menu.findItem(R.id.tips).setVisible(false);
                } else {
                    menu.findItem(R.id.tips).setVisible(true);
                }
            } catch (Exception e) {
                Log.w(TAG, "onPrepareOptionsMenu: menu find null view, Exception e = " + e.toString());
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
        }
        super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!Global.isSupportAccChinaSense()) {
            switch (item.getItemId()) {
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

    private final void initNonUIHandlerThread() {
        HandlerThread nonUIHandlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        nonUIHandlerThread.start();
        mNonUILooper = nonUIHandlerThread.getLooper();
        mNonUIHandler = new Handler(mNonUILooper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case NONUI_MSG_ADD_LAP_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_ADD_LAP_DATA");
                        addLapData();
                        break;
                    case NONUI_MSG_DELETE_LAP_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_DELETE_LAP_DATA");
                        deleteLapData();
                        break;
                    case NONUI_MSG_LOAD_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_LOAD_DATA");
                        mLapList = StopwatchUtils.LoadStopwatchLapData(mActivity);
                        mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_LIST_DATA);
                        break;
                }
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mStopwatchResUtils != null) {
            mStopwatchResUtils.resetLayout();
        }
        if (Global.isSupportAccChinaSense() && mFooterPopUpWindow != null) {
            if (mFooterPopUpWindow.isShowing()) {
                mFooterPopUpWindow.dismissWithoutAnimation();
            }
            if (mStopwatchResUtils != null) {
                mStopwatchResUtils.setPopUpWindowExpand(newConfig, mFooterPopUpWindow);
            }
        }
    }

    private void initMember() {
        mStopwatchState = new StopwatchState(StopwatchEnum.INIT);
        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UI_MSG_UPDATE_SELF_MSG:
                        if (mStopwatchState.getState() == StopwatchEnum.PLAY) {
                            updateNextStopWatchTimer();
                            mStopwatchResUtils.updateImageSrc(mRunningTotalTime);
                            mStopwatchResUtils.updateLapImageSrc(mRunningTotalTime - mLastRunningTotalTime);
                            mMainHandler.sendMessageDelayed(mMainHandler.obtainMessage(UI_MSG_UPDATE_SELF_MSG), DELAY_TIME_MILLIS);
                        }
                        break;
                    case UI_MSG_UPDATE_LIST_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_UPDATE_LIST_DATA");
                        if (mStopwatchAdapter != null) {
                            mStopwatchAdapter.changeList(mLapList);
                            mStopwatchAdapter.notifyDataSetChanged();
                            // set view to last lap position
                            if (mListView != null) {
                                mListView.setSelection(mLapList.size() - 1);
                            }
                            // show lap title or not
                            if (mLapList.size() == 0) {
                                mStopwatchResUtils.showLapTitleView(false);
                            } else {
                                mStopwatchResUtils.showLapTitleView(true);
                            }
                        }
                        break;
                }
            }
        };

        mStartBtnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mStopwatchState.getState()) {
                    case NORMAL:
                        if (Global.isSupportStopwatchPauseResumeButton()) {
                            mStopwatchResUtils.setStartButtonView(false, R.string.pause);
                        } else {
                            mStopwatchResUtils.setStartButtonView(false, R.string.stop);
                        }
                        mStopwatchResUtils.setLapButtonEnabled(true);
                        mStopwatchResUtils.setResetButtonEnabled(true);
                        mStopwatchState.changeState(StopwatchEnum.PLAY);
                        mStart = SystemClock.elapsedRealtime();
                        mMainHandler.sendMessage(mMainHandler.obtainMessage(UI_MSG_UPDATE_SELF_MSG));
                        mMainView.setKeepScreenOn(true);
                        break;
                    case PLAY:
                        if (Global.isSupportStopwatchPauseResumeButton()) {
                            mStopwatchResUtils.setStartButtonView(true, R.string.resume);
                        } else {
                            mStopwatchResUtils.setStartButtonView(true, R.string.start);
                        }
                        mStopwatchResUtils.setLapButtonEnabled(false);
                        mStopwatchResUtils.setResetButtonEnabled(true);
                        mStopwatchState.changeState(StopwatchEnum.STOP);
                        mPauseStart = SystemClock.elapsedRealtime();
                        addLapListViewData();
                        mMainView.setKeepScreenOn(false);
                        break;
                    case STOP:
                        if (Global.isSupportStopwatchPauseResumeButton()) {
                            mStopwatchResUtils.setStartButtonView(false, R.string.pause);
                        } else {
                            mStopwatchResUtils.setStartButtonView(false, R.string.stop);
                        }
                        mStopwatchResUtils.setLapButtonEnabled(true);
                        mStopwatchResUtils.setResetButtonEnabled(true);
                        mStopwatchState.changeState(StopwatchEnum.PLAY);
                        mStart += SystemClock.elapsedRealtime() - mPauseStart;
                        mMainHandler.sendMessage(mMainHandler.obtainMessage(UI_MSG_UPDATE_SELF_MSG));
                        mLastRunningTotalTime = mRunningTotalTime;
                        mMainView.setKeepScreenOn(true);
                        break;
                }
            }
        };

        mLapBtnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mStopwatchState.getState()) {
                    case PLAY:
                        addLapListViewData();
                        break;
                }
            }
        };

        mResetBtnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopwatchResUtils.setStartButtonView(true, R.string.start);
                mStopwatchResUtils.setLapButtonEnabled(false);
                mStopwatchResUtils.setResetButtonEnabled(false);
                mStopwatchState.changeState(StopwatchEnum.NORMAL);
                mRunningTotalTime = 0;
                mLastRunningTotalTime = 0;
                mStopwatchResUtils.updateImageSrc(mRunningTotalTime);
                mStopwatchResUtils.updateLapImageSrc(mRunningTotalTime - mLastRunningTotalTime);
                deleteLapListViewData();
                mMainView.setKeepScreenOn(false);
            }
        };
    }

    private void initFuntion() {
        // initial List and Adapter
        mLapList = new ArrayList<StopwatchLapData>();
        mStopwatchAdapter = new StopwatchAdapter(mLapList);
        mListView = (HtcListView) mMainView.findViewById(R.id.htclist);
        mListView.setAdapter(mStopwatchAdapter);
        htcListItemHeight = (Integer) HtcProperty.getProperty(mActivity, "HtcListItemHeight");
        htcListItemHeight = ResUtils.getDefaultSize(mActivity,htcListItemHeight);
        if (Global.isSupportAccChinaSense()) {
            initFooterMoreList();
        }
        initKeyListener();

        // load data from database
        mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);
    }

    private void initFooterMoreList() {
        mFooterList = new ArrayList<String>();
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
            mStopwatchResUtils.setPopUpWindowExpand(configuration, mFooterPopUpWindow);
        }
    }
    
    public void setFooter() {
        // set Button Click Listener
        if (mActivity == null) {
            Log.w(TAG, "stopwatch set footer: mActivity = null");
            return;
        }

        MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
        if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_STOPWATCH)) {
            return;
        }

        mStopwatchResUtils.setHtcFooterButtonResource();
        HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
        if (footer != null) {
            footer.findViewById(R.id.footer_btn2).setOnClickListener(mStartBtnClickListener);
            footer.findViewById(R.id.footer_btn3).setOnClickListener(mLapBtnClickListener);
            footer.findViewById(R.id.footer_btn4).setOnClickListener(mResetBtnClickListener);
            footer.findViewById(R.id.footer_btn4).setVisibility(View.VISIBLE);
            if (!Global.isSupportAccChinaSense()) {
                footer.findViewById(R.id.footer_btn1).setVisibility(View.GONE);
            }
        }
        if (Global.isSupportAccChinaSense()) {
            updateFooterMoreList();
        }
        updateLayout();
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
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: edit_tabs");
                            ((CarouselTab)getParentFragment()).enterCarouselEditMode();
                            break;
                        case 1:
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
    
    private void updateLayout() {
        if (mStopwatchState.getState() == StopwatchEnum.INIT) {
            if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][Stopwatch][DATA_READY]");
            mStopwatchState.changeState(StopwatchEnum.NORMAL);
        } else if (mStopwatchState.getState() == StopwatchEnum.PLAY) {
            mMainView.setKeepScreenOn(true);
            mMainHandler.sendMessage(mMainHandler.obtainMessage(UI_MSG_UPDATE_SELF_MSG));
            if (Global.isSupportStopwatchPauseResumeButton()) {
                mStopwatchResUtils.setStartButtonView(false, R.string.pause);
            } else {
                mStopwatchResUtils.setStartButtonView(false, R.string.stop);
            }
            mStopwatchResUtils.setLapButtonEnabled(true);
            mStopwatchResUtils.setResetButtonEnabled(true);
        } else if (mStopwatchState.getState() == StopwatchEnum.STOP) {
            mStopwatchResUtils.updateImageSrc(mRunningTotalTime);
            mStopwatchResUtils.updateLapImageSrc(mRunningTotalTime - mLastRunningTotalTime);
            // reset start button due to share with timer start button
            if (Global.isSupportStopwatchPauseResumeButton()) {
                mStopwatchResUtils.setStartButtonView(true, R.string.resume);
            } else {
                mStopwatchResUtils.setStartButtonView(true, R.string.start);
            }
            mStopwatchResUtils.setLapButtonEnabled(false);
            mStopwatchResUtils.setResetButtonEnabled(true);
        } else {
            mStopwatchResUtils.updateImageSrc(mRunningTotalTime);
            mStopwatchResUtils.updateLapImageSrc(mRunningTotalTime - mLastRunningTotalTime);
            // reset start button due to share with timer start button
            mStopwatchResUtils.setStartButtonView(true, R.string.start);
            mStopwatchResUtils.setLapButtonEnabled(false);
            mStopwatchResUtils.setResetButtonEnabled(false);
        }
        mStopwatchResUtils.setStartButtonImage();
    }

    private void removeAllHandlerMessages() {
        if (mNonUILooper != null) {
            mNonUILooper.quit();
        }

        mMainHandler.removeMessages(UI_MSG_UPDATE_SELF_MSG);
        mMainHandler.removeMessages(UI_MSG_UPDATE_LIST_DATA);
        mNonUIHandler.removeMessages(NONUI_MSG_ADD_LAP_DATA);
        mNonUIHandler.removeMessages(NONUI_MSG_DELETE_LAP_DATA);
        mNonUIHandler.removeMessages(NONUI_MSG_LOAD_DATA);
    }

    private void loadDataFromPreference() {
        mLapCount = PreferencesUtil.getLapCount(mActivity);
        // reset device state will be change state to INIT state
        StopwatchEnum prefState = mStopwatchState.findEnumById(PreferencesUtil.getStopwatchState(mActivity));
        if ((prefState != null) && (prefState != StopwatchEnum.INIT)) {
            mStopwatchState.changeState(prefState);
        }
        if (DEBUG_FLAG) Log.d(TAG, "loadDataFromPreference: mStopwatchState.getState() = " + mStopwatchState.getState());
        mStart = PreferencesUtil.getStartTime(mActivity);
        mPauseStart = PreferencesUtil.getPauseTime(mActivity);
        mRunningTotalTime = PreferencesUtil.getRunningTotalTime(mActivity);
        mLastRunningTotalTime = PreferencesUtil.getLastRunningTotalTime(mActivity);
    }

    private void saveDataToPreference() {
        PreferencesUtil.setStopwatchState(mActivity, mStateOrdinal);
        PreferencesUtil.setLapCount(mActivity, mLapCount);
        PreferencesUtil.setStartTime(mActivity, mStart);
        PreferencesUtil.setRunningTotalTime(mActivity, mRunningTotalTime);
        PreferencesUtil.setLastRunningTotalTime(mActivity, mLastRunningTotalTime);
        if (mStopwatchState.getState() == StopwatchEnum.PLAY) {
            PreferencesUtil.setPauseTime(mActivity, SystemClock.elapsedRealtime());
        } else {
            PreferencesUtil.setPauseTime(mActivity, mPauseStart);
        }
    }

    /**
     * get next time of stopwatch
     */
    public void updateNextStopWatchTimer() {
        double d;
        long progress;

        if (mStopwatchState.getState() == StopwatchEnum.PLAY) {
            progress = SystemClock.elapsedRealtime() - mStart;
            if (progress >= MAX_TIME) {
                progress %= MAX_TIME;
            }

            d = (double) progress / 100;
            mRunningTotalTime = (int) (d + 0.5);
        } else if (mStopwatchState.getState() == StopwatchEnum.STOP) {
            mRunningTotalTime = (int) ((mPauseStart - mStart) / 100);
        } else {
            mRunningTotalTime = 0;
        }

        // check total time is overflow or not, if yes, last total time reset to 0
        if (mRunningTotalTime < mLastRunningTotalTime) {
            mLastRunningTotalTime = 0;
        }
    }

    private void addLapData() {
        StopwatchUtils.AddStopwatchLapData(mActivity, mLapData);
    }

    private void addLapListViewData() {
        if ((mLapCount) > MAX_LAP_COUNT) {
            mStopwatchResUtils.showMaxNumErrorAlertDialog();
            return;
        }

        // add to db
        mLapData = new StopwatchLapData(mLapCount, mRunningTotalTime, mRunningTotalTime - mLastRunningTotalTime);
        mNonUIHandler.sendEmptyMessage(NONUI_MSG_ADD_LAP_DATA);
        mLapCount++;

        if (mLapList != null) {
            mLapList.add(mLapData);
            mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_LIST_DATA);
        }

        if (mStopwatchState.getState() == StopwatchEnum.PLAY) {
            mLastRunningTotalTime = mRunningTotalTime;
        }
    }

    private void deleteLapData() {
        StopwatchUtils.DeleteStopwatchLapData(mActivity);
    }

    private void deleteLapListViewData() {
        mNonUIHandler.sendEmptyMessage(NONUI_MSG_DELETE_LAP_DATA);
        mLapCount = 1;
        if (mLapList != null) {
            mLapList.clear();
            mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_LIST_DATA);
        }
    }

    class StopwatchAdapter extends BaseAdapter {
        protected ArrayList<?> mItems = null;
        protected LayoutInflater mInflater;
        protected View mLayout;

        public StopwatchAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public StopwatchAdapter(ArrayList<?> list) {
            if (list != null) {
                mItems = (ArrayList<?>) list.clone();
            }
            mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void changeList(ArrayList<?> list) {
            if (list != null) {
                mItems = (ArrayList<?>) list.clone();
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
                mLayout = mInflater.inflate(R.layout.specific_stopwatch_lap_item, null);
            }

            StopwatchLapData listItem = null;
            try {
                listItem = (StopwatchLapData) mItems.get(position);
                if (listItem == null) {
                    return mLayout;
                }
            } catch (IndexOutOfBoundsException e) {
                Log.w(TAG, "StopwatchAdapter.getView: e = " + e.toString());
                return mLayout;
            }

            TextView lapTime = (TextView) mLayout.findViewById(R.id.lap_time);
            lapTime.setHeight(htcListItemHeight);
            lapTime.setMaxLines(1);
            lapTime.setEllipsize(TruncateAt.MARQUEE);
            TextView lapTotalTime = (TextView) mLayout.findViewById(R.id.lap_total_time);
            lapTotalTime.setHeight(htcListItemHeight);
            lapTotalTime.setMaxLines(1);
            lapTotalTime.setEllipsize(TruncateAt.MARQUEE);
            TextView lapId = (TextView) mLayout.findViewById(R.id.lap_id);
            lapId.setHeight(htcListItemHeight);
            lapId.setMaxLines(1);
            lapId.setEllipsize(TruncateAt.MARQUEE);
            
            lapTime.setText(listItem.lap_time_str);
            lapTotalTime.setText(listItem.lap_total_time_str);
            // format arabic font to correct arabic language format
            if ("ar".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
                String localeLapId = String.format("%d", listItem.lap_id);
                lapId.setText(getResources().getString(R.string.lap_title) + " " + localeLapId);
            } else {
                lapId.setText(getResources().getString(R.string.lap_title) + " " + listItem.lap_id);
            }

            ResUtils itemREsUtil = new ResUtils(mActivity, mLayout);

            itemREsUtil.setLayout(R.id.lap_id, R.dimen.stopwatch_lap_item_width, 0, 0, 0);
            itemREsUtil.setLayout(R.id.lap_total_time, R.dimen.stopwatch_total_item_width, 0, 0, 0);
            itemREsUtil.setLayout(R.id.lap_time, R.dimen.stopwatch_thisLap_item_width, 0, 0, 0);

            return mLayout;
        }

        @Override
        public boolean isEnabled(int position) {
            // disable items selectable
            return false;
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
    
    private void initKeyListener() {
        mStopwatchView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
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
}
