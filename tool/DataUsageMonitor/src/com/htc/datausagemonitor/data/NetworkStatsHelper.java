package com.htc.datausagemonitor.data;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import com.htc.datausagemonitor.Util;

import java.util.Calendar;

/**
 * Created by majing on 17-7-19.
 */

public class NetworkStatsHelper {
    public static final String TAG = NetworkStatsHelper.class.getSimpleName();
    private NetworkStatsManager networkStatsManager;
    private int packageUid;
    private Context mContext;


    public NetworkStatsHelper(NetworkStatsManager networkStatsManager, Context context) {
        this.networkStatsManager = networkStatsManager;
        this.mContext = context;
    }

    public NetworkStatsHelper(NetworkStatsManager networkStatsManager, int packageUid, Context context) {
        this.networkStatsManager = networkStatsManager;
        this.packageUid = packageUid;
        this.mContext = context;
    }

    public long getAllTodayMobile(Context context, int sim) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE,sim),
                    getTimesMorning(),
                    System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        Log.d("getAllTodayMobile","getSubscriberId" +getSubscriberId(context, ConnectivityManager.TYPE_MOBILE,sim) +"sim:" +sim);
        return bucket.getTxBytes() + bucket.getRxBytes();
    }


    public long getAllMonthMobile(Context context, int sim) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE,sim),
                    getTimesMonthMorning(),
                    System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getRxBytes() + bucket.getTxBytes();
    }

    public long getYesterdayMobile(Context context, int sim){
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE,sim),
                    getTimesYesterdayMorning(),
                    getTimesYesterdayEnd());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getRxBytes() + bucket.getTxBytes();
    }

    public long getBeforeYesterdayMobile(Context context, int sim){
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE,sim),
                    getTimesBefYesterdayMorning(),
                    getTimesBefYesterdayEnd());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getRxBytes() + bucket.getTxBytes();
    }

    private long getTimesBefYesterdayEnd() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH,-1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis());
    }

    private long getTimesBefYesterdayMorning() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH,-2);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis());
    }

/*    @RequiresApi(api = Build.VERSION_CODES.M)
    public long getUserSetMobile(Context context, long time,int sim){
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE,sim),
                    time,
                    System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getTxBytes() + bucket.getRxBytes();
    }*/

    public long getUsedData(Context context, int simIndex){  //获取已用流量
        NetworkStats.Bucket bucket;
        //Util sp = new Util(context ,sim);
       // String used = sp.getPackageUsed(); //获取用户或者校正之后的已用流量，每月的起始日应该清除
        long time = Util.getDataChangeTime(mContext, simIndex); //获取用户更改已用流量时间或者校正的时间，这个时间应该每月的起始日去清除
        String idlePackage = Util.getIdlePackage(mContext, simIndex);
       // String idleUsed = sp.getIdleUsed();
        String id = getSubscriberId(context, ConnectivityManager.TYPE_MOBILE, simIndex);
        long usedData;

            try {
                bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                        id,
                        time,
                        System.currentTimeMillis());
            } catch (RemoteException e) {
                return -1;
            }
            //if (used.isEmpty()) {
                    return bucket.getTxBytes() + bucket.getRxBytes();

/*            } else {
                long data = dataMBTOB(used);
               usedData= data + bucket.getTxBytes() + bucket.getRxBytes();//如果当前卡槽没有插卡,bucket.getTxBytes() + bucket.getRxBytes() = 0
            }
            if(idlePackage.isEmpty()){
                return usedData;
            }else{
                long idleData = dataMBTOB(idleUsed);
                return usedData - idleData;
            }*/
    }

    public long getIdleUsedData(Context context, int simIndex){ //获取闲时已用流量,每天流量EndTime需要记录一次流量
        NetworkStats.Bucket bucket;
        long start,end;
        //Util sp = new Util(context ,sim);
        long correctTime = Util.getDataChangeTime(mContext, simIndex);
        int[] startTime = Util.getStartTime(mContext, simIndex); //获取闲时开启时间
        int[] endTime = Util.getEndTime(mContext, simIndex);//获取闲时结束时间
        if(startTime[0] < endTime[0]) {
            start = getStartTimes(startTime[0], startTime[1], true); //同一天
        }else{
            start = getStartTimes(startTime[0], startTime[1], false);
        }
        end = getEndTimes(endTime[0],endTime[1]);
        String id = getSubscriberId(context, ConnectivityManager.TYPE_MOBILE, simIndex);
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    id,
                    start,
                    end);
        } catch (RemoteException e) {
            return -1;
        }
//        if(used.isEmpty()){
        if(correctTime > end){  //当校正时间大于当天的结束时间时，返回0
            return 0;
        }else {
            return bucket.getTxBytes() + bucket.getRxBytes();
        }
