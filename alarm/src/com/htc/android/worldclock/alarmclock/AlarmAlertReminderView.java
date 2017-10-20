package com.htc.android.worldclock.alarmclock;

import com.htc.android.worldclock.R;
import com.htc.android.worldclock.voiceutils.VoiceManager;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.widget.reminder.drag.BaseTile.Button;
import com.htc.lib1.cc.widget.reminder.ui.ReminderView;
import com.htc.lib1.lockscreen.reminder.HtcReminderViewMode;
import com.htc.lib1.masthead.view.Masthead;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

public class AlarmAlertReminderView extends ReminderView {
    private static final String TAG = "WorldClock.AlarmAlertReminderView";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private Context mContext;
    private Button mDismiss;
    private Button mSnooze;

    private TextView mTextView1;
    private TextView mTextView2;
    private TextView mTextView3;
    
    private ReminderTile mTile;
    private Masthead mMasthead;

    //for voice commands
    private PopupWindow mPopupTip;
    private VoiceManager mVoiceManager;
    private static final String VOICE_HINT = "voice_hint";
    private static final String KEY_CANCELLED = "user_cancelled";

    public AlarmAlertReminderView(Context context) {
        super(context);
        initView(context);
    }
    
    public AlarmAlertReminderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }
    
    public AlarmAlertReminderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }
    
    private void initView(Context context) {
        mContext = context;
        // TODO: initial View.
        // setReminderTile(Layout ID, Index)
        // Index: 1 (1st tile) or 2 (2nd tile)
        mTile = setReminderTile(R.layout.specific_alert_reminder_3_lines, 1);
        // Error Handling
        if (mTile == null) {
            if (DEBUG_FLAG) Log.d(TAG, "initView Failed: tile");
            return;
        }
        mTile.setButtonAccessibilityEnabled(true);
        // Buttons.
        Resources  res = (mContext != null)? mContext.getResources(): null;
        if (res != null) {
            mDismiss = new Button(mTile);
            if (mDismiss != null) {
                mDismiss.setTitle(res.getString(R.string.alarm_alert_dismiss_text));
                mDismiss.setIcon(res.getDrawable(R.drawable.icon_btn_lockscreen_cancel_dark_xl));
            }
            mSnooze = new Button(mTile);
            if (mSnooze != null) {
                mSnooze.setTitle(res.getString(R.string.alarm_alert_snooze_text));
                mSnooze.setIcon(res.getDrawable(R.drawable.icon_btn_postpone_dark_xl));
            }
        }
        // Tile UI.
        mTextView1 = (TextView) this.findViewById(R.id.reminder_title);
        mTextView2 = (TextView) this.findViewById(R.id.reminder_description);
        mTextView3 = (TextView) this.findViewById(R.id.reminder_time);
//        updateUI();
  
        // set Masthead
        mMasthead = new Masthead(mContext);
        mMasthead.applyReminderViewSetting();
        setMastheadOnTop(mMasthead);
        mVoiceManager =  VoiceManager.getInstance(mContext);
        mVoiceManager.setLockScreenCallBack(new UIVoiceManagerCallBack());
    }
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        boolean isShowTips = mVoiceManager.getIsSupportForVoiceCommands() && !getUserCancelled();
        if (DEBUG_FLAG) Log.d(TAG, "can show tips =" + isShowTips);
        if (hasWindowFocus && isShowTips) {
            showVoiceTip();
        } else if (mPopupTip != null) {
            mPopupTip.dismiss();
            mPopupTip = null;
        }
    }


    /**
     * to show voice tip when lunch reminder view.
     */
    public void showVoiceTip() {
        if (mPopupTip == null) {
            View content = LayoutInflater.from(getContext()).inflate(R.layout.voice_hint, this, false);
            android.widget.Button cancelButton = (android.widget.Button) content.findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveUserCancelled();
                    mPopupTip.dismiss();
                }
            });
            mPopupTip = new PopupWindow(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (!mPopupTip.isShowing()) {
            mPopupTip.showAtLocation(this, Gravity.TOP, 0, 0);
        }
    }

    private void saveUserCancelled() {
        SharedPreferences sp = getContext().getSharedPreferences(VOICE_HINT, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_CANCELLED, true).apply();
    }

    private boolean getUserCancelled() {
        SharedPreferences sp = getContext().getSharedPreferences(VOICE_HINT, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_CANCELLED, false);
    }
    
    public void cleanUp() {
        super.cleanUp();
        mMasthead.stop();
    }
    
    public void updateUI(int id, long time, String description) {
        super.updateUI();
        // TODO: update UI
        setMessage(id, time, description);
    }
    
    private void setMessage(int id, long time, String description) {
        String line1 = mContext.getString(R.string.alarm);//TAG + " Line 1";
        String line2 = description;//TAG + " Line 2";
        String line3;//TAG + " Line 3";
        if (mTextView1 != null) {
            // setTitle(view, string):
            // Check the title if all letter should be uppercase.
            // by com.htc.util.res.HtcResUtil.isInAllCapsLocale(context)
            setTitle(mTextView1, line1);
        }
        
        if (mTextView2 != null) {
            if (TextUtils.isEmpty(line2)) {
                line2 = mContext.getString(R.string.alarm);
            }
            mTextView2.setText(line2);
        }

        line3 = DateFormat.getTimeFormat(mContext).format(time);
        if (mTextView3 != null) {
            mTextView3.setText(line3);
        }
        if (mTile != null) {
            mTile.resetStringForAccessibility();
            mTile.addStringForAccessibility(line1);
            mTile.addStringForAccessibility(line2);
            mTile.addStringForAccessibility(line3);
        }
    }
    
    @Override
    public void onTileDropEnd(ReminderTile tile) {
        super.onTileDropEnd(tile);
        if (tile != null) {
            // TODO: do something when tile is drop.
            if (tile == getTile(1)) {
                if (DEBUG_FLAG) Log.d(TAG, "onTileDropEnd: main Tile");
            }
            if (mCallback != null) {
                mCallback.onTileDropEnd();
            }
        }
    }

    public void onButtonDrop(Button button) {
        super.onButtonDrop(button);
        if (button != null) {
            int index = -1;
            // TODO: do something when button is drop.
            if (button == mDismiss) {
                if (DEBUG_FLAG) Log.d(TAG, "onButtonDrop: " + mDismiss);
                index = 0;
            } else if (button == mSnooze) {
                if (DEBUG_FLAG) Log.d(TAG, "onButtonDrop: " + mSnooze);
                index = 1;
            }
            if (mCallback != null) {
                mCallback.onButtonDrop(index);
            }
        }
    }
    
    public int getButtonCount() {
        // TODO: Button Count: 2 or 4
        return 2;
    }
    
    public Button getButton(int index) {
        // TODO: UI & Index : 
        // | Dismiss | Setting |
        // |    0    |    1    |
        if (index == 0) {
            return mDismiss;
        } else if (index == 1) {
            return mSnooze;
        }
        return null;
    }
    
    // TODO: Callback to Activity by yourself requirement.
    public interface Callback {
        void onTileDropEnd();
        void onButtonDrop(int index);
    };
    Callback mCallback;
    
    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    public int getViewMode() {
        return HtcReminderViewMode.ALARM_MODE;
    }

    /**
     * for voice manager callback for UI update.
     */
    class UIVoiceManagerCallBack implements VoiceManager.VoiceManagerUICallBack {
        @Override
        public void showTips(boolean show) {
            if (show && !getUserCancelled()) {
                showVoiceTip();
            } else if (mPopupTip != null) {
                mPopupTip.dismiss();
                mPopupTip = null;
            }
        }
    }
}
