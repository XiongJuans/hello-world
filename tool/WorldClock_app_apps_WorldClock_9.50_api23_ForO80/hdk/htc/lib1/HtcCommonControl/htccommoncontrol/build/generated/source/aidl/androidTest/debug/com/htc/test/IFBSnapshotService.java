/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/xiongjuan/code/WorldClock_app_apps_WorldClock_9.50_api23_ForO80/WorldClock_app_apps_WorldClock_9.50_api23_ForO80/hdk/htc/lib1/HtcCommonControl/htccommoncontrol/src/androidTest/aidl/com/htc/test/IFBSnapshotService.aidl
 */
package com.htc.test;
public interface IFBSnapshotService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.htc.test.IFBSnapshotService
{
private static final java.lang.String DESCRIPTOR = "com.htc.test.IFBSnapshotService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.htc.test.IFBSnapshotService interface,
 * generating a proxy if needed.
 */
public static com.htc.test.IFBSnapshotService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.htc.test.IFBSnapshotService))) {
return ((com.htc.test.IFBSnapshotService)iin);
}
return new com.htc.test.IFBSnapshotService.Stub.Proxy(obj);
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
case TRANSACTION_takeDefaultSnapShotByBitmap:
{
data.enforceInterface(DESCRIPTOR);
android.graphics.Bitmap _result = this.takeDefaultSnapShotByBitmap();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_takeSnapShotByBitmap:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
android.graphics.Bitmap _result = this.takeSnapShotByBitmap(_arg0, _arg1);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_saveDefaultSnapShot:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.saveDefaultSnapShot(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_saveSnapShot:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
java.lang.String _result = this.saveSnapShot(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_compareBitmapEqualBefore:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.Map _result = this.compareBitmapEqualBefore(_arg0);
reply.writeNoException();
reply.writeMap(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.htc.test.IFBSnapshotService
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
@Override public android.graphics.Bitmap takeDefaultSnapShotByBitmap() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.graphics.Bitmap _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_takeDefaultSnapShotByBitmap, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.graphics.Bitmap.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public android.graphics.Bitmap takeSnapShotByBitmap(int minLayer, int maxLayer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.graphics.Bitmap _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(minLayer);
_data.writeInt(maxLayer);
mRemote.transact(Stub.TRANSACTION_takeSnapShotByBitmap, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.graphics.Bitmap.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String saveDefaultSnapShot(java.lang.String name) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(name);
mRemote.transact(Stub.TRANSACTION_saveDefaultSnapShot, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String saveSnapShot(java.lang.String name, int minLayer, int maxLayer) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(name);
_data.writeInt(minLayer);
_data.writeInt(maxLayer);
mRemote.transact(Stub.TRANSACTION_saveSnapShot, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* add compare View */
@Override public java.util.Map compareBitmapEqualBefore(java.lang.String name) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.Map _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(name);
mRemote.transact(Stub.TRANSACTION_compareBitmapEqualBefore, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readHashMap(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_takeDefaultSnapShotByBitmap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_takeSnapShotByBitmap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_saveDefaultSnapShot = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_saveSnapShot = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_compareBitmapEqualBefore = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public android.graphics.Bitmap takeDefaultSnapShotByBitmap() throws android.os.RemoteException;
public android.graphics.Bitmap takeSnapShotByBitmap(int minLayer, int maxLayer) throws android.os.RemoteException;
public java.lang.String saveDefaultSnapShot(java.lang.String name) throws android.os.RemoteException;
public java.lang.String saveSnapShot(java.lang.String name, int minLayer, int maxLayer) throws android.os.RemoteException;
/* add compare View */
public java.util.Map compareBitmapEqualBefore(java.lang.String name) throws android.os.RemoteException;
}
