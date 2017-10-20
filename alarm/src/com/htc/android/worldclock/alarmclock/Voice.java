/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.htc.android.worldclock.alarmclock;

import com.htc.android.worldclock.utils.Global;

import android.app.Activity;
import android.app.VoiceInteractor;
import android.util.Log;

/**
 * Notifies Voice Interactor about whether the action
 * was successful. Voice Interactor is called only if
 * the build version is post-Lollipop.
 */
public final class Voice {
    private static final String TAG = "WorldClock.Voice";
    private static Delegate sDelegate = new VoiceInteractorDelegate();

    private Voice() { }

    public static void setDelegate(Delegate delegate) {
        sDelegate = delegate;
    }

    public static void notifySuccess(Activity activity, String message) {
        try {
            if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_M) {
                sDelegate.notifySuccess(activity.getVoiceInteractor(), message);
            }
        } catch (Exception e) {
            // This should never happen
            Log.w(TAG, "Exception notifySuccess: e = ", e);
        }
    }

    public static void notifyFailure(Activity activity, String message) {
        try {
            if (Global.getAndroidSdkPlatform() >= Global.ANDROID_PLATFORM_M) {
                sDelegate.notifyFailure(activity.getVoiceInteractor(), message);
            }
        } catch (Exception e) {
            // This should never happen
            Log.w(TAG, "notifyFailure: e = ", e);
        }
    }

    public interface Delegate {
        void notifySuccess(VoiceInteractor vi, String message);

        void notifyFailure(VoiceInteractor vi, String message);
    }

    private static class VoiceInteractorDelegate implements Delegate {
        @Override
        public void notifySuccess(VoiceInteractor vi, String message) {
            if (vi != null)  {
                final VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(message);
                vi.submitRequest(new VoiceInteractor.CompleteVoiceRequest(prompt, null));
            }
        }

        @Override
        public void notifyFailure(VoiceInteractor vi, String message) {
            if (vi != null)  {
                final VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(message);
                vi.submitRequest(new VoiceInteractor.AbortVoiceRequest(prompt, null));
            }
        }
    }
}
