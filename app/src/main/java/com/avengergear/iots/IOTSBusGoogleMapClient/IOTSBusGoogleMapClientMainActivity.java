package com.avengergear.iots.IOTSBusGoogleMapClient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.avengergear.iots.IOTSAndroidClientLibrary.ContentType;
import com.avengergear.iots.IOTSAndroidClientLibrary.IOTSException;
import com.avengergear.iots.IOTSAndroidClientLibrary.IOTSMessageCallback;
import com.avengergear.iots.IOTSBusGoogleMapClient.google.GoogleNearByDetailParser;
import com.avengergear.iots.IOTSBusGoogleMapClient.google.GoogleNearByParser;
import com.avengergear.iots.IOTSBusGoogleMapClient.google.GooglePlaceParser;
import com.avengergear.iots.IOTSBusGoogleMapClient.google.Route;
import com.avengergear.iots.IOTSBusGoogleMapClient.google.Routing;
import com.avengergear.iots.IOTSBusGoogleMapClient.google.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import com.avengergear.iots.IOTSAndroidClientLibrary.IOTS;

import org.eclipse.paho.client.mqttv3.MqttException;

public class IOTSBusGoogleMapClientMainActivity extends FragmentActivity implements RoutingListener, GoogleMap.OnMapLongClickListener, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener
{
    protected GoogleMap map;
    protected LatLng location;
    protected Marker location_marker;
    protected Routing routing;
    protected GooglePlaceNearbyBusStation mGooglePlaceNearbyBusStation;
    protected ProgressDialog mProgressDialog;
    protected List<String> mBusNumArray;
    protected String ipaddr;

    private String serverIP = "192.168.2.1";
    private String virtualserverIP = "192.168.2.4";

    private GoogleMapMarkerManager googleMapMarkerManager;

    private IOTSClientSingleton iotsClientSingleton;

    private void mapCleanUp()
    {
        if(map != null)
            map.clear();
    }

