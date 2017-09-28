package com.htc.datausagemonitor.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.datausagemonitor.R;
import com.htc.datausagemonitor.Util;
import com.htc.datausagemonitor.data.NetworkStatsHelper;
import com.htc.datausagemonitor.trafficcorrection.TrafficCorrectionWrapper;

import java.util.ArrayList;
import java.util.List;

import tmsdk.bg.module.network.CodeName;
import tmsdk.common.ErrorCode;

/**
 * Created by majing on 17-7-24.
 */
//省市运营商设置

public class SetOperatorFragment extends Fragment implements View.OnClickListener{

    public static final String TAG = SetOperatorFragment.class.getSimpleName();
    private Bundle bl;
    private boolean isWizard;
    private TextView mBack;
    private TextView mComplete;
    private TextView mSave;
    private Button mProvince;
    private Button mCity;
    private Button mCarry;
    private Context mContext;
    private int mSim;
    private List<String> province_List;
    private List<String> city_list;
    private List<String> city_sub_list;
    private List<String> carry_list;
    private ArrayAdapter<String> arr_adapter_province;
    private ArrayAdapter<String> arr_adapter_city;
    private ArrayAdapter<String> arr_carry_brand;

    private ArrayList<CodeName> mProvinces;
    private ArrayList<CodeName> mCitysList;
    private ArrayList<CodeName> mCarrys;
    private ArrayList<CodeName> mBrands;
    private String mProvinceId, mCityId, mCarryId,mBrandId;
    private int mProvinceName,mCityName,mCarryName,mBrandName,mCarryBrandName;
    private String cityName,brandName;
    private int[] mcarrynum;
    private int tempcity;
    private int tempcarry;
    private int tempprovice;

