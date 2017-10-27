package cn.com.heaton.blelibrary.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static cn.com.heaton.blelibrary.ble.BleConfig.WIFI_P;
import static cn.com.heaton.blelibrary.ble.BleConfig.WIFI_SSID;

/**
 * This class provides various APIs for Bluetooth operation
 * Created by liulei on 2016/12/7.
 */

public class BleManager<T extends BleDevice> {

    private final static String TAG = "BleManager";
    public static final int REQUEST_ENABLE_BT = 1;
    private Context mContext;
//    private BluetoothLeService mBluetoothLeService;
    //    private static BleLisenter mBleLisenter;
    private static List<BleLisenter> mBleLisenters = new ArrayList<>();
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private final ArrayList<T> mScanDevices = new ArrayList<>();
    private final ArrayList<T> mConnetedDevices = new ArrayList<>();
    private ArrayList<T> mConnectingDevices = new ArrayList<>();
    private final Object mLocker = new Object();
    private static BleManager instance;
    private BluetoothManager mBluetoothManager;//蓝牙管理服务
    private BleFactory<T> mBleFactory;

    //The device is currently connected
    private BluetoothDevice currentDevice = null;

    /**
     * Multiple device connections must put the gatt object in the collection
     */
    private Map<String, BluetoothGatt> mBluetoothGattMap;
    /**
     * The address of the connected device
     */
    private List<String> mConnectedAddressList;
    private List<BluetoothGattCharacteristic> mNotifyCharacteristics = new ArrayList<>();//Notification attribute callback array
    private int mNotifyIndex = 0;//Notification feature callback list
    private Map<String, BluetoothGattCharacteristic> mWriteCharacteristicMap = new HashMap<>();//可以写的特征值  就是这里


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BleConfig.BleStatus.ConnectTimeOut:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onConnectTimeOut();
                    }
                    break;
                case BleConfig.BleStatus.ConnectionChanged:
                    T device = null;
                    try {
                        device = mBleFactory.create(BleManager.this, (BluetoothDevice) msg.obj);
                        if (msg.arg1 == 1) {
                            //connect
                            device.setConnectionState(BleConfig.BleStatus.CONNECTED);
                            mConnetedDevices.add(device);
//                            Log.e("ConnectionChanged","Added a device");
                        } else if (msg.arg1 == 0) {
                            //disconnect
                            device.setConnectionState(BleConfig.BleStatus.DISCONNECT);
                            mConnetedDevices.remove(device);
//                            Log.e("ConnectionChanged","Removed a device");
                        } else if (msg.arg1 == 2) {
                            //connectting
                            device.setConnectionState(BleConfig.BleStatus.CONNECTING);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onConnectionChanged(device);
                    }
                    break;
                case BleConfig.BleStatus.Changed:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onChanged((BluetoothGattCharacteristic) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.Read:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onRead((BluetoothDevice) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.DescriptorWriter:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onDescriptorWriter((BluetoothGatt) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.OnReady:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onReady((BluetoothDevice) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.ConnectionNetwork:
                    //连接网络成功

                    break;
                case BleConfig.BleStatus.ServicesDiscovered:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onServicesDiscovered((BluetoothGatt) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.DescriptorRead:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onDescriptorRead((BluetoothGatt) msg.obj);
                    }
                    break;
            }
        }
    };

    protected BleManager(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleFactory = new BleFactory<>(context);
    }

    /**
     * Get the class object
     *
     * @param type TYPE
     * @param i    LOCATION
     * @return Object
     */
    private static Class getClass(Type type, int i) {
        if (type instanceof ParameterizedType) { //Processing generic types
            return getGenericClass((ParameterizedType) type, i);
        } else if (type instanceof TypeVariable) {
            return getClass(((TypeVariable) type).getBounds()[0], 0); // Handle the generic wipe object
        } else {// Class itself is also type, forced transformation
            return (Class) type;
        }
    }

    private static Class getGenericClass(ParameterizedType parameterizedType, int i) {
        Object genericClass = parameterizedType.getActualTypeArguments()[i];
        if (genericClass instanceof ParameterizedType) { // Processing multistage generic
            return (Class) ((ParameterizedType) genericClass).getRawType();
        } else if (genericClass instanceof GenericArrayType) { // Processing array generics
            return (Class) ((GenericArrayType) genericClass).getGenericComponentType();
        } else if (genericClass instanceof TypeVariable) { //Handle the generic wipe object
            return getClass(((TypeVariable) genericClass).getBounds()[0], 0);
        } else {
            return (Class) genericClass;
        }
    }

    public static <T extends BleDevice> BleManager<T> getInstance(Context context) throws Exception {
        if (instance == null) {
            synchronized (BleManager.class) {
                if (instance == null) {
                    instance = new BleManager(context);
                }
            }
        }
        if (instance.isSupportBle()) {
            return instance;
        } else {
            throw new Exception("BLE is not supported");
        }
    }

    /**
     * Whether to support Bluetooth
     *
     * @return Whether to support Ble
     */
    private boolean isSupportBle() {
        return (mBluetoothAdapter != null && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }


    /**
     * Bluetooth is turned on
     *
     * @return true  Bluetooth is turned on
     */
    public boolean isBleEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * open ble
     *
     * @param activity The context object
     */
    public void turnOnBlueTooth(Activity activity) {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!isBleEnable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * close ble
     */
    public boolean turnOffBlueTooth() {
        return !mBluetoothAdapter.isEnabled() || mBluetoothAdapter.disable();
    }

    /**
     * Starts scanning or stops scanning the device
     *
     * @param enable Whether to start
     */
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onStop();
                    }
                }
            }, BleConfig.SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            for (BleLisenter bleLisenter : mBleLisenters) {
                bleLisenter.onStart();
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            for (BleLisenter bleLisenter : mBleLisenters) {
                bleLisenter.onStop();
            }
        }
    }

    /**
     * 开启扫描得到的监听。
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            Log.e(TAG, "onLeScan: " + device);
            if (!contains(device)) {
                T bleDevice = (T) new BleDevice(device);
                for (BleLisenter bleLisenter : mBleLisenters) {
                    bleLisenter.onLeScan(bleDevice, rssi, scanRecord);
                }
                mScanDevices.add(bleDevice);
                String name = bleDevice.getmBleName();
                if (!TextUtils.isEmpty(name) && name.startsWith(BleConfig.LENOVOASSISTANT)) {
                    scanLeDevice(false);
                    connect(bleDevice.getBleAddress());
                }
            }
        }
    };

    public boolean contains(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        synchronized (mScanDevices) {
            for (T bleDevice : mScanDevices) {
                if (bleDevice.getBleAddress().equals(device.getAddress())) {
                    return true;
                }
            }
            return false;
        }
    }

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
                Log.e(BleConfig.TAG, "连接Connected to GATT server^^Attempting to start service discovery:" + mBluetoothGattMap.get(device.getAddress()).discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.removeCallbacks(mConnectTimeout);
                Log.e(BleConfig.TAG, "断开Disconnected from GATT server.");
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
                displayGattServices(gatt, gatt.getDevice().getAddress(), gatt.getServices());
                boolean b = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    b = gatt.requestMtu(256);
                }
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


    /**
     * 处理得到的Gatt服务
     *
     * @param address
     * @param gattServices
     */
    private void displayGattServices(BluetoothGatt gatt, final String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "displayGattServices: " + uuid);
            if (uuid.equals(BleConfig.UUID_SERVICE_TEXT)) {
                Log.d(TAG, "service_uuid: " + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equals(BleConfig.UUID_CHARACTERISTIC_TEXT)) {
                        mWriteCharacteristicMap.put(address, gattCharacteristic);
                        mNotifyCharacteristics.add(gattCharacteristic);
                    }
                }
                //Really set up notifications
                if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0) {
                    Log.e(BleConfig.TAG, "setCharaNotification");
                    setCharacteristicNotification(gatt, mNotifyCharacteristics.get(mNotifyIndex++), true);
                }

                sendData(address,"sp_team", "lenovo123");
            }
        }
    }


    public void setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || gatt == null) {
            Log.d(TAG, "BluetoothAdapter is null");
            return;
        }
        gatt.setCharacteristicNotification(characteristic, enabled);
    }


    /**
     * Get the lock
     */
    public Object getLocker() {
        return mLocker;
    }

    /**
     * Whether it is scanning
     */
    public boolean isScanning() {
        return mScanning;
    }

    /**
     * get BLE
     *
     * @param device blutoothdevice
     * @return bleDeive
     */
    public T getBleDevice(BluetoothDevice device) {
        if (device == null) {
            return null;
        }
        synchronized (mConnetedDevices) {
            if (mConnetedDevices.size() > 0) {
                for (T bleDevice : mConnetedDevices) {
                    if (bleDevice.getBleAddress().equals(device.getAddress())) {
                        return bleDevice;
                    }
                }
            }
            T newDevice = (T) new BleDevice(device);
            return newDevice;
        }
    }

    /**
     * Register the listening event
     *
     * @param bleListener Listener
     */
    public void registerBleListener(BleLisenter bleListener) {
        if (mBleLisenters.contains(bleListener)) {
            return;
        }
        mBleLisenters.add(bleListener);
    }

    /**
     * Cancel the registration event
     *
     * @param bleListener Listener
     */
    public void unRegisterBleListener(BleLisenter bleListener) {
        if (bleListener == null) {
            return;
        }
        if (mBleLisenters.contains(bleListener)) {
            mBleLisenters.remove(bleListener);
        }
    }

    /**
     * Gets the connected device
     *
     * @return connected device
     */

    public ArrayList<T> getConnetedDevices() {
        return mConnetedDevices;
    }

