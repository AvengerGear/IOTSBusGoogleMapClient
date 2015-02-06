package com.avengergear.iots.IOTSBusGoogleMapClient;

import android.content.Context;

import com.avengergear.iots.IOTSAndroidClientLibrary.IOTS;
import com.avengergear.iots.IOTSAndroidClientLibrary.IOTSException;

import org.eclipse.paho.client.mqttv3.MqttException;

public class IOTSClientSingleton {
    private static IOTS instance = null;
    private static Context mContext;
    private static String mServerIP;

    public IOTSClientSingleton(Context context, String serverIP) throws MqttException, IOTSException {
        mContext = context;
        mServerIP = serverIP;
    }

    public static IOTS getInstance(){
        if(instance == null) {
            try {
                instance = new IOTS(mContext, "bd365ca0-a5ce-11e4-a612-f384a4355284", "432948f145287eb92ed9e251fcf9cf4f8c9c57496ad3d4ed774d54b7d3e3b03d", "tcp://" + mServerIP + ":1883");
            } catch (MqttException e) {
                e.printStackTrace();
            } catch (IOTSException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }
}