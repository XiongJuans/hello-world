package com.example.xiongjuan.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.Toast;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.RemoteViews;


public class MainActivity extends AppCompatActivity {

    private Button check;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        check = (Button) findViewById(R.id.send);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(hasWeChat("com.tencent.mm")){  // com.tencent.mm
                    Toast.makeText(getApplicationContext(), "WeChat has already installed.", Toast.LENGTH_SHORT).show();
                    //createNotification();
                }else{
                    createNotification();
                    Log.i("xj","not notification");
                }
            }
        });

//        Uri uri = Uri.parse("www.baidu.com");
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        startActivity(intent);

    }

    public boolean hasWeChat(String packageName){
        PackageManager pm = getPackageManager();
        boolean installed =false;
        try {
            pm.getPackageInfo(packageName,PackageManager.GET_ACTIVITIES);
            installed =true;
        } catch(PackageManager.NameNotFoundException e) {
            installed =false;
        }
        return  installed;
    }

    public void createNotification(){
        int icon = R.mipmap.ic_launcher;
        CharSequence tickerText = "Notification01";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.activity_action);
        contentView.setImageViewResource(R.id.image, R.mipmap.ic_launcher);
        contentView.setTextViewText(R.id.title, "Title");
        contentView.setTextViewText(R.id.text, "message");
        notification.contentView = contentView;


        Uri uri = Uri.parse("http://www.caihcom.com/m/htcdownload");
        Intent it = new Intent(Intent.ACTION_VIEW, uri);
        //Intent notificationIntent = new Intent(this, NotificAtion.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, it, 0);
        notification.contentIntent = contentIntent;

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0,notification);


//        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.download_link)));
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,  intent, PendingIntent.FLAG_CANCEL_CURRENT);
//        builder.setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(getResources().getString(R.string.notification_esim_title))
//                .setContentText(getResources().getString(R.string.notification_esim_msg))
//                .setContentIntent(contentIntent)
//                .setAutoCancel(true);
//        //.setWhen(System.currentTimeMillis());
//        notifyManager.notify(0, builder.build());
    }

}


