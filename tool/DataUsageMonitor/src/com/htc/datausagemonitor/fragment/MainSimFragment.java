package com.htc.datausagemonitor.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.NetworkStatsHelper;

public class MainSimFragment extends PagerBaseFragment {


    private boolean monitor=true;
    private Context mContext;
    /*private int index=1;*/
    public Handler uiHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            String traffic = bundle.getString("traffic","");
            String info = bundle.getString("info","");
            String GMKB=bundle.getString("GMKB","");
            int progress=bundle.getInt("progress",0);
            switch (msg.what){
                //0-只显示本月使用 通用流量
                case 0:
                    if(monitor) {
                        //setTextInfoText(info);
                       // setTextGMKBText(GMKB);
                        setMain(traffic,GMKB,info,progress);
                        setProgressMain(progress, traffic);
                    }else{
                        setMain(traffic,GMKB,info,0);
                        setMainInit(GMKB,info,traffic);
                    }
                    break;
                case 1: //保留
                    setVisible(false);
                    break;
                case 2: //保留
                    setVisible(true);
                    break;
                //闲时流量
                case 3:
                 //   setTextAboveText(2, sr);
                    if(monitor) {
                   // setidleTextInfo(info);
                   // setidleTextGMKB(GMKB);
                        setIdle(traffic,GMKB,info,progress);
                        setidleProgress(progress, traffic);
                    }else{
                        setIdle(traffic,GMKB,info,progress);
                        setIdleInit(GMKB,info,traffic,2);
                    }
                    break;
                case 4:
                    //   setTextAboveText(2, sr);
                    setIdle(traffic,GMKB,info,progress);
                    setIdleInit(GMKB,info,traffic,2);

                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getContext();
        //Util temp = new Util(getContext());

                if (mContext!= null) {
                    monitor = Util.getIsDataMonitor(mContext);
                    int simIndex = 1;
                    //util = new Util(mContext, 1);
                    if (Util.getIdlePackage(mContext, simIndex).isEmpty()) { //保留
                        Vshow(0, "0");
                    } else {
                        long idleUsed = Util.getIdleDataUsed(mContext, simIndex); //闲时已用
                        long idleRes = Util.getIdleResidue(mContext, simIndex); //闲时剩余
                        int idleprogress = 0;
                        String traffic = "";
                        if (idleRes == 0) {
                            idleprogress = 100;
                            traffic = "0";
                        }
                        if ((idleUsed + idleRes) != 0) {
                            idleprogress = (int) ((float) idleUsed / (float) (idleRes + idleUsed) * 100);
                            //String sr2 = this.getResources().getString(R.string.data_used_2);
                            traffic = NetworkStatsHelper.byteToMBSize(idleRes);//String.format(sr2, NetworkStatsHelper.byteToMBSize(idleRes)); //闲时剩余
                        }
                        if(monitor)
                        Vshow(idleprogress, traffic);
                    }

                }
                updateUI();

    }

