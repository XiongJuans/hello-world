package com.htc.android.worldclock.alarmclock;

import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.aiservice.AiHtcDeleteButton;
import com.htc.android.worldclock.aiservice.AiUtils;
import com.htc.android.worldclock.alarmclock.SetAlarm.RepeatTypeEnum;
import com.htc.android.worldclock.utils.DigitalClock;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcSkinUtils;
import com.htc.android.worldclock.utils.HtcStorageChecker;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcDeleteButton;
import com.htc.lib1.cc.widget.HtcListView;
import com.htc.lib1.cc.widget.HtcListView.DeleteAnimationListener;
import com.htc.lib1.theme.ThemeType;

public class DeleteAlarm extends Activity {
    private static final String TAG = "WorldClock.DeleteAlarm";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    public static final String DELETE_ALARM_INDEX_KEY = "DeleteAlarmIndex";
    private final int UI_MSG_UPDATE_LIST_DATA = 0x0001;
    private final int NONUI_MSG_LOAD_DATA = 0x0100;

    private ArrayList<AlarmItem> mAlarmList;
    private HtcListView mListView;
    private DeleteAlarmAdapter mDeleteAlarmAdapter;
    private DeleteAlarmResUtils mDeleteAlarmResUtils;

    private Looper mNonUILooper = null;
    private Handler mNonUIHandler = null;

    private boolean[] mDeletedIndex;
    private int mDeleteNumber = 0;
    private int mStartWeekDay = 1; // 1 -- Sunday; 2 -- Monday
    private ArrayList<Integer> mDelItemList = new ArrayList<Integer>();
    
    // Htc font scale
    private boolean mHtcFontscale = false;
    
    private int mListItemHeight;
    private int mMaxTimeDisplay;
    private int mMaxAmPmDisplay;
    private int mMaxDigitalDisplay;
    private AiUtils mAiManager;

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
    
    protected static enum DeleteAlarmEnum {
        INIT, NORMAL, PAUSE, END
    }

    // state control
    private final DeleteAlarmState mDeleteAlarmState = new DeleteAlarmState(DeleteAlarmEnum.INIT);

    private class DeleteAlarmState {
        private DeleteAlarmEnum mPrevState;

        DeleteAlarmState(DeleteAlarmEnum initState) {
            changeState(initState);
        }

        public DeleteAlarmEnum getState() {
            return mPrevState;
        }

        public void changeState(DeleteAlarmEnum currentState) {
            if (DEBUG_FLAG) Log.d(TAG, "DeleteAlarmState.changeState: " + this.mPrevState + " -> " + currentState.toString());
            switch (currentState) {
                case INIT:
                    break;
                case NORMAL:
                    break;
                case PAUSE:
                    break;
                case END:
                    break;
                default:
                    Log.w(TAG, "DeleteAlarmState.changeState: No support state = " + currentState.toString());
            }
            this.mPrevState = currentState;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][DeleteAlarm][START]");
        mHtcFontscale = HtcSkinUtils.initHtcFontScale(this);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(icicle);
        setContentView(R.layout.main_delete_alarm);
        mDeleteAlarmResUtils = new DeleteAlarmResUtils(this, null);
        mDeleteAlarmResUtils.initResources();
        initNonUIHandlerThread();
        initFuntion();

        // load data from database
        mNonUIHandler.sendEmptyMessage(NONUI_MSG_LOAD_DATA);

        mAiManager = AiUtils.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        HtcStorageChecker.checkStorageFull(this);
        mListItemHeight = ResUtils.getListItemHeight(this);
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(DeleteAlarm.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
    }

    @Override
    public void onPause() {
        mDeleteAlarmState.changeState(DeleteAlarmEnum.PAUSE);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        removeAllHandlerMessages();
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        mDeleteAlarmState.changeState(DeleteAlarmEnum.END);
        super.onDestroy();
    }

    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case UI_MSG_UPDATE_LIST_DATA:
                    if (mDeleteAlarmAdapter != null) {
                        mDeleteAlarmAdapter.changeList(mAlarmList);
                        mDeleteAlarmAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    private final void initNonUIHandlerThread() {
        HandlerThread nonUIHandlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        nonUIHandlerThread.start();
        mNonUILooper = nonUIHandlerThread.getLooper();
        mNonUIHandler = new Handler(mNonUILooper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case NONUI_MSG_LOAD_DATA:
                        updateUI();
                        break;
                }
            }
        };
    }

