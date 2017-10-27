package cn.com.heaton.blelibrary.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.sax.StartElementListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author yzz
 *         Created on 2017/10/26 16:46
 */

@SuppressLint("NewApi")
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private Handler mHandler;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final Object mLocker = new Object();
    //    private BluetoothGattCharacteristic mWriteCharacteristic;//Writable GattCharacteristic object
    private List<BluetoothGattCharacteristic> mNotifyCharacteristics = new ArrayList<>();//Notification attribute callback array
    private int mNotifyIndex = 0;//Notification feature callback list

    private Map<String, BluetoothGattCharacteristic> mWriteCharacteristicMap = new HashMap<>();

    /**
     * Multiple device connections must put the gatt object in the collection
     */
    private Map<String, BluetoothGatt> mBluetoothGattMap;
    /**
     * The address of the connected device
     */
    private List<String> mConnectedAddressList;

    //The device is currently connected
    private BluetoothDevice currentDevice = null;


    private Runnable mConnectTimeout = new Runnable() { // 连接设备超时
        @Override
        public void run() {
            mHandler.sendEmptyMessage(BleConfig.BleStatus.ConnectTimeOut);
            if (currentDevice != null) {
                disconnect(currentDevice.getAddress());
                close(currentDevice.getAddress());
                mHandler.obtainMessage(BleConfig.BleStatus.ConnectionChanged, 0, 0, currentDevice).sendToTarget();
            }
        }
    };

    /**
     * Connection changes or services were found in a variety of state callbacks
     * 连接方法的回调
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            /**
             * 连接状态的变化
             */
            BluetoothDevice device = gatt.getDevice();
            //There is a problem here Every time a new object is generated that causes the same device to be disconnected and the connection produces two objects
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectedAddressList.add(device.getAddress());
                mHandler.removeCallbacks(mConnectTimeout);
                mHandler.obtainMessage(BleConfig.BleStatus.ConnectionChanged, 1, 0, device).sendToTarget();
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Connected to GATT server^^Attempting to start service discovery:" + mBluetoothGattMap.get(device.getAddress()).discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.removeCallbacks(mConnectTimeout);
                Log.i(TAG, "Disconnected from GATT server.");
                mHandler.obtainMessage(BleConfig.BleStatus.ConnectionChanged, 0, 0, device).sendToTarget();
                close(device.getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            /**
             * 设备已经发现，得到服务
             */
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(BleConfig.BleStatus.ServicesDiscovered, gatt).sendToTarget();
                //Empty the notification attribute list
                mNotifyCharacteristics.clear();
                mNotifyIndex = 0;
                //Start setting notification feature
                displayGattServices(gatt.getDevice().getAddress(), getSupportedGattServices(gatt.getDevice().getAddress()));
                boolean b = gatt.requestMtu(256);
                Log.e(TAG, "是否成功设置+++" + b);
            } else {
                Log.e(TAG, "onServicesDiscovered received连接GATT失败: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            /**
             * 特征的问题读成功
             */
            Log.e(TAG, "onCharacteristicRead:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(BleConfig.BleStatus.Read, gatt.getDevice()).sendToTarget();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            /**
             * 特征的问题写成功
             */
            System.out.println("--------特征的回调----- status:" + status + "得到的值+" + Arrays.toString(characteristic.getValue()));
            synchronized (mLocker) {
                Log.i(TAG, gatt.getDevice().getAddress() + " -- onCharacteristicWrite: " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mHandler.obtainMessage(BleConfig.BleStatus.Write, gatt).sendToTarget();
                }
            }
        }


        /*
         * when connected successfully will callback this method , this method can dealwith send password or data analyze
         * When setnotify (true) is set, the method is called back if the data on the MCU (device side) changes.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //监听的通知发生变化，会走这里。
            System.out.println("--------onCharacteristicChanged----- " + "通知回调的值+" + Arrays.toString(characteristic.getValue()));

            synchronized (mLocker) {
                byte[] valueGet = characteristic.getValue();
                Log.i(TAG, gatt.getDevice().getAddress() + " -- onCharacteristicWrite: " + (characteristic.getValue() != null ? Arrays.toString(characteristic.getValue()) : ""));
                mHandler.obtainMessage(BleConfig.BleStatus.Changed, characteristic).sendToTarget();
                if (valueGet != null) {
                    String stringValue = Arrays.toString(characteristic.getValue());
                    if (stringValue.startsWith("[-91, -91,") || stringValue.endsWith(" -75, -75]")) {
                        mHandler.obtainMessage(BleConfig.BleStatus.ConnectionNetwork, stringValue).sendToTarget();
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getCharacteristic().getUuid();
            Log.w(TAG, "onDescriptorWrite");
            Log.e(TAG, "descriptor_uuid:" + uuid);
            synchronized (mLocker) {
                Log.e(TAG, " -- onDescriptorWrite: " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0 && mNotifyIndex < mNotifyCharacteristics.size()) {
                        setCharacteristicNotification(gatt.getDevice().getAddress(), mNotifyCharacteristics.get(mNotifyIndex++), true);
                    } else {
                        Log.e(TAG, "====setCharacteristicNotification is true,ready to sendData===可以写入数据了onDescriptorWrite方法");
                        mHandler.obtainMessage(BleConfig.BleStatus.OnReady, gatt.getDevice()).sendToTarget();
                    }
                }
                mHandler.obtainMessage(BleConfig.BleStatus.DescriptorWriter, gatt).sendToTarget();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            UUID uuid = descriptor.getCharacteristic().getUuid();
            Log.w(TAG, "onDescriptorRead");
            Log.e(TAG, "descriptor_uuid:219" + uuid);
            mHandler.obtainMessage(BleConfig.BleStatus.DescriptorRead, gatt).sendToTarget();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            System.out.println("rssi = " + rssi);
        }


        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }



    /**
     * 在服务中找到这个Binder
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Initializes the ble Bluetooth device
     * 初始服务里面的蓝牙控制者
     *
     * @return Whether the initialization is successful
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        //Bluetooth 4.0, that API level> = 18, and supports Bluetooth 4.0 phone can use, if the mobile phone system version API level <18, is not used Bluetooth 4 android system 4.3 above, the phone supports Bluetooth 4.0
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Connects to a specified Bluetooth device
     *
     * @param address ble address
     * @return Whether connect is successful
     */
    // TODO: 2017/6/6  connect 连接蓝牙
    public boolean connect(final String address) {

        if (mConnectedAddressList == null) {
            mConnectedAddressList = new ArrayList<>();
        }
        if (mConnectedAddressList.contains(address)) {
            Log.d(TAG, "This is device already connected.");
            return true;
        }
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device. Try to reconnect. ()
        if (mBluetoothGattMap == null) {
            mBluetoothGattMap = new HashMap<>();
        }
        //10s after the timeout prompt
        mHandler.postDelayed(mConnectTimeout, BleConfig.CONNECT_TIME_OUT);

        //根据地址得到BluetoothDevice蓝牙设备的信息
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "no device");
            return false;
        }
        currentDevice = device;
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false
        mHandler.obtainMessage(BleConfig.BleStatus.ConnectionChanged, 2, 0, device).sendToTarget();
        /**
         * this:上下文
         * false：是否自动连接
         * mGattCallback：回调引用
         */
        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (bluetoothGatt != null) {
            mBluetoothGattMap.put(address, bluetoothGatt);
            Log.d(TAG, "Trying to create a new connection.");
//            mConnectedAddressList.add(address);//暂时注释
            return true;
        }
        return false;
    }

    /**
     * Disconnects the specified Bluetooth blinking device
     *
     * @param address ble address
     */
    public void disconnect(final String address) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
