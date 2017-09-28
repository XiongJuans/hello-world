package com.htc.datausagemonitor.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ListView;

import com.htc.datausagemonitor.DataMonitorService;
import com.htc.datausagemonitor.DataSettingActivity;
import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.NetworkStatsHelper;
import com.htc.datausagemonitor.data.SimUtils;
import com.htc.datausagemonitor.floatwindow.FloatWindowService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


/**
 * Created by majing on 17-7-20.
 */

public class DataSettingsFragment extends PreferenceFragment {
    public String TAG = "DataSettingsFragment";

    private SwitchPreference mSwitchMain;
    private SwitchPreference mMonitor;
    private SwitchPreference mFloat;
    private Preference mReminderMain;
    private Preference mOperatorMain;
    private Preference mPackageMain;
    private Preference mChangeUsedDataMain;
    private SwitchPreference mSwitchSub;
    private Preference mReminderSub;
    private Preference mOperatorSub;
    private Preference mPackageSub;
    private Preference mChangeUsedDataSub;
    private PreferenceCategory main;
    private PreferenceCategory sub;

    private Context mContext;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            Method listViewMethod = getClass().getMethod("getListView",  null);
            ListView listView = (ListView) listViewMethod.invoke(this, null);
            listView.setDivider(null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = (Context) getActivity();
        getPreferenceManager().setSharedPreferencesName(Util.SP_NAME_SETTING);
        addPreferencesFromResource(R.xml.data_monitor_setting);

        //spHelper = new Util(mContext);

        mOperatorMain = findPreference("data_operator_main");
        mOperatorSub = findPreference("data_operator_sub");
        mFloat =(SwitchPreference)findPreference("switch_data_float");
        mSwitchMain =(SwitchPreference)findPreference("data_auto_check_main");
        mSwitchSub =(SwitchPreference)findPreference("data_auto_check_sub");
        mMonitor =(SwitchPreference)findPreference("switch_data_monitor");
        main = (PreferenceCategory)findPreference("main_name");
        sub =(PreferenceCategory)findPreference("sub_name");

        updateUI();


    }

    private void updateUI() {
        String mainSubId = NetworkStatsHelper.getSubscriberId(mContext, ConnectivityManager.TYPE_MOBILE,0);
        String subId = NetworkStatsHelper.getSubscriberId(mContext, ConnectivityManager.TYPE_MOBILE,1);
        String name1 = mContext.getResources().getString(R.string.sim_main_name);
        String result= String.format(name1 , SimUtils.getSimOperatorName(mContext,Util.FIRST_SIM_SOLTID));
        main.setTitle(result);
        String name2 = mContext.getResources().getString(R.string.sim_sub_name);
        String result2= String.format(name2 , SimUtils.getSimOperatorName(mContext,Util.SECOND_SIM_SOLTID));
        sub.setTitle(result2);
        Log.d(TAG,"name1 =" +SimUtils.getSimOperatorName(mContext,Util.FIRST_SIM_SOLTID)
                +"name2 =" +SimUtils.getSimOperatorName(mContext,Util.SECOND_SIM_SOLTID));
        updateMonitorSummary();
        updateOperatorSummary(Util.FIRST_SIM_NO);
        updateOperatorSummary(Util.SECOND_SIM_NO);
    }

    private void updateOperatorSummary(int i) {  //设置运营商的Summary
        //Util sp = new Util(getContext(),i);

        ArrayList<String> name =Util.getOperatorName(mContext,i);
        if(!name.get(0).isEmpty() && !name.get(1).isEmpty()){
            String summary = name.get(0) +"  "+name.get(1);
            if(i == 1){
                mSwitchMain.setEnabled(true);
                mSwitchMain.setLayoutResource(R.layout.my_preference_layout);
                mOperatorMain.setSummary(summary);
            }else{
                mSwitchSub.setEnabled(true);
                mSwitchSub.setLayoutResource(R.layout.my_preference_layout);
                mOperatorSub.setSummary(summary);
            }
        }else{
            if(i ==1){
                mSwitchMain.setEnabled(false);
                mSwitchMain.setLayoutResource(R.layout.disable_preference_layout);
            }else{
                mSwitchSub.setEnabled(false);
                mSwitchSub.setLayoutResource(R.layout.disable_preference_layout);
            }
        }
    }

