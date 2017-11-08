package com.example.admin.mybledemo.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.admin.mybledemo.LeDeviceListAdapter;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.annotation.LLAnnotation;
import com.example.admin.mybledemo.annotation.OnClick;
import com.example.admin.mybledemo.annotation.ViewInit;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.com.heaton.blelibrary.ble.BleConfig;
import cn.com.heaton.blelibrary.ble.BleLisenter;
import cn.com.heaton.blelibrary.ble.BleManager;
import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 * 主Activity
 *
 * @author yzz
 *         Created on 2017/11/6 16:59
 */
public class MainActivity extends BaseActivity {

    private String TAG = MainActivity.class.getSimpleName();

    @ViewInit(R.id.sendData)
    private Button mSend;
    @ViewInit(R.id.updateOta)
    private Button mUpdateOta;
    @ViewInit(R.id.listView)
    private ListView mListView;
    @ViewInit(R.id.connected_num)
    private TextView mConnectedNum;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BleManager<BleDevice> mManager;

    @ViewInit(R.id.sid)
    private EditText editTextSid;
    @ViewInit(R.id.psd)
    private EditText editTextPsd;


    /**
     * 辅助处理监听，连接上还是断开都是这里处理的。
     * 刷新页面也是这里
     */
    private BleLisenter mLisenter = new BleLisenter() {
        @Override
        public void onStart() {
            super.onStart();
            //可以选择性实现该方法   不需要则不用实现
        }

        @Override
        public void onStop() {
            super.onStop();
            //可以选择性实现该方法   不需要则不用实现
            invalidateOptionsMenu();
        }

        @Override
        public void onConnectTimeOut() {
            super.onConnectTimeOut();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplication(), R.string.connect_timeout, Toast.LENGTH_SHORT).show();
                    synchronized (mManager.getLocker()) {
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            //扫描得到数据，并且在页面显示出来
            Logger.e("onLeScan");
            synchronized (mManager.getLocker()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onConnectionChanged(final BleDevice device) {
            Logger.e("onConnectionChanged" + device.getConnectionState() + device.isConnected());
            setConnectedNum();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        if (device.getBleAddress().equals(mLeDeviceListAdapter.getDevice(i).getBleAddress())) {
                            if (device.isConnected()) {
                                mLeDeviceListAdapter.getDevice(i).setConnectionState(BleConfig.BleStatus.CONNECTED);
                                Toast.makeText(MainActivity.this, R.string.line_success, Toast.LENGTH_SHORT).show();
                            } else if (device.isConnecting()) {
                                mLeDeviceListAdapter.getDevice(i).setConnectionState(BleConfig.BleStatus.CONNECTING);
                            } else {
                                mLeDeviceListAdapter.getDevice(i).setConnectionState(BleConfig.BleStatus.DISCONNECT);
                                Toast.makeText(MainActivity.this, R.string.line_disconnect, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    synchronized (mManager.getLocker()) {
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt) {
            super.onServicesDiscovered(gatt);
            //可以选择性实现该方法   不需要则不用实现  库中已设置Notify
        }

        @Override
        public void onReady(BluetoothDevice device) {
            super.onReady(device);
            Logger.e("onReady===+++++++可以写入数据了");
        }

        @Override
        public void onChanged(BluetoothGattCharacteristic characteristic) {
            Logger.e("data===" + Arrays.toString(characteristic.getValue()));
            Toast.makeText(MainActivity.this, "收到MCU返回数据：" + Arrays.toString(characteristic.getValue()), Toast.LENGTH_SHORT).show();
            //可以选择性实现该方法   不需要则不用实现
            //硬件mcu 返回数据
        }


        @Override
        public void onRead(BluetoothDevice device) {
            super.onRead(device);
            //可以选择性实现该方法   不需要则不用实现
            Logger.e("onRead");
        }

        @Override
        public void onDescriptorWriter(BluetoothGatt gatt) {
            super.onDescriptorWriter(gatt);
            //可以选择性实现该方法   不需要则不用实现
        }

        @Override
        public void onConnectionNetwork(String mac) {
            super.onConnectionNetwork(mac);
            long time = System.currentTimeMillis() - timeMillis;
            Log.e(BleConfig.TAG, "配网成功++" + time / 1000);
        }

        @Override
        public void onConnectionBleReturn(int returnCode) {
            super.onConnectionBleReturn(returnCode);
            Log.e(BleConfig.TAG, "return Code" + returnCode);
        }
    };
    private long timeMillis;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeMillis = System.currentTimeMillis();

        //初始化注解  替代findViewById
        LLAnnotation.viewInit(this);
        //初始化蓝牙
        initBle();
        initView();
    }


    private void initBle() {
        try {
//            mManager = BleManager.getInstance(getApplicationContext(), editTextSid.getText().toString(), editTextPsd.getText().toString());
            mManager = BleManager.getInstance(getApplicationContext(), "newifiixj", "12345678");
            mManager.registerBleListener(mLisenter);
            if (mManager != null) {
                if (!mManager.isBleEnable()) {//蓝牙未打开
                    mManager.turnOnBlueTooth(this);
                } else {//已打开
                    requestPermission(new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, getString(R.string.ask_permission), new GrantedResult() {
                        @Override
                        public void onResult(boolean granted) {
                            if (!granted) {
                                finish();
                            } else {
                                //开始扫描
                                mManager.scanLeDevice(true);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void junp(View view) {
        startActivity(new Intent(MainActivity.this, TestActivity.class));
    }

    private void initView() {
        setTitle("扫描界面");
        mConnectedNum = (TextView) findViewById(R.id.connected_num);
        // Initializes list view adapter.
        if (mManager != null) {
            mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        }
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: 2017/10/10 连接的问题还有断开的问题，信息存储在自己写的BleDevice类里面，
                final BleDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                if (mManager.isScanning()) {
                    mManager.scanLeDevice(false);
                }
                if (device.isConnected()) {
                    mManager.disconnect(device.getBleAddress());
                } else {
                    mManager.connect(device.getBleAddress());
                }
            }
        });
    }

    @OnClick(R.id.sendData)
    private void onClick11(View view) {
        Toast.makeText(getApplicationContext(), "成功", Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.updateOta)
    private void onUp(View view) {
        Toast.makeText(getApplicationContext(), "测试测试", Toast.LENGTH_LONG).show();
    }


    private void setConnectedNum() {
        if (mManager != null) {
            Log.e("mConnectedNum", "已连接的数量：" + mManager.getConnetedDevices().size() + "");
            for (BleDevice device : mManager.getConnetedDevices()) {
                Log.e("device", "设备地址：" + device.getBleAddress());
            }
            mConnectedNum.setText(getString(R.string.lined_num) + mManager.getConnetedDevices().size());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                Logger.e("点击了扫描按钮");
                if (mManager != null && !mManager.isScanning()) {
                    mLeDeviceListAdapter.clear();
                    mManager.clear();
                    mManager.scanLeDevice(true);
                }
                break;
            case R.id.menu_stop:
                Logger.e("点击了停止扫描按钮");
                if (mManager != null) {
                    mManager.scanLeDevice(false);
                }
                break;
            case R.id.menu_connect_all:
                Logger.e("点击了连接全部设备按钮");
                if (mManager != null) {
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        BleDevice device = mLeDeviceListAdapter.getDevice(i);
                        mManager.connect(device.getBleAddress());
                    }
                }
                break;
            case R.id.menu_disconnect_all:
                Logger.e("点击了断开全部设备按钮");
                if (mManager != null) {
                    ArrayList<BleDevice> list = mManager.getConnetedDevices();
                    for (BleDevice device : list) {
                        mManager.disconnect(device.getBleAddress());
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == BleManager.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else {
            if (mManager != null) {
                mManager.scanLeDevice(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mManager != null && !mManager.isScanning()) {
            mLeDeviceListAdapter.clear();
            mManager.clear();
            mManager.scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mManager != null) {
            mManager.scanLeDevice(false);
        }
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mManager != null) {
            mManager.clear();
            mManager.unRegisterBleListener(mLisenter);
        }
    }
}
