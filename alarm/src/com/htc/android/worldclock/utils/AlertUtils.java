package com.htc.android.worldclock.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.AlarmAlert;
import com.htc.android.worldclock.alarmclock.AlarmAlertReminder;
import com.htc.android.worldclock.alarmclock.AlarmService;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.timer.TimerAlert;
import com.htc.android.worldclock.timer.TimerAlertReminder;
import com.htc.android.worldclock.timer.TimerService;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.util.NotificationUtil;
import com.htc.lib1.settings.provider.HtcWrapSettings;
import com.htc.lib3.phonecontacts.telephony.HtcTelephonyManager;

public class AlertUtils {
    private static final String TAG = "WorldClock.AlertUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    public static final String DEFAULT_SNOOZE = "10";

    public static final String LOCAL_ACTION_CANCEL_ALERT = "com.htc.android.worldclock.intent.action.cancel_alert";
    public static final String ACTION_ALARM_TIMEOUT = "com.htc.intent.action.alarm_timeout";
    public static final String ACTION_TIMER_TIMEOUT = "com.htc.intent.action.timer_timeout";

    public static final String ACTION_ALARMALERT_INFROM = "com.htc.intent.action.alarmalert.inform";
    public static final String ACTION_SNOOZE_DISMISS_INFROM = "com.htc.intent.action.snooze.dismiss.inform";
    public static final String ACTION_SNOOZE_DISMISS_RECEIVE = "com.htc.intent.action.snooze.dismiss.receive";
    public static final String ACTION_VOLUMEKEY_EVENT = "com.htc.intent.action.volumekey.event";
    public static final String ACTION_TIMERALERT_INFROM = "com.htc.intent.action.timeralert.inform";

    // Baidu voice assistant has survey data for user clock operating requirement
    public static final String ACTION_DISALBE_ALARM = "com.htc.android.worldclock.DISALBE_ALARM";

    public static final int ALARM_NOTIFICATION_ID = 101;
    public static final int TIMER_NOTIFICATION_ID = 102;
    public static final int ALARMALERT_NOTIFICATION_ID = 103;
    public static final int TIMERALERT_NOTIFICATION_ID = 104;

    private static final String NOTIFICATION_ALARM_CHANNEL_ID = "alarm_channel";
    private static final String NOTIFICATION_ALARM_SNOOZE_CHANNEL_ID = "alarm_snooze_channel";
    private static final String NOTIFICATION_TIMER_CHANNEL_ID = "timer_channel";

    public static final String RESHOW_ALERT_DIALOG = "reshow_alert_dialog";

    public static final String ACTION_ALERT_TYPE = "alert_type";
    public static final String ACTION_ALARM_ACTION = "alarm_action";
    public static final String ACTION_TIMER_ACTION = "timer_action";
    public static final String ALERT_SNOOZE = "snooze";
    public static final String ALERT_DISMISS = "dismiss";

    public static final int ALERT_DIALOG_NORMAL = 0x10;
    public static final int ALERT_DIALOG_TIMEOUT = 0x11;
    public static final String EXTRA_ALERT_TYPE = "extra_alert_type";
    public static final String EXTRA_SHOW_SNOOZE = "extra_show_snooze";
    
    // for query syntax
    public static final String ACTION_QUERY_SYNTAX = "com.htc.intent.action.query.syntax";
    public static final String EXTRA_QUERY_VERSION = "extra_query_version";
    public static final String EXTRA_QUERY_SYNTAX = "extra_query_syntax";

    private static final String[] CATEGORYS = {"com.htc.intent.category.MATRIXVR",
            "com.google.intent.category.CARDBOARD", "com.google.intent.category.DAYDREAM"};

    // If the alert is older than STALE_WINDOW seconds, ignore it
    public final static int STALE_WINDOW = 10 * 60 * 1000; // 10 minutes to follow spec

    // for permission of M
    public static final int REQUEST_PERMISSION_ACCESS = 0x1001;
    
    /** Cap alarm count at this number */
    public final static int MAX_ALARM_COUNT = 500;
    
    private static boolean mAlarmSnoozeOrDismiss;
    private static boolean mTimerDismiss;

    public enum SIM {
        SLOT1,
        SLOT2,
    };
    
    public static void sendAlarmTimeoutIntent(Context context) {
        Intent alarmIntent = new Intent();
        alarmIntent.setAction(ACTION_ALARM_TIMEOUT);
        context.sendBroadcast(alarmIntent);
    }

    public static void sendTimerTimeoutIntent(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(ACTION_TIMER_TIMEOUT);
        context.sendBroadcast(timerIntent);
    }

    public static void disableAlertAndSetNextAlert(Context context, int id) {
        AlarmUtils.disableSnoozeAlert(context, id);
        AlarmUtils.disableAlert(context, id);
        AlarmUtils.setNextAlert(context);
    }