    private void updateMonitorSummary() {
        if(mMonitor.isChecked()){
            mMonitor.setSummary(R.string.switch_data_monitor_turnon);
        }else{
            mMonitor.setSummary(R.string.switch_data_monitor_turnoff);
        }
    }
    @Override
    public void onResume() {

        super.onResume();
        updateUI();
    }

/*    private void refreshOperator(){
        //根据存储的Operator值，在Summary中显示
    }*/

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        Intent intent;
        if(key.equals("switch_data_monitor")){
            if(mMonitor.isChecked()){
                //开启监控（Service）
                Intent startIntent = new Intent(getContext(), DataMonitorService.class);
                getContext().startService(startIntent);
            }else{
                //关闭监控（Service）
                Intent stopIntent = new Intent(getContext(), DataMonitorService.class);
                getContext().stopService(stopIntent);
                Log.d("DataSettingsFragment","stopService");
            }
            updateMonitorSummary();
        } else if(key.equals("data_auto_check_main")){
                if (mSwitchMain.isChecked()) {
                    //发起校正，并记录当前时间
                    Util.setDataAutoCheckMain(mContext,true);
                    Util.setAutoCorrectionTime(mContext,Util.FIRST_SIM_NO,System.currentTimeMillis() / 1000, Util.FIRST_SIM_NO);
                    if(Util.getSimIndex(mContext) == Util.FIRST_SIM_NO) {
                        DataMonitorService.mscheduleAlarms(mContext, Util.FIRST_SIM_NO);
                    }
                } else {
                    //如果流量卡是1的时候开了auto_check,然后切换流量到2,在去关，应该怎么处理(在切换卡的时候判断前一张流量卡是否是开，如果开，在去关掉，不清除时间点
                    // 下次在切换回来的时候继续校正，如果此时用户Check关，则将Time置为0)
                    Util.setDataAutoCheckMain(mContext,false);
                    Util.setAutoCorrectionTime(mContext,1,0,1);
                    if(Util.getSimIndex(mContext) == Util.FIRST_SIM_NO) {
                        DataMonitorService.canclescheduleAlarms(mContext, Util.FIRST_SIM_NO);
                    }
                }

        }else if(key.equals("data_auto_check_sub")) {
            if (mSwitchSub.isChecked()) {
                //发起校正，并记录当前时间
                Util.setDataAutoCheckSub(mContext,true);
                Util.setAutoCorrectionTime(mContext,Util.FIRST_SIM_NO,System.currentTimeMillis() / 1000, Util.SECOND_SIM_NO);
                if (Util.getSimIndex(mContext) == Util.SECOND_SIM_NO){
                    DataMonitorService.mscheduleAlarms(mContext, Util.SECOND_SIM_NO);
                }
            } else {
                Util.setDataAutoCheckSub(mContext,false);
                Util.setAutoCorrectionTime(mContext,Util.FIRST_SIM_NO,0, Util.SECOND_SIM_NO);
                if (Util.getSimIndex(mContext) == Util.SECOND_SIM_NO) {
                    DataMonitorService.canclescheduleAlarms(mContext, Util.SECOND_SIM_NO);
                }
            }
        }else if(key.equals("switch_data_float")) {
            //boolean isFloat = sp.getBoolean("switch_data_float",false);
            if(mFloat.isChecked()) {
                Util.setIsFloatWindowShow(mContext, true);
                Intent startIntent = new Intent(mContext, FloatWindowService.class);
                mContext.startService(startIntent);
            }else{
                Util.setIsFloatWindowShow(mContext, false);
                Intent stopIntent = new Intent(mContext, FloatWindowService.class);
                mContext.stopService(stopIntent);
            }
        }else if(key.equals("change_monthly_usedData_main")){ //月已用流量修改
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "change_usedData");
            intent.putExtra(Util.SIM_NO, Util.FIRST_SIM_NO);
            startActivity(intent);

        }else if(key.equals("change_monthly_usedData_sub")){
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "change_usedData");
            intent.putExtra(Util.SIM_NO, Util.SECOND_SIM_NO);
            startActivity(intent);

        }else if(key.equals("data_reminder_main")){  //流量提醒
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "data_reminder");
            intent.putExtra(Util.SIM_NO, Util.FIRST_SIM_NO); //需要判断是卡1还是卡2；
            startActivity(intent);

        }else if(key.equals("data_package_main")){  //套餐设置
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "set_data_package");
            intent.putExtra(Util.SIM_NO, Util.FIRST_SIM_NO); //需要判断是卡1还是卡2；
            startActivity(intent);

        }else if(key.equals("data_operator_main")){  //运营商设置
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "set_data_operator");
            intent.putExtra(Util.SIM_NO, Util.FIRST_SIM_NO); //需要判断是卡1还是卡2；
            startActivity(intent);

        }else if(key.equals("data_reminder_sub")){
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "data_reminder");
            intent.putExtra(Util.SIM_NO, Util.SECOND_SIM_NO); //需要判断是卡1还是卡2；
            startActivity(intent);

        }else if(key.equals("data_package_sub")){
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "set_data_package");
            intent.putExtra(Util.SIM_NO, Util.SECOND_SIM_NO); //需要判断是卡1还是卡2；
            startActivity(intent);

        }else if(key.equals("data_operator_sub")){
            intent = new Intent(getActivity(), DataSettingActivity.class);
            intent.putExtra(Util.FRAG_NAME, "set_data_operator");
            intent.putExtra(Util.SIM_NO, Util.SECOND_SIM_NO); //需要判断是卡1还是卡2；
            startActivity(intent);

        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
