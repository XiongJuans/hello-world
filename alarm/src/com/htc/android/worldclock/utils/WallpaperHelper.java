package com.htc.android.worldclock.utils;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.lockscreen.wallpaper.HtcLSWallpaperUtil;
/**
 * Wallpaper Helper
 */
public class WallpaperHelper {

    private static final String TAG = "WpHelp";

    private UIHandler mUIHandler;
    private BGHandler mBGHandler;
    private Looper mNonUiLooper;
    private Activity mActivity;
    private Context mContext;

    // Wallpaper Changed
    private static final String REGISTER_RECEIVER_WITH_PERMISSION = 
            "com.htc.permission.APP_DEFAULT";
    private BroadcastReceiver mWpChangedReceiver;
    private boolean mWallpaperChanged;
    private boolean mInitial;
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    /**
     * Wallpaper Helper
     * @param activity
     */
    public WallpaperHelper(Activity activity) {
        Log.i(TAG, "WHelper:" + activity);
        mWallpaperChanged = false;
        mInitial = false;
        mActivity = activity;
        mContext = (activity != null)? activity.getBaseContext():null;
        if (mNonUiLooper == null) {
            HandlerThread handerTread = new HandlerThread("WallpaperHelper BG");
            handerTread.start();
            mNonUiLooper = handerTread.getLooper();
        }
        mUIHandler = new UIHandler();
        mBGHandler = new BGHandler(mNonUiLooper);
        registerWallpaperChanged();
    }

    /**
     * set Wallpaper for activity of reminder view.
     * Because the background need the same as Lock Screen.
     */
    public void setWallpaper() {
        if (mActivity != null) {
            sendMessage(mBGHandler, WHAT_BG_CHECK_WALLPAPER);
        }
    }

    /** @hide */
    public void checkWallpaperChanged() {
        if (mActivity == null) {
            return;
        }
        if (mWallpaperChanged) {
            resetWindowStatus();
            sendMessage(mBGHandler, WHAT_BG_CHECK_WALLPAPER);
        } else if (mInitial) {
            Window window = mActivity.getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        }
    }

    /**
     * clean Up
     * Please call this API when onDestroy() for activity.
     */
    public void cleanUp() {
        Log.i(TAG, "cleanUp");
        mActivity = null;
        unregisterWallpaperChanged();
        removeMessage(mUIHandler, WHAT_UI_SET_WALLPAPER_DEFAULT);
        removeMessage(mUIHandler, WHAT_UI_SET_WALLPAPER_LOCKSCREEN);
        removeMessage(mBGHandler, WHAT_BG_CHECK_WALLPAPER);
        if (mNonUiLooper != null) {
            mNonUiLooper.quit();
            mNonUiLooper = null;
        }
    }

