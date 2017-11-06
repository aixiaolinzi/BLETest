package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
/**
 *
 * @author yzz
 * Created on 2017/11/6 16:21
 * Bluetooth factory
 */

public class BleFactory<T extends BleDevice>{
    private Context mContext;

    public BleFactory(Context context){
        mContext = context;
    }

    public T create(BleManager<T> bleManager,BluetoothDevice device) throws Exception{
        return bleManager.getBleDevice(device);
    }

    public Context getContext() {
        return mContext;
    }
}
