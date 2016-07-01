package com.example.melih.beacon_nav;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity {

    HashMap<String, ScanResult> listItems=new HashMap<String, ScanResult>();
    private BluetoothAdapter BTAdapter;
    private BluetoothLeScanner BTLE;
    private DeviceAdapter adapter;
    private Button scanBtn;
    private ListView listView;
    private List<ScanResult> resultLE;
    private ArrayList<String> deviceFilter;
    private ArrayList<Integer> rssiValues = new ArrayList<>();
    private  ArrayList<Map.Entry<String, ScanResult>>  list;
    private boolean log = false;
    private String logAddress = "";
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        listView = (ListView) findViewById(R.id.list);
        BTAdapter = getDefaultAdapter();
        BTLE = BTAdapter.getBluetoothLeScanner();
        deviceFilter = new ArrayList<>();
        list = new ArrayList<Map.Entry<String, ScanResult>>();
        rssiValues = new ArrayList<>();
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
                        Intent intent = new Intent(MainActivity.this, DeviceDetail.class);
                        listItems.put(result.getDevice().getAddress(), result);
                        list.addAll(listItems.entrySet());
                        if(log && result.getDevice().getAddress().equals(logAddress) && (rssiValues.size() < 15)){
                            progress = ProgressDialog.show(MainActivity.this, "", "Wait...", true);
                            rssiValues.add(result.getRssi());
                            intent.putExtra("tx" , result.getScanRecord().getBytes());
                            Log.d("LOGGING" , rssiValues.toString());
                        }else if(rssiValues.size() >= 10){
                            log = false;
                            logAddress = "";
                            progress.dismiss();
                            intent.putIntegerArrayListExtra("rssi", rssiValues);
                            startActivity(intent);
                        }
                        adapter = new DeviceAdapter(MainActivity.this , R.layout.item_view , list);
                        listView.setAdapter(adapter);
                        Log.d("INFO", "device: " + result.getDevice() + ", rssi: " + result.getRssi());
                    }
                };
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                ArrayList<ScanFilter> filters = new ArrayList<>();
                BTLE.startScan(filters, settings, scanCallback);
                scanCallback.onBatchScanResults(resultLE);

            }});

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    rssiValues.clear();
                    logAddress = ((TextView) view.findViewById(R.id.address)).getText().toString().substring(10);
                    log = true;
                }
            });
    }


    /** Called just before the activity is destroyed. */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(BTAdapter.isDiscovering())
            BTAdapter.cancelDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(BTAdapter.isDiscovering())
            BTAdapter.cancelDiscovery();
        rssiValues.clear();
    }

    protected static double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }



}
