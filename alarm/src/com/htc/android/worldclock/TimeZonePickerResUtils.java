package com.htc.android.worldclock;

import android.app.Activity;
import android.content.res.Configuration;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.ActionBarContainer;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.ActionBarSearch;
import com.htc.lib1.cc.widget.HtcListView;

public class TimeZonePickerResUtils extends ResUtils {
    private ActionBarExt mActionBarExt = null;
    private ActionBarContainer mActionBarContainer = null;
    private ActionBarSearch mActionBarSearch = null;
    private HtcListView mList;

    private static BackgroundColorSpan BACKGROUND_SPAN;
    private static int BACKGROUND_SPAN_COLOR;
    private static ForegroundColorSpan FOREGROUND_SPAN;
    private static int FOREGROUND_SPAN_COLOR;

    public TimeZonePickerResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        initSearchActionBar();
        ResUtils.enableStatusBarTheme(mActivity);
        setBackgroundTheme(mActivity, mActionBarExt);

        mList = (HtcListView)findViewById(R.id.list);
        BACKGROUND_SPAN_COLOR = ResUtils.getTextSelectionColor(mActivity);
        BACKGROUND_SPAN = new BackgroundColorSpan(BACKGROUND_SPAN_COLOR);
        FOREGROUND_SPAN_COLOR = mResource.getColor(R.color.black);
        FOREGROUND_SPAN = new ForegroundColorSpan(FOREGROUND_SPAN_COLOR);
    }

    public void initSearchActionBar() {
        // create and enable htc style action bar
        if (mActionBarExt == null) {
            mActionBarExt = new ActionBarExt(mActivity, mActivity.getActionBar());
        }

        // runtime create and setup the search container
        // more better to create when user enter search mode
        if (mActionBarContainer == null) {
            // create the search container (be carefully only need one time)
            mActionBarContainer = mActionBarExt.getSearchContainer();
        }

        if (mActionBarSearch == null) {
            mActionBarSearch = new ActionBarSearch(mActivity);
            mActionBarSearch.requestFocus();
        }

        // setup the search module on the search container
        // search container can be used for different visual
        mActionBarContainer = mActionBarExt.getCustomContainer();
        mActionBarContainer.addCenterView(mActionBarSearch);

        mActionBarContainer.setBackUpEnabled(true);
        mActionBarContainer.setBackUpOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mActionBarExt!=null)
                    mActivity.setResult(Activity.RESULT_CANCELED);
                    mActivity.finish();
            }
        });
    }

    public ActionBarSearch getActionBarSearchInstance() {
        return mActionBarSearch;
    }

    public ForegroundColorSpan getForegroundColorSpan() {
        return FOREGROUND_SPAN;
    }

    public BackgroundColorSpan getBackgroundColorSpan() {
        return BACKGROUND_SPAN;
    }
    
    public void switchTheme(Configuration newConfig) {
        HtcCommonUtil.updateCommonResConfiguration(mActivity);
        switchStatusBarActionBarBkg(newConfig.orientation, mActionBarExt);
    }
}
