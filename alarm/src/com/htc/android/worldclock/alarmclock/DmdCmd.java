package com.htc.android.worldclock.alarmclock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import com.htc.lib0.htcdebugflag.HtcWrapHtcDebugFlag;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class DmdCmd {
    private static final String TAG = "WorldClock.DmdCmd";
    private static final boolean DEBUG_FLAG = HtcWrapHtcDebugFlag.Htc_DEBUG_flag;
    private static final String FLASH_WRITE = ":XCMD:ALARM:WRITE:0132:";
    private String CLOCKD_CLIENT_SOCKET_PATH = "/dev/socket/clockd";
    private LocalSocket mRequestSocket;
    private boolean mConn;
    private OutputStream mOut;
    private InputStream mIn;

    public DmdCmd() {
        mRequestSocket = null;
        mConn = false;
        Log.i(TAG, "This version is general off mode alarm function");
    }

    public DmdCmd(String newHostname, int newPort) {
        mRequestSocket = null;
        mConn = false;
    }

    public boolean Conn() {
        if(DEBUG_FLAG) Log.d(TAG, "Conn: connect to server");
        try {
            mConn = false;
            // creating a socket to connect to the server
            mRequestSocket = new LocalSocket();
            LocalSocketAddress localSocketAddr = new LocalSocketAddress(CLOCKD_CLIENT_SOCKET_PATH, LocalSocketAddress.Namespace.FILESYSTEM);
            mRequestSocket.connect(localSocketAddr);
            if (mRequestSocket == null) {
                // New socket failure.
                return false;
            }
            // get Output streams.
            mOut = mRequestSocket.getOutputStream();
            mOut.flush();
            // get Input streams.
            mIn = mRequestSocket.getInputStream();
            mConn = true;
        } catch (UnknownHostException unknownHost) {
            // You are trying to connect to an unknown host.
            mConn = false;
        } catch (IOException e) {
            Log.w(TAG, "Conn: fail e = " + e.toString());
        }
        return mConn;
    }

    public void DisConn() {
        if(DEBUG_FLAG) Log.d(TAG, "DisConn: disconnect from server");
        try {
            if (mRequestSocket == null) {
                return;
            }
            mIn.close();
            mOut.close();
            mRequestSocket.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private String Command(String outstring) {
        String result;
        byte[] respond = new byte[512];

        if (mConn == false) {
            return null;
        }

        try {
            mOut.write(outstring.getBytes());
            mOut.flush();
            mIn.read(respond);
        } catch (IOException e) {
            Log.w(TAG, "Command: fail e = " + e.toString());
            mConn = false;
            DisConn();
        }

        if (mConn == true) {
            result = new String(respond).trim();
            return result;
        } else {
            return null;
        }
    }

    private String Command(byte[] output) {
        String result;
        byte[] respond = new byte[512];

        if (mConn == false) {
            return null;
        }

        try {
            mOut.write(output);
            mOut.flush();
            mIn.read(respond);
        } catch (IOException e) {
            Log.w(TAG, "Command: fail e = " + e.toString());
            mConn = false;
            DisConn();
        }

        if (mConn == true) {
            result = new String(respond).trim();
            return result;
        } else {
            return null;
        }
    }

    public String BinToHex(byte[] bytes) {
        StringBuffer rslt = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            StringBuffer s = new StringBuffer(Integer.toHexString(bytes[i] >= 0 ? bytes[i] : 256 + bytes[i]));
            if (s.length() == 1) {
                s.insert(0, "0");
            }
            rslt.append(s.charAt(0));
            rslt.append(s.charAt(1));
        }
        return rslt.toString();
    }

    public String HextoBin(String input) {
        String digital = "0123456789ABCDEF";
        char[] hex2char = input.toCharArray();
        byte[] bytes = new byte[input.length() / 2];
        int temp;
        for (int i = 0; i < bytes.length; i++) {
            temp = digital.indexOf(hex2char[2 * i]) * 16;
            temp += digital.indexOf(hex2char[(2 * i) + 1]);
            bytes[i] = (byte) (temp & 0xff);
        }
        String Ret = new String(bytes);
        return Ret;
    }

    public String writeData(String data) {
        if(DEBUG_FLAG) Log.d(TAG, "writeData: data = " + data);
        return Command(FLASH_WRITE + BinToHex(data.getBytes()));
    }

    public String writeData(byte[] data) {
        if(DEBUG_FLAG) Log.d(TAG, "writeData: write byte data");

        Long[] longHeader = { 0x55AA55AAL, 1L, 120L };
        byte[] byteHeader = AlarmUtils.convertLongToByte(longHeader);

        int cmdLength = FLASH_WRITE.getBytes().length;
        int headerLength = byteHeader.length;
        int dataLength = data.length;

        byte[] dst = new byte[cmdLength + headerLength + dataLength];
        System.arraycopy(FLASH_WRITE.getBytes(), 0, dst, 0, cmdLength);
        System.arraycopy(byteHeader, 0, dst, cmdLength, headerLength);
        System.arraycopy(data, 0, dst, cmdLength + headerLength, dataLength);

        StringBuffer logData = new StringBuffer("");
        for (int i = cmdLength; i < dst.length; i++) {
            int res = dst[i] & 0xFF; // for unsigned data

            logData.append("res[")
                .append(i)
                .append("] = ")
                .append(res)
                .append("<")
                .append(Long.toHexString(res))
                .append("> ,");
        }
        if(DEBUG_FLAG) Log.d(TAG, "writeData: logData = " + logData.toString());
        return Command(dst);
    }
}
