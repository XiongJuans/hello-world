package com.htc.datausagemonitor.fragment;


import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.htc.datausagemonitor.R;

/**
 * Created by majing on 17-9-11.
 */

public class MyPreferenceCategory extends PreferenceCategory {
    public MyPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setBackgroundResource(R.color.separator_color);
        if (view instanceof TextView) {
            TextView tv = (TextView) view;

            tv.setTextColor(Color.BLACK);
        }
    }
}
