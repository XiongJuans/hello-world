package com.htc.android.worldclock.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.preference.PreferenceActivity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.htc.android.worldclock.R;
import com.htc.lib1.cc.util.HtcCommonUtil;
import com.htc.lib1.cc.widget.ActionBarExt;
import com.htc.lib1.cc.widget.HtcListItem;
import com.htc.lib1.cc.widget.HtcListItem2LineText;
import com.htc.lib1.cc.widget.ListPopupBubbleWindow;
import com.htc.lib1.cc.widget.PopupBubbleWindow;

@SuppressLint("ResourceAsColor")
public class ResUtils {
    private static final int STATUS_BAR_BACKGROUND_INDEX = 0;
    private static final int APP_BACKGROUND_INDEX = 1;
    protected Activity mActivity;
    protected View mMainView;
    protected Resources mResource;
    
    // Htc Theme
    private static final int STATUS_BAR_BKG_ID = 1; // any value except 0 
    private Drawable mStatusBarColorDrawable;
    private Drawable mActionBarColorDrawable;
    private Drawable mWindowDrawable;
    private Drawable mHeaderDrawable;
    private LayerDrawable mWindowBkg;
    private int mStatusBarHeight;

    public ResUtils(Activity activity, View view) {
        mActivity = activity;
        mMainView = view;
        mResource = mActivity.getResources();
    }
    
    public void setActionBarShow(boolean isShow) {
        ActionBar actionBar = mActivity.getActionBar();
        if(actionBar != null) {
            if(isShow) {
                actionBar.show();
            } else {
                actionBar.hide();
            }
        }
    }

    public void setTextViewText(View view, int viewId, int strId, int visibility) {
        TextView textView = (TextView) view.findViewById(viewId);
        textView.setText(strId);
        textView.setVisibility(visibility);
    }

    public void setButtonText(int viewId, int strId) {
        Button button = (Button) findViewById(viewId);
        button.setText(strId);
    }

    public void setBackgroundColor(int viewId, int colorId) {
        View view = findViewById(viewId);
        view.setBackgroundColor(mResource.getColor(colorId));
    }

    public void setBackgroundResource(View view, String skinDrawableStr, int drawableId) {
        view.setBackgroundResource(drawableId);
    }

    public void setBackgroundResource(int viewId, String skinDrawableStr, int drawableId) {
        View view = findViewById(viewId);
        view.setBackgroundResource(drawableId);
    }

    public void setBackgroundDrawable(View view, int drawableId) {
        Drawable drawable = mResource.getDrawable(drawableId);
        view.setBackgroundDrawable(drawable);
    }

    public void setImageButtonImageResource(int viewId, String skinDrawableStr, int drawableId) {
        ImageButton imageButton = (ImageButton) findViewById(viewId);
        imageButton.setImageResource(drawableId);
    }

    public void setImageViewImageResource(int viewId, String skinDrawableStr, int drawableId) {
        ImageView imageView = (ImageView) findViewById(viewId);
        imageView.setImageResource(drawableId);
    }

    public int getResId(String skinDrawableStr, int drawableId) {
        return drawableId;
    }

    public int getSkinColorResId(String skinResStr, int resId) {
        return resId;
    }

    public void setLayout(int viewId, int dimenWidthId, int dimenHeightId, int dimenMarginLeftId, int dimenMarginTopId) {
        View view = findViewById(viewId);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)view.getLayoutParams();

