package com.htc.android.worldclock.alarmclock;

import java.util.ArrayList;

import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.SettingsActivity;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.lockscreen.reminder.HtcReminderClient;
import com.htc.lib1.lockscreen.reminder.HtcReminderManager;
import com.htc.lib1.lockscreen.reminder.HtcReminderViewMode;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

/**
 * TODO:
 * AlarmAlertReminderManager:
 * 1. Create HtcReminderManager
 * 2. Register HtcReminderClient to listen the callback for onViewModeChange().
 * 3. Register ViewMode when request to show view.
 * 4. Listen onViewModeChange(CurrentViewMode) to show and hide your view.
 * 5. Unregister ViewMode when your view disappear.
 * 6. Unregister HtcReminderClient if there is no any ViewMode.
 * 7. CleanUp HtcReminderManager when onDestroy().
 */
public class AlarmAlertReminderManager {
    private static final String TAG = "WorldClock.AlarmAlertReminderManager";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    
    private static AlarmAlertReminderManager mInstance;
    private Context mContext;
    private HtcReminderManager mHtcReminderManager;
    private HtcReminderClient mHtcReminderClient;
    private ArrayList<Integer> mViewMode = new ArrayList<Integer>();
    private int currentViewMode = HtcReminderViewMode.INVALID_MODE;
    
    public static synchronized AlarmAlertReminderManager getInstance() {
        if (mInstance == null) {
            mInstance = new AlarmAlertReminderManager();
        }
        return mInstance;
    }
    
    private AlarmAlertReminderManager() {
    }
    
    public void init(Context context) {
        if (DEBUG_FLAG) Log.d(TAG, "init");
        mContext = context;
        if (mHtcReminderManager == null) {
            mHtcReminderManager = new HtcReminderManager(mContext);
        }
    }
    
    public void cleanUp() {
        if (DEBUG_FLAG) Log.d(TAG, "cleanUp");
        if (mHtcReminderManager != null) {
            mHtcReminderManager.cleanUp();
            mHtcReminderManager = null;
        }
        mHtcReminderClient = null;
        if (mViewMode != null) {
            mViewMode.clear();
            mViewMode = null;
        }
    }

    private void initReminderClient(final Context context, final int id, final long time, final String description,
                                    final int alarmType, final boolean isLockScreen) {
        if (mHtcReminderClient == null) {
            mHtcReminderClient = new HtcReminderClient() {
                @Override
                public void onViewModeChange(int viewMode) {
                    if (DEBUG_FLAG) Log.d(TAG, "initReminderClient - onViewModeChange: viewMode = " + viewMode);
                    if (viewMode == HtcReminderViewMode.ALARM_MODE) {
                        AlertUtils.alarmAlert(context, id, time, description, alarmType, isLockScreen);
//                        AlertUtils.sendReminderReadyIntentToAlarmService(context);
                    } else {
                        // TODO:
                        // If you aren't the current view mode.
                        // Please follow your design to do the proper handling.
                        if (viewMode <= HtcReminderViewMode.ALARM_MODE) { // priority is higher than alarm
                            if (HtcReminderViewMode.ALARM_MODE == currentViewMode) {
                                if (DEBUG_FLAG) Log.d(TAG, "onViewModeChange: view mode's priority is higher than alarm, snooze alarm " + id);
                                AlertUtils.sendAlarmSnoozeIntent(context, id, description);
                            }
                        }
                    }
                    currentViewMode = viewMode;
                }

                @Override
                public Bundle sendCommand(String action, Bundle extras) {
                    return null;
                }

                @Override
                public void unlock() {
                    // TODO:
                    // We will notify the client and clear all ViewMode on Service side
                    // if there is the activity to call unlock().
//                    MyUtil.sendMessage(mUIHandler, WHAT_CLEAR_VIEW_MODE);
                    if (HtcReminderViewMode.ALARM_MODE != currentViewMode) {
                        // other app to call unlock
                        AlertUtils.sendAlarmDismissIntent(context, id);
                    }
                    if (DEBUG_FLAG) Log.d(TAG, "onViewModeChange: clearAllViewMode");
                    clearAllViewMode();
                }
            };
            registerClient();
        }
    }
    
    public int getActiveViewMode() {
        return currentViewMode;
    }
    
    /**
     * Register Client
     */
    private void registerClient() {
        synchronized (this) {
            if (DEBUG_FLAG) Log.d(TAG, "registerClient");
            if (mHtcReminderManager != null) {
                mHtcReminderManager.registerCallback(mHtcReminderClient);
            }
        }
    }
    
