/*
 * HTC Corporation Proprietary Rights Acknowledgment
 *
 * Copyright (C) 2008 HTC Corporation
 *
 * All Rights Reserved.
 *
 * The information contained in this work is the exclusive property of
 * HTC Corporation ("HTC"). Only the user who is legally
 * authorized by HTC ("Authorized User") has right to employ this work
 * within the scope of this statement. Nevertheless, the Authorized User
 * shall not use this work for any purpose other than the purpose agreed by HTC.
 * Any and all addition or modification to this work shall be unconditionally
 * granted back to HTC and such addition or modification shall be solely owned by HTC.
 * No right is granted under this statement, including but not limited to,
 * distribution, reproduction, and transmission, except as otherwise provided in this statement.
 * Any other usage of this work shall be subject to the further written consent of HTC.
 *
 */

package com.htc.android.worldclock.utils;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.content.pm.PackageManager;
public class HtcPhoneSensorFunctions implements SensorEventListener {
    protected static final String LOG_TAG = "HtcPhoneSensorFunctions";
    //********
    // Constant
    //*********
    protected static final float OneEightyOverPi = 57.29577957855f;

    //*********
    //Member data
    //*********
    protected Context mContext;
    protected final SensorManager mSensorManager;

    protected Sensor mOrientationSensor;

    protected boolean mRotateToSlientStart = false;
    protected int mRotateToSlientCount = 0;

    boolean mSupportedOrientSensor = true;

    // [HTC_PHONE] s: For low end device to avoid Z value becoming abnormal
    private float[] mAccelerometerSensorRawData = new float[3];
    // [HTC_PHONE] e: For low end device to avoid Z value becoming abnormal

    private FlipActionsCallBack mCallBack;

    private static HtcPhoneSensorFunctions sPhoneSensorFunctions;

    private boolean isFlipAlarmSuccessful;

    public static HtcPhoneSensorFunctions getInstances(Context context) {
        if (sPhoneSensorFunctions == null) {
            sPhoneSensorFunctions = new HtcPhoneSensorFunctions(context);
        }
        return sPhoneSensorFunctions;
    }

    public static void releaseInstances() {
        sPhoneSensorFunctions = null;
    }

    private HtcPhoneSensorFunctions(Context context) {
        mContext = context;

        PackageManager pkgMgr = mContext.getPackageManager();

        //Check whehter orientation sensor is enabled
        if (null != pkgMgr) {
            boolean hasGyro = pkgMgr.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
            boolean hasCompass = pkgMgr.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);

            if (!(hasGyro || hasCompass)) {
                mSupportedOrientSensor = false;
            }
        }

