// IMyAidlInterface.aidl
package com.htc.android.worldclock.aiservice;

// Declare any non-default types here with import statements

interface AiClentInterface {
     //skip regular alarms between startSkipTime and endSkipTime
     boolean setSkipAlarms(long startSkipTime, long endSkipTime);
     //set a early event alarm by time
     boolean setEarlyEventAlarm(long time);
     //get Ai version
     long getVersion();
     //query whether exists regular alarm need skip between skip start date and skip end date
     boolean queryRegularSkipAlarm(long skipStartTime, long endSkipTime);
     //get earliest regular alarm time by query date
     long getEarliestAlarmTime(long queryStartTime, long queryEndTime);
}
