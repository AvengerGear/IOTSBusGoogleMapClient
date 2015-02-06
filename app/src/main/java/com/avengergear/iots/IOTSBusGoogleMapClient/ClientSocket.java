package com.avengergear.iots.IOTSBusGoogleMapClient;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.avengergear.iots.IOTSAndroidClientLibrary.ContentType;
import com.avengergear.iots.IOTSAndroidClientLibrary.IOTS;
import com.avengergear.iots.IOTSAndroidClientLibrary.IOTSException;
import com.avengergear.iots.IOTSAndroidClientLibrary.IOTSMessageCallback;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;



public class ClientSocket extends AsyncTask<Void, Void, Void> {

    private Socket socket;

    private static final int SocketClientPORT = 6000;

    private IOTS mIotsClient;
    private String mServerAddr;
    private String mVirtualServerAddr;
    private String response = "";
    private JSONObject mJSONObject;
    private String mData;
    private int mPacketType;

    public ClientSocketOnComplete completion;

    ClientSocket(IOTS iotsClient, String virtualAddr, String addr, int packetType, String data) {
        mIotsClient = iotsClient;
        mServerAddr = addr;
        mVirtualServerAddr = virtualAddr;
        mPacketType = packetType;
        mData = data;
    }

    @Override
    protected void onPreExecute () {

    }

    @Override
    protected Void doInBackground(Void... arg0) {

        /*
         * Register iots client (Asynctask because it will block Main UI)
         */
        try {
            if(!mIotsClient.isConnected()){
                mIotsClient.connect();
                mIotsClient.setConnect(true);
            }

            if(mPacketType == EnumType.SUBSCRIBE) {
                mIotsClient.createTopic(mIotsClient.getEndpointTopic() + "/" + mData);
                mIotsClient.subscribe(mIotsClient.getEndpointTopic() + "/" + mData);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (IOTSException e) {
            e.printStackTrace();
        }

        /*
         * Create JSON Object
         */
        mJSONObject = new JSONObject();
        try {
            mJSONObject.put("com.avengergear.iots.IOTSBusGoogleMapClient.ClientTopicID", mIotsClient.getEndpointTopic() + "/" + mData);
            mJSONObject.put("com.avengergear.iots.IOTSBusGoogleMapClient.PacketType", mPacketType);
            mJSONObject.put("com.avengergear.iots.IOTSBusGoogleMapClient.Data", mData);
            mJSONObject.put("com.avengergear.iots.IOTSBusGoogleMapClient.RefreshFreq", 5);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /* Step 1 :
         * [ClientTopicID, BusNumber, RefreshFreq] --> Server
         */
        try {
            socket = new Socket(mVirtualServerAddr, SocketClientPORT);

            ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];

            int bytesRead;
            InputStream in = socket.getInputStream();
            PrintStream out = new PrintStream(socket.getOutputStream());

            out.println(mJSONObject);

            /*
             * notice:
             * inputStream.read() will block if no data return
             */
            while ((bytesRead = in.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d("IOTS", "Receive = " + response);

        //postHandler handler = new postHandler(response);
        //handler.run();

        try {
            completion.onComplete(mIotsClient, socket, mJSONObject.getString("com.avengergear.iots.IOTSBusGoogleMapClient.Data"), response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onPostExecute(result);
    }
}
