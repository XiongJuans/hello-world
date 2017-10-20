package com.htc.android.worldclock.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.CRC32;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.alarmclock.AlarmQueryMediaJobIntentService;
import com.htc.android.worldclock.alarmclock.AlarmQueryMediaService;
import com.htc.android.worldclock.alarmclock.AlarmUtils;
import com.htc.android.worldclock.alarmclock.AlarmUtils.AlarmColumns;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

public class WorldClockBackupAgent extends BackupAgent {
    private static final String TAG = "WorldClock.WorldClockBackupAgent";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String WORLDCLOCK_KEY = "_worldclock_";
    private static final String NULL_STRING = "null_string";

    /** this version num MUST be incremented if the flattened-file schema ever changes */
    static final int BACKUP_AGENT_VERSION = 1004;

    private static final int BACKUP_RESTORE_QUERY_JOB_ID = 1003;

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        if (DEBUG_FLAG) Log.d(TAG, "Enter WorldClockBackupAgent onBackup");
        long savedFileSize = -1;
        long savedCrc = -1;
        int savedVersion = -1;

        PreferencesUtil.setBackupAlarmDB(this, true);
        // Extract the previous clock backup file size & CRC from the saved state
        DataInputStream in = null;
        try {
            if (DEBUG_FLAG) Log.d(TAG, "onBackup: current version: " + BACKUP_AGENT_VERSION);
            in = new DataInputStream(new FileInputStream(oldState.getFileDescriptor()));
            savedFileSize = in.readLong();
            savedCrc = in.readLong();
            savedVersion = in.readInt();
        } catch (EOFException e) {
            // Unable to read state file... be safe and do a backup
            Log.w(TAG, "onBackup: no previous state, e = " + e.toString());
        } finally {
            if (in != null) {
                in.close();
            }
        }

        // Build a flattened representation of the clock table
        File tmpfile = File.createTempFile("bkp", null, getCacheDir());
        FileOutputStream outfstream = null;
        try {
            outfstream = new FileOutputStream(tmpfile);
            long newCrc = buildWorldClockFile(outfstream);

            // Any changes since the last backup?
            if ((savedVersion != BACKUP_AGENT_VERSION) || (newCrc != savedCrc) || (tmpfile.length() != savedFileSize)) {
                if (DEBUG_FLAG) Log.d(TAG, "Begin copy file to backup");
                // Different checksum or different size, so we need to back it up
                copyFileToBackup(WORLDCLOCK_KEY, tmpfile, data);
            }

            // Record our backup state and we're done
            writeBackupState(tmpfile.length(), newCrc, newState);
        } catch (Exception e) {
            Log.w(TAG, "onBackup: buildWorldClockFile or writeBackupState e = " + e.toString());
        } finally {
            if (outfstream != null) {
                outfstream.close();
            }
            // Make sure to tidy up when we're done
            tmpfile.delete();
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        if (DEBUG_FLAG) Log.d(TAG, "Enter WorldClockBackupAgent onRestore");
        long crc = -1;
        File tmpfile = File.createTempFile("rst", null, getFilesDir());
        DataInputStream in = null;
        try {
            while (data.readNextHeader()) {
                if (WORLDCLOCK_KEY.equals(data.getKey())) {
                    crc = copyBackupToFile(data, tmpfile, data.getDataSize());
                    in = new DataInputStream(new FileInputStream(tmpfile));

                    // retrieve data version
                    int backupVersion = in.readInt();
                    if (backupVersion > BACKUP_AGENT_VERSION) {
                        if (DEBUG_FLAG) Log.d(TAG, "not support downgrade! restoring: " + backupVersion + " to current version: " + BACKUP_AGENT_VERSION);
                        return;
                    }

                    // item 1: alarms
                    if (backupVersion < 1000) {
                        // no write version to data
                        int alarmCount = backupVersion; // due to version 1 doesn't be transfered
                        crc = restoreAlarms(crc, in, -1, alarmCount);
                    } else {
                        int alarmCount = in.readInt();
                        crc = restoreAlarms(crc, in, backupVersion, alarmCount);
                    }
                }
                // Last, write the state we just restored from so we can discern
                // changes whenever we get invoked for backup in the future
                writeBackupState(tmpfile.length(), crc, newState);
            }
        } catch (Exception e) {
            Log.w(TAG, "onRestore: restoreAlarms e = " + e.toString());
        } finally {
            if (in != null) {
                in.close();
            }
            // Whatever happens, delete the temp file
            tmpfile.delete();
        }
    }

