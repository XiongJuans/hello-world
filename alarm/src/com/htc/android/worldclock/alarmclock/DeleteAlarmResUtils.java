package com.htc.android.worldclock.alarmclock;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.widget.CheckBox;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.ActionBarContainer;
import com.htc.lib1.cc.widget.ActionBarText;
import com.htc.lib1.cc.widget.HtcDeleteButton;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;
import com.htc.lib1.cc.widget.HtcOverlapLayout;

public class DeleteAlarmResUtils extends ResUtils {
    private ActionBarExt mActionBarExt = null;
    private ActionBarContainer mActionBarContainer = null;
    private ActionBarText mActionBarText = null;

    private HtcFooterButton mDeleteButton;
    private String mDeleteString;
    private HtcOverlapLayout mDeleteAlarmView;

    public DeleteAlarmResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {
        initActionBar();
        setBackgroundTheme(mActivity, mActionBarExt);
        mDeleteAlarmView = (HtcOverlapLayout) findViewById(R.id.base_layout);
        ResUtils.enableStatusBarTheme(mActivity);
        
        ((HtcFooter) findViewById(R.id.btn)).ReverseLandScapeSequence(true);
        ((HtcFooterButton) findViewById(R.id.cmd_bar_btn_1)).setText(R.string.done);
        ((HtcFooterButton) findViewById(R.id.cmd_bar_btn_2)).setText(R.string.cancel);

        mDeleteButton = (HtcFooterButton) findViewById(R.id.cmd_bar_btn_1);
        mDeleteString = mResource.getString(R.string.delete);
        mDeleteButton.setText(mDeleteString + " (" + 0 + ")");
        mDeleteButton.setEnabled(false);
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
            mActionBarText.setPrimaryText(R.string.delete_alarm_caption);
            mActionBarContainer.addCenterView(mActionBarText);
        }
    }

    public void setDeleteButtonEnabled(boolean value) {
        if (value) {
            mDeleteButton.setEnabled(true);
        } else {
            mDeleteButton.setEnabled(false);
        }
    }

    public void setDeleteButtonText(int alarmNumber) {
        mDeleteButton.setText(mDeleteString + " (" + alarmNumber + ")");
    }

    public View setDeleteButton(View view, boolean enabled) {
        CheckBox cb = (CheckBox) view.findViewById(R.id.function_select);
        cb.setVisibility(View.GONE);
        HtcDeleteButton db = (HtcDeleteButton) view.findViewById(R.id.function_delete);
        db.setVisibility(View.VISIBLE);
        db.setChecked(enabled);
        return db;
    }

    public void switchTheme(Configuration newConfig) {
        HtcCommonUtil.updateCommonResConfiguration(mActivity);
        switchStatusBarActionBarBkg(newConfig.orientation, mActionBarExt);
    }
}