    private void initFuntion() {
        // set Button Click Listener
        findViewById(R.id.cmd_bar_btn_1).setOnClickListener(mDeleteBtnClickListener);
        findViewById(R.id.cmd_bar_btn_2).setOnClickListener(mCancelBtnClickListener);
        mStartWeekDay = AlarmUtils.getCalendarStartWeekday(this);

        // initial List and Adapter
        mAlarmList = new ArrayList<AlarmItem>();
        mDeleteAlarmAdapter = new DeleteAlarmAdapter(mAlarmList);
        mListView = (HtcListView) findViewById(R.id.alarms_list);
        mListView.setAdapter(mDeleteAlarmAdapter);
        mListView.setDeleteAnimationListener(new DeleteAnimationListener() {

            @Override
            public void onAnimationEnd() {
                Intent intent = new Intent();
                intent.putIntegerArrayListExtra(DELETE_ALARM_INDEX_KEY, mDelItemList);
                DeleteAlarm.this.setResult(RESULT_OK, intent);
                DeleteAlarm.this.finish();
            }

            @Override
            public void onAnimationStart() {

            }

			@Override
			public void onAnimationUpdate() {
				mDeleteAlarmAdapter.notifyDataSetChanged();
			}

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // create customize menu from inflater or runtime add from code
        getMenuInflater().inflate(R.menu.deletealarm_menuitems, menu);
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
        else if (mDeleteNumber == mAlarmList.size()) {
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

    private void updateUI() {
        updateListViewData();
    }

    @SuppressWarnings("unchecked")
    private void updateListViewData() {
        ArrayList<AlarmItem> list =  AlarmUtils.getAlarmListData(this);
        if(list != null) {
            mAlarmList = (ArrayList<AlarmItem>) list.clone();
        }

        if (mDeleteAlarmState.getState() == DeleteAlarmEnum.INIT) {
            if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][DeleteAlarm][DATA_READY]");
        }
        mDeleteAlarmState.changeState(DeleteAlarmEnum.NORMAL);
        updateDeleteAlarm();
        mMainHandler.sendEmptyMessage(UI_MSG_UPDATE_LIST_DATA);
    }

    private void removeAllHandlerMessages() {
        if (mNonUILooper != null) {
            mNonUILooper.quit();
        }

        mMainHandler.removeMessages(UI_MSG_UPDATE_LIST_DATA);
        mNonUIHandler.removeMessages(NONUI_MSG_LOAD_DATA);
    }

    private final View.OnClickListener mDeleteBtnClickListener = new View.OnClickListener() {
        LinkedList<Integer> aId;

        @Override
        public void onClick(View v) {
            aId = new LinkedList<Integer>();
            if ((mAlarmList == null) || (mDeletedIndex == null)) {
                return;
            }

            // DB access needs to run in non-UI thread
            ArrayList<AlarmItem> alarmList = mAlarmList;

            int i;
            for (i = 0; i < mDeletedIndex.length; i++) {
                if (mDeletedIndex[i] == true) {
                    try {
                        aId.add(alarmList.get(i).aId);
                        mDelItemList.add(i);
                    } catch (IndexOutOfBoundsException e) {
                        Log.w(TAG, "OnClickListener.Thread.run: e = " + e.toString());
                        break;
                    }
                }
            }

            ArrayList<Integer> delItemList = new ArrayList<Integer>();

            for (i = mDeletedIndex.length - 1; i >= 0; i--) {
                if (mDeletedIndex[i] == true) {
                    delItemList.add(i);
                    mDeleteAlarmAdapter.removeItem(i);
                    mDeletedIndex[i] = false;
                }
            }

            new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < aId.size(); i++) {
                        AlarmUtils.deleteAlarm(DeleteAlarm.this, aId.get(i));
                        if (mAiManager.checkIsSkipAlarmId(aId.get(i))) {
                            Log.d(AiUtils.AI_TAG, "clear skip alarm id" + aId.get(i));
                            mAiManager.clearSkipAlarmById(aId.get(i));
                        } else if (mAiManager.checkIsEarlyAlarmId(aId.get(i))) {
                            Log.d(AiUtils.AI_TAG, "clear early event alarm id" + aId.get(i));
                            mAiManager.clearEarlyEventById(aId.get(i));
                        }
                    }
                    AlarmUtils.setNextAlert(DeleteAlarm.this);
                }
            }.start();

            mListView.disableTouchEventInAnim();
            mListView.setDelPositionsList(mDelItemList);
        }
    };

