package com.htc.datausagemonitor.fragment;

import android.app.usage.NetworkStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.NetworkStatsHelper;

import static android.content.Context.NETWORK_STATS_SERVICE;


/**
 * Created by majing on 17-7-22.
 */

public class SettingBaseFragment extends Fragment implements View.OnClickListener{

    private FrameLayout frameLayout;
    private Button netControl;
    private TextView todayUsed;
    private TextView yesterdayUsed;
    private TextView befYesterdayUsed;
    private NetworkStatsHelper helper;
    private NetworkStatsManager networkStatsManager;
    private Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        context = getContext();
        Bundle bl = getArguments();
        view =inflater.inflate(R.layout.main_bottom_half,container,false);

        netControl = (Button)view.findViewById(R.id.net_control);

        todayUsed = (TextView)view.findViewById(R.id.today_used_summary);
        yesterdayUsed = (TextView)view.findViewById(R.id.yesterday_used_summary);
        befYesterdayUsed = (TextView)view.findViewById(R.id.bef_yesterday_used_summary);
        netControl.setOnClickListener(this);
        //sp = new Util(context);
        networkStatsManager = (NetworkStatsManager)context.getSystemService(NETWORK_STATS_SERVICE);
        helper = new NetworkStatsHelper(networkStatsManager, context);
        if(Util.isSimStateChange(context)){
            Toast.makeText(context, R.string.sim_change_summary, Toast.LENGTH_LONG).show();
        }
        //setData();
        return view;
    }


    public void setData(int simIndex) {
        //int simIndex = Util.getSimIndex(context);
        long today = helper.getAllTodayMobile(context,simIndex);
        long yesterday = helper.getYesterdayMobile(context,simIndex);
        long bef = helper.getBeforeYesterdayMobile(context,simIndex);
        Log.d("SettingBaseFragment","simIndex :" + simIndex +"today :" +today);
        todayUsed.setText(NetworkStatsHelper.byteToMB(today));

        yesterdayUsed.setText(NetworkStatsHelper.byteToMB(yesterday));
        befYesterdayUsed.setText(NetworkStatsHelper.byteToMB(bef));


        updateNetworkControl();

    }

    @Override
    public void onResume() {
        super.onResume();
        updateNetworkControl();
    }

    private void updateNetworkControl(){
        if(Util.getIsDataMonitor(context)){
            netControl.setAlpha((float)1);
            netControl.setEnabled(true);
            //netControl.setOnClickListener(this);
        }else{
            netControl.setAlpha((float)0.5);
            netControl.setEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        //联网控制的Click, 替换包名和Activity名字,TODO 目前点击会有问题，不能跳转，等该Activity设置好再进行测试

        Intent intent = new Intent();
        intent.setAction("com.htc.securitycenter.redirector.ACTION_FIREWALL");

        startActivityForResult(intent, 0);
    }
}
