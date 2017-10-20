package com.htc.android.worldclock.utils;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.worldclock.WorldClock;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.weather.WeatherConsts;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherUtility;

public class SettingsReader {
    private static final String TAG = "WorldClock.SettingsReader";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String URI_STRING = "content://customization_settings/SettingTable/";
    // for Calendar
    private static final String CATEGORY_MODULE = "application_Calendar";
    private static final String FUNCTION = "view";
    private static final String SET = "plenty_set1";
    private static final String ITEM_NAME = "start_weekday";

    // for worldclock
    private static final String WORLDCLOCK_CATEGORY_MODULE = "application_worldclock";
    private static final String WORLDCLOCK_FUNCTION = "settings";
    private static final String WORLDCLOCK_ITEM_NAME_TAB_SEQUENCE = "tab_sequence";
    private static final String WORLDCLOCK_ITEM_NAME_TAB_DEFAULT = "default_tab";

    //for weather provider
    private static final String WEATHER_PROVIDER_CATEGORY_MODULE = "application_weather_provider";

    private ContentResolver mContentResolver = null;
    private String mStartWeekDay = "1"; // 1 -- Sunday; 2 -- Monday

    public static final int TAB_DEFAULT = 1;
    public static final String TAB_SEQUENCE = "1234";
    private int mTabDefault = TAB_DEFAULT;
    private String mTabSequence = TAB_SEQUENCE;

    public SettingsReader(ContentResolver cr) {
        if (DEBUG_FLAG) Log.d(TAG, "SettingsReader");
        mContentResolver = cr;
        fromContent();
    }

    public String getStartWeekDay() {
        return mStartWeekDay;
    }

    public String getTabSequence() {
        return mTabSequence;
    }

    public int getTabDefault() {
        return mTabDefault;
    }

    private void fromContent() {
        try {
            queryCalendarUrl();
            queryTabSettingUrl();
            queryCity();
            checkFormat();
        } catch (Exception e) {
            Log.w(TAG, "fromContent: Query database fail e = " + e.toString());
        }
    }

    private void checkFormat() {
        //for TAB setting
        int tabCount = CarouselTab.getCarouselTabCount();
        if (TAB_SEQUENCE.indexOf(Integer.toString(mTabDefault)) == -1) {
            Log.w(TAG, "checkFormat: format error mTabDefault = " + mTabDefault);
            mTabDefault = TAB_DEFAULT;
        }

        if (mTabSequence.length() != tabCount) {
            Log.w(TAG, "checkFormat: length format error mTabSequence = " + mTabSequence);
            mTabSequence = TAB_SEQUENCE;
        } else {
            String matchString = mTabSequence;
            for (int i = 0; i < tabCount; i++) {
                matchString = matchString.replaceFirst(TAB_SEQUENCE.substring(i, i + 1), "");
            }
            if (matchString.length() != 0) {
                Log.w(TAG, "checkFormat: matchString format error mTabSequence = " + mTabSequence + " matchString = " + matchString);
                mTabSequence = TAB_SEQUENCE;
            }
        }
    }

    private void queryCalendarUrl() {
        String key, value;
        Uri uri = Uri.parse(URI_STRING + CATEGORY_MODULE);
        ContentResolver cr = mContentResolver;
        ContentProviderClient cpc = cr.acquireContentProviderClient(uri);

        if (DEBUG_FLAG) Log.d(TAG, "queryCalendarUrl: uri = " + uri);
        Cursor c = null;
        try {
            c = cpc.query(uri, null, null, null, null);
        } catch (Exception e) {
            Log.w(TAG, "queryCalendarUrl: e =" + e.toString());
        }
        if (c == null) {
            return;
        }

        try {
            if (0 == c.getCount()) {
                Log.w(TAG, "queryCalendarUrl: cursor size is 0");
                return;
            }

            final int idValue = c.getColumnIndex("value");
            if (-1 == idValue) {
                Log.w(TAG, "queryCalendarUrl: no customized data support");
                return;
            }
            c.moveToFirst();
            byte[] buffer = c.getBlob(idValue);
            Bundle bundle = byteArray2Bundle(buffer);
            if (bundle != null) {
                key = FUNCTION;
                Bundle configBundle = bundle.getBundle(key);
                if (configBundle != null) {
                    key = SET;
                    configBundle = configBundle.getBundle(key);
                    if (configBundle != null) {
                        // no child bundle if the set is single
                        key = ITEM_NAME;
                        value = configBundle.getString(key);
                        if (DEBUG_FLAG) Log.d(TAG, "queryCalendarUrl: start weekday item value = " + value);
                        if (value != null) {
                            mStartWeekDay = value;
                        }
                    }
                }
            }
            if (DEBUG_FLAG) Log.d(TAG, "queryCalendarUrl: query done, mStartWeekDay = " + mStartWeekDay);
        } catch (Exception e) {
            Log.w(TAG, "queryCalendarUrl: fail e = " + e.toString());
        } finally {
            if (c != null) {
                if (!c.isClosed()) {
                    c.close();
                }
                c = null;
            }
            if (cpc != null) {
                cpc.release();
                cpc = null;
            }
        }
    }

