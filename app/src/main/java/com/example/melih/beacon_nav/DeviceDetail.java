package com.example.melih.beacon_nav;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Created by Melih on 30.6.2016.
 */

public class DeviceDetail extends AppCompatActivity {

    private ArrayList<Integer> rssi ;
    private TextView avg, std_dev, est_dist;
    private String txValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rssi = getIntent().getIntegerArrayListExtra("rssi");
        byte[] record = getIntent().getByteArrayExtra("tx");
        avg = (TextView) findViewById(R.id.avg);
        std_dev = (TextView) findViewById(R.id.std_dev);
        est_dist = (TextView) findViewById(R.id.est_dist);

        try{
            txValue = String.format("%02x ", record[29]);
        }catch (NullPointerException e){
            Log.d("DeviceDetailActivity", e.toString());
        }

        avg.setText(""+ getAverage());
        std_dev.setText( ""+ getDeviation());
        est_dist.setText(MainActivity.calculateDistance();

    }

    private double getAverage(){
        double total =0;
        for(int i : rssi){
            total += i;
        }
        return total/rssi.size();
    }

    private double getDeviation(){
        double mean = getAverage();
        double total = 0;
        for(int i: rssi){
            total += (i- mean)*(i- mean);
        }
        return Math.sqrt(total/rssi.size());
    }

}
