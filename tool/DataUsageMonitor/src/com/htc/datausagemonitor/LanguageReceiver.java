package com.htc.datausagemonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import java.util.Locale;

import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.lang.ILangChangeListener;
import tmsdk.common.module.lang.ILangDef;
import tmsdk.common.module.lang.MultiLangManager;

/**
 * Created by majing on 17-9-4.
 */

public class LanguageReceiver extends BroadcastReceiver {
    public static final String TAG = LanguageReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
            Log.d(TAG, "data:" + intent.toString());

            Configuration config = context.getResources().getConfiguration();
            MultiLangManager multiLangManager = ManagerCreatorC.getManager(MultiLangManager.class);

            if(config.locale != Locale.ENGLISH)
                multiLangManager.onCurrentLangNotify(ILangDef.ELANG_CHS);
            else
                multiLangManager.onCurrentLangNotify(ILangDef.ELANG_ENG);
        }
    }
}
