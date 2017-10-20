package com.htc.android.worldclock.aiservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.htc.android.worldclock.alarmclock.AlarmItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class AlarmSorter implements Comparator<AlarmItem> {
    private static final String TAG = "WorldClock.AlarmSorter";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    public static final String SHARED_PREF_SORT_TYPE = "sort_type";
    public static final String SHARED_PREF_SORT_TYPE_ID = "sort_type_name";

    private static final int LESS = -1;
    private static final int MORE = 1;
    private static final int EQUAL = 0;
    private List<Integer> mSkipListIds;
    private List<Integer> mEarlyEventIds;
    private SORT_TYPE mSortType = SORT_TYPE.Time_Created;
    private Context mContext;
    private static AlarmSorter mInstance;

    public enum SORT_TYPE {
        Time,
        Time_Created,
    }

    public synchronized static AlarmSorter getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlarmSorter(context);
        }
        return mInstance;
    }

    private AlarmSorter(Context context) {
        this.mContext = context;
        //restore settings from preference
        restoreSetting();
    }

    private void restoreSetting() {
        try {
            SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREF_SORT_TYPE,
                    Context.MODE_PRIVATE);
            mSortType = SORT_TYPE.valueOf(sp.getString(SHARED_PREF_SORT_TYPE_ID, SORT_TYPE.Time_Created.name()));
        } catch (Exception e) {
            mSortType = SORT_TYPE.Time_Created;
            e.printStackTrace();
        }
        if (DEBUG_FLAG) Log.d(TAG, "restored sort type: " + mSortType.name());

    }

    public synchronized void setSortType(SORT_TYPE sortType) {
        if (mSortType != sortType) {
            mSortType = sortType;
            saveSetting();
        }
    }

    public SORT_TYPE getSortType() {
        return mSortType;
    }

    private void saveSetting() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(SHARED_PREF_SORT_TYPE, Context.MODE_PRIVATE).edit();
        editor.putString(SHARED_PREF_SORT_TYPE_ID, mSortType.name());
        editor.apply();
        if (DEBUG_FLAG) Log.d(TAG, "saved sort type: " + mSortType.name());
    }

    /**
     * add skipList item's id to mSkipListIds and add earlyEventList item's id tomEarlyEventIds .
     *
     * @param skipList       skip alarm list
     * @param earlyEventList early event list
     */
    public void convert(List<AlarmItem> skipList, List<AlarmItem> earlyEventList) {
        mSkipListIds = new ArrayList<>();
        if (skipList != null) {
            for (AlarmItem item : skipList) {
                mSkipListIds.add(item.aId);
            }
        }
        mEarlyEventIds = new ArrayList<>();
        if (earlyEventList != null) {
            for (AlarmItem item : earlyEventList) {
                mEarlyEventIds.add(item.aId);
            }
        }
    }

    /**
     * to compare two AlarmItem as Ai alarms compare by time
     * not Ai alarms compare by sort type
     * @param lAlarm the left item.
     * @param rAlarm the right item.
     */
    @Override
    public int compare(AlarmItem lAlarm, AlarmItem rAlarm) {
        boolean lAiAlarm = mSkipListIds.contains(lAlarm.aId) || mEarlyEventIds.contains(lAlarm.aId);
        boolean rAiAlarm = mSkipListIds.contains(rAlarm.aId) || mEarlyEventIds.contains(rAlarm.aId);
        if (lAiAlarm && rAiAlarm) {
            return clockCompareByTime(lAlarm, rAlarm);
        } else if (lAiAlarm) {
            return LESS;
        } else if (rAiAlarm) {
            return MORE;
        } else {
            switch (mSortType) {
                case Time:
                    return clockCompareByTime(lAlarm, rAlarm);
                case Time_Created:
                    return clockCompareByTimeCreate(lAlarm, rAlarm);
            }
        }
        return EQUAL;
    }

    /**
     * compare two AlarmItems by time create.
     *
     * @param lAlarm lAlarm
     * @param rAlarm rAlarm
     * @return more or less
     */
    private int clockCompareByTimeCreate(AlarmItem lAlarm, AlarmItem rAlarm) {
        final int lId = lAlarm.aId;
        final int rId = rAlarm.aId;

        return lId < rId ? LESS : ((lId == rId) ? EQUAL : MORE);
    }

    /**
     * compare two AlarmItems by time.
     *
     * @param lAlarm lAlarm
     * @param rAlarm rAlarm
     * @return more or less
     */
    private int clockCompareByTime(AlarmItem lAlarm, AlarmItem rAlarm) {
        if (lAlarm.aHour < rAlarm.aHour) {
            return LESS;
        } else if (lAlarm.aHour > rAlarm.aHour) {
            return MORE;
        } else {
            if (lAlarm.aMinutes < rAlarm.aMinutes) {
                return LESS;
            } else if (lAlarm.aMinutes > rAlarm.aMinutes) {
                return MORE;
            } else {
                //JDK7 Collections.Sort to compare need return -1,1,0
                //avoid 'comparison method violates its general contract!' exception
                return EQUAL;
            }
        }
    }
}