    private void displayProgressBar(int resid) {
        if (null == mProgressDialog) {
            mProgressDialog = ProgressDialog.show(this, null, getResources().getString(resid), false, true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    if (mGooglePlaceNearbyBusStation != null) {
                        mGooglePlaceNearbyBusStation.cancel(true);
                        mGooglePlaceNearbyBusStation = null;
                    }
                }
            });
        } else {
            mProgressDialog.setMessage(getResources().getString(resid));
        }
    }

    private void dismissProgressBar() {
        if (null != mProgressDialog) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void nearbyBuSearchExecution()
    {
        displayProgressBar(R.string.loading_text);
        try {
            mGooglePlaceNearbyBusStation = new GooglePlaceNearbyBusStation();
            mGooglePlaceNearbyBusStation.registerListener(this);
            mGooglePlaceNearbyBusStation.execute(location);
        } catch(RejectedExecutionException e) {
            dismissProgressBar();
            e.printStackTrace();
        }
    }

    private void routingExecution()
    {
        routing = new Routing(Routing.TravelMode.WALKING);
        routing.registerListener(this);

        routing.execute(location, location);
    }

    private void notifyUser(String alert, String title, String body)
    {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher, alert,
                System.currentTimeMillis());
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = Color.MAGENTA;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, IOTSBusGoogleMapClientMainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(this, title, body, contentIntent);
        nm.notify(1, notification);
    }

    private boolean sendRegisterDataToServer(Context context, String busNumber)
    {
        displayProgressBar(R.string.loading_text);

        ClientSocket clientSocket = new ClientSocket(iotsClientSingleton.getInstance(), virtualserverIP, serverIP, EnumType.SUBSCRIBE, busNumber);
        clientSocket.completion = new ClientSocketOnComplete() {
            @Override
            public void onComplete(IOTS iotsClient, Socket socket, String BusNumber, String response) {
                dismissProgressBar();

                if(response.equals("OK")) {
                    iotsClient.addTopicCallback(iotsClient.getEndpointTopic() + "/" + BusNumber, new IOTSMessageCallback(){
                        @Override
                        public void onMessage(String topic, String threadId,
                                              String source, ContentType type, Object content,
                                              int status) {
                            Log.d("IOTSTest", "Message Received:" + content.toString());
                            //notifyUser("Avengergear IOTSClient", "Message Received", content.toString());
                        }
                    });

                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(getBaseContext(), "Bus Number " + BusNumber + " register success !", Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getBaseContext(), "Response from server error, please try again !", Toast.LENGTH_LONG).show();
                    googleMapMarkerManager.removeMarker(BusNumber);
                    location_marker.remove();
                }
            }
        };
        clientSocket.execute();

        return true;
    }

    private boolean sendUnRegisterDataToServer(Context context, String busNumber)
    {
        displayProgressBar(R.string.loading_text);

        ClientSocket clientSocket = new ClientSocket(iotsClientSingleton.getInstance(), virtualserverIP, serverIP, EnumType.UNSUBSCRIBE, busNumber);
        clientSocket.completion = new ClientSocketOnComplete() {
            @Override
            public void onComplete(IOTS iotsClient, Socket socket, String BusNumber, String response) {
                dismissProgressBar();

                if(response.equals("OK")) {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(getBaseContext(), "Unsubscribe success !", Toast.LENGTH_LONG).show();
                }else
                    Toast.makeText(getBaseContext(), "Response from server error, please try again !", Toast.LENGTH_LONG).show();
            }
        };
        clientSocket.execute();

        return true;
    }

    /**
     * This activity loads a map and then displays the route and pushpins on it.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        ipaddr = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        mBusNumArray = new ArrayList<String>();

        //serverIP = settings.getString("SERVERIP", "192.168.2.1");
        //virtualserverIP = settings.getString("VIRTUALSERVERIP", "192.168.2.4");
        //Log.d("IOTSBusGoogleMapClient", "Server IP: " + serverIP + " , Virtual Server IP: " + virtualserverIP);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if(status != ConnectionResult.SUCCESS){ // Google Play Services are not available
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        }
        else { // Google Play Services are available
            SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            map = fm.getMap();
            map.setOnMapLongClickListener(this);
            map.setOnMarkerClickListener(this);
            map.setOnInfoWindowClickListener(this);
            map.setMyLocationEnabled(true);

            // Getting LocationManager object from System Service LOCATION_SERVICE
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Creating a criteria object to retrieve provider
            Criteria criteria = new Criteria();

            // Getting the name of the best provider
            String provider = locationManager.getBestProvider(criteria, true);

            // Getting Current Location
            Location location = locationManager.getLastKnownLocation(provider);

            if (location != null)
                onLocationChanged(location);

            locationManager.requestLocationUpdates(provider, 20000, 0, this);

            Toast.makeText(this, "Long press for search bus location", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy()
    {
        for(Map.Entry<String, Marker> entry : googleMapMarkerManager.getMarkers().entrySet()) {
            Log.d("IOTS", "onDestroy: entry.getKey() = " + entry.getKey());

            try {
                iotsClientSingleton.getInstance().unsubscribe(iotsClientSingleton.getInstance().getEndpointTopic() + "/" + entry.getKey());
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        iotsClientSingleton.getInstance().deleteEndpoint();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        menu.add(0,0,0,"Settings");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {
            case 0:
                // Get the layout inflater
                LayoutInflater inflater = this.getLayoutInflater();
                View input = inflater.inflate(R.layout.ipport_dialog, null);

                final EditText ServerIP = (EditText) input
                        .findViewById(R.id.serverip);
                final EditText VirtualServerIP = (EditText) input
                        .findViewById(R.id.virtualserverip);

                // Use the Builder class for convenient dialog construction
                final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_launcher)
                        .setView(input)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                //SharedPreferences settings = getSharedPreferences("IOTSBusGoogleMapClient", 0);

                                //SharedPreferences.Editor editor = settings.edit();
                                //editor.putString("SEVERIP",  ServerIP.getText().toString());
                                //editor.putString("VIRTUALSERVERIP",  VirtualServerIP.getText().toString());
                                //editor.commit();
                                serverIP = ServerIP.getText().toString();
                                virtualserverIP = VirtualServerIP.getText().toString();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                // Create the AlertDialog object and return it
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
        }

        return true;
    }

    @Override
    public void onLocationChanged(Location location) {

        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        // Creating a LatLng object for the current location
        LatLng latLng = new LatLng(latitude, longitude);

        // Showing the current location in Google Map
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        map.animateCamera(CameraUpdateFactory.zoomTo(15));

        Log.d("IOTSBusGoogleMapRoute", "Latitude: " + latitude + ", Longitude: " + longitude);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onMapLongClick(LatLng point) {
        Log.d("IOTSBusGoogleMapRoute", "onMapLongClick");
            //mapCleanUp();

            location = point;
            // Location Marker
            location_marker = map.addMarker(new MarkerOptions()
                    .position(point)
                    .snippet("Tap here to remove this marker")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .draggable(false));

            nearbyBuSearchExecution();
    }


    @Override
    public void onInfoWindowClick(Marker marker) {
        Log.d("IOTSBusGoogleMapRoute", "onInfoWindowClick");

        /*for(Map.Entry<String, Marker> entry : googleMapMarkerManager.getMarkers().entrySet()) {
            Log.d("IOTS", "entry.getKey() = " + entry.getKey() + " marker.getTitle() = " + marker.getTitle());
            if(entry.getKey().equals(marker.getTitle()) ){
                Toast.makeText(getBaseContext(), "Unsubscribe", Toast.LENGTH_SHORT).show();
                sendUnRegisterDataToServer(getBaseContext(), entry.getKey());
                googleMapMarkerManager.removeMarker(entry.getKey());
                marker.remove();
            }
        }*/
        Marker markerSelected = googleMapMarkerManager.getMarker(marker.getTitle());
        if(markerSelected != null){
            Toast.makeText(getBaseContext(), "Unsubscribe", Toast.LENGTH_SHORT).show();
            sendUnRegisterDataToServer(getBaseContext(), markerSelected.getTitle());
            markerSelected.remove();
            googleMapMarkerManager.removeMarker(markerSelected.getTitle());
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.d("IOTSBusGoogleMapRoute", "onMarkerClick");

        for(Map.Entry<String, Marker> entry : googleMapMarkerManager.getMarkers().entrySet()) {
            if(entry.getKey() == marker.getTitle() ){
                Toast.makeText(getBaseContext(), "Unsubscribe", Toast.LENGTH_SHORT).show();
                sendUnRegisterDataToServer(getBaseContext(), entry.getKey());
                googleMapMarkerManager.removeMarker(entry.getKey());
                marker.remove();
                return true;
            }
        }

        return false;

        /*if (marker.equals(location_marker))
        {
            Toast.makeText(getBaseContext(), "Unsubscribe", Toast.LENGTH_SHORT).show();
            sendUnRegisterDataToServer(getBaseContext(), null);
            mapCleanUp();
            return true;
        }
        return false;*/
    }

    @Override
    public void onRoutingFailure() {
      // The Routing request failed
    }

    @Override
    public void onRoutingStart() {
      // The Routing Request starts
    }

    @Override
    public void onRoutingSuccess(PolylineOptions mPolyOptions, Route route) {
      PolylineOptions polyoptions = new PolylineOptions();
      polyoptions.color(Color.BLUE);
      polyoptions.width(10);
      polyoptions.addAll(mPolyOptions.getPoints());
      map.addPolyline(polyoptions);
    }

    public class GooglePlaceNearbyBusStation extends AsyncTask<LatLng, Void, Boolean> {
        protected ArrayList<RoutingListener> _aListeners;
        protected String mKNightWengKey = "AIzaSyB_6wuOUcV0M8KcQEhoieT3WYv1omu9ZP8";
        private LatLng mLocation;

        public GooglePlaceNearbyBusStation() {
            this._aListeners = new ArrayList<RoutingListener>();
        }

        public void registerListener(RoutingListener mListener) {
            _aListeners.add(mListener);
        }

        protected void dispatchOnStart() {
            for (RoutingListener mListener : _aListeners) {
                mListener.onRoutingStart();
            }
        }

        protected void dispatchOnFailure() {
            for (RoutingListener mListener : _aListeners) {
                mListener.onRoutingFailure();
            }
        }

        protected void dispatchOnSuccess(PolylineOptions mOptions, Route route) {
            for (RoutingListener mListener : _aListeners) {
                mListener.onRoutingSuccess(mOptions, route);
            }
        }

        /**
         * Performs the call to the google maps API to acquire routing data and
         * deserializes it to a format the map can display.
         *
         * @param aPoints
         * @return
         */
        @Override
        protected Boolean doInBackground(LatLng... aPoints) {
            /*for (LatLng mPoint : aPoints) {
                if (mPoint == null) return false;
            }*/
            mLocation = aPoints[0];

            List<String> referenceListArray = new GooglePlaceParser(constructGooglePlaceURL(mLocation)).parse();
            if(referenceListArray.isEmpty()) return false;

            for (String reference : referenceListArray) {
                String nearByUrl = new GoogleNearByParser(constructGoogleNearByURL(reference)).parse();
                if(nearByUrl == null) return false;
                mBusNumArray = new GoogleNearByDetailParser(nearByUrl).parse();
                if(mBusNumArray == null) return false;
            }

            return true;
        }

        protected String constructGooglePlaceURL(LatLng point) {
            String sJsonURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";

            final StringBuffer mBuf = new StringBuffer(sJsonURL);
            mBuf.append("key=");
            mBuf.append(mKNightWengKey);
            mBuf.append("&location=");
            mBuf.append(point.latitude);
            mBuf.append(',');
            mBuf.append(point.longitude);
            mBuf.append("&sensor=true&radius=100&types=bus_station");

            Log.d("IOTSBusGoogleMapRoute", "place URL = " + mBuf.toString());

            return mBuf.toString();
        }

        protected String constructGoogleNearByURL(String reference) {
            String sJsonURL = "https://maps.googleapis.com/maps/api/place/details/json?";

            final StringBuffer mBuf = new StringBuffer(sJsonURL);
            mBuf.append("key=");
            mBuf.append(mKNightWengKey);
            mBuf.append("&sensor=true&reference=");
            mBuf.append(reference);

            Log.d("IOTSBusGoogleMapRoute", "nearby URL = " + mBuf.toString());

            return mBuf.toString();
        }

        @Override
        protected void onPreExecute() {
            //dispatchOnStart();
            try {
                iotsClientSingleton = new IOTSClientSingleton(getBaseContext(), serverIP);
            } catch (MqttException e) {
                e.printStackTrace();
            } catch (IOTSException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            dismissProgressBar();

            if (bool == false) {
                Toast.makeText(getBaseContext(), "Can't find any bus number", Toast.LENGTH_LONG).show();
                return;
            }

            final CharSequence[] items = mBusNumArray.toArray(new CharSequence[mBusNumArray.size()]);
            AlertDialog.Builder builder = new AlertDialog.Builder(IOTSBusGoogleMapClientMainActivity.this);
            builder.setCancelable(false);
            builder.setItems(items, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    Toast.makeText(getBaseContext(), items[which].toString(), Toast.LENGTH_SHORT).show();
                    if(googleMapMarkerManager.getMarker(items[which].toString()) == null) {
                        location_marker.setTitle(items[which].toString());
                        googleMapMarkerManager.addMarker(items[which].toString(), location_marker);
                        sendRegisterDataToServer(getBaseContext(), items[which].toString());
                    }else {
                        Toast.makeText(getBaseContext(), "Bus " + items[which].toString() + " already exists", Toast.LENGTH_SHORT).show();
                        googleMapMarkerManager.removeMarker(items[which].toString());
                        location_marker.remove();
                    }
                }

            });
            AlertDialog alert = builder.create();
            alert.show();
        }//end onPostExecute method
    }
}
