package com.htc.datausagemonitor.floatwindow;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.datausagemonitor.HomeActivity;
import com.htc.datausagemonitor.R;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import android.os.Handler;

import com.htc.datausagemonitor.Util;

/**
 * Created by nancy on 8/24/17.
 */
public class FloatWindowManager {
    public final static String TAG = FloatWindowManager.class.getSimpleName();
    private static FloatWindowManager instance;

    private WindowManager mWindowManager;
    private LayoutParams mLayout;
    private FloatWindowLayout mFloatView = null;
    private TextView mDataUsageTv = null;
    private ImageView mMobileTypeIcon = null;

    // 声明屏幕的宽高
    private float x, y;

    private long rxtxTotal = 0;
    private  int statusBarHigh = -1;
    private DecimalFormat showFloatFormat = new DecimalFormat("0.00");
    private  static boolean isShow = false;

    public static FloatWindowManager getInstance() {
        if (instance == null) {
            instance = new FloatWindowManager();
        }
        return instance;
    }


    private WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    public void createWindow(final Context context, boolean isShowOnCreate) {
        final WindowManager windowManager = getWindowManager(context);
        int mobileType = -1;

        statusBarHigh = Util.getPrefInt(context, Util.SP_STATUSBAR_HEIGHT);

        final ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = connectivityManager
                    .getActiveNetworkInfo();

        if (mLayout == null) {
            mLayout = getWindowParams(context);
        }
        if (mFloatView == null) {
            mFloatView = new FloatWindowLayout(context);
            if (netInfo != null && isShowOnCreate) {
                mobileType = netInfo.getType();
                if (mobileType ==ConnectivityManager.TYPE_WIFI || mobileType == ConnectivityManager.TYPE_WIFI) {
                    windowManager.addView(mFloatView, mLayout);
                    isShow = true;
                } else {
                    Log.w(TAG, "createWindow: networkInfo == null");
                }
            }

            mFloatView.setOnTouchListener(new View.OnTouchListener() {
                float mTouchStartX;
                float mTouchStartY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // 获取相对屏幕的坐标，即以屏幕左上角为原点
                    x = event.getRawX();
                    y = event.getRawY() - statusBarHigh;
                    Log.i(TAG, " startX:" + mTouchStartX + " startY:"
                            + mTouchStartY + " statusBarHigh:" + statusBarHigh);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // 获取相对View的坐标，即以此View左上角为原点
                            mTouchStartX = event.getX();
                            mTouchStartY = event.getY();
                            Log.i(TAG, "ACTION_DOWN: startX " + mTouchStartX + "====startY"
                                    + mTouchStartY);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // 更新浮动窗口位置参数
                            mLayout.x = (int) (x - mTouchStartX);
                            mLayout.y = (int) (y - mTouchStartY);

                            mWindowManager.updateViewLayout(v, mLayout);
                            break;
                        case MotionEvent.ACTION_UP:
                            Log.i(TAG, "ACTION_UP: startX " + mTouchStartX + "====startY"
                                    + mTouchStartY);
                            // 更新浮动窗口位置参数
                            mLayout.x = (int) (x - mTouchStartX);
                            mLayout.y = (int) (y - mTouchStartY);

                            //TODO: emulate Click event
                            float deltX = event.getX() - mTouchStartX;
                            float deltY = event.getY() - mTouchStartY;
                            if ((deltX < 4 && deltX > -4) && (deltY < 4 && deltY > -4)) {
                                Log.w(TAG, "got 4x4 area, would treat as click event");
                                final Intent intent = new Intent(context, HomeActivity.class);
                                context.startActivity(intent);
                            } else {
                                Log.w(TAG, "moved out of 4x4 area(" + deltX + ", " + deltY
                                        + "), not click event");
                                mWindowManager.updateViewLayout(v, mLayout);
                            }
                            saveLastSp(context, mLayout.x, mLayout.y);
                            mTouchStartX = mTouchStartY = 0;
                            break;
                    }
                    return true;
                }
            });

        }
        mDataUsageTv = (TextView) mFloatView.findViewById(R.id.data_usage_tv);
        mMobileTypeIcon = (ImageView)mFloatView.findViewById(R.id.mobile_type_imgv);
        updateMobileTypeIcon(mobileType);
    }

    public void hideWindow(Context context) {
        Log.d(TAG, "hideWindow mFloatView != null :" + (mFloatView != null) + " isShow =" + isShow);
        if (mFloatView != null && isShow == true) {
            WindowManager windowManager = getWindowManager(context);
            windowManager.removeView(mFloatView);
            isShow = false;
        }
    }

    public void showWindow(Context context, int netType) {
        Log.d(TAG, "showWindoe mFloatView != null :" + (mFloatView != null) + " isShow =" + isShow);
        if (mFloatView != null && isShow == false) {
            WindowManager windowManager = getWindowManager(context);
            windowManager.addView(mFloatView, mLayout);
            isShow = true;
        }
        updateMobileTypeIcon(netType);
    }

    public void closeWindow(Context context) {  //
        Log.d(TAG, "closeWindow mFloatView != null :" + (mFloatView != null) + " isShow =" + isShow);
        if (mFloatView != null ) {
            WindowManager windowManager = getWindowManager(context);
            if (isShow == true) {
                windowManager.removeView(mFloatView);
            }
            mFloatView = null;
            isShow = false;
        }
    }

    private LayoutParams getWindowParams(Context context) {
        LayoutParams windowParams = new WindowManager.LayoutParams();
        windowParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.flags = LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL;
        windowParams.gravity = Gravity.START | Gravity.TOP;
        windowParams.width = LayoutParams.WRAP_CONTENT;
        windowParams.height = LayoutParams.WRAP_CONTENT;
        int x = Util.getPrefInt(context, Util.SP_X);
        int y = Util.getPrefInt(context, Util.SP_X);
        if (x == -1 || y == -1) {
            x = 0;
            y = Util.getPrefInt(context, Util.SP_STATUSBAR_HEIGHT);
        }
        windowParams.x = x;
        windowParams.y = y;
        return windowParams;
    }

    public void initData() {
        rxtxTotal = TrafficStats.getTotalRxBytes()
                + TrafficStats.getTotalTxBytes();
    }


    public void updateViewData(Context context) {
        long tempSum = TrafficStats.getTotalRxBytes()
                + TrafficStats.getTotalTxBytes();
        long rxtxLast = tempSum - rxtxTotal;
        double totalSpeed = rxtxLast * 1000 / Util.TIME_SPAN;
        rxtxTotal = tempSum;
        if (mFloatView != null && mDataUsageTv != null && totalSpeed >= 0d) {
            mDataUsageTv.setText(showSpeed(totalSpeed));
        }
    }


    private String showSpeed(double speed) {
        String speedString;
        if (speed >= 1048576d) {
            speedString = showFloatFormat.format(speed / 1048576d) + "MB/s";
        } else {
            speedString = showFloatFormat.format(speed / 1024d) + "KB/s";
        }
        return speedString;
    }

    public boolean isWindowShowing() {
        if (mFloatView != null) {
            return  true;
        }
        return false;
    }

    public boolean hasFloatWindow() {
        if (mFloatView != null) {
            return  true;
        }
        return false;
    }

    public void saveLastSp (Context context, int x, int y) {
        Util.savePrefInt(context, Util.SP_X, x);
        Util.savePrefInt(context, Util.SP_Y, y);
    }

    public void updateMobileTypeIcon(int mobileType) {
        if (mMobileTypeIcon ==  null) {
            Log.e(TAG, "updataMobileTypeIcon failed since mMoblieTypeIcon == null");
            return;
        }
        if (mobileType == ConnectivityManager.TYPE_WIFI) {//Wifi
            mMobileTypeIcon.setImageResource(R.drawable.icon_indicator_wifi_dark_s);
        } else if (mobileType == ConnectivityManager.TYPE_MOBILE) {//Mobile
            mMobileTypeIcon.setImageResource(R.drawable.icon_indicator_mobile_data_dark_s);
        } else {//Mobile
            mMobileTypeIcon.setImageResource(android.R.drawable.presence_busy);
        }
    }

}
