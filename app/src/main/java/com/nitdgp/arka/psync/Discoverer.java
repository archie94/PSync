package com.nitdgp.arka.psync;
/**
 * @author  : Arka Prava
 * @version : 4 March 2016
 */
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Discoverer module
 */
public class Discoverer extends ActionBarActivity  {

    Button broadcast;
    Button stopBroadcast;
    Button listen;
    Button stopListen;
    ListView peerListView;
    volatile ConcurrentHashMap<String, Integer> peerList;

    private static final String SERVER_IP = "192.168.43.255";
    private static int PORT = 4446;
    private boolean serverRunning = false;
    private boolean clientRunning = false;


    private WebServer webServer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discoverer);

        /*
        Initialise
         */
        broadcast = (Button)findViewById(R.id.btn_broadcast);
        listen = (Button)findViewById(R.id.btn_listen);
        stopBroadcast = (Button)findViewById(R.id.btn_stop_broadcast);
        stopListen = (Button)findViewById(R.id.btn_stop_listener);
        peerListView = (ListView)findViewById(android.R.id.list);
        peerList = new ConcurrentHashMap<String, Integer>();
        /*
         * Check if device is connected via wifi
         */
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(networkInfo.isConnected()){
            displayToast("Connected via wifi");
        }else{
            displayToast("Not connected");
        }

        final UpdatePeerListViewThread updateView = new UpdatePeerListViewThread(this);
        final ServerThread server = new ServerThread() ;
        final ClientThread client = new ClientThread();
        final Thread[] thread = new Thread[3];

        thread[1] = new Thread(client);

        /*
        Broadcast message only if device is not listening for message
        Listen for message only if device itself not broadcasting
         */
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!clientRunning /*&& !serverRunning*/) {
                    if(thread[1].isAlive()){
                        client.revive();
                    }else {
                        thread[1] = new Thread(client);
                        thread[1].start();
                    }
                    thread[2] = new Thread(updateView);
                    thread[2].start();
                    clientRunning = true;
                }
            }
        });
        broadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!serverRunning /*&& !clientRunning*/) {
                    thread[0] = new Thread(server);
                    thread[0].start();
                    serverRunning = true;
                }
            }
        });
        stopBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serverRunning) {
                    server.stop();
                    serverRunning = false;
                }
            }
        });
        stopListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clientRunning) {
                    client.stop();
                    updateView.stop();
                    //clientRunning = false;
                }
            }
        });
        webServer = new WebServer(8080);
        try {
            webServer.start();
        } catch(IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        Log.w("Httpd", "Web server initialized.");
    }

    /**
     * A server that will write data on the buffer continuously
     */
    public class ServerThread implements Runnable {
        DatagramSocket datagramSocket;
        byte buffer[] = null;
        DatagramPacket datagramPacket;
        volatile boolean exit;

        public ServerThread() {
            exit = false;
        }

        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket();
                datagramSocket.setBroadcast(true);
                buffer = "Msg from server".getBytes("UTF-8");

                while(!exit) {
                    datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(SERVER_IP), PORT);
                    datagramSocket.send(datagramPacket);
                    Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Server : packet send", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Thread.sleep(3000);
                }
                exit = false;
            } catch (SocketException e) {
                e.printStackTrace();
            } catch(UnknownHostException e){
                e.printStackTrace();
            }catch(IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                datagramSocket.close();
            }

            Discoverer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Discoverer.this, "Broadcasting stopped", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void stop() {
            exit = true;
        }
    }

    /**
     * A client to listen for broadcasts
     */
    public class ClientThread implements Runnable{
        DatagramPacket datagramPacket;
        byte buffer[];
        DatagramSocket datagramSocket;
        volatile boolean exit;


        public ClientThread(){
            exit = false;
        }

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

                while(!exit) {
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
                    updatePeers(datagramPacket.getAddress().getHostAddress());
                } // end of while
                exit = false;
            }catch (UnknownHostException e){

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 1", Toast.LENGTH_SHORT).show();
                    }
                });
            }catch (IOException e){

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 2", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                datagramSocket.close();
            }

            multicastLock.release();

            Discoverer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Discoverer.this, "Listening stopped", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void stop() {
            exit = true;
            clientRunning = false;
        }

        /**
         * Handle zombie state of listening thread
         */
        public void revive() {
            exit = false;
        }

        /**
         * Update list of peers
         * @param s the ip address of the current peer
         */
        public void updatePeers(String s) {
            /*
            Put the ip address in the table
            Set its counter to 0
             */
            peerList.put(s, 0);
        }
    }

    /**
     * Thread to update the ListView of available peers
     */
    public class UpdatePeerListViewThread implements Runnable {
        boolean exit = false;

        Context context;
        public UpdatePeerListViewThread(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            while (!exit) {
                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> peer = new ArrayList<String>();
                        List<Integer> counter = new ArrayList<Integer>();
                        for (String s : peerList.keySet()) {
                            if(peerList.get(s) >= 10) {
                                peerList.remove(s);
                            } else {
                                peerList.put(s, peerList.get(s) + 1);
                                peer.add(s);
                                counter.add(peerList.get(s));
                            }
                        }
                        PeerListView peerListRow = new PeerListView(context, peer, counter);
                        peerListView.setAdapter(peerListRow);
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            exit = false;
        }

        public void stop() {
            exit = true;
        }
    }

    public void displayToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}