    public static void alarmAlert(Context context, int id, long time, String description, int alarmType, boolean isLockScreen) {
        /* launch UI */
        Intent fireAlarm;
        if (DEBUG_FLAG) Log.d(TAG, "alarmAlert: isLockScreen = " + isLockScreen);
        if (isLockScreen) {
            fireAlarm = new Intent(context, AlarmAlertReminder.class);
        } else {
            //	will not show alarm dialog while vr app running
            if (isVrAppRunning(context)) {
                Log.d(TAG, "Vr app was running: ");
                return;
            }
            fireAlarm = new Intent(context, AlarmAlert.class);
        }
        fireAlarm.putExtra(AlarmUtils.ID, id);
        fireAlarm.putExtra(AlarmUtils.TIME, time);
        fireAlarm.putExtra(AlarmUtils.DESCRIPTION, description);
        fireAlarm.putExtra(EXTRA_ALERT_TYPE, alarmType);
        fireAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        fireAlarm.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(fireAlarm);
    }

    /**
     * to judge is there any vr app running.
     *
     * @param context context
     * @return the result of judgement.
     */
    private static boolean isVrAppRunning(Context context) {
        List<String> installApps = getInstalledVrApps(context);
        String runningApp = whichAppInForeground(context);
        Log.d(TAG, "ForegroundApp: " + runningApp);
        for (String app : installApps) {
            if (app.equals(runningApp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * to get all installed vr apps.
     *
     * @param context the context.
     * @return the result of all installed vr apps.
     */
    private static List<String> getInstalledVrApps(Context context) {
        Log.d(TAG, "getInstalledVrApps");
        List<String> getVrAppsResult = new ArrayList<>();

        for (String category : CATEGORYS) {
            List<ResolveInfo> resolveInfo = queryIntent(category, context);
            if (resolveInfo.isEmpty()) {
                continue;
            }
            for (ResolveInfo info : resolveInfo) {
                getVrAppsResult.add(info.activityInfo.packageName);
            }
        }
        return getVrAppsResult;
    }

    /**
     * to query the app depend category.
     *
     * @param category the depend category.
     * @param context  the context.
     * @return the result of query.
     */
    private static List<ResolveInfo> queryIntent(String category, Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(category);
        return pm.queryIntentActivities(intent, 0);
    }

    /**
     * to get the foreground app.
     *
     * @param context the context.
     * @return the foreground app packageName.
     */
    private static String whichAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activityManager
                .getRunningTasks(Integer.MAX_VALUE);

        if (services == null) {
            return null;
        }
        if (services.size() > 0) {
            return services.get(0).topActivity
                    .getPackageName().toString();
        }
        return null;
    }


    public static void timerAlert(Context context, int timerType, boolean isLockScreen) {
        /* launch UI */
        Intent fireTimer;
        if (DEBUG_FLAG) Log.d(TAG, "timerAlert: isLockScreen = " + isLockScreen);
        if (isLockScreen) {
            fireTimer = new Intent(context, TimerAlertReminder.class);
        } else {
            fireTimer = new Intent(context, TimerAlert.class);
        }
        fireTimer.putExtra(EXTRA_ALERT_TYPE, timerType);
        fireTimer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        fireTimer.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(fireTimer);
    }

    public static String readTextFromFile(String filePath, String TAG) {
        File fileName = new File(filePath);
        StringBuilder strBuilder = new StringBuilder();

        try {
            BufferedReader buffer =
                new BufferedReader(new FileReader(fileName));
            try {
                String data = null;
                while ((data = buffer.readLine()) != null) {
                    strBuilder.append(data);
                    strBuilder.append(System.getProperty("line.separator"));
                }
            } finally {
                buffer.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "readTextFromFile: e = " + e.toString());
            return null;
        }
        return strBuilder.toString();
    }

    public static int getSnoozeMinute(Context context) {
        String snooze = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.KEY_ALARM_SNOOZE, DEFAULT_SNOOZE);
        return Integer.parseInt(snooze);
    }

    public static void alarmSnoozedNotification(Context context, int id, final long snoozeTarget) {
        Log.i(TAG, "alarmSnoozedNotification: id = " + id);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // cancel previous the same alarm snoozed notification first
        nm.cancel(id);

        // Get the display time for the snooze and update the notification.
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(snoozeTarget);

        // send notification
        // Append (snoozed) to the label.
        String label = context.getString(R.string.default_label);
        label = context.getString(R.string.alarm_notify_snooze_label, label);

        // Notify user that the alarm has been snoozed.
        Intent cancelSnooze = new Intent(context, AlarmService.class);
        cancelSnooze.setAction(AlarmUtils.ACTION_CANCEL_SNOOZE);
        cancelSnooze.putExtra(AlarmUtils.ID, id);
        PendingIntent cancelSnoozeBroadcast = PendingIntent.getService(context, id, cancelSnooze, PendingIntent.FLAG_CANCEL_CURRENT);

        // default style
        ContextThemeWrapper ctxWrapper = new ContextThemeWrapper(context, com.htc.lib1.cc.R.style.HtcDeviceDefault_CategoryTwo);
        HtcCommonUtil.initTheme(ctxWrapper, HtcCommonUtil.CATEGORYTWO);
        Notification.Builder nBuilder = new Notification.Builder(context);
        setNotificationChannelId(nm, nBuilder, label, NOTIFICATION_ALARM_SNOOZE_CHANNEL_ID);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
            nBuilder.setCategory(Notification.CATEGORY_ALARM);
        }
        nBuilder.setTicker(label);
        nBuilder.setSmallIcon(R.drawable.stat_notify_postpone);
        nBuilder.setContentTitle(label);
        nBuilder.setContentText(context.getString(R.string.alarm_notify_snooze_text, AlarmUtils.formatTime(context, c)));
        nBuilder.setUsesChronometer(true);
        nBuilder.setContentIntent(cancelSnoozeBroadcast);
        nBuilder.setAutoCancel(false);
        nBuilder.setOngoing(true);
        nBuilder.setPriority(Notification.PRIORITY_MAX + 5);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
            nBuilder.setColor(HtcCommonUtil.getCommonThemeColor(context, com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color));
        }
        Notification notification = nBuilder.build();
        NotificationUtil.enableNotificationFeatures(notification, "EXTRA_HTC_FEATURE_SPECIAL_PRIORITY");
        nm.notify(id, notification);
    }

