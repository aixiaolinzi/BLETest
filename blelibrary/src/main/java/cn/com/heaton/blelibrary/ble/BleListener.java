package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;


/**
 * Monitoring of BLE distribution network
 * @author yzz
 * Created on 2017/10/30 14:31
 */

public abstract class BleListener {

    /**
     * Start the scan
     */
    public void onStart() {
    }

    ;

    /**
     * Stop scanning
     */
    public void onStop() {
    }

    ;

    /**
     * Scan to device
     *
     * @param device     ble device object
     * @param rssi       rssi
     * @param scanRecord Bluetooth radio package
     */
    public abstract void onLeScan(BleDevice device, int rssi, byte[] scanRecord);
    
    /**
     * When the MCU returns the data read
     *
     * @param device ble device object
     */
    public void onRead(BluetoothDevice device) {
    }

    /**
     * MCU data sent to the app when the data callback call is setNotify
     *
     * @param characteristic characteristic
     */
    public void onChanged(BluetoothGattCharacteristic characteristic) {
    }

    /**
     * Set the notification feature to be successful and can send data
     *
     * @param device ble device object
     */
    public void onReady(BluetoothDevice device) {
    }

    /**
     * Set the notification here when the service finds a callback       setNotify
     *
     * @param gatt gatt
     */
    public void onServicesDiscovered(BluetoothGatt gatt) {
    };

    /**
     * The callback is disconnected or connected when the connection is changed
     *
     * @param device ble device object
     */
    public abstract void onConnectionChanged(BleDevice device);

    /**
     * The notification describes when the write succeeded
     *
     * @param gatt gatt
     */
    public void onDescriptorWriter(BluetoothGatt gatt) {
    }

    ;

    /**
     * Reads when the notification description is successful
     *
     * @param gatt gatt
     */
    public void onDescriptorRead(BluetoothGatt gatt) {
    }

    /**
     * device connect timeout
     */
    public void onConnectTimeOut() {
    }


    /**
     * 配网成功
     */
    public void onConnectionNetwork(String mac) {
    }

    /**
     * 配网发送信息的返回值
     */
    public void onConnectionBleReturn(int returnCode) {

    }
}
