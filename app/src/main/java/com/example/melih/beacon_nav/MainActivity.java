package com.example.melih.beacon_nav;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity {

    HashMap<String, ScanResult> listItems=new HashMap<String, ScanResult>();
    private BluetoothAdapter BTAdapter;
    private BluetoothLeScanner BTLE;
    private DeviceAdapter adapter;
    private Button scanBtn;
    private ListView listView;
    private TextView posView;
    private List<ScanResult> resultLE;
    private ArrayList<String> deviceFilter;
    private ArrayList<Integer> rssiValues = new ArrayList<>();
    private  ArrayList<Map.Entry<String, ScanResult>>  list;
    private boolean log = false;
    private String logAddress = "";
    private ProgressDialog progress;
    private ScanCallback scanCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanBtn = (Button) findViewById(R.id.scanBtn);
        listView = (ListView) findViewById(R.id.list);
        posView = (TextView) findViewById(R.id.pos);
        BTAdapter = getDefaultAdapter();
        BTLE = BTAdapter.getBluetoothLeScanner();
        deviceFilter = new ArrayList<>();
        list = new ArrayList<Map.Entry<String, ScanResult>>();
        rssiValues = new ArrayList<>();
        progress = new ProgressDialog(this);
        progress.setMessage("Wait...");
        final WeightedAvgEstimator WAEst = new WeightedAvgEstimator();

        if(!BTAdapter.isEnabled()) //enable bluetooth
            BTAdapter.enable();

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (BTAdapter.isDiscovering())
                    BTAdapter.cancelDiscovery();

                Log.d("INFO", "start ");

                scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {

                        Intent intent = new Intent(MainActivity.this, DeviceDetail.class);
                        String address = result.getDevice().getAddress();

                        // Getting calibration value
                        byte[] b = result.getScanRecord().getBytes();
                        int txp = 0;
                        try {
                            String temp = String.format("%02x ", b[29]);
                            txp = -(256 - Integer.parseInt(temp.substring(0, temp.length() - 1), 16));
                        } catch (NullPointerException e) {
                            Log.d(result.getDevice().getAddress(), e.toString());
                        }

                        if (WAEst.isContains(address)) {
                            WAEst.run(address, result.getRssi());

                            Tuple position = WAEst.getPosition(address, calculateDistance(txp, WAEst.getAverage(address)));
                            posView.setText("x: " + position.x + ", y: " + position.y + ", z: " + position.z);

                            //ArrayList<Map.Entry<String, ScanResult>> list = new ArrayList<Map.Entry<String, ScanResult>>();
                            //listItems.put(result.getDevice().getAddress(), result);
                            //list.addAll(listItems.entrySet());

                        }

                        // Logging mode
                        if (log && result.getDevice().getAddress().equals(logAddress) && (rssiValues.size() < 10)) {
                            progress.show();
                            rssiValues.add(result.getRssi());
                            intent.putExtra("tx", result.getScanRecord().getBytes());
                            Log.d("LOGGING", rssiValues.toString());
                        } else if (rssiValues.size() >= 10) {
                            progress.dismiss();
                            intent.putIntegerArrayListExtra("rssi", rssiValues);
                            startActivity(intent);
                        }

                        listItems.put(result.getDevice().getAddress(), result);
                        list.clear();
                        list.addAll(listItems.entrySet());
                        adapter = new DeviceAdapter(MainActivity.this, R.layout.item_view, list);

                        //Set Listview
                        if (!log) {
                            ((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
                            Log.d("INFO", "adapter set");
                        }

                    }
                };
                // BLE scan settings
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                List<ScanFilter> filters = new ArrayList<>();
                BTLE.startScan(filters, settings, scanCallback);
                scanCallback.onBatchScanResults(resultLE);

            }
        });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    rssiValues.clear();
                    logAddress = ((TextView) view.findViewById(R.id.address)).getText().toString().substring(10);
                    log = true;
                }
            });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(BTAdapter.isDiscovering())
            BTAdapter.cancelDiscovery();
        BTLE.stopScan(scanCallback);
        log = false;
        logAddress = "";
        list.clear();
        listItems.clear();
        rssiValues.clear();
        adapter = new DeviceAdapter(MainActivity.this, R.layout.item_view, list);
        ((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
        progress.dismiss();
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
            double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return distance;
        }
    }
}


///////////////////////////////////////////////////////////////////////////////////////////////

/*
    protected static double partial_x(Map<Tuple<Double, Double>, Double> m, Tuple<Double, Double> loc){

        double result = 0;

        for(Tuple<Double, Double> axis : m.keySet()) {
            result = result + (loc.x - axis.x) * (Math.pow(loc.x-axis.x,2) + Math.pow(loc.y-axis.y,2) - m.get(axis));
        }
        return 4 * result;

    }

    protected static double partial_y(Map<Tuple<Double, Double>, Double> m, Tuple<Double, Double> loc){

        double result = 0;

        for(Tuple<Double, Double> axis : m.keySet()) {
            result = result + (loc.y - axis.y) * (Math.pow(loc.x-axis.x,2) + Math.pow(loc.y-axis.y,2) - m.get(axis));
        }
        return 4 * result;

    }

    protected static double fx(Map<Tuple<Double, Double>, Double> m, Tuple<Double, Double> loc) {

        double result = 0;

        for(Tuple<Double, Double> axis : m.keySet()) {
            result = result + Math.pow((Math.pow(loc.x-axis.x,2) + Math.pow(loc.y-axis.y,2) - m.get(axis)),2);
        }

        return result;
    }
*/


/*
## Steepest descent method

        Tuple<Double, Double> z_0 = new Tuple<Double, Double>(8.0/3, 10.0/3);
        double partial_a, partial_b, f_x;
        double partial_a_new = partial_x(m, z_0);
        double partial_b_new = partial_y(m, z_0);
        double lambda = 0.001;

        do {
            partial_a = partial_a_new;
            partial_b = partial_b_new;
            f_x = fx(m, z_0);
            z_0.x = z_0.x - f_x * partial_a / (Math.pow(partial_a,2) + Math.pow(partial_b,2));
            z_0.y = z_0.y - f_x * partial_b / (Math.pow(partial_a,2) + Math.pow(partial_b,2));
            partial_a_new = partial_x(m, z_0);
            partial_b_new = partial_y(m, z_0);
        } while ( partial_a * partial_a_new > 0 && partial_b * partial_b_new > 0 );

        do {
            z_0.x = z_0.x - lambda * partial_x(m, z_0);
            z_0.y = z_0.y - lambda * partial_y(m, z_0);
        } while ( partial_a * partial_a_new > 0 && partial_b * partial_b_new > 0 );

        return z_0;

*/

/*

    protected static double squareEstimate(String s, int txPower){

        if( !positionCache.containsKey(s) ) return 0;
        Queue<Integer> q = positionCache.get(s);

        double mean = calculateAccuracy(txPower, getAverage(s));
        double sample_var = 0;

        for (int a : q) {
            sample_var = sample_var + Math.pow(mean-a,2);
        }
        sample_var = sample_var / (q.size() - 1);

        double estimator = (Math.pow(mean,4)) / (Math.pow(mean,2) + sample_var);
        return estimator;

    }
*/