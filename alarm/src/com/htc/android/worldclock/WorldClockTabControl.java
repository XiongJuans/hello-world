package com.htc.android.worldclock;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.htc.android.worldclock.alarmclock.AlarmItem;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.utils.AlertUtils;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcSkinUtils;
import com.htc.android.worldclock.utils.HtcStorageChecker;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.android.worldclock.worldclock.CityTime;
import com.htc.android.worldclock.worldclock.JobScheduleService;
import com.htc.android.worldclock.worldclock.WorldClock;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.theme.ThemeFileUtil;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherUtility;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcAlertDialog;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.theme.ThemeType;

public class WorldClockTabControl extends Activity {
    private static final String TAG = "WorldClock.WorldClockTabControl";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    public final static String LAUNCH_AP_ACTIVITY_NAME = WorldClockTabControl.class.getName();
    public final static int WORLDCLOCK_REQUEST_ADD = 0x0000;
    public final static int ALARMCLOCK_REQUEST_ADD = 0x0001;
    public final static int TIMER_REQUEST_ADD = 0x0002;
    public final static int NO_DB = 0x0003;
    public final static int FAIL_QUERYING = 0x0004;
    public final static int INTERNAL_STORAGE_FULL = 0x0005;
    public final static int LOCATION_SERVICE = 0x0006;

    private WorldClockTabControlResUtils mWorldClockTabControlResUtils;

    private CarouselTab mCarouselTab;
    private FrameLayout mMainView;

    //WorldClock city data cache
    private CityTime mCachedCityTimeCurrent = null;
    private CityTime mCachedCityTimeHome = null;
    private CityTime[] mCachedCityTimeCities = null;
    private boolean mIsWorldClockCachedDataReady = false;
    private ArrayList<AlarmItem> mAlarmClockList = null;
    
    private boolean mIsNavigationBarExist = false;
    // Htc font scale
    private boolean mHtcFontscale = false;
    // Htc Theme
    private boolean mIsThemeChanged = false;
    private static final int BACKGROUND_JOB_ID = 0;
    public static final int MSG_SERVICE_JOB = 0;
    private JobScheduler mJobScheduler = null;

    private static class JobHandler extends Handler {
        private WeakReference<WorldClockTabControl> mActivity;

        public JobHandler(WorldClockTabControl activity) {
            mActivity =  new WeakReference<WorldClockTabControl>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final WorldClockTabControl activity = mActivity.get();
            if (activity != null && msg.what == MSG_SERVICE_JOB) {
                activity.schedulerJob(activity, JobScheduleService.class, BACKGROUND_JOB_ID);
            }
        }
    }


    HtcCommonUtil.ThemeChangeObserver mThemeChangeObserver = new HtcCommonUtil.ThemeChangeObserver() {
        @Override
        public void onThemeChange(int type) {
                if (type == ThemeType.HTC_THEME_FULL || type == ThemeType.HTC_THEME_CC) {
                    mIsThemeChanged = true;
                }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_FLAG) Log.d(TAG, "onCreate");
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][WorldClockTabControl][START]");
        Log.i(TAG, "isHEPDevice = " + Global.isHEPDevice(this));
        