/*        }else{
            long data = dataMBTOB(used);
            return data+bucket.getTxBytes() + bucket.getRxBytes();*//*
        }*/
    }

/*    @RequiresApi(api = Build.VERSION_CODES.M)
    public long getResidueData(Context context, int sim){ //获取剩余流量
        NetworkStats.Bucket bucket;
        Util sp = new Util(context ,sim);
        String residue = sp.getPackageResidue(); //获取用户或者校正之后的已用流量，每月的起始日应该清除
        String dataPackage =sp.getUserPackage();
        long time = sp.getDataChangeTime(); //获取用户更改已用流量时间或者校正的时间，这个时间应该每月的起始日去清除
        String id = getSubscriberId(context, ConnectivityManager.TYPE_MOBILE,sim);
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    id,
                    time,
                    System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        long data = bucket.getTxBytes() + bucket.getRxBytes();
        //if(residue.isEmpty()){
            long userPackage = dataMBTOB(dataPackage);  //
            //sp.setPackageResidue(dataBTOString(userPackage-data)); //将剩余流量写入SP
            return userPackage-data;
*//*        }else{
            long residuePackage = dataMBTOB(residue);
            return residuePackage - data;
        }*//*
    }*/


    public long getIdleResidueData(Context context, int sim){

        return 0;
    }
