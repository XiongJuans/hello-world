package com.htc.datausagemonitor.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;


import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;

import static android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;
import static android.app.AlertDialog.THEME_HOLO_LIGHT;
import static android.app.AlertDialog.THEME_TRADITIONAL;

/**
 * Created by mj on 2017/7/23.
 */

public class SetPackageFragment extends Fragment implements View.OnClickListener{
    private TextView mNext;
    private Button mStartDate;
    private Button mStartTime;
    private Button mEndTime;
    private EditText mPackage;
    private EditText mIdle;
    private TextView mIdlePackage;
    //Util sp;
    private Context mContext;
    private Bundle bl;
    private boolean wizard;
    private int simNo;
    private int startDay;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        //套餐和起始日设置
        //应该需要区分卡1还是卡2
        mContext = (Context)getActivity();
        bl = getArguments();
        simNo  = bl.getInt(Util.SIM_NO,Util.FIRST_SIM_NO);
        //sp = new Util(getContext(),simNo);
        View rootView = inflater.inflate(R.layout.modify_monthly_used_traffic,container,false);
        mNext = (TextView)rootView.findViewById(R.id.next_step);
        mPackage = (EditText)rootView.findViewById(R.id.edit_package);
        mStartDate = (Button)rootView.findViewById(R.id.start_date);
        mIdlePackage =(TextView)rootView.findViewById(R.id.idle_package);
        mIdle = (EditText)rootView.findViewById(R.id.edit_idle_package);
        mStartTime = (Button)rootView.findViewById(R.id.start_time);
        mEndTime = (Button)rootView.findViewById(R.id.end_time);
        initView();


        return rootView;
    }

    private void initView(){
        bl = getArguments();
        wizard = bl.getBoolean(Util.IS_WIZARD);

        mNext.setOnClickListener(this);
        mStartDate.setOnClickListener(this);
        mStartTime.setOnClickListener(this);
        mEndTime.setOnClickListener(this);
        //mIdlePackage.setOnClickListener(this); //如果有需要可以添加点击事件
        String dataPackage = Util.getUserPackage(mContext, simNo);
        String idlePackage = Util.getIdlePackage(mContext, simNo);
        startDay = Util.getStartDate(mContext, simNo);
        //mStartDate.setText(String.valueOf(startDay));
        int[] startTime = Util.getStartTime(mContext, simNo);
        startHour = startTime[0];
        startMinute = startTime[1];
        int[] endTime = Util.getEndTime(mContext, simNo);
        endHour = endTime[0];
        endMinute =endTime[1];

        Log.d("SetPakageFragment","isWizard ="+ wizard);
        if(wizard) {
            mNext.setText(R.string.next_step);
            Log.d("SetPackageFragment","idlePackageFragment test");
        }else{
            mNext.setText(R.string.ok);
        }
        updatePackage(dataPackage,startDay,idlePackage);
        //根据储存的值，要显示Button上的数据,参数日，开始时间，结束时间
        updateButton(startHour,startMinute,true);
        updateButton(endHour,endMinute,false);
    }

    private void updatePackage(String userPackage, int day, String idlePackage){
        if(userPackage.isEmpty()) {
           userPackage = "1024";
        }
        mPackage.setText(userPackage);
        mStartDate.setText(String.valueOf(day));
        mIdle.setText(idlePackage);
        Log.d("Test","startDate =" + String.valueOf(day) + "day =" +day);
    }

    private void updateButton(int hour,int minute,boolean start){
        if(minute < 10) {
            if(start) {
                mStartTime.setText(hour + ":" + 0 + minute);
            }else{
                mEndTime.setText(hour + ":" + 0 + minute);
            }
        }else{
            if(start) {
                mStartTime.setText(hour + ":" + minute);
            }else{
                mEndTime.setText(hour + ":" + minute);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.next_step:
            {
                //跳转到省市运营商设置
                if(wizard) {
                    SetOperator();
                }else{
                    SetComplete();
                }
                break;
            }
            case R.id.start_date:
            {
                //设置起始日
                showDateDialog();
                break;
            }
            case R.id.start_time:
            {
                //设置闲时流量的开始时间
                showTimeDialog(true);
                break;
            }
            case R.id.end_time:
            {
                //设置闲时流量的结束时间
                showTimeDialog(false);
                break;
            }
        }
    }

    public void showDateDialog(){
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View layout = layoutInflater.inflate(R.layout.number_picker_layout,null);

        final NumberPicker numberPicker = (NumberPicker)layout.findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(31);
        numberPicker.setMinValue(1);
        numberPicker.setValue(startDay);  //设置弹出框的值
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        //builder.setTitle(R.string.set_start_date);
        builder.setView(layout);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //用户选择数据显示在Button上
                startDay = numberPicker.getValue();
                mStartDate.setText(String.valueOf(numberPicker.getValue()));
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.create();
        builder.show();
    }

    private void showTimeDialog(boolean start){
        TimePickerDialog timePickerDialog;
        if(start) {
            timePickerDialog = new TimePickerDialog(getActivity(),new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int i, int i1) {
                    startHour = i;
                    startMinute =i1;
                    updateButton(i,i1,true);
                }
            }, startHour,startMinute,true);
            //
        }else{
            timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int i, int i1) {
                    endHour = i;
                    endMinute =i1;
                    updateButton(i,i1,false);
                }
            }, endHour,endMinute,true);
        }

        timePickerDialog.show();
    }

    private void SetOperator(){
        Util.setUserPackage(mContext, simNo, mPackage.getText().toString());
        Util.setStartDate(mContext, simNo, startDay);

        if (!(mIdle.getText().toString().isEmpty())) {
            Util.setIdlePackage(mContext, simNo, mIdle.getText().toString());
            Util.setStartTime(mContext, simNo,startHour,startMinute);
            Util.setEndTime(mContext, simNo, endHour,endMinute);
        }
        getFragmentManager().beginTransaction().hide(this).commit();
        SetOperatorFragment fragment = new SetOperatorFragment();
        fragment.setArguments(bl);
        getFragmentManager().beginTransaction().add(R.id.sub_content,fragment,null).commit();
    }

    private void SetComplete(){
        //保存数据,可能会有值的比较逻辑
        Util.setUserPackage(mContext, simNo, mPackage.getText().toString());
        Util.setStartDate(mContext, simNo, startDay);

//        if (!(mIdle.getText().toString().isEmpty())) {
        Util.setIdlePackage(mContext, simNo,mIdle.getText().toString());
        Util.setStartTime(mContext, simNo,startHour,startMinute);
        Util.setEndTime(mContext, simNo,endHour,endMinute);
//        }
        Util.setDataChangeTime(mContext, simNo,System.currentTimeMillis());
        clearData();
        getActivity().finish();
    }

    private void clearData() {
        Util.setIdleUsed(mContext, simNo,0);
        Util.setPackageUsed(mContext, simNo, 0);
    }
}
