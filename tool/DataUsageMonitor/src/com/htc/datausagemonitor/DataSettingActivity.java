package com.htc.datausagemonitor;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.htc.datausagemonitor.fragment.DataRemiderFragment;
import com.htc.datausagemonitor.fragment.SetOperatorFragment;
import com.htc.datausagemonitor.fragment.SetPackageFragment;
import com.htc.datausagemonitor.fragment.SetUsedDataFragment;


public class DataSettingActivity extends AppCompatActivity {
    private View mBack;
    private TextView mTitle;
    private View mSettings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_setting);
        mBack = findViewById(R.id.back_btn);
        mTitle = (TextView) findViewById(R.id.title);
        mSettings = findViewById(R.id.settings_btn);
        mSettings.setVisibility(View.GONE);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Intent intent = getIntent();
        String name = intent.getCharSequenceExtra(Util.FRAG_NAME).toString();
        int sim = intent.getIntExtra(Util.SIM_NO,Util.FIRST_SIM_NO);
        initFragment(name,sim);
    }

/*    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }
    }*/

    private void initFragment(String fragName, int i){
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        Fragment fragment;
        Bundle bl = new Bundle(1);
        bl.putInt(Util.SIM_NO,i);
        Log.d("DataSettingActivity","fragName :" +fragName);
        switch (fragName) {
            case "change_usedData":
            {
                mTitle.setText(R.string.used_data_change_title);  //修改月使用流量
                fragment = new SetUsedDataFragment();
                fragment.setArguments(bl);
                fragmentTransaction.replace(R.id.data_set_content, fragment, null).commit();
                break;
            }
            case "data_reminder":
            {
                mTitle.setText(R.string.used_reminder_title); //流量提醒
                fragment = new DataRemiderFragment();
                fragment.setArguments(bl);
                fragmentTransaction.replace(R.id.data_set_content, fragment, null).commit();
                break;
            }
            case "set_data_package":
            {
                mTitle.setText(R.string.data_package); //流量套餐
                fragment = new SetPackageFragment();
                fragment.setArguments(bl);
                fragmentTransaction.replace(R.id.data_set_content, fragment, null).commit();
                break;
            }
            case "set_data_operator":
            {
                mTitle.setText(R.string.set_city_operator); //省市与运营商
                fragment = new SetOperatorFragment();
                fragment.setArguments(bl);
                fragmentTransaction.replace(R.id.data_set_content, fragment, null).commit();
                break;
            }
        }
    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
