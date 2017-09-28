package com.htc.datausagemonitor;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.htc.datausagemonitor.data.MonitorData;
import com.htc.datausagemonitor.data.MonitorDataChange;
import com.htc.datausagemonitor.data.NetworkStatsHelper;
import com.htc.datausagemonitor.data.SimUtils;
import com.htc.datausagemonitor.trafficcorrection.TrafficCorrectionWrapper;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tmsdk.bg.module.network.ITrafficCorrectionListener;
import tmsdk.bg.module.network.ProfileInfo;
import tmsdk.common.ErrorCode;
import tmsdk.common.IDualPhoneInfoFetcher;

import static android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;

public class DataMonitorService extends Service {

    public static final String TAG = DataMonitorService.class.getSimpleName();
    private NetworkStatsManager networkStatsManager;
    private NetworkStatsHelper networkStatsHelper;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private  int mSimIndex=0;
    private TimerTask mTimerTask;
    private String mQueryCode = "";
    private String mQueryPort = "";
    private String mQueryCode1 = "";
    private String mQueryPort1 = "";
    private String mQueryCode2 = "";
    private String mQueryPort2 = "";

    private static Timer timer;
    private long idleUsed1;
    private long idleResidue1;
    private long packageUsed1;
    private long packageResidue1;

    private long idleUsed2;
    private long idleResidue2;
    private long packageUsed2;
    private long packageResidue2;
    private boolean isClearData = false;
    private boolean isClearData1 = false;
    private boolean isClearData2 = false;
    private boolean isAutoTurnOffNetwork =false;
    private boolean dataOverDialog = true;
    private boolean dataResidueDialog = true;
    private boolean dataDailyDialog = true;
    private int newDayHour =0; //判断是不是新的一天
    private boolean serviceRunning = false;
    private int testid=0;
    private int progressPackage =0;  //Progress的数值
    private int progreessIdle =0;

    String progressMainString;  //流量值显示数据保存
    String progressIdleString;


    /**
     * 校正成功提示
     */
    private final int MSG_TRAFfICT_NOTIFY = 0x3a;
    private final int MSG_NEED_SEND_MSG = 0x3b;
    private final int MSG_NEED_ALARTDIALOG = 0x3d;
    private final int MSG_CORRECTION_RIGHT = 0x3e;
    private final int MSG_SHOW_NOTIFICATION = 0x3f;

    /***
     * 需要发送短信
     */

    static int Interval=60*60*24*1000;

    private Context mContext;

