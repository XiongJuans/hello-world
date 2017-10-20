package com.htc.android.worldclock.alarmclock;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.widget.CompoundButtonCompat;
import android.text.TextUtils.TruncateAt;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.CarouselTab.MyTabAdapter;
import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.aiservice.AiUtils;
import com.htc.android.worldclock.aiservice.AlarmSorter;
import com.htc.android.worldclock.alarmclock.AlarmUtils.AlarmColumns;
import com.htc.android.worldclock.alarmclock.SetAlarm.RepeatTypeEnum;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.DigitalClock;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.android.worldclock.utils.SettingsActivity;
import com.htc.android.worldclock.utils.ToastMaster;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcListItem1LineCenteredText;
import com.htc.lib1.cc.widget.HtcListView;
import com.htc.lib1.cc.widget.ListPopupBubbleWindow;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 * AlarmClock application.
 */
public class AlarmClock extends Fragment {
    private static final String TAG = "WorldClock.AlarmClock";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    private final int UI_MSG_UPDATE_LIST_DATA = 0x0001;
    private final int UI_MSG_SCROLL_TO_FIRST_ADD = 0x002;
    private final int NONUI_MSG_INIT_DATA = 0x0100;
    private final int NONUI_MSG_LOAD_DATA = 0x0200;
    private ArrayList<AlarmItem> mAlarmList = null;
    private HtcListView mListView;
    private AlarmClockAdapter mAlarmClockAdapter;
    private ListPopupBubbleWindow mFooterPopUpWindow;
    private ArrayList<String> mFooterList;
    private FooterAdapter mFooterAdapter;

    private Looper mNonUILooper = null;
    private Handler mNonUIHandler = null;

    private final static int LAUNCH_SET_ALARM = 0;
    private final static int LAUNCH_DELETE_ALARM = 1;

    ArrayList<Integer> mDelItemList;

    protected AlarmChangeObserver mAlarmChangeObserver;
    protected ContentResolver mContentResolver;
    protected CharSequence[] mDaysAbbr;

    protected View mHeaderView;
    private int mStartWeekDay = 1; // 1 -- Sunday; 2 -- Monday

    private Activity mActivity;
    private View mMainView;
    private AdapterView.OnItemClickListener mAlarmItemClick;

    private AlarmClockResUtils mAlarmClockResUtils;

    private Handler mMainHandler;
    private View.OnClickListener mAddBtnClickListener;
    private View.OnClickListener mDeleteBtnClickListener;
    
    private int mListItemHeight;
    private int mMaxTimeDisplay;
    private int mMaxAmPmDisplay;
    private int mMaxDigitalDisplay;
    private int mNewAlarmId;
    //is go set alarm activity
    private boolean mGoToSetAlarm = false;
    
    protected static enum AlarmClockEnum {
        INIT, NORMAL, DATA_CHANGE, PAUSE, END
    }

    // state control
    private AlarmClockState mAlarmClockState;

    private View mAiTipsContianer;
    private TextView mTxtTip;
    private boolean mNotAskShowAgain;
    private boolean mIsClickTipsCancel;
    private AiUtils mAiManager;
    private SharedPreferences mAiSharePrefences;

    private HtcAlertDialog mSortDialog;

