package com.htc.android.worldclock.utils;

import com.htc.android.worldclock.alarmclock.AlarmAlert;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.reminder.ui.ReminderView;
import com.htc.lib1.theme.ThemeType;
import com.htc.lib3.windowapi.HtcWrapWindowManager;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * ReminderActivity
 */
public class ReminderActivity extends Activity {
    private static final String TAG = "WorldClock.ReminderActivity";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    
    private WallpaperHelper mWallpaperHelper;
    private ReminderView mReminderView;
    
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
    
    /**
     * onCreate
     * @param Bundle
     */
    @Override
    protected void onCreate(Bundle arg0) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        super.onCreate(arg0);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        
        // set Transparent for Status Bar.
        // WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        // When this flag is enabled for a window, 
        // it automatically sets the system UI visibility flags 
        // SYSTEM_UI_FLAG_LAYOUT_STABLE and SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.
        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        // set Theme for Translucent and NoTitleBar.
        setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        
        try {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            HtcWrapWindowManager.addCustomFlags(attrs, HtcWrapWindowManager.CUSTOM_FLAG_FORBID_IMMERSIVE_CONFIRMATION);
            HtcWrapWindowManager.addCustomFlags(attrs, HtcWrapWindowManager.CUSTOM_FLAG_FORBID_TRANSIENT_STATUS_BAR);
            HtcWrapWindowManager.addCustomFlags(attrs, HtcWrapWindowManager.CUSTOM_FLAG_FORBID_TRANSIENT_NAVIGATION_BAR);
        } catch (NoSuchFieldException e) {
            // This app is not running on the HTC's device
            Log.w(TAG, "Custom window flag not supported: e = " + e.toString());
        } catch (Exception e) {
            Log.w(TAG, "onCreate Exception: e = " + e.toString());
        }
        // +++ Hide Navigation Bar UI
        View decorView = getWindow().getDecorView();
        if (decorView != null) {
            int flag = decorView.getSystemUiVisibility() | 
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(flag);
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override 
                public void onSystemUiVisibilityChange(int visibility) {
                    // set hide navigation again
                    View decorView = getWindow().getDecorView();
                    if (decorView != null) {
                        int flag = decorView.getSystemUiVisibility() | 
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                        decorView.setSystemUiVisibility(flag);
                    }
                }
            });
        }
        // --- Hide Navigation Bar UI
        // ++++++++++
        // The Wallpaper of HtcReminderView should be the same as Lock Screen.
        // Because it may need the time to load the wallpaper from LockScreen.
        // So, we will setFlag(FLAG_SHOW_WHEN_LOCKED) after the Wallpaper is ready.
        // Otherwise, you may encounter the flag black screen problem.
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mWallpaperHelper = new WallpaperHelper(this);
        if (mWallpaperHelper != null) {
            mWallpaperHelper.setWallpaper();
        }
        // ----------
    }

    /**
     * onDestroy
     */
    @Override
    protected void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy");
        if (mReminderView != null) {
            mReminderView.cleanUp();
        }
        if (mWallpaperHelper != null) {
            mWallpaperHelper.cleanUp();
        }
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onDestroy();
    }

    /**
     * onPause
     */
    @Override
    protected void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        if (mReminderView != null) {
            mReminderView.onPause();
        }
        super.onPause();
    }

    /**
     * onResume
     */
    @Override
    protected void onResume() {
        if (DEBUG_FLAG) Log.d(TAG, "onResume");
        if (mReminderView != null) {
            mReminderView.onResume();
        }
        super.onResume();
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(ReminderActivity.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
    }

    /**
     * onStart
     */
    @Override
    protected void onStart() {
        if (DEBUG_FLAG) Log.d(TAG, "onStart");
        if (mReminderView != null) {
            mReminderView.onStart();
        }
        if (mWallpaperHelper != null) {
            mWallpaperHelper.checkWallpaperChanged();
        }
        super.onStart();
    }

    /**
     * onStop
     */
    @Override
    protected void onStop() {
        if (DEBUG_FLAG) Log.d(TAG, "onStop");
        if (mReminderView != null) {
            mReminderView.onStop();
        }
        super.onStop();
    }

    /**
     * set Reminder View
     * @param view
     */
    protected void setReminderView(ReminderView view) {
        if (mReminderView != view) {
            if (mReminderView != null) {
                mReminderView.cleanUp();
            }
            mReminderView = view;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