    private void queryTabSettingUrl() {
        String key;
        Uri uri = Uri.parse(URI_STRING + WORLDCLOCK_CATEGORY_MODULE);
        ContentResolver cr = mContentResolver;
        ContentProviderClient cpc = cr.acquireUnstableContentProviderClient(uri);
        if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: SIE customization uri = " + uri);
        Cursor c = null;
        try {
            c = cpc.query(uri, null, null, null, null);
        } catch (Exception e) {
            Log.w(TAG, "queryTabSettingUrl: e =" + e.toString());
        }
        if (c == null) {
            return;
        }

        if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: c.getCount() = " + c.getCount());

        try {
            if (0 == c.getCount()) {
                Log.w(TAG, "queryTabSettingUrl: cursor size is 0");
                return;
            }

            final int idValue = c.getColumnIndex("value");
            if (-1 == idValue) {
                Log.w(TAG, "queryTabSettingUrl: no customized data support");
                return;
            }
            c.moveToFirst();
            byte[] buffer = c.getBlob(idValue);
            Bundle bundle = byteArray2Bundle(buffer);
            if (bundle != null) {
                key = WORLDCLOCK_FUNCTION;
                Bundle configBundle = bundle.getBundle(key);
                if (configBundle != null) {
                    // due to use set name = single, so no need to parse it
                    // no child bundle if the set is single
                    String defaultTabValue = configBundle.getString(WORLDCLOCK_ITEM_NAME_TAB_DEFAULT);
                    if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: default tab item value = " + defaultTabValue);

                    if (defaultTabValue != null) {
                        mTabDefault = Integer.parseInt(defaultTabValue);
                    }

                    String sequenceTabValue = configBundle.getString(WORLDCLOCK_ITEM_NAME_TAB_SEQUENCE);
                    if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: default tab item value = " + sequenceTabValue);

                    if (sequenceTabValue != null) {
                        mTabSequence = sequenceTabValue;
                    }
                }
            }
            if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: query done, mTabDefault = " + mTabDefault);
            if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: query done, mTabSequence = " + mTabSequence);
        } catch (Exception e) {
            Log.w(TAG, "queryTabSettingUrl: fail e = " + e.toString());
        } finally {
            if (c != null) {
                if (!c.isClosed()) {
                    c.close();
                }
                c = null;
            }
            if (cpc != null) {
                cpc.release();
                cpc = null;
            }
            if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: SIE customize mTabDefault = " + mTabDefault);
            if (DEBUG_FLAG) Log.d(TAG, "queryTabSettingUrl: SIE customize mTabSequence = " + mTabSequence);
        }
    }

    private void queryCity() {
        WeatherLocation[] w = WeatherUtility.loadLocations(mContentResolver, WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY);
        if ((w != null) && (w.length > 0)) {
            return;
        }

        //weather provider was killed by low memory during OOBE
        if (DEBUG_FLAG) Log.d(TAG, "queryCity: reload city data from SIE");

        try {
            byte[] customizeData = null;
            Cursor cursor = null;
            Uri uri = Uri.parse(URI_STRING + WEATHER_PROVIDER_CATEGORY_MODULE);
            if (DEBUG_FLAG) Log.d(TAG, "queryCity: SIE customization uri = " + uri);
            ContentResolver cr = mContentResolver;
            ContentProviderClient cpc = cr.acquireUnstableContentProviderClient(uri);
            try {
                cursor = cpc.query(uri, null, null, null, null);

                if ((cursor != null) && cursor.moveToFirst()) {
                    customizeData = cursor.getBlob(cursor.getColumnIndexOrThrow("value"));
                }
            } catch (Exception e) {
                Log.w(TAG, "queryCity: query failed e = " + e.toString());
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                if (cpc != null) {
                    cpc.release();
                    cpc = null;
                }
            }

            if (customizeData != null) {
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(customizeData, 0, customizeData.length);
                parcel.setDataPosition(0);
                Bundle bundle = new Bundle();
                bundle.readFromParcel(parcel);

                Bundle functionBundle = bundle.getBundle("default_city");
                if (functionBundle != null) {
                    int size = functionBundle.size();
                    for (int i = 0; i < size; i++) {
                        String key = "plenty_set" + (i + 1);
                        Bundle defaultCity = functionBundle.getBundle(key);
                        if (defaultCity != null) {
                            String app = getStringValue(defaultCity, "app", null);

                            if ((app != null) && (app.compareTo(WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY) == 0)) {
                                WeatherLocation location = new WeatherLocation();
                                location.setApp(app);
                                location.setCustomLocation(Integer.parseInt(getStringValue(defaultCity, "type", "" + WeatherConsts.LOCATION_TYPE_CODE)) == WeatherConsts.LOCATION_TYPE_CUSTOM);
                                location.setCode(getStringValue(defaultCity, "code", ""));
                                location.setName(getStringValue(defaultCity, "name", ""));
                                location.setState(getStringValue(defaultCity, "state", ""));
                                location.setCountry(getStringValue(defaultCity, "country", ""));
                                location.setLatitude(getStringValue(defaultCity, "latitude", ""));
                                location.setLongitude(getStringValue(defaultCity, "longitude", ""));
                                location.setTimezone(getStringValue(defaultCity, "timezone", ""));
                                location.setTimezoneId(getStringValue(defaultCity, "timezoneid", ""));

                                WeatherUtility.addLocation(cr, app, new WeatherLocation[] { location });
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "queryCity: add city failed e = " + e.toString());
        }
    }

    private String getStringValue(Bundle bundle, String key, String defaultValue) {
        String value = bundle.getString(key);
        return value == null ? defaultValue : value;
    }

    public static Bundle byteArray2Bundle(byte[] data) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(data, 0, data.length);
        parcel.setDataPosition(0); // support from donut
        Bundle bundle = new Bundle();

        try {
            bundle.readFromParcel(parcel);
        } catch (Exception e) {
            Log.w(TAG, "Exception e = " + e.toString());
        }
        return bundle;
    }
}
