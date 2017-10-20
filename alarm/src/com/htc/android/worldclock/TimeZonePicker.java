/*
 * design by sky and tiffanie
 * Activity to pick timezone for any location
 */

package com.htc.android.worldclock;

import java.util.Locale;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.AutoCompleteTextView;
import android.widget.ResourceCursorAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.HtcSkinUtils;
import com.htc.android.worldclock.utils.HtcStorageChecker;
import com.htc.android.worldclock.utils.PreferencesUtil;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.android.worldclock.utils.ToastMaster;
import com.htc.android.worldclock.worldclock.WorldClock;
import com.htc.android.worldclock.worldclock.WorldClockService;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib2.weather.WeatherConsts;
import com.htc.lib2.weather.WeatherLocation;
import com.htc.lib2.weather.WeatherUtility;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.HtcListItem2LineText;
import com.htc.lib1.cc.widget.HtcListView;
import com.htc.lib1.theme.ThemeType;

@SuppressWarnings("deprecation")
public class TimeZonePicker extends Activity {
    private static final String TAG = "WorldClock.TimeZonePicker";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;

    public static final String ACTION_HOME_CHANGED = "com.htc.intent.action.HOME_CHANGED";
    private static final String ACTION_INTENT_ADD_LOCATION = "com.htc.Weather.intent.action.ADD_LOCATION";

    public static final int CITY_NOT_FOUND = 0;
    private static final int SEARCH_FOR_ADD = 1;
    private static final int SEARCH_FOR_HOME_SETTINGS = 2;
    private static final int SEARCH_FOR_TIMEZONE = 3;
    private final int WHAT_ON_INIT = 700;
    private final int WHAT_ON_ADD_CITY = 701;
    private final int WHAT_ON_SET_HOME = 702;

    private boolean mAdded = false;

    private TextView mCityNotFoundAlert;
    private AutoCompleteTextView mEditor;

    private InputMethodManager mInputMethodManager;

    private HtcListView mTimeZoneList;

    private Cursor mCursor;

    protected HandlerThread mHandlerThread = null;
    protected Looper mLooper = null;
    private Handler mHandler = null;

    private boolean mInit = false;
    private boolean mDestroyed = false;
    // Htc font scale
    private boolean mHtcFontscale = false;

    // new
    public CityListAdapter mCursorAdapter;
    public TextWatcher mInputTextWatcher;
    public SimpleSearchModule mSearchModule;
    public boolean mDisableSearch = false;
    public boolean mCreated = false; // isCreated
    private int mIndexMode = WeatherConsts.LOCATION_LIST_COLUMN_NAME.name.ordinal();

    private final static int PRIMARY_TEXT_ITEM = 0;
    private final static int SECONDARY_TEXT_ITEM = 1;

    private TimeZonePickerResUtils mTimeZonePickerResUtils;

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
    
    protected static enum TimeZonePickerEnum {
        INIT ,PAUSE , END
    }

