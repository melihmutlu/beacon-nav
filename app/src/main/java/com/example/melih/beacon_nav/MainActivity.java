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

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;

public class MainActivity extends AppCompatActivity {

    HashMap<String, ScanResult> listItems=new HashMap<String, ScanResult>();
    private BluetoothAdapter BTAdapter;
    private BluetoothLeScanner BTLE;
    private DeviceAdapter adapter;
    private Button scanBtn;
    private ListView listView;
    private TextView posView;
    private CanvasView canvasView;
    private ArrayList<String> deviceFilter;
    private ArrayList<Integer> rssiValues = new ArrayList<>();
    private ArrayList<Map.Entry<String, ScanResult>>  list = new ArrayList<>();
    private static Map<String, Queue<Integer>> positionCache;                       // last n measurements of a beacon
    private static Map<Tuple, Double> estimationMap;                // distance estimation from a beacon with respect to getAverage() estimator
    private static Map<String, Tuple> positionMap;                  // position of a beacon
    private boolean log = false;
    private String logAddress = "";
    private ProgressDialog progress;
    private ScanCallback scanCallback;
    private static Tuple point;
    private static NormalDistribution Z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        listView = (ListView) findViewById(R.id.list);
        posView = (TextView) findViewById(R.id.pos);
        canvasView = (CanvasView) findViewById(R.id.canvas);

        BTAdapter = getDefaultAdapter();
        BTLE = BTAdapter.getBluetoothLeScanner();
        deviceFilter = new ArrayList<>();
        list = new ArrayList<Map.Entry<String, ScanResult>>();
        rssiValues = new ArrayList<>();
        progress = new ProgressDialog(this);
        progress.setMessage("Wait...");

        point = new Tuple(3.0, 4.5, 3.0);
        Z = new NormalDistribution(0, 1);

        if(!BTAdapter.isEnabled()) //enable bluetooth
            BTAdapter.enable();

        adapter = new DeviceAdapter(this , R.layout.item_view , list);
        listView.setAdapter(adapter);

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                positionCache = new HashMap<>();
                positionMap = new HashMap<>();
                estimationMap = new HashMap<>();

                positionCache.put("D0:30:AD:84:07:40", new LinkedList<Integer>());  //orta
                positionMap.put("D0:30:AD:84:07:40", new Tuple(0.0, 13.6, 3.0));

                positionCache.put("E0:2E:E2:ED:86:64", new LinkedList<Integer>());  //sağ
                positionMap.put("E0:2E:E2:ED:86:64", new Tuple(9.0, 13.6, 3.0));

                positionCache.put("FC:73:08:31:50:42", new LinkedList<Integer>());  //üst
                positionMap.put("FC:73:08:31:50:42", new Tuple(0.0, 0, 3.0));

                positionCache.put("D0:8B:08:63:C4:61", new LinkedList<Integer>());  // arbitrary
                positionMap.put("D0:8B:08:63:C4:61", new Tuple(0.0, 0.0, 0.0));