    private static final int WHAT_UI_SET_WALLPAPER_DEFAULT    = 1000;
    private static final int WHAT_UI_SET_WALLPAPER_LOCKSCREEN = 1001;
    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            switch (msg.what) {
            case WHAT_UI_SET_WALLPAPER_DEFAULT:
                removeMessage(mUIHandler, WHAT_UI_SET_WALLPAPER_DEFAULT);
                setDefaultWallpaper();
                break;
            case WHAT_UI_SET_WALLPAPER_LOCKSCREEN:
                removeMessage(mUIHandler, WHAT_UI_SET_WALLPAPER_LOCKSCREEN);
                setLockScreenWallpaper((Bitmap) msg.obj);
                break;
            }
        }
    }

    private void setDefaultWallpaper() {
        if (mActivity == null) {
            return;
        }
        Window window = mActivity.getWindow();
        if (window == null) {
            Log.w(TAG, "setDefaWp Fail");
            return;
        }
        Log.d(TAG, "setDefaWp");
        window.setFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        window.setFormat(PixelFormat.TRANSLUCENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mWallpaperChanged = false;
        mInitial = true;
    }

    private void setLockScreenWallpaper(Bitmap wallpaper) {
        if (mActivity == null) {
            return;
        }
        Window window = mActivity.getWindow();
        if (window == null) {
            Log.w(TAG, "setLSWp Fail");
            return;
        }
        if ((wallpaper == null) || wallpaper.isRecycled() || 
            (wallpaper.getHeight() <= 0) || (wallpaper.getWidth() <= 0)) {
            Log.w(TAG, "LSWp Invalid");
            setDefaultWallpaper();
            return;
        }
        Log.d(TAG, "setLSWp");
        try {
            // Added by amt_masd_li_yu to fix LockScreen picture shrink issue at 2017/03/02 [start]
            int dWidth = wallpaper.getWidth();
            int dHeight = wallpaper.getHeight();
            int vWidth = window.getWindowManager().getDefaultDisplay().getWidth();
            int vHeight = window.getWindowManager().getDefaultDisplay().getHeight();
            Log.d(TAG, "vWidth = " + vWidth + " , vHeight = " + vHeight + " , dWidth = " + dWidth + " , dHeight = " + dHeight);

            float dx = (dWidth - vWidth) * 0.5f;
            float dy = (dHeight - vHeight) * 0.5f;
            Log.d(TAG, "dx = " + dx + " dy = " + dy);
            if (dx > 0 || dy > 0) {
                if (dWidth * vHeight > vWidth * dHeight) {
                    // If width scale > height scale ,then cut picture width to fit screen size
                    wallpaper = Bitmap.createBitmap(wallpaper,
                            Math.round(dx),
                            0,
                            vWidth,
                            dHeight);
                } else {
                    // If height scale > width scale ,then cut picture height to fit screen size
                    wallpaper = Bitmap.createBitmap(wallpaper,
                            0,
                            Math.round(dy),
                            dWidth,
                            vHeight);
                }
            }
            // Added by amt_masd_li_yu to fix LockScreen picture shrink issue at 2017/03/02 [end]
            Drawable drawable = new BitmapDrawable(mActivity.getResources(), wallpaper);
            window.setBackgroundDrawable(drawable);
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, 
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } catch (Exception e) {
            Log.w(TAG, "setLSWp E: " + e);
        } catch (OutOfMemoryError e) {
             Log.w(TAG, "setLSWp E: " + e);
        }
        mWallpaperChanged = false;
        mInitial = true;
    }

    private void resetWindowStatus() {
        Window window = (mActivity != null)? mActivity.getWindow():null;
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            window.setFormat(PixelFormat.RGB_565);
            window.setBackgroundDrawable(null);
        }
    }

    private static final int WHAT_BG_CHECK_WALLPAPER = 2000;
    private class BGHandler extends Handler {
        public BGHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case WHAT_BG_CHECK_WALLPAPER:
                checkWallpaper();
                break;
            }
        }
    }

    private void checkWallpaper() {
        removeMessage(mBGHandler, WHAT_BG_CHECK_WALLPAPER);
        boolean isLockScreenWallpaper = false;
        ParcelFileDescriptor lockScreenWallpaperFD = null;
        //get sdk version to compare with android_N choose which method to get lockScreenWallpaper.
        if (DEBUG_FLAG) Log.d(TAG, "checkWallpaper: sdk version = " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            isLockScreenWallpaper = HtcLSWallpaperUtil.isLockScreenWallpaper(mContext);
        } else {
            int lockScreenWallpaperID = WallpaperManager.getInstance(mContext).getWallpaperId(WallpaperManager.FLAG_LOCK);
            lockScreenWallpaperFD = WallpaperManager.getInstance(mContext).getWallpaperFile(WallpaperManager.FLAG_LOCK);
            isLockScreenWallpaper = (lockScreenWallpaperID > 0 && lockScreenWallpaperFD != null);
        }
        if (!isLockScreenWallpaper) {
            sendMessage(mUIHandler, WHAT_UI_SET_WALLPAPER_DEFAULT);
        } else {
            // Get LockScreen Wallpaper
            Bitmap wallpaper = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                wallpaper = HtcLSWallpaperUtil.getLockScreenWallpaper(mContext);
            } else  {
                if (lockScreenWallpaperFD != null) {
                    if (DEBUG_FLAG) Log.d(TAG,"lockScreenWallpaperFD is not null");
                    wallpaper = BitmapFactory.decodeFileDescriptor(lockScreenWallpaperFD.getFileDescriptor());
                }
            }
            Message msg = Message.obtain();
            msg.what = WHAT_UI_SET_WALLPAPER_LOCKSCREEN;
            msg.obj = wallpaper;
            sendMessage(mUIHandler, msg);
        }
    }

    private void registerWallpaperChanged() {
        if (mContext != null && mWpChangedReceiver == null) {
            mWpChangedReceiver = new WallpaperChangedReceiver();
            IntentFilter filter = new IntentFilter(HtcLSWallpaperUtil.INTENT_ACTION_WALLPAPER_CHANGED);
            mContext.registerReceiver(mWpChangedReceiver, filter, REGISTER_RECEIVER_WITH_PERMISSION, null);
        }
    }

    private void unregisterWallpaperChanged() {
        if (mContext != null && mWpChangedReceiver != null) {
            mContext.unregisterReceiver(mWpChangedReceiver);
            mWpChangedReceiver = null;
        }
    }

    private class WallpaperChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "WCReceiver");
            mWallpaperChanged = true;
        }
    }

    private void sendMessage(Handler handler, int what) {
        sendMessage(handler, what, 0);
    }
    
    private void sendMessage(Handler handler, int what, long delay) {
        if (handler == null) {
            return;
        }
        if (delay > 0) {
            handler.sendEmptyMessageDelayed(what, delay);
        }
        else {
            handler.sendEmptyMessage(what);
        }
    }
    
    private void sendMessage(Handler handler, Message msg) {
        sendMessage(handler, msg, 0);
    }
    
    private void sendMessage(Handler handler, Message msg, long delay) {
        if (handler == null) {
            return;
        }
        if (delay > 0) {
            handler.sendMessageDelayed(msg, delay);
        }
        else {
            handler.sendMessage(msg);
        }
    }
    
    private void removeMessage(Handler handler, int what) {
        if (handler == null) {
            return;
        }
        handler.removeMessages(what);
    }
}