    View.OnFocusChangeListener mOnFocusChangeHandler =
        new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (mInputMethodManager == null) {
                    return;
                }
                if (!hasFocus) {
                    mInputMethodManager.hideSoftInputFromWindow(mEditor.getWindowToken(), 0);
                    // HtcLog.d("not has focus, hide");
                } else {
                    mInputMethodManager.showSoftInput(mEditor, 0);
                    // HtcLog.d("has focus, show");
                }
            }
        };

    @Override
    public void onCreate(Bundle icicle) {
        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](303) [LAUNCH_TIME][WorldClock][TimeZonePicker][START]");
        mHtcFontscale = HtcSkinUtils.initHtcFontScale(this);
        HtcCommonUtil.initTheme(this, HtcCommonUtil.CATEGORYTWO);
        // For Theme Change
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.registerThemeChangeObserver(this, ThemeType.HTC_THEME_CC, mThemeChangeObserver);
        super.onCreate(icicle);
        setContentView(R.layout.main_timezone_picker);

        mTimeZonePickerResUtils = new TimeZonePickerResUtils(this, null);
        mTimeZonePickerResUtils.initResources();
        mEditor = mTimeZonePickerResUtils.getActionBarSearchInstance().getAutoCompleteTextView();
        mEditor.setHint(getString(R.string.addcity_search));
        mEditor.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        mEditor.setOnFocusChangeListener(mOnFocusChangeHandler);
        setInputWatcher();
        mTimeZoneList = (HtcListView) findViewById(R.id.list);
        mTimeZoneList.setOnItemClickListener(mItemClick);

        // Remove fast scroller in Chinese language.
        Locale systemLocale = this.getResources().getConfiguration().locale;
        String systemLang = systemLocale.getLanguage();

        if ("zh".equals(systemLang)) {
            mTimeZoneList.setFastScrollEnabled(false);
        } else {
            mTimeZoneList.setFastScrollEnabled(true);
        }

        mCityNotFoundAlert = (TextView) findViewById(R.id.message);

        noCityAlert(true, false);
        // initColorSpan();
        initHandler();

        mHandler.sendEmptyMessage(WHAT_ON_INIT);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        if (DEBUG_FLAG) Log.d(TAG, "onNewIntent");

        if (mEditor != null) {
            mEditor.setText("");
        }
    }

    private void initHandler() {

        mHandlerThread = new HandlerThread("TimeZonePicker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mLooper = mHandlerThread.getLooper();
        mHandler = new Handler(mLooper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case WHAT_ON_INIT:
                        onInitParent();
                        if (DEBUG_FLAG || Global.PERFORMANCE_FLAG) Log.v(Global.PERFORMANCE_TAG, "[AutoProf](304) [LAUNCH_TIME][WorldClock][TimeZonePicker][DATA_READY]");
                        break;
                    case WHAT_ON_ADD_CITY:
                        addCityToDB((WeatherLocation[]) msg.obj);
                        break;
                    case WHAT_ON_SET_HOME:
                        WorldClockService.setHomeToDB(TimeZonePicker.this, (WeatherLocation[]) msg.obj);
                        setResult(RESULT_OK);
                        finish();
                        break;
                }
            }
        };
    }

    // non-UI function
    private void onInitParent() {

        if (mDestroyed) {
            return;
        }
        mSearchModule = new SimpleSearchModule();
        mInputMethodManager = (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private AdapterView.OnItemClickListener mItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            if (mAdded) {
                return;
            }
            mAdded = true;
            Intent intent = getIntent();
            int search_intention = intent.getIntExtra(WorldClock.SEARCH_INTENTION, -1);
            Cursor c = (Cursor) mCursorAdapter.getItem(position);

            WeatherLocation[] w = new WeatherLocation[1];
            w[0] = new WeatherLocation();
            w[0] = WeatherUtility.CursorToWeatherLocation(c);
            switch (search_intention) {
                case SEARCH_FOR_ADD:
                    Message msgAdd = Message.obtain();
                    msgAdd.what = WHAT_ON_ADD_CITY;
                    msgAdd.obj = w;
                    mHandler.sendMessage(msgAdd);
                    break;
                case SEARCH_FOR_HOME_SETTINGS:
                    Message msgHome = Message.obtain();
                    msgHome.what = WHAT_ON_SET_HOME;
                    msgHome.obj = w;
                    mHandler.sendMessage(msgHome);
                    break;
                case SEARCH_FOR_TIMEZONE:
                    Intent i = new Intent();
                    i.putExtra("timezoneId", w[0].getTimezoneId());
                    i.putExtra("name", w[0].getName());
                    i.putExtra("cityCode", w[0].getCode());

                    setResult(RESULT_OK, i);
                    finish();
                    break;
                default:
                    setResult(RESULT_CANCELED);
                    finish();
            }
        }
    };

    private void addCityToDB(WeatherLocation[] w) {
        ContentResolver cr = getContentResolver();
        boolean exist = false;
        WeatherLocation[] loc = WeatherUtility.loadLocations(cr, WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY);
        for (WeatherLocation data : loc) {
            if (data.getCode().equals(w[0].getCode())) {
                exist = true;
                break;
            }
        }

        if (!exist) {
            WeatherUtility.addLocation(cr, WorldClock.DB_APP_NAME_WORLD_CLOCK_CITY, w);
            PreferencesUtil.setSyncWorldClockDB(TimeZonePicker.this, false);
            Intent intent = new Intent();
            // get weather accedd code
            intent.putExtra("code", w[0].getCode());
            intent.putExtra("name", w[0].getName());
            intent.putExtra("state", w[0].getState());
            intent.putExtra("country", w[0].getCountry());
            intent.putExtra("timezone", w[0].getTimezone());
            intent.putExtra("timezoneid", w[0].getTimezoneId());
            intent.putExtra("status", exist);
            intent.putExtra("app", "com.htc.elroy.Weather");
            intent.putExtra("main_app_add", true);
            intent.setAction(ACTION_INTENT_ADD_LOCATION);
            this.sendBroadcast(intent, Global.PERMISSION_APP_HSP);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(TimeZonePicker.this,
                        R.string.city_duplicate, Toast.LENGTH_SHORT);
                    ToastMaster.setToast(toast);
                    toast.show();
                }
            });
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        HtcStorageChecker.checkStorageFull(this);
        if (mIsThemeChanged) {
            getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override
            public void run() {
                HtcCommonUtil.notifyChange(TimeZonePicker.this, HtcCommonUtil.TYPE_THEME);
                recreate();
            }});
            mIsThemeChanged = false;
        }
        if (mInit == false) {
            mInit = true;
            return;
        }
    }

    @Override
    protected void onPause() {
        if ((mEditor != null) && (mInputMethodManager != null)) {
            mInputMethodManager.hideSoftInputFromWindow(mEditor.getWindowToken(), 0);
        }
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;

        mHandler.removeMessages(WHAT_ON_INIT);
        if (mLooper != null) {
            mLooper.quit();
        }

        if (mCursor != null) {
            if (!mCursor.isClosed()) {
                mCursor.close();
            }
            mCursor = null;
        }

        if (mSearchModule != null) {
            mSearchModule.destroy();
        }

        if (mCursorAdapter != null) {
            mCursorAdapter = null;
        }
        mTimeZoneList.setAdapter(null);
        mInputMethodManager = null;
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_FULL, mThemeChangeObserver);
        HtcCommonUtil.unregisterThemeChangeObserver(ThemeType.HTC_THEME_CC, mThemeChangeObserver);
    }

    private void noCityAlert(boolean isVisible, boolean isShowText) {
        if (isVisible) {
            this.mTimeZoneList.setVisibility(View.GONE);
            this.mCityNotFoundAlert.setVisibility(View.VISIBLE);
        } else {
            this.mTimeZoneList.setVisibility(View.VISIBLE);
            this.mCityNotFoundAlert.setVisibility(View.GONE);
        }

        if (isShowText) {
            this.mCityNotFoundAlert.setText(R.string.city_not_found);
        } else {
            this.mCityNotFoundAlert.setText(null);
        }
    }

    public class CityListAdapter extends ResourceCursorAdapter implements SectionIndexer{

        private String mAlphabet;
        private AlphabetIndexer mIndexer;
        private Cursor mCursor;

        public CityListAdapter(Context context, int resource, Cursor c) {
            super(context, resource, c);
            mAlphabet = context.getString(R.string.fast_scroll_alphabet);
            if (c != null) {
                mCursor = c;
                mIndexer = new AlphabetIndexer(c, mIndexMode, mAlphabet);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            final View view = super.newView(context, cursor, parent);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {

            HtcListItem2LineText item2LineView = (HtcListItem2LineText) view.findViewById(R.id.text1);

            String pattern = getInputText();
            String text[] = WeatherUtility.generateDisplayText(c);
            spanSearchView(pattern, text[0], item2LineView, TimeZonePicker.PRIMARY_TEXT_ITEM);
            spanSearchView(pattern, text[1], item2LineView, TimeZonePicker.SECONDARY_TEXT_ITEM);
        }

        private void spanSearchView(String pattern, String text, HtcListItem2LineText view, int lineIndex) {

            //Ray:handle duplicate city
            int textLength = text.length();
            int midCharIndex = 0;

            midCharIndex = textLength/2;

            if(text.charAt(midCharIndex) == ' ' && textLength % 2 != 0) {

                String s1 = text.substring(0, midCharIndex);
                String s2 = text.substring(midCharIndex+1 , textLength);
                if(s1.equals(s2)) {
                    text = s1;
                }
            }
            //
            SpannableString displayString = new SpannableString(text);

            if (!TextUtils.isEmpty(text)) {
                if (!TextUtils.isEmpty(pattern)) {
                    int pos0 = text.toLowerCase().indexOf(pattern.toLowerCase());
                    int pos1 = pos0 + pattern.length();
                    if (pos0 >= 0) {
                        displayString.setSpan(mTimeZonePickerResUtils.getForegroundColorSpan(), pos0, pos1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        displayString.setSpan(mTimeZonePickerResUtils.getBackgroundColorSpan(), pos0, pos1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                if (lineIndex == TimeZonePicker.PRIMARY_TEXT_ITEM) {
                    view.setPrimaryText(displayString);
                } else if (lineIndex == TimeZonePicker.SECONDARY_TEXT_ITEM) {
                    view.setSecondaryText(displayString);
                } else {
                    Log.w(TAG, "spanSearchView: error lineIndex");
                }
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            updateIndexer(cursor);
        }

        private void updateIndexer(Cursor cursor) {
            mIndexer = null;
            if (cursor != null) {
                mCursor = cursor;
                mIndexer = new AlphabetIndexer(cursor, mIndexMode, mAlphabet);
                mIndexer.setCursor(cursor);
            }
        }

        @Override
        public boolean isEnabled(int position) {

            // disable items selectable
            if (mAdded) {
                return false;
            }
            return super.isEnabled(position);
        }

        @Override
        public boolean areAllItemsEnabled() {

            if (mAdded) {
                return false;
            }
            return super.areAllItemsEnabled();
        }

        @Override
        public int getPositionForSection(int sectionIndex) {

            if (mIndexer == null) {
                if (mCursor == null) {
                    // No cursor, the section doesn't exist so just return 0
                    return 0;
                }
                mIndexer = new AlphabetIndexer(mCursor, mIndexMode, mAlphabet);
            }

            return mIndexer.getPositionForSection(sectionIndex);
        }

        @Override
        public int getSectionForPosition(int position) {
        	if(mIndexer != null && mCursor != null && mCursor.getCount() > 0 ) {
          	  try {	
  	            return mIndexer.getSectionForPosition(position);
          	  } catch(Exception e) {
          		  Log.w(TAG, "Exception e = " + e.toString());
          		  return 0;
          	  }
          	}
          return 0;
        }

        @Override
        public Object[] getSections() {
            if (mIndexer != null)
                return mIndexer.getSections();
            return null;
        }
    }

    public class EditTextWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {

            // Do nothing !!
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            // Do nothing !!
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if(s.length() == 0) {
                mTimeZonePickerResUtils.getActionBarSearchInstance().setClearIconVisibility(View.INVISIBLE);
            }
            else {
                mTimeZonePickerResUtils.getActionBarSearchInstance().setClearIconVisibility(View.VISIBLE);
            }
            search((s != null) && (s.length() > 0) ? s.toString() : null);
        }
    }

    public void setInputWatcher() {

        mInputTextWatcher = new EditTextWatcher();

        if (mEditor != null) {
            mEditor.addTextChangedListener(mInputTextWatcher);
        }
    }

    private String getInputText() {

        return (mEditor != null) && (mEditor.getText().toString() != null) && (mEditor.getText().length() > 0) ? mEditor.getText().toString() : "";
    }

    protected class SimpleSearchModule {
        HandlerThread mSearchThread;
        Handler mSearchHandler;

        static final int MSG_SEARCH = 1;
        static final int MSG_SEARCH_COMPLETE = 2;
        static final int MSG_DESTROY = 3;
        static final int MSG_ONRESUME = 4;

        public SimpleSearchModule() {

            // Create thread for simple searching
            mSearchThread = new HandlerThread("SimpleSearchThread", Process.THREAD_PRIORITY_BACKGROUND);
            mSearchThread.start();
            mSearchHandler = new SearchHandler(mSearchThread.getLooper());
        }

        public void destroy() {

            mSearchHandler.obtainMessage(MSG_DESTROY).sendToTarget();
        }

        public void doSearch(String pattern) {

            final String searchPattern = (pattern != null) && (pattern.length() > 0) ? pattern : "";
            final String textString = getInputText();
            if (searchPattern.equals(textString)) {
                mSearchHandler.removeMessages(MSG_SEARCH);
                mSearchHandler.obtainMessage(MSG_SEARCH, searchPattern).sendToTarget();
            }
        }

        private class SearchHandler extends Handler {

            public SearchHandler(Looper looper) {
                super(looper);

            }

            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case MSG_SEARCH:
                        final String searchPattern = (msg.obj != null) && (((String) msg.obj).length() > 0) ? (String) msg.obj : "";
                        final String textString = getInputText();
                        mUIHandler.removeMessages(MSG_SEARCH_COMPLETE);

                        if (searchPattern.equals(textString)) {
                            Cursor c = coreSearch(searchPattern);
                            mUIHandler.obtainMessage(MSG_SEARCH_COMPLETE, c).sendToTarget();
                        }
                        break;

                    case MSG_DESTROY: {
                        Looper.myLooper().quit();
                    }
                        break;
                }
            }
        }

        private Cursor coreSearch(String pattern) {

            return ((pattern == null) || (pattern.length() < 1)) ? null
                : WeatherUtility.searchLocationListInEnglishAndLocaleLanguage(getBaseContext(), pattern, WeatherConsts.LOCATION_LIST_COLUMN_NAME.name.name());
        }
    }

    public void search(String pattern) {

        if (mDisableSearch == false) {
            if (mSearchModule != null) {
                mSearchModule.doSearch(pattern);
            }
        }
    }

    public void handleStuffAfterOnResume() {

        mCreated = true;
        if (!mDisableSearch) {
            search(null);
        }
    }

    private final Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case SimpleSearchModule.MSG_SEARCH_COMPLETE: {
                    Cursor cursor = (Cursor) msg.obj;
                    if (cursor != null) {
                        if (mCursorAdapter == null) {
                            mCursorAdapter = new CityListAdapter(TimeZonePicker.this, R.layout.common_city_list_text, cursor);
                            mTimeZoneList.setAdapter(mCursorAdapter);
                        } else {
                            mCursorAdapter.changeCursor(cursor);
                            mTimeZoneList.setAdapter(mCursorAdapter);
                        }

                        if (mCursor != null) {
                            if (!mCursor.isClosed()) {
                                mCursor.close();
                            }
                            mCursor = null;
                        }

                        mCursor = cursor;

                        int foundNum = mCursor.getCount();
                        if (foundNum == 0) {
                            noCityAlert(true, true);
                        } else {
                            noCityAlert(false, false);
                        }
                    } else { // cursor is null, needs not to search
                        if (mCursorAdapter != null) {
                            mCursorAdapter.changeCursor(null);
                            mTimeZoneList.setAdapter(mCursorAdapter);
                        }
                        noCityAlert(true, false);
                    }
                }
                    break;

                case SimpleSearchModule.MSG_ONRESUME: {
                    handleStuffAfterOnResume();
                }
                    break;

                default: {

                }
                    break;
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        HtcSkinUtils.initHtcFontScale(this);
        mTimeZonePickerResUtils.switchTheme(newConfig);
    }
}
