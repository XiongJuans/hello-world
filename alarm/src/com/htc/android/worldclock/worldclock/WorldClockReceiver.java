package com.htc.android.worldclock.worldclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.htc.android.worldclock.utils.Global;

public class WorldClockReceiver extends BroadcastReceiver {
    private static final int WORLDCLOCK_JOB_ID = 1000;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Intent service = new Intent(context, WorldClockService.class);
        service.setAction(intent.getAction());

        if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
            WorldClockJobIntentService.enqueueWork(context, WorldClockJobIntentService.class, WORLDCLOCK_JOB_ID, service);
        } else {
            context.startService(service);
        }
    }
}