//            Log.e(TAG, mBluetoothGattMap.get(address).getDevice().getAddress());
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mNotifyIndex = 0;
        mBluetoothGattMap.get(address).disconnect();
        mNotifyCharacteristics.clear();
        mWriteCharacteristicMap.remove(address);
    }

    /**
     * Clear the specified Bluetooth address of the Bluetooth bluetooth connection device
     *
     * @param address ble address
     */
    public void close(String address) {
        mConnectedAddressList.remove(address);
        if (mBluetoothGattMap.get(address) != null) {
            mBluetoothGattMap.get(address).close();
            mBluetoothGattMap.remove(address);
        }
    }

    /**
     * Clear all ble connected devices
     */
    public void close() {
        if (mConnectedAddressList == null) return;
        for (String address :
                mConnectedAddressList) {
            if (mBluetoothGattMap.get(address) != null) {
                mBluetoothGattMap.get(address).close();
            }
        }
        mBluetoothGattMap.clear();
        mConnectedAddressList.clear();
    }


    /**
     * send data
     * 服务里面的BluetoothGattCharacteristic写数据
     *
     * @param address ble address
     * @param value   Send data values
     * @return whether succeed
     */
    public boolean writeCharacteristic(String address, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattCharacteristic gattCharacteristic = mWriteCharacteristicMap.get(address);
        if (gattCharacteristic != null) {
            try {
                gattCharacteristic.setValue(value);
                BluetoothGatt gatt = mBluetoothGattMap.get(address);
                boolean result = gatt.writeCharacteristic(gattCharacteristic);
                Log.d(TAG, address + " -- write result:" + result + " -- write data:" + Arrays.toString(value));
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;

    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param address        ble address
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(String address, BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.d(TAG, "BluetoothAdapter is null");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled);

    }

    //Set the notification array

    /**
     * 处理得到的Gatt服务
     *
     * @param address
     * @param gattServices
     */
    private void displayGattServices(final String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "displayGattServices: " + uuid);
            List<BluetoothGattCharacteristic> characteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristicTest : characteristics) {
                boolean readable = isCharacterisitcReadable(characteristicTest);
                boolean notifiable = isCharacterisiticNotifiable(characteristicTest);
                boolean writeable = isCharacteristicWriteable(characteristicTest);
                Log.e(TAG, "displayGattServices测试可读: " + readable + "可写+" + writeable + "通知+" + notifiable);
            }
            if (uuid.equals(BleConfig.UUID_SERVICE_TEXT)) {
                Log.d(TAG, "service_uuid: " + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equals(BleConfig.UUID_CHARACTERISTIC_TEXT)){
                        mWriteCharacteristicMap.put(address, gattCharacteristic);
                        mNotifyCharacteristics.add(gattCharacteristic);
                    }
                }
                //Really set up notifications
                if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0) {
                    Log.e("setCharaNotification", "setCharaNotification");
                    setCharacteristicNotification(address, mNotifyCharacteristics.get(mNotifyIndex++), true);
                }
            }
        }
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @param address ble address
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (mBluetoothGattMap.get(address) == null)
            return null;

        return mBluetoothGattMap.get(address).getServices();
    }


    /**
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWriteable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacterisitcReadable(BluetoothGattCharacteristic pChar) {
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is supports notification
     */
    public boolean isCharacterisiticNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }


}
