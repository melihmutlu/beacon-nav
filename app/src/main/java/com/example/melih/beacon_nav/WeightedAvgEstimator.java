package com.example.melih.beacon_nav;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Melih on 4.7.2016.
 */
public class WeightedAvgEstimator {

    private static Map<String, Queue<Integer>> positionCache;       // last n measurements of a beacon
    private static Map<Tuple, Double> estimationMap;                // distance estimation from a beacon with respect to getAverage() estimator
    private static Map<String, Tuple> positionMap;                  // position of a beacon

    public WeightedAvgEstimator(){
        positionCache = new HashMap<>();
        positionMap = new HashMap<>();
        estimationMap = new HashMap<>();

        positionCache.put("D0:30:AD:84:07:40", new LinkedList<Integer>());  //orta
        positionMap.put("D0:30:AD:84:07:40", new Tuple(0.0, 0.0, 3.0));

        positionCache.put("E0:2E:E2:ED:86:64", new LinkedList<Integer>());  //sağ
        positionMap.put("E0:2E:E2:ED:86:64", new Tuple(9.0, 0.0, 3.0));

        positionCache.put("FC:73:08:31:50:42", new LinkedList<Integer>());  //üst
        positionMap.put("FC:73:08:31:50:42", new Tuple(0.0, 13.6, 3.0));

        positionCache.put("D0:8B:08:63:C4:61", new LinkedList<Integer>());  // arbitrary
        positionMap.put("D0:8B:08:63:C4:61", new Tuple(0.0, 0.0, 0.0));
    }

    public boolean isContains(String address){
        return positionCache.containsKey(address);
    }

    public Queue<Integer> getCache(String address){
        return positionCache.get(address);
    }

    public static Map<String, Queue<Integer>> getPositionCache() {
        return positionCache;
    }

    public static Map<Tuple, Double> getEstimationMap() {
        return estimationMap;
    }

    public static Map<String, Tuple> getPositionMap() {
        return positionMap;
    }

    public Tuple getPosition(String address , double dist){
        Tuple pos = getPositionMap().get(address);
        getEstimationMap().put(pos, dist);
        return getPos(getEstimationMap());
    }

    private Tuple getPos(Map<Tuple, Double> m){

        double Z = 0;
        double posX = 0;
        double posY = 0;
        double posZ = 0;

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

        return new Tuple(posX, posY, posZ);

    }

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

    public void run(String address , int rssi){
        Queue<Integer> q = getCache(address);
        if (q == null) q = new LinkedList<Integer>();
        if (q.size() < 10) {
            q.add(rssi);
        } else {
            q.poll();
            q.add(rssi);
        }
    }
}

