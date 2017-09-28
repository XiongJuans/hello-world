package com.htc.datausagemonitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.htc.datausagemonitor.data.NetworkStatsHelper;

import java.util.ArrayList;

/**
 * Created by majing on 17-7-24.
 */

public class Util {

    public static final String SP_NAME = "data_monitor";
    public static final String SP_NAME_SUB = "data_monitor_sub";
    public static final String SP_NAME_SETTING = "data_settings";

    public static final String SIM_NO = "sim_no";

    public static final String HAVE_SHOW_PERMISSION = "isShowPrompt";
    public static final String IS_WIZARD = "is_wizard";
    public static final String FRAG_NAME = "frag_name";

    public static final String PACKAGE_USED = "set_package_used";
    public static final String IDLE_USED = "set_idle_used";
    public static final String PACKAGE_RESIDUE = "set_package_residue";
    public static final String IDLE_RESIDUE = "set_IDLE_residue";
    public static final String USER_PACKAGE = "set_user_package";
    public static final String IDLE_PACKAGE = "set_idle_package";
    public static final String SET_TIME = "set_time";
    public static final String START_DATE = "start_date";
    public static final String START_HOUR = "start_hour";
    public static final String START_MINUTE = "start_minute";
    public static final String END_HOUR = "end_hour";
    public static final String End_MINUTE = "end_minute";

    public static final String PROVINCE = "mprovince";
    public static final String CITY = "mcity";
    public static final String OPERATOR = "moperator";
    public static final String BRAND = "mbrand";
    public static final String OPERATORBRAND = "moperatorbrand";

    public static final String CITY_NAME = "mcity_name";
    public static final String OPERATOR_NAME = "moperator_name";

    public static final String DATA_REMINDER = "data_reminder";
    public static final String AUTO_TURNOFF_NETWORK = "auto_turnoff_network";
    public static final String MONTHLY_REMINDER = "monthly_reminder";
    public static final String MONTHLY_REMINDER_DATA = "monthly_reminder_data";
    public static final String DAILY_REMINDER = "daily_reminder";
    public static final String DAILY_REMINDER_DATA = "daily_reminder_data";

    public static final String DATA_MONITOR = "switch_data_monitor";
    public static final String DATA_MONITOR_FLOAT = "switch_data_float";
    public static final String DATA_AUTO_CHECK_MAIN = "data_auto_check_main";
    public static final String DATA_AUTO_CHECK_SUB = "data_auto_check_SUB";

    public static final String DATA_CHANGE_TIME = "data_change_time";

    public static final String DATA_USED_PACKAGE = "cal_data_used_package";
    public static final String DATA_USED_IDLE = "cal_idle_used";
    public static final String RESIDUE_DATA_PACKAGE = "cal_residue_data_package";
    public static final String RESIDUE_IDLE_DATA = "cal_residue_idle";
    public static final String OVER_DATA_PACKAGE = "cal_over_data_package"; //超额
    public static final String CORRECT_IDLE_DATA = "correct_data_idle";
    public static final String SIM_INDEX = "sim_index";
    public static final String CORRECT_IDLE_USED_DATA = "correct_used_data_idle";
    public static final String CORRECT_PACKAGE_USED_DATA = "correct_package_data_idle";

    public static final String CorrectionTime="Manual_CorrectionTime";
    public static final String AutoCorrectionTime0="Auto_CorrectionTime0";
    public static final String AutoCorrectionTime1="Auto_CorrectionTime1";
//    public static final String IMSI="Sim_IMSI";
    public static final String IS_SIM_STATE_CHNAGE ="is_sim_state_change";

    public static final String IS_ESIM_ENABLE = "is_esim_enable";

    public static final String IS_FLOAT_WINDOW_SHOW = "is_float_window_show";
    public static final String NotifyFlag = "nontify_flag";

    public static final int FIRST_SIM_SOLTID = 0;
    public static final int SECOND_SIM_SOLTID = 1;
    public static final int FIRST_SIM_NO = 1;
    public static final int SECOND_SIM_NO = 2;
    // for float window
    public static double TIME_SPAN = 3000d;
    public static String SP_X = "SP_X";
    public static String SP_Y = "SP_Y";
    public static String SP_STATUSBAR_HEIGHT = "SP_STATUSBAR_HEIGHT";

    public static final int FIRST_SIM_READY = 1;
    public static final int SECOND_SIM_READY = 2;
    public static final int DUAL_SIM_READY = 3;

