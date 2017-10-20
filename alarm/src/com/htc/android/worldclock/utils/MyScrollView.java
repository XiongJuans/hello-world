package com.htc.android.worldclock.utils;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {
    private Handler mHandler;

    public static final int WHAT_ON_SCROLL = 6001;

    public MyScrollView(Context context) {
        this(context, null);
        
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        
        boolean bHandler = super.onTouchEvent(ev);
        // HtcLog.d("onTouchEvent action:" + ev.getAction());
        if (bHandler &&
                ((ev.getAction() == MotionEvent.ACTION_DOWN) ||
                    (ev.getAction() == MotionEvent.ACTION_UP))) {
            scrollNotify();
        }
        return bHandler;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        
        Rect r = new Rect();
        ViewGroup group = (ViewGroup) this.getChildAt(0);
        View v = group.getChildAt(0);
        v.getHitRect(r);
        // HtcLog.d("scrollview", "getY = " + ev.getY() + ", rawY = " + ev.getRawY());
        // HtcLog.d("scrollview", "scrollY = " + this.getScrollY());

        if ((ev.getY() + this.getScrollY() <= r.bottom) && (ev.getX() <= r.right)) {
            return false;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    private void scrollNotify() {
        
        if (mHandler != null) {
            mHandler.sendEmptyMessage(WHAT_ON_SCROLL);
        }
    }

    public void setHandler(Handler handler) {
        
        mHandler = handler;
    }
}
