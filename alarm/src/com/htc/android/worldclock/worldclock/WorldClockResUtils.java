package com.htc.android.worldclock.worldclock;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.WorldClockTabControl;
import com.htc.android.worldclock.utils.Global;
import com.htc.android.worldclock.utils.ResUtils;
import com.htc.android.worldclock.worldclock.WorldClock.WorldClockEnum;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.widget.ActionBarContainer;
import com.htc.lib1.cc.widget.ActionBarDropDown;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.HtcFooter;
import com.htc.lib1.cc.widget.HtcFooterButton;

public class WorldClockResUtils extends ResUtils {
    private static final String TAG = "WorldClock.WorldClockResUtils";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private ActionBarExt mActionBarExt = null;
    private ActionBarContainer mActionBarContainer = null;
    private ActionBarDropDown mActionBarDropDown = null;
    private ImageView mCurLocateImg;
    private HtcFooter mHtcFooter;

    private boolean mHasIcon = false;

    public WorldClockResUtils(Activity activity, View view) {
        super(activity, view);
    }

    public void initResources() {

        mActionBarExt = ((WorldClockTabControl) mActivity).getResUtilsInstance().getActionBarExt();
        mActionBarContainer = ((WorldClockTabControl) mActivity).getResUtilsInstance().getActionBarContainer();

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        if (Global.isSupportAccChinaSense()) {
            mHtcFooter = ((WorldClockTabControl) mActivity).getResUtilsInstance().getCarouselFooter();
            mHtcFooter.setDividerEnabled(false);
            setHtcFooterButtonResource();
        }
    }

    public void setHtcFooterButtonResource() {
        HtcFooterButton addButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn3);
        addButton.setEnabled(true);
        addButton.setImageResource(R.drawable.icon_btn_add_light);
        addButton.setText(R.string.va_add);
        HtcFooterButton deleteButton = (HtcFooterButton) mHtcFooter.findViewById(R.id.footer_btn2);
        deleteButton.setEnabled(true);
        deleteButton.setImageResource(R.drawable.icon_btn_edit_light);
        deleteButton.setText(R.string.edit);
    }
    
    public void initActionBarDropDown() {
        mActionBarDropDown = ((WorldClockTabControl) mActivity).getResUtilsInstance().getActionBarDropDown();
        mCurLocateImg = new ImageView(mActivity);
        mCurLocateImg.setId(android.R.id.icon);
        RelativeLayout.LayoutParams currentParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        currentParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        currentParams.setMargins(0, 0, mActivity.getResources().getDimensionPixelSize(R.dimen.margin_m), 0);
        mCurLocateImg.setLayoutParams(currentParams);
        mActionBarDropDown.setLeft(mActivity.getResources().getDimensionPixelSize(R.dimen.margin_m));
        mActionBarDropDown.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View arg0, int arg1, int arg2, int arg3,
					int arg4, int arg5, int arg6, int arg7, int arg8) {
				mActionBarDropDown.setLeft(mActivity.getResources().getDimensionPixelSize(R.dimen.margin_m));
			}
		})	;
    }

    public void setActionDropDownPrimaryText(String cityName, WorldClockEnum state) {
        if(mActionBarDropDown != null) {
            mActionBarDropDown.setPrimaryText(R.string.htc_private_app_clock);
            mActionBarDropDown.setArrowEnabled(false);
            removeIcon();
        }
    }
    public void removeIcon(){
        if(mHasIcon) {
            mActionBarDropDown.removeViewAt(0);
            mHasIcon = false;
        }
    }

    public void addLocIcon() {
        removeIcon();
        mActionBarDropDown.addView(mCurLocateImg,0);
        mHasIcon = true;
    }

    public ActionBarDropDown getActionBarDropDown() {
        return mActionBarDropDown;
    }

    public ActionBarExt getActionBarExt() {
        return mActionBarExt;
    }

    public ActionBarContainer getActionBarContainer() {
        return mActionBarContainer;
    }

	public void setActionBarDropDownClickAble(boolean clickable) {
		if(mActionBarDropDown != null) {
			mActionBarDropDown.setClickable(clickable);
		}
	}
}
