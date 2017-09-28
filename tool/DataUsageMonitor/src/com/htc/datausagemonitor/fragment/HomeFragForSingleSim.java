package com.htc.datausagemonitor.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.DataWatcher;
import com.htc.datausagemonitor.data.MonitorData;
import com.htc.datausagemonitor.data.MonitorDataChange;

import java.util.Observable;

/**
 * Created by majing on 17-8-14.
 */

public class HomeFragForSingleSim  extends Fragment {
    private MainSimFragment fragment;
    private TextView mEmptyText;
    private SharedPreferences dataSetting;
    private DataWatcher dataWatcher;

    private Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.single_sim_home_fragment,container,false);
        //sp = new Util(getContext());
        mEmptyText = (TextView)view.findViewById(R.id.text_monitor);
        fragment= new MainSimFragment();
        getChildFragmentManager().beginTransaction().replace(R.id.main_sim_fragment, fragment).commit();
        refreshText();

        dataWatcher = new DataWatcher(){
            @Override
            public void update(Observable observable, Object data) {
                super.update(observable, data);
                //观察者接受到被观察者的通知，来更新自己的数据操作。
                MonitorData mData = (MonitorData) data;
                fragment.updateUI();

                Log.i("Test", "mData---->>"+mData.getUsedDataChange());
            }
        };
        return view;
    }

    public void refreshText(){ //根据是否开始实时监控，显示不同的String
        boolean isMonitor = dataSetting.getBoolean(Util.DATA_MONITOR,true);
        if(isMonitor){
            mEmptyText.setText(R.string.data_monitor_on_summary);
        }else{
            mEmptyText.setText(R.string.data_monitor_off_summary);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshText();
        if(Util.getIsDataMonitor(mContext)) {
            MonitorDataChange.getInstance().addObserver(dataWatcher);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(Util.getIsDataMonitor(mContext)) {
            MonitorDataChange.getInstance().deleteObserver(dataWatcher);
        }
    }
}