        if (Global.isSupportAccChinaSense()) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
            getActionBar().hide();
        }
        mHtcFontscale = HtcSkinUtils.initHtcFontScale(this);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(savedInstanceState);
        
        // ATS Log
        if (DEBUG_FLAG) Log.d(TAG, "[ATS][com.htc.android.worldclock][press_widget][turning_on]");
        //to fix m10 flush issue
        this.getWindow().setFormat(PixelFormat.RGBA_8888);
        mIsNavigationBarExist = ResUtils.hasNavigationBar(this);
        PreferencesUtil.setSyncAlarmClockDB(this, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // WorldClock
                getWorldClockCachedData();
                // AlarmClock
                getAlarmClockCachedData();
                // SetAlarm
                getSetAlarmCachedData();
            }
        }).start();

        mWorldClockTabControlResUtils = new WorldClockTabControlResUtils(WorldClockTabControl.this, null);
        mWorldClockTabControlResUtils.initResources();
        mWorldClockTabControlResUtils.initTheme();

        //enable translucent mode for status bar
        ResUtils.enableStatusBarTheme(this);

        mMainView = new FrameLayout(this);
        if (!Global.isSupportAccChinaSense()) {
            // reserve ActionBar's space
            mMainView.setFitsSystemWindows(true);
        }
        mMainView.setId(123);
        setContentView(mMainView, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));

        FragmentManager fm = getFragmentManager();
        Fragment pagerFragment = fm.findFragmentById(123);
        FragmentTransaction transaction = fm.beginTransaction();
        if(pagerFragment == null) {
            transaction.add(123, pagerFragment = new CarouselTab(), "ClockPager");
            transaction.commit();
        }

        Log.i(TAG, "Is backup alarm db to google server = " + PreferencesUtil.getBackupAlarmDB(this));
        //if sdkVersion greater than or equal to 24 start JobSchedulerService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent startServiceIntent = new Intent(this, JobScheduleService.class);
            JobHandler jobHandler = new JobHandler(this);
            startServiceIntent.putExtra("messenger", new Messenger(jobHandler));
            startService(startServiceIntent);
        }
        if (DEBUG_FLAG) Log.d(TAG, "onCreate end");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG_FLAG) Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        Boolean isLocationEnabled = intent.getBooleanExtra(CarouselTab.EXTRA_LOCATION_SERVICE, true);
        if (DEBUG_FLAG) Log.d(TAG, "onNewIntent: isLocationEnabled = " + isLocationEnabled);
        String lastTab = intent.getStringExtra(CarouselTab.WORLDCLOCK_ACTION);
        if (DEBUG_FLAG) Log.d(TAG, "onNewIntent, lastTab = " + lastTab);
        // first priority, check location service
        if (!isLocationEnabled) {
            finish();
            // restart clock to launch worldclock page
            Intent intentLocService = new Intent(this, WorldClockTabControl.class);
            intentLocService.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_WORLDCLOCK);
            intentLocService.putExtra(CarouselTab.EXTRA_LOCATION_SERVICE, false);
            startActivity(intentLocService);
        }
        

        if (lastTab == null) {
            return;
        }

        if (lastTab.equals(CarouselTab.TAB_WORLDCLOCK)
            || lastTab.equals(CarouselTab.TAB_ALARM)
            || lastTab.equals(CarouselTab.TAB_STOPWATCH)
            || lastTab.equals(CarouselTab.TAB_TIMER)) {
            // don't set tab due to Carousel object not ready, will be set tab by setTabs function on CarouselTab
            if (mCarouselTab != null) {
                mCarouselTab.setCurrentTabTag(lastTab);
            }
        } else {
            Log.w(TAG, "WorldClockTabControl.onNewIntent: unknow tab = " + lastTab);
        }
    }

    @Override
    protected void onStart() {
        if (DEBUG_FLAG) Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG_FLAG) Log.d(TAG, "onResume");
        super.onResume();
        HtcStorageChecker.checkStorageFull(this);
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(WorldClockTabControl.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
    }

    @Override
    protected void onPause() {
        if (DEBUG_FLAG) Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (DEBUG_FLAG) Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG_FLAG) Log.d(TAG, "onDestroy");
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cancelAllJobs();
            Intent stopIntent = new Intent(this, JobScheduleService.class);
            stopService(stopIntent);
        }
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        int titleResId = 0;
        int messageResId = 0;
        switch (id) {
            case WORLDCLOCK_REQUEST_ADD:
                titleResId = R.string.add_city_error_title;
                messageResId = R.string.add_city_error_msg;
                break;
            case ALARMCLOCK_REQUEST_ADD:
                titleResId = R.string.error;
                messageResId = R.string.add_alarm_error;
                break;
            case NO_DB:
                titleResId = R.string.error;
                messageResId = R.string.dberror;
                break;
            case FAIL_QUERYING:
                titleResId = R.string.error_title;
                messageResId = R.string.query_fail;
                break;
            case INTERNAL_STORAGE_FULL:
                titleResId = R.string.error_title;
                messageResId = R.string.internal_memory_low;
                break;
            case LOCATION_SERVICE:
                return new HtcAlertDialog.Builder(this)
                        .setTitle(this.getString(R.string.location_dialog_title))
                        .setMessage(this.getString(R.string.location_dialog_msg))
                        .setNegativeButton(R.string.bt_cancel_str, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (DEBUG_FLAG) Log.i(TAG, "DIALOG_ENABLE_[Location] - no");
                            }
                        })
                        .setPositiveButton(R.string.nn_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "DIALOG_ENABLE_[Location] - isNeedToShowEnableLoc");
                                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(intent);
                                    }
                                }).start();
                            }
                        })
                       .create();
            default:
                return null;
        }
        return new HtcAlertDialog.Builder(this)
            .setTitle(getString(titleResId))
            .setMessage(getString(messageResId))
            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            }).create();
    }

    public void setCarouselTab(CarouselTab carouselTab) {
        mCarouselTab = carouselTab;
    }

    public CarouselTab getCarouselTab() {
        return mCarouselTab;
    }

    public WorldClockTabControlResUtils getResUtilsInstance() {
        return mWorldClockTabControlResUtils;
    }

    private void getWorldClockCachedData() {
        if (PreferencesUtil.getSyncWorldClockDB(WorldClockTabControl.this) == true) {
            if (DEBUG_FLAG) Log.d(TAG, "getWorldClockCachedData: use preference to load city data");
            if (WorldClock.isUseWirelessNetworks(WorldClockTabControl.this.getBaseContext())) {
                WeatherLocation[] currentLoc = WeatherUtility.loadLocations(WorldClockTabControl.this.getContentResolver(), WorldClock.APP_LOCATION_SERVICE);
                mCachedCityTimeCurrent = PreferencesUtil.getCityTimeCurrent(WorldClockTabControl.this);
                String cachedCurrentTimezoneId = "";

                if (mCachedCityTimeCurrent != null) {
                    WeatherLocation wl = mCachedCityTimeCurrent.getWeatherLocation();
                    if (wl != null) {
                        cachedCurrentTimezoneId = wl.getTimezoneId();
                    }
                }

                if ((mCachedCityTimeCurrent != null) && (TextUtils.isEmpty(cachedCurrentTimezoneId) == false)
                    && (currentLoc != null) && (currentLoc.length != 0)) {
                    if ((currentLoc[0].getName().equals(mCachedCityTimeCurrent.getCityId()) != true)
                        || (currentLoc[0].getTimezoneId().equals(cachedCurrentTimezoneId) != true)) {
                        PreferencesUtil.setSyncWorldClockDB(WorldClockTabControl.this, false);
                        return;
                    }
                } else {
                    PreferencesUtil.setSyncWorldClockDB(WorldClockTabControl.this, false);
                    return;
                }
            } else {
                mCachedCityTimeCurrent = null;
            }
            mCachedCityTimeHome = PreferencesUtil.getCityTimeHome(WorldClockTabControl.this);
            mCachedCityTimeCities = PreferencesUtil.getCityTimeCities(WorldClockTabControl.this);
            mIsWorldClockCachedDataReady = true;
        }
    }

    public CityTime getCachedCityTimeCurrent() {
        return mCachedCityTimeCurrent;
    }

    public CityTime getCachedCityTimeHome() {
        return mCachedCityTimeHome;
    }

    public CityTime[] getCachedCityTimeCities() {
        return mCachedCityTimeCities;
    }

    public boolean isWorldClockCachedDataReady() {
        if ((PreferencesUtil.getSyncWorldClockDB(WorldClockTabControl.this) == true) && (mIsWorldClockCachedDataReady == true)) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void getAlarmClockCachedData() {
        ArrayList<AlarmItem> list = AlarmUtils.getAlarmListData(this);
        if (list != null) {
            mAlarmClockList = (ArrayList<AlarmItem>) list.clone();
            PreferencesUtil.setSyncAlarmClockDB(this, true);
        }
    }

    public ArrayList<AlarmItem> getCachedAlarmClockList() {
        return mAlarmClockList;
    }

    public boolean isAlarmClockCachedDataReady() {
        return PreferencesUtil.getSyncAlarmClockDB(WorldClockTabControl.this);
    }

    private void getSetAlarmCachedData() {
        String alarmSoundTitle = "";
        Ringtone ringtone = RingtoneManager.getRingtone(this, AlertUtils.getAlarmDefaultAlertUri(this));
        if (ringtone != null) {
            ringtone.setStreamType(RingtoneManager.TYPE_ALARM);
            alarmSoundTitle = ringtone.getTitle(this);
        } else {
            Log.w(TAG, "getSetAlarmCachedData: alarmSoundTitle = null");
        }
        if (DEBUG_FLAG) Log.d(TAG, "getSetAlarmCachedData: alarmSoundTitle = " + alarmSoundTitle);
        PreferencesUtil.setAlarmSoundCachedTitle(this, alarmSoundTitle);
    }

    public static void launchShowme(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(new ComponentName("com.htc.guide", "com.htc.showme.ui.Search"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "clock");

        try {
            activity.startActivity(intent);
        } catch(Exception e) {
            Log.w(TAG, "Exception e = " + e.toString());
        }
    }

    public static boolean isShowMeInstall(Context context){
        PackageManager pm = context.getPackageManager();
        boolean result = false;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(new ComponentName("com.htc.guide", "com.htc.showme.ui.Search"));
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
        if (list.size() > 0) {
             result = true;
        } else {
            Log.w(TAG, "Tips & Help application does not exist.");
        }
        if (DEBUG_FLAG) Log.d(TAG, "isShowMeInstall: result = " + result);
        return result;
    }

    public boolean isNavigationBarExist() {
    	return mIsNavigationBarExist;
    }
    
    public boolean isWVGA() {
    	Display display = getWindowManager().getDefaultDisplay();
    	Point size = new Point();
    	display.getSize(size);
    	int width = size.x;
    	int height = size.y;
    
        if(width == 480 && height == 782) {
        	return true;
        } else if(width == 790 && height == 480) {
        	return true;
        }
        return false;
    }

    private void schedulerJob(Context context, Class jscheduleService, int jobid) {
        if (DEBUG_FLAG) Log.d(TAG, "SchedulerJob");
        if (context != null) {
            mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder  builder = new JobInfo.Builder(jobid, new ComponentName(context, jscheduleService));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            JobInfo jobInfo = builder.build();
            int joinResult = mJobScheduler.schedule(jobInfo);
            if (DEBUG_FLAG) Log.d(TAG, "SchedulerJob is success or fail " + joinResult);
        }
    }

    private void cancelAllJobs() {
        if (mJobScheduler != null) {
            if (DEBUG_FLAG) Log.d(TAG, "cancel all job");
            mJobScheduler.cancelAll();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        HtcSkinUtils.initHtcFontScale(this);
        mWorldClockTabControlResUtils.switchTheme(newConfig);
        if (Global.isSupportAccChinaSense()) {
            HtcFooter htcFooter = getResUtilsInstance().getCarouselFooter();
            if (htcFooter != null) {
                if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    htcFooter.enableThumbMode(true);
                } else {
                    htcFooter.enableThumbMode(false);
                }
            }
        }
    }
}
