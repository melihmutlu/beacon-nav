package com.example.melih.beacon_nav;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Melih on 24.6.2016.
 */
public class DeviceAdapter extends ArrayAdapter {
    public final String TAG = DeviceAdapter.class.getName();
    private  ArrayList<Map.Entry<String, ScanResult>>  mDevices;
    private Context context;
    private TextView name , rssi ,tx, dist, avgdist, address;

    public DeviceAdapter(Context context, int textViewResourceId,List<Map.Entry<String, ScanResult>> results) {
        super(context, textViewResourceId, results);
        mDevices = (ArrayList<Map.Entry<String, ScanResult>>) results;
        this.context = context;
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        String txValue = "";
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.item_view , parent, false);
        name = (TextView) convertView.findViewById(R.id.name);
        address = (TextView) convertView.findViewById(R.id.address);
        rssi = (TextView) convertView.findViewById(R.id.rssi);
        tx = (TextView) convertView.findViewById(R.id.tx);
        dist = (TextView) convertView.findViewById(R.id.distance);
        avgdist = (TextView) convertView.findViewById(R.id.avgdist);

        byte[] sr = mDevices.get(position).getValue().getScanRecord().getBytes();
        try{
            txValue = String.format("%02x ", sr[29]);
        }catch (NullPointerException e){
            Log.d(TAG, e.toString());
        }

        int txp = -(256 - Integer.parseInt(txValue.substring(0,txValue.length()-1),16));
        int rss = mDevices.get(position).getValue().getRssi();

        name.setText(mDevices.get(position).getValue().getDevice().getAddress());
        rssi.setText("rssi    : " + rss + "");
        tx.setText( "tx power: " + txp);
        dist.setText("distance: " + MainActivity.calculateDistance(txp, rss));
        avgdist.setText("average : " + MainActivity.calculateDistance(txp, MainActivity.getAverage(mDevices.get(position).getValue().getDevice().getAddress())));
        address.setText("address : " + mDevices.get(position).getValue().getDevice().getAddress());

        return convertView;
    }
}