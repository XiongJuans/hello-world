package com.htc.android.worldclock.utils;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class LongSummaryListPreference extends ListPreference {
    private static final int MAX_LINES = 5;

    public LongSummaryListPreference(Context ctx) {
        super(ctx);
    }

    public LongSummaryListPreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    @Override
    protected void onBindView(View view)
    {
        super.onBindView(view);
        TextView summary= (TextView)view.findViewById(android.R.id.summary);
        summary.setSingleLine(false);
        summary.setMaxLines(MAX_LINES);
    }

}
