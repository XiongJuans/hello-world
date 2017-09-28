package com.htc.datausagemonitor;

import android.app.Application;
import android.net.ConnectivityManager;
import android.util.Log;


import com.htc.datausagemonitor.data.NetworkStatsHelper;

import java.util.HashMap;
import java.util.Map;

import tmsdk.common.IDualPhoneInfoFetcher;
import tmsdk.common.ITMSApplicaionConfig;
import tmsdk.common.TMSDKContext;

/**
 * Created by mj on 2017/7/28.
 */

public class HTCApplication extends Application {
    public volatile static boolean mBresult  = false;

    @Override
    public void onCreate() {
        super.onCreate();

        /***
         * TMSDKContext.setDualPhoneInfoFetcher()方法为流量校准支持双卡情况时设置，其它情况不需要调用该函数。
         * 该函数中需要返回第一卡槽和第二卡槽imsi的读取内容。
         *
         * 实现此方法时。一定在TMSDKContext.init前调用
         */
		TMSDKContext.setDualPhoneInfoFetcher(new tmsdk.common.IDualPhoneInfoFetcher(){

			@Override
			public String getIMSI(int simIndex) {
				String imsi = "";
				if(simIndex == IDualPhoneInfoFetcher.FIRST_SIM_INDEX){
					imsi = NetworkStatsHelper.getSubscriberId(getApplicationContext(), ConnectivityManager.TYPE_MOBILE,1); //?TODO 卡槽1的imsi，需要厂商自己实现获取方法
				} else if(simIndex == IDualPhoneInfoFetcher.SECOND_SIM_INDEX) {
					imsi = NetworkStatsHelper.getSubscriberId(getApplicationContext(), ConnectivityManager.TYPE_MOBILE,2);; //?TODO 卡槽2的imsi，需要厂商自己实现获取方法
				}

 				/*try {
					TelephonyManager telephonyManager = (TelephonyManager) DemoApplication.this.getSystemService(Context.TELEPHONY_SERVICE);
					imsi = telephonyManager.getSubscriberId();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				android.util.Log.v("TrafficCorrectionUser", "getIMSI--imsi:[" + imsi + "]");*/
 				Log.d("demo","getIMSI imsi:" +imsi);
				return imsi;
			}

		});

        /**
         * setTMSDKLogEnable（true）会把tmsdk的log打开，release时可以去掉这个接口调用。
         */
        /*TMSDKContext.setTMSDKLogEnable(true);*/
        long start = System.currentTimeMillis();
        /**
         * setAutoConnectionSwitch（）影响渠道号上报这个自动联网项是否运行。请不要一直设置为false，影响激活量和活跃量统计,后台会关注并停止相关服务，请在过工信部测试允许联网后，设置回true
         */
        //boolean nFlag = true;//这里厂商应该用自己保存的用户设置
        //TMSDKContext.setAutoConnectionSwitch(nFlag);
        // TMSDK初始化
        mBresult   = TMSDKContext.init(this, HTCSecureService.class, new ITMSApplicaionConfig() {

            @Override
            public HashMap<String, String> config(
                    Map<String, String> src) {
                HashMap<String, String> ret = new HashMap<String, String>(src);
//						ret.put(TMSDKContext.TCP_SERVER_ADDRESS, "mazutest.3g.qq.com");
//						ret.put(TMSDKContext.CON_IS_TEST, "true");
//						ret.put(TMSDKContext.CON_HOST_URL, "http://wuptest.cs0309.3g.qq.com");
//						// 如厂商有自己服务器（如国外）中转需求，需配置自己服务器域名
//						// http 服务器
//						ret.put(TMSDKContext.CON_HOST_URL, "http://pmir.sec.miui.com");
//						// tcp 服务器
//						ret.put(TMSDKContext.TCP_SERVER_ADDRESS, "mazu.sec.miui.com");
//						ret.put(TMSDKContext.USE_IP_LIST, "false");
                return ret;
            }

        });

        long end = System.currentTimeMillis();
        Log.v("demo", "TMSDK init spend ="+(end-start));
        Log.v("demo", "init result =" + mBresult);
    }
}
