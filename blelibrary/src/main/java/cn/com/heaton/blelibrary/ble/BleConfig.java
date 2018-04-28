package cn.com.heaton.blelibrary.ble;

import android.support.annotation.IntDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * Ble蓝牙的类
 *
 * @author yzz
 *         Created on 2017/10/26 16:47
 */

public class BleConfig {


    /**
     * Annotations
     * prevent the defined constant values from being repeated
     */
    @IntDef({
            BleStatus.CONNECTED,
            BleStatus.CONNECTING,
            BleStatus.DISCONNECT,
            BleStatus.ConnectionChanged,
            BleStatus.ServicesDiscovered,
            BleStatus.Write,
            BleStatus.Changed,
            BleStatus.DescriptorWriter,
            BleStatus.DescriptorRead,
            BleStatus.Start,
            BleStatus.Stop,
            BleStatus.ConnectTimeOut,
            BleStatus.OnReady,
            BleStatus.ConnectionNetwork,
            BleStatus.ConnectionBleReturn
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface BleStatus {
        int CONNECTED = 2505;
        int CONNECTING = 2504;
        int DISCONNECT = 2503;
        int ConnectionChanged = 2511;//0，断开；1，连接上；2，正在连接。
        int ServicesDiscovered = 2512;
        int Write = 2514;
        int Changed = 2515;//onCharacteristicChanged变化
        int DescriptorWriter = 2516;
        int DescriptorRead = 2517;
        int Start = 2518;
        int Stop = 2519;
        int ConnectTimeOut = 2510;
        int OnReady = 2520; //onDescriptorWrite测试获得数据成功，可以读取了
        int ConnectionNetwork = 2521;//最后配网成功
        int ConnectionBleReturn = 2522;//配网返回的值
    }

    /**
     * The default scan time
     * 扫描10s超时
     */
    public final static int SCAN_PERIOD = 100000;

    /**
     * Connection time-out limit
     */
    public final static int CONNECT_TIME_OUT = 10 * 1000;

    /**
     * The service UUID string  0000180a-0000-1000-8000-00805f9b34fb
     */
    public static String UUID_SERVICE_TEXT = "0000180a-0000-1000-8000-00805f9b34fb";


    /**
     * Attribute UUID string 00009999-0000-1000-8000-00805f9b34fb
     */
    public static String UUID_CHARACTERISTIC_TEXT = "00009999-0000-1000-8000-00805f9b34fb";


    public static final byte[] VALUE_START = new byte[]{(byte) 0xa5, (byte) 0xa5};
    public static final byte[] VALUE_END = new byte[]{(byte) 0xb5, (byte) 0xb5};
    public static final String VALUE_STRING_START = "a5a5";
    public static final String VALUE_STRING_END = "b5b5";

    public static final String WIFI_SSID = "s";
    public static final String WIFI_P = "p";


    /* Manufacturer Specific Data. */
    public static final int BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    /*Return code */
    public static final int BLE_RETURN_LENGTH = 0x05;//返回码的基本长度
    public static final int BLE_RETURN_RIGHT = 0x00;//正确
    public static final int BLE_RETURN_WRONG_FORMAT = 0x01;//格式不对
    public static final int BLE_RETURN_LACK_SSID = 0x02;//缺少SSID
    public static final int BLE_RETURN_NETWORK_FAILURE = 0x03;//配网失败

    public static final String LENOVOASSISTANT = "LenovoAssistant";
    public static final String TAG = "蓝牙配网";

    public static final boolean AUTO_CONNECT = false;

}
