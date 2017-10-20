package com.htc.android.worldclock.worldclock;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.Locale;

import com.htc.android.worldclock.TimeZonePicker;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherUtility;

public class WorldClockService extends IntentService {
    private static final String TAG = "WorldClock.WorldClockService";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String ACTION_LOCALE = "android.intent.action.LOCALE_CHANGED";
    private static final String ACTION_CITY_RESTORED = "com.htc.provider.notify.city_restored";
    public static final String ADD_LOCATION_CHANGED = "com.htc.Weather.intent.action.ADD_LOCATION";
    private static final String DELETE_LOCATION_CHANGED = "com.htc.Weather.delete_location_changed";
    private static final String REARRANGE_LIST_CHANGED = "com.htc.Weather.rearrange_list_changed";
    private static final String ACTION_CL_UPDATED = "com.htc.htclocationservice.currentlocation.updated";
    private static final String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private String mAction;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public WorldClockService() {
        super("WorldClockService");
        setIntentRedelivery(true);
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "onHandleIntent: get intent data fail");
            return;
        }
        mAction = intent.getAction();
        if (mAction != null) {
            if (DEBUG_FLAG) Log.d(TAG, "onHandleIntent: action = " + mAction);
        } else {
            if (DEBUG_FLAG) Log.d(TAG, "onHandleIntent: action = null");
            return;
        }
        
        if (DEBUG_FLAG) Log.d(TAG, "mAction = " + mAction);
        if (mAction.equals(ACTION_LOCALE) || mAction.equals(ACTION_CITY_RESTORED) || mAction.equals(ADD_LOCATION_CHANGED) || mAction.equals(REARRANGE_LIST_CHANGED) ||
            mAction.equals(DELETE_LOCATION_CHANGED)) {
            PreferencesUtil.setSyncWorldClockDB(getBaseContext(), false);
            if (mAction.equals(ACTION_LOCALE)) {
                setHomeByLocaleChanged();
            }
        } else if (mAction.equals(ACTION_CL_UPDATED)) {
            // check home first
            WeatherLocation[] homeLoc = WeatherUtility.loadLocations(getContentResolver(), WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY_HOME);
            if ((homeLoc != null) && (homeLoc.length == 0)) {
                WeatherLocation[] currentLoc = WeatherUtility.loadLocations(getContentResolver(), WorldClock.APP_LOCATION_SERVICE);
                if ((currentLoc != null) && (currentLoc.length != 0)) {
                    Log.i(TAG, "First current location updated, set current to home");
                    if (Global.SECURITY_FLAG) Log.d(TAG, "set current to home, currentLoc[0].getName() = " + currentLoc[0].getName());
                    setHomeToDB(this, currentLoc);
                }
            } else {
                Log.i(TAG, "Current location updated, homeLoc is not empty");
            }
        } else if (mAction.equals(CONNECTIVITY_CHANGE)) {
            try {
                boolean isNewHome = PreferencesUtil.getNewHomeFromGeoCoder(this);
                if (DEBUG_FLAG) Log.d (TAG, "CONNECTIVITY_CHANGE: preference isNewHome = " + isNewHome);
                if (!isNewHome) {
                    ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    boolean isConnected = (activeNetwork != null) && (activeNetwork.isConnectedOrConnecting());
                    if (DEBUG_FLAG) Log.d (TAG, "CONNECTIVITY_CHANGE: isConnected = " + isConnected);
                    if (isConnected) {
                        setHomeByLocaleChanged();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "CONNECTIVITY_CHANGE: e = " + e.toString());
            }
        }
    }
    
    private void setHomeByLocaleChanged() {
        WeatherLocation[] homeLoc = WeatherUtility.loadLocations(getContentResolver(), WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY_HOME);
        if ((homeLoc != null) && (homeLoc.length != 0)) {
            if (Global.SECURITY_FLAG) Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getName() = " + homeLoc[0].getName());
            if (Global.SECURITY_FLAG) Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getCode() = " + homeLoc[0].getCode());
            //if (Global.SECURITY_FLAG) Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getLatitude() = " + homeLoc[0].getLatitude());
            //if (Global.SECURITY_FLAG) Log.d(TAG, "setHomeByLocaleChanged, homeLoc[0].getLongitude() = " + homeLoc[0].getLongitude());
        
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
            List<android.location.Address> list = null;
            list = geoCoder.getFromLocation(lat, lon, 5);
            if (list != null && list.size() > 0) {
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
        int lines = address.getMaxAddressLineIndex();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i <= lines; i++) {
            buffer.append(address.getAddressLine(i)).append(", ");
        }
        String singleLine = buffer.toString();
        if (singleLine.indexOf("Taiwan") >= 0 || singleLine.indexOf("Japan") >= 0) {
            retValue =true;
        }
        return retValue;
    }
    
    public static void setHomeToDB(Context context, WeatherLocation[] w) {
        ContentResolver cr = context.getContentResolver();
        WeatherUtility.saveLocations(cr, WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY_HOME, w);
        PreferencesUtil.setSyncWorldClockDB(context, false);
        Intent intent = new Intent();
        intent.setAction(TimeZonePicker.ACTION_HOME_CHANGED);
        context.sendBroadcast(intent, Global.PERMISSION_APP_HSP);
    }
}