    public static int getPrefInt(Context context, String key){
        SharedPreferences pref = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        return pref.getInt(key, -1);
    }
    public static void savePrefInt(Context context, String key, int value){
        SharedPreferences pref = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        pref.edit().putInt(key, value).apply();
    }
/*
    Context mContext;
    SharedPreferences mSharedPreferences;
    SharedPreferences settings;

    public Util(Context context){
        mContext = context;
        mSharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
    }

    public Util(Context context, int simIndex){
        mContext = context;
        if(simIndex == 1){
            mSharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }else{
            mSharedPreferences = context.getSharedPreferences(SP_NAME_SUB, Context.MODE_PRIVATE);
        }
        settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
    }
*/
    private static String getPreFileName(int simIndex) {
        if (simIndex == FIRST_SIM_NO) {
            return SP_NAME;
        } else if (simIndex == SECOND_SIM_NO) {
            return SP_NAME_SETTING;
        }
        return SP_NAME; //TBD need to check?
    }
/*    public void clearall(){
        mSharedPreferences.edit().clear().apply();
    }
    public void setSimIMSI(String imsi){
        mSharedPreferences.edit().putString(IMSI, imsi).apply();
    }
    public String getSimIMSI(){
        return mSharedPreferences.getString(IMSI, "");
    }*/

    public static boolean getIsEsimEnable(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getBoolean(IS_ESIM_ENABLE,false);
    }
    public static void setIsEsimEnable(Context context, int simIndex, boolean isEsimEnable){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putBoolean(IS_ESIM_ENABLE,isEsimEnable).apply();
    }

    public static boolean isSimStateChange(Context context){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        return settings.getBoolean(IS_SIM_STATE_CHNAGE,false);
    }
    public static void setIsSimStateChanage(Context context, boolean isChange){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        settings.edit().putBoolean(IS_SIM_STATE_CHNAGE,isChange).apply();
    }

    public static boolean getDataReminder(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getBoolean(DATA_REMINDER, true);
    }
    public static  void setDataReminder(Context context, int simIndex, boolean reminder){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putBoolean(DATA_REMINDER, reminder).apply();
    }

    public static boolean getAutoTurnoffNet(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getBoolean(AUTO_TURNOFF_NETWORK, false);
    }
    public static void setAutoTurnoffNet(Context context, int simIndex, boolean isAuto){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putBoolean(AUTO_TURNOFF_NETWORK, isAuto).apply();
    }

    public static boolean getMonthlyReminder(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getBoolean(MONTHLY_REMINDER, false);
    }
    public static void setMonthlyReminder(Context context, int simIndex, boolean isReminder){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putBoolean(MONTHLY_REMINDER, isReminder).apply();
    }

    public static long getMonthlyReminderData(Context context, int simIndex){  //月剩余流量提醒
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(MONTHLY_REMINDER_DATA, 0);
    }
    public static void setMonthlyReminderData(Context context, int simIndex, long data){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(MONTHLY_REMINDER_DATA, data).apply();
    }

    public static boolean getDailyReminder(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getBoolean(DAILY_REMINDER, false);
    }
    public static void setDailyReminder(Context context, int simIndex, boolean isReminder){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putBoolean(DAILY_REMINDER, isReminder).apply();
    }

    public static long getDailyReminderData(Context context, int simIndex){  //日已用流量提醒
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(DAILY_REMINDER_DATA, 0);
    }
    public static void setDailyReminderData(Context context, int simIndex, long data){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(DAILY_REMINDER_DATA, data).apply();
    }

    public static int getSimIndex(Context context){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        return settings.getInt(SIM_INDEX,1);
    }

    public static void setSimIndex(Context context, int simIndex){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        settings.edit().putInt(SIM_INDEX,simIndex).apply();
    }
    public static boolean getPrefHaveShowPermission(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getBoolean(HAVE_SHOW_PERMISSION, true);
    }
    public static void setPrefHaveShowPermission(Context context, int simIndex, boolean haveShowPermission){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putBoolean(HAVE_SHOW_PERMISSION, haveShowPermission).apply();
    }

    public static boolean getIsDataMonitor(Context context){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        return settings.getBoolean(DATA_MONITOR,true);
    }

    public static boolean getIsFloat(Context context){  //Service里面看是否要显示流量框
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        return settings.getBoolean(DATA_MONITOR_FLOAT,true);
    }

    public static boolean IsFloatWindowShow(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        return  settings.getBoolean(IS_FLOAT_WINDOW_SHOW, false);
    }

    public static void setIsFloatWindowShow(Context context, boolean isFloatWindowShow) {
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        settings.edit().putBoolean(IS_FLOAT_WINDOW_SHOW,isFloatWindowShow).apply();
    }

    public static boolean getNotifyFlag(Context context){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        return settings.getBoolean(NotifyFlag,true);
    }

    public static void setNotifyFlag(Context context, boolean notifyFlag) {
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        settings.edit().putBoolean(NotifyFlag,notifyFlag).apply();
    }

