package com.example.juan.ble_debug;

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
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.juan.tanit.selfadaptation.SAComponentInterface;

import java.util.List;
import java.util.UUID;

public class BatteryService extends Service {
    private final static String TAG = BatteryService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED_BATT =
            "com.example.juan.ble_debug.ACTION_GATT_CONNECTED_BATT";
    public final static String ACTION_GATT_DISCONNECTED_BATT =
            "com.example.juan.ble_debug.ACTION_GATT_DISCONNECTED_BATT";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_BATT =
            "com.example.juan.ble_debug.ACTION_GATT_SERVICES_DISCOVERED_BATT";
    public final static String ACTION_DATA_AVAILABLE_BATT =
            "com.example.juan.ble_debug.ACTION_DATA_AVAILABLE_BATT";
    public final static String EXTRA_DATA_BATT =
            "com.example.juan.ble_debug.EXTRA_DATA_BATT";
    public final static String DEVICE_DOES_NOT_SUPPORT_BATT =
            "com.example.juan.ble_debug.DEVICE_DOES_NOT_SUPPORT_BATT";
    public final static String ACTION_CHANGE_PERIOD =
            "com.example.juan.ble_debug.ACTION_CHANGE_PERIOD";


    public static final UUID BATT_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID BATT_CHAR_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID BATT_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private int quality;
    private Boolean end;
    private Integer period;


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED_BATT;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED_BATT;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt );

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_BATT);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE_BATT, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE_BATT, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, Double period) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_BATT, period);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is handling for the notification on TX Character of NUS service
        if (BATT_CHAR_UUID.equals(characteristic.getUuid())) {

            // Log.d(TAG, String.format("Received TX: %d",characteristic.getValue() ));
            intent.putExtra(EXTRA_DATA_BATT, characteristic.getValue());
        } else {
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /*@Override
    public Boolean putInSafeState() {
        return true;
    }

    @Override
    public Boolean disable() {
        return true;
    }

    @Override
    public Boolean enable() {
        return true;
    }

    @Override
    public Boolean tuneParameter(String key, Object param) {
        Log.d("cane",key+" "+param.toString());
        if(key.equals("battery_period")){
            // el param puedes hacer casting a Double
            period =Integer.parseInt((String)param);
            broadcastUpdate(ACTION_CHANGE_PERIOD, new Double(period));
            Log.d("cane","La frecuencia de la batería esta en "+ period);
            // sustituir por llamadas a los call backs
            /*if(period.compareTo(11)==0){
                Double precision=mainActivity.getPrecisionValue();
                precision=precision-25;
                mainActivity.updatePrecisionValue(precision);
            }else if(period.compareTo(6)==0){
                Double precision=mainActivity.getPrecisionValue();
                precision=precision+25;
                mainActivity.updatePrecisionValue(precision);
            }*/
       /* }else if(key.equals("battery_quality")){
            String qualityString=(String)param;
            if(qualityString.endsWith("1")){
                quality=1;
            }else if(qualityString.endsWith("2")){
                quality=2;
            }
            Log.d("cane","La calidad de la batería esta en "+quality);
        }
        return true;
    }*/

    public class LocalBinder extends Binder {
        BatteryService getService() {
            return BatteryService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new BatteryService.LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        period =6;
        quality=1;
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
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        // mBluetoothGatt.close();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *

     */
    public void enableBatteryNotification()
    {
    	/*
    	if (mBluetoothGatt == null) {
    		showMessage("mBluetoothGatt null" + mBluetoothGatt);
    		broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
    		return;
    	}
    		*/
        BluetoothGattService batteryService = mBluetoothGatt.getService(BATT_SERVICE_UUID);
        if (batteryService == null) {
            showMessage("Battery service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_BATT);
            return;
        }
        BluetoothGattCharacteristic batteryChar = batteryService.getCharacteristic(BATT_CHAR_UUID);
        if (batteryChar == null) {
            showMessage("Battery charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_BATT);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(batteryChar,true);

        BluetoothGattDescriptor descriptor = batteryChar.getDescriptor(BATT_CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
