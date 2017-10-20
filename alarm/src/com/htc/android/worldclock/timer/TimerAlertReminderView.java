package com.htc.android.worldclock.timer;

import java.util.Calendar;
import java.util.Date;

import com.htc.android.worldclock.R;
import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;
import com.htc.lib1.cc.widget.reminder.drag.BaseTile.Button;
import com.htc.lib1.cc.widget.reminder.ui.ReminderView;
import com.htc.lib1.lockscreen.reminder.HtcReminderViewMode;
import com.htc.lib1.masthead.view.Masthead;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class TimerAlertReminderView extends ReminderView {
    private static final String TAG = "WorldClock.TimerAlertReminderView";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private Context mContext;
    private Button mDismiss;
    private Button mSetting;

    private TextView mTextView1;
    private TextView mTextView2;
    private TextView mTextView3;
    
    private ReminderTile mTile;
    private Masthead mMasthead;

    public TimerAlertReminderView(Context context) {
        super(context);
        initView(context);
    }
    
    public TimerAlertReminderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }
    
    public TimerAlertReminderView(Context context, AttributeSet attrs, int defStyle) {
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
            mSetting = new Button(mTile);
            if (mSetting != null) {
                mSetting.setTitle(res.getString(R.string.timer_reminder_tile_setting));
                mSetting.setIcon(res.getDrawable(R.drawable.icon_btn_settings_dark_xl));
            }
        }
        // Tile UI.
        mTextView1 = (TextView) this.findViewById(R.id.reminder_title);
        mTextView2 = (TextView) this.findViewById(R.id.reminder_description);
        mTextView3 = (TextView) this.findViewById(R.id.reminder_time);
        updateUI();
        
        // set Masthead
        mMasthead = new Masthead(mContext);
        mMasthead.applyReminderViewSetting();
        setMastheadOnTop(mMasthead);
    }
    
    public void cleanUp() {
        super.cleanUp();
        mMasthead.stop();
    }
    
    public void updateUI() {
        super.updateUI();
        // TODO: update UI
        setMessage();
    }
    
    private void setMessage() {
        if (mContext == null) {
            return;
        }
        String line1 = mContext.getString(R.string.timer_caption);
        String line2 = mContext.getString(R.string.timer_reminder_tile_timer_up);
        String line3;
        if (mTextView1 != null) {
            // setTitle(view, string):
            // Check the title if all letter should be uppercase.
            // by com.htc.util.res.HtcResUtil.isInAllCapsLocale(context)
            setTitle(mTextView1, line1);
        }
        if (mTextView2 != null) {
            mTextView2.setText(line2);
        }
        line3 = DateFormat.getTimeFormat(mContext).format(Calendar.getInstance().getTimeInMillis());
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
            } else if (button == mSetting) {
                if (DEBUG_FLAG) Log.d(TAG, "onButtonDrop: " + mSetting);
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
            return mSetting;
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
        return HtcReminderViewMode.TIMER_MODE;
    }
    
}
