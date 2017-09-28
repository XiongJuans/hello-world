package com.htc.datausagemonitor.floatwindow;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.htc.datausagemonitor.Util;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by nancy on 8/24/17.
 */


public class FloatWindowService extends Service {
    private final static String TAG = FloatWindowLayout.class.getSimpleName();
    public final static String ACTION = "FLOAT_WINDOW_ACTION";

    public final static int MSG_CONN_CHANGE_EVENT = 0x01;
    public final static int MSG_CONN_STATUS_SHOW = 0x02;
    public final static int MSG_CONN_STATUS_HIDE = 0x03;

    private Handler handler = new Handler();
    private Timer timer;
    private boolean isShowFirstTime = true;

    private ConnectivityManager mConnManager;
    private NetworkRequest mConnListener;
    private ConnectivityManager.NetworkCallback mConnCallback;
    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        int statusBarHight = getStatusBarHeight();
        Util.savePrefInt(mContext, Util.SP_STATUSBAR_HEIGHT, statusBarHight);
        Log.d(TAG, "statusBarHight = " + statusBarHight);
        mConnManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        mConnListener = builder.build();
        mConnCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "onAvailable hasFloatWindow:" + FloatWindowManager.getInstance().hasFloatWindow());
                Message msg = mNetWorkChangeHandler.obtainMessage(MSG_CONN_CHANGE_EVENT, MSG_CONN_STATUS_SHOW);
                msg.arg1 = MSG_CONN_STATUS_SHOW;
                mNetWorkChangeHandler.sendMessage(msg);
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "onLost");
                Message msg = mNetWorkChangeHandler.obtainMessage(MSG_CONN_CHANGE_EVENT);
                msg.arg1 = MSG_CONN_STATUS_HIDE;
                mNetWorkChangeHandler.sendMessage(msg);
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new RefreshTask(), 0L, 3 * 1000);
        }
        mConnManager.registerNetworkCallback(mConnListener, mConnCallback);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Service被终止的同时也停止定时器继续运行
        timer.cancel();
        timer = null;
        FloatWindowManager.getInstance().closeWindow(getApplicationContext());
        mConnManager.unregisterNetworkCallback(mConnCallback);
    }

    private Handler mNetWorkChangeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Log.d(TAG, "mNetWorkChangeHandler handleMessage what= " + what);
            if (what == MSG_CONN_CHANGE_EVENT) {
                int arg1 = msg.arg1;
                Log.d(TAG, "arg1 = " + arg1);
                if (arg1 == MSG_CONN_STATUS_SHOW) {
                    NetworkInfo info = mConnManager.getActiveNetworkInfo();
                    if(info == null) {
                        Log.e(TAG, "mNetWorkChangeHandler:MSG_CONN_STATUS_SHOW networkInfo == null");
                        return;
                    }
                    //Util.savePrefInt(mContext, Util.SP_MOBILE_TYPE, info.getType());
                    if (FloatWindowManager.getInstance().hasFloatWindow() == true) {
                        FloatWindowManager.getInstance().showWindow(getApplicationContext(), info.getType());
                    }
                } else if (arg1 == MSG_CONN_STATUS_HIDE) {
                    if (FloatWindowManager.getInstance().hasFloatWindow() == true) {
                        FloatWindowManager.getInstance().hideWindow(getApplicationContext());
                    }
                }
            }

        }
    };

    class RefreshTask extends TimerTask {

        @Override
        public void run() {
            Log.d(TAG, "RefreshTask---run");
            if (FloatWindowManager.getInstance().hasFloatWindow() == false) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        FloatWindowManager.getInstance().initData();
                        FloatWindowManager.getInstance().createWindow(getApplicationContext(), isShowFirstTime);
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        FloatWindowManager.getInstance().updateViewData(getApplicationContext());
                    }
                });
            }
        }

    }


    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object o = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = (Integer) field.get(o);
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


}
