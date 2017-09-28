package com.htc.datausagemonitor.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.DataWatcher;
import com.htc.datausagemonitor.data.MonitorData;
import com.htc.datausagemonitor.data.MonitorDataChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class HomeFragment extends Fragment {

    private TabLayout tabs;
    private ViewPager viewPager;
    private TextView mEmptyText;
    private ImageView mIcon;
    private SharedPreferences dataSetting;
    private DataWatcher dataWatcher;
    public  MyPagerAdapter adapter;
    private SettingBaseFragment settingBaseFragment;

    private  Context mContext;
    private TextView mtitle;
    public int simIndex =1;
    private Message message;
    private final int MSG_REFRESH_VIEWPAGER = 0x4a;
    private final int MSG_UPDATE_DATA = 0x4b;

    private Handler mHandler=new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_REFRESH_VIEWPAGER:
                    refreshViewPage(msg.arg1);
                    break;
                case MSG_UPDATE_DATA:
                    updateFragData(msg.arg1);
                default:
                    break;
            }
        }

    };
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_pager_with_tab, container, false);
        mContext = getActivity();
        //sp=new Util((Context) getActivity());
        mIcon =(ImageView)view.findViewById(R.id.sim_icon);
        viewPager = (ViewPager)view.findViewById(R.id.viewpager);
        mEmptyText = (TextView)view.findViewById(R.id.text_monitor);
        dataSetting = mContext.getSharedPreferences(Util.SP_NAME_SETTING, Context.MODE_PRIVATE);
        settingBaseFragment = (SettingBaseFragment)getChildFragmentManager().findFragmentById(R.id.setting_content);
        MainSimFragment m1=new MainSimFragment();
        SecSimFragment s1=new SecSimFragment();
        List<Fragment> fragments=new ArrayList<Fragment>();

        simIndex = Util.getSimIndex(mContext);
        adapter=new MyPagerAdapter(getChildFragmentManager());
        adapter.addFragment(m1,"");
        adapter.addFragment(s1,"");
      //  FragmentPagerAdapter adapter = new FragmentPagerAdapter(etChildFragmentManager(), fragments, container.getContext());
      /*  Vector<Pair<Integer, Class<? extends Fragment>>>  fragments = new Vector<Pair<Integer, Class<? extends Fragment>>>();
        fragments.add(new Pair<Integer, Class<? extends Fragment>>(R.string.sim_frist_name, MainSimFragment.class));
        fragments.add(new Pair<Integer, Class<? extends Fragment>>(R.string.sim_sec_name, SecSimFragment.class));
*/

        viewPager.setAdapter(adapter);
        //tabs.setupWithViewPager(viewPager);

        if(simIndex ==1){
            viewPager.setCurrentItem(0);
            mIcon.setBackgroundResource(R.drawable.safehome_icon_second_slot1);
            message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,1,0);
        }else{
            viewPager.setCurrentItem(1);
            mIcon.setBackgroundResource(R.drawable.safehome_icon_second_slot2);
            message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,2,0);
        }
        message.sendToTarget();
/*        tabs.getTabAt(0).setIcon(R.drawable.icon_launcher_sim_1);
        tabs.getTabAt(1).setIcon(R.drawable.icon_launcher_sim_2);*/
        refreshText();
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
/*                if (position == 0) {
                    mIcon.setBackgroundResource(R.drawable.safehome_icon_second_slot1);
                    message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,1,0);
                }else {
                    mIcon.setBackgroundResource(R.drawable.safehome_icon_second_slot2);
                    message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,2,0);
                }
                message.sendToTarget();*/
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mIcon.setBackgroundResource(R.drawable.safehome_icon_second_slot1);
                    message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,1,0);
                }else {
                    mIcon.setBackgroundResource(R.drawable.safehome_icon_second_slot2);
                    message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,2,0);
                }
                message.sendToTarget();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
       /* tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {  //TODO 增加 TAB 1 2图标
                if (tabs.getTabAt(0).isSelected()) {
                    viewPager.setCurrentItem(0);
                    tabs.getTabAt(0).setIcon(R.drawable.safehome_icon_second_slot1);
                    message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,1,0);
                }else {
                    viewPager.setCurrentItem(1);
                    tabs.getTabAt(0).setIcon(R.drawable.safehome_icon_second_slot2);
                    message = mHandler.obtainMessage(MSG_REFRESH_VIEWPAGER,2,0);
                }
                message.sendToTarget();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
*/

        dataWatcher = new DataWatcher(){
            @Override
            public void update(Observable observable, Object data) {
                super.update(observable, data);
                //观察者接受到被观察者的通知，来更新自己的数据操作。
                MonitorData mData = (MonitorData) data;
                if(Util.getSimIndex(mContext) == Util.FIRST_SIM_NO){
                    MainSimFragment s=(MainSimFragment)adapter.getItem(0);
                    s.updateUI();
                }else{
                    SecSimFragment s=(SecSimFragment)adapter.getItem(1);
                    s.updateUI();
                }
                Log.i("Test", "mData---->>"+mData.getUsedDataChange());
            }
        };

        return view;
    }

    public void refreshViewPage(int i){
        Log.d("testmjmjmj","simindex =" +i);
        if(i == 1) {
//            tabs.getTabAt(0).getIcon().setAlpha(127);
//            tabs.getTabAt(1).getIcon().setAlpha(102);
            if(isAdded()) {
                String title = getResources().getString(R.string.app_name) + "(SIM1)";
                mtitle.setText(title);
            }
            simIndex = 1;
            settingBaseFragment.setData(simIndex);
            MainSimFragment s=(MainSimFragment)adapter.getItem(0);
            s.updateUI();
        }else{
//            tabs.getTabAt(0).getIcon().setAlpha(102);
//            tabs.getTabAt(1).getIcon().setAlpha(127);
            if(isAdded()) {
                String title = getResources().getString(R.string.app_name) + "(SIM2)";
                mtitle.setText(title);
            }
            simIndex = 2;
            settingBaseFragment.setData(simIndex);
            SecSimFragment s=(SecSimFragment)adapter.getItem(1);
            s.updateUI();
        }
    }

    private void updateFragData(int i){
        settingBaseFragment.setData(i);
    }

    public void refreshText(){ //根据是否开始实时监控，显示不同的String
        boolean isMonitor = dataSetting.getBoolean(Util.DATA_MONITOR,true);
        if(isMonitor){
            mEmptyText.setText(R.string.data_monitor_on_summary);
        }else{
            mEmptyText.setText(R.string.data_monitor_off_summary);
        }
    }

    public void setCustomTitle(TextView view){
        mtitle = view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshText();
        if(Util.getIsDataMonitor(mContext)) {
            message = mHandler.obtainMessage(MSG_UPDATE_DATA,simIndex,0);
            MonitorDataChange.getInstance().addObserver(dataWatcher);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(Util.getIsDataMonitor(mContext)) {
             MonitorDataChange.getInstance().deleteObserver(dataWatcher);
        }
    }

    public int getViewPagerSimIndex(){
        return simIndex;
    }
}
