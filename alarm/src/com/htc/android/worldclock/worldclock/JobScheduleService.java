package com.htc.android.worldclock.worldclock;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.htc.android.worldclock.TimeZonePicker;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherUtility;

import java.util.List;
import java.util.Locale;


public class JobScheduleService extends JobService {
    private static final String TAG = "JobScheduleService";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final int MAX_RESULT = 5;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG_FLAG) Log.d(TAG, "startService");
        if (intent != null) {
            Messenger callback = intent.getParcelableExtra("messenger");
            Message message = Message.obtain();
            message.what = WorldClockTabControl.MSG_SERVICE_JOB;
            try {
                //send message to tell activity start job
                callback.send(message);
            } catch (RemoteException e) {
                    Log.e(TAG, "Error passing service object back to activity." + e.toString());
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if (DEBUG_FLAG) Log.d(TAG, "onStartJob");
        // if meet the conditions ,return true , programmer control the job finish time
        // else when return  false tell system the job is finished.
        if (!PreferencesUtil.getNewHomeFromGeoCoder(JobScheduleService.this)) {
            new SetLocaleChangedTask(this).execute(params);
            return true;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (DEBUG_FLAG) Log.d(TAG, "onStopJob");
        return false;
    }

    private class SetLocaleChangedTask extends AsyncTask<JobParameters, Void, String> {

        private JobParameters mParameters;
        private final Context mContext;
        public SetLocaleChangedTask(Context context) {
            mContext = context;
        }

        //time consuming in doInBackgroud method.
        @Override
        protected String doInBackground(JobParameters... jobParameterses) {
            mParameters = jobParameterses[0];
            try {
                boolean isNewHome = PreferencesUtil.getNewHomeFromGeoCoder(JobScheduleService.this);
                if (DEBUG_FLAG) Log.d(TAG, " preference isNewHome = " + isNewHome);
                if (!isNewHome && mContext != null) {
                    ConnectivityManager connectManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = connectManager.getActiveNetworkInfo();
                    boolean isConnected = (activeNetwork != null) && (activeNetwork.isConnected());
                    if (DEBUG_FLAG) Log.d(TAG, "netWork: isConnected = " + isConnected);
                    if (isConnected) {
                        setHomeByLocaleChanged();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "e = " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            jobFinished(mParameters, false);
            if (DEBUG_FLAG) Log.d(TAG, "job finished");
        }
    }

    private void setHomeByLocaleChanged() {
        WeatherLocation[] homeLoc = WeatherUtility.loadLocations(getContentResolver(), WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY_HOME);
        if ((homeLoc != null) && (homeLoc.length != 0)) {
            if (Global.SECURITY_FLAG) {
                Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getName() = " + homeLoc[0].getName());
                Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getCode() = " + homeLoc[0].getCode());
                //Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getLatitude() = " + homeLoc[0].getLatitude());
                //Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getLongitude() = " + homeLoc[0].getLongitude());
            }
            if (homeLoc[0].getCode().isEmpty()) {
                try {
                    PreferencesUtil.setNewHomeFromGeoCoder(this, false);
                    Double lat = Double.parseDouble(homeLoc[0].getLatitude());
                    Double lon = Double.parseDouble(homeLoc[0].getLongitude());
                    // get real city information by geo coder using US language
                    Address enAddr = getAddress(false, lat, lon);
                    boolean isUseAdmin = isCityNameUseAdminArea(enAddr);
                    // get real city information by geo coder using local language
                    Address rgAddress = getAddress(true, lat, lon);
                    if (isUseAdmin) {
                        homeLoc[0].setName(rgAddress.getAdminArea());
                    } else {
                        homeLoc[0].setName(rgAddress.getLocality());
                    }
                    if (Global.SECURITY_FLAG) Log.d(TAG, "get home from geo coder, homeLoc[0].getName() = " + homeLoc[0].getName());
                    setHomeToDB(this, homeLoc);
                    PreferencesUtil.setNewHomeFromGeoCoder(this, true);
                } catch (Exception e) {
                    Log.w(TAG, "setHomeByLocaleChanged : e = " + e.toString());
                }
            } else {
                if (DEBUG_FLAG) Log.d(TAG, "setHomeByLocaleChanged, home's code is not empty.");
            }
        }
    }

    private Address getAddress(boolean isLocal, double lat, double lon) {
        Address rgAddress = null;
        try {
            Geocoder geoCoder = null;
            if (isLocal) {
                // new local language geocoder
                geoCoder = new Geocoder(this, Locale.getDefault());
            } else {
                // new english geocoder
                geoCoder = new Geocoder(this, Locale.US);
            }
            List<Address> list = null;
            list = geoCoder.getFromLocation(lat, lon, MAX_RESULT);
            if (list != null && list.isEmpty()) {
                for (android.location.Address addr : list) {
                    if (!TextUtils.isEmpty(addr.getLocality())) {
                        rgAddress = addr;
                        break;
                    }
                }
                if (rgAddress == null) {
                    rgAddress = list.get(0);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getAddress: e = " + e.toString());
        }
        return rgAddress;
    }

    private boolean isCityNameUseAdminArea(Address address) {
        boolean retValue = false;
        if (address != null) {
            int lines = address.getMaxAddressLineIndex();
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i <= lines; i++) {
                buffer.append(address.getAddressLine(i)).append(", ");
            }
            String singleLine = buffer.toString();
            if (singleLine.indexOf("Taiwan") >= 0 || singleLine.indexOf("Japan") >= 0) {
                retValue = true;
            }
        }
        return retValue;
    }

    public static void setHomeToDB(Context context, WeatherLocation[] wLocations) {
        if (context != null) {
            ContentResolver cr = context.getContentResolver();
            WeatherUtility.saveLocations(cr, WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY_HOME, wLocations);
            PreferencesUtil.setSyncWorldClockDB(context, false);
            Intent intent = new Intent();
            intent.setAction(TimeZonePicker.ACTION_HOME_CHANGED);
            context.sendBroadcast(intent, Global.PERMISSION_APP_HSP);
        }
    }

}
