package com.avengergear.iots.IOTSBusGoogleMapClient;

import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.Timer;

public class GoogleMapMarkerManager {

    public static HashMap<String, Marker> markers = new HashMap<String, Marker>();

    public static void addMarker(String index, Marker marker) {
        if( !markers.containsKey(index) ) {
            markers.put(index, marker);
        }
    }

    public static Marker getMarker(String index) {
        if(markers.containsKey(index) ) {
            return markers.get(index);
        }else
            return null;
    }

    public static boolean removeMarker(String index) {
        if( markers.containsKey(index) ) {
            markers.remove(index);
            return true;
        }
        return false;
    }

    public static HashMap<String, Marker> getMarkers() {
        return markers;
    }

    public static void removeMarkers() {
        markers.clear();
    }
}
