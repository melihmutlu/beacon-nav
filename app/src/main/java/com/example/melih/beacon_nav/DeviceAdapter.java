package com.example.melih.beacon_nav;

import android.bluetooth.le.ScanResult;
import android.content.Context;
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
    private  ArrayList<Map.Entry<String, ScanResult>>  mDevices;
    private Context context;
    private TextView name , rssi ,tx;

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

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.item_view , parent, false);
        name = (TextView) convertView.findViewById(R.id.name);
        rssi = (TextView) convertView.findViewById(R.id.rssi);
        tx = (TextView) convertView.findViewById(R.id.tx);


        name.setText(mDevices.get(position).getValue().getScanRecord().getDeviceName());
        rssi.setText("rssi  :   " + mDevices.get(position).getValue().getRssi() + "");
        tx.setText( "tx power   :   " + mDevices.get(position).getValue().getScanRecord().getTxPowerLevel() + "");

        return convertView;
    }
}