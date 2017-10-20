package com.htc.android.worldclock.alarmclock;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

/**
 * Created by xiongjuan on 17-10-18.
 */

public class AlarmReminderReceiver extends BroadcastReceiver {
        private static final String TAG = "WorldClock.AlarmReminderReceiver";
        private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;


        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            if (DEBUG_FLAG) Log.d(TAG, "onReceive: receive action = " + action);
            Log.d("juan", "onReceive: receive action = " + action);
            if (AlarmUtils.ACTION_ALARM_NOTICE.equals(action)){

                int alarmId = intent.getIntExtra(AlarmUtils.ID, -1);
                /*
                String channel_id_1 = "channel_1";
                String channel_name_1 = "channel_1_name";
                //send　notification
                NotificationChannel channel = new NotificationChannel(channel_id_1, channel_name_1, NotificationManager.IMPORTANCE_LOW);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//                notificationManager.cancel(AlertUtils.ALARMALERT_NOTIFICATION_ID);

                NotificationCompat.Builder builder=new NotificationCompat.Builder(context);

                Intent closeIntent = new Intent(); //action
                intent.setClass(context, AlarmReminderReceiver.class);
                closeIntent.setAction("testAction");
                closeIntent.putExtra(AlarmUtils.ID, alarmId); //id
                PendingIntent pendingItent = PendingIntent.getBroadcast(context, 1, closeIntent,0);

                builder.setContentTitle("闹钟")
                        .setContentText("闹钟即将响铃，您可以关闭此闹钟")
                        .setChannelId(channel_id_1)
                        .setSmallIcon(R.drawable.stat_notify_alarm_alert)
                        .setContentIntent(pendingItent)
                        .setPriority(Notification.PRIORITY_DEFAULT)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

                Notification notification = builder.build();
                //发送通知
                notificationManager.createNotificationChannel(channel);
                notificationManager.notify(AlertUtils.ALARMALERT_NOTIFICATION_ID, notification);
                */
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                int NOTIFY_ID_1 = 1000;
                String channel_id_1 = "channel_1";
                String channel_name_1 = "channel_1_name";
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel mChannel = new NotificationChannel(channel_id_1, channel_name_1, importance);
                mNotificationManager.createNotificationChannel(mChannel);

                Intent closeIntent = new Intent(context, AlarmReminderReceiver.class);
                closeIntent.setAction("testAction");
                closeIntent.putExtra(AlarmUtils.ID, alarmId);
                PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new Notification.Builder(context)
                        .setContentTitle("New Messages")
                        .setContentText("You've received 0 new messages.")
                        .setSmallIcon(R.drawable.icon_launcher_world_clock)
                        .setChannelId(channel_id_1)
                        .setContentIntent(contentIntent)
                        .build();
                mNotificationManager.notify(NOTIFY_ID_1, notification);

            } else if("testAction".equals(action)) {
                int id = intent.getIntExtra(AlarmUtils.ID, -1);
//                AlarmUtils.enableAlarm(context, id, false);
//                Ala.disableAlert

//                AlarmUtils.disableAlert(context, id);
//                AlertUtils.disableAlertAndSetNextAlert(context, id);

                Intent alarmDismissIntent = new Intent(context, AlarmService.class);
                alarmDismissIntent.setAction(AlertUtils.ALERT_DISMISS);
//                alarmDismissIntent.putExtra(AlertUtils.ACTION_ALARM_ACTION, AlertUtils.ALERT_DISMISS);
                alarmDismissIntent.putExtra(AlarmUtils.ID, id);
                context.startService(alarmDismissIntent);

            }

        }

}
