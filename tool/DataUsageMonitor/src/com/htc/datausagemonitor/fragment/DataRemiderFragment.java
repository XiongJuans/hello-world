package com.htc.datausagemonitor.fragment;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.NetworkStatsHelper;


public class DataRemiderFragment extends PreferenceFragment implements View.OnClickListener {
    //流量提醒UI

    private Switch mReminder;
    private Switch mAutoTurnoff;
    private Switch mMonthlyReminder;
    private EditText mMonthly;
    private Switch mDailyReminder;
    private EditText mDaily;
    private TextView mComplete;
    private Bundle bl;

    private Context mContext;
    private int simIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //addPreferencesFromResource(R.xml.data_reminder);
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.data_reminder_fragment,container,false);
        mReminder = (Switch)rootView.findViewById(R.id.data_reminder);
        mAutoTurnoff = (Switch)rootView.findViewById(R.id.auto_turnoff_network);
        mMonthlyReminder = (Switch) rootView.findViewById(R.id.monthly_reminder);
        mMonthly = (EditText)rootView.findViewById(R.id.monthly_reminder_set);
        mDailyReminder = (Switch)rootView.findViewById(R.id.daily_reminder);
        mDaily = (EditText)rootView.findViewById(R.id.daily_reminder_set);
        mComplete = (TextView)rootView.findViewById(R.id.reminder_complete);
        mComplete.setOnClickListener(this);
        //mMonthly.setFocusable(false);
        //mDaily.setFocusable(false);
        mMonthlyReminder.setOnClickListener(this);
        mMonthly.setOnClickListener(this);
        mDailyReminder.setOnClickListener(this);
        mDaily.setOnClickListener(this);
        initFragment();
        return rootView;
    }

    private void initFragment(){
        bl = getArguments();
        simIndex = bl.getInt(Util.SIM_NO,Util.FIRST_SIM_NO);
        //sp = new Util(getContext(),sim);
        Log.d("DataRemiderFragment","mMonthlyReminder" +Util.getMonthlyReminder(mContext, simIndex));
        mReminder.setChecked(Util.getDataReminder(mContext, simIndex));
        mAutoTurnoff.setChecked(Util.getAutoTurnoffNet(mContext, simIndex));
        Log.d("DataRemiderFragment", "mMonthlyReminder" + Util.getDailyReminder(mContext, simIndex));
        mMonthlyReminder.setChecked(Util.getMonthlyReminder(mContext, simIndex));
        Log.d("DataRemiderFragment","mMonthlyReminder" +Util.getDailyReminder(mContext, simIndex));

        if(Util.getMonthlyReminderData(mContext, simIndex) != 0){
            String monthly = NetworkStatsHelper.dataBTOString(Util.getMonthlyReminderData(mContext, simIndex));
            mMonthly.setText(monthly);
        }else{
            mMonthly.setHint(R.string.set_monthly_remaining_reminder);
        }
        mDailyReminder.setChecked(Util.getDailyReminder(mContext, simIndex));

       if(Util.getDailyReminderData(mContext, simIndex) != 0){
            String daily = NetworkStatsHelper.dataBTOString( Util.getDailyReminderData(mContext, simIndex));
            mDaily.setText(daily);
        }else{
            mDaily.setHint(R.string.set_daily_used_reminder);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //如果月流量提醒/日已用流量提醒开，但对应的值没有输入，完成按钮不可用
        if((mMonthlyReminder.isChecked() &&(mMonthly.getText().length()== 0)) ||
                (mDailyReminder.isChecked() && (mDaily.getText().length() == 0)) ) {
            mComplete.setEnabled(false);
        }else{
            mComplete.setEnabled(true);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.monthly_reminder:
            {
                if(mMonthlyReminder.isChecked() && mMonthly.getText().length()== 0){
                    mComplete.setEnabled(false);
                    mMonthlyReminder.setChecked(false);
                }else{
                    mComplete.setEnabled(true);
                }
                break;
            }
            case R.id.monthly_reminder_set:
            {
                mMonthly.setText("");
                mMonthly.requestFocus();
                break;
            }
            case R.id.daily_reminder:
            {
                if(mDailyReminder.isChecked() && mDaily.getText().length() == 0){
                    mComplete.setEnabled(false);
                    mDailyReminder.setChecked(false);
                }else{
                    mComplete.setEnabled(true);
                }
                break;
            }
            case R.id.daily_reminder_set:
            {
                mDaily.setText("");
                mDaily.requestFocus();
                break;
            }
            case R.id.reminder_complete:
            {
                setComplete();
                getActivity().finish();
                break;
            }
        }
    }

    private void setComplete(){
       // SharedPreferences.Editor editor = sp.edit();
        Log.d("DataRemiderFragment","setComplete mDailyReminder.isChecked(): "+mDailyReminder.isChecked());
        Log.d("DataRemiderFragment","setComplete mReminder.isChecked(): "+mReminder.isChecked());
        Log.d("DataRemiderFragment", "setComplete mReminder.isChecked(): " + mAutoTurnoff.isChecked());
        //int simIndex = Util.getSimIndex(mContext);
        Util.setDataReminder(mContext, simIndex, mReminder.isChecked());
        Util.setAutoTurnoffNet(mContext, simIndex,mAutoTurnoff.isChecked());

        Util.setMonthlyReminder(mContext, simIndex,mMonthlyReminder.isChecked());
        Util.setMonthlyReminderData(mContext, simIndex,NetworkStatsHelper.dataMBTOB(mMonthly.getText().toString()));
        Util.setDailyReminder(mContext, simIndex,mDailyReminder.isChecked());
        Util.setDailyReminderData(mContext, simIndex,NetworkStatsHelper.dataMBTOB(mDaily.getText().toString()));
    }
}
