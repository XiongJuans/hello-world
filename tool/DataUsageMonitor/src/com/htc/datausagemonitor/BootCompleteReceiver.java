package com.htc.datausagemonitor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.htc.datausagemonitor.data.SimUtils;
import com.htc.datausagemonitor.floatwindow.FloatWindowService;

public class BootCompleteReceiver extends BroadcastReceiver {
    //开机启动Service
    private final static String TAG = BootCompleteReceiver.class.getSimpleName();
    private final static String ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED
            = "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED";
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private final static String ACTION_ESIM_STATE_CHANGED = "com.htc.intent.action.ESIM_STATE_CHANGED";

    private final static String PACKAGE_NAME_SECURITYCENTER = "com.htc.securitycenter";
    private final static String PACKAGE_NAME_SECURITYCENTER_NAME = "package:com.htc.securitycenter";
    private final static String ACTION_APP_UNINSTALLED = "android.intent.action.PACKAGE_REMOVED";
    private final static String ACTION_APP_INSTALLED = "android.intent.action.PACKAGE_ADDED";
    private final static String SUBSCRIPTION_KEY = "subscription";

    private final static String STATE_KEY = "state";
    private final static String SLOT_KEY  = "slot";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //start the DataMonitorService
        //Util sp = new Util(context);
        String action =intent.getAction();
        Log.d(TAG, "BootCompleteReceiver action = " + action);
        if(action.equals("android.intent.action.BOOT_COMPLETED")) {
            if(appInstalledOrNot(context, PACKAGE_NAME_SECURITYCENTER)) {  //安全助手有安装的情况下, 开启流量监控服务
                boolean monitor = Util.getIsDataMonitor(context);
                if (monitor) {
                    Intent startIntent = new Intent(context, DataMonitorService.class);
                    context.startService(startIntent);
                    Log.d(TAG, "start DataMonitorService");
                }
                boolean isShowFloatWin = Util.IsFloatWindowShow(context);
                if (isShowFloatWin == true) {   //start float window
                    Intent startIntent = new Intent(context, FloatWindowService.class);
                    context.startService(startIntent);
                }
            }
        }else if(action.equals(ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)){
            /*
             *  sim1: slotId = 0, subId = 1
             *  sim2: slotId = 1, subId = 2
             */
            int simIndex = 1;
            boolean isSim1Ready = SimUtils.getSimStateBySlotIdx(context, Util.FIRST_SIM_SOLTID); // check sim1 is ready?
            boolean isSim2Ready = SimUtils.getSimStateBySlotIdx(context, Util.SECOND_SIM_SOLTID);//check sim2 is ready?
            int simSubId = intent.getIntExtra(SUBSCRIPTION_KEY,Util.FIRST_SIM_NO);  //数据网络SubId // subid =1
            Log.i(TAG," Data SIM Index =" + simSubId);
            if(!isSim1Ready && isSim2Ready){  //TBD why?,if sim1 is no sim ,and sim2 have sim ,need change simSubId to 2
               // simSubId =Util.SECOND_SIM_NO;
            }
            Log.d(TAG,"getSoltId =" + SimUtils.getSlotIdFromSubid(context,simSubId));
            if(SimUtils.getSlotIdFromSubid(context,simSubId) == 0){
                simIndex = 1;
            }else if(SimUtils.getSlotIdFromSubid(context,simSubId) == 1){
                simIndex = 2;
            }

            Util.setSimIndex(context, simIndex);
            //收到切卡消息去处理
            if(simIndex == Util.FIRST_SIM_NO){ //切换到卡1
                if(Util.getAutoCorrectionTime(context, Util.FIRST_SIM_NO, Util.SECOND_SIM_NO)>0){
                    DataMonitorService.canclescheduleAlarms(context, Util.SECOND_SIM_NO);
                }
                if(Util.getAutoCorrectionTime(context, Util.FIRST_SIM_NO,Util.FIRST_SIM_NO)>0){
                    DataMonitorService.mscheduleAlarms(context, Util.FIRST_SIM_NO);
                }
            }else if(simIndex == Util.SECOND_SIM_NO){ //切换到卡2
                if(Util.getAutoCorrectionTime(context, Util.FIRST_SIM_NO,Util.FIRST_SIM_NO)>0){
                    DataMonitorService.canclescheduleAlarms(context, Util.FIRST_SIM_NO);
                }
                if(Util.getAutoCorrectionTime(context, Util.FIRST_SIM_NO,Util.SECOND_SIM_NO)>0){
                    DataMonitorService.mscheduleAlarms(context, Util.SECOND_SIM_NO);
                }
            }
          //  NetworkStatsHelper.updateUtil(context, ConnectivityManager.TYPE_MOBILE);

            Log.i(TAG," Data SIM Index =" + Util.getSimIndex(context));

        }else if(action.equals(ACTION_SIM_STATE_CHANGED)){  //插拔SIM卡发送广播
            TelephonyManager tm = (TelephonyManager)context.getSystemService(Service.TELEPHONY_SERVICE);
            int simState = tm.getSimState();
            Log.d(TAG, "ACTION_SIM_STATE_CHANGED:" + simState);
            if(simState == TelephonyManager.SIM_STATE_READY){
                Util.setIsSimStateChanage(context, true);
            }
        }else if(action.equals(ACTION_APP_UNINSTALLED)){
            String packageName = intent.getDataString();
            Log.d(TAG, "ACTION_APP_UNINSTALLED packageName = " + packageName); //package:com.htc.securitycenter
            if(packageName.equals(PACKAGE_NAME_SECURITYCENTER_NAME)){
                //安全助手卸载之后,stop DataMonitorService
                Intent stopIntent = new Intent(context, DataMonitorService.class);
                context.stopService(stopIntent);
                //安全助手卸载之后,stop float window service
                boolean isShowFlotWin = Util.IsFloatWindowShow(context);
                Log.d(TAG, "ACTION_APP_UNINSTALLED isShowFlotWin = " + isShowFlotWin);
                if (isShowFlotWin) {
                    Intent stopFloatWinIntent = new Intent(context, FloatWindowService.class);
                    context.stopService(stopFloatWinIntent);
                    Util.setIsFloatWindowShow(context, false);
                }
            }
        }else if(action.equals(ACTION_APP_INSTALLED)){
            String packageName = intent.getDataString();
            if(packageName.equals(PACKAGE_NAME_SECURITYCENTER_NAME)){
                //安全助手安装之后,start DataMonitorService
                if(Util.getIsDataMonitor(context)) {
                    Intent startIntent = new Intent(context, DataMonitorService.class);
                    context.startService(startIntent);
                }
            }
        }else if(action.equals(ACTION_ESIM_STATE_CHANGED)){  //esim被激活请
            String eSimState = intent.getStringExtra(STATE_KEY);  // "0": isabled; "1":enabled  STATE_KEY = "state";
            int slotId = intent.getIntExtra(SLOT_KEY, 0);
            int subId = intent.getIntExtra(SUBSCRIPTION_KEY, -1);
            Log.i(TAG, "esim state:" + eSimState + " slot:" + slotId + " subId:" + subId);
            //Util eSp = new Util(context,slotId + 1);
            //eSp.setIsEsimEnable(context, subId, eSimState.equals("1"));
            Util.setIsEsimEnable(context, slotId+1, eSimState.equals("1"));
        }
    }

    private boolean appInstalledOrNot(Context context,String uri) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed ;
    }
}