    // Flatten the clock table into the given file, calculating its CRC in the process
    private long buildWorldClockFile(FileOutputStream outfstream) throws IOException {
        CRC32 crc = new CRC32();
        ByteArrayOutputStream bufstream = new ByteArrayOutputStream();
        DataOutputStream bout = new DataOutputStream(bufstream);

        // write version
        bout.writeInt(BACKUP_AGENT_VERSION);

        // item 1: alarms
        backupAlarms(outfstream, crc, bufstream, bout);
        if (bout != null) {
            bout.close();
        }
        return crc.getValue();
    }

    // Write the file to backup as a single record under the given key
    private void copyFileToBackup(String key, File file, BackupDataOutput data) throws IOException {
        final int CHUNK = 8192;
        byte[] buf = new byte[CHUNK];

        int toCopy = (int) file.length();
        data.writeEntityHeader(key, toCopy);
        FileInputStream in = null;

        try {
            in = new FileInputStream(file);
            int nRead;
            while (toCopy > 0) {
                nRead = in.read(buf, 0, CHUNK);
                data.writeEntityData(buf, nRead);
                toCopy -= nRead;
            }
        } catch (Exception e) {
            Log.w(TAG, "copyFileToBackup e = " + e.toString());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    // Read the given file from backup to a file, calculating a CRC32 along the
    // way
    private long copyBackupToFile(BackupDataInput data, File file, int toRead) throws IOException {
        final int CHUNK = 8192;
        byte[] buf = new byte[CHUNK];
        CRC32 crc = new CRC32();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            while (toRead > 0) {
                int numRead = data.readEntityData(buf, 0, CHUNK);
                crc.update(buf, 0, numRead);
                out.write(buf, 0, numRead);
                toRead -= numRead;
            }
        } catch (Exception e) {
            Log.w(TAG, "copyBackupToFile e = " + e.toString());
        } finally {
            if (out != null) {
                out.close();
            }
        }

        return crc.getValue();
    }

    // Write the given metrics to the new state file
    private void writeBackupState(long fileSize, long crc, ParcelFileDescriptor stateFile) throws IOException {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new FileOutputStream(stateFile.getFileDescriptor()));
            out.writeLong(fileSize);
            out.writeLong(crc);
            out.writeInt(BACKUP_AGENT_VERSION);
        } catch (Exception e) {
            Log.w(TAG, "writeBackupState: e = " + e.toString());
        } finally {
            if (out != null) {
                out.close();
            }
        }

    }

    private long restoreAlarms(long crc, DataInputStream in, int backupVersion, int alarmCount) {
        if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: do replace DB");
        try {
            if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: backupVersion = " + backupVersion);
            if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: alarmCount = " + alarmCount);
            // get cloud data
            getContentResolver().delete(AlarmColumns.CONTENT_URI, null, null);
            ArrayList<Integer> alarmId = new ArrayList<Integer>();
            ArrayList<String> alarmQuerySyntax = new ArrayList<String>();
            
            for (int i = 0; i < alarmCount; i++) {
                ContentValues values = new ContentValues();
                // alarm : 11 items
                values.put(AlarmColumns.HOUR, in.readInt());
                values.put(AlarmColumns.MINUTES, in.readInt());
                values.put(AlarmColumns.DAYS_OF_WEEK, in.readInt());
                values.put(AlarmColumns.ALARM_TIME, in.readInt());
                values.put(AlarmColumns.ENABLED, in.readInt());
                values.put(AlarmColumns.VIBRATE, in.readInt());
                values.put(AlarmColumns.MESSAGE, in.readUTF());
                String alertSoundUriString = in.readUTF();
                if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: alarm " + (i + 1) + ", alertSoundUriString = " + alertSoundUriString);
                if (NULL_STRING.equals(alertSoundUriString)) {
                    alertSoundUriString = null; // Silent Uri case
                }
                values.put(AlarmColumns.ALERT, alertSoundUriString);
                values.put(AlarmColumns.SNOOZED, in.readInt());
                String alarmSoundTitle;
                switch (backupVersion) {
                    case -1:
                        break;
                    case 1001:
                        // check alarm title is changed or not after restore
                        alarmSoundTitle = in.readUTF();
                        if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: alarm " + (i + 1) + ", alarmSoundTitle = " + alarmSoundTitle);
                        // check empty alarm alert uri, default alarm alert uri
                        if (!"".equals(alertSoundUriString) && !Settings.System.DEFAULT_ALARM_ALERT_URI.toString().equals(alertSoundUriString)) {
                            // check silent case
                            if (!TextUtils.isEmpty(alarmSoundTitle) && !getString(R.string.st_silent).equals(alarmSoundTitle)) {
                                Uri restoreUri = AlertUtils.getAlarmRestoreAlertUriByTitle(this, alarmSoundTitle);
                                if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: alarm " + (i + 1) + ", restoreUri = " + restoreUri);
                                values.put(AlarmColumns.ALERT, restoreUri.toString());
                                if ("".equals(restoreUri.toString())) {
                                    if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: add alarm id = " + (i + 1) + " to AlarmQueryMediaService");
                                    alarmId.add(i + 1);
                                    alarmQuerySyntax.add(alarmSoundTitle);
                                }
                            }
                        }
                        break;
                    case 1002:
                    case 1003:
                    case 1004:
                        //if alertSoundUriString is null don't restore alertQuerySyntax
                        if (alertSoundUriString != null) {
                            String alertQuerySyntax = in.readUTF();
                            if (!"".equals(alertQuerySyntax)) {
                                Uri restoreUri = AlertUtils.getAlarmRestoreAlertUriByQuertCondition(this, alertQuerySyntax);
                                if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: alarm " + (i + 1) + ", restoreUri = " + restoreUri);
                                values.put(AlarmColumns.ALERT, restoreUri.toString());
                                if ("".equals(restoreUri.toString())) {
                                    if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: add alarm id = " + (i + 1) + " to AlarmQueryMediaService");
                                    alarmId.add(i + 1);
                                    alarmQuerySyntax.add(alertQuerySyntax);
                                }
                            }
                        }
                        if (1003 <= backupVersion) {
                            values.put(AlarmColumns.OFFALARM, in.readInt());
                        }
                        if (1004 <= backupVersion) {
                            values.put(AlarmColumns.REPEAT_TYPE, in.readInt());
                        }
                        break;
                    default:
                        Log.w(TAG, "restoreAlarms: no this version = " + backupVersion);
                }
                getContentResolver().insert(AlarmColumns.CONTENT_URI, values);
            }

            if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: query AlarmQueryMediaService count = " + alarmId.size());
            if (alarmId.size() > 0) {
                if (DEBUG_FLAG) Log.d(TAG, "restoreAlarms: start AlarmQueryMediaService");
                Intent queryIntent = new Intent(this, AlarmQueryMediaService.class);
                queryIntent.setAction(AlertUtils.ACTION_QUERY_SYNTAX);
                queryIntent.putExtra(AlertUtils.EXTRA_QUERY_VERSION, backupVersion);
                queryIntent.putIntegerArrayListExtra(AlarmUtils.ID, alarmId);
                queryIntent.putStringArrayListExtra(AlertUtils.EXTRA_QUERY_SYNTAX, alarmQuerySyntax);
                if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_O) {
                    AlarmQueryMediaJobIntentService.enqueueWork(this, AlarmQueryMediaJobIntentService.class, BACKUP_RESTORE_QUERY_JOB_ID, queryIntent);
                } else {
                    startService(queryIntent);
                }
            }

            // trigger next alarm
            AlarmUtils.setNextAlert(WorldClockBackupAgent.this);
        } catch (IOException e) {
            Log.w(TAG, "restoreAlarms: quotes not restoring " + e.toString());
            crc = -1;
        }
        return crc;
    }

    private void backupAlarms(FileOutputStream outfstream, CRC32 crc, ByteArrayOutputStream bufstream, DataOutputStream bout) throws IOException {
        Cursor cursor = getContentResolver().query(
            AlarmColumns.CONTENT_URI,
            new String[] { AlarmColumns.HOUR, AlarmColumns.MINUTES,
                AlarmColumns.DAYS_OF_WEEK, AlarmColumns.ALARM_TIME,
                AlarmColumns.ENABLED, AlarmColumns.VIBRATE,
                AlarmColumns.MESSAGE, AlarmColumns.ALERT,
                AlarmColumns.SNOOZED, AlarmColumns.OFFALARM, AlarmColumns.REPEAT_TYPE }, null, null, null);
        // The first thing in the file is the row count...
        int count;
        if (cursor == null) {
            count = 0;
        } else {
            count = cursor.getCount();
        }
        if (DEBUG_FLAG) Log.d(TAG, "backupAlarms: Backing up " + count + " alarms");
        bout.writeInt(count);
        byte[] record = bufstream.toByteArray();
        crc.update(record);
        outfstream.write(record);
        // ... followed by the data for each row
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();

            // alarm : 9 items

            // get alert title
            Uri alertSoundUri;
            String alertSoundUriString = cursor.getString(7);
            if (DEBUG_FLAG) Log.d(TAG, "backupAlarms: alarm " + (i + 1) + ", alertSoundUriString = " + alertSoundUriString);
            if (alertSoundUriString == null) {
                // silent case
                alertSoundUri = null;
            } else {
                // general media provider case
                alertSoundUri = Uri.parse(alertSoundUriString);
            }
            if (DEBUG_FLAG) Log.d(TAG, "backupAlarms: alarm " + (i + 1) + ", alertSoundUri = " + alertSoundUri);

            String alertSoundTitle = "";
            // get alert sound title
            if (alertSoundUri == null) {
                alertSoundTitle = getString(R.string.st_silent);
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(this, alertSoundUri);
                if (ringtone != null) {
                    ringtone.setStreamType(RingtoneManager.TYPE_ALARM);
                    alertSoundTitle = ringtone.getTitle(this);
                }
            }
            if (DEBUG_FLAG) Log.d(TAG, "backupAlarms: alarm " + (i + 1) + ", alertSoundTitle = " + alertSoundTitle);

            // construct the flattened record in a byte array
            bufstream.reset();
            bout.writeInt(cursor.getInt(0)); // HOUR
            bout.writeInt(cursor.getInt(1)); // MINUTES
            bout.writeInt(cursor.getInt(2)); // DAYS_OF_WEEK
            bout.writeInt(cursor.getInt(3)); // ALARM_TIME
            bout.writeInt(cursor.getInt(4)); // ENABLED
            bout.writeInt(cursor.getInt(5)); // VIBRATE
            bout.writeUTF(cursor.getString(6)); // MESSAGE
            if (alertSoundUriString == null) {
                bout.writeUTF(NULL_STRING); // ALERT null case
            } else {
                bout.writeUTF(alertSoundUriString); // ALERT normal case
            }
            bout.writeInt(cursor.getInt(8)); // SNOOZED
            // if alertSoundUri is null don't backup alertQuerySyntax
            if (alertSoundUri != null) {
                String alertQuerySyntax = alertQueryCondition(alertSoundUri.toString(), alertSoundTitle);
                if (DEBUG_FLAG) Log.d(TAG, "backupAlarms: alertQuerySyntax = " + alertQuerySyntax);
                bout.writeUTF(alertQuerySyntax); // ALERT Query Syntax
            }
            bout.writeInt(cursor.getInt(9)); // OFFALARM
            bout.writeInt(cursor.getInt(10)); // RepeatType

            // Update the CRC and write the record to the temp file
            record = bufstream.toByteArray();
            crc.update(record);
            outfstream.write(record);
        }
        if (cursor != null) {
            if (cursor.isClosed() == false) {
                cursor.close();
            }
        }
    }

    private String alertQueryCondition(String alertSoundUriString, String alertSoundTitle) {
        String alertQuerySyntax = "";
        String fileString;
        Uri alarmSoundUri;
        if (DEBUG_FLAG) Log.d(TAG, "alertQueryCondition: alertSoundUriString = " + alertSoundUriString);
        if (Settings.System.DEFAULT_ALARM_ALERT_URI.toString().equals(alertSoundUriString)) {
            // default case, get audio alert from Setting
            alarmSoundUri = AlertUtils.getAlarmDefaultAlertUri(this);
            return alertQuerySyntax;
        } else if (alertSoundUriString == null) {
            // silent case
            alarmSoundUri = null;
            return alertQuerySyntax;
        } else {
            // general media provider case
            alarmSoundUri = Uri.parse(alertSoundUriString);
            if (DEBUG_FLAG) Log.d(TAG, "alertQueryCondition: alarmSoundUri = " + alarmSoundUri);
        }
        if (!"".equals(alarmSoundUri.toString())) {
            fileString = getAlertUriFileName(alarmSoundUri.toString());
            alertQuerySyntax = "_data like " + DatabaseUtils.sqlEscapeString("%/" + fileString) + " AND title = " + DatabaseUtils.sqlEscapeString(alertSoundTitle);
        }
        return alertQuerySyntax;
    }

    // get alarm file
    public String getAlertUriFileName(String alarmSoundTitle) {
        Cursor cursor = null;
        String[] SplitArray = { "" };
        int splitLength = 0;
        try {
            String[] projection = { MediaStore.MediaColumns.DATA };
            SplitArray = alarmSoundTitle.split("/");
            splitLength = SplitArray.length;
            if (DEBUG_FLAG) Log.d(TAG, "getAlertUriFileName: SplitArray[splitLength - 1] = " + SplitArray[splitLength - 1]);
            String where = "_id = " + DatabaseUtils.sqlEscapeString(SplitArray[splitLength - 1]);
            if (DEBUG_FLAG) Log.d(TAG, "getAlertUriFileName: where = " + where);

            if (alarmSoundTitle.contains(MediaStore.Audio.Media.INTERNAL_CONTENT_URI.toString())) {
                cursor = getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, where, null, null);
                if (cursor != null) {
                    if (DEBUG_FLAG) Log.d(TAG, "getAlertUriFileName: INTERNAL_CONTENT_URI cursor.getCount() = " + cursor.getCount());
                }
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    if (DEBUG_FLAG) Log.d(TAG, "getAlertUriFileName: cursor.getString(0) = " + cursor.getString(0));
                    SplitArray = cursor.getString(0).split("/");
                    splitLength = SplitArray.length;
                }
            } else if (alarmSoundTitle.contains(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, where, null, null);
                if (cursor != null) {
                    if (DEBUG_FLAG) Log.d(TAG, "getAlertUriFileName: EXTERNAL_CONTENT_URI cursor.getCount() = " + cursor.getCount());
                }
                if ((null != cursor) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                    if (DEBUG_FLAG) Log.d(TAG, "getAlertUriFileName: cursor.getString(0) = " + cursor.getString(0));
                    SplitArray = cursor.getString(0).split("/");
                    splitLength = SplitArray.length;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getAlertUriFileName: e = " + e.toString());
        } finally {
            if (cursor != null) {
                if (cursor.isClosed() == false) {
                    cursor.close();
                }
            }
        }

        if (DEBUG_FLAG) Log.d(TAG, "getAlertUriFileName: SplitArray[splitLength - 1] = " + SplitArray[splitLength - 1]);
        return SplitArray[splitLength - 1];
    }
}
