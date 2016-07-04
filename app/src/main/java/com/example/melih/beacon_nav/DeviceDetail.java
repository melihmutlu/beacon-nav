package com.example.melih.beacon_nav;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Melih on 30.6.2016.
 */

public class DeviceDetail extends AppCompatActivity {

    private ArrayList<Integer> rssi ;
    private TextView avg, std_dev, est_dist, values;
    private String txValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_layout);
        rssi = getIntent().getIntegerArrayListExtra("rssi");
        avg = (TextView) findViewById(R.id.avg);
        std_dev = (TextView) findViewById(R.id.std_dev);
        values = (TextView) findViewById(R.id.values);

        String v = rssi.toString();
        values.setText(v.substring(1, v.length() - 1));
        avg.setText(""+  String.format("%.2f" , getAverage()));
        std_dev.setText("" + String.format("%.2f" , getDeviation()));

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
        return Math.sqrt(total/rssi.size() - 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("INFO" , "back pressed");
    }
}