                if(BTAdapter.isDiscovering())
                    BTAdapter.cancelDiscovery();
                Log.d("INFO", "start ");
                scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {

                        Intent intent = new Intent(MainActivity.this, DeviceDetail.class);
                        Log.d("INFO" , result.getDevice().getAddress());
                        listItems.put(result.getDevice().getAddress(), result);
                        list.clear();
                        list.addAll(listItems.entrySet());
                        adapter = new DeviceAdapter(MainActivity.this , R.layout.item_view , list);

                        String address = result.getDevice().getAddress();
                        byte[] b = result.getScanRecord().getBytes();
                        int txp = 0;
                        try{
                            String temp = String.format("%02x ", b[29]);
                            txp = -(256 - Integer.parseInt(temp.substring(0,temp.length()-1),16));
                        }catch (NullPointerException e){
                            Log.d(result.getDevice().getAddress(), e.toString());
                        }

                        if (positionCache.containsKey(address)) {

                            //*/

                            Queue<Integer> q = positionCache.get(address);
                            if (q == null) q = new LinkedList<Integer>();
                            if (q.size() < 10) {
                                q.add(result.getRssi());
                            } else {
                                q.poll();
                                q.add(result.getRssi());
                            }

                            //*/

                            Tuple pos= positionMap.get(address);
                            estimationMap.put(pos, calculateDistance(txp, getAverage(address)));
                            canvasView.setBeaconList(estimationMap);
                            //estimationMap.put(pos, calculateDistance(txp, result.getRssi()));
                            double a = -1;
                            double avgX = 0;
                            double avgY = 0;
                            double avgZ = 0;
                            List<Tuple> dotList = new LinkedList<Tuple>();

                            for(int j=0; j<1000; j++) {
                                a = getPosition(estimationMap);
                                dotList.add(new Tuple(point.x, point.y, point.z));
                                avgX = avgX + point.x;
                                avgY = avgY + point.y;
                                avgZ = avgZ + point.z;
                            }
                            canvasView.setDots(dotList);

                            point.x = avgX / 1000;
                            point.y = avgY / 1000;
                            point.z = avgZ / 1000;
                            posView.setText("x: " + point.x + ", y: " + point.y + ", z: " + point.z + ", pr: " + a);
                        }

                        Log.d("INFO", "device: " + result.getDevice() + ", rssi: " + result.getRssi() );
                        if(log && result.getDevice().getAddress().equals(logAddress) && (rssiValues.size() < 10)){
                            progress.show();
                            rssiValues.add(result.getRssi());
                            intent.putExtra("tx" , result.getScanRecord().getBytes());
                            Log.d("LOGGING" , rssiValues.toString());
                        }else if(rssiValues.size() >= 10){
                            progress.dismiss();
                            intent.putIntegerArrayListExtra("rssi", rssiValues);
                            startActivity(intent);
                        }

                        if(!log){
                            ((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
                            Log.d("INFO", "adapter set");
                        }

                    }
                };
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                List<ScanFilter> filters = new ArrayList<>();
                BTLE.startScan(filters, settings, scanCallback);

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
        adapter = new DeviceAdapter(MainActivity.this , R.layout.item_view , list);
        listView.setAdapter(adapter);
        progress.dismiss();
    }

    /*
    @Override
    protected void onPause() {
        super.onPause();
        if(BTAdapter.isDiscovering())
            BTAdapter.cancelDiscovery();
        BTLE.stopScan(scanCallback);
        log = false;
        logAddress = "";
        list.clear();
        listItems.clear();
        rssiValues.clear();
        adapter = new DeviceAdapter(MainActivity.this , R.layout.item_view , list);
        listView.setAdapter(adapter);
        progress.dismiss();

    }
    */

    protected static double calculateDistance(int txPower, double rssi) {
        double n = 2;
        return Math.pow(10d, ((double) txPower - rssi) / (10 * n));
        /*
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
        */
    }

    protected static double getPosition(Map<Tuple, Double> m){

        double pr_new = 1;
        double pr_old = 1;

        Random r = new Random();
        double x = point.x + r.nextGaussian()*2;
        double y = point.y + r.nextGaussian()*2;
        double z = point.z + r.nextGaussian()*2;

        for(Tuple t : m.keySet()){
            double d_new = Math.sqrt( Math.pow(t.x - x,2) + Math.pow(t.y - y,2) + Math.pow(t.z - z,2));
            double d = m.get(t);
            pr_new = pr_new * Z.cumulativeProbability(d-d_new);
        }

        for(Tuple t : m.keySet()){
            double d_old = Math.sqrt( Math.pow(t.x - point.x,2) + Math.pow(t.y - point.y,2) + Math.pow(t.z - point.z,2));
            double d = m.get(t);
            pr_old = pr_old * Z.cumulativeProbability(d-d_old);
        }

        Random r2 = new Random();
        double a = r2.nextDouble();

        if ( a < pr_new / pr_old) {
            point.x = x;
            point.y = y;
            point.z = z;
        }

        return pr_new/pr_old;

        /*
        // normalising factor
        for ( double e: m.values()) {
            Z = Z + 1 / e;
        }

        // accumulate weighted beacon positions
        for ( Tuple t : m.keySet()) {
            double temp = (1 / m.get(t)) / Z;
            posX = posX + temp * t.x;
            posY = posY + temp * t.y;
            posZ = posZ + temp * t.z;
        }
        */
    }

    // n-point running average estimator
    //

    public static double getAverage(String s) {

        if ( !positionCache.containsKey(s) ) return 0;

        double mean = 0;
        Queue<Integer> cache = positionCache.get(s);

        for (int i : cache) {
            mean = mean + i;
        }
        mean = mean / cache.size();
        return mean;
    }
}

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