        if (dimenWidthId != 0) {
            layoutParams.width = mResource.getDimensionPixelSize(dimenWidthId);
        }
        if (dimenHeightId != 0) {
            layoutParams.height = mResource.getDimensionPixelSize(dimenHeightId);
        }
        if (dimenMarginLeftId != 0) {
            layoutParams.leftMargin = mResource.getDimensionPixelSize(dimenMarginLeftId);
        }
        if (dimenMarginTopId != 0) {
            layoutParams.topMargin = mResource.getDimensionPixelSize(dimenMarginTopId);
        }
        view.setLayoutParams(layoutParams);
    }

    public void setLayoutWithRealMeasure(int viewId, int dimenWidthId, int dimenHeightId, int dimenMarginLeftId, int dimenMarginTopId) {
        View view = findViewById(viewId);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)view.getLayoutParams();

        if (dimenWidthId != 0) {
            layoutParams.width = dimenWidthId;
        }
        if (dimenHeightId != 0) {
            layoutParams.height = dimenHeightId;
        }
        if (dimenMarginLeftId != 0) {
            layoutParams.leftMargin = dimenMarginLeftId;
        }
        if (dimenMarginTopId != 0) {
            layoutParams.topMargin = dimenMarginTopId;
        }
        view.setLayoutParams(layoutParams);
    }

    protected View findViewById(int resId) {
        if (mMainView == null) {
            return mActivity.findViewById(resId);
        } else {
            return mMainView.findViewById(resId);
        }
    }
    
    public static boolean hasNavigationBar(Context context) {
        Point size = new Point();
        Point realSize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(size);
        windowManager.getDefaultDisplay().getRealSize(realSize);
        return !size.equals(realSize);
    }
    
    public static int getThemeColor(Context context) {
        int color = HtcCommonUtil.getCommonThemeColor(context, com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color);
        return color;
    }
    
    public static int getTextSelectionColor(Context context) {
        int color = HtcCommonUtil.getCommonThemeColor(context, com.htc.lib1.cc.R.styleable.ThemeColor_text_selection_color);
        return color;
    }
    
    public static int getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static void enableStatusBarTheme(Activity activity) {
        Window window = activity.getWindow();
        Double retValue = Global.getAccBySenseVersion();
        if (retValue >= Global.HTC_SENSE_VERSION_8) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.R.color.transparent);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
    
    public static void enablePreferenceStatusBarTheme(Activity activity) {
        ViewGroup vg = (ViewGroup)activity.findViewById(android.R.id.content);
        
        if(vg != null) {
            vg.getChildAt(0).setFitsSystemWindows(true);
         }

        Window window = activity.getWindow();
        Double retValue = Global.getAccBySenseVersion();
        if (retValue >= Global.HTC_SENSE_VERSION_8) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.R.color.transparent);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

    }

    public static int getListItemHeight(Activity activity){
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        LinearLayout itemView = (LinearLayout) inflater.inflate(R.layout.specific_worldclock_item, null);
        HtcListItem listItem = (HtcListItem) itemView.findViewById(R.id.info_item);
        HtcListItem2LineText text = (HtcListItem2LineText) listItem.findViewById(R.id.zone_information);
        text.setPrimaryText("test");
        text.setSecondaryText("test");
        listItem.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    	
        return listItem.getMeasuredHeight();
    }
    
    // Htc Theme
    protected void setBackgroundTheme(Context context, ActionBarExt actionBarExt) {
        mStatusBarHeight = getStatusBarHeight(mActivity);
        int categoryColor = HtcCommonUtil.getCommonThemeColor(context, com.htc.lib1.cc.R.styleable.ThemeColor_multiply_color);
        mStatusBarColorDrawable = new ColorDrawable(categoryColor);
        mActionBarColorDrawable = new ColorDrawable(categoryColor);
        Drawable[] drawables = {mStatusBarColorDrawable, context.getResources().getDrawable(com.htc.lib1.cc.R.drawable.common_app_bkg)};
        mWindowBkg = new LayerDrawable(drawables);
        mWindowBkg.setLayerInset(1, 0, mStatusBarHeight, 0, 0);
        mActivity.getWindow().setBackgroundDrawable(mWindowBkg);
        mWindowDrawable = HtcCommonUtil.getCommonThemeTexture(context, com.htc.lib1.cc.R.styleable.CommonTexture_android_windowBackground);
        mHeaderDrawable = HtcCommonUtil.getCommonThemeTexture(context, com.htc.lib1.cc.R.styleable.CommonTexture_android_headerBackground);
        mWindowBkg.setId(0, STATUS_BAR_BKG_ID);
        switchStatusBarActionBarBkg(context.getResources().getConfiguration().orientation, actionBarExt);
    }

    protected void switchStatusBarActionBarBkg(int orientation, ActionBarExt actionBarExt) {
        Drawable windowBkgTextDrawable = HtcCommonUtil.getCommonThemeTexture(mActivity, com.htc.lib1.cc.R.styleable.CommonTexture_android_windowBackground);
        if ((windowBkgTextDrawable != null) && (windowBkgTextDrawable instanceof BitmapDrawable)) {
            // Let status bar texture align top
            ((BitmapDrawable)windowBkgTextDrawable).setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL);
        }
        if (mWindowDrawable != null) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE || !isDefaultDensity(mActivity)) {
                // Show the pure color
                mWindowBkg.setDrawableByLayerId(STATUS_BAR_BKG_ID, mStatusBarColorDrawable);
            } else{
                // Show the texture
                mWindowBkg.setDrawableByLayerId(STATUS_BAR_BKG_ID, mWindowDrawable);
            }
        }
        if (mHeaderDrawable != null) {
            if(orientation == Configuration.ORIENTATION_LANDSCAPE || !isDefaultDensity(mActivity)) {
                actionBarExt.setBackgroundDrawable(mActionBarColorDrawable);
            }else {
                actionBarExt.setBackgroundDrawable(mHeaderDrawable);
            }
        } else {
            actionBarExt.setBackgroundDrawable(mActionBarColorDrawable);
        }
    }

    public static boolean isDefaultDensity(Activity activity) {
        boolean displayNormal = true;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            int densityDpi = activity.getResources().getConfiguration().densityDpi;
            int defaultDpi = DisplayMetrics.DENSITY_DEVICE_STABLE;
            displayNormal = (densityDpi == defaultDpi);
        }
        return displayNormal;
    }

    public static void setPopUpWindowExpand(Configuration config, ListPopupBubbleWindow footerPopUpWindow) {
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            footerPopUpWindow.setExpandDirection(PopupBubbleWindow.EXPAND_LEFT);
        } else {
            footerPopUpWindow.setExpandDirection(PopupBubbleWindow.EXPAND_DEFAULT);
        }
    }

    public static int getDefaultFontSize(Context context, int spRes) {
        return getDimensionPixelSize(context,spRes);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static int getDimensionPixelSize(Context context, int dimenRes) {
        int size = context.getResources().getDimensionPixelSize(dimenRes);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
            return size;
        }

        int defaultDpi = DisplayMetrics.DENSITY_DEVICE_STABLE;
        int currentDpi = context.getResources().getConfiguration().densityDpi;
        return currentDpi > defaultDpi ? (size * defaultDpi / currentDpi) : size;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static int getDefaultSize(Context context, int scaledSize) {
        int defaultSize;
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            defaultSize = scaledSize;
        } else {
            int defaultDpi = DisplayMetrics.DENSITY_DEVICE_STABLE;
            int currentDpi = context.getResources().getConfiguration().densityDpi;
            defaultSize = currentDpi > defaultDpi ? (scaledSize * defaultDpi / currentDpi) : scaledSize;
        }
        return defaultSize;
    }

}