    public static void mscheduleAlarms(Context ctxt, int simIndex) {  //自动定时校正

        Log.d(TAG,"进入mscheduleAlarms");
        AlarmManager mgr = (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
        Bundle bundle = new Bundle();
        bundle.putInt("simIndex",simIndex);
       // Intent i = new Intent(ctxt, DataMonitorService.class);
        Intent i = new Intent();
        i.setAction("auto_check");
        i.putExtras(bundle);
        PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);  //没有及时收到广播，去做事情
        mgr.cancel(pi);
        long time = System.currentTimeMillis() + 100;
        mgr.setRepeating(AlarmManager.RTC_WAKEUP, time,
                Interval, pi);
    }
    public static void canclescheduleAlarms(Context ctxt, int simIndex) {
        AlarmManager mgr = (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent();
        i.setAction("auto_check");
        PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);
        mgr.cancel(pi);
    }


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_NEED_SEND_MSG :{
                    Bundle bundle = msg.getData();
                    int i = bundle.getInt("simIndex");
                    String s = bundle.getString("queryCode");
                    String s1 = bundle.getString("queryPort");
                    sendSMS(s1,s,i);
                    break;
                }
                case MSG_TRAFfICT_NOTIFY: {  //处理返回的数据
                    String logTemp = (String) msg.obj;
                    Log.d(TAG, "MSG_TRAFfICT_NOTIFY");
                    if (IDualPhoneInfoFetcher.FIRST_SIM_INDEX == msg.arg1) {  //根据不同流量存储，用B来保存
//                        mTVSim1Detail.setText(mTVSim1Detail.getText() + logTemp);
                    } else if (IDualPhoneInfoFetcher.SECOND_SIM_INDEX == msg.arg1) {
//                        mTVSim2Detail.setText(mTVSim2Detail.getText() + logTemp);
                    }
                    break;
                }
                case MSG_CORRECTION_RIGHT: {  //
                    Bundle bundle = msg.getData();
                    boolean isRight = bundle.getBoolean("correct_right");
                    if (isRight) {
                        Toast.makeText(getApplicationContext(), R.string.data_correct_success, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.data_correct_error, Toast.LENGTH_LONG).show();
                    }
                    break;
                }
                case MSG_NEED_ALARTDIALOG: {
                    Bundle bundle = msg.getData();
                    int index = bundle.getInt("index");
                    long data = bundle.getLong("data");
                    showDialog(index,data);
                    break;
                }
                case MSG_SHOW_NOTIFICATION: {
                    sendNotification();
                    break;
                }

            }
        }
    };
    private BroadcastReceiver SMSReceiver=new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            Log.d(TAG,"BroadcastReceiver--in");
            if(action.equals("android.provider.Telephony.SMS_RECEIVED")){
                // an Intent broadcast.
                SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
//                SmsMessage msg = ;
//                if (null != bundle) {
//                    Object[] smsObj = (Object[]) bundle.get("pdus");
                    String format = intent.getStringExtra("format");
                    for (int i = 0; i < msgs.length; i++) {
                        //时间
                        SmsMessage sms = msgs[i];
                        System.out.println("number:" + sms.getOriginatingAddress()
                                + "   body:" + sms.getDisplayMessageBody() + "  time:"
                                + sms.getTimestampMillis());
                        int id = SimUtils.getSlotIdFromSubid(context,getMsgSubId(sms));
                        Log.d(TAG,"smgSubId =" +id);
                        String addr = sms.getOriginatingAddress();
                        if (NetworkStatsHelper.isDualSimEnable(mContext) == 3) {
                            //如果是插双卡这里要单独处理
                            if(id == 0){
                               if(addr.equals(mQueryPort1)){
                                   TrafficCorrectionWrapper.getInstance().analysisSMS(Util.FIRST_SIM_SOLTID,
                                           mQueryCode1,
                                           mQueryPort1,
                                           sms.getDisplayMessageBody());
                               }
                                Log.d(TAG,"analysisSMS QueryCode1 =" +mQueryCode1 +"mQueryPort1 =" +mQueryPort1);
                            }else if(id ==1){
                                if(addr.equals(mQueryPort2)) {
                                    TrafficCorrectionWrapper.getInstance().analysisSMS(Util.SECOND_SIM_SOLTID,  //这里用全局变量，双卡可能有问题
                                            mQueryCode2,
                                            mQueryPort2,
                                            sms.getDisplayMessageBody());
                                    Log.d(TAG,"analysisSMS QueryCode2 =" +mQueryCode2 +"mQueryPort2 =" +mQueryPort2);
                                }
                            }
                        } else {
                            //在这里写自己的逻辑
                            if (addr.equals(mQueryPort)) { //mQueryPort
                                //TODO
                                Log.i(TAG, "msg receive");
                                Log.d(TAG, sms.getDisplayMessageBody());
                                TrafficCorrectionWrapper.getInstance().analysisSMS(mSimIndex,  //这里用全局变量，双卡可能有问题
                                        mQueryCode,
                                        mQueryPort,
                                        sms.getDisplayMessageBody());
                                Log.i(TAG, "msg receive end");
                                //Toast.makeText(context,msg.getDisplayMessageBody(),Toast.LENGTH_LONG).show();
                            }
                        }
                    //}
                }
            }else if(action.equals("auto_check")){  //重新打开应用会收到这个广播
                //每天自动校验checked
                Log.i(TAG,"receive auto correct check");
                Bundle bundle = intent.getExtras();
                int simIndex=bundle.getInt("simIndex");
                if (null != bundle) {
                    if(Util.getIsEsimEnable(mContext, simIndex) || NetworkStatsHelper.isVirtureSim(mContext, simIndex)) {
                        Log.i(TAG,"isEsim or virSIM");
                    }else {
                        //Toast.makeText(getApplicationContext(), R.string.start_data_correct, Toast.LENGTH_SHORT).show();
                        int retCode = TrafficCorrectionWrapper.getInstance().startCorrection(simIndex - 1); //这里获取的SIM是1或2，TMSDK需要的是0或1
                        Log.d(TAG, "sim =" + simIndex);
                        if (retCode != ErrorCode.ERR_NONE) {
                        /*Toast.makeText(context, " : ", Toast.LENGTH_LONG).show();*/
                            //TODO 这里时间还有点问题
                            Util.setAutoCorrectionTime(context, Util.FIRST_SIM_NO, System.currentTimeMillis() / 1000, simIndex);
                        }
                    }
                }

            }else if(action.equals("auto_check_cancel")){
                //每天自动校验关
            }
        }
    };

    private int getMsgSubId(SmsMessage msg){
        int subid =0;
        try {
            Method getSubIdMethod = msg.getClass().getDeclaredMethod("getSubId", (Class[]) null);
            if(getSubIdMethod != null){
                subid = (int)getSubIdMethod.invoke(msg, (Object[]) null);
            }
        }catch (Exception ex) {
            Log.e(TAG, "Error setting mobile data state", ex);
        }
        return subid;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long period = 5000;
        int delay=0;//intent.getIntExtra("delayTime",0);
        if(timer == null){
            timer = new Timer();
        }
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                monitorData(Util.FIRST_SIM_NO);
                monitorData(Util.SECOND_SIM_NO);
                int simIndex = Util.getSimIndex(mContext);
                //五秒更新一次
                MonitorData data = new MonitorData();
                data.setDataChange(Util.getDataUsed(mContext, simIndex), Util.getPackageResidue(mContext, simIndex),
                        Util.getPackageOver(mContext, simIndex), Util.getIdleDataUsed(mContext, simIndex),
                        Util.getIdleResidue(mContext, simIndex));
                MonitorDataChange.getInstance().notifyDataChange(data);
            }
        };
        timer.schedule(mTimerTask,delay,period);
        return super.onStartCommand(intent, flags, startId);
    }

    private void monitorData(int simIndex){
        //判断闲时流量包是否为空
        //int simIndex = Util.getSimIndex(mContext);


        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        if(newDayHour < hour){
            dataDailyDialog = true;
        }
        newDayHour = hour;  //新的一天设置为true,来监测新的一天
        //sp = new Util(this,sim);
        int start =Util.getStartDate(mContext, simIndex);

        if(simIndex == Util.FIRST_SIM_NO){
            isClearData = isClearData1;
        }else{
            isClearData = isClearData2;
        }
        if(day == start && !isClearData){  //如果当前日期是StartDate的日期，则去清空流量数据
            //清空数据
            clearData(simIndex);
            if(simIndex == Util.FIRST_SIM_NO) {
                isClearData1 = true;
            }else{
                isClearData2 =true;
            }
            dataOverDialog = true;
            dataResidueDialog = true;
        }else if(day != start){
            if(simIndex == Util.FIRST_SIM_NO) {
                isClearData1 = false;
            }else{
                isClearData2 =false;
            }
        }


        long idleUsed = 0;
        if(!Util.getIdlePackage(mContext, simIndex).isEmpty()){  //计算闲时已用流量
            //监控闲时流量
            long data = Util.getIdleUsed(mContext, simIndex); //先获取储存的值（第三方设置的/每日定点累计的）
            idleUsed = data + networkStatsHelper.getIdleUsedData(this,simIndex);  //每次设置Package的时候都让data清空
            Util.setIdleDataUsed(mContext, simIndex, idleUsed);
            int[] endTime =Util.getEndTime(mContext, simIndex);

            if(hour == endTime[0] && minute == endTime[1] && second < 10){
                Util.setIdleUsed(mContext, simIndex, idleUsed);
            }
            if(simIndex ==1){
                idleUsed1 = idleUsed;
            }else{
                idleUsed2 = idleUsed;
            }
            Log.d(TAG,"monitorData idleUsed ：" +idleUsed + "data =" +data);
            //计算剩余流量
            updateIdleResidue(idleUsed ,simIndex);
        }else{
            progreessIdle = 0;  //如果闲时流量包未设置，则progressBarIdle设为0
        }
        updatePackageUsed(idleUsed,simIndex);

        if(Util.getDailyReminder(mContext, simIndex)) {
            monitorDailyData(simIndex);
        }
        //return  simIndex;
    }

    private void monitorDailyData(int simIndex) {  //监控每日流量

        long data = networkStatsHelper.getAllTodayMobile(this,simIndex);
        Log.d(TAG, "monitorData DailyData ：" + data);
        long setData = Util.getDailyReminderData(mContext, simIndex);
        if(data >= setData && dataDailyDialog){
            //TODO 弹流量提示框，需要SendMessage交给Handler处理
            Message msg = handler.obtainMessage(MSG_NEED_ALARTDIALOG,0,0);
            Bundle bundle = new Bundle();
            bundle.putInt("index",3);
            bundle.putLong("data",setData);
            msg.setData(bundle);
            msg.sendToTarget();
            //showDialog(3);
        }
    }

    private void clearData(int simIndex) { //用户设置流量，监测的流量都清除
        Util.setPackageUsed(mContext, simIndex, 0);
        Util.setDataUsed(mContext, simIndex,0);
        Util.setPackageResidue(mContext, simIndex, 0);
        Util.setPackageOver(mContext, simIndex, 0);
        Util.setIdleUsed(mContext, simIndex, 0);
        Util.setIdleDataUsed(mContext, simIndex, 0);
        Util.setIdleResidue(mContext, simIndex, 0);
        Util.setCorrectIdleData(mContext, simIndex, 0);
        Util.setDataChangeTime(mContext, simIndex, System.currentTimeMillis());  //设置月流量计算时间为当前时间
        //sp.setIdleDataOver(0);
    }

    private void updatePackageUsed(long idledata, int simIndex) { //计算Package已用流量
        long packageUsed = Util.getPackageUsed(mContext, simIndex);
        long time = Util.getDataChangeTime(mContext, simIndex);
        long correctIdleData = Util.getCorrectIdleData(mContext, simIndex);
        long dataUsed = packageUsed + networkStatsHelper.getUsedData(this, simIndex) +correctIdleData - idledata ; //这里加上了校正后的闲时流量

        if(simIndex == 1){
            packageUsed1 = dataUsed;
        }else{
            packageUsed2 = dataUsed;
        }
        Util.setDataUsed(mContext, simIndex, dataUsed);
        Log.d(TAG,"monitorData dataUsed ：" +dataUsed +"packageUsed =" + packageUsed +"correctIdle = " + correctIdleData +"idledata =" + idledata +
              "cac=" + networkStatsHelper.getUsedData(this, simIndex));
        Log.d(TAG,"monitorData dataUsed get：" +Util.getDataUsed(mContext, simIndex));
        if(!Util.getUserPackage(mContext, simIndex).isEmpty()) {
            updatePackageResidue(dataUsed, simIndex); //流量套餐不为空时，才去计算剩余流量
        }else{
            progressPackage = 0;
            //String sr = this.getResources().getString(R.string.data_used);
            //progressMainString = String.format(sr ,NetworkStatsHelper.byteToMB(dataUsed));
        }
    }

    private void updatePackageResidue(long data,int simIndex) {  //剩余流量监控
        //long residue = sp.getPackageResidue();
        long residueData =0;
        long dataPackage = NetworkStatsHelper.dataMBTOB(Util.getUserPackage(mContext, simIndex));
        if(dataPackage < data){ //如果已用流量大于流量套餐的流量，则把流量保存至超额流量，并将剩余流量设置为0
            if(SimUtils.supporteSIM() && !isAppInstalled("com.caiharbor.esim") && Util.getNotifyFlag(mContext)) {
                Util.setNotifyFlag(mContext,false);
                Message msg = handler.obtainMessage(MSG_SHOW_NOTIFICATION,0,0);
                msg.sendToTarget();
            }
            residueData = data -dataPackage;
            progressPackage = 100;
//            String sr = this.getResources().getString(R.string.data_over);
            //progressMainString = String.format(sr ,NetworkStatsHelper.byteToMB(residueData));
            Util.setPackageResidue(mContext, simIndex, 0);
            Util.setPackageOver(mContext, simIndex, residueData);
            if(dataOverDialog){
                //TODO 弹流量提示框，需要SendMessage交给Handler处理
                Message msg = handler.obtainMessage(MSG_NEED_ALARTDIALOG,0,0);
                Bundle bundle = new Bundle();
                bundle.putInt("index",1);
                bundle.putLong("data",0);
                msg.setData(bundle);
                msg.sendToTarget();
                //showDialog(1);
            }
            //TODO set corrent package name
        }else {
            residueData = dataPackage - data;
            progressPackage = (int)(data/dataPackage*100);
//            String sr = this.getResources().getString(R.string.data_residue);
            //progressMainString = String.format(sr ,NetworkStatsHelper.byteToMB(residueData));
            if(Util.getMonthlyReminder(mContext, simIndex)){
                monitorMonthlyData(residueData, simIndex);
            }
            Util.setNotifyFlag(mContext,true);
            Util.setPackageResidue(mContext, simIndex, residueData);
            Util.setPackageOver(mContext, simIndex, 0);
        }
        if(simIndex ==1){
            packageResidue1 = residueData;
        }else{
            packageResidue2 = residueData;
        }
        Log.d(TAG,"monitorData residueData ：" +residueData);
    }

    private void monitorMonthlyData(long residueData, int simIndex) {
        long data = Util.getMonthlyReminderData(mContext, simIndex);
        Log.d(TAG,"monitorData MonthlyData ：" +data);
        if(residueData <= data && dataResidueDialog){
            //TODO 弹流量提示框，需要SendMessage交给Handler处理
            Message msg = handler.obtainMessage(MSG_NEED_ALARTDIALOG,0,0);
            Bundle bundle = new Bundle();
            bundle.putInt("index",2);
            bundle.putLong("data",data);
            msg.setData(bundle);
            msg.sendToTarget();
            //showDialog(2);
        }
    }

    private void updateIdleResidue(long data,int simIndex) { //闲时剩余流量监控
        long idleResidue;
        long idlePackageData = NetworkStatsHelper.dataMBTOB(Util.getIdlePackage(mContext,simIndex));
        //TODO 闲时流量需要超额吗？
        if(idlePackageData < data) {
            idleResidue = 0;  //超额流量UI
            //progreessIdle = 100;
           // String sr = this.getResources().getString(R.string.data_over);
            //progressIdleString = String.format(sr ,NetworkStatsHelper.byteToMB(idleResidue));
            Util.setIdleResidue(mContext, simIndex, 0);  //闲时剩余流量设为0,闲时已用设置为idlePackageData
            Util.setIdleDataUsed(mContext, simIndex, idlePackageData);
        }else {
            idleResidue =idlePackageData- data;//校正之后，Package是否也校正了，如果校正了，剩余流量就等于idlePackageData-dataprogress
            //progreessIdle = (int)(data/idlePackageData*100); //计算Progress值
//            String sr = this.getResources().getString(R.string.data_residue);
            //progressIdleString = String.format(sr ,NetworkStatsHelper.byteToMB(idleResidue));
            Util.setIdleResidue(mContext, simIndex, idleResidue);
        }
        if(simIndex ==1){
            idleResidue1 = idleResidue;
        }else{
            idleResidue2 = idleResidue;
        }
        Log.d(TAG,"monitorData idleResidue ：" +idleResidue);
    }

    private void sendNotification() {
        Log.d(TAG,"sendNotification");
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.download_link)));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,  intent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setSmallIcon(R.drawable.esim_icon_notification)
                .setContentTitle(getResources().getString(R.string.notification_esim_title))
                .setContentText(getResources().getString(R.string.notification_esim_msg))
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        //.setWhen(System.currentTimeMillis());
        notifyManager.notify(0, builder.build());
    }
    private boolean isAppInstalled(String uri){
        PackageManager pm = getPackageManager();
        boolean installed =false;
        try{
            pm.getPackageInfo(uri,PackageManager.GET_ACTIVITIES);
            installed =true;
        }catch(PackageManager.NameNotFoundException e){
            installed =false;
        }
        return installed;
    }

    private void showDialog(int i,long data) {
        String sr;
        String string,sr_data;
        String message;
        int simIndex = Util.getSimIndex(mContext);
        isAutoTurnOffNetwork = Util.getAutoTurnoffNet(mContext, simIndex);
        if(simIndex ==1){
            sr=getResources().getString(R.string.sim_frist_name);
        }else{
            sr=getResources().getString(R.string.sim_sec_name);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this,THEME_DEVICE_DEFAULT_LIGHT);
        switch(i){
            case 1:{
                builder.setTitle(R.string.used_reminder);

                if(!isAutoTurnOffNetwork) {
                    string = getResources().getString(R.string.monthly_used_reminder_message);
                    message = String.format(string,sr);
                    builder.setMessage(message);
                    builder.setPositiveButton(R.string.turn_off_mobile_data, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            turnOffMobileData();
                        }
                    });
                }else{
                    string = getResources().getString(R.string.used_reminder_message);
                    message = String.format(string,sr);
                    builder.setMessage(message);
                    turnOffMobileData(); //直接关掉网络
                }
                dataOverDialog = false;
                break;
            }
            case 2:{
                builder.setTitle(R.string.monthly_remaining_reminder);
                sr_data = NetworkStatsHelper.byteToMB(data);
                string = getResources().getString(R.string.monthly_remaining_reminder_message);
                message = String.format(string,sr,sr_data);
                builder.setMessage(message);
                builder.setPositiveButton(R.string.turn_off_mobile_data, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        turnOffMobileData();
                    }
                });
                dataResidueDialog = false;
                break;
            }
            case 3:{
                builder.setTitle(R.string.daily_used_reminder);
                sr_data = NetworkStatsHelper.byteToMB(data);
                string = getResources().getString(R.string.daily_used_reminder_message);
                message = String.format(string,sr,sr_data);
                builder.setMessage(message);
                builder.setPositiveButton(R.string.turn_off_mobile_data, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        turnOffMobileData();
                    }
                });
                dataDailyDialog = false;
                break;
            }
            default:
                break;
        }
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.create();
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        dialog.show();
    }

    private void turnOffMobileData() {  // 关闭数据网络
        try {
            TelephonyManager telephonyService = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            Method setMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (null != setMobileDataEnabledMethod) {
                setMobileDataEnabledMethod.invoke(telephonyService, false);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error setting mobile data state", ex);
        }

        //telephonyManager.setDataEnabled(false);  换方法
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        Log.d(TAG,"onCreate");
        serviceRunning=true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("auto_check");
        filter.addAction("auto_check_cancel");
        registerReceiver(SMSReceiver, filter);
        networkStatsManager = (NetworkStatsManager)getSystemService(NETWORK_STATS_SERVICE);
        networkStatsHelper = new NetworkStatsHelper(networkStatsManager, mContext);
        //spDataMonitor=new Util(this);
        TrafficCorrectionWrapper.getInstance().setTrafficCorrectionListener(new ITrafficCorrectionListener() {
            /**
             * 需要发短信校正，收到此通知后，根据查询码与端口号，发送查询短信，等收到短信后，调用analysisSMS接口来获取流量信息
             * @param simIndex  卡槽1: IDualPhoneInfoFetcher.FIRST_SIM_INDEX
             * 					卡槽2: IDualPhoneInfoFetcher.SECOND_SIM_INDEX
             * 					如果单卡手机，为IDualPhoneInfoFetcher.FIRST_SIM_INDEX
             * @param queryCode 查询码
             * @param queryPort 查询端口号
             */

            @Override
            public void onNeedSmsCorrection(int simIndex, String queryCode, String queryPort) {
//                i:0 s: 6666 s1:10086
                Util.setIsSimStateChanage(mContext, false); //收到校正消息，将SimStateChange设为false
                Log.d(TAG, String.valueOf(simIndex));
                Log.d(TAG,queryCode);
                Log.d(TAG,queryPort);
                mSimIndex=simIndex;
                mQueryCode=queryCode;
                mQueryPort=queryPort;
                if(simIndex == 0){
                    mQueryCode1 = queryCode;
                    mQueryPort1 = queryPort;
                }else{
                    mQueryCode2 = queryCode;
                    mQueryPort2 = queryPort;
                }
                Message msg = handler.obtainMessage(MSG_NEED_SEND_MSG,0,0);
                Bundle bundle = new Bundle();
                bundle.putInt("simIndex",simIndex);
                bundle.putString("queryCode",queryCode);
                bundle.putString("queryPort",queryPort);
                msg.setData(bundle);
                msg.sendToTarget();




            }
            /**
             * 获取流量值(KByte)，剩余流量值或已用流量值，由参数subClass识别
             * @param simSolt  卡槽1: IDualPhoneInfoFetcher.FIRST_SIM_INDEX
             * 					卡槽2: IDualPhoneInfoFetcher.SECOND_SIM_INDEX
             * 					如果单卡手机，为IDualPhoneInfoFetcher.FIRST_SIM_INDEX
             * @param trafficClass 流量种类，如TC_TrafficCommonNum
             * @param subClass 流量子类， 剩余流量为TSC_LeftKByte，已用流量为TSC_UsedKBytes
             * @param kBytes 流量值（单位为K）
             */

            @Override
            public void onTrafficInfoNotify(int simSolt, int trafficClass, int subClass, int kBytes) {
                Log.d(TAG, "onTrafficInfoNotify");
                int simIndex = Util.getSimIndex(mContext);
                Util.setDataChangeTime(mContext, simIndex, System.currentTimeMillis());  //获得流量后存入时间
                logTrafficInfo(simIndex, simSolt, trafficClass, subClass, kBytes);  //不应该直接处理
                Message msg = handler.obtainMessage(MSG_CORRECTION_RIGHT);
                Bundle bundle = new Bundle();
                bundle.putBoolean("correct_right",true);
                msg.setData(bundle);
                msg.sendToTarget();
              /*  Message msg = handler.obtainMessage(MSG_TRAFfICT_NOTIFY,simIndex,0);
                msg.obj = logTrafficInfo(simIndex, trafficClass, subClass, kBytes);
                msg.sendToTarget();*/
               // android.util.Log.v(TAG, "onTrafficNotify-" + (String)msg.obj);
            }
//            onError中的errorCode说明：
//            ERR_CORRECTION_FEEDBACK_UPLOAD_FAIL = -10002; // 回包异常
//            ERR_CORRECTION_BAD_SMS     = -10003; // 运营商无效短信
//            ERR_CORRECTION_PROFILE_UPLOAD_FAIL = -10004;  // 省、市、运营商上报失败
//            ERR_CORRECTION_LOCAL_NO_TEMPLATE = -10005; // 本地无模板
//            ERR_CORRECTION_PROFILE_ILLEGAL = -10006; //  省、市、运营商不合法
//            ERR_CORRECTION_LOCAL_TEMPLATE_UNMATCH = -10007; // 本地模板不匹配
            public void onProfileNotify(int simIndex, ProfileInfo info){}

            public void onCorrectionResult(int simIndex, int retCode){
                //校正失败
//                if(retCode == -1){
//                    Message msg = handler.obtainMessage(MSG_CORRECTION_RIGHT);
//                    Bundle bundle = new Bundle();
//                    bundle.putBoolean("correct_right",false);
//                    msg.setData(bundle);
//                    msg.sendToTarget();
//                }
                Log.i(TAG, "onCorrectionResult:[" + simIndex + "]retCode:[" + retCode + "]");
            }
            @Override
            public void onError(int simIndex, int errorCode) {
                //返回校正出错
                Log.d(TAG,"error");
                Message msg = handler.obtainMessage(MSG_CORRECTION_RIGHT);
                Bundle bundle = new Bundle();
                bundle.putBoolean("correct_right",false);
                msg.setData(bundle);
                msg.sendToTarget();
                String strState = "状态信息：";
                strState += "卡：[" + simIndex + "]校正出错:[" + errorCode + "]";

                if(IDualPhoneInfoFetcher.FIRST_SIM_INDEX == simIndex){
                 //   mTVSim1Detail.setText(strState);
                }else if(IDualPhoneInfoFetcher.SECOND_SIM_INDEX == simIndex){
                  //  mTVSim2Detail.setText(strState);
                }
                Log.i(TAG, "onError--simIndex:[" + simIndex + "]errorCode:[" + errorCode + "]");
            }
        });
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(SMSReceiver);
        mTimerTask.cancel();
        Log.d(TAG,"stopService");
        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }



    public void sendSMS(String phoneNumber,String message,int simIndex){
        //获取短信管理器
        android.telephony.SmsManager smsManager;
        if(NetworkStatsHelper.isDualSimEnable(mContext) ==3) { //如果是插两张卡
             smsManager = android.telephony.SmsManager.getSmsManagerForSubscriptionId(simIndex + 1);
            Log.d(TAG,"getsubid test = " +smsManager.getSubscriptionId());
        }else{
            smsManager = android.telephony.SmsManager.getDefault();
        }
        //用哪张卡校正
        //拆分短信内容（手机短信长度限制）,貌似长度限制为140个字符,就是
        //只能发送70个汉字,多了要拆分成多条短信发送
        //第四五个参数,如果没有需要监听发送状态与接收状态的话可以写null
        List<String> divideContents = smsManager.divideMessage(message);
        for (String text : divideContents) {
            smsManager.sendTextMessage(phoneNumber, null, text, null, null);
        }
    }
/*    private boolean hasPermissionToReadNetworkStats() {

        final AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        requestReadNetworkStats();
        return false;
    }*/
    // 打开“有权查看使用情况的应用”页面
/*    private void requestReadNetworkStats() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }*/
    //保留相关信息
    String logTrafficInfo(int simIndex, int simSolt, int trafficClass, int subClass, int kBytes){
        String logTemp = "";
        Log.d(TAG,"simIndex =" + simIndex +"simSolt =" +simSolt);
        if(simIndex != simSolt+1){
            simIndex = simSolt+1;
        }
        if(trafficClass == ITrafficCorrectionListener.TC_TrafficCommon){
            logTemp += "--package";
            if(subClass == ITrafficCorrectionListener.TSC_LeftKByte){
                logTemp = logTemp + "-left:[" + kBytes + "]";
                Util.setPackageResidue(mContext, simIndex, kBytes*1024L);
                long packageData = Util.getResDataPackage(mContext, simIndex);
                if(packageData !=0){
                    long data = packageData - kBytes*1024L;
                    Util.setPackageUsed(mContext, simIndex, data);
                    logTemp = logTemp + "-used:[" + data + "]";
                }
            }else if(subClass == ITrafficCorrectionListener.TSC_UsedKBytes){
                logTemp = logTemp + "-used info:[" + kBytes + "]";
                Util.setPackageUsed(mContext, simIndex, kBytes*1024L);
            }else if(subClass == ITrafficCorrectionListener.TSC_TotalKBytes){
                logTemp = logTemp + "-all:[" + kBytes + "]";
                Util.setUserPackage(mContext, simIndex, String.valueOf(kBytes / 1024));
                Util.setResDataPackage(mContext, simIndex, kBytes * 1024L);
            }

        }else if(trafficClass == ITrafficCorrectionListener.TC_TrafficFree){
            logTemp += "--Idle";
            if(subClass == ITrafficCorrectionListener.TSC_LeftKByte){
                logTemp = logTemp + "-left:[" + kBytes + "]";
                Util.setIdleResidue(mContext, simIndex, kBytes * 1024L);
                long idleData = Util.getResDataIdle(mContext, simIndex);
                if(idleData != 0){
                    long data = idleData - kBytes*1024L;
                    Util.setIdleUsed(mContext, simIndex, data);
                    Util.setCorrectIdleData(mContext, simIndex, data);
                    logTemp = logTemp + "idleData:["+idleData +"]"+ "-used:[" + data + "]";
                }
            }else if(subClass == ITrafficCorrectionListener.TSC_UsedKBytes){
                logTemp = logTemp + "-used info:[" + kBytes + "]";
                Util.setIdleUsed(mContext, simIndex, kBytes*1024L);
            }else if(subClass == ITrafficCorrectionListener.TSC_TotalKBytes){
                logTemp = logTemp + "-all:[" + kBytes + "]";
                Util.setIdlePackage(mContext, simIndex, String.valueOf(kBytes / 1024));
                Util.setResDataIdle(mContext, simIndex, kBytes * 1024L);
            }
        }else if(trafficClass == ITrafficCorrectionListener.TC_Traffic4G){
            logTemp += "--4G \n";
        }
        Log.i(TAG, logTemp);

        return logTemp;
    }
}
