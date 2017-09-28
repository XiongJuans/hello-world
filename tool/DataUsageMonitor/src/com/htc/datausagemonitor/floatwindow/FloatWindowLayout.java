package com.htc.datausagemonitor.floatwindow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.htc.datausagemonitor.R;


/**
 * Created by nancy on 8/23/17.
 */
public class FloatWindowLayout extends LinearLayout {
    private final static String TAG = FloatWindowLayout.class.getSimpleName();

    public FloatWindowLayout(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);

        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        View view = LayoutInflater.from(context).inflate(
                R.layout.float_window, null);
        this.addView(view);
    }


}