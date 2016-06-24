package com.example.melih.beacon_nav;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> results;
    private double txPower = -8.0;
    private BluetoothAdapter BTAdapter;
    private BluetoothLeScanner BTLE;
    private ArrayAdapter adapter;
    private Button scanBtn;
    private ListView listView;
    private List<ScanResult> resultLE;

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                Log.d("DEVICES", "Device: " + device.getName() + " " + rssi + " " + device.getAddress());
                double dist = calculateAccuracy(rssi);
                results.add(device.getAddress() + "\n " + rssi + "\n " + dist);
                adapter = new ArrayAdapter<>(MainActivity.this, R.layout.main_list, results);
                listView.setAdapter(adapter);
            }else if(ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d("INFO", "finished");
            }else if(ACTION_DISCOVERY_STARTED.equals(action)){
                results.clear();
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        listView = (ListView) findViewById(R.id.list);
        results = new ArrayList<>();
        BTAdapter = getDefaultAdapter();
        BTLE = BTAdapter.getBluetoothLeScanner();

        if(!BTAdapter.isEnabled())
            BTAdapter.enable();

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BTAdapter.isDiscovering())
                    BTAdapter.cancelDiscovery();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                filter.addAction(ACTION_DISCOVERY_FINISHED);
                filter.addAction(ACTION_DISCOVERY_STARTED);
                //registerReceiver(bReceiver, filter);
                Log.d("INFO", "başladı ");

                ScanCallback scan = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        results.add("device: " + result.getDevice() + ", rss: " + result.getRssi());
                        adapter = new ArrayAdapter<>(MainActivity.this, R.layout.main_list, results);
                        listView.setAdapter(adapter);

                        Log.d("INFO", "device: " + result.getDevice() + ", rss: " + result.getRssi());
                    }
                };
                BTLE.startScan(scan);
                scan.onBatchScanResults(resultLE);

                //BTAdapter.startDiscovery();

            }});
    }


    /** Called just before the activity is destroyed. */
    @Override
    public void onDestroy() {
        if(BTAdapter.isDiscovering())
            BTAdapter.cancelDiscovery();
        unregisterReceiver(bReceiver);
        super.onDestroy();
    }

    protected double calculateAccuracy(double rssi) {
        return Math.pow(10d, ( txPower - rssi) / (10 * 2));

    }
}