    public static void cancelAlarmSnoozedNotification(Context context, int id) {
        Log.i(TAG, "cancelAlarmSnoozedNotification: id = " + id);
        // Cancel the notification and stop playing the alarm
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(id);
    }

    private static boolean isAlarmLEDEnabled(Context context) {
        boolean isLedEnabled = true;
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_M) {
            int value = Settings.Global.getInt(context.getContentResolver(), "htc_notification_light_alarm", -1);
            isLedEnabled = (value == 1) ? true : false; 
        } else {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(Uri.withAppendedPath(Uri.parse("content://com.htc.provider.settings"), "nfl"), null, null, null, null);
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    isLedEnabled = cursor.getInt(6)== 1 ? true : false; // column 6 is alarm notification setting
                }
            } catch (Exception e) {
                Log.w(TAG, "isAlarmLEDEnabled: e = " + e.toString());
            } finally {
                if(cursor != null) {
                    if(cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "isAlarmLEDEnabled: isLedEnabled = " + isLedEnabled);
        return isLedEnabled;
    }
    
    private static int getAlarmColor(Context context) {
        int alarmColor = -1;
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_M) {
            alarmColor = Settings.Global.getInt(context.getContentResolver(), "htc_notification_light_alarm_color", -1);
        } else {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(Uri.withAppendedPath(Uri.parse("content://com.htc.provider.settings"), "nfl"), null, null, null, null);
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    alarmColor = cursor.getInt(13); // column 13 is alarm color
                }
            } catch (Exception e) {
                Log.w(TAG, "getAlarmColor: e = " + e.toString());
            } finally {
                if(cursor != null) {
                    if(cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "getAlarmColor: alarmColor = " + alarmColor);
        return alarmColor;
    }
    
    public static Notification alarmNotification(Context context, int id, long time, String description, int type, boolean showSnooze, boolean notify) {
        Log.i(TAG, "alarmNotification: id = " + ALARMALERT_NOTIFICATION_ID);
        String label = context.getString(R.string.alarm);
        java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(context);
        StringBuffer alarmTime = new StringBuffer(mTimeFormat.format(time));

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // cancel previous the same alarm notification first
        nm.cancel(ALARMALERT_NOTIFICATION_ID);

        // Notify user that device can reshow alert dialog.
        Intent showDialog = new Intent(context, AlarmService.class);
        showDialog.setAction(RESHOW_ALERT_DIALOG);
        showDialog.putExtra(AlarmUtils.ID, id);
        showDialog.putExtra(AlarmUtils.TIME, time);
        showDialog.putExtra(AlarmUtils.DESCRIPTION, description);
        showDialog.putExtra(AlertUtils.EXTRA_ALERT_TYPE, type);
        showDialog.putExtra(AlertUtils.EXTRA_SHOW_SNOOZE, showSnooze);
        PendingIntent showDialogBroadcast = PendingIntent.getService(context, ALARMALERT_NOTIFICATION_ID, showDialog, PendingIntent.FLAG_CANCEL_CURRENT);

        // Notify user that device can show dismiss action on status bar.
        Intent dismissIntent = new Intent(context, AlarmService.class);
        dismissIntent.setAction(ALERT_DISMISS);
        dismissIntent.putExtra(AlarmUtils.ID, id);
        PendingIntent dismissBroadcast = PendingIntent.getService(context, ALARMALERT_NOTIFICATION_ID, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Notify user that device can show snooze action on status bar.
        Intent snoozeIntent = new Intent(context, AlarmService.class);
        snoozeIntent.setAction(ALERT_SNOOZE);
        snoozeIntent.putExtra(AlarmUtils.ID, id);
        PendingIntent snoozeBroadcast = PendingIntent.getService(context, ALARMALERT_NOTIFICATION_ID, snoozeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // default style
        ContextThemeWrapper ctxWrapper = new ContextThemeWrapper(context, com.htc.lib1.cc.R.style.HtcDeviceDefault_CategoryTwo);
        HtcCommonUtil.initTheme(ctxWrapper, HtcCommonUtil.CATEGORYTWO);
        Notification.Builder nBuilder = new Notification.Builder(context);
        setNotificationChannelId(nm, nBuilder, label, NOTIFICATION_ALARM_CHANNEL_ID);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
            nBuilder.setCategory(Notification.CATEGORY_ALARM);
        }
        nBuilder.setTicker(label);
        nBuilder.setSmallIcon(R.drawable.stat_notify_alarm_alert);
        nBuilder.setContentTitle(label);
        nBuilder.setContentText(alarmTime);
        nBuilder.setUsesChronometer(true);
        nBuilder.setContentIntent(showDialogBroadcast);
        nBuilder.setAutoCancel(false);
        nBuilder.setOngoing(true);
        nBuilder.setPriority(Notification.PRIORITY_MAX + 5);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
            nBuilder.setColor(HtcCommonUtil.getCommonThemeColor(context, com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color));
        }
        nBuilder.addAction(R.drawable.icon_btn_cancel_light_s, context.getString(R.string.alarm_alert_dismiss_text), dismissBroadcast);
        if (showSnooze) {
            nBuilder.addAction(R.drawable.icon_btn_postpone_light_s, context.getString(R.string.alarm_alert_snooze_text), snoozeBroadcast);
            if (isAlarmLEDEnabled(context)) {
                if (Global.isSupportAlarmColorLed()) {
                    int colorValue = getAlarmColor(context);
                    if (-1 != colorValue) {
                        nBuilder.setLights(colorValue, 500, 500);
                    } else {
                        nBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
                    }
                } else {
                    nBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
                }
            }
        }

        Notification notification = nBuilder.build();
        NotificationUtil.enableNotificationFeatures(notification, "EXTRA_HTC_FEATURE_SPECIAL_PRIORITY");
        if (notify) {
            nm.notify(AlertUtils.ALARMALERT_NOTIFICATION_ID, notification);
        }
        return notification;
    }

    public static void cancelAlarmNotification(Context context, int id) {
        Log.i(TAG, "cancelAlarmNotification: id = " + ALARMALERT_NOTIFICATION_ID);
        // Cancel alarm notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(ALARMALERT_NOTIFICATION_ID);
    }

    public static Notification timerNotification(Context context, boolean isTimeout, boolean notify) {
        Log.i(TAG, "timerNotification: id = " + TIMERALERT_NOTIFICATION_ID);
        String label = context.getString(R.string.timer_caption);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // cancel previous the same timer notification first
        nm.cancel(TIMERALERT_NOTIFICATION_ID);

        // Notify user that device can show dismiss action on status bar.
        Intent dismissIntent = new Intent(context, TimerService.class);
        dismissIntent.setAction(ALERT_DISMISS);
        PendingIntent dismissBroadcast = PendingIntent.getService(context, ALARMALERT_NOTIFICATION_ID, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // default style
        ContextThemeWrapper ctxWrapper = new ContextThemeWrapper(context, com.htc.lib1.cc.R.style.HtcDeviceDefault_CategoryTwo);
        HtcCommonUtil.initTheme(ctxWrapper, HtcCommonUtil.CATEGORYTWO);
        Notification.Builder nBuilder = new Notification.Builder(context);
        setNotificationChannelId(nm, nBuilder, label,NOTIFICATION_TIMER_CHANNEL_ID);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
            nBuilder.setCategory(Notification.CATEGORY_ALARM);
        }
        nBuilder.setTicker(label);
        nBuilder.setSmallIcon(R.drawable.stat_notify_timer);
        nBuilder.setContentTitle(label);
        nBuilder.setContentText(context.getString(R.string.time_up));
        nBuilder.setUsesChronometer(true);
        nBuilder.setContentIntent(dismissBroadcast);
        nBuilder.addAction(R.drawable.icon_btn_cancel_light_s, context.getString(R.string.alarm_alert_dismiss_text), dismissBroadcast);
        nBuilder.setAutoCancel(false);
        nBuilder.setOngoing(true);
        nBuilder.setPriority(Notification.PRIORITY_MAX + 5);
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_L) {
            nBuilder.setColor(HtcCommonUtil.getCommonThemeColor(context, com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color));
        }
        if (!isTimeout) {
            if (isAlarmLEDEnabled(context)) {
                nBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
            }
        }
        
        Notification notification = nBuilder.build();
        NotificationUtil.enableNotificationFeatures(notification, "EXTRA_HTC_FEATURE_SPECIAL_PRIORITY");
        if (notify) {
            nm.notify(AlertUtils.TIMERALERT_NOTIFICATION_ID, notification);
        }
        return notification;

    }

    public static void cancelTimerNotification(Context context) {
        Log.i(TAG, "cancelTimerNotification: id = " + TIMERALERT_NOTIFICATION_ID);
        // Cancel timer notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(TIMERALERT_NOTIFICATION_ID);
    }

    /**
     * user notification channel to show notification after sdk 26
     * @param nm notificationManager
     * @param nBuilder Notification Builder
     * @param label notification label name
     * @param channelId channel id
     */
    private static void setNotificationChannelId(NotificationManager nm, Notification.Builder nBuilder, String label, String channelId) {
        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            NotificationChannel channel = new NotificationChannel(channelId, label, NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
            nBuilder.setChannelId(channelId);
        }
    }


    public static void sendCancelAlertIntent(Context context, int id) {
        // cancel intent
        Intent cancelIntent = new Intent();
        cancelIntent.setPackage(context.getPackageName());
        cancelIntent.setAction(AlertUtils.LOCAL_ACTION_CANCEL_ALERT);
        cancelIntent.putExtra(AlarmUtils.ID, id);
        context.sendBroadcast(cancelIntent);
    }
    
    public static Uri getAlarmDefaultAlertUri(Context context) {
        Uri alertUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
        if (DEBUG_FLAG) Log.d(TAG, "getAlarmDefaultAlertUri: getActualDefaultRingtoneUri alertUri = " + alertUri);
        if (alertUri == null) {
            if (DEBUG_FLAG) Log.d(TAG, "getAlarmDefaultAlertUri: alertUri = null, try to get first cursor alarm");
        }
        
        // check valid ringtone
        if (!isRingToneExist(context, alertUri)) {
            // get first cursor alarm
            Cursor cursor = null;
            try {
                RingtoneManager rm = new RingtoneManager(context);
                rm.setType(RingtoneManager.TYPE_ALARM);
                cursor = rm.getCursor();
                if ((null != cursor) && cursor.moveToFirst()) {
                    alertUri = rm.getRingtoneUri(cursor.getPosition());
                    Log.i(TAG, "Setting default alarm is invalid, get first alarm alertUri = " + alertUri);
                }
            } catch (Exception e) {
                Log.w(TAG, "getAlarmDefaultAlertUri: e = " + e.toString());
            } finally {
                if (cursor != null) {
                    if (cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
            }
        }
        
        return alertUri;
    }

    // get timer default ringtone
    public static Uri getTimerDefaultAlertUri(Context context) {
        Uri alertUri = null;
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE };
            String where = "title = 'Feverfew' OR title = 'Amber'";
            cursor = context.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, where, null, null);
            if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                String secondWhere = String.format("_data = '%s'", cursor.getString(0));
                // close cursor before use the same cursor
                if (cursor != null) {
                    if (cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
                cursor = context.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, null, secondWhere, null, null);
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    alertUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getTimerDefaultAlertUri: e = " + e.toString());
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }

        if (DEBUG_FLAG) Log.d(TAG, "getTimerDefaultAlertUri: alertUri = " + alertUri);
        if (alertUri == null) {
            Log.w(TAG, "getTimerDefaultAlertUri: alertUri = null");
            return getAlarmDefaultAlertUri(context);
        } else {
            return alertUri;
        }
    }

    public static boolean getLockScreenMode(Context context) {
            boolean keyguard = false;
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            keyguard = km.inKeyguardRestrictedInputMode();
            if (DEBUG_FLAG) Log.d(TAG, "getLockScreenMode: inKeyguardRestrictedInputMode keyguard = " + keyguard);
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOff = !pm.isScreenOn();
            if (DEBUG_FLAG) Log.d(TAG, "getLockScreenMode: isScreenOff = " + isScreenOff);
            return (isScreenOff || keyguard);
    }

    public static boolean isAlarmSnoozeOrDismissByUser() {
        return mAlarmSnoozeOrDismiss;
    }

    public static void setAlarmSnoozeOrDismissByUser(boolean value) {
        mAlarmSnoozeOrDismiss = value;
    }

    public static boolean isTimerDismissByUser() {
        return mTimerDismiss;
    }

    public static void setTimerDismissByUser(boolean value) {
        mTimerDismiss = value;
    }

    public static void sendAlarmSnoozeOrDismissIntent(Context context, String alarmAction) {
        Intent alarmIntent = new Intent();
        alarmIntent.setAction(ACTION_SNOOZE_DISMISS_INFROM);
        alarmIntent.putExtra(ACTION_ALERT_TYPE, ACTION_ALARM_ACTION);
        alarmIntent.putExtra(ACTION_ALARM_ACTION, alarmAction);
        context.sendBroadcast(alarmIntent);
    }
    
    public static void sendTimerDismissIntent(Context context, String timerAction) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(ACTION_SNOOZE_DISMISS_INFROM);
        timerIntent.putExtra(ACTION_ALERT_TYPE, ACTION_TIMER_ACTION);
        timerIntent.putExtra(ACTION_TIMER_ACTION, timerAction);
        context.sendBroadcast(timerIntent);
    }

    public static boolean isCallStateIdle(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean isCallStateIdle = (TelephonyManager.CALL_STATE_IDLE == tm.getCallState());
        if (DEBUG_FLAG) Log.d(TAG, "isCallStateIdle: isCallStateIdle = " + isCallStateIdle);
        return isCallStateIdle;
    }

    public static boolean isDoNotDisturbEnabled(Context context) {
        boolean ret = false;
        try {
            int dnd_feature_enabled = Settings.System.getInt(context.getContentResolver(), HtcWrapSettings.System.HTC_DND_FEATURE_ENABLED, 0);
            int dnd_play_sound_enabled = Settings.System.getInt(context.getContentResolver(), HtcWrapSettings.System.HTC_DND_PLAY_SOUND_ENABLED, 0);
            if (DEBUG_FLAG) Log.d(TAG, "isDoNotDisturbEnabled: dnd_feature_enabled = " + dnd_feature_enabled);
            if (DEBUG_FLAG) Log.d(TAG, "isDoNotDisturbEnabled: dnd_play_sound_enabled = " + dnd_play_sound_enabled);
            if ((dnd_feature_enabled  == 1) && (dnd_play_sound_enabled == 0)) {
                ret = true;
            }
        } catch (Exception e) {
            Log.w(TAG, "isDoNotDisturbEnabled: isDoNotDisturbEnabled fail e = " + e.toString());
        }
        if (DEBUG_FLAG) Log.d(TAG, "isDoNotDisturbEnabled: ret = " + ret);
        return ret;
    }

    public static void sendAlarmSnoozeIntent(Context context, int id, String description) {
        if (DEBUG_FLAG) Log.d(TAG, "sendAlarmSnoozeIntent, id = " + id);
        // alarm snooze intent
        Intent alarmSnoozeIntent = new Intent();
        alarmSnoozeIntent.setAction(AlertUtils.ACTION_SNOOZE_DISMISS_RECEIVE);
        alarmSnoozeIntent.putExtra(AlertUtils.ACTION_ALARM_ACTION, AlertUtils.ALERT_SNOOZE);
        alarmSnoozeIntent.putExtra(AlarmUtils.ID, id);
        alarmSnoozeIntent.putExtra(AlarmUtils.DESCRIPTION, description);
        context.sendBroadcast(alarmSnoozeIntent);
    }
    
    public static void sendAlarmDismissIntent(Context context, int id) {
        if (DEBUG_FLAG) Log.d(TAG, "sendAlarmDismissIntent, id = " + id);
        // alarm dismiss intent
        Intent alarmDismissIntent = new Intent();
        alarmDismissIntent.setAction(AlertUtils.ACTION_SNOOZE_DISMISS_RECEIVE);
        alarmDismissIntent.putExtra(AlertUtils.ACTION_ALARM_ACTION, AlertUtils.ALERT_DISMISS);
        alarmDismissIntent.putExtra(AlarmUtils.ID, id);
        context.sendBroadcast(alarmDismissIntent);
    }
    
    public static void sendTimerDismissIntent(Context context) {
        if (DEBUG_FLAG) Log.d(TAG, "sendTimerDismissIntent");
        // timer dismiss intent
        Intent timerDismissIntent = new Intent();
        timerDismissIntent.setAction(AlertUtils.ACTION_SNOOZE_DISMISS_RECEIVE);
        timerDismissIntent.putExtra(AlertUtils.ACTION_TIMER_ACTION, AlertUtils.ALERT_DISMISS);
        context.sendBroadcast(timerDismissIntent);
    }

    public static boolean isTopActivity(Context context) {
        ActivityManager myActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        // 1 means top 1 activity
        List<ActivityManager.RunningTaskInfo> list = myActivityManager.getRunningTasks(1);
        if ((list != null) && !list.isEmpty()) {
            String topActivity = null;
            if (list.get(0).topActivity != null) {
                topActivity = list.get(0).topActivity.getClassName();
                if (DEBUG_FLAG) Log.d(TAG, "isTopActivity: topActivity = " + topActivity);
                String callerClassName = context.getClass().getName();
                if (DEBUG_FLAG) Log.d(TAG, "isTopActivity: callerClassName = " + callerClassName);
                if (topActivity.compareTo(callerClassName) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // get alarm restore ringtone
    public static Uri getAlarmRestoreAlertUriByTitle(Context context, String alarmSoundTitle) {
        Uri alertUri = null;
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE };
            String where = "title = '" + alarmSoundTitle + "'";
            cursor = context.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, where, null, null);
            if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                String secondWhere = String.format("_data = '%s'", cursor.getString(0));
                // close cursor before use the same cursor
                if (cursor != null) {
                    if (cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
                cursor = context.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, null, secondWhere, null, null);
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    alertUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
                }
            } else {
                // close cursor before use the same cursor
                if (cursor != null) {
                    if (cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
                cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, where, null, null);
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    String secondWhere = String.format("_data = '%s'", cursor.getString(0));
                    // close cursor before use the same cursor
                    if (cursor != null) {
                        if (cursor.isClosed() == false) {
                            cursor.close();
                        }
                    }
                    cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, secondWhere, null, null);
                    if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                        String id = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                        alertUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getAlarmRestoreAlertUriByTitle: e = " + e.toString());
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }

        if (DEBUG_FLAG) Log.d(TAG, "getAlarmRestoreAlertUriByTitle: alertUri = " + alertUri);
        if (alertUri == null) {
            return Uri.parse("");
        } else {
            return alertUri;
        }
    }
    
    // get alarm restore ringtone
    public static Uri getAlarmRestoreAlertUriByQuertCondition(Context context, String alertQuerySyntax) {
        Uri alertUri = null;
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.MediaColumns.DATA };
            String where = alertQuerySyntax;
            if (DEBUG_FLAG) Log.d(TAG, "getAlarmRestoreAlertUriByQuertCondition: where = " + where);
            cursor = context.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, where, null, null);
            if (cursor != null) {
                if (DEBUG_FLAG) Log.d(TAG, "getAlarmRestoreAlertUriByQuertCondition: INTERNAL_CONTENT_URI cursor.getCount() = " + cursor.getCount());
            }
            if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                String secondWhere = String.format("_data = '%s'", cursor.getString(0));
                // close cursor before use the same cursor
                if (cursor != null) {
                    if (cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
                cursor = context.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, null, secondWhere, null, null);
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    alertUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
                }
            } else {
                // close cursor before use the same cursor
                if (cursor != null) {
                    if (cursor.isClosed() == false) {
                        cursor.close();
                    }
                }
                cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, where, null, null);
                if (cursor != null) {
                    if (DEBUG_FLAG) Log.d(TAG, "getAlarmRestoreAlertUriByQuertCondition: EXTERNAL_CONTENT_URI cursor.getCount() = " + cursor.getCount());
                }
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    String secondWhere = String.format("_data = '%s'", cursor.getString(0));
                    // close cursor before use the same cursor
                    if (cursor != null) {
                        if (cursor.isClosed() == false) {
                            cursor.close();
                        }
                    }
                    cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, secondWhere, null, null);
                    if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                        String id = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                        alertUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getAlarmRestoreAlertUriByQuertCondition: e = " + e.toString());
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }

        if (DEBUG_FLAG) Log.d(TAG, "getAlarmRestoreAlertUriByQuertCondition: alertUri = " + alertUri);
        if (alertUri == null) {
            return Uri.parse("");
        } else {
            return alertUri;
        }
    }
    
    public static boolean isRingToneExist(Context context, Uri uri) {
        boolean isExist = true;
        MediaPlayer audio = new MediaPlayer();
        try {
            if (DEBUG_FLAG) Log.d(TAG, "isRingToneExist: uri = " + uri);
            if (uri != null) {
                audio.setDataSource(context, uri);
            }
        } catch (Exception e) {
            Log.w(TAG, "isRingToneExist: e = " + e.toString());
            isExist = false;
        } finally {
            if (DEBUG_FLAG) Log.d(TAG, "isRingToneExist: isExist = " + isExist);
            audio.release();
            audio = null;
        }

        return isExist;
    }
    
    public static MediaPlayer startPlayAlert(Context context, Uri alertUri) {
        /* we need a new MediaPlayer when we change media URLs */
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                try {
                    Log.w(TAG, "startPlay: Error occurred while playing audio.");
                    if (mp!= null) {
                        if (mp.isPlaying()) {
                            mp.stop();
                        }
                        mp.release();
                        mp = null;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "startPlay: Error setOnErrorListener e = " + e.toString());
                }
                return true;
            }
        });
            
        try {
            mediaPlayer.setDataSource(context, alertUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            if (DEBUG_FLAG) Log.d(TAG, "startPlay: mMediaPlayer.start() done");
        } catch (Exception e) {
            Log.w(TAG, "startPlay: Error playing alarm e = " + e.toString());
        }
        return mediaPlayer;
    }
    
    public static boolean isHtcSoundPickerExist(Context context) {
        boolean isExist = false;
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(Global.HTC_SOUND_PICKER_ACTION_NAME);
        ComponentName comp = null;
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
        if (DEBUG_FLAG) Log.d(TAG, "isHtcSoundPickerExist: list.size() = " + list.size());
        if ((list != null) && (!list.isEmpty())) {
            ResolveInfo info = list.get(0);
            if (info.activityInfo != null) {
                if (DEBUG_FLAG) Log.d(TAG, "isHtcSoundPickerExist: package name = " + info.activityInfo.packageName);
                if (Global.HTC_SOUND_PICKER_PACKAGENAME.equals(info.activityInfo.packageName)) {
                    isExist = true;
                }
            }
        }
        if (DEBUG_FLAG) Log.d(TAG, "isHtcSoundPickerExist: isExist = " + isExist);
        return isExist;
    }
    
    public static boolean reflectIsMultiSimEnabled(Context context) {
        // hide field from google framework
        final String CLASS_NAME = "android.telephony.TelephonyManager";
        final String METHOD = "isMultiSimEnabled";
        boolean retValue = false;
    
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> c = Class.forName(CLASS_NAME);
            Method m = c.getDeclaredMethod(METHOD, (Class[]) null);
            Object obj = m.invoke(telephonyManager, (Object[]) null);
            retValue =  (Boolean)obj;
        } catch (Exception e) {
            Log.w(TAG, "reflectIsMultiSimEnabled: e = " + e.toString());
        }
        return retValue;
    }
    
    public static String reflectSystemPropertyGet(String key, String defaultValue) {
        // hide field from google framework
        final String CLASS_NAME = "android.os.SystemProperties";
        final String METHOD = "get";
        String retValue = defaultValue;
        try {
            final Class<?> systemProperties = Class.forName(CLASS_NAME);
            final Method get = systemProperties.getMethod(METHOD, String.class, String.class);
            retValue = (String) get.invoke(null, key, defaultValue);
        } catch (Exception e) {
            // This should never happen
            Log.w(TAG, "Exception while getting system property: ", e);
        }
        return retValue;
    }
    
    public static boolean reflectIsDeviceEncryptionEnabled() {
        // hide field from google framework
        final String CLASS_NAME = "com.android.internal.widget.LockPatternUtils";
        final String METHOD = "isDeviceEncryptionEnabled";
        boolean retValue = false;
    
        try {
            Class<?> c = Class.forName(CLASS_NAME);
            Method m = c.getDeclaredMethod(METHOD, (Class[]) null);
            Object obj = m.invoke(null, (Object[]) null);
            retValue = (Boolean)obj;
        } catch (Exception e) {
            Log.w(TAG, "reflectIsDeviceEncryptionEnabled: e = " + e.toString());
        }
        return retValue;
    }
    
    public static boolean reflectIsCurrentUser() {
        final String METHOD1 = "getIdentifier";
        final String METHOD2 = "getCurrentUser";
        boolean retValue = true;
        try {
            Method getUhId = UserHandle.class.getDeclaredMethod(METHOD1, (Class[]) null);
            Method getCuId = ActivityManager.class.getDeclaredMethod(METHOD2, (Class[]) null);
            Integer myUhId = (Integer) getUhId.invoke(android.os.Process.myUserHandle(), (Object[]) null);
            Integer cuId = (Integer) getCuId.invoke(null, (Object[]) null);
            retValue = ((myUhId != null) && (myUhId.equals(cuId)));
        } catch (Exception e) {
            Log.w(TAG, "reflectIsCurrentUser: e = " + e.toString());
        }
        Log.i(TAG, "reflectIsCurrentUser: retValue = " + retValue);
        return retValue;
    }
    
    public static boolean getCurrentUserIsOwner(Context context) {
        boolean isOwner = false;
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (um != null) {
            UserHandle userHandle = android.os.Process.myUserHandle();
            isOwner = um.getSerialNumberForUser(userHandle) == 0;
        }
        Log.i(TAG, "getCurrentUserIsOwner: isOwner = " + isOwner);
        return isOwner;
    }
}
