package com.htc.datausagemonitor.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.NetworkStatsHelper;


public abstract class PagerBaseFragment extends Fragment implements OnClickListener{

    public static final String package_wave_color_normal = "#00b4ff";
    public static final String package_wave_color_over = "#ff0000";
    public static final String package_text_color_normal = "#265cae";  //文字颜色
    public static final String package_text_color_no = "#7f265cae";

    public static final String idle_wave_color_normal = "#00ffff";
    public static final String idle_wave_color_over = "#ff0000";
    public static final String idle_wave_color_over_no = "#7fff0000";
    public static final String idle_wave_color_no = "#7f00ffff";

    public static final  String custom_color="#D3D3D3";
    public static final String background_color = "#11FFFFFF";

    protected Context mContext;

    //负责处理上半部分显示已用/剩余流量部分
    ImageView imageView;

    CircleProgressView circleProgressbar;
    String temptest="mytest";
    RoundProgressBar roundProgressBar;
    public TextView maintraffic;
    public TextView maingmkb;
    public TextView maininfo;
    public TextView maintext;
    public TextView subtraffic;
    public TextView subgmkb;
    public TextView subinfo;
    public TextView subtext;

    private boolean Idle;
    private int currentidleprogress=0;
    public int idleprogressshow=0;
    public String trafficshow="0";
    private static final int INVALIDATE = 0X777;


    //闲时流量动态刷新
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case INVALIDATE:
                    if(currentidleprogress>=0) {
                        if (currentidleprogress < idleprogressshow) {
                            currentidleprogress++;
                            roundProgressBar.setProgress(currentidleprogress, trafficshow);
                            if (currentidleprogress < idleprogressshow)
                                sendEmptyMessageDelayed(INVALIDATE, 2); //第二个参数为延时时间
                        } else {
                            if (currentidleprogress > idleprogressshow) {
                                currentidleprogress--;
                                if (currentidleprogress >= 0)
                                    roundProgressBar.setProgress(currentidleprogress, trafficshow);
                                if (currentidleprogress > idleprogressshow)
                                    sendEmptyMessageDelayed(INVALIDATE, 2); //第二个参数为延时时间
                            } else if (currentidleprogress == idleprogressshow) {
                                roundProgressBar.setProgress(currentidleprogress, trafficshow);
                            }
                        }
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sim_pager, container, false);

        circleProgressbar = (CircleProgressView)view.findViewById(R.id.circleProgressbar);
        circleProgressbar.setOnClickListener(this);
        roundProgressBar=(RoundProgressBar)view.findViewById(R.id.roundProgressBar);
      //  roundProgressBar.setOnClickListener(this);
        roundProgressBar.bringToFront();

        maingmkb=(TextView)view.findViewById(R.id.maingmkb);
        maintraffic=(TextView)view.findViewById(R.id.maintraffic);
        maininfo=(TextView)view.findViewById(R.id.maininfo);
        TextPaint tp = maininfo.getPaint();
        tp.setFakeBoldText(true);
        maintext=(TextView)view.findViewById(R.id.maintext);
        subgmkb=(TextView)view.findViewById(R.id.subgmkb);
        subinfo=(TextView)view.findViewById(R.id.subinfo);
        TextPaint tpsub = subinfo.getPaint();
        tpsub.setFakeBoldText(true);
        subtraffic=(TextView)view.findViewById(R.id.subtraffic);
        subtext=(TextView)view.findViewById(R.id.subtext);

        Init();
        updateView();

