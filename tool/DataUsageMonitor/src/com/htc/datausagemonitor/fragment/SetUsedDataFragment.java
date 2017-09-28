package com.htc.datausagemonitor.fragment;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.NetworkStatsHelper;


/**
 * Created by majing on 17-7-24.
 */

public class SetUsedDataFragment extends Fragment implements View.OnClickListener{
    public String TAG ="SetUsedDataFragment";
    private EditText mPackageUsed;
    private EditText mIdleUsed;
    private TextView mComplete;

    private SharedPreferences.Editor editor;
    private Bundle bl;
    private Context mContext;
    private int mSimIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_used_data_change,container,false);
        bl =getArguments();
        mSimIndex = bl.getInt(Util.SIM_NO, Util.FIRST_SIM_NO);
        //sp = new Util(getContext(),sim);
        long sr1 = Util.getDataUsed(mContext, mSimIndex);  //监控去写的值

        mPackageUsed =(EditText)rootView.findViewById(R.id.set_package_used);
        mIdleUsed = (EditText)rootView.findViewById(R.id.set_night_used);
        mComplete = (TextView) rootView.findViewById(R.id.button_complete);
        Log.d(TAG,"monitorData dataUsed get：" +sr1);
        mPackageUsed.setText(NetworkStatsHelper.dataBTOString(sr1));

        mComplete.setOnClickListener(this);
        updateUI();
        return rootView;
    }

    private void updateUI() {
        if(Util.getIdlePackage(mContext, mSimIndex).isEmpty()){
            mIdleUsed.setEnabled(false);
        }else{
            mIdleUsed.setEnabled(true);
            long sr2 = Util.getIdleDataUsed(mContext, mSimIndex);
            mIdleUsed.setText(NetworkStatsHelper.dataBTOString(sr2));
        }
    }

    @Override
    public void onResume() {
        updateUI();
        super.onResume();
    }

    //用户设置已用流量
    @Override
    public void onClick(View view) {
        Util.setPackageUsed(mContext, mSimIndex,NetworkStatsHelper.dataMBTOB(mPackageUsed.getText().toString()));
        Util.setIdleUsed(mContext, mSimIndex, NetworkStatsHelper.dataMBTOB(mIdleUsed.getText().toString()));
        Util.setCorrectIdleData(mContext, mSimIndex, NetworkStatsHelper.dataMBTOB(mIdleUsed.getText().toString()));//用户设置的已用闲时流量作为计算套餐已用流量的基数

        Log.d(TAG,"set: packageUsed =" +Util.getPackageUsed(mContext, mSimIndex) + "idleUsed =" +Util.getIdleUsed(mContext, mSimIndex));
        Util.setDataChangeTime(mContext, mSimIndex, System.currentTimeMillis());
        getActivity().finish();
    }
}
