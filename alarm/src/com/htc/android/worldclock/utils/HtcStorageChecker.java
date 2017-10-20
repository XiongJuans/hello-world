package com.htc.android.worldclock.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.StatFs;

import com.htc.android.worldclock.R;
import com.htc.lib1.cc.widget.HtcAlertDialog;

public class HtcStorageChecker {
    public static final long INTERNAL_MEMORY_THRESOLD = 10 * 1024 * 1024; // 10MB
    private static final String PATH = Environment.getDataDirectory().getPath();

    private static synchronized long checkIternalMemory() {
        StatFs fileStats = new StatFs(PATH);
        long availableBlocks = fileStats.getAvailableBlocks();
        long blockSize = fileStats.getBlockSize();
        long size = availableBlocks * blockSize;
        return size;
    }

    public static synchronized boolean isInternalStorageEnough() {
        long size = checkIternalMemory();
        return ((size <= INTERNAL_MEMORY_THRESOLD) ? false : true);
    }

    public static void checkStorageFull(final Activity activity) {
        if (!HtcStorageChecker.isInternalStorageEnough()) {
            String desc = String.format(activity.getString(R.string.storage_full_description));
            HtcAlertDialog.Builder alertDialogView = new HtcAlertDialog.Builder(activity);
            alertDialogView.setTitle(R.string.storage_full);
            alertDialogView.setMessage(desc);
            alertDialogView.setCancelable(false); // set touch outside of dialog to disable for ICS
            alertDialogView.setPositiveButton(R.string.settings_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent i = new Intent(Intent.ACTION_MANAGE_PACKAGE_STORAGE);
                        activity.startActivity(i);
                        activity.finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            alertDialogView.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.finish();
                }
            });
            alertDialogView.create().show();
        }
    }
}