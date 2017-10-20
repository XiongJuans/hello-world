package com.htc.android.worldclock.aiservice;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;

import com.htc.android.worldclock.R;
import com.htc.lib1.cc.widget.HtcDeleteButton;

public class AiHtcDeleteButton extends HtcDeleteButton {
    public AiHtcDeleteButton(Context context) {
        super(context);
    }

    public AiHtcDeleteButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AiHtcDeleteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * set delete button background color for ai alarm
     */
    public void setAiBackgroundColor() {
        if (mBackgroundRest != null) {
            mBackgroundRest.setColorFilter(getResources().getColor(R.color.ai_tip_background, null), PorterDuff.Mode.SRC_ATOP);
            invalidate();
        }
    }

    /**
     * set delete button background color for normal alarm for theme color
     */
    public void restoreAiColor() {
        if (mBackgroundRest != null) {
            mBackgroundRest.setColorFilter(mCategoryColor, PorterDuff.Mode.SRC_ATOP);
            invalidate();
        }
    }

}
