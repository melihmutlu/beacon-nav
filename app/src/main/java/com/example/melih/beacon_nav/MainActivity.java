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
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> address ;
    private String beaconAddress ;
    private int counter = 0;
    private BluetoothAdapter mBTAdapter;
    private Button scanBtn;
    private TextView text ;
    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            address.add("D0:30:AD:84:07:40");
            address.add("E0:2E:E2:ED:86:64");
            address.add("D0:8B:08:63:C4:61");
            address.add("FC:73:08:31:50:42");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("INFO", "Device found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                Log.d("DEVICES", "Device: " + device.getName() + " " + rssi + " " + device.getAddress());
                beaconAddress = device.getAddress();
                if(address.contains(beaconAddress)){
                    Log.d("DEVICES", "Device: " + device.getName() + " " + rssi + " " + device.getAddress());
                    text.setText(device.getAddress() + "\n " + rssi );
                }
            }else if(ACTION_DISCOVERY_FINISHED.equals(action)){
                mBTAdapter.startDiscovery();
            }else if(ACTION_DISCOVERY_STARTED.equals(action)){
            }
            address.clear();
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        text = (TextView) findViewById(R.id.deviceName);
        address = new ArrayList<>();
        mBTAdapter = getDefaultAdapter();

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
            if(mBTAdapter.isDiscovering())
                mBTAdapter.cancelDiscovery();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(ACTION_DISCOVERY_FINISHED);
            filter.addAction(ACTION_DISCOVERY_STARTED);
            registerReceiver(bReceiver, filter);
            Log.d("INFO", "başladı ");
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
}