    /**
     * Unregister Client
     */
    private void unregisterClient() {
        synchronized (this) {
        if (DEBUG_FLAG) Log.d(TAG, "unregisterClient");
            if (mHtcReminderManager != null) {
                mHtcReminderManager.unregisterCallback(mHtcReminderClient);
                mHtcReminderClient = null;
            }
        }
    }
    
    /**
     * Register View Mode
     */
   public void initRegisterViewMode(Context context, int viewMode, int id, long time, String description, int alarmType, boolean isLockScreen) {
        initReminderClient(context, id, time, description, alarmType, isLockScreen);
        if (DEBUG_FLAG) Log.d(TAG, "registerViewMode: " + viewMode);
        if (mHtcReminderManager != null) {
            mHtcReminderManager.registerViewMode(viewMode);
            if (mViewMode != null && !mViewMode.contains(viewMode)) {
                mViewMode.add(viewMode);
            }
        }
    }
    
   /**
    * Unregister View Mode
    */
    public void unregisterViewMode(int viewMode) {
        if (DEBUG_FLAG) Log.d(TAG, "unregisterViewMode: " + viewMode);
        if (mHtcReminderManager != null) {
            mHtcReminderManager.unregisterViewMode(viewMode);
            if (mViewMode != null && mViewMode.contains(viewMode)) {
                mViewMode.remove(new Integer(viewMode));
            }
        }
        checkClientState();
    }
    
    /**
     * Unlock
     */
    public void unlock(Activity activity) {
        if (DEBUG_FLAG) Log.d(TAG, "unlock: " + activity);
        if (mHtcReminderManager != null) {
            keyguardUnlock(activity);
        }
    }
    
    public void requestShowIdleScreen() {
        if (DEBUG_FLAG) Log.d(TAG, "requestShowIdleScreen");
        if (mHtcReminderManager != null) {
            mHtcReminderManager.sendCommand(HtcReminderManager.ACTION_BACK_TO_LOCKSCREEN, null);
        }
    }
    
    /**
     * UnlockAndFinish
     */
    public void requestUnlockAndFinish(Activity activity, PendingIntent pendingIntent) {
        if (DEBUG_FLAG) Log.d(TAG, "unlock: " + activity);
        if (mHtcReminderManager != null) {
            keyguardRequestUnlockAndFinish(activity, pendingIntent);
        }
    }
    
    private void clearAllViewMode() {
        if (DEBUG_FLAG) Log.d(TAG, "clearAllViewMode");
        if (mViewMode != null) {
            mViewMode.clear();
        }
        checkClientState();
    }
    
    private void checkClientState() {
        if (!(mViewMode != null && mViewMode.size() > 0)) {
            unregisterClient();
        }
    }

/* ------------------------------------------------------------------------------------------ */
    
    private void keyguardUnlock(final Activity activity) {
        // Dismiss Keyguard.
        if (activity != null) {
            activity.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    Window w = activity.getWindow();
                    if (w != null) {
                        w.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                    }
                }
            });
        }
        notifyInUnlocking();
    }

    private void notifyInUnlocking() {
        // Notify ReminderService when App is in unlocking.
        if (mHtcReminderManager != null) {
            mHtcReminderManager.sendCommand(HtcReminderManager.ACTION_NOTIFY_IN_UNLOCKING, null);
        }
    }

    Handler mUIHandler = new Handler(Looper.getMainLooper());

    public void keyguardRequestUnlockAndFinish(final Activity activity, final PendingIntent intent) {
        if (DEBUG_FLAG) Log.d(TAG, "reqUnlockFinish: " + activity + ", " + intent);
        // Dismiss Keyguard.
        activity.runOnUiThread(new Runnable(){
            @Override
            public void run() {
                Window w = activity.getWindow();
                if (w != null) {
                    w.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                }
            }
        });
        // Notify ReminderService for unlocking.
        notifyInUnlocking();
        // Start activity and Finish itself.
        if (mUIHandler != null) {
            mUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    activity.runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            startActivityAndFinish(activity, intent);
                        }
                    });
                }
            }, 100);
        } else {
            Log.w(TAG, "reqUnlockFinish: Handle NULL");
        }
    }
    
    private void startActivityAndFinish(Activity activity, PendingIntent intent) {
        try {
            if (DEBUG_FLAG) Log.d(TAG, "startActivityAndFinish");
         if (intent != null) {
                intent.send();
            }
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.finish();
            } else {
                Log.w(TAG, "activity already finished");
            }
        } catch (Exception e) {
            Log.w(TAG, "startActiFinish E: " + e);
        }
    }
}
