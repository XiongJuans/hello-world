package com.htc.android.worldclock.utils;

import com.htc.lib1.cc.R;
import com.htc.lib1.cc.widget.HtcListItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class ClockListItem extends HtcListItem {
	
    private Drawable mFocusIndicator;
    private boolean mDrawFocusIndicator;


	public ClockListItem(Context arg0, AttributeSet arg1) {
		super(arg0, arg1);
	}

	public ClockListItem(Context arg0, int arg1) {
		super(arg0, arg1);
	}

	public ClockListItem(Context arg0) {
		super(arg0);
	}

	@Override
	public void setFocusable(boolean focusable) {
	      super.setFocusable(focusable);
	        if (focusable && mFocusIndicator == null) {
	            mFocusIndicator = getContext().getResources().getDrawable(R.drawable.common_focused);
	            if (mFocusIndicator != null) {
	                mFocusIndicator.mutate();
	                mFocusIndicator.setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.overlay_color), PorterDuff.Mode.SRC_ATOP));
	            }
	        }
	}

	@Override
	protected void onDraw(Canvas canvas) {
        // Regular draw jobs
        if (mDrawFocusIndicator) {
            drawIndicatorWhenFocused(canvas);
         }

	}
	
    protected void drawIndicatorWhenFocused(Canvas canvas) {
        mFocusIndicator.setBounds(canvas.getClipBounds());
        mFocusIndicator.draw(canvas);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        mDrawFocusIndicator = gainFocus;
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }
}