        return view;
    }

    protected void updateView(){
        //Util sp =new Util(getContext());
/*        int color = getResources().getColor(R.color.colorPrimary);
        if(!sp.getIsDataMonitor()){
            color = getResources().getColor(android.R.color.darker_gray);
        }
        imageView.setBackgroundColor(color);*/
    }
    public void Vshow(int progress,String traffic){

        idleprogressshow=progress;
        trafficshow=traffic;
        handler.sendEmptyMessageDelayed(INVALIDATE,100);
    }

    //实时流量关闭时候
    public void setMainInit(String GMKB,String info,String traffic){

        circleProgressbar.setProgress(0);
        if(isAdded()) {
            if (maininfo.getText().equals(getResources().getString(R.string.data_over_1))) {
                maingmkb.setTextColor(Color.parseColor(idle_wave_color_over_no));
                maintraffic.setTextColor(Color.parseColor(idle_wave_color_over_no));
                maininfo.setTextColor(Color.parseColor(idle_wave_color_over_no));
            } else {
                maingmkb.setTextColor(Color.parseColor(package_text_color_no));
                maintraffic.setTextColor(Color.parseColor(package_text_color_no));
                maininfo.setTextColor(Color.parseColor(package_text_color_no));
            }
        }else{
            maingmkb.setTextColor(Color.parseColor(package_text_color_no));
            maintraffic.setTextColor(Color.parseColor(package_text_color_no));
            maininfo.setTextColor(Color.parseColor(package_text_color_no));
        }
    }
    //实时流量关闭时候 夜间
    public void setIdleInit(String GMKB,String info,String traffic,int flag){
        //  roundProgressBar.setCricleColor();
//        roundProgressBar.setTextColor(Color.parseColor(custom_color));
        roundProgressBar.setCricleProgressColor(Color.parseColor(idle_wave_color_no));   //闲时流量 没有 圈颜色
        if(flag==2){
//            roundProgressBar.setTextColor(Color.parseColor(idle_wave_color_no));
            if(isAdded()) {
                if (subinfo.getText().equals(getResources().getString(R.string.data_usage_null))) {
                    subgmkb.setTextColor(Color.parseColor(idle_wave_color_over_no));
                    subtraffic.setTextColor(Color.parseColor(idle_wave_color_over_no));
                    subinfo.setTextColor(Color.parseColor(idle_wave_color_over_no));
                } else {
                    subgmkb.setTextColor(Color.parseColor(idle_wave_color_no));
                    subtraffic.setTextColor(Color.parseColor(idle_wave_color_no));
                    subinfo.setTextColor(Color.parseColor(idle_wave_color_no));
                }
            }else{
                subgmkb.setTextColor(Color.parseColor(idle_wave_color_no));
                subtraffic.setTextColor(Color.parseColor(idle_wave_color_no));
                subinfo.setTextColor(Color.parseColor(idle_wave_color_no));
            }

        }
//        roundProgressBar.setidleTextInfo(info);
//        roundProgressBar.setidleTextGMKB(GMKB);
        roundProgressBar.setProgress(0,traffic);
      //  roundProgressBar.setTraffic(traffic);

    }
    private void Init() {
      //  circleProgressbar.setWave(1,10);
       // circleProgressbar.setWaveSpeed(1000);
       // circleProgressbar.setTextBottomText("通用流量");
      /*  circleProgressbar2.setWave(1,10);
        circleProgressbar2.setWaveSpeed(100);
        circleProgressbar2.setTextBottomText("闲时流量");
        circleProgressbar2.setText("#FFFFFF",40);
        circleProgressbar3.setWave(1,10);
        circleProgressbar3.setWaveSpeed(100);
        circleProgressbar3.setTextBottomText("通用流量");*/


       // circleProgressbar.setBGColor(background_color);   //TODO 里面的圈的背景色

    }
    public void setBGColor(String mBGColor) {
        circleProgressbar.setBGColor(mBGColor);
    }
    public void setidleTextInfo(String idleTextInfo) {
        roundProgressBar.setidleTextInfo(idleTextInfo);
    }
    public void setidleTextGMKB(String idleTextGMKB) {
        roundProgressBar.setidleTextGMKB(idleTextGMKB);
    }
    public void setVisible(boolean isIdle)
    {
        Idle=isIdle;
        if(isIdle){
          //  circleProgressbar.setVisibility(View.VISIBLE);
        }
        else{
          //  circleProgressbar.setVisibility(View.GONE);
        }

    }
    public void setTextGMKBText(String str){
            circleProgressbar.setTextGMKBText(str);
    }
    public void setTextInfoText(String str){
            circleProgressbar.setTextInfoText(str);
    }
    public void setMain(String traffic,String gmkb,String info,int progress){

        if(progress>=100){
            maingmkb.setTextColor(Color.parseColor(idle_wave_color_over));
            maintraffic.setTextColor(Color.parseColor(idle_wave_color_over));
            maininfo.setTextColor(Color.parseColor(idle_wave_color_over));
        }
        else{
            maingmkb.setTextColor(Color.parseColor(package_text_color_normal));
            maintraffic.setTextColor(Color.parseColor(package_text_color_normal));
            maininfo.setTextColor(Color.parseColor(package_text_color_normal));
        }
        maingmkb.setText(gmkb);
        maintraffic.setText(traffic);
        maininfo.setText(info);

    }
    public void setIdle(String traffic,String gmkb,String info,int progress){

        if(progress>=100){
            subgmkb.setTextColor(Color.parseColor(idle_wave_color_over));
            subtraffic.setTextColor(Color.parseColor(idle_wave_color_over));
            subinfo.setTextColor(Color.parseColor(idle_wave_color_over));
        }
        else if(progress==-1){
            subgmkb.setTextColor(Color.parseColor(idle_wave_color_no));
            subtraffic.setTextColor(Color.parseColor(idle_wave_color_no));
            subinfo.setTextColor(Color.parseColor(idle_wave_color_no));
        }
        else{
            subgmkb.setTextColor(Color.parseColor(idle_wave_color_normal));
            subtraffic.setTextColor(Color.parseColor(idle_wave_color_normal));
            subinfo.setTextColor(Color.parseColor(idle_wave_color_normal));
        }
        Log.d("mjtest","info = " +info);
        subgmkb.setText(gmkb);
        subtraffic.setText(traffic);
        subinfo.setText(info);

    }

    public void setidleProgress(int progress,String traffic) {

        idleprogressshow=progress;
        trafficshow=traffic;

       // roundProgressBar.setProgress(progress,traffic);
       if(progress>=100){
           roundProgressBar.setCricleProgressColor(Color.parseColor(idle_wave_color_over));   //闲时流量 超额 圈颜色
           roundProgressBar.setTextColor(Color.parseColor(idle_wave_color_over)); //闲时流量 超额  字体颜色
           //roundProgressBar.setCricleColor(Color.parseColor("#87ceeb"));    //圈背景色
        }else if(progress == -1){
           roundProgressBar.setCricleProgressColor(Color.parseColor(idle_wave_color_no));   //闲时流量 没有 圈颜色
           roundProgressBar.setTextColor(Color.parseColor(idle_wave_color_no)); //闲时流量 没有  字体颜色
       }
        else{
           roundProgressBar.setCricleProgressColor(Color.parseColor(idle_wave_color_normal)); //闲时流量 正常  圈进度条颜色  黄色
           roundProgressBar.setTextColor(Color.parseColor(idle_wave_color_normal)); //闲时流量 正常 字体颜色
        }
        handler.sendEmptyMessageDelayed(INVALIDATE,100);

    }
    public void setProgressMain(int progress,String info) {
            if(progress>=100){

               circleProgressbar.setWaveColor(package_wave_color_over);    //通用流量 超额 波浪颜色
               circleProgressbar.setTextColor(package_wave_color_over);     //通用流量 超额 字体颜色
             //   circleProgressbar.setBlankColor("#87ceeb");
            }
            else{
              //  progress=5;
                circleProgressbar.setWaveColor(package_wave_color_normal);   //通用流量 正常 波浪颜色
                circleProgressbar.setTextColor(package_text_color_normal);  //通用流量 正常 字体颜色
            }
            circleProgressbar.setCurrent(progress, info);
       // Log.d("circleProgressbar2","progress =" +progress);

    }



//    public void refreshText(){ //根据是否开始实时监控，显示不同的String
//        boolean isMonitor = dataSetting.getBoolean(Util.DATA_MONITOR,true);
//        if(isMonitor){
//            mEmptyText.setText(R.string.data_monitor_on_summary);
//        }else{
//            mEmptyText.setText(R.string.data_monitor_off_summary);
//        }
//    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        //if DataMonitor is on ,need startActivity
        if(Util.getIsDataMonitor(mContext)) {
            Intent intent1 = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
            ComponentName cName = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
            intent1.setComponent(cName);
            startActivity(intent1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        //refreshText();
    }
}
