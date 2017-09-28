package com.htc.datausagemonitor.data;

/**
 * Created by majing on 17-8-7.
 */

public class MonitorData {

    private long usedDataChange;
    private long resudiseDataChange;
    private long overDataChange;
    private long idleUsedChange;
    private long idleResChange;

    public MonitorData() {
    }

    public long getUsedDataChange() {
        return usedDataChange;
    }

    public long getResudiseDataChange(){
        return  resudiseDataChange;
    }

    public long getOverDataChange(){
        return  overDataChange;
    }

    public long getIdleUsedChange(){
        return  idleUsedChange;
    }

    public long getIdleResChange(){
        return idleResChange;
    }

    public void setDataChange(long usedDataChange, long resudiseDataChange, long overDataChange,
                              long idleUsedChange, long idleResChange) {
        this.usedDataChange = usedDataChange;
        this.resudiseDataChange = resudiseDataChange;
        this.overDataChange = overDataChange;
        this.idleUsedChange = idleUsedChange;
        this.idleResChange = idleResChange;
    }
}
