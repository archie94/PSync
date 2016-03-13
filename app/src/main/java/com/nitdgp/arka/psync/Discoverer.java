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
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * The Discoverer module
 */
public class Discoverer extends ActionBarActivity  {

    Button broadcast;
    Button startClient;
    android.os.Handler handler;

    private static final String SERVER_IP = "192.168.43.255";
    private static int PORT = 4446;
    private static boolean serverRunning = false;
    private static boolean clientRunning = false;
    InetAddress group ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discoverer);

        /*
        Initialise
         */
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
        /*
        If device is connected to a hotspot it will only receive the message
        If device is not connected assume it has hosted the hotspot -- it will broadcast the message
         */
        if(networkInfo.isConnected()){
            displayToast("Connected via wifi");
            startClient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!clientRunning) {
                        new ClientThread().start();
                        clientRunning = true;
                    }
                }
            });
        }else{
            displayToast("Not connected");
            broadcast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                   if (!serverRunning) {
                       new ServerThread().start();
                       serverRunning = true;
                   }
                }
            });
        }
    }

    /**
     * A server that will write data on the buffer continuously
     */
    public class ServerThread extends Thread {

        DatagramSocket datagramSocket;
        byte buffer[] = null;
        DatagramPacket datagramPacket;

        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket(PORT);
                datagramSocket.setBroadcast(true);
                buffer = "Msg from server".getBytes("UTF-8");
                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Server : Socket created", Toast.LENGTH_LONG).show();
                    }
                });

                while(true) {
                    datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(SERVER_IP), PORT);
                    datagramSocket.send(datagramPacket);
                    Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Server : packet send", Toast.LENGTH_LONG).show();
                        }
                    });
                    Thread.sleep(3000);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch(UnknownHostException e){
                e.printStackTrace();
            }catch(IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            datagramSocket.close();
        }
    }

    /**
     * A CLient to listen for broadcasts
     */
    public class ClientThread extends Thread{
        DatagramPacket datagramPacket;
        byte buffer[];
        DatagramSocket datagramSocket;

        @Override
        public void run() {
            WifiManager wifiManager;
            wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock("lock");
            multicastLock.acquire();
            try{
                datagramSocket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
                datagramSocket.setBroadcast(true);

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client : socket created", Toast.LENGTH_SHORT).show();
                    }
                });
                while(true) {
                    buffer = new byte[15000];
                    datagramPacket = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(datagramPacket);
                    byte[] data = datagramPacket.getData();
                    InputStreamReader inputStreamReader = new InputStreamReader(new ByteArrayInputStream(data), Charset.forName("UTF-8"));

                    final StringBuilder stringBuilder = new StringBuilder();
                    try {
                        for (int value; (value = inputStreamReader.read()) != -1; ) {
                            stringBuilder.append((char) value);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Msg Received " + stringBuilder.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } // end of while

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
            } finally {
                datagramSocket.close();
            }
            multicastLock.release();
        }
    }

    public void displayToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
}