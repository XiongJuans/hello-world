package com.htc.android.worldclock.aiservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.AlarmItem;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.utils.AlertUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class AiClientService extends Service {
    private static final long CURRENT_AI_VERSION = 1;
    private static final long NO_MATCH_ALARM = -1;
    private final  Object mLock = new Object();

    private AiUtils mAiManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mAiManager = AiUtils.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(AiUtils.AI_TAG, "AiClientService:onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(AiUtils.AI_TAG, "AiClientService:IBinder()");
        return mBinder;
    }

    private final AiClentInterface.Stub mBinder = new AiClentInterface.Stub() {
        @Override
        public long getVersion() {
            return CURRENT_AI_VERSION;
        }

        /**
         * skip start time and end time eligible alarm
         * such as want 5.1/5.2 is holiday want skip eligible alarm during in 2 days
         * @param skipStartTime 5.1 00:00:00:00 accurate to milliseconds
         * @param skipEndTime 5.3 00:00:00:00 accurate to milliseconds
         * @return true skip success
         */
        @Override
        public boolean setSkipAlarms(long skipStartTime, long skipEndTime) {
            Log.i(AiUtils.AI_TAG, "setSkipAlarms");
            ArrayList<AlarmItem> skipAlarms = AiUtils.getAllSkipAlarms(getApplicationContext(), skipStartTime, skipEndTime);
            if (skipAlarms == null || skipAlarms.isEmpty()) {
                return false;
            } else {
                Log.i(AiUtils.AI_TAG, "setSkipAlarms skipAlarms size : " + skipAlarms.size());
                mAiManager.setSkipAlarmsTime(skipAlarms, skipStartTime, skipEndTime);
                mAiManager.setSkipAlarms(skipAlarms, skipStartTime, skipEndTime);
                AlarmUtils.setNextAlert(getApplicationContext());
                return true;
            }
        }

        /**
         * add early meeting alarm
         * @param time add alarm time for early Meeting
         * @return true success false failed
         */
        @Override
        public boolean setEarlyEventAlarm(long time) {
            Log.i(AiUtils.AI_TAG, "setEarlyEventAlarm start: " + time + "Date: " + new Date(time));
            if (time <= 0) {
                return false;
            }
            synchronized (mLock) {
                //get early meeting list
                ArrayList<AlarmItem> earlyEventAlarmList = mAiManager.getEarlyEventAlarms();

                //don't add same alarm for early event
                if (earlyEventAlarmList != null) {
                    for (AlarmItem alarmItem : earlyEventAlarmList) {
                        if (alarmItem.aAlertTime == time) {
                            return false;
                        }
                    }
                }
                //don't outnumber for Max alarm count
                ArrayList<AlarmItem> alarmItemArrayList = AlarmUtils.getAlarmListData(AiClientService.this);
                if (alarmItemArrayList != null && alarmItemArrayList.size() >= AlertUtils.MAX_ALARM_COUNT) {
                    return false;
                }
                //new a AlarmItem
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(time);
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minutes = c.get(Calendar.MINUTE);
                AlarmUtils.DaysOfWeek daysOfWeek = new AlarmUtils.DaysOfWeek();
                int dayOfWeek = daysOfWeek.getCoded();
                int id = mAiManager.getEarlyEventAlarmId();
                AlarmItem alarmItem = new AlarmItem();
                alarmItem.aId = ++id;
                alarmItem.aHour = hour;
                alarmItem.aMinutes = minutes;
                alarmItem.aDaysOfWeek = dayOfWeek;
                //get settings alarm alert uri
                alarmItem.aAlert = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
                alarmItem.aEnabled = true;
                alarmItem.aVibrate = true;
                alarmItem.aDescription = getResources().getString(R.string.ai_early_event_description);

                alarmItem.aOffAlarm = false;
                alarmItem.aAlertTime = c.getTimeInMillis();
                alarmItem.aRepeatType = 1;
                alarmItem.aSnoozed = false;

                //keep id
                mAiManager.setEarlyEventAlarmId(id);

                //judge is first enter?
                if (earlyEventAlarmList == null) {
                    earlyEventAlarmList = new ArrayList<>();
                }
                earlyEventAlarmList.add(alarmItem);

                mAiManager.setEarlyEventAlarms(earlyEventAlarmList);
                AlarmUtils.setNextAlert(getApplicationContext());
                Log.i(AiUtils.AI_TAG, "setEarlyEventAlarm end: " + "early event id: " + alarmItem.aId + "early event time: " + new Date(alarmItem.aAlertTime));
            }
            return true;
        }

        /**
         * query between skip start time and end time eligible alarm
         * such as want 5.1/5.2 is holiday want skip eligible alarm during in 2 days
         * @param skipStartTime 5.1 00:00:00:00 accurate to milliseconds
         * @param skipEndTime 5.3 00:00:00:00 accurate to milliseconds
         * @return true exist
         */
        @Override
        public boolean queryRegularSkipAlarm(long skipStartTime, long skipEndTime) {
            Log.i(AiUtils.AI_TAG, "queryRegularSkipAlarm");
            ArrayList<AlarmItem> skipAlarms = AiUtils.getAllSkipAlarms(getApplicationContext(), skipStartTime, skipEndTime);
            if (skipAlarms != null && skipAlarms.size() > 0) {
                return true;
            }
            return false;
        }

       /**
        * get earliest alarm time from query time
        * @param queryStartTime start query time
        * @param queryEndTime end query time
        * @return earliest alarm time
        */
       @Override
       public long getEarliestAlarmTime(long queryStartTime, long queryEndTime) {
           Log.d(AiUtils.AI_TAG, "queryStartTime =" + new Date(queryStartTime) + "queryEndTime = " + new Date(queryEndTime));
           //get all alarm list contain AI and normal alarms
           ArrayList<AlarmItem> allQueryAlarms = AiUtils.getAllQueryAlarms(AiClientService.this, queryStartTime, queryEndTime);
           //get earliest alarm time in all query alarms
           if (allQueryAlarms != null && allQueryAlarms.size() != 0) {
               AlarmItem tempAlarm = allQueryAlarms.get(0);
               for (AlarmItem alarmItem : allQueryAlarms) {
                   if (alarmItem.aAlertTime <= tempAlarm.aAlertTime) {
                       tempAlarm = alarmItem;
                   }
               }
               Log.i(AiUtils.AI_TAG, "getEarliestAlarmTime math alarm: " + tempAlarm.aAlertTime + "Date: " + new Date(tempAlarm.aAlertTime));
               return tempAlarm.aAlertTime;
           }

           Log.i(AiUtils.AI_TAG , "getEarliestAlarmTime not match alarm");
           //if don't have alarm return -1
           return NO_MATCH_ALARM;
       }
    };
}