    private final View.OnClickListener mCancelBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DeleteAlarm.this.setResult(RESULT_CANCELED);
            DeleteAlarm.this.finish();
        }
    };

    private void updateDeleteAlarm() {
        if ((mAlarmList == null) || (mAlarmList.size() == 0)) {
            return;
        }

        mDeletedIndex = new boolean[mAlarmList.size()];
        for (int i = 0; i < mDeletedIndex.length; i++) {
            mDeletedIndex[i] = false;
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                int position, long id) {
                if (mDeletedIndex == null) {
                    return;
                }

                mDeletedIndex[position] = !mDeletedIndex[position];
                HtcDeleteButton cb = (HtcDeleteButton) v.findViewById(R.id.function_delete);
                cb.setChecked(mDeletedIndex[position]);
                handleDeleteCount(mDeletedIndex[position]);
            }
        });
    }

    private void handleDeleteCount(boolean isChecked) {

        if (isChecked) {
            mDeleteNumber++;
            if (mDeleteNumber == 1) {
                mDeleteAlarmResUtils.setDeleteButtonEnabled(true);
            }
        } else {
            mDeleteNumber--;
            if (mDeleteNumber == 0) {
                mDeleteAlarmResUtils.setDeleteButtonEnabled(false);
            }
        }
        mDeleteAlarmResUtils.setDeleteButtonText(mDeleteNumber);
    }

    private void calculateDigitalMaxDisplay() {
        mMaxTimeDisplay = DigitalClock.getAlarmMaxTimeDisplay(this, mAlarmList);
        mMaxAmPmDisplay = DigitalClock.getMaxAMPMDisplay(this);
        mMaxDigitalDisplay = DigitalClock.getMaxDigitalDisplay(this, mMaxTimeDisplay, mMaxAmPmDisplay);
    }
    
    class DeleteAlarmAdapter extends BaseAdapter {
        protected ArrayList<?> mItems = null;
        protected LayoutInflater mInflater;
        protected View mLayout;
        protected boolean[] mIsSet;
        protected boolean[] mIsSet2;

        public DeleteAlarmAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            calculateDigitalMaxDisplay();
        }

        public DeleteAlarmAdapter(ArrayList<?> list) {
            if (list != null) {
                mItems = (ArrayList<?>) list.clone();
            }
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mIsSet = new boolean[7];
            mIsSet2 = new boolean[7];
            calculateDigitalMaxDisplay();
        }

        public void changeList(ArrayList<?> list) {
            if (list != null) {
                mItems = (ArrayList<?>) list.clone();
            }
            calculateDigitalMaxDisplay();
        }

        public void removeItem(int position) {
            mItems.remove(position);
            //adapter has changed , ListView need to receive a notification.
            this.notifyDataSetChanged();
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

            AlarmItem ai;
            try {
                ai = (AlarmItem) mItems.get(position);
            } catch (Exception e) {
                Log.w(TAG, "DeleteAlarmAdapter.getView: e = " + e.toString());
                return mLayout;
            }

            if (ai == null) {
                return mLayout;
            }

            final AlarmUtils.DaysOfWeek daysOfWeek = new AlarmUtils.DaysOfWeek(ai.aDaysOfWeek);
            final int positionId = position;

            /* reset description layout width for description text is too short in landscape mode */
            ResUtils res = new ResUtils(DeleteAlarm.this, mLayout);

            // calculate all displays for digital clock
            TextView timeDisplay = (TextView) mLayout.findViewById(R.id.timeDisplay);
            timeDisplay.setWidth(mMaxTimeDisplay);
            timeDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDefaultFontSize(DeleteAlarm.this,R.dimen.time_display_size));
            TextView ampmDisplay = (TextView) mLayout.findViewById(R.id.am_pm);
            ampmDisplay.setWidth(mMaxAmPmDisplay);
            ampmDisplay.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDefaultFontSize(DeleteAlarm.this,R.dimen.am_pm_size));
            
            timeDisplay.setSingleLine(true);
            timeDisplay.setEllipsize(TruncateAt.MARQUEE);
            timeDisplay.setHorizontalFadingEdgeEnabled(true);
            ampmDisplay.setSingleLine(true);
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
            
            AlarmClockResUtils.setDigitalClock(DeleteAlarm.this, mLayout, ai.aHour, ai.aMinutes);
            TextView description = (TextView) mLayout.findViewById(R.id.description);
            AlarmClockResUtils.setDescription(DeleteAlarm.this, mLayout, ai.aDescription);
            AlarmClockResUtils.setDaysOfWeek(DeleteAlarm.this, mLayout, mStartWeekDay, daysOfWeek);
            AiHtcDeleteButton db = (AiHtcDeleteButton) mDeleteAlarmResUtils.setDeleteButton(mLayout, mDeletedIndex[positionId]);

            //set ai color for digitalClock, description, checkBox.
            if (mAiManager.checkIsEarlyAlarmId(ai.aId) || mAiManager.checkIsSkipAlarmId(ai.aId)) {
                digitalClock.setDigitalClockAiColor();

                description.setTextColor(getResources().getColor(R.color.ai_tip_background, null));
                if (mAiManager.checkIsSkipAlarmId(ai.aId)) {
                    description.setText(AiUtils.getInstance(DeleteAlarm.this).getAiSkipResumeTxt());
                    description.setAlpha(1.0f);
                }

                db.setAiBackgroundColor();
            } else {
                description.setTextColor(getResources().getColor(R.color.light_grey, null));
                db.restoreAiColor();
            }

            db.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDeletedIndex == null) {
                        return;
                    }

                    mDeletedIndex[positionId] = ((HtcDeleteButton) v).isChecked();
                    handleDeleteCount(mDeletedIndex[positionId]);
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
                    skipHoliday.setText(getResources().getTextArray(R.array.repeat_option)[RepeatTypeEnum.SKIPHOLIDAY.ordinal()].toString());
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

    private void handleDeleteAllCount(boolean isChecked) {

        if ((mDeletedIndex != null) && (mDeletedIndex.length > 0)) {
            for (int i = 0; i < mDeletedIndex.length; i++) {
                if (mDeletedIndex[i] != isChecked) {
                    mDeletedIndex[i] = isChecked;
                    handleDeleteCount(isChecked);
                }
            }
            mDeleteAlarmAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        HtcSkinUtils.initHtcFontScale(this);
        mDeleteAlarmResUtils.switchTheme(newConfig);
    }
}