    public  static boolean getIsDataAutoCheck(Context context, int sim){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        if(sim==0) {
            return settings.getBoolean(DATA_AUTO_CHECK_MAIN, true);
        }else{
            return settings.getBoolean(DATA_AUTO_CHECK_SUB,true);
        }
    }

//    public boolean getIsDataAutoCheckSub(){
//        return settings.getBoolean(DATA_AUTO_CHECK_SUB,true);
//    }
    public static void setDataAutoCheckMain(Context context, boolean bool){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        settings.edit().putBoolean(DATA_AUTO_CHECK_MAIN, bool).apply();
    }
    public static void setDataAutoCheckSub(Context context, boolean bool){
        SharedPreferences settings = context.getSharedPreferences(SP_NAME_SETTING, Context.MODE_PRIVATE);
        settings.edit().putBoolean(DATA_AUTO_CHECK_SUB, bool).apply();
    }
    public static long getPackageUsed(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(PACKAGE_USED, 0);
    }
    public static void setPackageUsed(Context context, int simIndex, long packageUsed){ //校正或者修改的已用流量，第三方设置的流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(PACKAGE_USED, packageUsed).apply();
    }

    public static long getIdleUsed(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(IDLE_USED, 0);
    }
    public static void setIdleUsed(Context context, int simIndex, long idleUsed){//校正或者修改的已用流量，第三方设置的流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(IDLE_USED, idleUsed).apply();
    }

    public static String getUserPackage(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getString(USER_PACKAGE, "");
    }
    public static void setUserPackage(Context context, int simIndex, String userPackage){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putString(USER_PACKAGE, userPackage).apply();
    }

    public static int getStartDate(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getInt(START_DATE,1);
    }

    public static void setStartDate(Context context, int simIndex, int startDate){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putInt(START_DATE, startDate).apply();
    }

    public static String getIdlePackage(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getString(IDLE_PACKAGE, "");
    }
    public static void setIdlePackage(Context context, int simIndex, String userPackage){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putString(IDLE_PACKAGE, userPackage).apply();
    }

    public static int[] getStartTime(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        int[] time = new int[2];
        int hour = pref.getInt(START_HOUR, 23);
        int minute =pref.getInt(START_MINUTE, 0);
        time[0] = hour;
        time[1] = minute;
        return time;
    }
    public static void setStartTime(Context context, int simIndex, int startHour,int startMinute){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(START_HOUR, startHour);
        editor.putInt(START_MINUTE, startMinute);
        editor.apply();
    }

    public static int[] getEndTime(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        int[] time = new int[2];
        int hour = pref.getInt(END_HOUR, 8);
        int minute = pref.getInt(End_MINUTE, 0);
        time[0] = hour;
        time[1] = minute;
        return time;
    }

    public static void setEndTime(Context context, int simIndex, int endHour,int endMinute){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(END_HOUR, endHour);
        editor.putInt(End_MINUTE, endMinute);
        editor.apply();
    }

   public static ArrayList<String> getOperatorName(Context context, int simIndex){
       SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        ArrayList<String> operatorList = new ArrayList<>();
        operatorList.add(0, pref.getString(CITY_NAME, ""));  //默认上海移动4G
        operatorList.add(1, pref.getString(OPERATOR_NAME, ""));

        return operatorList;
    }

    public static void setOperatorName(Context context, int simIndex, String province, String operator){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        Log.d("setOperator","province:"+province);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(CITY_NAME,province);
        editor.putString(OPERATOR_NAME,operator);
        editor.apply();
    }

    public static void setAutoCorrectionTime(Context context, int simIndex, long time,int sim) {
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        Log.d("setAutoCorrectionTime", "AutoCorrectionTime:" + time+" sim:"+sim);
        SharedPreferences.Editor editor = pref.edit();
        if(sim==1){
            editor.putLong(AutoCorrectionTime0, time);
        }else{
            editor.putLong(AutoCorrectionTime1, time);
        }
        editor.apply();
    }

    public static long getAutoCorrectionTime(Context context, int simIndex, int sim) {
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        Log.d("getAutoCorrectionTime", "getAutoCorrectionTime:" );
        if(sim==1){
            return pref.getLong(AutoCorrectionTime0, 0);  //
        }
        else{
            return pref.getLong(AutoCorrectionTime1, 0);  //
        }
    }