    //Monitor sharePreferences changed to update UI
    private SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ((key.equals(AiUtils.AI_ALARM_EARLY_EVENT_LIST)) && !mGoToSetAlarm) {
                Log.d(AiUtils.AI_TAG, "update ai alarm SharedPreferenceChangeListener");
                mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);
            }
        }
    };

    private class AlarmClockState {
        private AlarmClockEnum mPrevState;
        private AlarmClockEnum mRestoreState;

        AlarmClockState(AlarmClockEnum initState) {
            changeState(initState);
        }

        public AlarmClockEnum getState() {
            return mPrevState;
        }

        public void restoreState() {
            if (mPrevState == AlarmClockEnum.PAUSE) {
                mPrevState = mRestoreState;
            }
            if (DEBUG_FLAG) Log.d(TAG, "mAlarmClockState.restoreState: " + mPrevState.toString());
        }

        public void changeState(AlarmClockEnum currentState) {
            if (DEBUG_FLAG) Log.d(TAG, "AlarmClockState.changeState: " + this.mPrevState + " -> " + currentState.toString());
            switch (currentState) {
                case INIT:
                    break;
                case NORMAL:
                    break;
                case DATA_CHANGE:
                    break;
                case PAUSE:
                    if (mPrevState != currentState) {
                        mRestoreState = mPrevState;
                    }
                    break;
                case END:
                    break;
                default:
                    Log.w(TAG, "AlarmClockState.changeState: No support state = " + currentState.toString());
            }
            this.mPrevState = currentState;
        }
    }

    protected static enum LoadDataEnum {
        NO_ANIMATION, ADD_ANIMATION
    }

    // state control
    private LoadDataState mLoadDataState;

    private class LoadDataState {
        private LoadDataEnum mPrevState;

        LoadDataState(LoadDataEnum initState) {
            changeState(initState);
        }

        public LoadDataEnum getState() {
            return mPrevState;
        }

        public void changeState(LoadDataEnum currentState) {
            if (DEBUG_FLAG) Log.d(TAG, "AlarmClockState.changeState: " + this.mPrevState + " -> " + currentState.toString());
            switch (currentState) {
                case NO_ANIMATION:
                    break;
                case ADD_ANIMATION:
                    break;
                default:
                    Log.w(TAG, "LoadDataState.changeState: No support state = " + currentState.toString());
            }
            this.mPrevState = currentState;
        }
    }

    @Override
    public void onCreate(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        mAiSharePrefences = getActivity().getSharedPreferences(AiUtils.AI_ALARM, Context.MODE_PRIVATE);

        mAiSharePrefences.registerOnSharedPreferenceChangeListener(mListener);

        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][AlarmClock][START]");
        super.onCreate(sis);
        // ATS log
        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][select_tage][complete]");
        setHasOptionsMenu(true);
        initMember();



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreateView");
        mMainView = inflater.inflate(R.layout.main_alarm_clock, container, false);
        if (DEBUG_FLAG) Log.d(TAG, "onCreateView end");
        return mMainView;
    }

    @Override
    public void onActivityCreated(Bundle sis) {
        if (DEBUG_FLAG) Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(sis);
        mActivity = getActivity();
        mAlarmClockResUtils = new AlarmClockResUtils(mActivity, mMainView);
        mAlarmClockResUtils.initResources();
        initNonUIHandlerThread();
        initUIFuntion();

        mAiManager = AiUtils.getInstance(mActivity);
        if (DEBUG_FLAG) Log.d(TAG, "onActivityCreated end");

        mAiManager.resumeSkipAlarm(mActivity, System.currentTimeMillis());

    }

    @Override
    public void onStart() {
        if (DEBUG_FLAG) Log.d(TAG, "onStart");
        super.onStart();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onResume() {
        if (DEBUG_FLAG) Log.d(TAG, "onResume");
        super.onResume();
        mAlarmClockState.restoreState();
        if (mAlarmClockState.getState() == AlarmClockEnum.INIT) {
            boolean dataReady = ((WorldClockTabControl) mActivity).isAlarmClockCachedDataReady();
            if (DEBUG_FLAG) Log.d(TAG, "onResume: isAlarmClockCachedDataReady = " + dataReady);
            if (dataReady) {
                mAlarmList = (ArrayList<AlarmItem>) ((WorldClockTabControl) mActivity).getCachedAlarmClockList().clone();
                if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][AlarmClock][DATA_READY]");
                mAlarmClockState.changeState(AlarmClockEnum.NORMAL);
            } else {
                // load data from database
                mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);
            }
            initList();
        } else {
            // load data from database
            mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);
        }

        mListItemHeight = ResUtils.getListItemHeight(mActivity);
        if (Global.isSupportAccChinaSense()) {
            setFooter();
        }
        if (DEBUG_FLAG) Log.d(TAG, "onResume end");
    }

    @Override
    public void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        ToastMaster.cancelToast();
        mActivity.closeOptionsMenu();
        mAlarmClockState.changeState(AlarmClockEnum.PAUSE);
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

        if ((mContentResolver != null) && (mAlarmChangeObserver != null)) {
            mContentResolver.unregisterContentObserver(mAlarmChangeObserver);
        }
        if (mAiSharePrefences != null && mListener != null) {
            Log.d(AiUtils.AI_TAG, "unregisterOnSharedPreferenceChangeListener");
            mAiSharePrefences.unregisterOnSharedPreferenceChangeListener(mListener);
        }

        mAlarmClockState.changeState(AlarmClockEnum.END);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!Global.isSupportAccChinaSense()) {
            // create customize menu from inflater or runtime add from code
            inflater.inflate(R.menu.alarmclock_menuitems, menu);
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
    
            if (mAlarmClockState.getState() == AlarmClockEnum.INIT) {
                return;
            }
    
            try {
                if ((mAlarmList == null) || (mAlarmList.size() == 0)) {
                    menu.findItem(R.id.delete_alarm).setEnabled(false);
                } else {
                    menu.findItem(R.id.delete_alarm).setEnabled(true);
                }
    
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!Global.isSupportAccChinaSense()) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.add_alarm:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: add_alarm");
                    if ((mAlarmList != null) && (mAlarmList.size() >= AlertUtils.MAX_ALARM_COUNT)) {
                        mActivity.showDialog(WorldClockTabControl.ALARMCLOCK_REQUEST_ADD);
                    } else {
                        intent = new Intent(mActivity, SetAlarm.class);
                        intent.putExtra(AlarmUtils.ID, -1);
                        startActivityForResult(intent, LAUNCH_SET_ALARM);
                    }
                    break;
                case R.id.delete_alarm:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: delete_alarm");
                    intent = new Intent(mActivity, DeleteAlarm.class);
                    startActivityForResult(intent, LAUNCH_DELETE_ALARM);
                    break;
                case R.id.sort:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: sort");
                    showSorterDialog();
                    break;
                case R.id.alarm_settings:
                    if (DEBUG_FLAG) Log.d(TAG, "onOptionsItemSelected: alarm_settings");
                    intent = new Intent(mActivity, SettingsActivity.class);
                    startActivity(intent);
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

    /**
     * create htc alert dialog to show sort options
     */
    private void showSorterDialog() {
        final AlarmSorter alarmSorter = AlarmSorter.getInstance(mActivity);
        //Used to display the selected parameters of single box
        AlarmSorter.SORT_TYPE oriSortType = alarmSorter.getSortType();
        int oriIndex = 1;
        if (oriSortType != null) {
            oriIndex = oriSortType.ordinal();
        }
        String[] sortType = mActivity.getResources().getStringArray(R.array.sort_types);
        HtcAlertDialog.Builder alertDialogView = new HtcAlertDialog.Builder(mActivity);
        alertDialogView.setTitle(getString(R.string.sort_by));
        alertDialogView.setSingleChoiceItems(sortType, oriIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (DEBUG_FLAG) Log.d(TAG, "sort dialog click which = " + which);
                if (which >= 0 && which < AlarmSorter.SORT_TYPE.values().length) {
                    alarmSorter.setSortType(AlarmSorter.SORT_TYPE.values()[which]);
                    Collections.sort(mAlarmList, alarmSorter);
                    mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_LIST_DATA);
                }
                dismissSortDialog();
            }
        }).setNegativeButton(getString(R.string.bt_cancel_str), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (DEBUG_FLAG) Log.d(TAG, "sort dialog click cancel button");
            }
        });
        dismissSortDialog();
        mSortDialog = alertDialogView.create();
        mSortDialog.show();
    }

    private void dismissSortDialog() {
        if (mSortDialog != null) {
            mSortDialog.dismiss();
            mSortDialog = null;
        }
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
                    case NONUI_MSG_INIT_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_INIT_DATA");
                        initNonUIFunction();
                        break;
                    case NONUI_MSG_LOAD_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mNonUIHandler.handleMessage: NONUI_MSG_LOAD_DATA");
                        updateListViewData();
                        break;
                }
            }
        };
    }

    private void initMember() {
        mAlarmClockState = new AlarmClockState(AlarmClockEnum.INIT);
        mLoadDataState = new LoadDataState(LoadDataEnum.NO_ANIMATION);
        mDelItemList = new ArrayList<Integer>();
        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UI_MSG_UPDATE_LIST_DATA:
                        if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_UPDATE_LIST_DATA");
                        if (mAlarmClockAdapter != null) {
                            mAlarmClockAdapter.changeList(mAlarmList);
                            mAlarmClockAdapter.notifyDataSetChanged();
                            updateAiTipsView(mActivity);
                        }
                        break;
                    case UI_MSG_SCROLL_TO_FIRST_ADD:
                        if (DEBUG_FLAG) Log.d(TAG, "mMainHandler.handleMessage: UI_MSG_SCROLL_TO_FIRST_ADD");
                        if (mListView != null) {
                            if (mAlarmList.size() == 1) {
                                //raymond: no alarm item
                                int position = mAlarmList.size() - 1;
                                ArrayList<Integer> addedItemList = new ArrayList<Integer>();
                                addedItemList.add(position);
                                mAlarmClockAdapter.changeList(mAlarmList);
                                mAlarmClockAdapter.notifyDataSetChanged();
                            } else if (mAlarmClockAdapter.getCount() == mAlarmList.size()) {
                                //back from edit alarm
                                mLoadDataState.changeState(LoadDataEnum.NO_ANIMATION);
                                mAlarmClockAdapter.changeList(mAlarmList);
                                mAlarmClockAdapter.notifyDataSetChanged();
                            } else {
                                int position = mAlarmList.size() - 1;
                                ArrayList<Integer> addedItemList = new ArrayList<Integer>();
                                addedItemList.add(position);
                                mAlarmClockAdapter.changeList(mAlarmList);
                                mAlarmClockAdapter.notifyDataSetChanged();
                                mListView.smoothScrollToPosition(position);
                            }
                            updateAiTipsView(mActivity);
                        }
                        break;
                }
            }
        };

        if (Global.isSupportAccChinaSense()) {
            mAddBtnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ((mAlarmList != null) && (mAlarmList.size() >= AlertUtils.MAX_ALARM_COUNT)) {
                        mActivity.showDialog(WorldClockTabControl.ALARMCLOCK_REQUEST_ADD);
                    } else {
                        Intent intent = new Intent(mActivity, SetAlarm.class);
                        intent.putExtra(AlarmUtils.ID, -1);
                        startActivityForResult(intent, LAUNCH_SET_ALARM);
                    }
                }
            };
            
           mDeleteBtnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mActivity, DeleteAlarm.class);
                    startActivityForResult(intent, LAUNCH_DELETE_ALARM);
                }
            };
        }
        
        mAlarmItemClick = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                AlarmItem ai = null;
                try {
                    ai = mAlarmList.get(position);
                } catch (IndexOutOfBoundsException e) {
                    Log.w(TAG, "HtcAdapterView.onItemClick: e = " + e.toString());
                    return;
                }
                if (mAiManager.checkIsEarlyAlarmId(ai.aId) || mAiManager.checkIsSkipAlarmId(ai.aId)) {
                    showAiDialog(mActivity, ai.aId, position, false);
                } else {
                    Intent intent = new Intent(mActivity, SetAlarm.class);
                    intent.putExtra(AlarmUtils.ID, ai.aId);
                    startActivityForResult(intent, LAUNCH_SET_ALARM);
                }
            }
        };
    }


    /**
     * modify early events data click item and checkbox
     * @param ai early event alarm item
     * @param enabled true : alarm enabled (click item)
     *                    false :alarm unEnabled (click checkbox)
     */
    private void modifyEarlyEventsData(AlarmItem ai, boolean enabled) {
        Uri uri = AlarmUtils.addAlarm(getActivity(), getActivity().getContentResolver());
        if (uri != null) {
            String segment = uri.getPathSegments().get(1);
            mNewAlarmId = Integer.parseInt(segment);
        }
        Log.d(AiUtils.AI_TAG, "modifyEarlyEventsData: " + "mNewAlarmId:" + mNewAlarmId + "forSetAlarm: " + enabled);
        if (mNewAlarmId != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(ai.aAlertTime);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            ai.aEnabled = enabled;
            // click item go to set alarm for add this alarm for normal alarm
            AlarmUtils.setAlarm(getActivity(), mNewAlarmId, ai.aEnabled, hour, ai.aMinutes,
                    new AlarmUtils.DaysOfWeek(ai.aDaysOfWeek), ai.aVibrate, ai.aDescription,
                    ai.aAlert, ai.aSnoozed, ai.aOffAlarm, ai.aRepeatType);
        }
        mAiManager.clearEarlyEventById(ai.aId);
    }

    private void initUIFuntion() {
        mNonUIHandler.sendEmptyMessage(NONUI_MSG_INIT_DATA);
        initKeyListener();
        if (Global.isSupportAccChinaSense()) {
            initFooterMoreList();
        }
    }

    private void initFooterMoreList() {
        mFooterList = new ArrayList<String>();
        mFooterList.add(getString(R.string.sort_by));
        mFooterList.add(getString(R.string.alarm_settings));
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
            mAlarmClockResUtils.setPopUpWindowExpand(configuration, mFooterPopUpWindow);
        }
    }
    
    public void setFooter() {
        if (mActivity == null)
            return;

        MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
        if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_ALARM)) {
            return;
        }

        mAlarmClockResUtils.setHtcFooterButtonResource();
        // set button click listener
        HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
        if (footer != null) {
            footer.findViewById(R.id.footer_btn3).setOnClickListener(mAddBtnClickListener);
            footer.findViewById(R.id.footer_btn2).setOnClickListener(mDeleteBtnClickListener);
            footer.findViewById(R.id.footer_btn4).setVisibility(View.GONE);
            if (!Global.isSupportAccChinaSense()) {
                footer.findViewById(R.id.footer_btn1).setVisibility(View.GONE);
            }
        }

        if (Global.isSupportAccChinaSense()) {
            updateFooterMoreList();
            if (footer != null) {
                if ((mAlarmList == null) || (mAlarmList.size() == 0)) {
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
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: sort");
                            showSorterDialog();
                            break;
                        case 1:
                            if (DEBUG_FLAG) Log.d(TAG, "mFooterPopUpWindow: onItemClick: alarm_settings");
                            Intent intent = new Intent(mActivity, SettingsActivity.class);
                            startActivity(intent);
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
    
    private void initNonUIFunction() {
        mStartWeekDay = AlarmUtils.getCalendarStartWeekday(mActivity);

        // register alarm change observer for tap alarm to cancel on notification bar
        mContentResolver = mActivity.getContentResolver();
        mAlarmChangeObserver = new AlarmChangeObserver();
        if ((mContentResolver != null) && (mAlarmChangeObserver != null)) {
            mContentResolver.registerContentObserver(AlarmColumns.CONTENT_URI, true, mAlarmChangeObserver);
        }
    }

    private void initList() {
        // initial List and Adapter
        mAlarmClockAdapter = new AlarmClockAdapter(mAlarmList);
        mListView = (HtcListView) mMainView.findViewById(R.id.alarms_list);
        mListView.setAdapter(mAlarmClockAdapter);
        mListView.setOnItemClickListener(mAlarmItemClick);

        //Ai Tip view
        mAiTipsContianer = mMainView.findViewById(R.id.ai_tips_container);
        mTxtTip = (TextView) mMainView.findViewById(R.id.ai_alarm_tips);
        Button mBtnCancelTip = (Button) mMainView.findViewById(R.id.ai_cancel_button);
        mBtnCancelTip.setOnClickListener(new TipCancelListener());
        updateAiTipsView(mActivity);
    }

    private class TipCancelListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            mIsClickTipsCancel = true;
            mAiTipsContianer.setVisibility(View.GONE);
        }
    }

    private void updateAiTipsView(Context context) {
        ArrayList<AlarmItem> skipAlarms = mAiManager.getSkipAlarms();
        ArrayList<AlarmItem> earlyAlarms = mAiManager.getEarlyEventAlarms();

        boolean hasAiAlarm;
        if ((skipAlarms == null || skipAlarms.isEmpty()) && (earlyAlarms == null || earlyAlarms.isEmpty())) {
            hasAiAlarm = false;
        } else {
            hasAiAlarm = true;
        }
        if (!mIsClickTipsCancel && hasAiAlarm) {
            String strTips = null;
            if (skipAlarms == null) {
                strTips = context.getString(R.string.ai_tips_add);
            } else {
                strTips = context.getString(R.string.ai_tips_modify);
            }
            mAiTipsContianer.setVisibility(View.VISIBLE);
            mTxtTip.setText(strTips);
        } else {
            mAiTipsContianer.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateListViewData() {
        ArrayList<AlarmItem> list = AlarmUtils.getAlarmListData(mActivity);
        if(list != null) {
            mAlarmList = (ArrayList<AlarmItem>)list.clone();
        }
        if (mAlarmClockState.getState() == AlarmClockEnum.INIT) {
            if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][AlarmClock][DATA_READY]");
        }
        mAlarmClockState.changeState(AlarmClockEnum.NORMAL);
        // for add and delete alarm need to update actionbar
        if (mLoadDataState.getState() == LoadDataEnum.NO_ANIMATION) {
            mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_LIST_DATA);
        } else if (mLoadDataState.getState() == LoadDataEnum.ADD_ANIMATION) {
            mMainHandler.sendEmptyMessage(UI_MSG_SCROLL_TO_FIRST_ADD);
        }
        mActivity.invalidateOptionsMenu();
        mLoadDataState.changeState(LoadDataEnum.NO_ANIMATION);
    }

    private void removeAllHandlerMessages() {
        if (mNonUILooper != null) {
            mNonUILooper.quit();
        }

        mMainHandler.removeMessages(UI_MSG_UPDATE_LIST_DATA);
        mMainHandler.removeMessages(UI_MSG_SCROLL_TO_FIRST_ADD);
        mNonUIHandler.removeMessages(NONUI_MSG_INIT_DATA);
        mNonUIHandler.removeMessages(NONUI_MSG_LOAD_DATA);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mAlarmClockResUtils != null) {
            mAlarmClockResUtils.resetLayout();
        }
        if (Global.isSupportAccChinaSense() && mFooterPopUpWindow != null) {
            if (mFooterPopUpWindow.isShowing()) {
                mFooterPopUpWindow.dismissWithoutAnimation();
            }
            if (mAlarmClockResUtils != null) {
                mAlarmClockResUtils.setPopUpWindowExpand(newConfig, mFooterPopUpWindow);
            }
        }
    }

    private void calculateDigitalMaxDisplay() {
        mMaxTimeDisplay = DigitalClock.getAlarmMaxTimeDisplay(mActivity, mAlarmList);
        mMaxAmPmDisplay = DigitalClock.getMaxAMPMDisplay(mActivity);
        mMaxDigitalDisplay = DigitalClock.getMaxDigitalDisplay(mActivity, mMaxTimeDisplay, mMaxAmPmDisplay);
    }
    
    class AlarmClockAdapter extends BaseAdapter {
        protected ArrayList<AlarmItem> mItems = null;
        protected LayoutInflater mInflater;
        protected View mLayout;

        public AlarmClockAdapter(ArrayList<AlarmItem> list) {
            if (list != null) {
                mItems = new ArrayList<AlarmItem>(list);
            }
            mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            calculateDigitalMaxDisplay();
            if (Global.isSupportAccChinaSense()) {
                MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
                if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_ALARM)) {
                    return;
                }
                HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();

                if (footer != null) {
                    if ((mItems == null) || (mItems.size() == 0)) {
                        footer.findViewById(R.id.footer_btn2).setEnabled(false);
                    } else {
                        footer.findViewById(R.id.footer_btn2).setEnabled(true);
                    }
                }
            }
        }

        public void changeList(ArrayList<AlarmItem> list) {
            if (list != null) {
                mItems = new ArrayList<AlarmItem>(list);
            }
            calculateDigitalMaxDisplay();
            if (Global.isSupportAccChinaSense()) {
                MyTabAdapter adapter = (MyTabAdapter) ((WorldClockTabControl) mActivity).getCarouselTab().getAdapter();
                if (adapter != null) {
                    if (!adapter.getCurrentTabTag().equals(CarouselTab.TAB_ALARM)) {
                        return;
                    }
                    HtcFooter footer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
                    if (footer != null) {
                        if ((mItems == null) || (mItems.size() == 0)) {
                            footer.findViewById(R.id.footer_btn2).setEnabled(false);
                        } else {
                            footer.findViewById(R.id.footer_btn2).setEnabled(true);
                        }
                    }
                }
            }
        }

        public void addItem(int position, AlarmItem data) {
            mItems.add(position, data);
        }

        public void removeItem(int position) {
            mItems.remove(position);
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
                mLayout = mInflater.inflate(R.layout.specific_alarm_time, null);
            }

            final AlarmItem ai;
            try {
                ai = mItems.get(position);
            } catch (Exception e) {
                Log.w(TAG, "AlarmClockAdapter.getView: e = " + e.toString());
                return mLayout;
            }

            if (ai == null) {
                return mLayout;
            }

            final int id = ai.aId;
            final AlarmUtils.DaysOfWeek daysOfWeek = new AlarmUtils.DaysOfWeek(ai.aDaysOfWeek);
            final boolean enabled = ai.aEnabled;
            final int positionId = position;

            /* reset description layout width for description text is too short in landscape mode */
            ResUtils res = new ResUtils(mActivity, mLayout);
            if (res != null) {
                res.setLayout(R.id.am_pm
                        , 0, 0,
                        0, R.dimen.digital_alarm_am_pm_marginTop);
            }

            // calculate all displays for digital clock
            TextView timeDisplay = (TextView) mLayout.findViewById(R.id.timeDisplay);
            timeDisplay.setWidth(mMaxTimeDisplay);
            timeDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDefaultFontSize(mActivity, R.dimen.time_display_size));
            TextView ampmDisplay = (TextView) mLayout.findViewById(R.id.am_pm);
            ampmDisplay.setWidth(mMaxAmPmDisplay);
            ampmDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDefaultFontSize(mActivity, R.dimen.am_pm_size));
            
            timeDisplay.setMaxLines(1);
            timeDisplay.setEllipsize(TruncateAt.MARQUEE);
            timeDisplay.setHorizontalFadingEdgeEnabled(true);
            ampmDisplay.setMaxLines(1);
            ampmDisplay.setEllipsize(TruncateAt.MARQUEE);
            ampmDisplay.setHorizontalFadingEdgeEnabled(true);
            
            DigitalClock digitalClock = (DigitalClock) mLayout.findViewById(R.id.common_digital_clock_btn);
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
                }
                digitalClock.setLayoutParams(lp_digital);
            }
            // end of calculate all displays for digital clock
            
            AlarmClockResUtils.setDigitalClock(mActivity, mLayout, ai.aHour, ai.aMinutes);
            AlarmClockResUtils.setDescription(mActivity, mLayout, ai.aDescription);
            AlarmClockResUtils.setDaysOfWeek(mActivity, mLayout, mStartWeekDay, daysOfWeek);
            TextView description = (TextView) mLayout.findViewById(R.id.description);

            CheckBox cb = (CheckBox) mAlarmClockResUtils.getCheckBox(mLayout);
            //set ai alarm blue color for digitalClock description checkBox
            if (mAiManager.checkIsEarlyAlarmId(ai.aId) || mAiManager.checkIsSkipAlarmId(ai.aId)) {
                digitalClock.setDigitalClockAiColor();
                description.setTextColor(getResources().getColor(R.color.ai_tip_background));
                if (mAiManager.checkIsSkipAlarmId(ai.aId)) {
                    description.setText(AiUtils.getInstance(mActivity).getAiSkipResumeTxt());
                    description.setAlpha(1.0f);
                    cb.setChecked(false);
                } else {
                    cb.setChecked(enabled);
//                    AlarmUtils.enableAheadNotification(mActivity, id, true);  //add juan
                }
                CompoundButtonCompat.setButtonTintList(cb, ColorStateList.valueOf(getResources().getColor(R.color.ai_tip_background)));
            } else {
                description.setTextColor(getResources().getColor(R.color.light_grey));
                cb.setChecked(enabled);
//                AlarmUtils.enableAheadNotification(mActivity, id, true);  //add juan
            }

            cb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAlarmList == null) {
                        return;
                    }
                    boolean checked = ((CheckBox) v).isChecked();
                    if (mAiManager.checkIsSkipAlarmId(ai.aId)) {
                        checked = false;
                    }
                    if (checked) {
                        if (DEBUG_FLAG) Log.d(TAG, "AlarmClockAdapter.getView.onClick: checked = true");
                        // ATS log
                        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][set_time][complete]");
                        try {
                            mAlarmList.get(positionId).aEnabled = true;
                            // fix slow operation for strict mode enabled
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    AlarmUtils.enableAlarm(mActivity, id, true);
                                    AlarmUtils.enableAheadNotification(mActivity, id, true);  //add juan
                                }
                            }).start();
                            SetAlarm.popAlarmSetToast(mActivity, ai.aHour, ai.aMinutes, daysOfWeek, ai.aRepeatType);
                        } catch (Exception e) {
                            Log.w(TAG, "setOnClickListener.onClick: e = " + e.toString());
                        }
                    } else {
                        if (DEBUG_FLAG) Log.d(TAG, "AlarmClockAdapter.getView.onClick: checked = false");
                        // ATS log
                        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][alarm_schedule][delete]");
                        if (mAiManager.checkIsSkipAlarmId(id)) {
                            if (mNotAskShowAgain) {
                                handleSkipAlarm(id, positionId, true);
                            } else {
                                showAiDialog(mActivity, id, positionId, true);
                            }
                        } else if (mAiManager.checkIsEarlyAlarmId(id)) {
                            if (mNotAskShowAgain) {
                                handleEarlyEventAlarm(id, positionId, true);
                            } else {
                                showAiDialog(mActivity, id, positionId, true);
                            }
                        } else {
                            try {
                                mAlarmList.get(positionId).aEnabled = false;
                                // fix slow operation for strict mode enabled
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlarmUtils.enableAlarm(mActivity, id, false);
                                    }
                                }).start();
                            } catch (Exception e) {
                                Log.w(TAG, "setOnClickListener.onClick: e = " + e.toString());
                            }
                        }
                    }
                }
            });

            LinearLayout item = (LinearLayout) mLayout.findViewById(R.id.alarm_time_description_layout);
            
            LayoutParams lp = (LayoutParams) item.getLayoutParams();
            lp.height = mListItemHeight;
            item.setLayoutParams(lp);
            
            if ((Global.isSupportAccChinaSense()) && (ai.aRepeatType == SetAlarm.RepeatTypeEnum.SKIPHOLIDAY.ordinal())) {
                LinearLayout daysLayout = (LinearLayout) mLayout.findViewById(R.id.days_layout);
                if (daysLayout != null) {
                    daysLayout.setVisibility(View.GONE);
                }
                TextView skipHoliday = (TextView) mLayout.findViewById(R.id.skip_holiday);
                if (skipHoliday != null) {
                    skipHoliday.setVisibility(View.VISIBLE);
                    skipHoliday.setText(mActivity.getResources().getTextArray(R.array.repeat_option)[RepeatTypeEnum.SKIPHOLIDAY.ordinal()].toString());
                }
            } else {
                TextView skipHoliday = (TextView) mLayout.findViewById(R.id.skip_holiday);
                if (skipHoliday != null) {
                    skipHoliday.setVisibility(View.GONE);
                }
                LinearLayout daysLayout = (LinearLayout) mLayout.findViewById(R.id.days_layout);
                if (daysLayout != null) {
                    daysLayout.setVisibility(View.VISIBLE);
                }
            }

            return mLayout;
        }
    }

    /**
     * show ai dialog to handle ai data
     * @param context context
     * @param id ai alarm id
     * @param positionId alarm list position
     * @param clickCheckBox is click checkBox true:checkBox false:click list item
     */
    private void showAiDialog(final Context context, final int id, final int positionId, final boolean clickCheckBox) {
        Log.d(AiUtils.AI_TAG, "showAiDialog: " + "id: " + id + "positionId: " + positionId + "clickCheckBox: " + clickCheckBox);
        HtcAlertDialog.Builder chkMsgBuilder = new HtcAlertDialog.Builder(
                context)
                .setTitle(R.string.ai_dialog_title)
                .setMessage(R.string.ai_dialog_message)
                .setNeutralButton(R.string.ai_dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(AiUtils.AI_TAG, "showAiDialog onClick Neutral button");
                        mAlarmList.get(positionId).aEnabled = true;
                        mAlarmClockAdapter.notifyDataSetChanged();
                    }
                })
                .setPositiveButton(R.string.ai_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.d(AiUtils.AI_TAG, "showAiDialog onClick Positive button item id: " + id);
                        if (mAiManager.checkIsSkipAlarmId(id)) {
                            handleSkipAlarm(id, positionId, clickCheckBox);
                        } else {
                            handleEarlyEventAlarm(id, positionId, clickCheckBox);
                        }
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mAlarmList.get(positionId).aEnabled = true;
                        mAlarmClockAdapter.notifyDataSetChanged();
                    }
                });
        if (clickCheckBox) {
            chkMsgBuilder.setCheckBox(getString(R.string.ai_dialog_not_ask), false, new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                mNotAskShowAgain = true;
                            } else {
                                mNotAskShowAgain = false;
                            }
                        }
                    },
                    true);
        }
        chkMsgBuilder.create();
        chkMsgBuilder.show();
    }


    private void handleSkipAlarm(final int id, final int positionId, boolean clickCheckBox) {
        Log.d(AiUtils.AI_TAG, "handleSkipAlarm: " + "id: " + id + "positionId: " + positionId + "clickCheckBox: " + clickCheckBox);
        //update data
        mAiManager.clearSkipAlarmById(id);
        if (clickCheckBox) {
            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AlarmUtils.enableAlarm(mActivity, id, true);
                    }
                }).start();
            } catch (Exception e) {
                Log.w(TAG, "setOnClickListener.onClick: e = " + e.toString());
            }
        } else {
            //go to set alarm
            AlarmItem ai = null;
            try {
                ai = mAlarmList.get(positionId);
            } catch (IndexOutOfBoundsException e) {
                Log.w(AiUtils.AI_TAG, "HtcAdapterView.onItemClick: e = " + e.toString());
                return;
            }
            AlarmUtils.setNextAlert(mActivity);
            Intent intent = new Intent(mActivity, SetAlarm.class);
            intent.putExtra(AlarmUtils.ID, ai.aId);
            startActivityForResult(intent, LAUNCH_SET_ALARM);
        }

    }

    private void handleEarlyEventAlarm(final int id, final int positionId, final boolean clickCheckBox) {
        Log.d(AiUtils.AI_TAG, "handleEarlyEventAlarm: " + "id: " + id + "positionId: " + positionId + "clickCheckBox: " + clickCheckBox);
        AlarmItem ai = null;
        try {
            ai = mAlarmList.get(positionId);
        } catch (IndexOutOfBoundsException e) {
            Log.w(AiUtils.AI_TAG, "HtcAdapterView.onItemClick: e = " + e.toString());
            return;
        }
        // fix slow operation for strict mode enabled
        final AlarmItem finalAi = ai;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (clickCheckBox) {
                    Log.d(AiUtils.AI_TAG, "handleEarlyEventAlarm click list item CheckBox");
                    mGoToSetAlarm = false;
                    modifyEarlyEventsData(finalAi, false);
                } else {
                    Log.d(AiUtils.AI_TAG, "handleEarlyEventAlarm click list item");
                    mGoToSetAlarm = true;
                    modifyEarlyEventsData(finalAi, true);
                }
                AlarmUtils.enableAlarm(mActivity, id, false);
            }
        }).start();
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
    
    private class AlarmChangeObserver extends ContentObserver {
        public AlarmChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            // check state
            if ((mAlarmClockState.getState() == AlarmClockEnum.PAUSE) ||
                (mAlarmClockState.getState() == AlarmClockEnum.END)) {

                if (DEBUG_FLAG) Log.d(TAG, "AlarmChangeObserver.onChange: meet PAUSE or END state");
                return;
            }
            Log.d(AiUtils.AI_TAG, "AlarmChangeObserver onChange: " + "mGoToSetAlarm :" + mGoToSetAlarm);
            if (mGoToSetAlarm) {
                if (mNewAlarmId != 0) {
                    Intent intent = new Intent(mActivity, SetAlarm.class);
                    intent.putExtra(AlarmUtils.ID, mNewAlarmId);
                    startActivityForResult(intent, LAUNCH_SET_ALARM);
                    //reset id and tag
                    mNewAlarmId = 0;
                    mGoToSetAlarm = false;
                }
            } else {
                if (mAlarmClockAdapter != null) {
                    if (mLoadDataState.getState() == LoadDataEnum.NO_ANIMATION) {
                        mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);
                    }
                    mAlarmClockState.changeState(AlarmClockEnum.DATA_CHANGE);
                }
            }
        }
    }

    private void initKeyListener() {
        mMainView.setFocusableInTouchMode(true);
        mMainView.requestFocus();
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
        switch (requestCode) {
            case LAUNCH_SET_ALARM:
                if (resultCode == Activity.RESULT_OK) {
                    mLoadDataState.changeState(LoadDataEnum.ADD_ANIMATION);
                }
                break;
        }
    }
}
