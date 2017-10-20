package com.htc.android.worldclock.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib2.configuration.HtcWrapConfiguration;

public class HtcSkinUtils {
	public static boolean initHtcFontScale(Context context) {
    	return HtcWrapConfiguration.applyHtcFontscale(context);
    }
	
	public static int getHtcThemeID(Context context) {
		int theme = HtcCommonUtil.getHtcThemeId(context, HtcCommonUtil.CATEGORYTWO);
		return theme;
    }
	
	public static void checkSkinChange(final Activity activity, boolean appliedHtcFontscale, int themeID) {
		
		boolean skinChange = false;
		
		skinChange =checkHtcFontscaleChanged(activity, appliedHtcFontscale) || checkHtcThemeChanged(activity, themeID);
		
		if(skinChange) {
			activity.getWindow().getDecorView().postOnAnimation(new Runnable() {
				@Override
				public void run() {
					activity.recreate();
				}
			});
		}
		
		
	}
    
    private static boolean checkHtcFontscaleChanged(final Activity activity,boolean appliedHtcFontscale) {
    	return HtcWrapConfiguration.checkHtcFontscaleChanged(activity,appliedHtcFontscale);
    }
    
    private static boolean checkHtcThemeChanged(final Activity activity,int themeID) {
		return getHtcThemeID(activity) != themeID;
    }
}
