package com.htc.android.worldclock.alarmclock;

import android.content.Intent;
import android.os.Bundle;

import com.htc.android.worldclock.CarouselTab;
import com.htc.android.worldclock.WorldClockTabControl;

/**
 * add a new shortcut activity to handle shortcut intent.
 * to fix issue about clock not show in recent.
 */
public class ShortcutActivity extends HandleApiCalls {

    /**
     * add stopwatch shortcut intent.
     */
    private static final String ACTION_SET_STOPWATCH = "com.htc.action.SET_STOPWATCH";

    @Override
    protected void onCreate(Bundle icicle) {
        //handle api calls first
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent != null) {
            //add extra shortcut intent for stopwatch
            if (ACTION_SET_STOPWATCH.equals(intent.getAction())) {
                handleShowStopwatch();
                finish();
            }
        }
    }

    private void handleShowStopwatch() {
        Intent stopwatchIntent = new Intent();
        stopwatchIntent.setClassName(getPackageName(),
                WorldClockTabControl.LAUNCH_AP_ACTIVITY_NAME);
        stopwatchIntent.putExtra(CarouselTab.WORLDCLOCK_ACTION, CarouselTab.TAB_STOPWATCH);
        startActivity(stopwatchIntent);
    }
}