    public void updateUI() {
        int progress;
        String info="";
        String traffic="";
        String GMKB="";
        String sr,sr2;
        //Util temp = new Util(getContext());
        if(mContext != null) {
            monitor = Util.getIsDataMonitor(mContext);
        }
        if (mContext != null) {
                int simIndex = 1;
                //util = new Util(getContext(), 1);
                long packageData = NetworkStatsHelper.dataMBTOB(Util.getUserPackage(mContext, simIndex)); //流量套餐值
                long idleData = NetworkStatsHelper.dataMBTOB(Util.getIdlePackage(mContext, simIndex));//显示流量套餐值
                long usedData = Util.getDataUsed(mContext, simIndex); //已用流量
                long resudiseData = Util.getPackageResidue(mContext, simIndex);//package剩余流量
                long overData = Util.getPackageOver(mContext, simIndex); //package超额流量
                if (isAdded()) {
                    //闲时流量
                    if (Util.getIdlePackage(mContext, simIndex).isEmpty()) { //保留
                        //TODO 只显示中间的ProgressView
                        // uiHandler.sendEmptyMessage(1);
                        info = this.getResources().getString(R.string.data_residue_1);
                        GMKB = NetworkStatsHelper.byteToGMK(idleData);
                        traffic = "0";
                        Message msg = uiHandler.obtainMessage(4, 0, 0);
                        Bundle bundle = new Bundle();
                        bundle.putString("traffic", traffic);
                        bundle.putString("GMKB", GMKB);
                        bundle.putString("info", info);
                        bundle.putInt("progress", 0);
                        msg.setData(bundle);
                        msg.sendToTarget();
                    } else {
                        // uiHandler.sendEmptyMessage(2);
                        long idleUsed = Util.getIdleDataUsed(mContext, simIndex); //闲时已用
                        long idleRes = Util.getIdleResidue(mContext, simIndex); //闲时剩余
                        //    long idleOver = util.getIdleDataOver(); //闲时超额
                        int idleprogress = 0;
                        if (idleRes == 0) {
                            info = this.getResources().getString(R.string.data_usage_null);
                            GMKB = NetworkStatsHelper.byteToGMK(idleRes);
                            traffic = "0";
                            idleprogress = 100;
                        }else {
                            if ((idleUsed + idleRes) != 0) {
                                idleprogress = (int) ((float) idleUsed / (float) (idleRes + idleUsed) * 100);
                                if (idleprogress == 0 && idleUsed > 0)
                                    idleprogress = 1;
                                info = this.getResources().getString(R.string.data_residue_1);
                                //   setTextAboveText(2, sr);
//                        sr2 = this.getResources().getString(R.string.data_used_2);
                                GMKB = NetworkStatsHelper.byteToGMK(idleRes);
                                traffic = NetworkStatsHelper.byteToMBSize(idleRes);//String.format(sr2, NetworkStatsHelper.byteToMBSize(idleRes)); //闲时剩余
                                //   setidleProgress(idleprogress, info);
                            }
                        }
                        Message msg = uiHandler.obtainMessage(3, 0, 0);
                        Bundle bundle = new Bundle();
                        bundle.putString("traffic", traffic);
                        bundle.putString("GMKB", GMKB);
                        bundle.putString("info", info);
                        bundle.putInt("progress", idleprogress);
                        msg.setData(bundle);
                        msg.sendToTarget();
                    }
                    // 通用流量
                    if (Util.getUserPackage(mContext, simIndex).isEmpty()) {
                        //TODO 显示"本月已用"
                        info = this.getResources().getString(R.string.data_used_1);
                        // setTextAboveText(1, sr);
//                   sr2  = this.getResources().getString(R.string.data_used_2);
                        traffic = NetworkStatsHelper.byteToMBSize(usedData);//String.format(sr2, NetworkStatsHelper.byteToMBSize(usedData));
                        //  setProgressMain(0, info);
                        GMKB = NetworkStatsHelper.byteToGMK(usedData);
                        Message msg = uiHandler.obtainMessage(0, 0, 0);
                        Bundle bundle = new Bundle();
                        bundle.putString("traffic", traffic);
                        bundle.putString("info", info);
                        bundle.putString("GMKB", GMKB);
                        bundle.putInt("progress", 0);
                        msg.setData(bundle);
                        msg.sendToTarget();
                    } else {
                        if (packageData != 0) {
                            progress = (int) (((float) usedData / (float) packageData) * 100);
                            if (progress == 0 && usedData > 0) {
                                progress = 1;
                            }
                            if (progress >= 100) {
                                progress = 100;
                                info = this.getResources().getString(R.string.data_over_1);
//                             sr2 = this.getResources().getString(R.string.data_over_2);
                                GMKB = NetworkStatsHelper.byteToGMK(overData);
                                traffic = NetworkStatsHelper.byteToMBSize(overData);//String.format(sr2, NetworkStatsHelper.byteToMBSize(overData));
                            } else {
                                info = this.getResources().getString(R.string.data_residue_1);
                                // setBGColor("#000000");
//                             sr2 = this.getResources().getString(R.string.data_residue_2);
                                GMKB = NetworkStatsHelper.byteToGMK(resudiseData);
                                traffic = NetworkStatsHelper.byteToMBSize(resudiseData);//String.format(sr2, NetworkStatsHelper.byteToMBSize(resudiseData));
                            }
                            Log.i("Test:", "progress = " + progress + "usedData =" + usedData + "packageData =" + packageData);
                            Message msg = uiHandler.obtainMessage(0, 0, 0);
                            Bundle bundle = new Bundle();
                            bundle.putString("traffic", traffic);
                            bundle.putString("GMKB", GMKB);
                            bundle.putString("info", info);
                            bundle.putInt("progress", progress);
                            msg.setData(bundle);
                            msg.sendToTarget();
                        }
                    }
                }
            }

    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