    public static void setCorrectionTime(Context context, int simIndex) {
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        Log.d("setCorrectionTime", "CorrectionTime:" + System.currentTimeMillis()/1000);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(CorrectionTime, System.currentTimeMillis()/1000);
        editor.apply();
}
    public static long getCorrectionTime(Context context, int simIndex) {
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        Log.d("getCorrectionTime", "getCorrectionTime:" );
        return pref.getLong(CorrectionTime, 0);  //默认上海移动4G
    }
    public static int[] getOperator(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        int[] operatorList = new int[5];

        operatorList[0]=pref.getInt(PROVINCE, 0);  //默认北京移动4G
        operatorList[1]=pref.getInt(CITY, 0);
        operatorList[2]=pref.getInt(OPERATOR, 0);
        operatorList[3]=pref.getInt(BRAND,0);
        operatorList[4]=pref.getInt(OPERATORBRAND,0);
//        operatorList[0]=mSharedPreferences.getInt("province", 1);  //默认上海移动4G
    return operatorList;
    }

    public static void setOperator(Context context, int simIndex ,int province,int city,int operatro,int brand,int operatrobrand) {
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        Log.d("setOperator", "province:" + province);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PROVINCE, province);
        editor.putInt(CITY, city);
        editor.putInt(OPERATOR, operatro);
        editor.putInt(BRAND, brand);
        editor.putInt(OPERATORBRAND, operatrobrand);
        editor.apply();
    }

    public static long getPackageResidue(Context context, int simIndex){  //这界面显示用
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(PACKAGE_RESIDUE,0);
    }

    public static void setPackageResidue(Context context, int simIndex, long residue){//校正的流量和监控剩余流量都保存在这里
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(PACKAGE_RESIDUE,residue).apply();
    }

    public static long getPackageOver(Context context, int simIndex){  //这界面显示用
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(OVER_DATA_PACKAGE,0);
    }

    public static void setPackageOver(Context context, int simIndex, long residue){  //超额流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(OVER_DATA_PACKAGE,residue).apply();
    }

    public static long getIdleResidue(Context context, int simIndex){  //主界面显示用
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(IDLE_RESIDUE,0);
    }

    public static void setIdleResidue(Context context, int simIndex, long residue){  //校正的流量和监控剩余流量都保存在这里
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(IDLE_RESIDUE,residue).apply();
    }

    public static long getCorrectIdleData(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(CORRECT_IDLE_DATA,0);
    }  //计算Package已用流量的基准

    public static void setCorrectIdleData(Context context, int simIndex, long data){  
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(CORRECT_IDLE_DATA,data).apply();
    }

    public static long getCorrectIdleUsedData(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(CORRECT_IDLE_USED_DATA,0);
    }  //计算Package已用流量的基准

    public static void setCorrectIdleUsedData(Context context, int simIndex, long data){  
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(CORRECT_IDLE_USED_DATA,data).apply();
    }

    public static long getCorrectPackageUsedData(Context context, int simIndex){
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(CORRECT_PACKAGE_USED_DATA,0);
    }  //计算Package已用流量的基准

    public static void setCorrectPackageUsedData(Context context, int simIndex, long data){  //超额流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(CORRECT_PACKAGE_USED_DATA,data).apply();
    }

    public static long getDataChangeTime(Context context, int simIndex){  //更改已用流量或者校正之后的时间点（1.更改已用流量 2.校正成功 3.修改流量套餐）
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        int day = getStartDate(context, simIndex);
        int time = NetworkStatsHelper.getTimesStartDayMorning(day);
      return pref.getLong(DATA_CHANGE_TIME,time);
    }

    public static void setDataChangeTime(Context context, int simIndex, long time) {
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(DATA_CHANGE_TIME,time).apply();
    }

    public static long getDataUsed(Context context, int simIndex){  //监控中设置计算后的已用流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(DATA_USED_PACKAGE,0);
    }  //已用流量

    public static void setDataUsed(Context context, int simIndex, long data){//监控中设置计算后的已用流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(DATA_USED_PACKAGE,data).apply();
    }

    public static long getIdleDataUsed(Context context, int simIndex){ //Service监控中设置计算后的闲时已用流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(DATA_USED_IDLE,0);
    }

    public static void setIdleDataUsed(Context context, int simIndex, long data){ //监控中设置计算后的闲时已用流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(DATA_USED_IDLE,data).apply();
    }

    public static long getResDataPackage(Context context, int simIndex){ //校正之后的Package流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(RESIDUE_DATA_PACKAGE,0);
    }

    public static void setResDataPackage(Context context, int simIndex, long data){//校正之后的Package流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(RESIDUE_DATA_PACKAGE,data).apply();
    }

    public static long getResDataIdle(Context context, int simIndex){ //校正之后的Package流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        return pref.getLong(RESIDUE_IDLE_DATA,0);
    }

    public static  void setResDataIdle(Context context, int simIndex, long data){//校正之后的Package流量
        SharedPreferences pref = context.getSharedPreferences(getPreFileName(simIndex), Context.MODE_PRIVATE);
        pref.edit().putLong(RESIDUE_IDLE_DATA,data).apply();
    }
}
