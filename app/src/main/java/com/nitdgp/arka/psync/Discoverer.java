package com.nitdgp.arka.psync;
/**
 * @author  : Arka Prava
 * @version : 4 March 2016
 */
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * The Discoverer module
 */
public class Discoverer extends ActionBarActivity  {

    TextView receivedMessage;
    EditText broadcastMessage;
    Button broadcast;
    Button startClient;
    android.os.Handler handler;

    private static final String SERVER_IP = "192.168.1.255";
    private static int PORT = 4446;
    private static boolean serverRunning = false;
    InetAddress group ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discoverer);

        /*
        Initialise
         */
        receivedMessage = (TextView)findViewById(R.id.text_received_message);
        broadcastMessage = (EditText)findViewById(R.id.editText_broadcast_message);
        broadcast = (Button)findViewById(R.id.btn_broadcast);
        startClient = (Button)findViewById(R.id.btn_start_client);
        /*
         * Check if device is connected via wifi
         */
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        try {
            group = getBroadcastAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(networkInfo.isConnected()){
            displayToast("Connected via wifi");
            startClient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new ClientThread().start();
                }
            });
        }else{
            displayToast("Not connected");
            broadcast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!serverRunning) {
                        new ServerThread( broadcastMessage.getText().toString()).start();
                    }
                }
            });
        }
        /*broadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!serverRunning) {
                    displayToast("Starting server");
                    new ServerThread(broadcastMessage.getText().toString()).start();
                }
            }
        });
        displayToast("Starting client");
        new ClientThread().start();*/
       handler = new android.os.Handler(Looper.getMainLooper()){
           @Override
           public void handleMessage(Message msg) {
               displayToast(msg.toString());
           }
       };
    }
    public InetAddress getBroadcastAddress()throws IOException {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        if(dhcpInfo == null) {
            return null;
        }

        int broadcast = (dhcpInfo.ipAddress & dhcpInfo.netmask) | ~dhcpInfo.netmask;
        byte quads[] = new byte[4];
        for(int i = 0; i<4; i++){
            quads[i] = (byte) ((broadcast >> i * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }
    /**
     * A simple server socket that accepts connection and
     * write some data on the buffer
     */
    public class ServerThread extends Thread {

        DatagramSocket datagramSocket;
        byte buffer[];
        DatagramPacket datagramPacket;

        public ServerThread(String s) {
            buffer = new byte[256];
            buffer = s.getBytes();
        }

        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket(PORT);
                datagramSocket.setBroadcast(true);
                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Server : Socket created", Toast.LENGTH_LONG).show();
                    }
                });


                    datagramPacket = new DatagramPacket(buffer, buffer.length, group, PORT);
                    datagramSocket.send(datagramPacket);
                    Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Server : packet send", Toast.LENGTH_LONG).show();
                        }
                    });
                    //Toast.makeText(getActivity(), "Sending ", Toast.LENGTH_SHORT).show();



            } catch (SocketException e) {
                e.printStackTrace();
            } catch(UnknownHostException e){
                e.printStackTrace();
            }catch(IOException e) {
                e.printStackTrace();
            }
        }
            //datagramSocket.close();
            //serverRunning = false;
        }

    public class ClientThread extends Thread{

        MulticastSocket multicastSocket;
        DatagramPacket datagramPacket;
        byte buffer[];

        @Override
        public void run() {
            try{
                multicastSocket = new MulticastSocket(PORT);
                multicastSocket.setBroadcast(true);

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client : socket created", Toast.LENGTH_LONG).show();
                    }
                });
                while(true) {
                    multicastSocket.joinGroup(group);
                    Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Client : group joined", Toast.LENGTH_LONG).show();
                        }
                    });
                    buffer = new byte[256];
                    datagramPacket = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(datagramPacket);
                    multicastSocket.leaveGroup(group);

                }
                //Toast.makeText(, "Received : " + buffer.toString(), Toast.LENGTH_SHORT).show();

            }catch (UnknownHostException e){

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 1", Toast.LENGTH_LONG).show();
                    }
                });
            }catch (IOException e){

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 2", Toast.LENGTH_LONG).show();
                    }
                });
            }
            multicastSocket.close();
        }
    }
    public void displayToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}