        //Get Sensor manager
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        registerOrientationSensor();

    }

    public void setCallBack(FlipActionsCallBack callBack) {
        this.mCallBack =callBack;
    }

    protected void resetSensorFlags() {
        // rotate to slient
        mRotateToSlientStart = false;
        mRotateToSlientCount = 0;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }


    public static boolean isSupportSensorFeature(Context context) {
        PackageManager pkgMgr = context.getPackageManager();
        if (null != pkgMgr) {
            boolean hasGyro = pkgMgr.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
            boolean hasCompass = pkgMgr.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
            boolean hasAcce = pkgMgr.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
            Log.d(LOG_TAG, "hasGyro:" + hasGyro + "hasCompass:" + hasCompass + "hasAcce:" + hasAcce);
            if (hasGyro || hasCompass || hasAcce) {
                return true;
            }
        }
        return false;
    }


    public void registerOrientationSensor() {
        if (mOrientationSensor == null) {
            log("registerOrientationSensor");
            //Use orientation sensor or simulate orientation by accelerometer
            if (mSupportedOrientSensor) {
                mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            } else {
                mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
            mSensorManager.registerListener(this, mOrientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            // force clean due to timing issue
            // sometimes the sensor notify event even unregistered.
            // leak?
            resetSensorFlags();
        }
    }

    public void unregisterOrientationSensor() {
        if (mOrientationSensor != null) {
            log("unregisterOrientationSensor()");
            cleanOrientationSensor();
            resetSensorFlags();
        }
    }

    private void cleanOrientationSensor() {
        mSensorManager.unregisterListener(this, mOrientationSensor);
        mOrientationSensor = null;
    }


    public void onSensorChanged(SensorEvent event) {
        manipulateSenesorData(event);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

    private void manipulateSenesorData(SensorEvent event) {
        log("event.sensor.getType(): " + event.sensor.getType());
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            float pitch = event.values[1];
            float roll = event.values[2];

            handleOrientationSensor(pitch, roll);

        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Handle these events only for device without orientation sensor
            if (!mSupportedOrientSensor) {
                Orientation orientation = new Orientation(event);

                // [HTC_PHONE] s: For low end device to avoid Z value becoming abnormal
                mAccelerometerSensorRawData[0] = event.values[0];
                mAccelerometerSensorRawData[1] = event.values[1];
                mAccelerometerSensorRawData[2] = event.values[2];
                // [HTC_PHONE] e: For low end device to avoid Z value becoming abnormal

                handleOrientationSensor(orientation.pitch, orientation.roll);
            }
        }
    }


    private void handleOrientationSensor(float pitch, float roll) {
        isFlipAlarmSuccessful = false;
        log("Pitch: " + pitch + ". Roll: " + roll);
        handleRotateToSilent(pitch, roll);
    }

    public boolean getIsFlipAlarmSuccessful() {
        return isFlipAlarmSuccessful;
    }

    private void handleRotateToSilent(float pitch, float roll) {
        // face to sky, init case
        if (mSupportedOrientSensor) {
            //modified by jason liu #2013/12/14: pitch from -60 to -90:
            //The value may need to be optimized. Try always flip to mute, -60 is too small and sometimes failed to trigger roate to silent again
            if ((pitch < 60.0 && pitch > -90.0) && (roll < 50.0 && roll > -50.0)) {
            /*
            for "if( !(pitch==0.0 ||roll==0.0) ) " check,
            This is a dirty patch for we may got few begining wrong pitch/roll values
            when device is totally face down to earth as an initial case
            The better way is to find out why wrong values are reutrned when totally face down
            htc shawn
            */
                if (!(pitch == 0.0 && roll == 0.0)) {
                    log("RotateToSilent face to sky. Pitch: " + pitch + " Roll: " + roll);
                    mRotateToSlientStart = true;
                    mRotateToSlientCount = 0;
                }
            }
        } else {
            if ((pitch > 0 && (pitch < 50.0 || pitch > 130.0)) || (roll > 0 && (roll < 50.0 || roll > 130.0))) {
                mRotateToSlientStart = true;
                mRotateToSlientCount = 0;
            }
        }

        // face to ground
        if (mRotateToSlientStart) {
            if (mSupportedOrientSensor) {
                // both 10 degree to judge face down
                if ((pitch < -170.0 || pitch > 170.0) && (roll < 10.0 && roll > -10.0)) {
                    mRotateToSlientCount++;
                }
            } else {
                // both 10 degree to judge face down
                if ((pitch > -100.0 && pitch < -80.0) && (roll > -100.0 && roll < -80.0)) {
                    mRotateToSlientCount++;
                }
            }

            if (mRotateToSlientCount > 2) {
                log("RotateToSilent face to ground. Pitch: " + pitch + " Roll: " + roll);
                log("RotateToSilent, flip alarm successful");
                //It seems we have some workaround in callnotifer.silenceRinger();
                //But it have delay if we use telecommManager.
                //It should be ok if we call stop ring first and trigger silenceRinger again.
                //mRinger.stopRing();
                //call back alarm behavior
                if (mCallBack != null) {
                    mCallBack.flipOptions();
                }
                unregisterOrientationSensor();
                isFlipAlarmSuccessful = true;
            }
        }
    }

    public class Orientation {
        float pitch = 0.0f;
        float roll = 0.0f;

        public Orientation(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                pitch = (float) Math.atan2(z, y) * OneEightyOverPi;
                roll = (float) Math.atan2(z, x) * OneEightyOverPi;
            }
        }
    }

    public interface FlipActionsCallBack {
        void flipOptions();
    }
}

