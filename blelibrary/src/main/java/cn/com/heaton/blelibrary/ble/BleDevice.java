package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothDevice;

/**
 *
 * @author yzz
 * Created on 2017/10/30 14:31
 */

public class BleDevice {

    public final static String TAG = BleDevice.class.getSimpleName();

    /**
     * Is connected
     */
    private boolean isConnected = false;

    /**
     * is connectting
     */
    private boolean mConnecting = false;

    /**
     *  Connection Status:
     *  2503 Not Connected
     *  2504 Connecting
     *  2505 Connected
     */
    private int mConnectionState = BleConfig.BleStatus.DISCONNECT;

    /**
     *   Bluetooth address
     */
    private String mBleAddress;

    /**
     *  Bluetooth name
     */
    private String mBleName;
    /**
     *   Bluetooth modified name
     */
    private String mBleAlias;

    public BleDevice(BluetoothDevice device) {
        this.mBleAddress = device.getAddress();
        this.mBleName = device.getName();
    }

    public boolean isConnected() {
        return mConnectionState == BleConfig.BleStatus.CONNECTED;
    }

    public boolean isConnecting() {
        return mConnectionState == BleConfig.BleStatus.CONNECTING;
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public void setConnectionState(@BleConfig.BleStatus int state){
        mConnectionState = state;
    }


    public String getBleAddress() {
        return mBleAddress;
    }

    public void setBleAddress(String mBleAddress) {
        this.mBleAddress = mBleAddress;
    }

    public String getmBleName() {
        return mBleName;
    }

    public void setBleName(String mBleName) {
        this.mBleName = mBleName;
    }

    public String getBleAlias() {
        return mBleAlias;
    }

    public void setBleAlias(String mBleAlias) {
        this.mBleAlias = mBleAlias;
    }
}
