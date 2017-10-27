package cn.com.heaton.blelibrary.ble;

import android.support.annotation.IntDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * Ble蓝牙的类
 * @author yzz
 * Created on 2017/10/26 16:47 
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
            BleStatus.Read,
            BleStatus.Write,
            BleStatus.Changed,
            BleStatus.DescriptorWriter,
            BleStatus.DescriptorRead,
            BleStatus.Start,
            BleStatus.Stop,
            BleStatus.ConnectTimeOut,
            BleStatus.OnReady,
            BleStatus.ConnectionNetwork
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface BleStatus {
        int CONNECTED = 2505;
        int CONNECTING = 2504;
        int DISCONNECT = 2503;
        int ConnectionChanged = 2511;
        int ServicesDiscovered = 2512;
        int Read = 2513;
        int Write = 2514;
        int Changed = 2515;
        int DescriptorWriter = 2516;
        int DescriptorRead = 2517;
        int Start = 2518;
        int Stop = 2519;
        int ConnectTimeOut = 2510;
        int OnReady = 2520;
        int ConnectionNetwork = 2521;
    }

    /**
     * The default scan time
     * 扫描10s超时
     */
    public final static int SCAN_PERIOD = 10000;

    /**
     * Connection time-out limit
     */
    public final static int CONNECT_TIME_OUT = 10 * 1000;

    /* Manufacturer Specific Data. */
    public static final int BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    /**
     * Describes the UUID string  00002901-0000-1000-8000-00805f9b34fb
     */
    public static String UUID_DESCRIPTOR_TEXT = "00002902-0000-1000-8000-00805f9b34fb";
    /**
     * The service UUID string  0000180a-0000-1000-8000-00805f9b34fb
     */
    public static String UUID_SERVICE_TEXT = "0000180a-0000-1000-8000-00805f9b34fb";

    /**
     * The service UUID string
     */
    public static UUID UUID_SERVICE = UUID.fromString(UUID_SERVICE_TEXT);

    public static UUID UUID_DESCRIPTOR = UUID.fromString(UUID_DESCRIPTOR_TEXT);
    /**
     * Attribute UUID string 00009999-0000-1000-8000-00805f9b34fb
     */
    public static String UUID_CHARACTERISTIC_TEXT = "00009999-0000-1000-8000-00805f9b34fb";
    /**
     * Sets the notification attribute UUID string d44bc439-abfd-45a2-b575-925416129601
     */
    public static String UUID_NOTIFY_TEXT = "d44bc439-abfd-45a2-b575-925416129601";
    /**
     * CHARACTERISTIC UUID string
     */
    public static UUID UUID_CHARACTERISTIC = UUID.fromString(UUID_CHARACTERISTIC_TEXT);


    public static final byte[] VALUE_START = new byte[]{(byte) 0xa5, (byte) 0xa5};
    public static final byte[] VALUE_END = new byte[]{(byte) 0xb5, (byte) 0xb5};

    public static final String WIFI_SSID = "s";
    public static final String WIFI_P = "p";

    


    /**
     * Sets the Notification UUID string
     *
     * @param uuidNotifyText Notification UUID string
     */
    public static void setUuidNotifyText(String uuidNotifyText) {
        if (TextUtils.isEmpty(uuidNotifyText)) {
            return;
        }
        UUID_NOTIFY_TEXT = uuidNotifyText;
    }

    public static UUID getUuidService() {
        return UUID_SERVICE;
    }

    public static UUID getUuidCharacteristic() {
        return UUID_CHARACTERISTIC;
    }

    public static UUID getUuidDescriptor() {
        return UUID_DESCRIPTOR;
    }

    public static String getNotifyText() {
        return UUID_NOTIFY_TEXT;
    }




}
