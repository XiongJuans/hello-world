package com.htc.android.worldclock.utils;

import com.htc.android.worldclock.R;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.ActionBarContainer;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.ActionBarText;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;

public class SettingsResUtils extends ResUtils{

    private ActionBarExt mActionBarExt = null;
    private ActionBarContainer mActionBarContainer = null;
    private ActionBarText mActionBarText = null;

    public SettingsResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        initActionBar();
        setBackgroundTheme(mActivity, mActionBarExt);
    }

    public void initActionBar() {
        if (mActionBarExt == null) {
            mActionBarExt = new ActionBarExt(mActivity, mActivity.getActionBar());
        }

        // runtime create and generate module container
        mActionBarContainer = mActionBarExt.getCustomContainer();

        if (mActionBarText == null) {
            mActionBarText = new ActionBarText(mActivity);
            mActionBarText.setPrimaryText(R.string.settings);
            mActionBarContainer.addCenterView(mActionBarText);
            mActionBarContainer.setBackUpEnabled(true);
            mActionBarContainer.setBackUpOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mActionBarExt!=null)
                        mActivity.finish();
                }
            });
        }
    }
    
    public void switchTheme(Configuration newConfig) {
        HtcCommonUtil.updateCommonResConfiguration(mActivity);
        switchStatusBarActionBarBkg(newConfig.orientation, mActionBarExt);
    }
}
