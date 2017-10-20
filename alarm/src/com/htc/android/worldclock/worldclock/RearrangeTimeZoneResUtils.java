package com.htc.android.worldclock.worldclock;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.ActionBarContainer;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.ActionBarText;
import com.htc.lib1.cc.widget.HtcReorderListView;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;

public class RearrangeTimeZoneResUtils extends ResUtils {
    private ActionBarExt mActionBarExt = null;
    private ActionBarContainer mActionBarContainer = null;
    private ActionBarText mActionBarText = null;
    private HtcReorderListView mRearrangeList;

    public RearrangeTimeZoneResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        initActionBar();
        setBackgroundTheme(mActivity, mActionBarExt);
        mRearrangeList = (HtcReorderListView) findViewById(R.id.touch_interceptor);
        ((HtcFooter) findViewById(R.id.btn)).ReverseLandScapeSequence(true);
        ((HtcFooterButton) findViewById(R.id.cmd_bar_btn_1)).setText(R.string.done);
        ((HtcFooterButton) findViewById(R.id.cmd_bar_btn_2)).setText(R.string.cancel);
    }

    public void initActionBar() {
        // create and enable htc style action bar
        if (mActionBarExt == null) {
            mActionBarExt = new ActionBarExt(mActivity, mActivity.getActionBar());
        }

        // runtime create and generate module container
        mActionBarContainer = mActionBarExt.getCustomContainer();

        if (mActionBarText == null) {
            mActionBarText = new ActionBarText(mActivity);
            mActionBarText.setPrimaryText(R.string.rearrange_city_caption);
            mActionBarContainer.addCenterView(mActionBarText);
        }
    }
    
    public void switchTheme(Configuration newConfig) {
        HtcCommonUtil.updateCommonResConfiguration(mActivity);
        switchStatusBarActionBarBkg(newConfig.orientation, mActionBarExt);
    }
}