//    /**
//     * connect bleDevice
//     * 连接设备，BluetoothLeService开始工作
//     *
//     * @param address ble address
//     */
//    public boolean connect(String address) {
//        synchronized (mLocker) {
//            boolean result = false;
//            if (mBluetoothLeService != null) {
//                result = mBluetoothLeService.connect(address);
//            }
//            return result;
//        }
//    }


    /**
     * connect bleDevice
     * 连接设备，BluetoothLeService开始工作
     *
     * @param address ble address
     */
    public boolean connect(String address) {
        synchronized (mLocker) {

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
            BluetoothGatt bluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
            if (bluetoothGatt != null) {
                mBluetoothGattMap.put(address, bluetoothGatt);
                Log.d(TAG, "Trying to create a new connection.");
                return true;
            }
            return false;


        }


    }


    /**
     * disconnect device
     *
     * @param address ble address
     */
    public void disconnect(String address) {
        synchronized (mLocker) {
            if (mBluetoothGattMap == null) {
                return;
            }
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
    }


    /**
     * 发送数据，就是WiFi名称和密码
     *
     * @param address
     * @param ssid
     * @param mPass
     * @return
     */
    public boolean sendData(String address, String ssid, String mPass) {
        byte[] dataByte = getDataByte(ssid, mPass);
        synchronized (mLocker) {
            if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return false;
            }
            BluetoothGattCharacteristic gattCharacteristic = mWriteCharacteristicMap.get(address);
            if (gattCharacteristic != null) {
                try {
                    gattCharacteristic.setValue(dataByte);
                    BluetoothGatt gatt = mBluetoothGattMap.get(address);
                    boolean result1 = gatt.writeCharacteristic(gattCharacteristic);
                    Log.d(TAG, address + " -- write result:" + result1 + " -- write data:" + Arrays.toString(dataByte));
                    return result1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;

        }
    }


    //Release Empty all resources
    public void clear() {
        synchronized (mLocker) {
            for (T bleDevice : mConnetedDevices) {
                disconnect(bleDevice.getBleAddress());
            }

            mConnectedAddressList.clear();
            mConnetedDevices.clear();
            mConnectingDevices.clear();
            mScanDevices.clear();
        }
    }


    private String makeJsonString(String key, String value, String endStr) {
        if (!TextUtils.isEmpty(value))
            return "\"" + key + "\":\"" + value + "\"" + endStr;
        else
            return "";
    }

    //拼接WiFi的信息
    public byte[] getDataByte(String ssid, String mPass) {
        String str = "{";
        str += makeJsonString(WIFI_SSID, ssid, ",");
        str += makeJsonString(WIFI_P, mPass, "");
        str += "}";

        byte[] data = str.getBytes();
        byte[] dataSpell = getByteValue(BleConfig.VALUE_START, data, BleConfig.VALUE_END);

        Log.e("data:", Arrays.toString(dataSpell));
        return dataSpell;
    }


    /**
     * 拼接Value的值，加头部和
     *
     * @param aa
     * @param bb
     * @param cc
     * @return
     */
    private byte[] getByteValue(byte[] aa, byte[] bb, byte[] cc) {
        // TODO Auto-generated method stub
        List collect = new ArrayList();
        collect.add(aa);
        collect.add(bb);
        collect.add(cc);
        byte[] aa0 = null;
        // tyy 每次都是两个数组合并 所以合并的次数为 collect.size() ，第一个是虚拟的数组
        for (byte i = 0; i < collect.size(); i++) {
            byte[] aa1 = (byte[]) collect.get(i);
            byte[] newInt = onArrayTogether(aa0, aa1);
            aa0 = newInt;
        }
        return aa0;
    }

    private static byte[] onArrayTogether(byte[] aa, byte[] bb) {
        // TODO Auto-generated method stub
        if (aa == null) {
            return bb;
        }
        byte[] collectionInt = new byte[aa.length + bb.length];
        for (int i = 0; i < aa.length; i++) {
            collectionInt[i] = aa[i];
        }
        for (int i = aa.length; i < aa.length + bb.length; i++) {
            collectionInt[i] = bb[i - aa.length];
        }
        return collectionInt;
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
     * unbind service
     */
    public void unService() {
        if (mContext != null) {

        }
    }

}
