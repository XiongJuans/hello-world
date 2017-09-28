package com.htc.datausagemonitor;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tmsdk.common.TMSBootReceiver;

/**
 * Created by mj on 2017/7/28.
 */

public class HTCTMSBootReceiver extends TMSBootReceiver {

    public HTCTMSBootReceiver() {

        super();

    }

    @Override
    public void doOnRecv(Context context, Intent intent) {
        super.doOnRecv(context, intent);
        Log.d("HTCTMSBootReceiver","kaishi");

    }
}