/*  //目前不用 考虑健壮性就需要 即卡槽换卡之后就需要动态更新
    public static void updateUtil(Context context, int networkType) {
        boolean sim1 = SimUtils.getSimStateBySlotIdx(context,0);
        boolean sim2 = SimUtils.getSimStateBySlotIdx(context,1);
        DualSimManager uuSimManager = DualSimManager.getInstance(Build.MODEL);
        String[] strings = uuSimManager.getEntirImsi(context);
        Util sp1=new Util(context,1);
        Util sp2=new Util(context,2);
        if (ConnectivityManager.TYPE_MOBILE == networkType) {
            if(sim1 &&sim2) {
                if (sim1) {
                    if (sp1.getSimIMSI() != strings[1]) {
                        sp1.clearall();
                        sp1.setSimIMSI(strings[1]);
                        Log.d("updateUtil", "setsimimsi 1 " + strings[1]);  //测试发现双卡时先读卡2
                    }

                }
                if (sim2) {
                    if (sp2.getSimIMSI() != strings[0]) {
                        sp2.clearall();
                        sp2.setSimIMSI(strings[0]);
                        Log.d("updateUtil", "setsimimsi 2 " + strings[0]);
                    }
                }
            }
            else{
                if(sim1){
                    if (sp1.getSimIMSI() != uuSimManager.getSubscriberId(context)) {
                        sp1.clearall();
                        sp1.setSimIMSI(uuSimManager.getSubscriberId(context));
                        Log.d("updateUtil", "setsimimsi 3 " + uuSimManager.getSubscriberId(context));
                    }
                }else if(sim2 ){
                    if (sp2.getSimIMSI() != uuSimManager.getSubscriberId(context)) {
                        sp2.clearall();
                        sp2.setSimIMSI(uuSimManager.getSubscriberId(context));
                        Log.d("updateUtil", "setsimimsi 4" + uuSimManager.getSubscriberId(context));
                    }
                }

            }

        }
    }*/
    // simIdex = 1 卡1; simIndex = 2 卡2; return imsi
    public static String getSubscriberId(Context context, int networkType, int simIndex) {
        boolean isSim1Ready = SimUtils.getSimStateBySlotIdx(context, Util.FIRST_SIM_SOLTID);  //soltId=0 卡1是否ready
        boolean isSim2Ready = SimUtils.getSimStateBySlotIdx(context, Util.SECOND_SIM_SOLTID); //soltId=1 卡2是否ready
        DualSimManager uuSimManager = DualSimManager.getInstance(Build.MODEL);
        String[] strings = uuSimManager.getEntirImsi(context);
        if (ConnectivityManager.TYPE_MOBILE == networkType) {
            //Util sp = new Util(context, simIndex);
            //TODO 需要验证，如果当前卡是Esim卡，返回空
/*            if (Util.getIsEsimEnable(context, simIndex)) {
                return "";
            } else {*/
                if (isSim1Ready && isSim2Ready) {
                    if (simIndex == Util.FIRST_SIM_NO) {
                        if(strings.length ==1){  //加保护，有可能获取不到IMSI
                            return "";
                        }
                        Log.i(TAG, "sim 1" + strings[1]);  //双卡时卡1IMSI对应到String[1]
                        return strings[1];
                    } else {
                        Log.i(TAG, "sim 2" + strings[0]);
                        return strings[0];
                    }

                } else {
                    if ((isSim1Ready && simIndex == Util.FIRST_SIM_NO)
                            || (isSim2Ready && simIndex == Util.SECOND_SIM_NO) ) {
                        Log.i(TAG, "sim imsi" + uuSimManager.getSubscriberId(context));
                        return uuSimManager.getSubscriberId(context);
                    } else {
                        return "";
                    }
                }
//            }
        }
        return "";
    }

    /**
     * 获取当天的零点时间
     *
     * @return
     */
    public static long getTimesMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis());
    }

    //获得本月第一天0点时间
    public static int getTimesMonthMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return (int) (cal.getTimeInMillis());
    }

    public static int getTimesStartDayMorning(int startday){
        Calendar cal = Calendar.getInstance();
        //cal.add(Calendar.DAY_OF_MONTH, -startday);
        if(startday > cal.get(Calendar.DAY_OF_MONTH)){
            cal.add(Calendar.MONTH,-1);
            cal.set(Calendar.DAY_OF_MONTH,startday);
            cal.set(Calendar.HOUR_OF_DAY,0);
            cal.set(Calendar.SECOND,0);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.MILLISECOND,0);
            //cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) -1, startday, 0, 0, 0);
            return (int) (cal.getTimeInMillis());
        }else{
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), startday, 0, 0, 0);
            return (int) (cal.getTimeInMillis());
        }
    }

    public static long getStartTimes(int hour,int minute,boolean sameday){
        Calendar cal = Calendar.getInstance();
        if(!sameday) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis());
    }

    public static long getEndTimes(int hour,int minute){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis());
    }

    public static long getTimesYesterdayEnd() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis());
    }

    public static long getTimesYesterdayMorning() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis());
    }

    public static long dataMBTOB(String data){
        if(data.isEmpty()){
            return 0;
        }
        return Long.parseLong(data) *1024*1024;
    }

    public static long dataKBTOB(int data) {
        return (long)data *1024;
    }

    public static String dataBTOString(long data){
        long kb =1024;
        long mb = kb * 1024;
        long f = data/mb;
        return String.format(f > 100 ?"%d":"%d",f) ;

    }

    public static String byteToGMK(long size){
        long kb = 1024;
        long mb = kb*1024;
        long gb = mb*1024;
        if (size >= gb){
            return "G";
        }else if (size >= mb){
            float f = (float) size/mb;
            return "M";
        }else if (size > kb){
            float f = (float) size / kb;
            return "K";
        }else { return "B"; } }

    //most 4 number and .
    public static String byteToMBSize(long size){
        long kb = 1024;
        long mb = kb*1024;
        long gb = mb*1024;
        if (size >= gb){
            float f = (float)size/gb;
            if(f<1000) {
                return String.format(f > 100 ? "%.1f" : "%.2f", f);
            }else{
                return String.format("%.0f",f);
            }
        }else if (size >= mb){
            float f = (float) size/mb;
            if(f<1000) {
                return String.format(f > 100 ? "%.1f" : "%.2f", f);
            }else{
                return String.format("%.0f",f);
            }
        }else if (size > kb){
            float f = (float) size / kb;
            if(f<1000) {
                return String.format(f > 100 ? "%.1f" : "%.2f", f);
            }else{
                return String.format("%.0f",f);
            }
        }else { return String.format("%d",size); } }

    public static String byteToMB(long size){
        long kb = 1024;
        long mb = kb*1024;
        long gb = mb*1024;
        if (size >= gb){
            return String.format("%.2f G",(float)size/gb);
        }else if (size >= mb){
            float f = (float) size/mb;
            return String.format(f > 100 ?"%.0f M":"%.0f M",f);
        }else if (size > kb){
            float f = (float) size / kb;
            return String.format(f>100?"%.0f KB":"%.0f KB",f);
        }else { return String.format("%d B",size); } }

    public static boolean isVirtureSim(Context context,int simIndex){
        String imsi = getSubscriberId(context,ConnectivityManager.TYPE_MOBILE,simIndex);
        if(imsi.startsWith("46000") || imsi.startsWith("46002") || imsi.startsWith("46001")
                || imsi.startsWith("46003")){
            Log.i(TAG,"is normal sim");
            return false;
        }else if(imsi.isEmpty()){
            Log.i(TAG,"no sim");
            return false;
        }else{
            Log.i(TAG,"is vir sim");
            return true;
        }
    }

    public static int isDualSimEnable(Context context){
        boolean isSim1Ready = SimUtils.getSimStateBySlotIdx(context, Util.FIRST_SIM_SOLTID); // check sim1 is ready?
        boolean isSim2Ready = SimUtils.getSimStateBySlotIdx(context, Util.SECOND_SIM_SOLTID);//check sim2 is ready?

        if(isSim1Ready && isSim2Ready){
            return Util.DUAL_SIM_READY;
        }else if(isSim1Ready){
            return Util.FIRST_SIM_READY;
        }else if(isSim2Ready){
            return Util.SECOND_SIM_READY;
        }
        return 0;
    }
}
