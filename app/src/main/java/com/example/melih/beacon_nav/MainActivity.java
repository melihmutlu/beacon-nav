package com.example.melih.beacon_nav;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> address ;
    private ListView  listView;
    private boolean state = true;
    private ArrayAdapter adapter;
    private ArrayList<String> results ;
    private double txPower = -59.0;
    private String beaconAddress ;
    private int counter = 0;
    private BluetoothAdapter mBTAdapter;
    private Button scanBtn;
    private TextView text ;
    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d("INFO", "searching");
            address.add("D0:30:AD:84:07:40");
            address.add("E0:2E:E2:ED:86:64");
            address.add("D0:8B:08:63:C4:61");
            address.add("FC:73:08:31:50:42");
            address.add("0C:D2:92:2E:5C:9C");   // MELIH-PC
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                Log.d("DEVICES", "Device: " + device.getName() + " " + rssi + " " + device.getAddress());
                beaconAddress = device.getAddress();
                if(address.contains(beaconAddress)){
                    Log.d("INFO", "Device found");
                    Log.d("DEVICES", "Device: " + device.getName() + " " + rssi + " " + device.getAddress());
                    double dist = calculateAccuracy(rssi);
                    results.add(device.getAddress() + "\n " + rssi + "\n " + dist);
                    adapter = new ArrayAdapter<>(MainActivity.this, R.layout.main_list, results);
                    listView.setAdapter(adapter);

                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("INFO", "finished");
            }else if( BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                counter = 0;
                results.clear();
            }
            address.clear();
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        address = new ArrayList<>();
        results = new ArrayList<>();
        mBTAdapter = getDefaultAdapter();
        listView = (ListView) findViewById(R.id.list);

        if(!mBTAdapter.isEnabled())
            mBTAdapter.enable();

        if (mBTAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("INFO", "clicked");
                if(mBTAdapter.isDiscovering())
                    mBTAdapter.cancelDiscovery();
                Log.d("INFO", "start");
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                registerReceiver(bReceiver, filter);
                mBTAdapter.startDiscovery();
            }});
    }


    @Override
    public void onDestroy() {
        if(mBTAdapter.isDiscovering())
            mBTAdapter.cancelDiscovery();
        unregisterReceiver(bReceiver);
        super.onDestroy();
    }

    protected double calculateAccuracy(double rssi) {
        return Math.pow(10d, ( txPower - rssi) / (10 * 2));

    }
}
