package com.htc.datausagemonitor;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.htc.datausagemonitor.fragment.DataSettingsFragment;
import com.htc.datausagemonitor.fragment.SetPackageFragment;


/**
 * Created by mj on 2017/7/23.
 */

public class SubActivity extends AppCompatActivity {
    private View mBack;
    private TextView mTitle;
    private View mSettings;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sub_activity);
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
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initFragment(String fragName, int i){
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        Bundle bl = new Bundle(1);
        bl.putInt(Util.SIM_NO,i);
        Log.d("SubActivity","fragName :" +fragName);
        switch (fragName) {
            case "package_set_wizard": {
                bl.putBoolean(Util.IS_WIZARD,true);
                mTitle.setText(R.string.correction_wizard);  //校正向导
                SetPackageFragment fragment = new SetPackageFragment();
                fragment.setArguments(bl);
                fragmentTransaction.replace(R.id.sub_content, fragment, "SetPackageFragment").commit();
                break;
            }
            case "action_setting" :
            {
                mTitle.setText(R.string.data_monitor_settings); //流量监控设置
                fragmentTransaction.replace(R.id.sub_content, new DataSettingsFragment(), null).commit();
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
