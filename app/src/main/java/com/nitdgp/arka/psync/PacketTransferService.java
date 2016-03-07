package com.nitdgp.arka.psync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by arka on 7/3/16.
 */
public class PacketTransferService extends IntentService {


    private static final String ACTION_SEND_PACKET = "com.nitdgp.arka.psync.SEND_PACKET";
    public static final String EXTRAS_ADDRESS = "go_host";
    public static final String EXTRAS_PORT = "go_port";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public PacketTransferService(String name) {
        super(name);
    }

    public PacketTransferService(){
        super("PacketTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_PACKET)) {
            DatagramSocket datagramSocket = null;
            byte buffer[] = new byte[256];
            DatagramPacket datagramPacket = null;

            String host = intent.getExtras().getString(EXTRAS_ADDRESS);
            int port = intent.getExtras().getInt(EXTRAS_PORT);

            try {
                datagramSocket = new DatagramSocket(port, InetAddress.getByName(host));
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            buffer = "Hello".getBytes();

            try {
                datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(host), 8988);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            try {
                datagramSocket.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                datagramSocket.close();
            }
        }
    }
}
