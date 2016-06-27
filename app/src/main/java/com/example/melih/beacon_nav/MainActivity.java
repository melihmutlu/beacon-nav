package com.example.melih.beacon_nav;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity {

    HashMap<String, ScanResult> listItems=new HashMap<String, ScanResult>();
    private double txPower = -59.0;
    private BluetoothAdapter BTAdapter;
    private BluetoothLeScanner BTLE;
    private DeviceAdapter adapter;
    private Button scanBtn;
    private ListView listView;
    private List<ScanResult> resultLE;
    private ArrayList<String> deviceFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        listView = (ListView) findViewById(R.id.list);
        BTAdapter = getDefaultAdapter();
        BTLE = BTAdapter.getBluetoothLeScanner();
        deviceFilter = new ArrayList<>();
        // addresses to filter
        deviceFilter.add("D0:30:AD:84:07:40");
        deviceFilter.add("E0:2E:E2:ED:86:64");
        deviceFilter.add("D0:8B:08:63:C4:61");
        deviceFilter.add("FC:73:08:31:50:42");
        deviceFilter.add("D4:22:FF:09:00:E9");




        if(!BTAdapter.isEnabled()) // enable bluetooth
            BTAdapter.enable();

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BTAdapter.isDiscovering())
                    BTAdapter.cancelDiscovery();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                filter.addAction(ACTION_DISCOVERY_FINISHED);
                filter.addAction(ACTION_DISCOVERY_STARTED);
                Log.d("INFO", "start ");

                ScanCallback scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                       // if(deviceFilter.contains(result.getDevice().getAddress()))
                            listItems.put(result.getDevice().getAddress() , result);
                        //TODO custom adapter
                        ArrayList<Map.Entry<String, ScanResult>>  list = new ArrayList<Map.Entry<String, ScanResult>>();
                        list.addAll(listItems.entrySet());
                        adapter = new DeviceAdapter(MainActivity.this , R.layout.item_view , list);
                        listView.setAdapter(adapter);
                        Log.d("INFO", "device: " + result.getDevice() + ", rssi: " + result.getRssi() );
                    }
                };
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                ArrayList<ScanFilter> filters = new ArrayList<>();
                BTLE.startScan(filters, settings, scanCallback);
                scanCallback.onBatchScanResults(resultLE);

            }});
    }


    /** Called just before the activity is destroyed. */
    @Override
    public void onDestroy() {
        if(BTAdapter.isDiscovering())
            BTAdapter.cancelDiscovery();
        super.onDestroy();
    }

}
