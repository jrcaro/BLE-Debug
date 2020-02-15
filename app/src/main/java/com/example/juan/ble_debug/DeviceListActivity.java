package com.example.juan.ble_debug;


import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceListActivity extends Activity {

    public static UUID[] UART_UUID = new UUID[]{UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")}; //API <21
    public static final String TAG = "DeviceListActivity";
    private static final long SCAN_PERIOD = 10000; //scanning for 10 seconds

    private BluetoothAdapter mBluetoothAdapter;
    private  Handler mHandler = new Handler();
    private boolean mScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mHandler = new Handler();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable){
        final BluetoothLeScanner mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        final ScanSettings scanSettings = new ScanSettings.Builder().build();
        final List<ScanFilter> filterList = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setDeviceAddress("FE:0E:06:69:A9:BB").build();
        filterList.add(filter);

        if(enable){
            mScanning = true;
            //mLEScanner.startScan(filterList, scanSettings, mScanCallback);
            mBluetoothAdapter.startLeScan(UART_UUID, mLeScanCallback); //API <21
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mScanning) {
                        mScanning = false;
                        //mLEScanner.stopScan(mScanCallback);
                        mBluetoothAdapter.stopLeScan(mLeScanCallback); //API <21
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                }
            },SCAN_PERIOD);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback); //API <21
            //mLEScanner.stopScan(mScanCallback);
        }
    }

    /*private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            scanLeDevice(false);
            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, result.getDevice().getAddress());

            Intent device = new Intent();
            device.putExtras(b);
            setResult(Activity.RESULT_OK, device);
            finish();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };*/

    //API <21
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(device.getAddress().equals("FE:0E:06:69:A9:BB")){
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                Bundle b = new Bundle();
                                b.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());

                                Intent result = new Intent();
                                result.putExtras(b);
                                setResult(Activity.RESULT_OK, result);
                                finish();
                            }
                        }
                    });
                }
            };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStop() {
        super.onStop();
        scanLeDevice(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);
    }

    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }
}
