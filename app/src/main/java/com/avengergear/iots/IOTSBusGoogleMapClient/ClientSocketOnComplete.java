package com.avengergear.iots.IOTSBusGoogleMapClient;

import com.avengergear.iots.IOTSAndroidClientLibrary.IOTS;

import java.net.Socket;

public interface ClientSocketOnComplete {
    public void onComplete(IOTS iotsClient, Socket socket, String BusNumber, String response);
}
