package com.htc.datausagemonitor.data;

import android.util.Log;

import java.util.Observable;

/**
 * Created by majing on 17-8-7.
 */

public class MonitorDataChange extends Observable {
    private static MonitorDataChange instance = null;

    public static MonitorDataChange getInstance() {
        if (null == instance) {
            instance = new MonitorDataChange();
        }
        return instance;
    }

    public void notifyDataChange(MonitorData data) {
        //被观察者怎么通知观察者数据有改变了呢？？这里的两个方法是关键。
        setChanged();
        notifyObservers(data);
        Log.d("MonitorDataChange","notifyDataChange");
    }
}
