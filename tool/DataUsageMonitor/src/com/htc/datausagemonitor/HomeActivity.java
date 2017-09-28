package com.htc.datausagemonitor;

import android.Manifest;
import android.app.ActivityManager;
import android.support.v4.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.app.ActivityCompat;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.datausagemonitor.data.DataWatcher;
import com.htc.datausagemonitor.data.DualSimManager;
import com.htc.datausagemonitor.data.MonitorData;
import com.htc.datausagemonitor.data.MonitorDataChange;
import com.htc.datausagemonitor.data.NetworkStatsHelper;
import com.htc.datausagemonitor.fragment.HomeFragForSingleSim;
import com.htc.datausagemonitor.fragment.HomeFragment;
import com.htc.datausagemonitor.fragment.MainSimFragment;
import com.htc.datausagemonitor.fragment.SecSimFragment;
import com.htc.datausagemonitor.fragment.WarningDialogDismissListener;
import com.htc.datausagemonitor.fragment.WarningDialogFragment;
import com.htc.datausagemonitor.trafficcorrection.TrafficCorrectionWrapper;

import java.util.ArrayList;
import java.util.Observable;

import tmsdk.common.ErrorCode;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String action = "android.intent.action.SDK";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private HomeFragment homeFragment;
    private boolean needWizard = true; //判断当前套餐和省市运营商是否未设置
    private FloatingActionButton fab;
    private View mBack;
    private TextView mTitle;
    private View mSettings;

    private Util util;
    private int mSimIndex;

    private FragmentManager manager;
    private FragmentTransaction transaction;
    private int flag=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        //util = new Util(this);
        //增加判斷 是否IMSI為保存信息的同一張卡，如果換卡，則直接清空保存信息 目前不用
        // NetworkStatsHelper.updateUtil(this, ConnectivityManager.TYPE_MOBILE);


        mSimIndex = Util.getSimIndex(this);
        homeFragment = new HomeFragment();

        mBack = findViewById(R.id.back_btn);
        mTitle = (TextView) findViewById(R.id.title);
        mSettings = findViewById(R.id.settings_btn);
        homeFragment.setCustomTitle(mTitle);

        mBack.setOnClickListener(this);
        mTitle.setText(R.string.app_name);
        mSettings.setOnClickListener(this);

        NeedAutoCorrection();
        //final Util eSp = new Util(this,mSimIndex);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                   .setAction("Action", null).show();*/
                //TODO 添加判断是否是Esim，如果是弹出Toast提示当前是Esim不支持校正
                int simIndex = homeFragment.getViewPagerSimIndex();
                Log.d(TAG,"getViewPagerSimInder = "+simIndex);
                if(Util.getIsEsimEnable(HomeActivity.this, simIndex) || NetworkStatsHelper.isVirtureSim(HomeActivity.this, simIndex)) {
                    Toast.makeText(getApplicationContext(), R.string.esim_toast_msg, Toast.LENGTH_LONG).show();
                }else {
                    if (checkNeedWizard(simIndex)) {
                            Intent intent = new Intent(HomeActivity.this, SubActivity.class);
                            intent.putExtra(Util.FRAG_NAME, "package_set_wizard");
                            intent.putExtra(Util.SIM_NO, simIndex); //需要判断是卡1还是卡2；
                            HomeActivity.this.startActivity(intent);
                        } else {
                            //直接校正
                            if ((System.currentTimeMillis() / 1000 - Util.getCorrectionTime(HomeActivity.this,1)) <= 5) {
                                Toast.makeText(getApplicationContext(), R.string.data_correct_frequent_operation, Toast.LENGTH_SHORT).show();
                            } else {
                                //需要考虑 simIndex
                                int retCode = TrafficCorrectionWrapper.getInstance().startCorrection(simIndex - 1);  //目前得到是1或者2,需要传给TMSDK 0或者1
                                Toast.makeText(getApplicationContext(), R.string.start_data_correct, Toast.LENGTH_SHORT).show();
                                Log.d(TAG,"simIndex =" +simIndex);
                                if (retCode != ErrorCode.ERR_NONE) {
                                    Toast.makeText(getApplicationContext(), R.string.data_correct_error, Toast.LENGTH_LONG).show();
                                } else {
                                    Util.setCorrectionTime(HomeActivity.this,1);
                                }
                            }

                        }
                    }
                   Util.setIsSimStateChanage(HomeActivity.this, false);  //校正之后设置换卡提示为false
                }
            });
            DualSimManager uuSimManager = DualSimManager.getInstance(Build.MODEL);
            if(uuSimManager.isMultiSimSupport(this)) {
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, homeFragment).commit();
            }else{
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, new HomeFragForSingleSim()).commit(); //如果是手机不支持双卡走下面
            }
            permssionCheck();

            showPermissonDialog();
            refreshUI();
            //add by zw
            if(!HTCApplication.mBresult) {
                //tmsdk初始化失败 ,可能是TMSDK通用so加载失败。
                new android.app.AlertDialog.Builder(this).setTitle(R.string.dialog_load_sdk_failed_title)
                        .setMessage(R.string.dialog_load_sdk_failed_msg)
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                HomeActivity.this.finish();
                            }
                        })
                        .show();
            }
            TrafficCorrectionWrapper.getInstance().init(getApplicationContext());


        }

    private void NeedAutoCorrection() {
        if(Util.getIsDataAutoCheck(this, mSimIndex)) {
            Log.d(TAG,"启动程序时候判断autocorrection");
            long autocorrectiontime = Util.getAutoCorrectionTime(this,Util.FIRST_SIM_NO,mSimIndex);
//        long autocorrectiontime1= util.getAutoCorrectionTime(simIndex);
            if (autocorrectiontime > 0 && autocorrectiontime< System.currentTimeMillis()) {
                //启动
                //  MyReceiver.mscheduleAlarms(getApplicationContext(),0);
            }
//        if(autocorrectiontime1>0 && autocorrectiontime1<System.currentTimeMillis()){
//            //启动
//            MyReceiver.mscheduleAlarms(getApplicationContext(),1);
//        }
        }
    }

    private boolean checkNeedWizard(int sim) {  //Check是否需要校正导向
        //Util sp = new Util(this,simIndex);
        String dataPackage = Util.getUserPackage(this, sim);
        ArrayList<String> name = Util.getOperatorName(this, sim);
        if(dataPackage.isEmpty() || name.get(0).isEmpty() || Util.isSimStateChange(this)){
            return true;
        }else {
            return false;
        }

    }

    private void permssionCheck() {

        if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "permission denied to READ_PHONE_STATE - requesting it");
            String[] permissions = {Manifest.permission.READ_PHONE_STATE};

            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
        if(checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "permission denied to READ_PHONE_STATE - requesting it");
            String[] permissions = {Manifest.permission.SEND_SMS};

            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }

        if(checkSelfPermission(Manifest.permission.RECEIVE_SMS)!= PackageManager.PERMISSION_GRANTED ){
            Log.d(TAG, "permission denied to READ_PHONE_STATE - requesting it");
            String[] permissions = {Manifest.permission.RECEIVE_SMS};

            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //用户使用短信，被拒绝
        String[] checkPermissions = {Manifest.permission.SEND_SMS};
        int[] result = {android.content.pm.PackageManager.PERMISSION_DENIED};
        if(permissions.equals(checkPermissions) && grantResults.equals(result)){
            String sr = getResources().getString(R.string.permission_toast_msg);
            String toast = String.format(sr,getResources().getString(R.string.toast_msg));
            Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
        }
    }

    private void showPermissonDialog(){  //权限提醒Dialog显示

            if(Util.getPrefHaveShowPermission(this,1)) {
                WarningDialogFragment fragment = new WarningDialogFragment();
                FragmentManager fm = getSupportFragmentManager();
                fragment.show(fm, "license");
                fragment.setOnDismissListener(new WarningDialogDismissListener() {
                    @Override
                    public void onDialogDismiss(boolean finishActivity) {
                        if (finishActivity) {
                            ActivityCompat.finishAfterTransition(HomeActivity.this);
                        } else {
                            Util.setPrefHaveShowPermission(HomeActivity.this,1, false);
                            if(!isWorked("com.htc.datausagemonitor.DataMonitorService")) {
                                if (Util.getIsDataMonitor(HomeActivity.this)) {
                                    Intent startIntent = new Intent(HomeActivity.this, DataMonitorService.class);
                                    startService(startIntent);
                                    Log.d(TAG, "start DataMonitorService");
                                }
                            }
                        }
                    }
                });
            }

        }

        private void refreshUI(){  //判断实时监控是否关闭，是否显示校正按钮,改变背景颜色
            if(Util.getIsDataMonitor(this)){
                //int color = Color.parseColor(String.valueOf(R.color.colorPrimary));
                //ColorDrawable drawable = new ColorDrawable(color);
               // getSupportActionBar().setBackgroundDrawable(drawable);
//                getApplication().setTheme(R.style.AppTheme);
                fab.setVisibility(View.VISIBLE);
            }else{
//                getApplication().setTheme(R.style.DataMonitor_OFF);
                fab.setVisibility(View.INVISIBLE);
                //int color = Color.parseColor(String.valueOf(android.R.color.darker_gray));
               // ColorDrawable drawable = new ColorDrawable(color);
               // getSupportActionBar().setBackgroundDrawable(drawable);
            }
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_home, menu);
            return true;
        }

/*        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            switch (item.getItemId()) {
                case android.R.id.home:
                    finish();
                    break;
                case R.id.action_settings:
                    Intent intent = new Intent(HomeActivity.this, SubActivity.class);
                    intent.putExtra(Util.FRAG_NAME, "action_setting");
                    startActivity(intent);
                    break;
                default:
                    break;
            }

            return super.onOptionsItemSelected(item);
        }*/

    @Override
    protected void onResume() {
        super.onResume();

        refreshUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.back_btn){
            ActivityCompat.finishAfterTransition(HomeActivity.this);
        }else if(id == R.id.settings_btn){
            Intent intent = new Intent(HomeActivity.this, SubActivity.class);
            intent.putExtra(Util.FRAG_NAME, "action_setting");
            startActivity(intent);
        }

    }

    private boolean isWorked(String className) {
        ActivityManager myManager = (ActivityManager) HomeActivity.this
                .getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(className)) {
                return true;
            }
        }
        return false;
    }
}