    private int mSimIndex;





    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.set_operator_fragment,container,false);
        bl = getArguments();
        isWizard =bl.getBoolean(Util.IS_WIZARD); //判断是否是校正向导
        mSimIndex = bl.getInt(Util.SIM_NO, Util.FIRST_SIM_NO);
        Log.d(TAG,"mSimIndexmj =" +mSimIndex);
        mBack = (TextView)view.findViewById(R.id.back_button);
        mComplete = (TextView)view.findViewById(R.id.wizard_complete);
        mSave = (TextView)view.findViewById(R.id.set_operator_complete);
        mProvince = (Button)view.findViewById(R.id.Province);
        mCity= (Button)view.findViewById(R.id.city);
        mCarry=(Button)view.findViewById(R.id.carry);
        mContext=getActivity();
        //sp = new Util(context,mSim);
        initSpinner();
        if(isWizard){
            mSave.setVisibility(View.GONE);
            mComplete.setOnClickListener(this);
            mBack.setOnClickListener(this);
        }else{
            mBack.setVisibility(View.GONE);
            mComplete.setVisibility(View.GONE);
            mSave.setOnClickListener(this);
        }

        return view;
    }

    private void initSpinner(){
        int[] list = Util.getOperator(mContext, mSimIndex);
        city_list = new ArrayList<String>();
        carry_list=new ArrayList<String>();
        mProvinces = TrafficCorrectionWrapper.getInstance().getAllProvinces();
        mProvince.setText(mProvinces.get(list[0]).mName);
        ArrayList<String> name =Util.getOperatorName(mContext, mSimIndex);
        // mCity.setText(name.get(0));
        // mCarry.setText(name.get(1));
        mProvince.setOnClickListener(this);
        mCity.setOnClickListener(this);
        mCarry.setOnClickListener(this);
        province_List = new ArrayList<String>();
        String[] nItems = new String[mProvinces.size()];
        if(nItems.length>0) {
            mProvinceName = 0;
            mCityName=0; //理论上要再加判断
        }
        for(int i = 0; i < nItems.length; i++) {
            nItems[i] = mProvinces.get(i).mName;//各省份名称
            province_List.add(nItems[i]);
        }

        mCarrys = TrafficCorrectionWrapper.getInstance().getCarries();//返回运营商列表,CodeName格式如("CMCC","中国移动")
        mBrands=new ArrayList<CodeName>();
        nItems = new String[mCarrys.size()];
        mcarrynum=new int[mCarrys.size()];
        int index=0;
        int temp=0;

        for(int i = 0; i < nItems.length; i++) {
            Log.d(String.valueOf(i), mCarrys.get(i).mName);
            nItems[i] = mCarrys.get(i).mName;//各运营商名称
            ArrayList<CodeName> stemp=TrafficCorrectionWrapper.getInstance().getBrands(mCarrys.get(i).mCode);

            String[] nSubItems = new String[stemp.size()];
            if(nSubItems.length>0) {
                mCarryName=0;
                mBrandName = 0;
                brandName=stemp.get(0).mName;
            }
            mcarrynum[i]=temp+nSubItems.length;
            temp= mcarrynum[i];
            for(int j=0;j<nSubItems.length;j++){

                Log.d(String.valueOf(j), stemp.get(j).mName);
                mBrands.add(stemp.get(j));
                nSubItems[j]=stemp.get(j).mName;
                carry_list.add(index,nSubItems[j]);
                index++;
            }
        }
        //设置默认值
        mCitysList = TrafficCorrectionWrapper.getInstance().getCities(mProvinces.get(list[0]).mCode);//城市列表

        nItems = new String[mCitysList.size()];
        for(int i = 0; i < nItems.length; i++) {
            nItems[i] = mCitysList.get(i).mName;//各省份名称
            city_list.add(nItems[i]);
        }
        mCity.setText(mCitysList.get(list[1]).mName);
        mCarry.setText(mBrands.get(list[4]).mName);
        cityName = mCitysList.get(list[1]).mName;
        brandName = mBrands.get(list[4]).mName;

        //默认
        mProvinceId=mProvinces.get(list[0]).mCode;
        mCityId = mCitysList.get(list[1]).mCode;//SIM卡所属城市ID
        mCarryId = mCarrys.get(list[2]).mCode;//运营商
        mBrandId=mBrands.get(list[3]).mCode;
        mProvinceName =list[0];
        mCityName=list[1];
        mCarryName =list[2];
        mBrandName =list[3];
        mCarryBrandName = list[4];
        tempprovice = list[0];
        tempcity=list[1];
        tempcarry = list[4];
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.back_button: {
                backPackageSet();
                break;
            }
            case R.id.wizard_complete: {
                setComplete();
                break;
            }
            case R.id.set_operator_complete: {
                setComplete();
                break;
            }
            case R.id.Province: {
                showAlertDialog(province_List, 1);

                break;
            }
            case R.id.city: {
                showAlertDialog(city_list, 2);

                break;
            }
            case R.id.carry: {
                showAlertDialog(carry_list, 3);

                break;
            }
        }
    }

    private void showAlertDialog(List<String> list,int i) {
                /*
         * 设置单选items
         * */
        final int index=i;
        int selectionindex=0;
        int[] l = Util.getOperator(mContext,mSimIndex);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);//内部使用构建者的设计模式
        arr_adapter_province= new ArrayAdapter<String>(getActivity(), R.layout.dialog_operator, list);
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View mTitleView = layoutInflater.inflate(R.layout.alertdialog_title, null);
        TextView dialogtitle= (TextView) mTitleView.findViewById(R.id.txtPatient);
        builder.setCustomTitle(mTitleView);
        if(i == 1) {
            dialogtitle.setText(R.string.set_Provinces_City);
            selectionindex=tempprovice;
        }else if(i == 2){
            dialogtitle.setText(R.string.set_Provinces_City);
            selectionindex=tempcity;
        }else{
            dialogtitle.setText(R.string.set_carry);
            //selectionindex=l[4];  //TODO
            selectionindex=tempcarry;
        }

        builder.setSingleChoiceItems(arr_adapter_province, selectionindex,new DialogInterface.OnClickListener() {//第二个参数是设置默认选中哪一项-1代表默认都不选

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                //Toast.makeText(MainActivity.this, items[which], 0).show();

                if(index==1){
                    city_list.clear();
                    mCitysList = TrafficCorrectionWrapper.getInstance().getCities(mProvinces.get(which).mCode);//城市列表
                    String[] nItems = new String[mCitysList.size()];
                    for(int i = 0; i < nItems.length; i++) {
                        nItems[i] = mCitysList.get(i).mName;//各省份名称
                        city_list.add(nItems[i]);
                    }
                    mProvinceId=mProvinces.get(which).mCode;
                    mProvince.setText(province_List.get(which));  //button 上的字要更新
                    mProvinceName = which;

                    mCityId = mCitysList.get(0).mCode;//SIM卡所属城市ID
                    mCity.setText(city_list.get(0));  //button 上的字要更新
                    cityName = city_list.get(0);
                    mCityName = 0;
                    tempprovice = which;
                    tempcity=0;
                    // tempcarry=0;
                }
                else if(index==2){
                    mCityId = mCitysList.get(which).mCode;//SIM卡所属城市ID
                    mCity.setText(city_list.get(which));  //button 上的字要更新
                    cityName = city_list.get(which);
                    mCityName = which;
                    tempcity = which;
                }
                else{
                    int s=0;
                    int j=0;
                    int i=which;
                    for(int x=0;x<mcarrynum.length;x++)
                    {
                        j=i-s;
                        s=mcarrynum[x];
                        if(i<mcarrynum[x]){
                            mCarryId = mCarrys.get(x).mCode;//运营商
                            mBrandId=mBrands.get(i).mCode;
                            mCarryName = x;
                            mBrandName = j;
                            mCarryBrandName=i;
                            brandName = mBrands.get(i).mName;
                            break;
                        }
                    }
                    mCarry.setText(carry_list.get(which));  //button 上的字要更新
                    mCarryBrandName = which;
                    tempcarry = which;
                }

                //更新CityList ，update City Button上的字
                dialog.dismiss();
            }
        });

        builder.create().show();//创建对象
    }

    private void setComplete(){
        Log.d("SetOperatorFragment","cityname =" + cityName  + "brandname =" +brandName);
        Util.setOperator(mContext, mSimIndex, mProvinceName, mCityName, mCarryName, mBrandName, mCarryBrandName);
        Util.setOperatorName(mContext, mSimIndex, cityName, brandName);
        //需要保存数据,并开启校正
        Log.d("SetOperatorFragment","setOperator ProvinceName:" +mProvinceName +"CityName:" +mCityName +
                "CarryName:" +mCarryName +"BrandName:"+mBrandName);
        int result = TrafficCorrectionWrapper.getInstance().setConfig(
                mSimIndex-1,
                mProvinceId,
                mCityId,
                mCarryId,
                mBrandId,
                1);//保存配置。在进行流量校正之前，必要进行设置。返回ErrorCode ,代码里的SIMIndex是1和2,需要传给TMSDK 0和1
        if(result != ErrorCode.ERR_NONE) {
            Toast.makeText(mContext, R.string.data_correct_error, Toast.LENGTH_LONG).show();
            Log.i(TAG,"set config error : "+result);
        }
        else{
            if(Util.getIsEsimEnable(mContext, mSimIndex) || NetworkStatsHelper.isVirtureSim(mContext, mSimIndex)) {
                Log.i(TAG,"isEsim or virSIM");
            }else {
                int retCode = TrafficCorrectionWrapper.getInstance().startCorrection(mSimIndex - 1);
                Toast.makeText(mContext, R.string.start_data_correct, Toast.LENGTH_SHORT).show();
                if (retCode != ErrorCode.ERR_NONE) {
                    Toast.makeText(mContext, R.string.data_correct_error, Toast.LENGTH_LONG).show();

                }
            }

        }

        getActivity().finish();
    }

/*    private void setSave(){
        //需要保存数据
        sp.setOperator(mProvinceName,mCityName,mCarryName,mBrandName,mCarryBrandName);
        int result = TrafficCorrectionWrapper.getInstance().setConfig(
                mSim,
                mProvinceId,
                mCityId,
                mCarryId,
                mBrandId,
                1);//保存配置。在进行流量校正之前，必要进行设置。返回ErrorCode
        if(result != ErrorCode.ERR_NONE) {
            Toast.makeText(context, "set config error : "+result, Toast.LENGTH_LONG).show();
        }
        
        getActivity().finish();
    }*/

    private void backPackageSet(){

        getFragmentManager().beginTransaction().remove(this).commit();
        SetPackageFragment setPackageFragment;
        setPackageFragment= (SetPackageFragment) getFragmentManager().findFragmentByTag("SetPackageFragment");
        getFragmentManager().beginTransaction().show(setPackageFragment).commit();
    }
}
