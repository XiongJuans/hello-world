/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/xiongjuan/code/WorldClock_app_apps_WorldClock_9.50_api23_ForO80/WorldClock_app_apps_WorldClock_9.50_api23_ForO80/app/packages/apps/WorldClock/src/com/htc/android/worldclock/aiservice/AiClentInterface.aidl
 */
package com.htc.android.worldclock.aiservice;
// Declare any non-default types here with import statements

public interface AiClentInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.htc.android.worldclock.aiservice.AiClentInterface
{
private static final java.lang.String DESCRIPTOR = "com.htc.android.worldclock.aiservice.AiClentInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.htc.android.worldclock.aiservice.AiClentInterface interface,
 * generating a proxy if needed.
 */
public static com.htc.android.worldclock.aiservice.AiClentInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.htc.android.worldclock.aiservice.AiClentInterface))) {
return ((com.htc.android.worldclock.aiservice.AiClentInterface)iin);
}
return new com.htc.android.worldclock.aiservice.AiClentInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_setSkipAlarms:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _arg1;
_arg1 = data.readLong();
boolean _result = this.setSkipAlarms(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setEarlyEventAlarm:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
boolean _result = this.setEarlyEventAlarm(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getVersion:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.getVersion();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_queryRegularSkipAlarm:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _arg1;
_arg1 = data.readLong();
boolean _result = this.queryRegularSkipAlarm(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getEarliestAlarmTime:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _arg1;
_arg1 = data.readLong();
long _result = this.getEarliestAlarmTime(_arg0, _arg1);
reply.writeNoException();
reply.writeLong(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.htc.android.worldclock.aiservice.AiClentInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
//skip regular alarms between startSkipTime and endSkipTime

@Override public boolean setSkipAlarms(long startSkipTime, long endSkipTime) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(startSkipTime);
_data.writeLong(endSkipTime);
mRemote.transact(Stub.TRANSACTION_setSkipAlarms, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//set a early event alarm by time

@Override public boolean setEarlyEventAlarm(long time) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(time);
mRemote.transact(Stub.TRANSACTION_setEarlyEventAlarm, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//get Ai version

@Override public long getVersion() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getVersion, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//query whether exists regular alarm need skip between skip start date and skip end date

@Override public boolean queryRegularSkipAlarm(long skipStartTime, long endSkipTime) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(skipStartTime);
_data.writeLong(endSkipTime);
mRemote.transact(Stub.TRANSACTION_queryRegularSkipAlarm, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
//get earliest regular alarm time by query date

@Override public long getEarliestAlarmTime(long queryStartTime, long queryEndTime) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(queryStartTime);
_data.writeLong(queryEndTime);
mRemote.transact(Stub.TRANSACTION_getEarliestAlarmTime, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_setSkipAlarms = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_setEarlyEventAlarm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_queryRegularSkipAlarm = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getEarliestAlarmTime = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
//skip regular alarms between startSkipTime and endSkipTime

public boolean setSkipAlarms(long startSkipTime, long endSkipTime) throws android.os.RemoteException;
//set a early event alarm by time

public boolean setEarlyEventAlarm(long time) throws android.os.RemoteException;
//get Ai version

public long getVersion() throws android.os.RemoteException;
//query whether exists regular alarm need skip between skip start date and skip end date

public boolean queryRegularSkipAlarm(long skipStartTime, long endSkipTime) throws android.os.RemoteException;
//get earliest regular alarm time by query date

public long getEarliestAlarmTime(long queryStartTime, long queryEndTime) throws android.os.RemoteException;
}
